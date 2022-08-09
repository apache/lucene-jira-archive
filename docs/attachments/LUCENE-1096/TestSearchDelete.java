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

import junit.framework.TestCase;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

/**
 * Test deletions during search.
 */
public class TestSearchDelete extends TestCase {

  private static boolean VERBOSE = true;  
  private static final String TEXT_FIELD = "text";

  private static Directory directory;

  public void setUp() throws Exception {
    // Create an index writer.
    directory = new RAMDirectory();
    IndexWriter writer = new IndexWriter(directory, new WhitespaceAnalyzer(), true);
    for (int i=0; i<402; i++) {
      writer.addDocument(createDocument(i));
    }
    writer.optimize();
    writer.close();
  }

  /**
   * Deletions during search should not cause an exception.
   */
  public void testSearchHitsDelete() throws Exception {
    IndexSearcher searcher = new IndexSearcher(directory);
    IndexReader reader = searcher.getIndexReader();
    Query q = new TermQuery(new Term(TEXT_FIELD,"text"));
    Hits hits = searcher.search(q);
    log("Got "+hits.length()+" results");
    for (int i = 0; i < hits.length(); i++) {
      int id = hits.id(i);
      Document doc = hits.doc(id);
      log(i+".  deleting doc "+doc+" with id "+id);
      reader.deleteDocument(id);
    }
    searcher.close();
  }

  private static Document createDocument(int id) {
    Document doc = new Document();
    doc.add(new Field(TEXT_FIELD, "text of document"+id, Field.Store.YES, Field.Index.TOKENIZED));
    return doc;
  }

  private static void log (String s) {
    if (VERBOSE) {
      System.out.println(s);
    }
  }
}
