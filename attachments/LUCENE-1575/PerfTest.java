import org.apache.lucene.store.*;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.*;
import java.io.File;
import java.util.Random;

public class PerfTest {
  public static void main(String[] args) throws Exception {

    File indexDir = new File(args[0]);
    Directory dir = new FSDirectory(indexDir, new NativeFSLockFactory(indexDir));
		
    if (!indexDir.exists() || indexDir.list().length == 0) {
      Random r = new Random();
      r.seed(17);
      int numDocs = 10000000;
      IndexWriter writer = new IndexWriter(dir, null, MaxFieldLength.UNLIMITED);
      writer.setMergeFactor(50);
      writer.setMaxBufferedDocs(numDocs/100);
      System.out.println("populating index");
      long time = System.currentTimeMillis();
      for (int i = 0; i < numDocs; i++) {
        Document doc = new Document();
        doc.add(new Field("c", "test", Store.NO, Index.NOT_ANALYZED));
        doc.add(new Field("i", r.nextInt()+"", Store.NO, Index.NOT_ANALYZED));
        writer.addDocument(doc);
      }
      writer.close(false);
      System.out.println("time=" + (System.currentTimeMillis() - time));
    }
		
    System.out.println("searching");
    IndexSearcher searcher = new IndexSearcher(dir);
    System.out.println("numSegments=" + searcher.getIndexReader().getSequentialSubReaders().length);
    Sort sort = new Sort(new SortField("i", SortField.INT));
    final TermQuery q = new TermQuery(new Term("c", "test"));
    searcher.search(q, null, 10, sort);
    long time = System.currentTimeMillis();
    double numQueries = 20;
    long bestTime = Long.MAX_VALUE;
    int sum = 0;
    for (int i = 0; i < numQueries; i++) {
      final long t0 = System.nanoTime();
      sum += searcher.search(q, null, 10, sort).totalHits;
      final long t = System.nanoTime()-t0;
      //System.out.println((t1-t0) + " millis");
      if (t < bestTime) {
        bestTime = t;
      }
    }
    time = System.currentTimeMillis() - time;
    //System.out.println("avg. time=" + (time / numQueries)
    //+ " ms");
    System.out.println("best time=" + (bestTime/1000000.0) + " ms; avg=" + (time/numQueries) + " ms; sum=" + (sum/1000000) + " M");
    searcher.close();
  }
}