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

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Random;

import org.apache.lucene.codecs.blocktree.BlockTreeTermsReader;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class TestTermsDictRamBytesUsed {

  // Flake IDs (https://github.com/boundary/flake) with
  //   - 6 bytes from the timestamp
  //   - 6 bytes from the mac address
  //   - 3 bytes from the sequence number
  public static void testIndexFlakeIDs() throws IOException {
    Directory dir = FSDirectory.open(Files.createTempDirectory("flake"));

    IndexWriter w = new IndexWriter(dir, new IndexWriterConfig().setMergePolicy(new LogDocMergePolicy()));
    int numDocs = 10000000;
    final int batchSize = 50;
    final int timePerBatchinMillis = 3;
    Random r = new Random(0);
    // about 40 years after Epoch
    long currentTimeMillis = 40L * 365 * 24 * 60 * 60 * 1000;
    byte[] macAddress = new byte[6];
    r.nextBytes(macAddress);
    int sequenceNumber = r.nextInt();
    Document doc = new Document();
    byte[] id = new byte[15];
    StringField idField = new StringField("id", new BytesRef(id), Store.NO);
    doc.add(idField);

    for (int i = 0; i < numDocs; i += batchSize) {
      currentTimeMillis += r.nextInt(2 * timePerBatchinMillis);

      for (int j = 0; j < batchSize; ++j) {
        int seqNum = ++sequenceNumber & 0xffffff;
        if (seqNum == 0) {
          currentTimeMillis++;
        }
        int k = 0;

        // Lower 6 bytes of the timestamp
        id[k++] = (byte) (currentTimeMillis >>> 40);
        id[k++] = (byte) (currentTimeMillis >>> 32);
        id[k++] = (byte) (currentTimeMillis >>> 24);
        id[k++] = (byte) (currentTimeMillis >>> 16);
        id[k++] = (byte) (currentTimeMillis >>> 8);
        id[k++] = (byte) (currentTimeMillis >>> 0);

        // 6 bytes from the mac address
        System.arraycopy(macAddress, 0, id, k, 6);
        k += 6;

        // 3 bytes from the sequence number
        id[k++] = (byte) (seqNum >>> 16);
        id[k++] = (byte) (seqNum >>> 8);
        id[k++] = (byte) (seqNum >>> 0);

        if (k != 15) throw new Error();

        w.addDocument(doc);
      }
    }
    w.forceMerge(1);
    w.close();

    DirectoryReader reader = DirectoryReader.open(dir, Collections.singletonMap(BlockTreeTermsReader.FST_MODE_KEY, "ON_HEAP"));
    SegmentReader sr = (SegmentReader) reader.leaves().get(0).reader();
    System.out.println("FLAKE IDs memory usage: " + sr.getPostingsReader().ramBytesUsed());
    reader.close();
    dir.close();
  }

  public static void main(String[] args) throws Exception {
    testIndexFlakeIDs();
  }

}
