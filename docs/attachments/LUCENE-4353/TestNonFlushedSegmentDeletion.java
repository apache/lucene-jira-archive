package org.apache.lucene.index;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.LuceneTestCase;

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

public class TestNonFlushedSegmentDeletion extends LuceneTestCase {
  
  public void testNonFlushedSegmentDeletion() throws IOException {
    Directory dir = new RAMDirectory();
    IndexWriter iw = new IndexWriter(dir, newIndexWriterConfig(
        TEST_VERSION_CURRENT, new WhitespaceAnalyzer(TEST_VERSION_CURRENT)));
    Document doc = new Document();
    doc.add(new TextField("f", "a", Store.NO));
    iw.addDocument(doc);
    
    iw.deleteDocuments(new Term("f", "a"));
    doc = new Document();
    doc.add(new TextField("f", "a", Store.NO));
    iw.addDocument(doc);
    iw.close();
    
    int count = 0;
    IndexReader reader = DirectoryReader.open(dir);
    BytesRef bytes = new BytesRef("a");
    IndexReaderContext topReaderContext = reader.getContext();
    for (AtomicReaderContext atomicReaderContext : topReaderContext.leaves()) {
      DocsAndPositionsEnum docsAndPosEnum = atomicReaderContext.reader()
          .termPositionsEnum(null, "f", bytes);
      assertNotNull(docsAndPosEnum);
      if (atomicReaderContext.reader().maxDoc() == 0) {
        continue;
      }
      while (docsAndPosEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
        count++;
      }
    }
    assertEquals("Wrong number of docs for f:a", 1, count);
    reader.close();
  }
  
}
