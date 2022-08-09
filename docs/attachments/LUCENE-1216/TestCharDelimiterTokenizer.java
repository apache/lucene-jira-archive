package org.apache.lucene.analysis;

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
import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.util.LuceneTestCase;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class TestCharDelimiterTokenizer extends LuceneTestCase {
  private Directory dir;
  private Reader input;
  
  private void createLuceneEnv(){
    dir = new RAMDirectory();
    input = new StringReader("a b\t\u3042\u3001\u3044");
  }
  
  public void testDefault() throws IOException {
    createLuceneEnv();
    CharDelimiterTokenizer stream = new CharDelimiterTokenizer(input);
    
    Document doc = new Document();
    doc.add(new Field("preanalyzed", stream, TermVector.NO));
    
    IndexWriter writer = new IndexWriter(dir, new SimpleAnalyzer(), IndexWriter.MaxFieldLength.LIMITED);
    writer.addDocument(doc);
    writer.close();
    
    IndexReader reader = IndexReader.open(dir);
    TermPositions termPositions = reader.termPositions(new Term("preanalyzed", "a b\t\u3042\u3001\u3044"));
    assertTrue(termPositions.next());
    assertEquals(1, termPositions.freq());
    assertEquals(0, termPositions.nextPosition());
    reader.close();
  }

  public void testWhitespace() throws IOException {
    createLuceneEnv();
    CharDelimiterTokenizer stream = new CharDelimiterTokenizer(input);
    stream.setWhitespaceDelimiter(true);
    
    Document doc = new Document();
    doc.add(new Field("preanalyzed", stream, TermVector.NO));
    
    IndexWriter writer = new IndexWriter(dir, new SimpleAnalyzer(), IndexWriter.MaxFieldLength.LIMITED);
    writer.addDocument(doc);
    writer.close();
    
    IndexReader reader = IndexReader.open(dir);
    
    TermPositions termPositions = reader.termPositions(new Term("preanalyzed", "a"));
    assertTrue(termPositions.next());
    assertEquals(1, termPositions.freq());
    assertEquals(0, termPositions.nextPosition());
    
    termPositions = reader.termPositions(new Term("preanalyzed", "b"));
    assertTrue(termPositions.next());
    assertEquals(1, termPositions.freq());
    assertEquals(1, termPositions.nextPosition());
    
    termPositions.seek(new Term("preanalyzed", "\u3042\u3001\u3044"));
    assertTrue(termPositions.next());
    assertEquals(1, termPositions.freq());
    assertEquals(2, termPositions.nextPosition());
    
    reader.close();
  }

  public void testAddDelimiter() throws IOException {
    createLuceneEnv();
    CharDelimiterTokenizer stream = new CharDelimiterTokenizer(input);
    stream.addDelimiter('\u3001');
    
    Document doc = new Document();
    doc.add(new Field("preanalyzed", stream, TermVector.NO));
    
    IndexWriter writer = new IndexWriter(dir, new SimpleAnalyzer(), IndexWriter.MaxFieldLength.LIMITED);
    writer.addDocument(doc);
    writer.close();
    
    IndexReader reader = IndexReader.open(dir);
    
    TermPositions termPositions = reader.termPositions(new Term("preanalyzed", "a b\t\u3042"));
    assertTrue(termPositions.next());
    assertEquals(1, termPositions.freq());
    assertEquals(0, termPositions.nextPosition());
    
    termPositions.seek(new Term("preanalyzed", "\u3044"));
    assertTrue(termPositions.next());
    assertEquals(1, termPositions.freq());
    assertEquals(1, termPositions.nextPosition());
    
    reader.close();
  }
}
