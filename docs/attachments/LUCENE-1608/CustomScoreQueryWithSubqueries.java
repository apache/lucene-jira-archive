package org.apache.lucene.search.function;

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
import java.util.Arrays;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ComplexExplanation;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.function.CustomScoreQuery;
import org.apache.lucene.search.function.ValueSourceQuery;
import org.apache.lucene.util.ToStringUtils;

/**
 * Query that sets document score as a programmatic function of several (sub) scores:
 * <ol>
 *    <li>the score of its mainQuery (any query). This query is used in order
 *    		to retrieve the matching documents</li>
 *    <li>(optional) the score of its Query instances</li>
 *    <li>(optional) the score of its ValueSourceQuery (or queries).
 *        For most simple/convenient use cases this query is likely to be a 
 *        {@link org.apache.lucene.search.function.FieldScoreQuery FieldScoreQuery}</li>
 * </ol>
 * Subclasses can modify the computation by overriding one of the following methods pairs
 * <ul>
 * <li>{@link #customScore(int, float)} and {@link #customExplain(int, Explanation)}</li>
 * <li>{@link #customScoreNSubQueries(int, float, float[])} and {@link #customExplainNSubQueries(int, Explanation, Explanation[])}</li>
 * <li>{@link #customScoreNSubQueriesNValueSources(int, float, float[], float[]) and {@link #customExplainNSubQueriesNValueSources(int, Explanation, Explanation[], Explanation[])}</li>
 * <li>{@link #customScoreNSubQueriesOneValueSource(int, float, float[], float)} and {@link #customExplainNSubQueriesOneValueSource(int, Explanation, Explanation[], Explanation)}</li>
 * <li>{@link #customScoreNValueSources(int, float, float[])} and {@link #customExplainNValueSources(int, Explanation, Explanation[])}</li>
 * <li>{@link #customScoreOneSubQuery(int, float, float)} and {@link #customExplainOneSubQuery(int, Explanation, Explanation)}</li>
 * <li>{@link #customScoreOneSubQueryNValueSources(int, float, float, float[])} and {@link #customExplainOneSubQueryNValueSources(int, Explanation, Explanation, Explanation[])}</li>
 * <li>{@link #customScoreOneSubQueryOneValueSource(int, float, float, float)} and {@link #customExplainOneSubQueryOneValueSource(int, Explanation, Explanation, Explanation)}</li>
 * <li>{@link #customScoreNValueSources(int, float, float[])} and {@link #customExplainNValueSources(int, Explanation, Explanation[])}</li>
 * </ul>
 * 
 * This class is based on {@link CustomScoreQuery} and is designed for using with Lucene 2.9.1. This is the reason why the class
 * CustomScoreProvider (that exists starting in Lucene 2.9.2) is not used
 * 
 * @author fernandowasylyszyn
 *  
 * <p><font color="#FF0000">
 * WARNING: The status of the <b>search.function</b> package is experimental. 
 * The APIs introduced here might change in the future and will not be 
 * supported anymore in such a case.</font>
 */
public class CustomScoreQueryWithSubqueries extends Query {

  /**
   * Empty array of {@link Query} in order to be used when no subquery 
   * is provided for programatic score calculation
   */
  private static final Query[] EMPTY_SUBQUERIES = new Query[0];
	
  /**
   * Empty array of {@link ValueSourceQuery} in order to be used when no valSrcQuery 
   * is provided for programatic score calculation
   */
  private static final ValueSourceQuery[] EMPTY_VALUE_SOURCE_QUERIES = new ValueSourceQuery[0];
    
  /**
   * Main query (whose scored is being customed).
   * Protected in order to be used directly in subclasses if {@link #toString(String)}
   * method is overriden 
   */
  protected Query mainQuery;
  
  /**
   * Subqueries (used for score customization). Never null (empty array if there are no subqueries)
   * Protected in order to be used directly in subclasses if {@link #toString(String)}
   * method is overriden
   */
  protected Query[] subQueries;
  
  /**
   * ValurSourceQueries (used for score customization). Never <code>null</code> (empty array if there are no valSrcQueries).
   * Protected in order to be used directly in subclasses if {@link #toString(String)}
   * method is overriden 
   */
  protected ValueSourceQuery[] valSrcQueries;
  
  /**
   * If <code>true</code>, valueSource part of query does not take part in weights normalization.
   * Protected in order to be used directly in subclasses if {@link #toString(String)}
   * method is overriden
   */
  protected boolean strict = false;   
  
  /**
   * Create a CustomScoreQuery over input mainQuery.
   * @param mainQuery the query whose scored is being customed. Must not be null. 
   * @throws IllegalArgumentException if the query is <code>null</code>
   */
  public CustomScoreQueryWithSubqueries(Query mainQuery) {
    this(mainQuery, EMPTY_SUBQUERIES, EMPTY_VALUE_SOURCE_QUERIES);
  }

  /**
   * Create a CustomScoreQuery over input mainQuery and a {@link ValueSourceQuery}.
   * @param mainQuery the sub query whose score is being customized. Must not be null.
   * @param valSrcQuery a value source query whose scores are used in the custom score
   * computation. For most simple/convenient use case this would be a 
   * {@link org.apache.lucene.search.function.FieldScoreQuery FieldScoreQuery}.
   * This parameter is optional - it can be null.
   * @throws IllegalArgumentException if the query is <code>null</code>
   */
  public CustomScoreQueryWithSubqueries(Query subQuery, ValueSourceQuery valSrcQuery) {
	  this(subQuery, EMPTY_SUBQUERIES, valSrcQuery!=null ? // don't want an array that contains a single null.. 
        new ValueSourceQuery[] {valSrcQuery} : new ValueSourceQuery[0]);
  }

  /**
   * Create a CustomScoreQuery over input mainQuery and a {@link ValueSourceQuery}.
   * @param mainQuery the sub query whose score is being customized. Must not be null.
   * @param valSrcQueries value source queries whose scores are used in the custom score
   * computation. For most simple/convenient use case these would be 
   * {@link org.apache.lucene.search.function.FieldScoreQuery FieldScoreQueries}.
   * This parameter is optional - it can be null or even an empty array.
   * @throws IllegalArgumentException if the query is <code>null</code>
   */
  public CustomScoreQueryWithSubqueries(Query mainQuery, ValueSourceQuery valSrcQueries[]) {
	  this(mainQuery,EMPTY_SUBQUERIES,valSrcQueries!=null? valSrcQueries : new ValueSourceQuery[0]);
  }
  
  /**
   * Create a CustomScoreQuery over input mainQuery and a {@link ValueSourceQuery}.
   * @param mainQuery the sub query whose score is being customized. Must not be null.
   * @param subQuery query whose score is used in the custom score computation.
   * This parameter is optional - it can be null.
   * @param valSrcQueries value source queries whose scores are used in the custom score
   * computation. For most simple/convenient use case these would be 
   * {@link org.apache.lucene.search.function.FieldScoreQuery FieldScoreQueries}.
   * This parameter is optional - it can be null or even an empty array.
   * @throws IllegalArgumentException if the query is <code>null</code>
   */
  public CustomScoreQueryWithSubqueries(Query mainQuery, Query subQuery, ValueSourceQuery valSrcQuery) {
	  this(mainQuery,
			  subQuery!=null ? new Query[] {subQuery} : new Query[0],
					  valSrcQuery!=null ? new ValueSourceQuery[] {valSrcQuery} : new ValueSourceQuery[0]);
  }
  
  
  /**
   * Create a CustomScoreQuery over input mainQuery and a {@link ValueSourceQuery}.
   * @param mainQuery the sub query whose score is being customized. Must not be null.
   * @param subQuery query whose score is used in the custom score computation.
   * This parameter is optional - it can be null.
   * @param valSrcQueries value source queries whose scores are used in the custom score
   * computation. For most simple/convenient use case these would be 
   * {@link org.apache.lucene.search.function.FieldScoreQuery FieldScoreQueries}.
   * This parameter is optional - it can be null or even an empty array.
   * @throws IllegalArgumentException if the mainQuery is <code>null</code>
   */
  public CustomScoreQueryWithSubqueries(Query mainQuery, Query subQuery, ValueSourceQuery valSrcQueries[]) {
	  this(mainQuery,subQuery!=null ? new Query[] {subQuery} : new Query[0],valSrcQueries);
  }
  
  /**
   * Create a CustomScoreQuery over input mainQuery and a {@link ValueSourceQuery}.
   * @param mainQuery the sub query whose score is being customized. Must not be null.
   * @param subQueries queries whose scores are used in the custom score computation.
   * This parameter is optional - it can be null or even an empty array.
   * @param valSrcQueries value source queries whose scores are used in the custom score
   * computation. For most simple/convenient use case these would be 
   * {@link org.apache.lucene.search.function.FieldScoreQuery FieldScoreQueries}.
   * This parameter is optional - it can be null or even an empty array.
   * @throws IllegalArgumentException if the mainQuery is <code>null</code>
   */
  public CustomScoreQueryWithSubqueries(Query mainQuery, Query[] subQueries, ValueSourceQuery valSrcQuery) {
	 if (mainQuery == null) throw new IllegalArgumentException("<mainQuery> must not be null!");
	 this.mainQuery = mainQuery;
	 this.subQueries = subQueries!=null ? subQueries : new Query[0];
	 this.valSrcQueries = valSrcQuery!=null ? new ValueSourceQuery[] {valSrcQuery} : new ValueSourceQuery[0];
  }
  
  
  /**
   * Create a CustomScoreQuery over input mainQuery and a {@link ValueSourceQuery}.
   * @param mainQuery the sub query whose score is being customized. Must not be null.
   * @param subQueries queries whose scores are used in the custom score computation.
   * This parameter is optional - it can be null or even an empty array.
   * @param valSrcQueries value source queries whose scores are used in the custom score
   * computation. For most simple/convenient use case these would be 
   * {@link org.apache.lucene.search.function.FieldScoreQuery FieldScoreQueries}.
   * This parameter is optional - it can be null or even an empty array.
   * @throws IllegalArgumentException if the mainQuery is <code>null</code>
   */
  public CustomScoreQueryWithSubqueries(Query mainQuery, Query[] subQueries, ValueSourceQuery[] valSrcQueries) {
	 if (mainQuery == null) throw new IllegalArgumentException("<mainQuery> must not be null!");
	 this.mainQuery = mainQuery;
	 this.subQueries = subQueries!=null ? subQueries : new Query[0];
     this.valSrcQueries = valSrcQueries!=null ? valSrcQueries : new ValueSourceQuery[0];
  }

  /*(non-Javadoc) @see org.apache.lucene.search.Query#rewrite(org.apache.lucene.index.IndexReader) */
  public Query rewrite(IndexReader reader) throws IOException {
    CustomScoreQueryWithSubqueries clone = null;
    
    final Query sq = mainQuery.rewrite(reader);
    if (sq != mainQuery) {
      clone = (CustomScoreQueryWithSubqueries) clone();
      clone.mainQuery = sq;
    }
    
    for (int i = 0; i < subQueries.length; i++) {
    	final Query subQuery = subQueries[i].rewrite(reader);
    	if (subQuery != subQueries[i]) {
    		if (clone == null) clone = (CustomScoreQueryWithSubqueries) clone();
    		clone.subQueries[i] = subQuery;
    	}
    }

    for(int i = 0; i < valSrcQueries.length; i++) {
      final ValueSourceQuery v = (ValueSourceQuery) valSrcQueries[i].rewrite(reader);
      if (v != valSrcQueries[i]) {
        if (clone == null) clone = (CustomScoreQueryWithSubqueries) clone();
        clone.valSrcQueries[i] = v;
      }
    }
    
    return (clone == null) ? this : clone;
  }

  /*(non-Javadoc) @see org.apache.lucene.search.Query#extractTerms(java.util.Set) */
  public void extractTerms(Set terms) {
    mainQuery.extractTerms(terms);
    for(int i = 0; i < subQueries.length; i++) {
    	subQueries[i].extractTerms(terms);
    }
    for(int i = 0; i < valSrcQueries.length; i++) {
      valSrcQueries[i].extractTerms(terms);
    }
  }

  /*(non-Javadoc) @see org.apache.lucene.search.Query#clone() */
  public Object clone() {
    CustomScoreQueryWithSubqueries clone = (CustomScoreQueryWithSubqueries)super.clone();
    clone.mainQuery = (Query) mainQuery.clone();
    clone.subQueries = new Query[subQueries.length];
    for(int i = 0; i < subQueries.length; i++) {
        clone.subQueries[i] = (Query) subQueries[i].clone();
      }
    clone.valSrcQueries = new ValueSourceQuery[valSrcQueries.length];
    for(int i = 0; i < valSrcQueries.length; i++) {
      clone.valSrcQueries[i] = (ValueSourceQuery) valSrcQueries[i].clone();
    }
    return clone;
  }

  /* (non-Javadoc) @see org.apache.lucene.search.Query#toString(java.lang.String) */
  public String toString(String field) {
    StringBuffer sb = new StringBuffer(name()).append("(");
    sb.append(mainQuery.toString(field));
    for(int i = 0; i < subQueries.length; i++) {
        sb.append(", ").append(subQueries[i].toString(field));
    }
    for(int i = 0; i < valSrcQueries.length; i++) {
      sb.append(", ").append(valSrcQueries[i].toString(field));
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
    CustomScoreQueryWithSubqueries other = (CustomScoreQueryWithSubqueries)o;
    if (this.getBoost() != other.getBoost() ||
        !this.mainQuery.equals(other.mainQuery) ||
        this.strict != other.strict ||
        this.valSrcQueries.length != other.valSrcQueries.length ||
        this.subQueries.length != other.subQueries.length) {
      return false;
    }
    for (int i=0; i<subQueries.length; i++) { //TODO simplify with Arrays.deepEquals() once moving to Java 1.5
        if (!subQueries[i].equals(other.subQueries[i])) {
          return false;
        }
    }
    for (int i=0; i<valSrcQueries.length; i++) { //TODO simplify with Arrays.deepEquals() once moving to Java 1.5
      if (!valSrcQueries[i].equals(other.valSrcQueries[i])) {
        return false;
      }
    }
    return true;
  }

  /** Returns a hash code value for this object. */
  public int hashCode() {
    int valSrcHash = 0;
    for (int i=0; i<subQueries.length; i++) { //TODO simplify with Arrays.deepHashcode() once moving to Java 1.5
        valSrcHash += subQueries[i].hashCode();
      }
    for (int i=0; i<valSrcQueries.length; i++) { //TODO simplify with Arrays.deepHashcode() once moving to Java 1.5
      valSrcHash += valSrcQueries[i].hashCode();
    }
    return (getClass().hashCode() + mainQuery.hashCode() + valSrcHash) ^
      Float.floatToIntBits(getBoost()) ^ (strict ? 1234 : 4321);
  }  
  
  /**
   * @param array float array which members will be multiplied
   * @return the product of all the members of the array
   */
  protected static float productOfArrayElements(float[] array) {
	  float product = 1;
	  for (float element : array) {
		  product *= element;
	  }
	  return product;
  }
  
  /**
   * @param array explanation array which values will be multiplied
   * @return the product of all the values of the explanations in the array
   */
  protected static float productOfExplanationsValues(Explanation[] expls) {
	  float product = 1;
	  for (Explanation expl : expls) {
		  product *= expl.getValue();
	  }
	  return product;
  }
  
  /**
   * Computes a custom score using the mainQuery score.</br>
   * This method will be called if the constructor 
   * {@link CustomScoreQueryWithSubqueries#CustomScoreQueryWithSubqueries(Query)} was used.</br>
   * If you want to customize the score using the mainQuery only, override this method and
   * {@link CustomScoreQueryWithSubqueries#customExplain(int, Explanation)}
   * @param doc doc id
   * @param mainQueryScore the score of the main query
   * @return the custom score
   */
  protected float customScore(int doc, float mainQueryScore) {
	  return mainQueryScore;
  }
      
  /**
   * Compute a custom score using the mainQuery score and a single subQuery score.</br>
   * If you want to customize the score using the mainQuery, a single subQuery and a single ValueSourceQuery score
   * override this method and
   * {@link CustomScoreQueryWithSubqueries#customExplainOneSubQuery(int, Explanation, Explanation)}
   * @param doc doc id
   * @param mainQueryScore the score of the main query
   * @param subQueryScore the score of a single subQuery
   * @return the custom score
   */
  protected float customScoreOneSubQuery(int doc, float mainQueryScore, float subQueryScore) {
    return mainQueryScore * subQueryScore;
  }
  
  /**
   * Compute a custom score using the mainQuery score and a single ValueSourceQuery score.</br>
   * If you want to customize the score using the mainQuery score and a single ValueSourceQuery score
   * override this method and
   * {@link CustomScoreQueryWithSubqueries#customExplainOneValueSource(int, Explanation, Explanation)}
   * @param doc doc id
   * @param mainQueryScore the score of the main query
   * @param valSrcScore the score of a single ValueSourceQuery
   * @return the custom score
   */
  protected float customScoreOneValueSource(int doc, float mainQueryScore, float valSrcScore) {
    return mainQueryScore * valSrcScore;
  }
  
  /**
   * Compute a custom score using the mainQuery score and multiple subQueries scores.</br>
   * If you want to customize the score using the mainQuery score and multiple subQueries scores
   * override this method and
   * {@link CustomScoreQueryWithSubqueries#customExplainNSubQueries(int, Explanation, Explanation[])}
   * @param doc doc id
   * @param mainQueryScore the score of the main query
   * @param subQueriesScores the scores of multiple subQueries
   * @return the custom score
   */
  protected float customScoreNSubQueries(int doc, float mainQueryScore, float[] subQueriesScores) {
     return mainQueryScore * productOfArrayElements(subQueriesScores);
  }
  
  /**
   * Compute a custom score using the mainQuery score and multiple ValueSourceQuery scores.</br>
   * If you want to customize the score using the mainQuery score and multiple ValueSourceQuery scores
   * override this method and
   * {@link CustomScoreQueryWithSubqueries#customExplainNValueSources(int, Explanation, Explanation[])}
   * @param doc doc id
   * @param mainQueryScore the score of the main query
   * @param valSrcScores the scores of multiple ValueSourceQuery
   * @return the custom score
   */
  protected float customScoreNValueSources(int doc, float mainQueryScore, float[] valSrcScores) {
     return mainQueryScore * productOfArrayElements(valSrcScores);
  }
  
  /**
   * Computes a custom score using the mainQuery score, a single subQuery score and a single ValueSourceQuery score.</br>
   * If you want to customize the score using the mainQuery score, a single subQuery score and a single ValueSourceQuery score
   * {@link CustomScoreQueryWithSubqueries#customExplainOneSubQueryOneValueSource(int, Explanation, Explanation, Explanation)}
   * @param doc doc id
   * @param mainQueryScore the score of the main query
   * @param subQueryScore the score of a single subQuery
   * @param valSrcScore the score of a single ValueSourceQuery
   * @return the custom score
   */
  protected float customScoreOneSubQueryOneValueSource(int doc, float mainQueryScore, float subQueryScore, float valSrcScore) {
    return mainQueryScore * subQueryScore * valSrcScore;
  }
  
  /**
   * Computes a custom score using the mainQuery score, a single subQuery score and multiple ValueSourceQuery scores.</br>
   * If you want to customize the score using the mainQuery score, a single subQuery score and multiple ValueSourceQuery scores
   * {@link CustomScoreQueryWithSubqueries#customExplainOneSubQueryNValueSources(int, Explanation, Explanation, Explanation[])}
   * @param doc doc id
   * @param mainQueryScore the score of the main query
   * @param subQueryScore the score of a single subQuery
   * @param valSrcScores the scores of multiple ValueSourceQuery
   * @return the custom score
   */
  protected float customScoreOneSubQueryNValueSources(int doc, float mainQueryScore, float subQueryScore, float[] valSrcScores) {
    return mainQueryScore * subQueryScore * productOfArrayElements(valSrcScores);
  }
  
  /**
   * Computes a custom score using the mainQuery score, multiple subQueries scores and a single ValueSourceQuery score.</br>
   * If you want to customize the score using the mainQuery score, multiple subQueries scores and a single ValueSourceQuery score
   * {@link CustomScoreQueryWithSubqueries#customExplainNSubQueriesOneValueSource(int, Explanation, Explanation[], Explanation)}
   * @param doc doc id
   * @param mainQueryScore the score of the main query
   * @param subQueriesScores the scores of multiple subQueries
   * @param valSrcScore the score of a single ValueSourceQuery
   * @return the custom score
   */
  protected float customScoreNSubQueriesOneValueSource(int doc, float mainQueryScore, float[] subQueriesScores, float valSrcScore) {
    return mainQueryScore * productOfArrayElements(subQueriesScores) * valSrcScore;
  }
  
  /**
   * Computes a custom score using the mainQuery score, multiple subQueries scores and multiple ValueSourceQuery scores.</br>
   * If you want to customize the score using the mainQuery score, multiple subQueries scores and multiple ValueSourceQuery scores
   * {@link CustomScoreQueryWithSubqueries#customExplainNSubQueriesNValueSources(int, Explanation, Explanation[], Explanation[])}
   * @param doc doc id
   * @param mainQueryScore the score of the main query
   * @param subQueriesScores the scores of multiple subQueries
   * @param valSrcScores the scores of multiple ValueSourceQuery
   * @return the custom score
   */
  protected float customScoreNSubQueriesNValueSources(int doc, float mainQueryScore, float[] subQueryScores, float[] valSrcScores) {
	  return mainQueryScore * productOfArrayElements(subQueryScores) * productOfArrayElements(valSrcScores);
	  
  }
  
  /**
   * Internal method that dispatches the appropiate custom score calculation based on the number of optional
   * subQueries and optional ValueSourceQueries. This method is invoked by {@link CustomScorer#score()}
   */
  private float dispatchCustomScore(int doc, float mainQueryScore, float[] subQueryScores, float[] valSrcScores) {
     if (subQueryScores.length == 0 && valSrcScores.length == 0) {
    	 return this.customScore(doc, mainQueryScore);
     }
     if (subQueryScores.length == 0 && valSrcScores.length == 1) {
    	 return this.customScoreOneValueSource(doc, mainQueryScore, valSrcScores[0]);
     }
     if (subQueryScores.length == 1 && valSrcScores.length == 0) {
    	 return this.customScoreOneSubQuery(doc, mainQueryScore, subQueryScores[0]);
     }
     if (subQueryScores.length == 1 && valSrcScores.length == 1) {
    	 return this.customScoreOneSubQueryOneValueSource(doc, mainQueryScore, subQueryScores[0], valSrcScores[0]);
     }
     if (subQueryScores.length == 1 && valSrcScores.length > 1) {
    	 return this.customScoreOneSubQueryNValueSources(doc, mainQueryScore, subQueryScores[0], valSrcScores);
     }
     if (subQueryScores.length > 1 && valSrcScores.length == 1) {
    	 return this.customScoreNSubQueriesOneValueSource(doc, mainQueryScore, subQueryScores, valSrcScores[0]);
     }
     return this.customScoreNSubQueriesNValueSources(doc, mainQueryScore, subQueryScores, valSrcScores);
  }
  
  /**
   * Explains a custom score using the mainQuery score.</br>
   * If you want to customize the score using the mainQuery only, override this method and
   * {@link CustomScoreQueryWithSubqueries#customScore(int, Explanation)}
   * @param doc doc id
   * @param mainQueryExpl explanation for the mainQuery part.
   * @return an explanation for the custom score
   */
  protected Explanation customExplain(int doc, Explanation mainQueryExpl) {
	  return mainQueryExpl;
  }
      
  /**
   * Explains a custom score using the mainQuery score.</br>
   * If you want to customize the score using the mainQuery only, override this method and
   * {@link CustomScoreQueryWithSubqueries#customScore(int, Explanation)}
   * @param doc doc id
   * @param mainQueryExpl explanation for the mainQuery part.
   * @return an explanation for the custom score
   */
  protected Explanation customExplainOneSubQuery(int doc, Explanation mainQueryExpl, Explanation subQueryExpl) {
	  Explanation exp = new Explanation( mainQueryExpl.getValue() * subQueryExpl.getValue(), "custom score: product of:");
	  exp.addDetail(mainQueryExpl);
	  exp.addDetail(subQueryExpl);
	  return exp;
  }
  
  /**
   * Explains a custom score using the mainQuery score and a single ValueSourceQuery score.</br>
   * If you want to customize the score using the mainQuery and a single ValueSourceQuery score, override this method and
   * {@link CustomScoreQueryWithSubqueries#customScoreOneValueSource(int, float, float)}
   * @param doc doc id
   * @param mainQueryExpl explanation for the mainQuery part.
   * @param valSrcExpl explanation for the ValueSourceQuery part
   * @return an explanation for the custom score
   */
  protected Explanation customExplainOneValueSource(int doc, Explanation mainQueryExpl, Explanation valSrcExpl) {
	  Explanation exp = new Explanation( mainQueryExpl.getValue() * valSrcExpl.getValue(), "custom score: product of:");
	  exp.addDetail(mainQueryExpl);
	  exp.addDetail(valSrcExpl);
	  return exp;
  }
  
  /**
   * Explains a custom score using the mainQuery score and multiple subQueries scores.</br>
   * If you want to customize the score using the mainQuery and multiple subQueries scores, override this method and
   * {@link CustomScoreQueryWithSubqueries#customScoreNSubQueries(int, float, float[])}
   * @param doc doc id
   * @param mainQueryExpl explanation for the mainQuery part.
   * @param subQueryExpls explanations for the subQueries part
   * @return an explanation for the custom score
   */
  protected Explanation customExplainNSubQueries(int doc, Explanation mainQueryExpl, Explanation[] subQueryExpls) {
	 float subQueriesProduct = productOfExplanationsValues(subQueryExpls);
	 Explanation exp = new Explanation( mainQueryExpl.getValue() * subQueriesProduct, "custom score: product of:");
     exp.addDetail(mainQueryExpl);
     for (int i = 0; i < subQueryExpls.length; i++) {
       exp.addDetail(subQueryExpls[i]);
     }
     return exp;
  }
  
  /**
   * Explains a custom score using the mainQuery score and multiple ValueSourceQuery scores.</br>
   * If you want to customize the score using the mainQuery and multiple ValueSourceQuery scores, override this method and
   * {@link CustomScoreQueryWithSubqueries#customScoreNValueSources(int, float, float[])}
   * @param doc doc id
   * @param mainQueryExpl explanation for the mainQuery part.
   * @param valSrcExpls explanations for the ValueSourceQuery part
   * @return an explanation for the custom score
   */
  protected Explanation customExplainNValueSources(int doc, Explanation mainQueryExpl, Explanation[] valSrcExpls) {
	  float valSrcProduct = productOfExplanationsValues(valSrcExpls);
	  Explanation exp = new Explanation( mainQueryExpl.getValue() * valSrcProduct, "custom score: product of:");
	  exp.addDetail(mainQueryExpl);
	  for (int i = 0; i < valSrcExpls.length; i++) {
		  exp.addDetail(valSrcExpls[i]);
	  }
	  return exp;
  }
  
  /**
   * Explains a custom score using the mainQuery score, a single subQuery score and a single ValueSourceQuery score.</br>
   * If you want to customize the score using the mainQuery score, a single subQuery score and a single ValueSourceQuery score, override this method and
   * {@link CustomScoreQueryWithSubqueries#customScoreOneSubQueryOneValueSource(int, float, float, float)}
   * @param doc doc id
   * @param mainQueryExpl explanation for the mainQuery part.
   * @param subQueryExpl explanation for the subQuery part
   * @param valSrcExpl explanation for the ValueSourceQuery part
   * @return an explanation for the custom score
   */
  protected Explanation customExplainOneSubQueryOneValueSource(int doc, Explanation mainQueryExpl, Explanation subQueryExpl, Explanation valSrcExpl) {
	  Explanation exp = new Explanation( mainQueryExpl.getValue() * subQueryExpl.getValue() * valSrcExpl.getValue(), "custom score: product of:");
	  exp.addDetail(mainQueryExpl);
	  exp.addDetail(subQueryExpl);
	  exp.addDetail(valSrcExpl);
	  return exp;
  }
  
  /**
   * Explains a custom score using the mainQuery score, a single subQuery score and multiple ValueSourceQuery scores.</br>
   * If you want to customize the score using the mainQuery score, a single subQuery score and a single ValueSourceQuery score, override this method and
   * {@link CustomScoreQueryWithSubqueries#customScoreOneSubQueryNValueSources(int, float, float, float[])}
   * @param doc doc id
   * @param mainQueryExpl explanation for the mainQuery part.
   * @param subQueryExpl explanation for the subQuery part
   * @param valSrcExpls explanations for the ValueSourceQuery part
   * @return an explanation for the custom score
   */
  protected Explanation customExplainOneSubQueryNValueSources(int doc, Explanation mainQueryExpl, Explanation subQueryExpl, Explanation[] valSrcExpls) {
	  float valSrcProduct = productOfExplanationsValues(valSrcExpls);
	  Explanation exp = new Explanation( mainQueryExpl.getValue() * subQueryExpl.getValue() * valSrcProduct, "custom score: product of:");
	  exp.addDetail(mainQueryExpl);
	  exp.addDetail(subQueryExpl);
	  for (int i = 0; i < valSrcExpls.length; i++) {
		  exp.addDetail(valSrcExpls[i]);
	  }
	  return exp;
  }
  
  /**
   * Explains a custom score using the mainQuery score, multiple subQueries scores and a single ValueSourceQuery score.</br>
   * If you want to customize the score using the mainQuery score, multiple subQueries scores and a single ValueSourceQuery score, override this method and
   * {@link CustomScoreQueryWithSubqueries#customScoreNSubQueriesOneValueSource(int, float, float[], float)}
   * @param doc doc id
   * @param mainQueryExpl explanation for the mainQuery part.
   * @param subQueryExpls explanations for the subQueries part
   * @param valSrcExpl explanation for the ValueSourceQuery part
   * @return an explanation for the custom score
   */
  protected Explanation customExplainNSubQueriesOneValueSource(int doc, Explanation mainQueryExpl, Explanation[] subQueriesExpls, Explanation valSrcExpl) {
	  float subQueriesProduct = productOfExplanationsValues(subQueriesExpls);
	  Explanation exp = new Explanation( mainQueryExpl.getValue() * subQueriesProduct * valSrcExpl.getValue(), "custom score: product of:");
	  exp.addDetail(mainQueryExpl);
	  for (int i = 0; i < subQueriesExpls.length; i++) {
		  exp.addDetail(subQueriesExpls[i]);
	  }
	  exp.addDetail(valSrcExpl);
	  return exp;
  }
  
  /**
   * Explains a custom score using the mainQuery score, multiple subQueries scores and multiple ValueSourceQuery scores.</br>
   * If you want to customize the score using the mainQuery score, multiple subQueries scores and multiple ValueSourceQuery scores, override this method and
   * {@link CustomScoreQueryWithSubqueries#customScoreNSubQueriesNValueSources(int, float, float[], float[])}
   * @param doc doc id
   * @param mainQueryExpl explanation for the mainQuery part.
   * @param subQueryExpls explanations for the subQueries part
   * @param valSrcExpls explanations for the ValueSourceQuery part
   * @return an explanation for the custom score
   */
  protected Explanation customExplainNSubQueriesNValueSources(int doc, Explanation mainQueryExpl, Explanation[] subQueriesExpls, Explanation[] valSrcExpls) {
	  float subQueriesProduct = productOfExplanationsValues(subQueriesExpls);
	  float valSrcProduct = productOfExplanationsValues(valSrcExpls);
	  Explanation exp = new Explanation( mainQueryExpl.getValue() * subQueriesProduct * valSrcProduct, "custom score: product of:");
	  exp.addDetail(mainQueryExpl);
	  for (int i = 0; i < subQueriesExpls.length; i++) {
		  exp.addDetail(subQueriesExpls[i]);
	  }
	  for (int i = 0; i < valSrcExpls.length; i++) {
		  exp.addDetail(valSrcExpls[i]);
	  }
	  return exp;
  }
  
  /**
   * Internal method that dispatches the appropiate custom explanation based on the number of optional
   * subQueries and optional ValueSourceQueries. This method is invoked by {@link CustomWeight#doExplain(IndexReader, int)}
   */
  private Explanation dispatchCustomExplain(int doc, Explanation mainQueryExpl, Explanation[] subQueriesExpls, Explanation[] valSrcExpls) {
     if (subQueriesExpls.length == 0 && valSrcExpls.length == 0) {
    	 return this.customExplain(doc, mainQueryExpl);
     }
     if (subQueriesExpls.length == 0 && valSrcExpls.length == 1) {
    	 return this.customExplainOneValueSource(doc, mainQueryExpl, valSrcExpls[0]);
     }
     if (subQueriesExpls.length == 1 && valSrcExpls.length == 0) {
    	 return this.customExplainOneSubQuery(doc, mainQueryExpl, subQueriesExpls[0]);
     }
     if (subQueriesExpls.length == 1 && valSrcExpls.length == 1) {
    	 return this.customExplainOneSubQueryOneValueSource(doc, mainQueryExpl, subQueriesExpls[0], valSrcExpls[0]);
     }
     if (subQueriesExpls.length == 1 && valSrcExpls.length > 1) {
    	 return this.customExplainOneSubQueryNValueSources(doc, mainQueryExpl, subQueriesExpls[0], valSrcExpls);
     }
     if (subQueriesExpls.length > 1 && valSrcExpls.length == 1) {
    	 return this.customExplainNSubQueriesOneValueSource(doc, mainQueryExpl, subQueriesExpls, valSrcExpls[0]);
     }
     return this.customExplainNSubQueriesNValueSources(doc, mainQueryExpl, subQueriesExpls, valSrcExpls);
  }
    
  //=========================== W E I G H T ============================
  
  private class CustomWeight extends Weight {
    Similarity similarity;
    Weight mainQueryWeight;
    Weight[] subQueriesWeights;
    Weight[] valSrcWeights;
    boolean qStrict;

    public CustomWeight(Searcher searcher) throws IOException {
      this.similarity = getSimilarity(searcher);
      this.mainQueryWeight = mainQuery.weight(searcher);
      this.subQueriesWeights = new Weight[subQueries.length];
      for(int i = 0; i < subQueries.length; i++) {
        this.subQueriesWeights[i] = subQueries[i].createWeight(searcher);
      }
      this.valSrcWeights = new Weight[valSrcQueries.length];
      for(int i = 0; i < valSrcQueries.length; i++) {
        this.valSrcWeights[i] = valSrcQueries[i].createWeight(searcher);
      }
      this.qStrict = strict;
    }

    /*(non-Javadoc) @see org.apache.lucene.search.Weight#getQuery() */
    public Query getQuery() {
      return CustomScoreQueryWithSubqueries.this;
    }

    /*(non-Javadoc) @see org.apache.lucene.search.Weight#getValue() */
    public float getValue() {
      return getBoost();
    }

    /*(non-Javadoc) @see org.apache.lucene.search.Weight#sumOfSquaredWeights() */
    public float sumOfSquaredWeights() throws IOException {
      float sum = mainQueryWeight.sumOfSquaredWeights();
      for(int i = 0; i < subQueriesWeights.length; i++) {
          if (qStrict) {
        	  subQueriesWeights[i].sumOfSquaredWeights(); // do not include ValueSource part in the query normalization
          } else {
            sum += subQueriesWeights[i].sumOfSquaredWeights();
          }
       }
      for(int i = 0; i < valSrcWeights.length; i++) {
        if (qStrict) {
          valSrcWeights[i].sumOfSquaredWeights(); // do not include ValueSource part in the query normalization
        } else {
          sum += valSrcWeights[i].sumOfSquaredWeights();
        }
      }
      sum *= getBoost() * getBoost(); // boost each sub-weight
      return sum ;
    }

    /*(non-Javadoc) @see org.apache.lucene.search.Weight#normalize(float) */
    public void normalize(float norm) {
      norm *= getBoost(); // incorporate boost
      mainQueryWeight.normalize(norm);
      for(int i = 0; i < subQueriesWeights.length; i++) {
          if (qStrict) {
        	  subQueriesWeights[i].normalize(1); // do not normalize the ValueSource part
          } else {
        	  subQueriesWeights[i].normalize(norm);
          }
        }
      for(int i = 0; i < valSrcWeights.length; i++) {
        if (qStrict) {
          valSrcWeights[i].normalize(1); // do not normalize the ValueSource part
        } else {
          valSrcWeights[i].normalize(norm);
        }
      }
    }

    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      // Pass true for "scoresDocsInOrder", because we
      // require in-order scoring, even if caller does not,
      // since we call advance on the subQueriesScorer and valSrcScorers.  Pass
      // false for "topScorer" because we will not invoke
      // score(Collector) on these scorers:
      Scorer mainQueryScorer = mainQueryWeight.scorer(reader, true, false);
      if (mainQueryScorer == null) {
        return null;
      }
      Scorer[] subQueriesScorers = new Scorer[subQueriesWeights.length];
      for(int i = 0; i < subQueriesScorers.length; i++) {
    	  subQueriesScorers[i] = subQueriesWeights[i].scorer(reader, true, topScorer);
      }
      Scorer[] valSrcScorers = new Scorer[valSrcWeights.length];
      for(int i = 0; i < valSrcScorers.length; i++) {
         valSrcScorers[i] = valSrcWeights[i].scorer(reader, true, topScorer);
      }
      return new CustomScorer(similarity, reader, this, mainQueryScorer, subQueriesScorers, valSrcScorers);
    }

    public Explanation explain(IndexReader reader, int doc) throws IOException {
      Explanation explain = doExplain(reader, doc);
      return explain == null ? new Explanation(0.0f, "no matching docs") : doExplain(reader, doc);
    }
    
    private Explanation doExplain(IndexReader reader, int doc) throws IOException {
      Scorer[] valSrcScorers = new Scorer[valSrcWeights.length];
      for(int i = 0; i < valSrcScorers.length; i++) {
         valSrcScorers[i] = valSrcWeights[i].scorer(reader, true, false);
      }
      Explanation mainQueryExpl = mainQueryWeight.explain(reader, doc);
      if (!mainQueryExpl.isMatch()) {
        return mainQueryExpl;
      }
      // match
      Explanation[] subQueriesExpls = new Explanation[subQueriesWeights.length];
      for(int i = 0; i < subQueriesExpls.length; i++) {
        subQueriesExpls[i] = subQueriesWeights[i].explain(reader,doc);
      }
      Explanation[] valSrcExpls = new Explanation[valSrcScorers.length];
      for(int i = 0; i < valSrcScorers.length; i++) {
        valSrcExpls[i] = valSrcScorers[i].explain(doc);
      }
      Explanation customExp = dispatchCustomExplain(doc,mainQueryExpl,subQueriesExpls,valSrcExpls);
      float sc = getValue() * customExp.getValue();
      Explanation res = new ComplexExplanation(
        true, sc, CustomScoreQueryWithSubqueries.this.toString() + ", product of:");
      res.addDetail(customExp);
      res.addDetail(new Explanation(getValue(), "queryBoost")); // actually using the q boost as q weight (== weight value)
      return res;
    }

    public boolean scoresDocsOutOfOrder() {
      return false;
    }
    
  }


  //=========================== S C O R E R ============================
  
  /**
   * A scorer that applies a (callback) function on scores of the mainQuery.
   */
  private class CustomScorer extends Scorer {
    private final CustomWeight weight;
    private final float qWeight;
    private Scorer mainQueryScorer;
    private Scorer[] subQueriesScorers;
    private Scorer[] valSrcScorers;
    private IndexReader reader;
    /**
     * Scores of the {@link #subQueriesScorers}
     * Reused in score() to avoid allocating this array for each doc
     */
    private float sScores[];
    /**
     * This boolean array keeps track, for each subScorer of each subQuery,
     * if it has or not results for the current document that matches the mainQuery scorer.
     * This array is used in the {@link #score()} method in order to ask or not the score
     * for each subQueryScorer, and to avoid asign erroneous scores to documents
     */
    private boolean subQueriesScoresForCurrentDocument[];
    /**
     * This array is used in the method {@link #advanceSubQueriesScorers(int)} in order to 
     * store the next matching documents for each subQueryScorer.
     * 
     * This method calls {@link Scorer#advance(int)} for each scorer
     * in {@link #subQueriesScorers} and stores the returned doc id in
     * {@link #nextDocsSubqueries}.
     * This way, we can remember the last
     * advanced doc id for each scorer and call {@link Scorer#advance(int)}
     * method only if the remembered doc id for a subQuery scorer is less than
     * the current doc id matching the mainQuery. 
     * This is necessary because the {@link Scorer#advance(int)} advance no matter
     * if the current doc id equals the doc id in the parameter or not (does not check
     * if the current doc id equals the doc id in the parameter)
     */
    private int nextDocsSubqueries[];
    /**
     * Scores of the {@link #valSrcScorers}
     * Reused in score() to avoid allocating this array for each doc
     */
    private float vScores[]; 

    // constructor
    private CustomScorer(Similarity similarity, IndexReader reader, CustomWeight w,
        Scorer mainQueryScorer, Scorer[] subQueriesScorers, Scorer[] valSrcScorers) throws IOException {
      super(similarity);
      this.weight = w;
      this.qWeight = w.getValue();
      this.mainQueryScorer = mainQueryScorer;
      this.subQueriesScorers = subQueriesScorers;
      this.valSrcScorers = valSrcScorers;
      this.reader = reader;
      this.sScores = new float[subQueriesScorers.length];
      this.subQueriesScoresForCurrentDocument = new boolean[subQueriesScorers.length];
      this.nextDocsSubqueries = new int[subQueriesScorers.length];
      Arrays.fill(this.nextDocsSubqueries, -1);
      this.vScores = new float[valSrcScorers.length];
    }

    /** @deprecated use {@link #nextDoc()} instead. */
    public boolean next() throws IOException {
      return nextDoc() != NO_MORE_DOCS;
    }
    
    /**
     * This method calls {@link Scorer#advance(int)} for each scorer
     * in {@link #subQueriesScorers} and stores the returned doc id in
     * {@link #nextDocsSubqueries}. This way, we can remember the last
     * advanced doc id for each scorer and call {@link Scorer#advance(int)}
     * method only if the remembered doc id for a subQuery scorer is less than
     * the current doc id matching the mainQuery. 
     * This is necessary because the {@link Scorer#advance(int)} advance no matter
     * if the current doc id equals the doc id in the parameter or not (does not check
     * if the current doc id equals the doc id in the parameter)
     * @param doc doc id matching the mainQuery
     * @throws IOException
     */
    private void advanceSubQueriesScorers(int doc) throws IOException {
    	for (int i = 0; i < this.subQueriesScorers.length; i++) {
    		if (this.nextDocsSubqueries[i] != NO_MORE_DOCS && this.nextDocsSubqueries[i] < doc) {
    			this.nextDocsSubqueries[i] = this.subQueriesScorers[i].advance(doc);
    		}
    	}
    }

    public int nextDoc() throws IOException {
      int doc = mainQueryScorer.nextDoc();
      if (doc != NO_MORE_DOCS) {
    	  this.advanceSubQueriesScorers(doc);
    	  for (int i = 0; i< this.nextDocsSubqueries.length; i++) {
    		  subQueriesScoresForCurrentDocument[i] = this.nextDocsSubqueries[i] == doc ? true : false;
    	  }
          for (int i = 0; i < valSrcScorers.length; i++) {
        	  valSrcScorers[i].advance(doc);
          }
        }
      return doc;
    }

    /** @deprecated use {@link #docID()} instead. */
    public int doc() {
      return mainQueryScorer.doc();
    }

    public int docID() {
      return mainQueryScorer.docID();
    }
    
    /*(non-Javadoc) @see org.apache.lucene.search.Scorer#score() */
    public float score() throws IOException {
      for (int i = 0; i < subQueriesScorers.length; i++) {
    	  sScores[i] = subQueriesScoresForCurrentDocument[i] ? subQueriesScorers[i].score() : 0f;
      }
      for (int i = 0; i < valSrcScorers.length; i++) {
        vScores[i] = valSrcScorers[i].score();
      }
      return qWeight * dispatchCustomScore(mainQueryScorer.docID(), mainQueryScorer.score(), sScores, vScores);
    }

    /** @deprecated use {@link #advance(int)} instead. */
    public boolean skipTo(int target) throws IOException {
      return advance(target) != NO_MORE_DOCS;
    }

    public int advance(int target) throws IOException {
      int doc = mainQueryScorer.advance(target);
      if (doc != NO_MORE_DOCS) {
    	  for (int i = 0; i < subQueriesScorers.length; i++) {
            	subQueriesScorers[i].advance(doc);
            }
        for (int i = 0; i < valSrcScorers.length; i++) {
          valSrcScorers[i].advance(doc);
        }
      }
      return doc;
    }
    
    // TODO: remove in 3.0
    /*(non-Javadoc) @see org.apache.lucene.search.Scorer#explain(int) */
    public Explanation explain(int doc) throws IOException {
      Explanation subQueryExpl = weight.mainQueryWeight.explain(reader,doc);
      if (!subQueryExpl.isMatch()) {
        return subQueryExpl;
      }
      // match
      Explanation[] subQueriesExpl = new Explanation[subQueriesScorers.length];
      for(int i = 0; i < subQueriesScorers.length; i++) {
    	  subQueriesExpl[i] = subQueriesScoresForCurrentDocument[i] ? subQueriesScorers[i].explain(doc) : null;
      }
      Explanation[] valSrcExpls = new Explanation[valSrcScorers.length];
      for(int i = 0; i < valSrcScorers.length; i++) {
        valSrcExpls[i] = valSrcScorers[i].explain(doc);
      }
      Explanation customExp = dispatchCustomExplain(doc,subQueryExpl,subQueriesExpl,valSrcExpls);
      float sc = qWeight * customExp.getValue();
      Explanation res = new ComplexExplanation(true, sc, CustomScoreQueryWithSubqueries.this.toString() + ", product of:");
      res.addDetail(customExp);
      res.addDetail(new Explanation(qWeight, "queryBoost")); // actually using the q boost as q weight (== weight value)
      return res;
    }
  }

  public Weight createWeight(Searcher searcher) throws IOException {
    return new CustomWeight(searcher);
  }

  /**
   * Checks if this is strict custom scoring.
   * In strict custom scoring, the ValueSource part does not participate in weight normalization.
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
    return "customWithSubqueries";
  }

}

