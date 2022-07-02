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
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;

import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * Default MultiIndexAccessor implementation.
 * 
 */
public class DefaultMultiIndexAccessor implements MultiIndexAccessor {
  private final static Logger logger = Logger
      .getLogger(DefaultMultiIndexAccessor.class.getPackage().getName());
  private final Map<Searcher, IndexAccessor> cachedIndexAccessors = new HashMap<Searcher, IndexAccessor>();
  private final Map<MultiSearcherConfig, MultiSearcher> cachedSearchers = new HashMap<MultiSearcherConfig, MultiSearcher>();

  public Searcher getMultiSearcher(Set<File> indexes) throws IOException {
    return getMultiSearcher(Similarity.getDefault(), indexes, null);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.MultiIndexAccessor#getMultiSearcher(java.util.Set,
   *      org.apache.lucene.index.IndexReader)
   */
  public Searcher getMultiSearcher(Set<File> indexes, IndexReader indexReader)
      throws IOException {
    return getMultiSearcher(Similarity.getDefault(), indexes, indexReader);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.MultiIndexAccessor#getMultiSearcher(org.apache.lucene.search.Similarity,
   *      java.util.Set)
   */
  public Searcher getMultiSearcher(Similarity similarity, Set<File> indexes)
      throws IOException {
    return getMultiSearcher(similarity, indexes, null);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.MultiIndexAccessor#getMultiSearcher(org.apache.lucene.search.Similarity,
   *      java.util.Set, org.apache.lucene.index.IndexReader)
   */
  public Searcher getMultiSearcher(Similarity similarity, Set<File> indexes,
      IndexReader indexReader) throws IOException {
    MultiSearcher multiSearcher = null;
    MultiSearcherConfig config = new MultiSearcherConfig(similarity, indexes);

    synchronized (this) {
      multiSearcher = cachedSearchers.get(config);

      if (multiSearcher != null) {
        if (logger.isLoggable(Level.FINE)) {
          logger.fine("returning cached searcher");
        }
      } else {
        if (logger.isLoggable(Level.FINE)) {
          logger.fine("opening new searcher and caching it");
        }

        Searcher[] searchers = new Searcher[indexes.size()];
        Iterator<File> it = indexes.iterator();
        IndexAccessorFactory factory = IndexAccessorFactory.getInstance();

        for (int i = 0; i < searchers.length; i++) {
          File index = it.next();
          IndexAccessor indexAccessor = factory.getAccessor(index);
          Searcher searcher = indexAccessor
              .getSearcher(similarity, indexReader);
          searchers[i] = searcher;
          cachedIndexAccessors.put(searcher, indexAccessor);
        }

        multiSearcher = new MultiSearcher(searchers);
        cachedSearchers.put(config, multiSearcher);
      }
    }

    return multiSearcher;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.MultiIndexAccessor#release(org.apache.lucene.search.Searcher)
   */
  public void release(Searcher searcher) {
    Searchable[] searchers = ((MultiSearcher) searcher).getSearchables();

    synchronized (this) {
      for (Searchable searchable : searchers) {
        cachedIndexAccessors.get(searchable).release((Searcher) searchable);
      }
    }
  }

  /**
   * Used to as key to Searcher cache.
   * 
   */
  class MultiSearcherConfig {
    private Set<File> indexes;
    private Similarity similarity;

    MultiSearcherConfig(Similarity similarity, Set<File> indexes) {
      this.similarity = similarity;
      this.indexes = indexes;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (getClass() != obj.getClass()) {
        return false;
      }

      final MultiSearcherConfig other = (MultiSearcherConfig) obj;

      if (indexes == null) {
        if (other.indexes != null) {
          return false;
        }
      } else if (!indexes.equals(other.indexes)) {
        return false;
      }

      if (similarity == null) {
        if (other.similarity != null) {
          return false;
        }
      } else if (!similarity.equals(other.similarity)) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = (prime * result) + ((indexes == null) ? 0 : indexes.hashCode());
      result = (prime * result)
          + ((similarity == null) ? 0 : similarity.hashCode());

      return result;
    }
  }
}
