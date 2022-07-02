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

	String field = "text";

    TermQuery tq1 = new TermQuery(new Term(field, "directeur"));
    TermQuery tq2 = new TermQuery(new Term(field, "directeurs"));

    BooleanQuery requete1 = new BooleanQuery();
    requete1.add(tq1, false, false);
    requete1.add(tq2, false, false);

    PhraseQuery pq = new PhraseQuery();
    pq.add(new Term(field, "recherche"));
    pq.add(new Term(field, "informations"));

    BooleanQuery requete2 = new BooleanQuery();
    requete2.add(pq, false, false);

    BooleanQuery bigger = new BooleanQuery();
	bigger.add(requete1, true, false);
    bigger.add(requete2, true, false);


    System.err.println(bigger.toString());


    try {

      long l0 = System.currentTimeMillis();
      Hits hits = searcher.search(bigger);
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
