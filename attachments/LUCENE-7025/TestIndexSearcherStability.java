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

package org.apache.solr.core;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.ExitableDirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.NoMergePolicy;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.uninverting.UninvertingReader;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.search.SolrQueryTimeoutImpl;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestIndexSearcherStability extends SolrTestCaseJ4 {

  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  @Test
  public void testLuceneIndexSearcherDVGen() throws Exception {
    // Open directory and get a writer
    Directory dir = newDirectory();
    IndexWriterConfig conf = newIndexWriterConfig(new MockAnalyzer(random()));
    conf.setMergePolicy(NoMergePolicy.INSTANCE);
    final IndexWriter writer = new IndexWriter(dir, conf);


    // Index one doc
    Document doc = new Document();
    doc.add(new StringField("id", "42", Store.YES));
    doc.add(new NumericDocValuesField("foo_i_dvo", 1));
    writer.addDocument(doc);
    writer.commit();

    // Open a searcher
    Map<String, UninvertingReader.Type> mapping = new HashMap();
    mapping.put("foo_i_dvo", UninvertingReader.Type.INTEGER);
    final DirectoryReader reader = ExitableDirectoryReader.wrap(UninvertingReader.wrap(
        DirectoryReader.open(writer), mapping), SolrQueryTimeoutImpl.getInstance());
    IndexSearcher searcher = new IndexSearcher(reader);
    LeafReader lr = UninvertingReader.unwrap(ExitableDirectoryReader.unwrap(
        (DirectoryReader)searcher.getIndexReader()).leaves().get(0).reader());
    //LeafReader lr = SlowCompositeReaderWrapper.wrap(reader);
    log.info("Before: "+searcher);
    long beforeDVGen = ((SegmentReader)lr.leaves().get(0).reader()).getSegmentInfo().getDocValuesGen();

    // Start updates to DVs in a separate thread
    Thread t = new Thread() {
      @Override
      public void run() {
        for (int i=0; i<1000; i++) {
          try {
            writer.updateNumericDocValue(new Term("id", "42"), "foo_i_dvo", 5L+new Random().nextInt(1000));
            writer.forceMerge(1);
            writer.commit();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
    };
    t.start();
    t.join();

    long dvValue = lr.getNumericDocValues("foo_i_dvo").get(0);
    assertEquals(1l, dvValue);
    System.out.println("After dv is: "+dvValue);

    log.info("After: "+searcher);

    // Assert that the current searcher's DVGen didn't change
    long afterDvGen = ((SegmentReader)lr.leaves().get(0).reader()).getSegmentInfo().getDocValuesGen();
    assertEquals("DVGen didn't match for an already open searcher", beforeDVGen, afterDvGen);

    reader.close();
    writer.close();

  }
}
