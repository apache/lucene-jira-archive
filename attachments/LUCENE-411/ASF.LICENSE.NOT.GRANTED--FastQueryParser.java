/*
 * Created on Jun 23, 2005
 */
package org.apache.lucene.queryParser;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FastPrefixQuery;
import org.apache.lucene.search.FastWildcardQuery;
import org.apache.lucene.search.Query;

/**
 * @author rayt
 */
public class FastQueryParser extends QueryParser
{
    public FastQueryParser(String query, Analyzer analyzer)
    {
        super(query, analyzer);
    }

    /**
     * @param arg0
     */
    public FastQueryParser(CharStream stream)
    {
        super(stream);
    }

    /**
     * @param arg0
     */
    public FastQueryParser(QueryParserTokenManager manager)
    {
        super(manager);
    }

    protected Query getPrefixQuery(String field, String termStr)
            throws ParseException
    {
        if (lowercaseWildcardTerms)
        {
            termStr = termStr.toLowerCase();
        }
        Term t = new Term(field, termStr);
        return new FastPrefixQuery(t);

    }
    protected Query getWildcardQuery(String field, String termStr)
            throws ParseException
    {
        return new FastWildcardQuery(new Term(field, termStr));
    }
}