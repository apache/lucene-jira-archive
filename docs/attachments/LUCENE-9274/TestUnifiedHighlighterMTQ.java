/*
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
package org.apache.lucene.search.uhighlight;


import static org.junit.Assert.assertEquals;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.Test;


public class TestUnifiedHighlighterMTQ  {

  @Test
  public void testSpanWildcard() throws Exception {
    StandardAnalyzer analyzer = new StandardAnalyzer();
    Directory directory = new ByteBuffersDirectory(); 

    IndexWriterConfig config = new IndexWriterConfig(analyzer);

    String[] docs = new String[]{"This is a test.","Test a one sentence document."};
    try (IndexWriter w = new IndexWriter(directory, config)) {
      for (String docText : docs) {
        Document doc = new Document();
        Field field = new TextField("body", docText, Field.Store.YES);
        doc.add(field);
        w.addDocument(doc);
      }
    }
    
    IndexReader reader = DirectoryReader.open(directory); 
    IndexSearcher searcher = new IndexSearcher(reader);
    UnifiedHighlighter highlighter = new UnifiedHighlighter(searcher,analyzer);

    Query query = new SpanMultiTermQueryWrapper<>(new WildcardQuery(new Term("body", "te*")));
    
    TopDocs topDocs = searcher.search(query, 10, Sort.INDEXORDER);
    assertEquals(2, topDocs.totalHits.value);
    String snippets[] = highlighter.highlight("body", query, topDocs);
    assertEquals(2, snippets.length);
    assertEquals("This is a <b>test</b>.", snippets[0]);
    assertEquals("<b>Test</b> a one sentence document.", snippets[1]);
    
    //wildcard query that creates an automaton with type SINGLE
    query = new SpanMultiTermQueryWrapper<>(new WildcardQuery(new Term("body", "test")));
    
    topDocs = searcher.search(query, 10, Sort.INDEXORDER);
    assertEquals(2, topDocs.totalHits.value);
    snippets = highlighter.highlight("body", query, topDocs);
    assertEquals(2, snippets.length);
     // highlighting does not work 
    assertEquals("This is a <b>test</b>.", snippets[0]);
    assertEquals("<b>Test</b> a one sentence document.", snippets[1]);

    reader.close();
  }
}
