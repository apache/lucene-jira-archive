/*
 * Created on Jun 24, 2005
 */
package org.apache.lucene.search;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;

/**
 * @author rayt
 */
public abstract class MultiSearchableQuery extends Query
{
    public static final int [] ZERO_STARTS = {0};
    
    public Query rewrite(IndexReader reader) throws IOException
    {
        return rewrite(reader, 0, ZERO_STARTS);
    }
    
    public abstract Query rewrite(IndexReader reader, int i, int [] starts) throws IOException;
}
