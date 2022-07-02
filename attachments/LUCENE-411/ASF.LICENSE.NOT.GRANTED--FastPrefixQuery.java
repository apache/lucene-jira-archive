package org.apache.lucene.search;

import java.io.IOException;
import java.util.BitSet;
import java.util.WeakHashMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;

/** A Query that matches documents containing terms with a specified prefix. */
public class FastPrefixQuery extends PrefixQuery
{
    private final int MAX_TERMS_BATCH = 1000;
    private transient WeakHashMap cache = null;
    
    /** Constructs a query for terms starting with <code>prefix</code>. */
    public FastPrefixQuery(Term prefix)
    {
        
        super(prefix);
    }
    
    private int mergeBatchIntoQuery(IndexReader reader, BitSetQuery query, Term [] terms, String prefixText, int length) throws IOException
    {
        
        int lo = 0;
        int hi = length - 1;
        
        while (hi >= lo)
        {
            int mid = (lo + hi) >> 1;
            Term term = terms[mid];
            
            if (term.text().startsWith(prefixText))
            {
                lo = mid + 1;
            }
            else
            {
                hi = mid - 1;
            }
        }
        hi++;
        
        for (int i = 0; i < hi; i++)
        {
            TermDocs docs = reader.termDocs(terms[i]);
            while (docs.next())
            {
                query.setBit(docs.doc());
            }
        }
        
        return hi;

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
        
        int termsLength = 0;
        Term [] terms = new Term[MAX_TERMS_BATCH];
        
        
        BitSetQuery query = new BitSetQuery(new BitSet(reader.maxDoc()));
        Term prefix = getPrefix();
        
        TermEnum enumerator = reader.terms(prefix);
        String prefixText = prefix.text();
        String prefixField = prefix.field();
        
        try
        {
            do
            {
                Term term = enumerator.term();
                
                
                if (term != null && term.field() == prefixField)
                {
                    terms[termsLength] = term;
                    termsLength++;
                }
                
                if (termsLength == MAX_TERMS_BATCH)
                {
                    int lastGoodTerm = mergeBatchIntoQuery(reader, query, terms, prefixText, termsLength);
                    termsLength = 0;
                    
                    if (lastGoodTerm != MAX_TERMS_BATCH)
                    {
                        //System.out.println("lastGoodTerm = " + terms[lastGoodTerm].text());
                        break;
                    }
                }
            } while (enumerator.next());
        }
        finally
        {
            if (termsLength > 0)
            {
                int lastGoodTerm = mergeBatchIntoQuery(reader, query, terms, prefixText, termsLength);
                
                //System.out.println("lastGoodTerm = " + terms[lastGoodTerm].text());
            }
            
            enumerator.close();
        }
        
        query.setBoost(getBoost());
        
        synchronized(cache)
        {
            cache.put(reader, query);
        }
        
        return query;
    }

    public Query combine(Query[] queries)
    {
        return Query.mergeBooleanQueries(queries);
    }
}