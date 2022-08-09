/*
 * Created on Nov 2, 2004
 */
package org.apache.lucene.search;

import java.io.IOException;
import java.util.BitSet;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Weight;


/**
 * @author rayt
 */
public class BitSetQuery extends MultiSearchableQuery
{
    private BitSet bits = null;

    private Scorer scorer = null;
    
    private boolean startSet = false;
    
    private int [] starts = MultiSearchableQuery.ZERO_STARTS;
    
    private int i = 0;

    public BitSetQuery(BitSet bits)
    {
        this.bits = bits;
    }
    
    public BitSetQuery(BitSet bits, int i, int [] starts)
    {
        this.bits = bits;
        this.starts = starts;
        this.i = i;
        this.startSet = true;
    }
    
    public void setBit(int bit)
    {
        bits.set(bit);
    }
    
    public void clearBit(int bit)
    {
        bits.clear(bit);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.Query#toString(java.lang.String)
     */
    public String toString(String field)
    {
        return String.valueOf("bitset(" + bits.size() + ", "
                + bits.cardinality() + ")");
    }

    protected Weight createWeight(Searcher searcher)
    {
        return new BitSetWeight(searcher);
    }

    private class BitSetWeight implements Weight
    {
        private Searcher searcher = null;

        public BitSetWeight(Searcher searcher)
        {
            this.searcher = searcher;
            scorer = new BitSetScorer(getSimilarity(searcher), bits, i, starts);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.lucene.search.Weight#getQuery()
         */
        public Query getQuery()
        {
            return BitSetQuery.this;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.lucene.search.Weight#getValue()
         */
        public float getValue()
        {
            return getBoost();
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.lucene.search.Weight#sumOfSquaredWeights()
         */
        public float sumOfSquaredWeights() throws IOException
        {
            return 1;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.lucene.search.Weight#normalize(float)
         */
        public void normalize(float norm)
        {
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.lucene.search.Weight#scorer(org.apache.lucene.index.IndexReader)
         */
        public Scorer scorer(IndexReader reader) throws IOException
        {
            return scorer;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.apache.lucene.search.Weight#explain(org.apache.lucene.index.IndexReader,
         *      int)
         */
        public Explanation explain(IndexReader reader, int doc)
                throws IOException
        {
            if (bits.get(doc))
            {
                return new Explanation(1.0f, doc + " is in BitSet");
            }
            else
            {
                return new Explanation(0.0f, doc + " is not in BitSet");
            }
        }
    }

    /* (non-Javadoc)
     * @see org.apache.lucene.search.MultiSearchableQuery#rewrite(org.apache.lucene.index.IndexReader, int, int[])
     */
    public Query rewrite(IndexReader reader, int i, int[] starts) throws IOException
    {
        if (startSet)
        {
            return this;
        }
        return new BitSetQuery(bits, i, starts);
    }
}