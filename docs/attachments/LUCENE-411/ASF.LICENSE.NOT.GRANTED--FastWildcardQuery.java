/*
 * Created on Jun 23, 2005
 */
package org.apache.lucene.search;

import java.io.IOException;
import java.util.BitSet;
import java.util.WeakHashMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;

/**
 * @author rayt
 */
public class FastWildcardQuery extends WildcardQuery
{
    private transient WeakHashMap cache = null;

    public FastWildcardQuery(Term term)
    {
        super(term);
    }

    public Query rewrite(IndexReader reader) throws IOException
    {
        synchronized(this)
        {
            if (cache == null)
            {
                cache = new WeakHashMap();
            }
        }
        
        synchronized(cache)
        {
            Query query = (Query) cache.get(reader);
            if (query != null)
            {
                return query;
            }
        }
        
        BitSetQuery query = new BitSetQuery(new BitSet(reader.maxDoc()));
        
        FilteredTermEnum enumerator = getEnum(reader);
        try
        {
            do
            {
                Term t = enumerator.term();
                if (t != null)
                {
                    TermDocs docs = reader.termDocs(t);
                    while (docs.next())
                    {
                        query.setBit(docs.doc());
                    }
                }
            } while (enumerator.next());
        }
        finally
        {
            enumerator.close();
        }
        query.setBoost(getBoost());
        
        synchronized(cache)
        {
            cache.put(reader, query);
        }
        
        return query;
    }
}