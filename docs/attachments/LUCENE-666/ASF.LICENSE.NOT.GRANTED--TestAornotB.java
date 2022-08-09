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
import org.apache.lucene.analysis.SimpleAnalyzer;
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
public class TestAornotB extends TestCase {

  private RAMDirectory dir;
  private int docCount;
  private Analyzer theAnalyzer = new SimpleAnalyzer();
  private IndexSearcher searcher;
  private String contentFieldName = "c";
  private String idFieldName = "id";


  public void setUp() throws Exception {
    dir = new RAMDirectory();
    //setup hte data in the index
    initializeIndex(new String[] {"A B C"});
    addDoc("A B C D");
    addDoc("A C D");
    addDoc("B C D");
    addDoc("C D");
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

    // getParser, getQuery from org.apache.lucene.queryParser.TestQueryParser

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

  public void testPrintCorpusAndQueries() {
  System.out.println(
            "Doc1 = A B C\n" +
            "Doc2 = A B C D\n" +
            "Doc3 = A   C D\n" +
            "Doc4 =   B C D\n" +
            "Doc5 =     C D\n" +
            "-------------------------------------------------\n" +
            "With query \"A OR NOT B\" we expect to hit\n" +
            "all documents EXCEPT Doc4, instead we only match on Doc3.\n" +
            "While LUCENE currently explicitly does not support queries of\n" +
            "the type \"find docs that do not contain TERM\" - this explains\n" +
            "not finding Doc5, but does not justify elimnating Doc1 and Doc2\n" +
            "-------------------------------------------------\n" +
            " the fix shoould likely require a modification to QueryParser.jj\n" +
            " around the method:\n" +
            " protected void addClause(Vector clauses, int conj, int mods, Query q)"
  );
}

  public void testFAIL() throws Exception {
     //theSIMPLEquery should return only Doc3
    String theQueryS = "A OR NOT B";

    Query theQuery = getQuery(theQueryS, theAnalyzer);
    Hits hits = searcher.search(theQuery);
    String resultDocs = "";
    System.out.println("Query:" + theQuery + " hits.length=" + hits.length());
    for (int i = 0; i < hits.length(); i++) {
      System.out.println("Query Found:Doc[" + i + "]= "+ hits.doc(i).get(contentFieldName));
      resultDocs += hits.doc(i).get(contentFieldName);
    }
    for (int i=0; i++ < 3;) {
      System.out.println(searcher.explain(theQuery,i));
    }

    assertEquals("resultDocs =" + resultDocs, 3, hits.length());

}

  public void testOK() throws Exception {
     //theSIMPLEquery should return only Doc3
    String theQueryS = "A OR (NOT B)";

    Query theQuery = getQuery(theQueryS, theAnalyzer);
    Hits hits = searcher.search(theQuery);
    String resultDocs = "";
    System.out.println("Query:" + theQuery + " hits.length=" + hits.length());
    for (int i = 0; i < hits.length(); i++) {
      System.out.println("Query Found:Doc[" + i + "]= "+ hits.doc(i).get(contentFieldName));
      resultDocs += hits.doc(i).get(contentFieldName);
    }
    for (int i=0; i++ < 3;) {
      System.out.println(searcher.explain(theQuery,i));
    }

    assertEquals("resultDocs =" + resultDocs, 3, hits.length());

}
}
