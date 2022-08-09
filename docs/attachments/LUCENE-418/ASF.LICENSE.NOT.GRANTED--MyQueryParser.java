/* $Id$ */

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;

/**
 * Overrides Lucene's default QueryParser so that {@link #getWildcardQuery(String, String)}
 * and {@link #getPrefixQuery(String, String)} also passes through the default analyzer, but
 * that wildcards don't get removed from the terms.
 *
 * @author  <a href="mailto:ronnie.kolehmainen@ub.uu.se">Ronnie Kolehmainen</a>
 * @version $Revision$, $Date$
 */
public class MyQueryParser
    extends org.apache.lucene.queryParser.QueryParser
{
    /**
     * Constructs a query parser.
     * @param field    the default field for query terms.
     * @param analyzer used to find terms in the query text.
     */
    public MyQueryParser(String field, Analyzer analyzer)
    {
        super(field, analyzer);
    }
    
    public MyQueryParser(org.apache.lucene.queryParser.CharStream c)
    { 
        super(c);
    }
    
    public MyQueryParser(org.apache.lucene.queryParser.QueryParserTokenManager t)
    { 
        super(t);
    }
    
    
    /**
     * Parses a query string, returning a {@link org.apache.lucene.search.Query}.
     * @param  query    the query string to be parsed.
     * @param  field    the default field for query terms.
     * @param  analyzer used to find terms in the query text.
     * @throws ParseException if the parsing fails
     */
    static public Query parse(String query, String field, Analyzer analyzer)
        throws ParseException
    {
        MyQueryParser parser = new MyQueryParser(field, analyzer);
        return parser.parse(query);
    }
    
    /**
     * Called when parser
     * parses an input term token that contains one or more wildcard
     * characters (? and *), but is not a prefix term token (one
     * that has just a single * character at the end)
     * <p>
     * Depending on analyzer and settings, a wildcard term may (most probably will)
     * be lower-cased automatically. It <b>will</b> go through the default Analyzer.
     * <p>
     * Overrides super class, by passing terms through analyzer.
     *
     * @param  field   Name of the field query will use.
     * @param  termStr Term token that contains one or more wild card
     *                 characters (? or *), but is not simple prefix term
     *
     * @return Resulting {@link Query} built for the term
     * @throws ParseException throw in overridden method to disallow
     */
    protected Query getWildcardQuery(String field, String termStr)
        throws ParseException
    {
        ArrayList wlist = null;
        if (termStr.indexOf('?') > -1 || termStr.indexOf('*') > -1 ) {
            /* somewhat a hack: find/store wildcard chars
             * in order to put them back after tokenizing */
            wlist = new ArrayList();
            char[] chars = termStr.toCharArray();
            for (int i = 0; i < termStr.length(); i++) {
                if (chars[i] == '?' || chars[i] == '*') {
                    wlist.add(new Character(chars[i]));
                }
            }
        }
        
        // get Analyzer from superclass and tokenize the term
        TokenStream source = getAnalyzer().tokenStream(field,
                                                       new StringReader(termStr));
        ArrayList tlist = new ArrayList();
        org.apache.lucene.analysis.Token t;
        
        while (true) {
            try {
                t = source.next();
            } catch (IOException e) {
                t = null;
            }
            if (t == null) {
                break;
            }
            tlist.add(t.termText());
        }
        
        try {
            source.close();
        } catch (IOException e) {
            // ignore
        }
        
        if (tlist.size() == 0) {
            return null;
        } else if (tlist.size() == 1) {
            if (wlist != null && wlist.size() == 1) {
                /* if wlist contains one wildcard, it must be at the end, because:
                 * 1) wildcards are not allowed in 1st position of a term by QueryParser
                 * 2) if wildcard was *not* in end, there would be *two* or more tokens */
                return super.getWildcardQuery(field, 
                                              (String) tlist.get(0) + (((Character) wlist.get(0)).toString()));
            } else {
                /* we should never get here! if so, this method was called
                 * with a termStr containing no wildcard ... */
                return super.getWildcardQuery(field, (String) tlist.get(0));
            }
        } else {
            /* the term was tokenized, let's rebuild to one token
             * with wildcards put back in postion */
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < tlist.size(); i++) {
                sb.append((String) tlist.get(i));
                if (wlist != null && wlist.size() > i) {
                    sb.append(((Character) wlist.get(i)).charValue());
                }
            }
            return super.getWildcardQuery(field, sb.toString());
        }
    }
    
    /**
     * Called when parser parses an input term
     * token that uses prefix notation; that is, contains a single '*' wildcard
     * character as its last character. Since this is a special case
     * of generic wildcard term, and such a query can be optimized easily,
     * this usually results in a different query object.
     * <p>
     * Depending on analyzer and settings, a prefix term may (most probably will)
     * be lower-cased automatically. It <b>will</b> go through the default Analyzer.
     * <p>
     * Overrides super class, by passing terms through analyzer.
     *
     * @param  field   Name of the field query will use.
     * @param  termStr Term token to use for building term for the query
     *                 (<b>without</b> trailing '*' character!)
     *
     * @return Resulting {@link Query} built for the term
     * @throws ParseException throw in overridden method to disallow
     */
    protected Query getPrefixQuery(String field, String termStr)
        throws ParseException
    {
        // get Analyzer from superclass and tokenize the term
        TokenStream source = getAnalyzer().tokenStream(field,
                                                       new StringReader(termStr));
        ArrayList tlist = new ArrayList();
        org.apache.lucene.analysis.Token t;
        
        while (true) {
            try {
                t = source.next();
            } catch (IOException e) {
                t = null;
            }
            if (t == null) {
                break;
            }
            tlist.add(t.termText());
        }
        
        try {
            source.close();
        } catch (IOException e) {
            // ignore
        }
        
        if (tlist.size() == 0) {
            return null;
        } else if (tlist.size() == 1) {
            return super.getPrefixQuery(field, (String) tlist.get(0));
        } else {
            // FIXME: now what? throw a ParseException??
            // this *should* not happen, if it does it is not really a prefix query!
            throw new ParseException("PrefixQuery has multiple (" + tlist.size() + ") terms!");
        }
    }
    
}
