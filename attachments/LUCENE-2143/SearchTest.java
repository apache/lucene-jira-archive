import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.document.*;
import org.apache.lucene.util.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.search.*;
import java.io.*;

public class SearchTest {

  public static void main(String[] args) throws Exception {
    Directory dir = FSDirectory.open(new File(args[0]));
    String warmMethod = args[1];

    if (warmMethod.equals("nrt") || warmMethod.equals("writer")) {
      IndexWriter w = new IndexWriter(dir, new SimpleAnalyzer(), IndexWriter.MaxFieldLength.UNLIMITED);
      Document d = new Document();
      d.add(new Field("body", "a ba cd e f", Field.Store.NO, Field.Index.ANALYZED));
      for(int iter=0;iter<20000;iter++) {
        w.addDocument(d);
        if (warmMethod.equals("nrt") && (iter%200) == 0) {
          w.getReader().close();
        }
      }
      w.rollback();
    } else if (warmMethod.equals("reader") || warmMethod.equals("searcher")) {
      for(int iter=0;iter<10;iter++) {
        IndexReader r = IndexReader.open(dir);
        if (warmMethod.equals("searcher")) {
          IndexSearcher s = new IndexSearcher(r);
          for(int i=0;i<5;i++) {
            for(int j=0;j<10;j++) {
              s.search(new TermQuery(new Term("body", ""+j)), 10);
            }
          }
        }

        r.close();
      }
    } else if (warmMethod.equals("none")) {
      // ok
    } else {
      throw new RuntimeException("warmMethod must be nrt, writer, reader, searcher, or none");
    }

    IndexReader r = IndexReader.open(dir);
    IndexSearcher s = new IndexSearcher(r);
    System.out.println("do searches...");
    long tot = 0;
    long minTime = 0;
    for(int iter=0;iter<10;iter++) {
      final long t0 = System.currentTimeMillis();
      for(int i=0;i<20;i++) {
        for(int j=0;j<10;j++) {
          tot += s.search(new TermQuery(new Term("body", ""+j)), 10).totalHits;
        }
      }
      final long t = System.currentTimeMillis() - t0;
      if (iter == 0 || t < minTime) {
        minTime = t;
      }
      System.out.println(iter + ": took " + t + " msec; tot=" + tot);
    }
    dir.close();
    System.out.println("BEST " + minTime + " msec");
    r.close();
  }

}