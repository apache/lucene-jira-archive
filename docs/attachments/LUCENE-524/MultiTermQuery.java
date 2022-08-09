package org.apache.lucene.search;

/**
 * Copyright 2004 The Apache Software Foundation
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

import java.io.IOException;
import java.util.Vector;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

/**
 * A {@link Query} that matches documents containing a subset of terms provided
 * by a {@link FilteredTermEnum} enumeration.
 * <P>
 * <code>MultiTermQuery</code> is not designed to be used by itself.
 * <BR>
 * The reason being that it is not intialized with a {@link FilteredTermEnum}
 * enumeration. A {@link FilteredTermEnum} enumeration needs to be provided.
 * <P>
 * For example, {@link WildcardQuery} and {@link FuzzyQuery} extend
 * <code>MultiTermQuery</code> to provide {@link WildcardTermEnum} and
 * {@link FuzzyTermEnum}, respectively.
 */
public abstract class MultiTermQuery extends Query {
    private Term term;
    private Vector<TermQuery> tqueries = new Vector<TermQuery>();

    /** Constructs a query for terms matching <code>term</code>. */
    public MultiTermQuery(Term term) {
        this.term = term;
    }

    /** Returns the pattern term. */
    public Term getTerm() { return term; }
    
    private class MultiTermWeight implements Weight {

		private Searcher searcher;
        private Vector<Weight> weights = new Vector<Weight>();

        public MultiTermWeight(Searcher searcher) {
          this.searcher = searcher;
          for (TermQuery tq : tqueries) {
              Weight w = tq.createWeight(searcher);
              weights.add(w);       	  
          }
        }

        public Query getQuery() { return MultiTermQuery.this; }
        public float getValue() { return getBoost(); }

        public float sumOfSquaredWeights() throws IOException {
          float maxw = 0.0f;
          for (int i = 0 ; i < weights.size(); i++) {
			Weight w = (Weight)weights.elementAt(i);
			maxw = Math.max(maxw, w.sumOfSquaredWeights()); // this isn't quite right because max weight doesn't necessarily correspond to the same
			                                              // term that generates max score; no significantly better way to normalize however
          }

          maxw *= getBoost() * getBoost();             // boost for this query's factor

          return maxw ;
        }


        public void normalize(float norm) {
          norm *= getBoost();                         // incorporate boost
          for (int i = 0 ; i < weights.size(); i++) {
            Weight w = (Weight)weights.elementAt(i);
            w.normalize(norm);
          }
        }

        public Scorer scorer(IndexReader reader) throws IOException {
        	MultiTermScorer result = new MultiTermScorer(getSimilarity(searcher));
        	for (Weight w : weights) result.add(w.scorer(reader));
            return result;
        }

        public Explanation explain(IndexReader reader, int doc)
          throws IOException {
          Explanation maxExpl = new Explanation();
          maxExpl.setDescription("max of:");
          float maxsc = 0.0f;
          int maxidx = 0;
          for (int i = 0 ; i < weights.size(); i++) {
            Weight w = weights.elementAt(i);
            Scorer scorer = w.scorer(reader);
            scorer.skipTo(doc);
            if (scorer.doc() != doc) continue;
            if (maxsc < scorer.score()) {
            	maxsc = scorer.score();
            	maxidx = i;
            }
          }
          Explanation e = weights.elementAt(maxidx).explain(reader, doc);
	        if (e.getValue() > 0) {
	            maxExpl.addDetail(e);
	        }
   
          maxExpl.setValue(maxsc);
          return maxExpl;

        }
      }

      protected Weight createWeight(Searcher searcher) {
        return new MultiTermWeight(searcher);
      }

    /** Construct the enumeration to be used, expanding the pattern term. */
    protected abstract FilteredTermEnum getEnum(IndexReader reader)
      throws IOException;

    /** Prints a user-readable version of this query. */
    public String toString(String field) {
        StringBuffer buffer = new StringBuffer();
        if (!term.field().equals(field)) {
            buffer.append(term.field());
            buffer.append(":");
        }
        buffer.append(term.text());
        if (getBoost() != 1.0f) {
            buffer.append("^");
            buffer.append(Float.toString(getBoost()));
        }
        return buffer.toString();
    }

	@Override
	public Query rewrite(IndexReader reader) throws IOException {
        FilteredTermEnum enumerator = getEnum(reader);
        try {
          do {
            Term t = enumerator.term();
            if (t != null) {
              TermQuery tq = new TermQuery(t);      // found a match
              tq.setBoost(getBoost() * enumerator.difference()); // 1.9.1 change * enumerator.difference()); // set the boost
              tqueries.add(tq);
            }
          } while (enumerator.next());
        } finally {
          enumerator.close();
        }
        return this;
	}

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof MultiTermQuery)) return false;

      final MultiTermQuery multiTermQuery = (MultiTermQuery) o;

      if (!term.equals(multiTermQuery.term)) return false;

      return getBoost() == multiTermQuery.getBoost();
    }

    public int hashCode() {
      return term.hashCode() + Float.floatToRawIntBits(getBoost());
    }
}
