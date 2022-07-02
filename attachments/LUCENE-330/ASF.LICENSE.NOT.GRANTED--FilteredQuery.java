package org.apache.lucene.search;
/**
 * Copyright 2004-2005 Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.DocNrSkipper;
import org.apache.lucene.util.SortedVIntList;
import java.io.IOException;
import java.util.BitSet;


/**
 * A query that applies a filter to the results of another query.
 * <br>The scorer of the query must provide skipTo().
 *
 * <p>Currently BooleanScorer does not have this property, which
 * means that the query should not be a BooleanQuery.
 * <br>An alternative boolean scorer is decribed here:
 * http://issues.apache.org/bugzilla/show_bug.cgi?id=31785
 * <br> The corresponding patch
 * "The replacement for BooleanScorer built into BooleanQuery"
 * is here:
 * http://issues.apache.org/bugzilla/attachment.cgi?id=13739
 *
 * <p>Note: When a Filter is used, the Filter bits are retrieved
 * from the filter each time this query is used in a search<br>
 * Use a {@link CachingWrapperFilter} to avoid
 * regenerating Filter bits every time.
 *
 * <p>Created: Apr 20, 2004 8:58:29 AM
 *
 * @author  Tim Jones
 * @author  Paul Elschot
 * @since   1.4
 * @version $Id: Feb 8, 2005, Paul Elschot$
 */
public class FilteredQuery
extends Query {
  private Query query;
  private Filter filter = null;
  private SkipFilter skipFilter = null;

  /**
   * Constructs a new query which applies a filter to the results
   * of the original query.
   * Filter.bits() will be called every time this query is used in a search.
   * <p>
   * This version always converts the BitSet to an intIterator for testing
   * the IterFilter.
   * @param query  Query to be filtered, cannot be <code>null</code>.
   * @param filter Filter to apply to query results, cannot be <code>null</code>.
   */
  public FilteredQuery(Query query, Filter filter) {
    this.query = query;
    this.filter = filter;
    //useSkipFilter(); // for testing only
  }

  /**
   * Constructs a new query which applies an skip filter to the results
   * of the original query.
   * SkipFilter.getDocNrSkipper() will be called every time this query
   * is used in a search.
   * @param query  Query to be filtered, cannot be <code>null</code>.
   * @param skipFilter To apply to query results, cannot be <code>null</code>.
   */
  public FilteredQuery(Query query, SkipFilter skipFilter) {
    this.query = query;
    this.skipFilter = skipFilter;
  }

  /** Switch from Filter to SkipFilter, for testing only. */
  void useSkipFilter() {
    if (filter == null) {
      return;
    }
    skipFilter = new SkipFilter() {
      private Filter filt = filter;
      public DocNrSkipper getDocNrSkipper(IndexReader ir)
          throws IOException {
        BitSet bitset = filt.bits(ir);
        SortedVIntList svil = new SortedVIntList(bitset);
        return svil.getDocNrSkipper();
      }
    };
    filter = null;
  }

  /**
   * Returns a Weight that applies the filter to the enclosed query's Weight.
   * This is accomplished by overriding the Scorer returned by the Weight.
   */
  protected Weight createWeight(final Searcher searcher) {
    final Weight weight = query.createWeight(searcher);

    abstract class FilteredWeight implements Weight {
      public float getValue() {
        return weight.getValue();
      }

      public float sumOfSquaredWeights() throws IOException {
        return weight.sumOfSquaredWeights();
      }

      public void normalize(float v) {
        weight.normalize(v);
      }

      public Explanation explain(IndexReader ir, int docNr) throws IOException {
        return scorer(ir).explain(docNr);
      }

      public Query getQuery() {
        return FilteredQuery.this;
      }
    }

    abstract class FilteredScorer extends Scorer {
      Scorer scorer;
      int scorerDocNr = -1;
      int filterDocNr = -1;

      FilteredScorer(Similarity similarity, Scorer scorer) {
        super(similarity);
        this.scorer = scorer;
      }

      public Explanation explain(int docNr) throws IOException {
        Explanation res = new Explanation();
        Explanation scxp = scorer.explain(docNr);
        if (filterSkipTo(docNr) && (filterDocNr == docNr)) {
          res.setValue(scxp.getValue());
          res.setDescription("allowed by filter");
        } else {
          res.setValue(0.0f); // indicates no match
          res.setDescription("removed by filter");
        }
        res.addDetail(scxp);
        return res;
      }

      public float score() throws IOException {
        return scorer.score();
      }

      boolean scorerSkipTo(int docNr) throws IOException {
        if (! scorer.skipTo(docNr)) {
          return false;
        }
        scorerDocNr = scorer.doc();
        return true;
      }

      abstract int filterNextDocNr(int docNr);

      boolean filterSkipTo(int docNr) {
        filterDocNr = filterNextDocNr(docNr);
        return filterDocNr != -1;
      }

      public int doc() {
        return scorerDocNr;
      }

      public boolean next() throws IOException {
        // requires initialisation of scorerDocNr at -1
        return skipTo(scorerDocNr + 1);
      }

      public boolean skipTo(int docNr) throws IOException {
        assert scorerDocNr == filterDocNr : "skipTo or next already return false";
        assert scorerDocNr < docNr : "can only skipTo forward";
        if (! filterSkipTo(docNr)) {
          return false;
        }
        assert scorerDocNr < filterDocNr;
        if (! scorerSkipTo(filterDocNr)) {
          return false;
        }
        while (filterDocNr != scorerDocNr) {
          assert filterDocNr < scorerDocNr;
          if (! filterSkipTo(scorerDocNr)) {
            return false;
          }
          assert scorerDocNr <= filterDocNr;
          if (scorerDocNr < filterDocNr) {
            if (! scorerSkipTo(filterDocNr)) {
              return false;
            }
          }
        }
        return true;
      }
    }

    return new FilteredWeight() {
      public Scorer scorer(IndexReader ir) throws IOException {
        if (filter != null) { // use Filter BitSet
          final BitSet bitset = filter.bits(ir);
          return new FilteredScorer(query.getSimilarity(searcher),
                                    weight.scorer(ir)) {
            int filterNextDocNr(int docNr) {
              return bitset.nextSetBit(docNr);
            }
          };
        } else { // use SkipFilter DocNrSkipper
          final DocNrSkipper docNrSkipper = skipFilter.getDocNrSkipper(ir);
          return new FilteredScorer(query.getSimilarity(searcher),
                                    weight.scorer(ir)) {
            int filterNextDocNr(int docNr) {
              return docNrSkipper.nextDocNr(docNr);
            }
          };
        }
      }
    };
  }

  /** Rewrites the wrapped query. */
  public Query rewrite(IndexReader reader) throws IOException {
    Query rewritten = query.rewrite(reader);
    if (rewritten != query) {
      FilteredQuery clone = (FilteredQuery)this.clone();
      clone.query = rewritten;
      return clone;
    } else {
      return this;
    }
  }

  public Query getQuery() {
    return query;
  }

  /** Prints a user-readable version of this query. */
  public String toString(String s) {
    return "filtered(" + query.toString(s) + ")->" + filter;
  }

  /** Returns true iff <code>o</code> is equal to this. */
  public boolean equals(Object o) {
    if (!(o instanceof FilteredQuery)) {
      return false;
    } else {
      FilteredQuery fq = (FilteredQuery) o;
      return query.equals(fq.query) &&
              ((filter != null)
               ? filter.equals(fq.filter)
               : skipFilter.equals(fq.skipFilter));
    }
  }

  /** Returns a hash code value for this object. */
  public int hashCode() {
    return query.hashCode() ^
          ((filter != null)
           ? filter.hashCode()
           : skipFilter.hashCode());
  }
}
