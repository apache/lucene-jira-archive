// Test driver for Lucene timeout functionality

import org.apache.lucene.store.*;
import org.apache.lucene.document.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.*;

class LuceneTimeoutTest {
  public static void main(String[] args) {
    try {
      System.out.println( "Opening index at: " + args[0] );
      Directory dir = FSDirectory.getDirectory(args[0]);
      Searcher searcher = new IndexSearcher(dir);
      //      TimerThread timer = new TimerThread(1);
      //timer.start();
      //searcher.setTimeout(1, timer);
      Analyzer analyzer = new SimpleAnalyzer();

      // simple query
      QueryParser parser = new QueryParser("name", analyzer);
      parser.setPhraseSlop(4);
      Query query = parser.parse("*:*");
      System.out.println("Query: " + query.toString("name"));

      // do search
      MyHitCollector myHc = new MyHitCollector();
      HitCollector collector = new TimeLimitedCollector(myHc, 100);
      long start = System.currentTimeMillis();
      try {
        searcher.search(query, collector);
      }
      catch(TimeLimitedCollector.TimeExceeded x) {
        System.out.println( "Search timed out." + x );
      }
      long elapsed = System.currentTimeMillis() - start;

      // print results
      System.out.println("Elapsed time: " + elapsed + " ms." );
      System.out.println(myHc.hitCount() + " total results");

      // clean up
      searcher.close();
      dir.close();
    }
    catch( Exception x ) {
      System.err.println( "Caught " + x );
    }
  }
}
