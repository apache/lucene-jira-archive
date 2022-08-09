package org.apache.lucene.facet;

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
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.FixedBitSet;

/**
 * Collects hits for subsequent faceting. Once you've run a search and collect
 * hits into this, instantiate one of the {@link Facets} subclasses to do the
 * facet counting. Use the {@code search} utility methods to perform an
 * "ordinary" search but also collect into a {@link Collector}.
 */
public class SamplingFacetsCollector extends FacetsCollector {
  
  protected int binsize;
  
  /** Default constructor */
  public SamplingFacetsCollector(int binsize) {
    super();
    this.binsize = binsize;
  }
  
  /**
   * Create this; if {@code keepScores} is true then a float[] is allocated to
   * hold score of all hits.
   */
  public SamplingFacetsCollector(boolean keepScores,int binsize) {
    super(keepScores);
    this.binsize = binsize;
  }
  
  /**
   * Creates a {@link org.apache.lucene.facet.FacetsCollector.Docs} to record
   * hits. The default uses {@link FixedBitSet} to record hits and you can
   * override to e.g. record the docs in your own {@link DocIdSet}. I override
   * this method to created a sampled FixedBitSet
   */
  protected Docs createDocs(final int maxDoc) {
    return new Docs() {
      private final FixedBitSet bits = new FixedBitSet(maxDoc);
      
      @Override
      public void addDoc(int docId) throws IOException {
        bits.set(docId);
      }
      
      @Override
      public DocIdSet getDocIdSet() {
        createSample(bits);
        return bits;
      }
    };
  }
  
  /**
   * Removes hits from the given docIdSet, by leaving just (1 / binsize)  of the hits.
   * @param docIdSet The {@link FixedBitSet} to sample.
    */
  protected void createSample(FixedBitSet docIdSet) {
    int size = docIdSet.length();
    int countInBin =0;
    int randomIndex=(int) (Math.random()*binsize);
    System.out.println("Size:" + size);
    
    for (int i = 0; i < size; i++) 
    {
      countInBin++;
      if (countInBin==binsize)
      {
        countInBin=0;
        randomIndex=(int) (Math.random()*binsize);
      }
      if (docIdSet.get(i) && !(countInBin==randomIndex)) docIdSet.clear(i);
    }
  }
  
}
