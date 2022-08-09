package org.apache.lucene.search;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
// import org.apache.lucene.search.*; // need this package if class moves

/**
 * <p>Generates alternative queries by examining term(s) from a TermQuery
 * or a BooleanQuery with TermQuery's, and determines if a term can be 
 * replaced with a similar spelled term which can produce more hits.
 * <p>Example usage (pseudo-code):
 * <pre>
 * Query originalQuery = QueryParser.parse(queryString, &quot;contents&quot;, analyzer);
 * Hits hits = searcher.search(originalQuery);
 *
 * if (hits.length() &lt; SUGGESTION_THRESHOLD) {
 *     DidYouMeanQueryGenerator generator = new DidYouMeanQueryGenerator(originalQuery, reader);
 *     Query alternativeQuery = generator.getQuerySuggestion(true, true);
 *     if (alternativeQuery.equals(originalQuery)) {
 *         // no better alternative found...
 *     } else {
 *         // an alternate query was generated, do something with it
 *         String suggestion = alternativeQuery.toString(&quot;contents&quot;);
 *         out.write(&quot;Did you mean: &lt;a href='search?q=&quot; + suggestion + &quot;'&gt;&quot;
 *                   + suggestion + &quot;&lt;/a&gt;?&quot;);
 *     }
 * }
 *
 * // do something with hits...
 * </pre>
 *
 * @author  Ronnie Kolehmainen (ronnie.kolehmainen at ub.uu.se)
 * @version $Revision$, $Date$
 */
public class DidYouMeanQueryGenerator
{
    private Query originalQuery;
    private IndexReader reader;


    /**
     * Constructor with {@link Query} and {@link IndexReader}.
     * @param  query  the query
     * @param  reader the Indexreader
     */
    public DidYouMeanQueryGenerator(Query query, IndexReader reader)
    {
        this.originalQuery = query;
        this.reader = reader;
    }
    

    /**
     * Tries to find a query which will give more hits using the {@link FuzzyQuery}
     * algorithm (<a href="http://www.levenshtein.net/">Levenshtein distance</a>)
     * along with document frequency for a term.
     * @param  useFreqCount    Set to <tt>true</tt> to only replace terms which
     *                         have a higher document frequency count (and would
     *                         give more hits) than the replaced original term.
     * @param  useDefaultBoost Set to <tt>true</tt> to give all replaced term queries
     *                         a boost of 1.0f instead of the boost computed from
     *                         the edit distance (suitable for human-readable
     *                         query.toString(field) result).
     * @return If and only if a more suitable query can be calculated, a new {@link Query}
     *         is returned. In all other cases the original query is returned.
     */
    public Query getQuerySuggestion(boolean useFreqCount, boolean useDefaultBoost)
        throws IOException
    {
        if (originalQuery instanceof TermQuery) {
            TermQuery returnQuery = getBetterTermQuery((TermQuery) originalQuery,
                                                       useFreqCount,
                                                       useDefaultBoost);
            return returnQuery != null ? returnQuery : originalQuery;
        } else if (originalQuery instanceof BooleanQuery) {
            BooleanQuery returnQuery = recursiveExtract((BooleanQuery) originalQuery,
                                                        useFreqCount,
                                                        useDefaultBoost);
            return returnQuery != null ? returnQuery : originalQuery;
        }
        return originalQuery;
    }


    /**
     * Recursive extraction of the BooleanQuery in order to find all TermQuery's.
     */
    private BooleanQuery recursiveExtract(BooleanQuery bq, 
                                          boolean useFreqCount,
                                          boolean useDefaultBoost)
        throws IOException
    {
        BooleanClause[] clauses = bq.getClauses();
        BooleanQuery returnQuery = new BooleanQuery();
        for (int i = 0; i < clauses.length; i++) {
            // REVISIT: use isProhibited() and getQuery() in the future, public
            //          members prohibited and query were deprecated in revision 150457
            if (!clauses[i].prohibited && clauses[i].query instanceof BooleanQuery) {
                returnQuery.add(recursiveExtract((BooleanQuery) clauses[i].query,
                                                 useFreqCount,
                                                 useDefaultBoost),
                                clauses[i].required,
                                clauses[i].prohibited);
                continue;
            } else if (!clauses[i].prohibited && clauses[i].query instanceof TermQuery) {
                TermQuery newTermQuery = getBetterTermQuery((TermQuery) clauses[i].query,
                                                            useFreqCount,
                                                            useDefaultBoost);
                if (newTermQuery != null) {
                    returnQuery.add(newTermQuery, clauses[i].required, clauses[i].prohibited);
                    continue;
                }
            }
            returnQuery.add(clauses[i]);
        }
        return returnQuery;
    }
        

    /**
     * This is where the magic happens.
     */
    private TermQuery getBetterTermQuery(TermQuery tq, 
                                         boolean useFreqCount, 
                                         boolean useDefaultBoost)
        throws IOException
    {
        OrderedTermsFuzzyQuery fq =
            new OrderedTermsFuzzyQuery(tq.getTerm());
        List list = fq.bestOrderRewrite(reader, useFreqCount);
        if (list.size() > 0) {
            return modifyBoost((TermQuery) list.get(0), useDefaultBoost);
        }
        return null;
    }


    private TermQuery modifyBoost(TermQuery tq, boolean removeComputedBoost)
    {
        if (removeComputedBoost) {
            tq.setBoost(1.0f);
        }
        return tq;
    }


    /**
     * A sibling of the FuzzyQuery class with the ability to give a score sorted
     * list of fuzzy term queries.
     */
    private class OrderedTermsFuzzyQuery
        extends MultiTermQuery
    {
        private Term term;

        private OrderedTermsFuzzyQuery(Term term){
            super(term);
            this.term = term;
        }

        protected FilteredTermEnum getEnum(IndexReader reader)
            throws IOException
        {
            return new FuzzyTermEnum(reader, getTerm());
        }
        
        public String toString(String field)
        {
            return super.toString(field) + '~';
        }
        
        /**
         * Computes an ordered list of alternative terms, ordered by edit distance.
         * @param  reader       the IndexReader
         * @param  useFreqOrder if <tt>true</tt>, return list will only contain
         *                      terms which will give more hits
         * @return List<TermQuery>
         */
        List bestOrderRewrite(IndexReader reader, boolean useFreqOrder)
            throws IOException
        {
            int originalDocFreq = useFreqOrder ? reader.docFreq(term) : 0;
            ArrayList terms = new ArrayList();
            FilteredTermEnum enumerator = getEnum(reader);
            try {
                do {
                    Term t = enumerator.term();
                    if (t != null && !t.text().equals(term.text())
                        && (!useFreqOrder || (reader.docFreq(t) > originalDocFreq))) {
                        // found a match
                        TermQuery tq = new TermQuery(t);
                        // set the boost
                        // FilteredTermEnum.difference() was protected in revisions < 150572,
                        // forcing this class to be in same package if not used with
                        // Lucene revision >= 150572
                        tq.setBoost(getBoost() * enumerator.difference());
                        // add to query
                        terms.add(tq);
                    }
                } while (enumerator.next());
            } finally {
                enumerator.close();
            }
            Collections.sort(terms, new FuzzyTermScoreComparator(term));
            return terms;
        }
    }
    

    /**
     * Sorts terms by their boost. If two terms have equal boost, their
     * length of the shared base is compared to the original term. That is,
     * the more consecutive chars equal to the original term, the higher the rank.
     */
    private class FuzzyTermScoreComparator
        implements Comparator
    {
        private Term term;
        private String t, t1, t2;
        
        public FuzzyTermScoreComparator(Term term)
        {
            this.term = term;
        }
        
        public int compare(Object o1, Object o2)
        {
            // compare boosts
            float f1 = ((TermQuery) o1).getBoost();
            float f2 = ((TermQuery) o2).getBoost();
            if (f1 == f2) {
                // equal score, now compare distance
                t1 = ((TermQuery) o1).getTerm().text();
                t2 = ((TermQuery) o2).getTerm().text();
                t = term.text();
                // double extra safe check
                if (t == null || t1 == null || t2 == null) {
                    return 0;
                }
                for (int i = 0; i < t.length(); i++) {
                    if (i == t1.length() || i == t2.length()) {
                        // out of bounds in either term
                        return 0;
                    }
                    if (t1.charAt(i) == t2.charAt(i)) {
                        // equal so far, continue
                        continue;
                    }
                    // now the terms have different chars
                    if (t1.charAt(i) == t.charAt(i)) {
                        return -1;
                    } else if (t2.charAt(i) == t.charAt(i)) {
                        return 1;
                    }
                    // both terms had different char than the original term...try next
                }
                return 0;
            }
            return f1 < f2 ? 1 : -1;
        }
    }

}
