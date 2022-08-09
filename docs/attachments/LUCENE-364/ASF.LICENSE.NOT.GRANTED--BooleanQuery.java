package org.apache.lucene.search;

/**
 * Copyright 2004-2005 The Apache Software Foundation
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
import java.util.Arrays;

import org.apache.lucene.index.IndexReader;

/** A Query that matches documents matching boolean combinations of other
  * queries, typically {@link TermQuery}s or {@link PhraseQuery}s.
  */
public class BooleanQuery extends Query {
  
  /** The maximum number of clauses permitted. Default value is 1024.
   * Use the <code>org.apache.lucene.maxClauseCount</code> system property
   * to override.
   * <p>TermQuery clauses are generated from for example prefix queries and
   * fuzzy queries. Each TermQuery needs some buffer space during search,
   * so this parameter indirectly controls the maximum buffer requirements for
   * query search.
   * <p>When this parameter becomes a bottleneck for a Query one can use a
   * Filter. For example instead of a {@link RangeQuery} one can use a
   * {@link RangeFilter}.
   * <p>Normally the buffers are allocated by the JVM. When using for example
   * {@link org.apache.lucene.store.MMapDirectory} the buffering is left to
   * the operating system.
   */
  public static int maxClauseCount =
    Integer.parseInt(System.getProperty("org.apache.lucene.maxClauseCount",
      "1024"));

  /** Thrown when an attempt is made to add more than {@link
   * #getMaxClauseCount()} clauses. This typically happens if
   * a PrefixQuery, FuzzyQuery, WildcardQuery, or RangeQuery 
   * is expanded to many terms during search. 
   */
  public static class TooManyClauses extends RuntimeException {}

  /** Return the maximum number of clauses permitted, 1024 by default.
   * Attempts to add more than the permitted number of clauses cause {@link
   * TooManyClauses} to be thrown.
   * @see #maxClauseCount
   */
  public static int getMaxClauseCount() { return maxClauseCount; }

  /** Set the maximum number of clauses permitted. */
  public static void setMaxClauseCount(int maxClauseCount) {
    BooleanQuery.maxClauseCount = maxClauseCount;
  }

  private Vector clauses = new Vector();
  private boolean disableCoord;

  /** Constructs an empty boolean query. */
  public BooleanQuery() {}

  /** Constructs an empty boolean query.
   *
   * {@link Similarity#coord(int,int)} may be disabled in scoring, as
   * appropriate. For example, this score factor does not make sense for most
   * automatically generated queries, like {@link WildcardQuery} and {@link
   * FuzzyQuery}.
   *
   * @param disableCoord disables {@link Similarity#coord(int,int)} in scoring.
   */
  public BooleanQuery(boolean disableCoord) {
    this.disableCoord = disableCoord;
  }

  /** Returns true iff {@link Similarity#coord(int,int)} is disabled in
   * scoring for this query instance.
   * @see #BooleanQuery(boolean)
   */
  public boolean isCoordDisabled() { return disableCoord; }

  // Implement coord disabling.
  // Inherit javadoc.
  public Similarity getSimilarity(Searcher searcher) {
    Similarity result = super.getSimilarity(searcher);
    if (disableCoord) {                           // disable coord as requested
      result = new SimilarityDelegator(result) {
          public float coord(int overlap, int maxOverlap) {
            return 1.0f;
          }
        };
    }
    return result;
  }

  /** Adds a clause to a boolean query.  Clauses may be:
   * <ul>
   * <li><code>required</code> which means that documents which <i>do not</i>
   * match this sub-query will <i>not</i> match the boolean query;
   * <li><code>prohibited</code> which means that documents which <i>do</i>
   * match this sub-query will <i>not</i> match the boolean query; or
   * <li>neither, in which case matched documents are neither prohibited from
   * nor required to match the sub-query. However, a document must match at
   * least 1 sub-query to match the boolean query.
   * </ul>
   * It is an error to specify a clause as both <code>required</code> and
   * <code>prohibited</code>.
   *
   * @deprecated use {@link #add(Query, BooleanClause.Occur)} instead:
   * <ul>
   *  <li>For add(query, true, false) use add(query, BooleanClause.Occur.MUST)
   *  <li>For add(query, false, false) use add(query, BooleanClause.Occur.SHOULD)
   *  <li>For add(query, false, true) use add(query, BooleanClause.Occur.MUST_NOT)
   * </ul>
   */
  public void add(Query query, boolean required, boolean prohibited) {
    add(new BooleanClause(query, required, prohibited));
  }

  /** Adds a clause to a boolean query.
   *
   * @throws TooManyClauses if the new number of clauses exceeds the maximum clause number
   * @see #getMaxClauseCount()
   */
  public void add(Query query, BooleanClause.Occur occur) {
    add(new BooleanClause(query, occur));
  }

  /** Adds a clause to a boolean query.
   * @throws TooManyClauses if the new number of clauses exceeds the maximum clause number
   * @see #getMaxClauseCount()
   */
  public void add(BooleanClause clause) {
    if (clauses.size() >= maxClauseCount)
      throw new TooManyClauses();

    clauses.addElement(clause);
  }

  /** Returns the set of clauses in this query. */
  public BooleanClause[] getClauses() {
    return (BooleanClause[])clauses.toArray(new BooleanClause[0]);
  }

  private class BooleanWeight implements Weight {
    protected Searcher searcher;
    protected Vector weights = new Vector();

    public BooleanWeight(Searcher searcher) {
      this.searcher = searcher;
      for (int i = 0 ; i < clauses.size(); i++) {
        BooleanClause c = (BooleanClause)clauses.elementAt(i);
        weights.add(c.getQuery().createWeight(searcher));
      }
    }

    public Query getQuery() { return BooleanQuery.this; }
    public float getValue() { return getBoost(); }

    public float sumOfSquaredWeights() throws IOException {
      float sum = 0.0f;
      for (int i = 0 ; i < weights.size(); i++) {
        BooleanClause c = (BooleanClause)clauses.elementAt(i);
        Weight w = (Weight)weights.elementAt(i);
        if (!c.isProhibited())
          sum += w.sumOfSquaredWeights();         // sum sub weights
      }

      sum *= getBoost() * getBoost();             // boost each sub-weight

      return sum ;
    }


    public void normalize(float norm) {
      norm *= getBoost();                         // incorporate boost
      for (int i = 0 ; i < weights.size(); i++) {
        BooleanClause c = (BooleanClause)clauses.elementAt(i);
        Weight w = (Weight)weights.elementAt(i);
        if (!c.isProhibited())
          w.normalize(norm);
      }
    }

    /** @return A good old 1.4 Scorer */
    public Scorer scorer(IndexReader reader) throws IOException {
      // First see if the (faster) ConjunctionScorer will work.  This can be
      // used when all clauses are required.  Also, at this point a
      // BooleanScorer cannot be embedded in a ConjunctionScorer, as the hits
      // from a BooleanScorer are not always sorted by document number (sigh)
      // and hence BooleanScorer cannot implement skipTo() correctly, which is
      // required by ConjunctionScorer.
      boolean allRequired = true;
      boolean noneBoolean = true;
      for (int i = 0 ; i < weights.size(); i++) {
        BooleanClause c = (BooleanClause)clauses.elementAt(i);
        if (!c.isRequired())
          allRequired = false;
        if (c.getQuery() instanceof BooleanQuery)
          noneBoolean = false;
      }

      if (allRequired && noneBoolean) {           // ConjunctionScorer is okay
        ConjunctionScorer result =
          new ConjunctionScorer(getSimilarity(searcher));
        for (int i = 0 ; i < weights.size(); i++) {
          Weight w = (Weight)weights.elementAt(i);
          Scorer subScorer = w.scorer(reader);
          if (subScorer == null)
            return null;
          result.add(subScorer);
        }
        return result;
      }

      // Use good-old BooleanScorer instead.
      BooleanScorer result = new BooleanScorer(getSimilarity(searcher));

      for (int i = 0 ; i < weights.size(); i++) {
        BooleanClause c = (BooleanClause)clauses.elementAt(i);
        Weight w = (Weight)weights.elementAt(i);
        Scorer subScorer = w.scorer(reader);
        if (subScorer != null)
          result.add(subScorer, c.isRequired(), c.isProhibited());
        else if (c.isRequired())
          return null;
      }
      
      if (allowSkipTo14) {
        result.allowSkipTo();
      }

      return result;
    }

    public Explanation explain(IndexReader reader, int doc)
      throws IOException {
      Explanation sumExpl = new Explanation();
      sumExpl.setDescription("sum of:");
      int coord = 0;
      int maxCoord = 0;
      float sum = 0.0f;
      for (int i = 0 ; i < weights.size(); i++) {
        BooleanClause c = (BooleanClause)clauses.elementAt(i);
        Weight w = (Weight)weights.elementAt(i);
        Explanation e = w.explain(reader, doc);
        if (!c.isProhibited()) maxCoord++;
        if (e.getValue() > 0) {
          if (!c.isProhibited()) {
            sumExpl.addDetail(e);
            sum += e.getValue();
            coord++;
          } else {
            return new Explanation(0.0f, "match prohibited");
          }
        } else if (c.isRequired()) {
          return new Explanation(0.0f, "match required");
        }
      }
      sumExpl.setValue(sum);

      if (coord == 1)                               // only one clause matched
        sumExpl = sumExpl.getDetails()[0];          // eliminate wrapper

      float coordFactor = getSimilarity(searcher).coord(coord, maxCoord);
      if (coordFactor == 1.0f)                      // coord is no-op
        return sumExpl;                             // eliminate wrapper
      else {
        Explanation result = new Explanation();
        result.setDescription("product of:");
        result.addDetail(sumExpl);
        result.addDetail(new Explanation(coordFactor,
                                         "coord("+coord+"/"+maxCoord+")"));
        result.setValue(sum*coordFactor);
        return result;
      }
    }
  }

  private class BooleanWeight2 extends BooleanWeight {
    /* Merge into BooleanWeight in case the 1.4 BooleanScorer is dropped */
    public BooleanWeight2(Searcher searcher) {  super(searcher); }

    /** @return A Scorer that uses and provides skipTo(),
     *          and scores documents in document number order.
     */
    public Scorer scorer(IndexReader reader) throws IOException {
      BooleanScorer2 result = disableCoord
                          ? new BooleanScorer2()
                          : new BooleanScorer2Coord(getSimilarity(searcher));

      for (int i = 0 ; i < weights.size(); i++) {
        BooleanClause c = (BooleanClause)clauses.elementAt(i);
        Weight w = (Weight)weights.elementAt(i);
        Scorer subScorer = w.scorer(reader);
        if (subScorer != null)
          result.add(subScorer, c.getOccur());
        else if (c.isRequired())
          return null;
      }
      return result.getDelegatedScorer(); // Avoid indirection when possible.
    }
    
    public Explanation explain(IndexReader reader, int doc) throws IOException {
      return scorer(reader).explain(doc);
    }
  }

  /** Indicates whether to use good old 1.4 BooleanScorer. */
  private static boolean useScorer14 = false;
  /** Indicates whether the 1.4 BooleanScorer should allow skipTo() */
  private static boolean allowSkipTo14 = false;
  
  
  /** Allows the use of the 1.4 BooleanScorer.
   * This is useful for comparing various BooleanScorer implementations.
   * <br>This is deprecated because it may be removed after
   * a definite choice/implementation is made for the BooleanScorer.
   * @param use14  True iff the 1.4 BooleanScorer must be used by any BooleanQuery.
   *               Otherwise a BooleanScorer2 or a BooleanScorer2Coord will be used.
   * @param allowSkipTo True when any 1.4 BooleanScorer should allow the use of
   *                    the skipTo() method.
   * @deprecated
   */
  public static void setUseScorer14(boolean use14, boolean allowSkipTo) {
    useScorer14 = use14;
    allowSkipTo14 = allowSkipTo;
  }

  /** Returns the last value passed to the use14 argument to setUseScorer14.
   * @see #setUseScorer14(boolean, boolean)  
   * @deprecated
   */
  public static boolean getUseScorer14() {
    return useScorer14;
  }
  
  /** Returns the last value passed to the allowSkipTo argument to setUseScorer14.
   * @see #setUseScorer14(boolean, boolean)  
   * @deprecated
   */
  public static boolean getAllowSkipTo14() {
    return allowSkipTo14;
  }
  
  
  protected Weight createWeight(Searcher searcher) {
    return getUseScorer14() ? (Weight) new BooleanWeight(searcher)
                            : (Weight) new BooleanWeight2(searcher);
  }

  public Query rewrite(IndexReader reader) throws IOException {
    if (clauses.size() == 1) {                    // optimize 1-clause queries
      BooleanClause c = (BooleanClause)clauses.elementAt(0);
      if (!c.isProhibited()) {			  // just return clause

        Query query = c.getQuery().rewrite(reader);    // rewrite first

        if (getBoost() != 1.0f) {                 // incorporate boost
          if (query == c.getQuery())                   // if rewrite was no-op
            query = (Query)query.clone();         // then clone before boost
          query.setBoost(getBoost() * query.getBoost());
        }

        return query;
      }
    }

    BooleanQuery clone = null;                    // recursively rewrite
    for (int i = 0 ; i < clauses.size(); i++) {
      BooleanClause c = (BooleanClause)clauses.elementAt(i);
      Query query = c.getQuery().rewrite(reader);
      if (query != c.getQuery()) {                     // clause rewrote: must clone
        if (clone == null)
          clone = (BooleanQuery)this.clone();
        clone.clauses.setElementAt
          (new BooleanClause(query, c.getOccur()), i);
      }
    }
    if (clone != null) {
      return clone;                               // some clauses rewrote
    } else
      return this;                                // no clauses rewrote
  }


  public Object clone() {
    BooleanQuery clone = (BooleanQuery)super.clone();
    clone.clauses = (Vector)this.clauses.clone();
    return clone;
  }

  /** Prints a user-readable version of this query. */
  public String toString(String field) {
    StringBuffer buffer = new StringBuffer();
    if (getBoost() != 1.0) {
      buffer.append("(");
    }

    for (int i = 0 ; i < clauses.size(); i++) {
      BooleanClause c = (BooleanClause)clauses.elementAt(i);
      if (c.isProhibited())
	buffer.append("-");
      else if (c.isRequired())
	buffer.append("+");

      Query subQuery = c.getQuery();
      if (subQuery instanceof BooleanQuery) {	  // wrap sub-bools in parens
	buffer.append("(");
	buffer.append(c.getQuery().toString(field));
	buffer.append(")");
      } else
	buffer.append(c.getQuery().toString(field));

      if (i != clauses.size()-1)
	buffer.append(" ");
    }

    if (getBoost() != 1.0) {
      buffer.append(")^");
      buffer.append(getBoost());
    }

    return buffer.toString();
  }

  /** Returns true iff <code>o</code> is equal to this. */
  public boolean equals(Object o) {
    if (!(o instanceof BooleanQuery))
      return false;
    BooleanQuery other = (BooleanQuery)o;
    return (this.getBoost() == other.getBoost())
      &&  Arrays.equals(this.getClauses(), other.getClauses());
  }

  /** Returns a hash code value for this object.*/
  public int hashCode() {
    return Float.floatToIntBits(getBoost()) ^ clauses.hashCode();
  }

}
