/*
 * Created on Sep 23, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.TermQuery;

/**
 * @author jnioche
 *
 */
public class Tester {

  public static void main(String args[]) {

    IndexSearcher searcher = null;
    try {
      searcher = new IndexSearcher(args[0]);
    }
    catch (Exception e) {
      e.printStackTrace();
      return;
    }

    PhraseQuery pq = new PhraseQuery();
    pq.add(new Term("titre", "asia"));
    pq.add(new Term("titre", "pacific"));
    pq.add(new Term("titre", "economic"));
    pq.add(new Term("titre", "co"));
    pq.add(new Term("titre", "operation"));
    pq.add(new Term("titre", "forum"));
    pq.setSlop(0);

    System.err.println(pq.toString());

    try {

      long l0 = System.currentTimeMillis();
      Hits hits = searcher.search(pq);
      long l1 = System.currentTimeMillis();
      System.err.println(hits.length() + " docs trouvés en " + (l1 - l0) + " msec");
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    try {
      searcher.close();
    }
    catch (Exception e) {
      e.printStackTrace();
    }

  }

}
