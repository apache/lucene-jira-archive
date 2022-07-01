package org.apache.lucene.queryParser;

import java.util.Locale;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.search.Query;

public class TestQueryParserLocaleOperators extends TestCase
{
    public void testOR() throws Exception {
        verify(Locale.ENGLISH, "a AND b", "+a +b", "+a +b");
        verify(Locale.US, "a AND b", "+a +b", "+a +b");
        verify(Locale.UK, "a AND b", "+a +b", "+a +b");
        verify(Locale.CANADA_FRENCH, "a OU b", "a b", "a ou b");
        verify(Locale.CANADA_FRENCH, "a OR b", "a b", "a b");
        verify(Locale.FRENCH, "a OU b", "a b", "a ou b");
        verify(Locale.FRENCH, "a OR b", "a b", "a b");
    }

    public void testAND() throws Exception {
        verify(Locale.ENGLISH, "a AND b", "+a +b", "+a +b");
        verify(Locale.US, "a AND b", "+a +b", "+a +b");
        verify(Locale.UK, "a AND b", "+a +b", "+a +b");
        verify(Locale.CANADA_FRENCH, "a ET b", "+a +b", "a et b");
        verify(Locale.CANADA_FRENCH, "a AND b", "+a +b", "+a +b");
        verify(Locale.FRENCH, "a ET b", "+a +b", "a et b");
        verify(Locale.FRENCH, "a AND b", "+a +b", "+a +b");
    }

    public void testNOT() throws Exception {
        verify(Locale.ENGLISH, "a NOT b", "a -b", "a -b");
        verify(Locale.US, "a NOT b", "a -b", "a -b");
        verify(Locale.UK, "a NOT b", "a -b", "a -b");
        verify(Locale.CANADA_FRENCH, "a SAUF b", "a -b", "a sauf b");
        verify(Locale.CANADA_FRENCH, "a NOT b", "a -b", "a -b");
        verify(Locale.FRENCH, "a SAUF b", "a -b", "a sauf b");
        verify(Locale.FRENCH, "a NOT b", "a -b", "a -b");
    }

    public void verify(Locale locale, String userString, String actif, String inactif) throws Exception {
        QueryParser qp = getParser(null);
        qp.setLocale(locale);

        assertQueryEquals(qp, false, userString, inactif);
        assertQueryEquals(qp, true, userString, actif);
    }

    public void assertQueryEquals(QueryParser qp, boolean useLocalizedOperator, String query, String result) throws ParseException
    {
        qp.setUseLocalizedOperators(useLocalizedOperator);

        Query q = qp.parse(query);
        String s = q.toString("field");

        if (!s.equals(result)) {
            fail("Query /" + query + "/ yielded /" + s
                 + "/, expecting /" + result + "/");
          }
    }

    public QueryParser getParser(Analyzer a) throws Exception {
        if (a == null)
          a = new SimpleAnalyzer();
        QueryParser qp = new QueryParser("field", a);
        qp.setDefaultOperator(QueryParser.OR_OPERATOR);
        return qp;
      }

}
