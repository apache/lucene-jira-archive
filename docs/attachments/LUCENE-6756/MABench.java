import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


public class MABench {

  // run this first to create the index
  public static void main1(String[] args) throws Exception {
    Directory dir = FSDirectory.open(Paths.get("/tmp/a"));
    IndexWriter w = new IndexWriter(dir, new IndexWriterConfig(null));
    Random r = new Random(0);
    for (int i = 0; i < 10000000; ++i) {
      Document doc = new Document();
      doc.add(new NumericDocValuesField("val", r.nextInt() & 0xFFFFFFFFL));
      w.addDocument(doc);
    }
    w.commit();
    w.close();
  }

  public static void main(String[] args) throws Exception {
    Directory dir = FSDirectory.open(Paths.get("/tmp/a"));
    DirectoryReader reader = DirectoryReader.open(dir);
    IndexSearcher searcher = new IndexSearcher(reader);
    Query q = new MatchAllDocsQuery();
    for (int i = 0; i < 1000; ++i) {
      MaxValueCollector c = new MaxValueCollector();
      long start = System.nanoTime();
      searcher.search(q, c);
      System.out.println((System.nanoTime() - start) / 1000 + " " + c.max);
    }
  }

  static class MaxValueCollector implements Collector {

    public long max = Long.MIN_VALUE;

    @Override
    public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
      final NumericDocValues values = context.reader().getNumericDocValues("val");
      return new LeafCollector() {

        @Override
        public void setScorer(Scorer scorer) throws IOException {
          // no-op
        }

        @Override
        public void collect(int doc) throws IOException {
          max = Math.max(max, values.get(doc));
        }
      };
    }

    @Override
    public boolean needsScores() {
      return false;
    }

  }

}
