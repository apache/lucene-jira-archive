
import java.io.*;
import java.util.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.*;
import org.junit.*;
import static org.junit.Assert.assertEquals;

public class Lucene6207Test {

  /**
   * Verifies that multiple filtered subsets of the same physical index passed to
   * IW.addIndexes() produce an index with correct SortedDocValues.
   *
   * Fails for Lucene 4.9 up to 4.10.3.
   */
  @Test
  public void addIndexesShouldProduceCorrectSortedDocValues() throws IOException {
    final int TOTAL_DOC_COUNT = 3;
    final int SPLIT_TERM_COUNT = 2;

    // Create index
    Directory dir = new RAMDirectory();
    createTestIndex(dir, Version.LATEST, TOTAL_DOC_COUNT, SPLIT_TERM_COUNT);

    // Filters to split by
    Filter[] filters = new Filter[SPLIT_TERM_COUNT];
    for (int i = 0; i < SPLIT_TERM_COUNT; i++) {
      filters[i] = new TermsFilter("split", new BytesRef(Integer.toString(i)));
    }

    // Get filtering readers
    DirectoryReader reader = DirectoryReader.open(dir);
    int leafCount = reader.leaves().size();
    CodecReader[][] views = new CodecReader[SPLIT_TERM_COUNT][leafCount];
    for (int filterIdx = 0; filterIdx < SPLIT_TERM_COUNT; filterIdx++) {
      for (int leafIdx = 0; leafIdx < leafCount; leafIdx++) {
        views[filterIdx][leafIdx] = new FilteredView(reader.leaves().get(leafIdx), filters[filterIdx]);
      }
    }

    // 1) Call addIndexes() separately for each filter term, i.e. there is no
    //    "overlapping" between the readers passed in each addIndexes() call.
    //    This passes always, even in 4.9/4.10.
    Directory compoundDest = buildIndexFromViews(views);
    // Assert that docs have correct SortedDocValues
    checkIndexCorrectness(compoundDest);

    // 2) Call addIndexes() once passing all views (i.e. overlapping occurs).
    //    Fails in 4.9/4.10.
    List<IndexReader> allViews = new ArrayList<>();
    for (IndexReader[] v : views) {
      allViews.addAll(Arrays.asList(v));
    }
    compoundDest = buildIndexFromViews(allViews.toArray(new CodecReader[allViews.size()]));
    // Assert that docs have correct SortedDocValues
    checkIndexCorrectness(compoundDest);
  }

  private static Directory buildIndexFromViews (CodecReader[]... views) throws IOException
  {
    Directory dir = new RAMDirectory();
    try (IndexWriter iw = new IndexWriter(dir, new IndexWriterConfig(null))) {
      for (CodecReader[] group : views)
        iw.addIndexes(group);
    }
    return dir;
  }

  private static void createTestIndex(Directory dir, Version version, int docCount, int splitTermCount) throws IOException {
    try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(null))) {
      for (int i = 0; i < docCount; i++) {
        Document doc = new Document();
        String id = Integer.toString(i + 1);
        String splitVal = Integer.toString(i % splitTermCount);
        doc.add(new Field("id", id, StringField.TYPE_STORED));
        doc.add(new Field("split", splitVal, StringField.TYPE_STORED));
        doc.add(new SortedDocValuesField("sdv", new BytesRef(id)));
        writer.addDocument(doc);
      }
    }
  }

  // Asserts that documents have correct SortedDocValues
  private static void checkIndexCorrectness(Directory dir) throws IOException {
    DirectoryReader r = DirectoryReader.open(dir);
    for (LeafReaderContext ctx : r.leaves()) {
      LeafReader rs = ctx.reader();
      SortedDocValues sdv = rs.getSortedDocValues("sdv");
      for (int docId = 0; docId < rs.maxDoc(); docId++) {
        StoredDocument doc = rs.document(docId);
        String id = doc.get("id");
        String splitval = doc.get("split");
        String idFromDocVal = sdv.get(docId).utf8ToString();
        assertEquals(String.format("Correct docval? (split val %s, doc %d, id %s)", splitval, docId, id),
                     id, idFromDocVal);
      }
    }
  }

  /**
   * An AtomicReader exposing a subset of documents from an underlying reader. Basically
   * the same as PKIndexSplitter.DocumentFilteredAtomicIndexReader.
   */
  private static class FilteredView extends FilterCodecReader {

    final FixedBitSet liveDocs;
    protected int numDocs;

    public FilteredView(LeafReaderContext context, Filter filter) throws IOException {
      // test will pass, since this will dodge reuse bugs: super(SlowCodecReaderWrapper.wrap(context.reader()));
      super(SlowCodecReaderWrapper.wrap(new FilterLeafReader(context.reader())));

      final int maxDoc = in.maxDoc();
      final FixedBitSet bits = new FixedBitSet(maxDoc);

      final DocIdSet docs = filter.getDocIdSet(context, in.getLiveDocs());
      if (docs != null) {
        final DocIdSetIterator it = docs.iterator();
        if (it != null) {
          bits.or(it);
        }
      }

      this.liveDocs = bits;
      this.numDocs = bits.cardinality();
    }

    @Override
    public int numDocs() {
      return numDocs;
    }

    @Override
    public boolean hasDeletions() {
      return (in.maxDoc() != numDocs);
    }

    @Override
    public Bits getLiveDocs() {
      return liveDocs;
    }
  }
}