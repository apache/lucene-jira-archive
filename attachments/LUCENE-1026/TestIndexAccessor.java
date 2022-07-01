package com.mhs.indexaccessor;

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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.indexaccessor.IndexAccessor;
import org.apache.lucene.indexaccessor.IndexAccessorFactory;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NoLockFactory;

import java.io.File;
import java.io.IOException;

import java.util.HashSet;
import java.util.Set;

public class TestIndexAccessor extends TestCase {
  private static LockFactory lockFactory = new NoLockFactory();
  private String tempDir;
  final private File index1;
  final private File index2;

  public TestIndexAccessor() {
    tempDir = System.getProperty("java.io.tmpdir") + "indexaccessor_test";
    index1 = new File(tempDir, "testindex");
    index2 = new File(tempDir, "testindex2");
  }

  public void testCreateIndex() throws IOException {
    Analyzer analyzer = new WhitespaceAnalyzer();
    FSDirectory dir = FSDirectory.getDirectory(index1, lockFactory);
    IndexAccessorFactory.getInstance().createAccessor(dir, analyzer);

    assertTrue(index1.exists());

    delete(index1);

    IndexAccessorFactory.getInstance().closeAllAccessors();
  }

  public void testSingleSimpleSearch() throws IOException, ParseException {
    Analyzer analyzer = new WhitespaceAnalyzer();
    FSDirectory dir = FSDirectory.getDirectory(index1, lockFactory);
    IndexAccessorFactory.getInstance().createAccessor(dir, analyzer);

    IndexAccessor accessor = IndexAccessorFactory.getInstance().getAccessor(
        index1);
    IndexWriter writer = accessor.getWriter(false);
    Document doc = new Document();
    doc.add(new Field("field", "test", Store.NO, Index.TOKENIZED));
    writer.addDocument(doc);
    accessor.release(writer);

    Searcher searcher = accessor.getSearcher(null);

    try {
      QueryParser qp = new QueryParser("field", new KeywordAnalyzer());

      Hits hits = searcher.search(qp.parse("test"));

      assertEquals(1, hits.length());
    } finally {
      accessor.release(searcher);
    }

    delete(index1);
    IndexAccessorFactory.getInstance().closeAllAccessors();
  }

  public void testMultiSimpleSearch() throws IOException, ParseException {
    Analyzer analyzer = new WhitespaceAnalyzer();
    FSDirectory dir = FSDirectory.getDirectory(index1, lockFactory);
    IndexAccessorFactory.getInstance().createAccessor(dir, analyzer);

    analyzer = new WhitespaceAnalyzer();
    dir = FSDirectory.getDirectory(index2, lockFactory);
    IndexAccessorFactory.getInstance().createAccessor(dir, analyzer);

    IndexAccessor accessor = IndexAccessorFactory.getInstance().getAccessor(
        index1);
    IndexAccessor accessor2 = IndexAccessorFactory.getInstance().getAccessor(
        index2);
    IndexWriter writer = accessor.getWriter(false);
    IndexWriter writer2 = accessor2.getWriter(false);
    Document doc = new Document();
    Document doc2 = new Document();
    doc.add(new Field("field", "test", Store.NO, Index.TOKENIZED));
    doc2.add(new Field("field", "test", Store.NO, Index.TOKENIZED));
    writer.addDocument(doc);
    accessor.release(writer);
    writer2.addDocument(doc2);
    accessor2.release(writer2);

    Set<File> indexes = new HashSet<File>();
    indexes.add(index1);
    indexes.add(index2);

    Searcher searcher = IndexAccessorFactory.getInstance()
        .getMultiIndexAccessor().getMultiSearcher(indexes, null);

    try {
      QueryParser qp = new QueryParser("field", new KeywordAnalyzer());

      Hits hits = searcher.search(qp.parse("test"));

      assertEquals(2, hits.length());
    } finally {
      IndexAccessorFactory.getInstance().getMultiIndexAccessor().release(
          searcher);
    }

    delete(index1);
    delete(index2);

    IndexAccessorFactory.getInstance().closeAllAccessors();
  }

  public void testSimpleReaderRefresh() throws IOException, ParseException {
    IndexAccessorFactory factory = IndexAccessorFactory.getInstance();

    Analyzer analyzer = new WhitespaceAnalyzer();
    FSDirectory dir = FSDirectory.getDirectory(index1, lockFactory);
    factory.createAccessor(dir, analyzer);

    IndexAccessor accessor = factory.getAccessor(index1);
    IndexWriter writer = accessor.getWriter(false);
    Document doc = new Document();
    doc.add(new Field("field", "test", Store.NO, Index.TOKENIZED));
    writer.addDocument(doc);
    accessor.release(writer);

    new Thread() {
      public void run() {
        IndexAccessorFactory factory = IndexAccessorFactory.getInstance();

        IndexAccessor accessor = factory.getAccessor(index1);
        Searcher searcher = null;

        try {
          searcher = accessor.getSearcher(null);

          QueryParser qp = new QueryParser("field", new KeywordAnalyzer());

          Hits hits = searcher.search(qp.parse("test"));

          assertEquals(1, hits.length());
        } catch (IOException e) {
          throw new RuntimeException(e);
        } catch (ParseException e) {
          throw new RuntimeException(e);
        } finally {
          accessor.release(searcher);
        }

        new Thread() {
          public void run() {
            IndexAccessorFactory factory = IndexAccessorFactory.getInstance();

            IndexAccessor accessor = factory.getAccessor(index1);

            IndexWriter writer;

            try {
              writer = accessor.getWriter(false);
              writer.deleteDocuments(new Term("field", "test"));

              Document doc = new Document();
              doc.add(new Field("field", "update", Store.NO, Index.TOKENIZED));
              writer.addDocument(doc);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }

            accessor.release(writer);

            new Thread() {
              public void run() {
                IndexAccessorFactory factory = IndexAccessorFactory
                    .getInstance();

                IndexAccessor accessor = factory.getAccessor(index1);
                Searcher searcher = null;

                try {
                  searcher = accessor.getSearcher(null);

                  QueryParser qp = new QueryParser("field",
                      new KeywordAnalyzer());

                  Hits hits = searcher.search(qp.parse("update"));

                  assertEquals(1, hits.length());
                } catch (IOException e) {
                  throw new RuntimeException(e);
                } catch (ParseException e) {
                  throw new RuntimeException(e);
                } finally {
                  accessor.release(searcher);
                }
              }
            }.start();
          }
        }.start();
      }
    }.start();

    // cleanup
    delete(index1);
    IndexAccessorFactory.getInstance().closeAllAccessors();
  }

  /**
   * Test showing that query parsing and searching would dominate running time
   * over synchronized access calls.
   * 
   * @throws IOException
   * @throws ParseException
   * @throws InterruptedException
   */
  public void testGetSearcherSpeed() throws IOException, ParseException,
      InterruptedException {
    System.out.println("Without Contention");
    System.out.println("Just retrieve and release Searcher 100000 times");
    System.out.println("----");
    testGetSearcherSpeedImpl(true, false);

    System.out.println("");
    System.out.println("Parse query and search on 1 doc 100000 times");
    System.out.println("----");
    testGetSearcherSpeedImpl(false, false);
    System.out.println("");
    System.out.println("");
    System.out.println("With Contention");
    System.out.println("Just retrieve and release Searcher 100000 times");
    System.out.println("----");
    testGetSearcherSpeedImpl(true, true);

    System.out.println("");
    System.out.println("Parse query and search on 1 doc 100000 times");
    System.out.println("----");
    testGetSearcherSpeedImpl(false, true);
  }

  private void testGetSearcherSpeedImpl(final boolean onlyAccessSearcher,
      boolean withContention) throws IOException, CorruptIndexException,
      ParseException, InterruptedException {
    Analyzer analyzer = new WhitespaceAnalyzer();
    FSDirectory dir = FSDirectory.getDirectory(index1, lockFactory);
    IndexAccessorFactory.getInstance().createAccessor(dir, analyzer);

    IndexAccessor accessor = IndexAccessorFactory.getInstance().getAccessor(
        index1);
    IndexWriter writer = accessor.getWriter(false);
    Document doc = new Document();
    doc.add(new Field("field", "test", Store.NO, Index.TOKENIZED));
    writer.addDocument(doc);
    accessor.release(writer);

    if (withContention) {
      for (int i = 0; i < 40; i++) {
        Thread thread = new Thread() {
          public void run() {
            IndexAccessor accessor = IndexAccessorFactory.getInstance()
                .getAccessor(index1);
            Searcher searcher = null;

            float times = 80000f;

            try {
              for (int i = 0; i < times; i++) {
                try {
                  searcher = accessor.getSearcher(null);
                } catch (IllegalStateException e) {
                  break;
                }

                if (!onlyAccessSearcher) {
                  QueryParser qp = new QueryParser("field",
                      new KeywordAnalyzer());
                  Hits hits = searcher.search(qp.parse("test"));
                }

                accessor.release(searcher);
              }
            } catch (IOException e) {
              throw new RuntimeException(e);
            } catch (ParseException e) {
              throw new RuntimeException(e);
            }
          }
        };

        thread.setDaemon(true);
        thread.start();
      }

      Thread.sleep(500);
    }

    Searcher searcher = null;
    StopWatch sw = new StopWatch();
    sw.start();

    float times = 100000f;

    for (int i = 0; i < times; i++) {
      searcher = accessor.getSearcher(null);

      if (!onlyAccessSearcher) {
        QueryParser qp = new QueryParser("field", new KeywordAnalyzer());
        Hits hits = searcher.search(qp.parse("test"));
      }

      accessor.release(searcher);
    }

    sw.stop();

    System.out.println("avg time:" + (sw.toValue() / times) + " ms");
    System.out.println("total time:" + sw);

    IndexAccessorFactory.getInstance().closeAllAccessors();
    delete(index1);
  }

  private void delete(File file) {
    boolean success = file.delete();

    if (!success) {
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
      }

      success = file.delete();

      if (!success) {
        file.deleteOnExit();
      }
    }
  }

  public void testLotsOfAccess() throws InterruptedException,
      CorruptIndexException, IOException {
    Analyzer analyzer = new WhitespaceAnalyzer();
    FSDirectory dir = FSDirectory.getDirectory(index1, lockFactory);
    IndexAccessorFactory.getInstance().createAccessor(dir, analyzer);

    for (int i = 0; i < 10; i++) {
      Thread thread = new Thread() {
        public void run() {
          IndexAccessor accessor = IndexAccessorFactory.getInstance()
              .getAccessor(index1);
          Searcher searcher = null;

          float times = 8000f;

          try {
            for (int i = 0; i < times; i++) {
              searcher = accessor.getSearcher(null);

              QueryParser qp = new QueryParser("field", new KeywordAnalyzer());
              Hits hits = searcher.search(qp.parse("test"));
              accessor.release(searcher);
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          } catch (ParseException e) {
            throw new RuntimeException(e);
          }
        }
      };

      thread.setDaemon(true);
      System.out.println("starting search thread");
      thread.start();
    }

    for (int i = 0; i < 8; i++) {
      Thread thread = new Thread() {
        public void run() {
          IndexAccessor accessor = IndexAccessorFactory.getInstance()
              .getAccessor(index1);

          float times = 3000f;

          try {
            for (int i = 0; i < times; i++) {
              IndexWriter writer = accessor.getWriter(false);
              Document doc = new Document();
              doc.add(new Field("field", "test", Store.NO, Index.TOKENIZED));
              writer.addDocument(doc);
              accessor.release(writer);
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      };

      thread.setDaemon(true);
      thread.start();
    }

    Thread.sleep(4000);

    IndexAccessorFactory.getInstance().closeAllAccessors();
    delete(index1);
  }
}
