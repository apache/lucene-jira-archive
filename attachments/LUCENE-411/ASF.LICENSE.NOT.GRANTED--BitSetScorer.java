/*
 * Created on Nov 2, 2004
 */
package org.apache.lucene.search;

import java.io.IOException;
import java.util.BitSet;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;

/**
 * @author rayt
 */
public class BitSetScorer extends Scorer
{
    private BitSet bits = null;

    private int current = 0;
    
    private int i = 0;
    
    private int [] starts = null;

    public BitSetScorer(Similarity similarity, BitSet bits, int i, int [] starts)
    {
        super(similarity);
        this.bits = bits;
        this.i = i;
        this.starts = starts;
        
        current = bits.nextSetBit(starts[i]);
        if (current >= 0)
        {
            current--;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.Scorer#next()
     */
    public boolean next() throws IOException
    {
        current = bits.nextSetBit(current + 1);
        if (current < 0)
        {
            return false;
        }
        if (i < starts.length - 1)
        {
            return current >= starts[i] && current < starts[i + 1];
        }
        else
        {
            return current >= starts[i];
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.Scorer#doc()
     */
    public int doc()
    {
        return current - starts[i];
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.Scorer#score()
     */
    public float score() throws IOException
    {
        return 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.Scorer#skipTo(int)
     */
    public boolean skipTo(int target) throws IOException
    {
        do
        {
            if (!next())
                return false;
        } while (target > doc());
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.Scorer#explain(int)
     */
    public Explanation explain(int doc) throws IOException
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