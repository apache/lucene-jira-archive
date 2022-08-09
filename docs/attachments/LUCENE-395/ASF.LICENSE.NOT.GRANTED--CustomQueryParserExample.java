package org.apache.lucene.queryParser;

import java.io.Reader;
import java.util.Vector;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.CoordConstrainedBooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;


/**This class extends QueryParser to allow a CoordConstrainedBooleanQuery to be defined as part 
 * of the query syntax.
 * 
 * This demonstrates the potential of a QueryParser that allows user-defined functions to
 * receive parameters defined in the query syntax. Proper support for this in the QueryParser would
 * allow custom parser extensions to be created without needing to modify the javacc 
 * of the existing parser. Custom query factories could be associated with function names found in
 * the query strings and the parser could pass the embedded properties and child queries etc
 * More thought required on such a framework.... 
 * @author maharwood
 */
public class CustomQueryParserExample extends QueryParser
{
    //base class constructors
    public CustomQueryParserExample(CharStream stream) 		{ super(stream);}
    public CustomQueryParserExample(QueryParserTokenManager tm){super(tm);  }
    public CustomQueryParserExample(String f, Analyzer a) 		{super(f, new CustomParamsAnalyzer(a));}

    //Example use
    public static void main(String[] args) throws Exception
    {
        IndexSearcher is=new IndexSearcher("d:/indexes/uniqueenronemails");
        QueryParser qp=new CustomQueryParserExample("contents",new StandardAnalyzer());
        
        //Note (mis)use of fieldname "min_coord" to pass the required parameter - a more formal
        //support for passing query function_names and parameters to user-defined query factories 
        //would be a very useful addition to the QueryParser base class
        
        Query q=qp.parse("ljm AND fuzzyFromOrTo:(min_coord:2 thome buy shackleton smith kaminski crenshaw )");
        
        Hits hits=is.search(q);
        System.out.println(q);
        System.out.println(hits.length());
        is.close();
    }
        
    protected Query getBooleanQuery(Vector clauses, boolean disableCoord) throws ParseException
    {
        BooleanClause bc=(BooleanClause) clauses.get(0);
        Query firstQuery=bc.getQuery();
        if(firstQuery instanceof TermQuery)
        {
            TermQuery tq=(TermQuery) firstQuery;
            Term t=tq.getTerm();
            if(t.field().equals("min_coord"))
            {
                int minCoord=Integer.parseInt(t.text());
                CoordConstrainedBooleanQuery query = new CoordConstrainedBooleanQuery(minCoord); 
                for (int i = 1; i < clauses.size(); i++) {
                    query.add((BooleanClause)clauses.elementAt(i));
                }

                return query;                
            }
        }
        return  super.getBooleanQuery(clauses,disableCoord);
    }
    //used to preserve the numbers passed as parameters in the query string
    static class CustomParamsAnalyzer extends Analyzer
    {
        Analyzer fieldAnalyzer;
        Analyzer paramsAnalyzer;
        public CustomParamsAnalyzer(Analyzer a)
        {
            fieldAnalyzer=a;
            paramsAnalyzer=new WhitespaceAnalyzer();
        }
        public TokenStream tokenStream(String fieldName, Reader reader) 
        {           
            Analyzer analyzer=fieldAnalyzer;
            if("min_coord".equals(fieldName))
            {
                analyzer=paramsAnalyzer;                
            }
            return analyzer.tokenStream(fieldName,reader);
    	}        
    }
    
}
