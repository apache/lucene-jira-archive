/** Licensed to the Apache Software Foundation (ASF) under one or more
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

package org.apache.lucene.spatial;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.util.ToStringUtils;

/**
 * TODO: Implement new Spatial API: Point, GeoDistanceCalculator, LocationDataSet
 * Distance in miles? - holy crap, we should use SI-units. Please use km or a UOM parameter.
 * 
 * @lucene.experimental
 */
public class DistanceQuery extends Query {

  protected final Point center;
  protected final double maxDistance;
  protected final GeoDistanceCalculator distanceCalculator;

  /**
   * TODO
   */
  public DistanceQuery(Point center, double maxDistance, GeoDistanceCalculator distanceCalculator) {
    this.center = center;
    this.maxDistance = maxDistance;
    this.distanceCalculator = distanceCalculator;
  }

  @Override
  public String toString(String field) {
    final StringBuilder sb = new StringBuilder("DistanceQuery(");
    sb.append("center=").append(center).append(',');
    sb.append("maxDistance=").append(maxDistance);
    return sb.append(')').append(ToStringUtils.boost(getBoost())).toString();
  }

  /** Returns true if <code>o</code> is equal to this. */
  @Override
  public boolean equals(Object o) {
    if (o == null) return false;
    if (o == this) return true;
    if (this.getClass() != o.getClass()) return false;
    final DistanceQuery other = (DistanceQuery) o;
    return super.equals(o) &&
      this.center.equals(other.center) &&
      this.maxDistance == other.maxDistance;
  }

  /** Returns a hash code value for this object. */
  @Override
  public int hashCode() {
    int h = super.hashCode();
    h = 31*h + center.hashCode();
    h = 31*h + (int)Double.doubleToLongBits(maxDistance);
    return h;
  }
  
  public void extractTerms(Set<Term> terms) {
    // OK to not add any terms when used for MultiSearcher,
    // but may not be OK for highlighting
  }

  @Override
  public Weight createWeight(Searcher searcher) throws IOException {
    return new DistanceWeight(searcher);
  }
  
  /**
   * Creates a {@link DocIdSet} for the query's bounding box.
   * This can come from a {@link Filter#getDocIdSet} or may be
   * a wrapped {@link Scorer} of another query.
   */
  protected DocIdSet getBBOXDocIdSet(IndexReader reader) throws IOException {
    // TODO:
    final CartesianPolyFilterBuilder cpf = new CartesianPolyFilterBuilder(tierFieldPrefix, minTierIndexed, maxTierIndexed);
    return cpf.getBoundingArea(center, maxDistance).getDocIdSet(reader);
  }
  
  /**
   * Returns a {@link LocationDataSet} to get the latitude/longitude
   * of each dataset in the given {@link IndexReader}.
   */
  protected LocationDataSet getLocationDataSet(IndexReader reader) throws IOException {
    // TODO
    return null;
  }
  
  /**
   * Calculates a score (0..1) out of a given distance.
   * The default implementation returns {@code (maxDistance - docDistance) / maxDistance}.
   */
  protected float getDistanceScore(double docDistance) {
    return (float) ((maxDistance - docDistance) / maxDistance);
  }

  //=========================== W E I G H T ============================
  
  private class DistanceWeight extends Weight {
    private final Similarity similarity;
    private float queryNorm;
    private float queryWeight;

    DistanceWeight(Searcher searcher) throws IOException {
      this.similarity = getSimilarity(searcher);
    }

    @Override
    public Query getQuery() {
      return DistanceQuery.this;
    }

    @Override
    public float getValue() {
      return queryWeight;
    }

    @Override
    public float sumOfSquaredWeights() throws IOException {
      queryWeight = getBoost();
      return queryWeight * queryWeight;
    }

    @Override
    public void normalize(float norm) {
      this.queryNorm = norm;
      queryWeight *= this.queryNorm;
    }

    @Override
    public Scorer scorer(final IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      return new DistanceScorer(similarity, reader, this);
    }

    @Override
    public Explanation explain(final IndexReader reader, int doc) throws IOException {
      final DistanceScorer ds = new DistanceScorer(similarity, reader, this);
      final ComplexExplanation result = new ComplexExplanation();
      if (ds.advance(doc) == doc) {
        result.setDescription(DistanceQuery.this.toString() + ", product of:");
        result.setValue(ds.score());
        result.setMatch(Boolean.TRUE);
        result.addDetail(new Explanation(getBoost(), "boost"));
        result.addDetail(new Explanation(queryNorm, "queryNorm"));
        // TODO: miles?
        result.addDetail(new Explanation(getDistanceScore(ds.docDistance), "distanceScore(" + ds.docDistance + " miles)"));
      } else {
        result.setDescription(DistanceQuery.this.toString() + " doesn't match id " + doc);
        result.setValue(0.0f);
        result.setMatch(Boolean.FALSE);
      }
      return result;
    }
    
  }

  //=========================== S C O R E R ============================
  
  private class DistanceScorer extends Scorer {
    private final float qWeight;
    private final DocIdSetIterator iterator;
    private final LocationDataSet locations;
    
    private int doc = -1;
    double docDistance = Double.NaN; // weight's explain needs access, so package protected

    DistanceScorer(final Similarity similarity, final IndexReader reader, final DistanceWeight w) throws IOException {
      super(similarity);
      this.qWeight = w.getValue();
      this.iterator = getBBOXDocIdSet(reader).iterator();
      this.locations = getLocationDataSet(reader);
    }
    
    @Override
    public int nextDoc() throws IOException {
      while ((doc = iterator.nextDoc()) != NO_MORE_DOCS) {
        docDistance = distanceCalculator.getDistance(locations.getPoint(doc), center);
        if (docDistance < maxDistance) break;
      }
      return doc;
    }
    
    @Override
    public int advance(int target) throws IOException {
      if ((doc = iterator.advance(target)) != NO_MORE_DOCS) do {
        docDistance = distanceCalculator.getDistance(locations.getPoint(doc), center);
        if (docDistance < maxDistance) break;
      } while ((doc = iterator.nextDoc()) != NO_MORE_DOCS);
      return doc;
    }

    @Override
    public int docID() {
      return doc;
    }

    @Override
    public float score() {
      return qWeight * getDistanceScore(docDistance);
    }

  }

}
