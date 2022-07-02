package org.apache.lucene.search;

/**
 * Copyright 2004 The Apache Software Foundation
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

import junit.framework.TestCase;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;

/** Regression test to cover issue Lucene-72
 *
 * @author Dejan Nenov
 * @version
 */
public class TestRegressionLucene72 extends TestCase {

  private RAMDirectory dir;
  private int docCount;
  private  Analyzer theAnalyzer = new WhitespaceAnalyzer();
  private IndexSearcher searcher;
  private String contentFieldName = "c";
  private String idFieldName = "id";


  public void setUp() throws Exception {
    dir = new RAMDirectory();
    //setup hte data in the index
    initializeIndex(new String[] {"fruits vegetables tomatoes bananas"});
    addDoc("fruits vegetables tomatoes kiwis");
    addDoc("fruits vegetables peppers kiwis");
    addDoc("fruits vegetables peppers bananas");
    searcher = new IndexSearcher(dir);
  }

    public void tearDown() throws Exception {
        searcher.close();
        dir.close();
    }

    // initializeIndex, addDoc and insertDoc borrowed directly from
    //TestRanegQuery @author goller
     private void initializeIndex(String[] values) throws IOException {
    IndexWriter writer = new IndexWriter(dir, theAnalyzer, true);
     for (int i = 0; i < values.length; i++) {
      insertDoc(writer, values[i]);
    }
    writer.close();
  }

  private void addDoc(String content) throws IOException {
    IndexWriter writer = new IndexWriter(dir, theAnalyzer, false);
    insertDoc(writer, content);
    writer.close();
  }

  private void insertDoc(IndexWriter writer, String content) throws IOException {
    Document doc = new Document();

    doc.add(new Field(idFieldName, idFieldName + docCount, Field.Store.YES, Field.Index.UN_TOKENIZED));
    doc.add(new Field(contentFieldName, content, Field.Store.YES, Field.Index.TOKENIZED));

    writer.addDocument(doc);
    docCount++;
  }

    // getParser, getQuery and assertQueryEquals borrowed from org.apache.lucene.queryParser.TestQueryParser

    public QueryParser getParser(Analyzer a) throws Exception {
    if (a == null)
      a = theAnalyzer;
    QueryParser qp = new QueryParser(contentFieldName, a);
    qp.setDefaultOperator(QueryParser.OR_OPERATOR);
    return qp;
  }
     public Query getQuery(String query, Analyzer a) throws Exception {
    return getParser(a).parse(query);
  }
    public void assertQueryEquals(String query, Analyzer a, String result)
    throws Exception {
    Query q = getQuery(query, a);
    String s = q.toString(contentFieldName);
    if (!s.equals(result)) {
      fail("Query /" + query + "/ yielded /" + s + "/, expecting /" + result + "/");
    }
  }

// query string directly form the Lucene-72 issue:
  // the four documents in this test contain:
  //Doc1: "fruits vegetables tomatoes bananas"
  //Doc2: "fruits vegetables tomatoes kiwis"
  //Doc3: "fruits vegetables peppers kiwis"
  //Doc4: "fruits vegetables peppers bananas"


  public void testPrintCorpusAndQueries() {
  System.out.println("Iindex data and queries:\n" +
            "Doc1 = fruits vegetables tomatoes bananas\n" +
            "Doc2 = fruits vegetables tomatoes kiwis\n" +
            "Doc3 = fruits vegetables peppers kiwis\n" +
            "Doc4 = fruits vegetables peppers bananas\n" +
            "theFisrtS = fruits OR -tomatoes\n" +
            "theSimpleQueryS = fruits vegetables -tomatoes -bananas\n" +
            "theANDQueryS = +(fruits vegetables) AND (-tomatoes -bananas)\n" +
            "theNOTQueryS = +(fruits vegetables) NOT (tomatoes bananas)"
  );
}

  public void testFirst() throws Exception {
     //theSIMPLEquery should return only Doc3
    String theFirstS = "fruits OR -tomatoes";
    assertQueryEquals(theFirstS,
                        theAnalyzer,
                       "fruits -tomatoes");


    Query theQuery = getQuery(theFirstS, theAnalyzer);
    Hits hits = searcher.search(theQuery);
    String resultDocs = "";
    System.out.println("FirstQuery:" + theQuery + " hits.length=" + hits.length());
    for (int i = 0; i < hits.length(); i++) {
      System.out.println("FirstQuery:Doc[" + i + "]= "+ hits.doc(i).get(contentFieldName));
      resultDocs += hits.doc(i).get(contentFieldName);
    }

    assertEquals("resultDocs =" + resultDocs, 4, hits.length());

}

  public void testSIMPLE() throws Exception {

    //theSIMPLEquery should return only Doc3
    String theSimpleQueryS = "fruits vegetables -tomatoes -bananas";
    assertQueryEquals(theSimpleQueryS,
                        theAnalyzer,
                       "fruits vegetables -tomatoes -bananas");


    Query theQuery = getQuery(theSimpleQueryS, theAnalyzer);
    Hits hits = searcher.search(theQuery);
    String resultDocs = "";
    System.out.println("SimpleQuery:" + theQuery + " hits.length=" + hits.length());
    for (int i = 0; i < hits.length(); i++) {
      System.out.println("SimpleQuery:Doc[" + i + "]= "+ hits.doc(i).get(contentFieldName));
      resultDocs += hits.doc(i).get(contentFieldName);
    }

    assertEquals("resultDocs =" + resultDocs, 1, hits.length());
    }

  public void testAND() throws Exception {

    //theANDquery should return Doc2, Doc3, Doc4 - because the clause left of the AND returns all docs
    //   and the clause to the right of the AND returns all docs EXCEPT DOC1
    // Note that we have used QueryParser.OR_OPERATOR above
    //First test if the Query Parser does what we expect
    String theANDQueryS = "+(fruits vegetables) AND (-tomatoes -bananas)";
    assertQueryEquals(theANDQueryS,
                        theAnalyzer,
                        "+(fruits vegetables) +(-tomatoes -bananas)");

    Query theQuery = getQuery(theANDQueryS, theAnalyzer);
    Hits hits = searcher.search(theQuery);
    String resultDocs = "";
    System.out.println("ANDQuery:" + theQuery + " hits.length=" + hits.length());
    for (int i = 0; i < hits.length(); i++) {
      System.out.println("ANDQuery:Doc[" + i + "]= "+ hits.doc(i).get(contentFieldName));
      resultDocs += hits.doc(i).get(contentFieldName);
    }

    assertEquals("resultDocs = " + resultDocs,3, hits.length());

  }

  public void testNOT() throws Exception {

    //theNOTquery should return only Doc3
    String theNOTQueryS = "+(fruits vegetables) NOT (tomatoes bananas)";
    assertQueryEquals(theNOTQueryS, theAnalyzer, "+(fruits vegetables) -(tomatoes bananas)");

    Query theQuery = getQuery(theNOTQueryS, theAnalyzer);
    Hits hits = searcher.search(theQuery);
    String resultDocs = "";
    System.out.println("NOTQuery:" + theQuery + " hits.length=" + hits.length());
    for (int i = 0; i < hits.length(); i++) {
      System.out.println("NOTQuery:Doc[" + i + "]= "+ hits.doc(i).get(contentFieldName));
      resultDocs += hits.doc(i).get(contentFieldName);
    }

    assertEquals("resultDocs = " + resultDocs, 1, hits.length());


  }


}
