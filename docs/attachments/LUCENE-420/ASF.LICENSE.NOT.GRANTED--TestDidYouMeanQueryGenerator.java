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
import java.util.List;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.store.*;
// import org.apache.lucene.search.*; // need this package if class moves


/**
 *
 * @author  Ronnie Kolehmainen (ronnie.kolehmainen at ub.uu.se)
 * @version $Revision$, $Date$
 */
public class TestDidYouMeanQueryGenerator
    extends TestCase
{
    Directory     directory;
    Analyzer      analyzer;
    IndexReader   reader;
    IndexSearcher searcher;
    IndexWriter   writer;
    String[]      documents;
    String[]      moreHitsQueries;
    String[]      noMoreHitsQueries;
    String[]      spellingQueries;

    public void setUp()
    {
        documents = new String[] {
            "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Etiam turpis orci, sollicitudin in, ornare eu, mattis id, nunc. Praesent pulvinar consequat orci. Suspendisse arcu. In venenatis est eu lectus. Donec eget mi. Aenean tristique, lectus a scelerisque commodo, enim est vehicula enim, vitae iaculis metus eros eu felis. Vivamus vitae quam. Quisque sodales eros eu tortor. Morbi ultricies bibendum urna. Duis nunc orci, sodales id, vestibulum vel, bibendum tincidunt, lorem. Proin sed ante at mauris consequat ultricies. Mauris quis lorem vitae eros faucibus egestas. Curabitur iaculis, ligula ac convallis convallis, pede sem iaculis tortor, ac luctus sapien tortor sit amet neque. Ut justo nunc, faucibus a, facilisis vel, ultrices eget, nibh. Aenean nibh. Mauris molestie, ante at placerat adipiscing, lorem erat tempus erat, ut placerat lacus urna sit amet augue. Aliquam pharetra commodo eros. In eget eros. Duis rutrum wisi convallis lorem. Nulla urna mauris, suscipit id, faucibus sed, porta vitae, dolor.",
            "Aenean laoreet. Nullam suscipit bibendum massa. Donec eu pede. Sed laoreet justo in diam. Morbi volutpat. Vivamus tincidunt neque. Etiam accumsan, urna vitae adipiscing dictum, nisl mauris luctus arcu, ut viverra metus tortor vitae metus. Donec blandit cursus nibh. Mauris condimentum magna. Pellentesque ipsum massa, hendrerit sit amet, commodo a, accumsan non, augue. Donec risus mauris, lobortis ut, facilisis sed, rutrum id, ipsum. Suspendisse potenti. Maecenas euismod auctor justo. Curabitur id justo. Integer pellentesque volutpat lacus. Maecenas posuere. Morbi leo mi, blandit tincidunt, viverra dapibus, luctus ac, ipsum. Donec a orci id lacus bibendum gravida. Nam euismod risus eu diam. Praesent dapibus augue quis leo feugiat vestibulum.",
            "Ut sit amet pede laoreet dolor euismod ullamcorper. Cras arcu lacus, ornare ac, tincidunt eu, fermentum id, metus. Aenean congue neque id elit. Nam vehicula odio sed diam. Nullam vitae orci ac diam egestas iaculis. Pellentesque magna elit, pellentesque ac, venenatis in, imperdiet convallis, elit. Integer rutrum tincidunt diam. Nunc nec augue. Integer tempus lorem in elit. Vivamus sed magna.",
            "Nulla facilisi. Cras malesuada, magna fringilla consectetuer, arcu metus vulputate dolor, at cursus arcu tortor at enim. Proin mattis fermentum urna. Morbi eu neque in velit viverra fermentum. Proin sed nulla. Vestibulum pharetra luctus risus. Ut et mauris et nunc convallis facilisis. Nulla mollis, magna nec pretium vehicula, lacus turpis dictum purus, a varius magna quam ut erat. Phasellus non ligula. In hac habitasse platea dictumst. In hac habitasse platea dictumst. Quisque nibh lacus, condimentum eget, egestas sit amet, lacinia auctor, augue. Nulla et tortor. Mauris condimentum metus ac nibh.",
            "Morbi at purus. Proin feugiat adipiscing lectus. Morbi purus erat, mollis at, iaculis non, tempor non, eros. Proin in sapien. Phasellus lacinia est. Aliquam et eros vel erat imperdiet tincidunt. Duis quis nibh. Integer rhoncus sapien. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Phasellus lectus enim, placerat varius, pretium non, commodo vel, lectus. Phasellus adipiscing accumsan odio. Morbi dictum pede sed urna nulla. Vivamus nibh pede, egestas id, hendrerit a, varius fringilla, risus. Suspendisse ut est vitae odio euismod ultricies. Suspendisse at sapien sit amet wisi congue lobortis. In nibh augue, porttitor aliquet, pulvinar non, gravida nec, felis."
        };
        moreHitsQueries = new String[] {
            "nullam",          // "nullam" in 2 docs, "nulla"  in 3 docs
            "dictumst",        // "dictumst" in 1 doc, "dictum" in 3 docs
            "Pellentescue",    // "Pellentescue" in 0 docs, "pellentesque" in 2 docs
            "lorrem AND (ipsum OR (rutrum AND wisi*))", // 1 doc
            "lorrem AND (ippsum AND (rutrum AND wisi*))" // 1 doc
        };
        noMoreHitsQueries = new String[] {
            "lorem",           // "lorem" in 2 docs
            "ipsum",           // "ipsum" in 3 docs
            "lorem ipsum",     // "lorem OR ipsum" in 4 docs
            "lorem AND ipsum", // "lorem AND ipsum" in 1 doc
            "lorem AND (ipsum OR (rutrum AND wisi*))", // 1 doc
            // following types of queries are not handled by DidYouMeanQueryGenerator
            "\"lorem ipsum\"", // "\"lorem ipsum\"" in 1 doc (phrase query)
            "lore~",           // "lore~" in 2 docs (fuzzy query)
            "lore*",           // "lore*" in 2 docs (prefix query)
            "[l TO m]"         // "[l TO m]" in 5 docs (range query)
        };
        spellingQueries = new String[] {
            "nulla",           // "nulla" in 3 docs, "nullam" in 2 docs
            "dictum",          // "dictum" in 3 docs", "dictumst" in 1 doc 
            "lorem AND (ispum OR (rutrum AND wisi*))" // 1 doc
        };

        try {
            directory = new RAMDirectory();  
            analyzer = new StandardAnalyzer();
            writer = new IndexWriter(directory, analyzer, true);
            
            for (int i = 0; i < documents.length; i++) {
                Document d = new Document();
                d.add(Field.Text("contents", documents[i]));
                writer.addDocument(d);
            }
            writer.close();
            
            reader = IndexReader.open(directory);
            searcher = new IndexSearcher(reader);
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }

    public void testMoreHitsSuggestion()
    {
        Query originalQuery;
        Query suggestedQuery;
        Hits hits;
        int originalHits;
        int suggestedHits;
        DidYouMeanQueryGenerator generator;

        System.out.println(" ");
        System.out.println("** Give suggestion **");
        for (int i = 0; i < moreHitsQueries.length; i++) {
        System.out.println(" ");
            try {
                originalQuery = QueryParser.parse(moreHitsQueries[i], "contents", analyzer);
                hits = searcher.search(originalQuery);
                originalHits = hits.length();
                System.out.println("Original:   " + originalQuery.toString("contents")
                                   + " : " + originalHits + " hits");
                generator = new DidYouMeanQueryGenerator(originalQuery, reader);
                suggestedQuery = generator.getQuerySuggestion(true, true);
                assertFalse("Got query suggestion", originalQuery.equals(suggestedQuery));
                hits = searcher.search(suggestedQuery);
                suggestedHits = hits.length();
                System.out.println("Suggestion: " + suggestedQuery.toString("contents")
                                   + " : " + suggestedHits + " hits");
                assertTrue("Suggestion (" + suggestedQuery.toString("contents")
                           + ") got more hits", 
                           suggestedHits > originalHits);
            } catch (ParseException pe) {
                fail(pe.toString());
            } catch (IOException ioe) {
                fail(ioe.toString());
            } 
        }   
    }

    public void testNoMoreHitsSuggestion()
    {
        Query originalQuery;
        Query suggestedQuery;
        Hits hits;
        int originalHits;
        int suggestedHits;
        DidYouMeanQueryGenerator generator;

        System.out.println(" ");
        System.out.println("** Do not give suggestion **");
        for (int i = 0; i < noMoreHitsQueries.length; i++) {
        System.out.println(" ");
            try {
                originalQuery = QueryParser.parse(noMoreHitsQueries[i], "contents", analyzer);
                hits = searcher.search(originalQuery);
                originalHits = hits.length();
                System.out.println("Original:   " + originalQuery.toString("contents")
                                   + " : " + originalHits + " hits");
                generator = new DidYouMeanQueryGenerator(originalQuery, reader);
                suggestedQuery = generator.getQuerySuggestion(true, true);
                System.out.println("Suggestion: " + suggestedQuery.toString("contents"));
                assertTrue("Got no query suggestion", originalQuery.equals(suggestedQuery));
            } catch (ParseException pe) {
                fail(pe.toString());
            } catch (IOException ioe) {
                fail(ioe.toString());
            } 
        }   
    }

    public void testSpellingSuggestion()
    {
        Query originalQuery;
        Query suggestedQuery;
        Hits hits;
        int originalHits;
        int suggestedHits;
        DidYouMeanQueryGenerator generator;

        System.out.println(" ");
        System.out.println("** Give spell suggestion **");
        for (int i = 0; i < spellingQueries.length; i++) {
        System.out.println(" ");
            try {
                originalQuery = QueryParser.parse(spellingQueries[i], "contents", analyzer);
                hits = searcher.search(originalQuery);
                originalHits = hits.length();
                System.out.println("Original:   " + originalQuery.toString("contents")
                                   + " : " + originalHits + " hits");
                generator = new DidYouMeanQueryGenerator(originalQuery, reader);
                suggestedQuery = generator.getQuerySuggestion(false, true);
                assertFalse("Got spell suggestion", originalQuery.equals(suggestedQuery));
                hits = searcher.search(suggestedQuery);
                suggestedHits = hits.length();
                System.out.println("Suggestion: " + suggestedQuery.toString("contents")
                                   + " : " + suggestedHits + " hits");
            } catch (ParseException pe) {
                fail(pe.toString());
            } catch (IOException ioe) {
                fail(ioe.toString());
            } 
        }   
    }

    public void tearDown()
    {
        try {
            searcher.close();
            reader.close();
            directory.close();
        } catch (IOException ioe) {
            fail(ioe.toString());
        }
    }
}
