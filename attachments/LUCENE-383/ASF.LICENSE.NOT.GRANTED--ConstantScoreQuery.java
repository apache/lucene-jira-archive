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

import org.apache.lucene.index.IndexReader;

import java.io.IOException;
import java.util.BitSet;

/**
 * A query that wraps a filter and simply returns a constant score for every document in the filter.
 *
 * @author yonik
 * @version $Id: ConstantScoreQuery.java,v 1.1 2005/04/24 23:33:00 yonik Exp $
 */
public class ConstantScoreQuery extends Query {
  protected final Filter filter;
  protected final float constantScore;
  protected static float DEFAULT_SCORE=1e-4f;

  public ConstantScoreQuery(Filter filter) {
    this.filter=filter;
    constantScore=DEFAULT_SCORE;
  }

  public ConstantScoreQuery(Filter filter, float constantScore) {
    this.filter=filter;
    this.constantScore=constantScore;
  }

  public Query rewrite(IndexReader reader) throws IOException {
    return this;
  }

  protected class ConstantWeight implements Weight {
    private Searcher searcher;
    private float queryNorm;
    private float queryWeight;
    private float value;

    public ConstantWeight(Searcher searcher) {
      this.searcher = searcher;
    }

    public Query getQuery() {
      return ConstantScoreQuery.this;
    }

    public float getValue() {
      return value;
    }

    public float sumOfSquaredWeights() throws IOException {
      queryWeight = constantScore * getBoost();
      return queryWeight * queryWeight;
    }

    public void normalize(float norm) {
      this.queryNorm = norm;
      queryWeight *= this.queryNorm;
      value = queryWeight * getBoost();
    }

    public Scorer scorer(IndexReader reader) throws IOException {
      return new ConstantScorer(getSimilarity(searcher), reader, this);
    }

    public Explanation explain(IndexReader reader, int doc) throws IOException {
      return new Explanation(); // TODO
    }
  }

  protected class ConstantScorer extends Scorer {
    protected final BitSet bits;
    protected final float theScore;
    int doc=-1;

    public ConstantScorer(Similarity similarity, IndexReader reader, Weight w) throws IOException {
      super(similarity);
      // TODO - is this where I factor in the boost?
      theScore = w.getValue();
      bits = filter.bits(reader);
    }

    public boolean next() throws IOException {
      doc = bits.nextSetBit(doc+1);
      return doc >= 0;
    }

    public int doc() {
      return doc;
    }

    public float score() throws IOException {
      return theScore;
    }

    public boolean skipTo(int target) throws IOException {
      doc = bits.nextSetBit(target);  // requires JDK 1.4
      return doc >= 0;
    }

    public Explanation explain(int doc) throws IOException {
      return new Explanation(); //TODO
    }
  }


  protected Weight createWeight(Searcher searcher) {
    return new ConstantScoreQuery.ConstantWeight(searcher);
  }


  /** Prints a user-readable version of this query. */
  public String toString(String field)
  {
    return "ConstantScore(" + filter.toString()
      + ", score=" + constantScore
      + (getBoost()==1.0 ? ")" : "^" + getBoost());
  }


  /** Returns true if <code>o</code> is equal to this. */
  public boolean equals(Object o) {
    if (true) throw new UnsupportedOperationException("Filters don't support equals yet!");
    if (this == o) return true;
    if (!(o instanceof ConstantScoreQuery)) return false;
    ConstantScoreQuery other = (ConstantScoreQuery)o;
    return this.getBoost()==other.getBoost() && filter.equals(other.filter);
  }

  /** Returns a hash code value for this object. */
  public int hashCode() {
    if (true) throw new UnsupportedOperationException("Filters don't support hashCode yet!");
    int h = Float.floatToIntBits(getBoost());
    int h2 = filter.hashCode();
    h ^= ((h2 << 8) | (h2 >>> 25));
    h ^= Float.floatToIntBits(constantScore);
    return h;
  }

}


