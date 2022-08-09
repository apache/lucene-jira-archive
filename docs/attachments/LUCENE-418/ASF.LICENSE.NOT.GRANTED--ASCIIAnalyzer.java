/* $Id$ */

/**
 * Copyright 2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.Reader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 * Filters {@link StandardTokenizer} with {@link StandardFilter},
 * {@link ASCIIFilter} and {@link LowerCaseFilter} (in that order).
 *
 * @author  <a href="mailto:ronnie.kolehmainen@ub.uu.se">Ronnie Kolehmainen</a>
 * @version $Revision$, $Date$
 */
public class ASCIIAnalyzer 
    extends org.apache.lucene.analysis.Analyzer
{
    /**
     * Builds an analyzer.
     */
    public ASCIIAnalyzer()
    {
    }
    
    /**
     * Constructs a {@link StandardTokenizer} filtered by a {@link StandardFilter},
     * a {@link ASCIIFilter} and a {@link LowerCaseFilter} (in that order).
     * @param  fieldName <i>not implemented</i>
     * @param  reader    the input source
     * @return a tokenized stream from the reader.
     */
    public TokenStream tokenStream(String fieldName, Reader reader)
    {
        TokenStream result = new StandardTokenizer(reader);
        
        /*
         * Removes <tt>'s</tt> from the end of words.
         * Removes dots from acronyms.
         */
        result = new StandardFilter(result);
        
        /*
         * Normalizes umlauts and diacretes to their closest
         * ASCII equivalents.
         */
        result = new ASCIIFilter(result);
        
        /*
         * Normalizes token text to lower case.
         */
        result = new LowerCaseFilter(result);
        
        return result;
    }
    
    /**
     * Test method. Creates a new {@link QueryParser} with this
     * analyzer, parses args[0] and outputs the resulting query.
     * @param  args where args[0] is query string to be parsed.
     */
    public static void main(String[] args)
        throws Exception
    {
        Analyzer a = new ASCIIAnalyzer();
        org.apache.lucene.search.Query q =
            org.apache.lucene.queryParser.QueryParser.parse(args[0],
                                                            "field",
                                                            a);
        System.out.println("Analyzer:  " + a.getClass().getName());
        System.out.println("Queryimpl: " + q.getClass().getName());
        System.out.println("query:     " + q.toString("field"));
    }
    
}
