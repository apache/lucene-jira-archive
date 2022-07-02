package org.apache.lucene.search;

import org.apache.lucene.search.*;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ComplexExplanation;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.ToStringUtils;

/**
 * Query that sets document score as a programmatic function of (up to) two (sub) scores.
 * <ol>
 *    <li>the score of its subQuery (any query)</li>
 *    <li>(optional) the score of its boosting Query,
 *        for most simple/convineient use case this query would be a 
 *        {@link org.apache.lucene.search.function.FieldScoreQuery FieldScoreQuery}</li>
 * </ol>
 * Subclasses can modify the computation by overriding {@link #customScore(int, float, float)}.
 *
 * Note: documents will only match based on the first sub scorer.
 * 
 * <p><font color="#FF0000">
 * WARNING: The status of the <b>search.function</b> package is experimental. 
 * The APIs introduced here might change in the future and will not be 
 * supported anymore in such a case.</font>
 */
public class CustomBoostQuery extends Query {

  private Query subQuery;
  private Query boostQuery; // optional, can be null
  private boolean strict = false; // if true, boosting part of query does not take part in weights normalization.  
  
  /**
   * Create a CustomBoostQuery over input subQuery.
   * @param subQuery the sub query whose scored is being customed. Must not be null. 
   */
  public CustomBoostQuery(Query subQuery) {
    this(subQuery,null);
  }

  /**
   * Create a CustomBoostQuery over input subQuery and a {@link Query}.
   * @param subQuery the sub query whose score is being customed. Must not be null.
   * @param boostQuery a value source query whose scores are used in the custom score
   * computation. For most simple/convineient use case this would be a 
   * {@link org.apache.lucene.search.function.FieldScoreQuery FieldScoreQuery}.
   * This parameter is optional - it can be null.
   */
  public CustomBoostQuery(Query subQuery, Query boostQuery) {
    super();
    this.subQuery = subQuery;
    this.boostQuery = boostQuery;
    if (subQuery == null) throw new IllegalArgumentException("<subqyery> must not be null!");
  }

  /*(non-Javadoc) @see org.apache.lucene.search.Query#rewrite(org.apache.lucene.index.IndexReader) */
  public Query rewrite(IndexReader reader) throws IOException {
    subQuery = subQuery.rewrite(reader);
    if (boostQuery!=null) {
      boostQuery = (Query) boostQuery.rewrite(reader);
    }
    return this;
  }

  /*(non-Javadoc) @see org.apache.lucene.search.Query#extractTerms(java.util.Set) */
  public void extractTerms(Set terms) {
    subQuery.extractTerms(terms);
    if (boostQuery!=null) {
      boostQuery.extractTerms(terms);
    }
  }

  /*(non-Javadoc) @see org.apache.lucene.search.Query#clone() */
  public Object clone() {
    CustomBoostQuery clone = (CustomBoostQuery)super.clone();
    clone.subQuery = (Query) subQuery.clone();
    if (boostQuery!=null) {
      clone.boostQuery = (Query) boostQuery.clone();
    }
    return clone;
  }

  /* (non-Javadoc) @see org.apache.lucene.search.Query#toString(java.lang.String) */
  public String toString(String field) {
    StringBuffer sb = new StringBuffer(name()).append("(");
    sb.append(subQuery.toString(field));
    if (boostQuery!=null) {
      sb.append(", ").append(boostQuery.toString(field));
    }
    sb.append(")");
    sb.append(strict?" STRICT" : "");
    return sb.toString() + ToStringUtils.boost(getBoost());
  }

  /** Returns true if <code>o</code> is equal to this. */
  public boolean equals(Object o) {
    if (getClass() != o.getClass()) {
      return false;
    }
    CustomBoostQuery other = (CustomBoostQuery)o;
    return this.getBoost() == other.getBoost()
           && this.subQuery.equals(other.subQuery)
           && (this.boostQuery==null ? other.boostQuery==null 
               : this.boostQuery.equals(other.boostQuery));
  }

  /** Returns a hash code value for this object. */
  public int hashCode() {
    int boostHash = boostQuery==null ? 0 : boostQuery.hashCode();
    return (getClass().hashCode() + subQuery.hashCode() + boostHash) ^ Float.floatToIntBits(getBoost());
  }  
  
  /**
   * Compute a custom score by the subQuery score and the Query score.
   * <p> 
   * Subclasses can override this method to modify the custom score.
   * <p>
   * The default computation herein is:
   * <pre>
   *     ModifiedScore = boostScore * subQueryScore.
   * </pre>
   * 
   * @param doc id of scored doc. 
   * @param subQueryScore score of that doc by the subQuery.
   * @param boostScore score of that doc by the Query.
   * @return custom score.
   */
  public float customScore(int doc, float subQueryScore, float boostScore) {
    return boostScore * subQueryScore;
  }

  /**
   * Explain the custom score.
   * Whenever overriding {@link #customScore(int, float, float)}, 
   * this method should also be overriden to provide the correct explanation
   * for the part of the custom scoring. 
   * @param doc doc being explained.
   * @param subQueryExpl explanation for the sub-query part.
   * @param boostExpl explanation for the value source part.
   * @return an explanation for the custom score
   */
  public Explanation customExplain(int doc, Explanation subQueryExpl, Explanation boostExpl) {
    float boostScore = boostExpl==null ? 1 : boostExpl.getValue();
    Explanation exp = new Explanation( boostScore * subQueryExpl.getValue(), "custom score: product of:");
    exp.addDetail(subQueryExpl);
    if (boostExpl != null) {
      exp.addDetail(boostExpl);
    }
    return exp;
  }
  //=========================== W E I G H T ============================
  
  private class CustomWeight implements Weight {
    Searcher searcher;
    Weight subQueryWeight;
    Weight boostWeight; // optional
    boolean qStrict;

    public CustomWeight(Searcher searcher) throws IOException {
      this.searcher = searcher;
      this.subQueryWeight = subQuery.weight(searcher); 
      if (boostQuery!=null) {
        this.boostWeight = boostQuery.createWeight(searcher);
      }
      this.qStrict = strict;
    }

    /*(non-Javadoc) @see org.apache.lucene.search.Weight#getQuery() */
    public Query getQuery() {
      return CustomBoostQuery.this;
    }

    /*(non-Javadoc) @see org.apache.lucene.search.Weight#getValue() */
    public float getValue() {
      return getBoost();
    }

    /*(non-Javadoc) @see org.apache.lucene.search.Weight#sumOfSquaredWeights() */
    public float sumOfSquaredWeights() throws IOException {
      float sum = subQueryWeight.sumOfSquaredWeights();
      if (boostWeight!=null) {
        if (qStrict) {
          boostWeight.sumOfSquaredWeights(); // do not include ValueSource part in the query normalization
        } else {
          sum += boostWeight.sumOfSquaredWeights();
        }
      }
      sum *= getBoost() * getBoost(); // boost each sub-weight
      return sum ;
    }

    /*(non-Javadoc) @see org.apache.lucene.search.Weight#normalize(float) */
    public void normalize(float norm) {
      norm *= getBoost(); // incorporate boost
      subQueryWeight.normalize(norm);
      if (boostWeight!=null) {
        if (qStrict) {
          boostWeight.normalize(1); // do not normalize the ValueSource part
        } else {
          boostWeight.normalize(norm);
        }
      }
    }

    /*(non-Javadoc) @see org.apache.lucene.search.Weight#scorer(org.apache.lucene.index.IndexReader) */
    public Scorer scorer(IndexReader reader) throws IOException {
      Scorer subQueryScorer = subQueryWeight.scorer(reader);
      Scorer boostScorer = (boostWeight==null ? null : boostWeight.scorer(reader));
      return new CustomScorer(getSimilarity(searcher), reader, this, subQueryScorer, boostScorer);
    }

    /*(non-Javadoc) @see org.apache.lucene.search.Weight#explain(org.apache.lucene.index.IndexReader, int) */
    public Explanation explain(IndexReader reader, int doc) throws IOException {
      return scorer(reader).explain(doc);
    }
  }


  //=========================== S C O R E R ============================
  
  /**
   * A scorer that applies a (callback) function on scores of the subQuery.
   */
  private class CustomScorer extends Scorer {
    private final CustomWeight weight;
    private final float qWeight;
    private Scorer subQueryScorer;
    private Scorer boostScorer; // optional
    private IndexReader reader;

    // constructor
    private CustomScorer(Similarity similarity, IndexReader reader, CustomWeight w,
        Scorer subQueryScorer, Scorer boostScorer) throws IOException {
      super(similarity);
      this.weight = w;
      this.qWeight = w.getValue();
      this.subQueryScorer = subQueryScorer;
      this.boostScorer = boostScorer;
      this.reader = reader;
    }

    /*(non-Javadoc) @see org.apache.lucene.search.Scorer#next() */
    public boolean next() throws IOException {
      boolean hasNext = subQueryScorer.next();
      if (boostScorer!=null && hasNext) {
        boostScorer.skipTo(subQueryScorer.doc());
      }
      return hasNext;
    }

    /*(non-Javadoc) @see org.apache.lucene.search.Scorer#doc() */
    public int doc() {
      return subQueryScorer.doc();
    }

    /*(non-Javadoc) @see org.apache.lucene.search.Scorer#score() */
    public float score() throws IOException {
      float boostScore = (boostScorer==null ? 1 : boostScorer.score());
      return qWeight * customScore(subQueryScorer.doc(), subQueryScorer.score(), boostScore);
    }

    /*(non-Javadoc) @see org.apache.lucene.search.Scorer#skipTo(int) */
    public boolean skipTo(int target) throws IOException {
      boolean hasNext = subQueryScorer.skipTo(target);
      if (boostScorer!=null && hasNext) {
        boostScorer.skipTo(subQueryScorer.doc());
      }
      return hasNext;
    }

    /*(non-Javadoc) @see org.apache.lucene.search.Scorer#explain(int) */
    public Explanation explain(int doc) throws IOException {
      Explanation subQueryExpl = weight.subQueryWeight.explain(reader,doc);
      if (!subQueryExpl.isMatch()) {
        return subQueryExpl;
      }
      // match
      Explanation boostExpl = boostScorer==null ? null : boostScorer.explain(doc);
      Explanation customExp = customExplain(doc,subQueryExpl,boostExpl);
      float sc = qWeight * customExp.getValue();
      Explanation res = new ComplexExplanation(
        true, sc, CustomBoostQuery.this.toString() + ", product of:");
      res.addDetail(customExp);
      res.addDetail(new Explanation(qWeight, "queryBoost")); // actually using the q boost as q weight (== weight value)
      return res;
    }
  }

  /*(non-Javadoc) @see org.apache.lucene.search.Query#createWeight(org.apache.lucene.search.Searcher) */
  protected Weight createWeight(Searcher searcher) throws IOException {
    return new CustomWeight(searcher);
  }

  /**
   * Checks if this is strict custom scoring.
   * In strict custom scoring, the ValueSource part of does not participate in weight normalization.
   * This may be useful when one wants full control over how scores are modified, and does 
   * not care about normalizing by the ValueSource part.
   * One particular case where this is useful if for testing this query.   
   * <P>
   * Note: only has effect when the ValueSource part is not null.
   */
  public boolean isStrict() {
    return strict;
  }

  /**
   * Set the strict mode of this query. 
   * @param strict The strict mode to set.
   * @see #isStrict()
   */
  public void setStrict(boolean strict) {
    this.strict = strict;
  }

  /**
   * A short name of this query, used in {@link #toString(String)}.
   */
  public String name() {
    return "custom";
  }

}
