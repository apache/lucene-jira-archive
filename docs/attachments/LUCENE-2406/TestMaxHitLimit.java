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

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.LuceneTestCase;

public class TestMaxHitLimit extends LuceneTestCase {
  
  /**
   * Illustrates the issue with searching based on a very large hit limit
   * 
   * @throws Exception
   */
  public void testMaxHitLimit() throws Exception {
    RAMDirectory directory = new RAMDirectory();
    IndexWriter writer = new IndexWriter(directory, new SimpleAnalyzer(),
        MaxFieldLength.LIMITED);
    
    // Ensure that there's at least one segment in the index
    Document document = new Document();
    document.add(new Field("name", "value", Field.Store.YES,
        Field.Index.ANALYZED));
    writer.addDocument(document);
    
    // Obtain reader after the document has been written
    IndexReader reader = writer.getReader();
    IndexSearcher searcher = new IndexSearcher(reader);
    
    // Check to see if there's exactly one matching document
    // Note that this will throw 
    assertEquals(1, searcher.search(new TermQuery(new Term("name", "value")),
        Integer.MAX_VALUE).totalHits);
  }
  
}
