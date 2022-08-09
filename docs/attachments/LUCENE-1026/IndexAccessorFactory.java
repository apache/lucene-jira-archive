package org.apache.lucene.indexaccessor;

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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.LockObtainFailedException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An IndexAccessorFactory allows the sharing of IndexAccessors and
 * MultiIndexAccessors across threads.
 * 
 */
public class IndexAccessorFactory {
  private static final IndexAccessorFactory indexAccessorFactory = new IndexAccessorFactory();
  private Map<File, IndexAccessor> indexAccessors = new HashMap<File, IndexAccessor>();
  private MultiIndexAccessor multiIndexAccessor = new DefaultMultiIndexAccessor();

  /**
   * @return
   */
  public static IndexAccessorFactory getInstance() {
    return indexAccessorFactory;
  }

  /**
   * Closes all of the open IndexAccessors.
   */
  public void closeAllAccessors() {
    synchronized (indexAccessors) {
      Set<File> accessorDirs = indexAccessors.keySet();

      for (File dir : accessorDirs) {
        IndexAccessor accessor = indexAccessors.get(dir);
        accessor.close();
      }

      indexAccessors.clear();
    }
  }

  public void createAccessor(FSDirectory dir, Analyzer analyzer)
      throws IOException {
    IndexAccessor accessor = new DefaultIndexAccessor(dir, analyzer);

    accessor.open();

    if (!new File(dir.toString()).exists()) {
      IndexWriter indexWriter = new IndexWriter(dir, null, true);
      indexWriter.close();
    }

    synchronized (indexAccessors) {
      indexAccessors.put(dir.getFile(), accessor);
    }
  }

  /**
   * @param indexDir
   * @return
   */
  public IndexAccessor getAccessor(File indexDir) {
    synchronized (indexAccessors) {
      if (indexAccessors.containsKey(indexDir)) {
        IndexAccessor accessor = indexAccessors.get(indexDir);

        return accessor;
      } else {
        throw new IllegalStateException("Requested Accessor does not exist");
      }
    }
  }

  /**
   * @return
   */
  public MultiIndexAccessor getMultiIndexAccessor() {
    return multiIndexAccessor;
  }
}
