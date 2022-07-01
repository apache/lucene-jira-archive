import java.io.File;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.TestUtil;


public class SortBench {

  private static final Path DIR_PATH = new File("/tmp/a").toPath();
  private static long dummy; // prevent JVM optimizations

  public static void main(String[] args) throws Exception {
    writeIndexIfNecessary();
    FSDirectory dir = FSDirectory.open(DIR_PATH);
    IndexReader reader = DirectoryReader.open(dir);
    System.out.println(reader.maxDoc() + " docs in the index");
    System.out.println(reader.leaves().size() + " segments");
    IndexSearcher searcher = new IndexSearcher(reader);
    for (int k = 0; k < 3; ++k) {
      System.out.println();
      long longAsc = Long.MAX_VALUE;
      long doubleAsc = Long.MAX_VALUE;
      long stringAsc = Long.MAX_VALUE;
      long longDesc = Long.MAX_VALUE;
      long doubleDesc = Long.MAX_VALUE;
      long stringDesc = Long.MAX_VALUE;
      long multiAsc = Long.MAX_VALUE;
      long multiDesc = Long.MAX_VALUE;
      for (int i = 0; i < 10; ++i) {
        long start = System.nanoTime();
        TopFieldDocs topDocs = searcher.search(new MatchAllDocsQuery(), 50, new Sort(
            new SortField("a", SortField.Type.LONG),
            new SortField("b", SortField.Type.DOUBLE)));
        long duration = System.nanoTime() - start;
        dummy = topDocs.scoreDocs[0].doc;
        if (i > 3)
          multiAsc =  Math.min(multiAsc, duration);

        start = System.nanoTime();
        topDocs = searcher.search(new MatchAllDocsQuery(), 50, new Sort(
            new SortField("a", SortField.Type.LONG, true),
            new SortField("b", SortField.Type.DOUBLE, true)));
        duration = System.nanoTime() - start;
        dummy = topDocs.scoreDocs[0].doc;
        if (i > 3)
          multiDesc =  Math.min(multiDesc, duration);

        start = System.nanoTime();
        topDocs = searcher.search(new MatchAllDocsQuery(), 50, new Sort(
            new SortField("a", SortField.Type.LONG)));
        duration = System.nanoTime() - start;
        dummy = topDocs.scoreDocs[0].doc;
        if (i > 3)
          longAsc =  Math.min(longAsc, duration);

        start = System.nanoTime();
        topDocs = searcher.search(new MatchAllDocsQuery(), 50, new Sort(
            new SortField("b", SortField.Type.DOUBLE)));
        duration = System.nanoTime() - start;
        dummy = topDocs.scoreDocs[0].doc;
        if (i > 3)
          doubleAsc =  Math.min(doubleAsc, duration);

        start = System.nanoTime();
        topDocs = searcher.search(new MatchAllDocsQuery(), 50, new Sort(
            new SortField("c", SortField.Type.STRING)));
        duration = System.nanoTime() - start;
        dummy = topDocs.scoreDocs[0].doc;
        if (i > 3)
          stringAsc =  Math.min(stringAsc, duration);

        start = System.nanoTime();
        topDocs = searcher.search(new MatchAllDocsQuery(), 50, new Sort(
            new SortField("a", SortField.Type.LONG, true)));
        duration = System.nanoTime() - start;
        dummy = topDocs.scoreDocs[0].doc;
        if (i > 3)
          longDesc =  Math.min(longDesc, duration);

        start = System.nanoTime();
        topDocs = searcher.search(new MatchAllDocsQuery(), 50, new Sort(
            new SortField("b", SortField.Type.DOUBLE, true)));
        duration = System.nanoTime() - start;
        dummy = topDocs.scoreDocs[0].doc;
        if (i > 3)
          doubleDesc =  Math.min(doubleDesc, duration);

        start = System.nanoTime();
        topDocs = searcher.search(new MatchAllDocsQuery(), 50, new Sort(
            new SortField("c", SortField.Type.STRING, true)));
        duration = System.nanoTime() - start;
        dummy = topDocs.scoreDocs[0].doc;
        if (i > 3)
          stringDesc =  Math.min(stringDesc, duration);
      }
      System.out.println("long asc\t" + longAsc / 1000000);
      System.out.println("long desc\t" + longDesc / 1000000);
      System.out.println("double asc\t" + doubleAsc / 1000000);
      System.out.println("double desc\t" + doubleDesc / 1000000);
      System.out.println("string asc\t" + stringAsc / 1000000);
      System.out.println("string desc\t" + stringDesc / 1000000);
      System.out.println("multi asc\t" + multiAsc / 1000000);
      System.out.println("multi desc\t" + multiDesc / 1000000);
    }
    reader.close();
    dir.close();
  }

  static void writeIndexIfNecessary() throws Exception {
    if (DIR_PATH.toFile().exists()) {
      System.out.println("Index already exists");
      return;
    }
    System.out.println("Creating index");
    FSDirectory dir = FSDirectory.open(DIR_PATH);
    IndexWriter w = new IndexWriter(dir, new IndexWriterConfig(new KeywordAnalyzer()));
    Document doc = new Document();
    NumericDocValuesField f1 = new NumericDocValuesField("a", 0);
    NumericDocValuesField f2 = new NumericDocValuesField("b", 0);
    SortedDocValuesField f3 = new SortedDocValuesField("c", new BytesRef());
    doc.add(f1);
    doc.add(f2);
    doc.add(f3);
    Random r = ThreadLocalRandom.current();
    for (int i = 0; i < 10000000; ++i) {
      f1.setLongValue(r.nextInt(1000));
      f2.setLongValue(Double.doubleToLongBits(r.nextDouble()));
      f3.setBytesValue(new BytesRef(TestUtil.randomSimpleString(r)));
      w.addDocument(doc);
      if (i % 100000 == 0) {
        System.out.println(i + " docs indexed");
      }
    }
    w.commit();
    w.close();
    dir.close();
    System.out.println("Finished");
  }

}
