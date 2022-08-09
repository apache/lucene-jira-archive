package org.apache.lucene.indexaccessor;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;

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
import java.io.File;
import java.io.IOException;

import java.util.Set;

/**
 * A MultiIndexAccessor allows you to retrieve a MultiSearcher across Multiple
 * indexes.
 * 
 */
public interface MultiIndexAccessor {
  /**
   * @param indexes
   * @return new or cached MultiSearcher
   */
  public Searcher getMultiSearcher(Set<File> indexes) throws IOException;

  /**
   * 
   * 
   * @param indexes
   * @param indexReader
   * @return new or cached MultiSearcher
   * @throws IOException
   */
  public Searcher getMultiSearcher(Set<File> indexes, IndexReader indexReader)
      throws IOException;

  /**
   * 
   * 
   * @param similarity
   * @param indexes
   * @return new or cached MultiSearcher
   * @throws IOException
   */
  public Searcher getMultiSearcher(Similarity similarity, Set<File> indexes)
      throws IOException;

  /**
   * 
   * 
   * @param similarity
   * @param indexes
   * @param indexReader
   * @return new or cached MultiSearcher
   * @throws IOException
   */
  public Searcher getMultiSearcher(Similarity similarity, Set<File> indexes,
      IndexReader indexReader) throws IOException;

  /**
   * 
   * @param searcher
   */
  public void release(Searcher searcher);
}
