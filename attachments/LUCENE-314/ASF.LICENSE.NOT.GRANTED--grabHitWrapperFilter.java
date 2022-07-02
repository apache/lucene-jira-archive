package org.apache.lucene.search;

import org.apache.lucene.search.Filter;
import java.util.BitSet;
import org.apache.lucene.index.IndexReader;
import java.io.IOException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Query;

/**
 * Goal: for one or multiple refinements search, filter the search with
 * anterior hits:
 *
 *  see main static method to testcase
 *
 * @author Nicolas Maisonneuve
 * @version 1.0
 */
public class grabHitWrapperFilter
    extends Filter {
  private Filter filter;
  private BitSet filterbit;
  private BitSet hitdocs ;

  public grabHitWrapperFilter() {
    this(null);
  }

  public grabHitWrapperFilter(Filter filter) {
    this.filter = filter;
  }

  final class GrabBitSet
      extends BitSet {
    public boolean get(int n) {
      hitdocs.set(n);
      return (filterbit == null) ? true : filterbit.get(n);
    }
  }

  public final BitSet bits(IndexReader reader) throws IOException {
    hitdocs    = new BitSet();
    if (filter != null) {
      filterbit = filter.bits(reader);
    }
    return new GrabBitSet();
  }

  public Filter getHitFilter() {
    return new Filter() {
      public BitSet bits(IndexReader reader) {
        return hitdocs;
      }
    };
  }

  static public void main(String[] args) {
    try {
      IndexSearcher search = new IndexSearcher("d:\\myindex");

      Query initquery = new TermQuery(new Term("", ""));
      Query subquery1 = new TermQuery(new Term("", ""));
      Query subquery2 = new TermQuery(new Term("", ""));
      Query subquery3 = new TermQuery(new Term("", ""));

      //FIRST SEARCH
      // we create a grabHitWrapperFilter to grab the hits for the next possible reffinement.
      //You can add your own filter ( new grabHitWrapper(myFilter) )
      grabHitWrapperFilter grabhit = new grabHitWrapperFilter();
      Hits hits = search.search(initquery, grabhit);

      //SECOND SEARCH: reffinement of the first search
      // the hit filter of the first search
      Filter filterhit = grabhit.getHitFilter();

      // we want grab the new hits for the next possible reffinement
      grabhit = new grabHitWrapperFilter(filterhit);

      hits = search.search(subquery1, grabhit);

      //three search: reffinement of the second search, no grab the hits just filter
      filterhit = grabhit.getHitFilter();
      hits = search.search(subquery1, filterhit);

    }
    catch (IOException e) {

    }
  }
}
