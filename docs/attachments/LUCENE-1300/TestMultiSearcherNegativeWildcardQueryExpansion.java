package org.apache.lucene.search;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.LuceneTestCase;

/**
 * 
 * Bug summary: Negative wildcard searches on MultiSearcher not eliminating correctly.
 * 
 * Bug description:
 * 
 * If you do a search for a negative wildcard query on a MultiSearcher where one of the 
 * searchers is empty e.g. "lucene -bug*" the hits returned incorrectly include articles 
 * with words that should be eliminated, e.g. "bug" and "bugs".  This is because the 
 * query expansion is done on the index with docs in and the empty index *separately* and then
 * combined as an OR to be run on the MultiSearcher.  This incorrectly lets in docs that have the
 * excluded wildcard terms, e.g. "bug" and "bugs".
 * This bug would also show up with two indexes full of docs, and I can send a test to show that if
 * required, but I think this test demonstrates the bug in the simplest way.
 * 
 * The attached class TestMultiSearcherNegativeWildcardQueryExpansion.java can be put in with 
 * other tests in org.apache.lucene.search and run and will fail, showing the bug exists.
 * 
 * I have tested this bug with the currently unrelease 2.3.2 and the released 2.3.1.
 * 
 * This class demonstrates a bug with MultiSearcher query expansion on negative wildcard searches.
 * 
 * See the explanation above the failing testNegativeWildcardSearchOnMultiSearcher.
 * 
 */
public class TestMultiSearcherNegativeWildcardQueryExpansion extends
    LuceneTestCase {
  private MultiSearcher multiSearcher;

  private IndexSearcher indexSearcher1, indexSearcher2;

  public TestMultiSearcherNegativeWildcardQueryExpansion(String name) {
    super(name);
  }

  private static final String defaultFieldName = "contents";

  private static Document createSingleFieldDoc(String contents) {
    Document document = new Document();
    document.add(new Field(defaultFieldName, contents, Field.Store.YES,
        Field.Index.TOKENIZED));
    return document;
  }

  /*
   * Creates two IndexSearchers and a MultiSearcher.
   * indexWriter1 has three docs in it, and indexWriter2 is empty.
   * 
   */
  public void setUp() throws Exception {
    super.setUp();
    RAMDirectory ramDirectory1, ramDirectory2;

    ramDirectory1 = new RAMDirectory();
    ramDirectory2 = new RAMDirectory();

    boolean create = true;
    IndexWriter indexWriter1 = null;
    IndexWriter indexWriter2 = null;
    try {
      indexWriter1 = new IndexWriter(ramDirectory1, new StandardAnalyzer(),
          create);
      indexWriter1
          .addDocument(createSingleFieldDoc("lucene is great"));
      indexWriter1
          .addDocument(createSingleFieldDoc("lucene coders are good at fixing bugs :-)"));
      indexWriter1
          .addDocument(createSingleFieldDoc("lucene has a bug, oh dear"));
      indexWriter2 = new IndexWriter(ramDirectory2, new StandardAnalyzer(),
          create);
    } finally {
      indexWriter1.close();
      indexWriter2.close();
    }

    indexSearcher1 = new IndexSearcher(ramDirectory1);
    indexSearcher2 = new IndexSearcher(ramDirectory2);

    multiSearcher = new MultiSearcher((new Searcher[] { indexSearcher1,
        indexSearcher2 }));
  }

  /*
   * This test passes fine, demonstrating that 
   * as expected if you search across the multisearcher for
   * "lucene" you will get three hits.
   */
  public void testPositiveSearchOnMultisearcher() throws Exception {
    String queryString = "lucene";
    Hits hits = getHitsForSearch(multiSearcher, queryString);
    assertTrue("search for lucene should return 3 docs (and does)", hits
        .length() == 3);
  }

  /*
   * This test also passes fine, demonstrating that
   * if you search for "lucene -bug*" on the first IndexSearcher
   * you will only get one result as the two docs with "bug" and "bugs"
   * in them get eliminated.
   */
  public void testNegativeWildcardSearchOnIndexSearcher() throws Exception {
    String queryString = "lucene -bug*";
    Hits hits = getHitsForSearch(indexSearcher1, queryString);
    assertTrue(
        "search for lucene -bug* on the first indexSearcher1 should " +
        "return only one doc (and does)",
        hits.length() == 1);
  }

  /*
   * 
   * This test fails, because we run the same "lucene -bug*" query
   * as last time, but just on the MultiSearcher instead of the first
   * IndexSearcher and we expect the same single result, but we get all
   * three docs matched, which is wrong.
   * 
   * This demonstrates the bug that occurs on negative expanded queries on 
   * MultiSearchers.
   * 
   * This is because it runs the query:-
   * 
   *  contents:lucene -contents:bug*
   *  
   *  which gets rewritten to:-
   *  
   *  (contents:lucene -()) (contents:lucene -(contents:bug contents:bugs))
   *  
   *  (you can see that by looking at your console output)
   *  
   *  As you can see this rewritten query is a combination of the 
   *  rewritten query from the empty index:-
   *  (contents:lucene -()) 
   *  and the rewritten query from the index with three stories in it:-
   *  (contents:lucene -(contents:bug contents:bugs))
   *  
   *  but the resulting rewritten query when it is run
   *   across both indexes  will find all docs with
   *  "lucene" in them but NOT eliminate all docs with 
   *  words starting with "bug"
   * 
   */
  public void testNegativeWildcardSearchOnMultiSearcher() throws Exception {
    String queryString = "lucene -bug*";
    Hits hits = getHitsForSearch(multiSearcher, queryString);
    assertTrue(
        "ALERT, ALERT, same search for lucene -bug* on multisearcher gives " +
        "3 results, when it should be 1 like it was on " +
        "the single searcher!!!", hits
            .length() == 1);
  }

  private Hits getHitsForSearch(Searcher searcher, String queryString)
      throws ParseException, IOException {
    QueryParser queryParser = new QueryParser(defaultFieldName,
        new StandardAnalyzer());
    Query query = queryParser.parse(queryString);
    System.out.println("Using queryString = " + queryString);
    System.out.println("query.toString() = " + query.toString());
    Query rewrittenQuery = searcher.rewrite(query);
    System.out.println("rewrittenQuery.toString() = "
        + rewrittenQuery.toString() + "\n");

    Hits hits = searcher.search(query);
    return hits;
  }

}
