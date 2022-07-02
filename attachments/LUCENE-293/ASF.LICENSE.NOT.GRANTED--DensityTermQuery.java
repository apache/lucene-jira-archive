package org.apache.lucene.search;
/**
 * Copyright 2005 Apache Software Foundation
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

/** Derived from TermQuery, August 2004 */

import java.io.IOException;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;

/** A TermQuery that scores with a DensityTermScorer.
 */
public class DensityTermQuery extends TermQuery {
  /* needs protected:
   * class TermQuery.TermWeight
   * TermWeight.queryWeight
   * TermWeight.queryNorm
   * TermWeight.value
   */
  
  class DensityTermWeight extends TermQuery.TermWeight {
    public DensityTermWeight(Searcher searcher) {
      super(searcher);
    }
    
    public Query getQuery() { return DensityTermQuery.this; }

    public float sumOfSquaredWeights() throws IOException {
      queryWeight = getBoost(); 
      return queryWeight * queryWeight;
    }

    public void normalize(float queryNorm) {
      this.queryNorm = queryNorm;
      queryWeight *= queryNorm; // normalize query weight
      value = queryWeight; // idf for document 
    }
    
    public Scorer scorer(IndexReader reader) throws IOException {
      return new DensityTermScorer(this,
                                   reader.termDocs(getTerm()),
                                   similarity,
                                   reader.norms(getTerm().field()));
    }
  }

  /** Constructs a DensityTermQuery */
  public DensityTermQuery(Term t, Similarity similarity) {
    super(t);
    this.similarity = similarity;
  }
  
  private Similarity similarity;

  protected Weight createWeight(Searcher searcher) {
    return new DensityTermWeight(searcher);
  }
  
  public String toString() {
    return getTerm().field() + ":%:" + getTerm().text();
  }

  /** Returns true iff <code>o</code> is equal to this. */
  public boolean equals(Object o) {
    if (!(o instanceof DensityTermQuery))
      return false;
    DensityTermQuery other = (DensityTermQuery)o;
    return (this.getBoost() == other.getBoost())
      && this.getTerm().equals(other.getTerm());
  }
}
