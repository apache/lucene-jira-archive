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


import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.queryparser.surround.parser.QueryParser;
import org.apache.lucene.queryparser.surround.query.BasicQueryFactory;
import org.apache.lucene.queryparser.surround.query.SrndQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.BaseDirectoryWrapper;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.After;
import org.junit.Before;

import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;

/**
 * Some tests that highlight wildcard, fuzzy, etc queries.
 */
public class TestUnifiedHighlighterSurround extends LuceneTestCase {

  final FieldType fieldType;

  BaseDirectoryWrapper dir;
  Analyzer indexAnalyzer;

  @ParametersFactory
  public static Iterable<Object[]> parameters() {
    return UHTestHelper.parametersFactoryList();
  }

  public TestUnifiedHighlighterSurround(FieldType fieldType) {
    this.fieldType = fieldType;
  }

  @Before
  public void doBefore() throws IOException {
    dir = newDirectory();
    indexAnalyzer = new MockAnalyzer(random(), MockTokenizer.SIMPLE, true);//whitespace, punctuation, lowercase
  }

  @After
  public void doAfter() throws IOException {
    dir.close();
  }

  public void testSurround() throws Exception {
    RandomIndexWriter iw = new RandomIndexWriter(random(), dir, indexAnalyzer);

    Field body = new Field("body", "", fieldType);
    Document doc = new Document();
    doc.add(body);

    body.setStringValue("This is a test.");
    iw.addDocument(doc);
    body.setStringValue("Test a one sentence document.");
    iw.addDocument(doc);

    IndexReader ir = iw.getReader();
    iw.close();

    IndexSearcher searcher = newSearcher(ir);
    UnifiedHighlighter highlighter = new UnifiedHighlighter(searcher, indexAnalyzer);

    // Mimicking what SurroundQParserParser.parse() does
    SrndQuery sq = QueryParser.parse("2w(one, document)");
    BasicQueryFactory bqFactory = new BasicQueryFactory(1000);
    Query query = sq.makeLuceneQueryField("body", bqFactory);
    
    /* This works --
    SpanTermQuery[] queries = {
        new SpanTermQuery(new Term("body", "one")),
        new SpanTermQuery(new Term("body", "document"))
    };
    
    Query query = new SpanNearQuery(queries, 2, true);
    */
    
    TopDocs topDocs = searcher.search(query, 10, Sort.INDEXORDER);
    assertEquals(1, topDocs.totalHits);
    
    String snippets[] = highlighter.highlight("body", query, topDocs);
    assertEquals(1, snippets.length);
    assertEquals("Test a <b>one</b> sentence <b>document</b>.", snippets[0]);

    ir.close();
  }
}
