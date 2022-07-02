package org.apache.lucene.search;

/**
 * Copyright 2004 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.IOException;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.IndexReader;

/**
 * A Query that matches documents containing a term. This may be combined with
 * other terms with a {@link BooleanQuery}.
 */
public class IntegerRangeQuery extends Query
{
    private Integer lowerTerm;

    private Integer upperTerm;

    private String field;

    private boolean inclusive;

    private class IntegerRangeWeight implements Weight
    {
        private Searcher searcher;

        private float value;

        private float queryNorm;

        private float queryWeight;

        public IntegerRangeWeight(Searcher searcher)
        {
            this.searcher = searcher;
        }

        public String toString()
        {
            return "weight(" + IntegerRangeQuery.this + ")";
        }

        public Query getQuery()
        {
            return IntegerRangeQuery.this;
        }

        public float getValue()
        {
            return value;
        }

        public float sumOfSquaredWeights() throws IOException
        {
            queryWeight = getBoost(); // compute query weight
            return queryWeight * queryWeight; // square it
        }

        public void normalize(float queryNorm)
        {
            this.queryNorm = queryNorm;
            queryWeight *= queryNorm; // normalize query weight
            value = queryWeight;
        }

        public Scorer scorer(IndexReader reader) throws IOException
        {
            int docnos[] = FieldCache.DEFAULT.getIntegerDocnos(reader, field);
            int docvals[] = FieldCache.DEFAULT.getInts(reader, field);
            if (docnos == null || docvals == null) {
                throw new IOException("Field not indexed properly for integer range queries:" + field);
            }
            return new IntegerRangeScorer(this, lowerTerm.intValue(), upperTerm
                    .intValue(), inclusive, docnos, docvals,
                    getSimilarity(searcher), reader.norms(field));

        }

        public Explanation explain(IndexReader reader, int doc)
                throws IOException
        {

            Explanation result = new Explanation();
            result.setDescription("weight(" + getQuery() + " in " + doc
                    + "), product of:");

            // explain query weight
            Explanation queryExpl = new Explanation();
            queryExpl.setDescription("queryWeight(" + getQuery()
                    + "), product of:");

            Explanation boostExpl = new Explanation(getBoost(), "boost");
            if (getBoost() != 1.0f) queryExpl.addDetail(boostExpl);

            Explanation queryNormExpl = new Explanation(queryNorm, "queryNorm");
            queryExpl.addDetail(queryNormExpl);

            queryExpl.setValue(boostExpl.getValue() * queryNormExpl.getValue());

            result.addDetail(queryExpl);

            // explain field weight
            Explanation fieldExpl = new Explanation();
            fieldExpl.setDescription("fieldWeight(" + lowerTerm + ","
                    + upperTerm + " in " + doc + "), product of:");

            Explanation fieldNormExpl = new Explanation();
            byte[] fieldNorms = reader.norms(field);
            float fieldNorm = fieldNorms != null ? Similarity
                    .decodeNorm(fieldNorms[doc]) : 0.0f;
            fieldNormExpl.setValue(fieldNorm);
            fieldNormExpl.setDescription("fieldNorm(field=" + field + ", doc="
                    + doc + ")");
            fieldExpl.addDetail(fieldNormExpl);

            fieldExpl.setValue(fieldNormExpl.getValue());

            result.addDetail(fieldExpl);

            // combine them
            result.setValue(queryExpl.getValue() * fieldExpl.getValue());

            if (queryExpl.getValue() == 1.0f) return fieldExpl;

            return result;
        }
    }

    /**
     * Constructs a query selecting all terms greater than
     * <code>lowerTerm</code> but less than <code>upperTerm</code>. There
     * must be at least one term and either term may be null, in which case
     * there is no bound on that side, but if there are two terms, both terms
     * <b>must</b> be for the same field.
     */
    public IntegerRangeQuery(String field, Integer lowerTerm,
            Integer upperTerm, boolean inclusive)
    {
        this.field = field.intern();

        // if we have a lowerTerm, start there. otherwise, start at beginning
        if (lowerTerm != null)
        {
            this.lowerTerm = lowerTerm;
        }
        else
        {
            this.lowerTerm = new Integer(Integer.MIN_VALUE);
        }

        if (upperTerm != null)
        {
            this.upperTerm = upperTerm;
        }
        else
        {
            this.upperTerm = new Integer(Integer.MAX_VALUE);
        }
        this.inclusive = inclusive;

        if (this.lowerTerm > this.upperTerm) { throw new IllegalArgumentException(
                "Lower bound must be smaller than upper bound");

        }
    }

    protected Weight createWeight(Searcher searcher)
    {
        return new IntegerRangeWeight(searcher);
    }

    /** Prints a user-readable version of this query. */
    public String toString(String field)
    {
        StringBuffer buffer = new StringBuffer();
        if (!field.equals(field))
        {
            buffer.append(field);
            buffer.append(":[");
        }
        buffer.append(lowerTerm);
        buffer.append(" TO ");
        buffer.append(upperTerm);
        if (getBoost() != 1.0f)
        {
            buffer.append("^");
            buffer.append(Float.toString(getBoost()));
        }
        return buffer.toString();
    }

    /** Returns true iff <code>o</code> is equal to this. */
    public boolean equals(Object o)
    {
        if (!(o instanceof IntegerRangeQuery)) return false;
        IntegerRangeQuery other = (IntegerRangeQuery) o;
        return (this.getBoost() == other.getBoost())
                && this.lowerTerm.equals(other.lowerTerm)
                && this.upperTerm.equals(other.upperTerm);
    }

    /** Returns a hash code value for this object. */
    public int hashCode()
    {
        return Float.floatToIntBits(getBoost()) ^ lowerTerm.hashCode()
                ^ upperTerm.hashCode();
    }

}
