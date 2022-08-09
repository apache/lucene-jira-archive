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
import java.util.Random;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.FixedBitSet;

/**
 * Collects hits for subsequent faceting. Once you've run a search and collect
 * hits into this, instantiate one of the {@link Facets} subclasses to do the
 * facet counting. Use the {@code search} utility methods to perform an
 * "ordinary" search but also collect into a {@link Collector}.
 */
public class SamplingFacetsCollector extends FacetsCollector {
  
  protected SamplingParams params;
  
  /** Default constructor */
  public SamplingFacetsCollector(SamplingParams params) {
    super();
    this.params = params;
  }
  
  /**
   * Create this; if {@code keepScores} is true then a float[] is allocated to
   * hold score of all hits.
   */
  public SamplingFacetsCollector(boolean keepScores, SamplingParams params) {
    super(keepScores);
    this.params = params;
  }
  
  /**
   * Parameters to be used in the sampling of facets using the
   * {@link SamplingFacetsCollector}
   */
  public static class SamplingParams {
    
    /**
     * The sampleRatio. 1.0 means all hits are used. 0.1 means 10% is used. Etc.
     */
    public double sampleRatio;
    
    /**
     * The random seed to use. Can be fixed, useful for testing. If {@code null}
     * the PRNG is initialized without seed
     */
    public Long randomSeed;
  }
  
  /**
   * Used during collection to record matching docs and then return a sampled
   * {@link DocIdSet} that contains them. The original {@link FixedBitSet} is
   * also available.
   */
  public static class SampledDocs extends Docs {
    private final FixedBitSet bits;
    private FixedBitSet sampledBits;
    private double sampleRatio;
    private final XORShift64Random random;
    
    public SampledDocs(int maxDoc, SamplingParams params) {
      if (params.sampleRatio<=0.0)
      {
        throw new IllegalArgumentException("SamplingParams.sampleRatio should be greated than 0.0!");
      }
      bits = new FixedBitSet(maxDoc);
      this.sampleRatio = params.sampleRatio;
      if (params.randomSeed != null) {
        this.random = new XORShift64Random(params.randomSeed);
      } else {
        this.random = new XORShift64Random();
      }
    }
    
    /**
     * Faster alternative for java.util.Random, inspired by
     * http://dmurphy747.wordpress.com/2011/03/23/xorshift-vs-random-
     * performance-in-java/
     */
    class XORShift64Random {
      long x;
      
      public XORShift64Random(long seed) {
        x = seed == 0 ? 0xdeadbeef : seed;
      }
      
      public XORShift64Random() {
        x = new Random().nextLong();
        if (x == 0) {
          x = 0xdeadbeef;
        }
      }
      
      public long randomLong() {
        x ^= (x << 21);
        x ^= (x >>> 35);
        x ^= (x << 4);
        return x;
      }
      
      public int nextInt(int n) {
        int res = (int) (randomLong() % n);
        return (res < 0) ? -res : res;
      }
    }
    
    
    protected FixedBitSet createSample(FixedBitSet docIdSet) {
      FixedBitSet sampledBits = docIdSet.clone();
      int size = docIdSet.length();
      
      int binsize = (int) (1.0 / this.sampleRatio);
      
      // we need to sample each hit because else might have to few hits in this
      // docIdSet.
      // Even better (for quality) is to do this for the 'remaining' bin of all
      // docIdSets.
      // I'm not really sure if the speed will suffer from this, nor if the
      // quality will really suffer that much
      
      if (size < binsize * 2) {
        int randomLimit = (int) (this.sampleRatio * 100000);
        for (int i = 0; i < size; i++) {
          if (sampledBits.get(i)) {
            //if the randomnumber > randomlimit, we clear the hit. Else we keep it
            if (random.nextInt(100000) > randomLimit) sampledBits.clear(i);
          }
        }
        
      } else {
        int countInBin = 0;
        
        int randomIndex = random.nextInt(binsize);
        
        for (int i = 0; i < size; i++) {
          // if a hit, it counts as being in the bin
          if (sampledBits.get(i)) {
            countInBin++;
          }
          if (countInBin == binsize) {
            countInBin = 0;
            randomIndex = random.nextInt(binsize);
          }
          if (sampledBits.get(i) && !(countInBin == randomIndex)) {
            sampledBits.clear(i);
          }
        }
      }
      return sampledBits;
      
    }
    
    /**
     * Creates a subsampled {@link DocIdSet} from the given {@link DocIdSet}, by
     * leaving just (sampleRatio*hits) of the hits.
     * 
     * @param docIdSet
     *          The {@link FixedBitSet} to sample.
     */
    protected FixedBitSet createSample2(FixedBitSet docIdSet) {
      FixedBitSet sampledBits = new FixedBitSet(docIdSet.length());
      int size = docIdSet.length();
      
      int binsize = (int) (1.0 / this.sampleRatio);
      
      // we need to sample each hit because else might have to few hits in this
      // docIdSet.
      // Even better (for quality) is to do this for the 'remaining' bin of all
      // docIdSets.
      // I'm not really sure if the speed will suffer from this, nor if the
      // quality will really suffer that much
      
      if (size < binsize * 2) {
        int randomLimit = (int) (this.sampleRatio * 100000);
        for (int i = 0; i < size; i++) {
          if (sampledBits.get(i)) {
            //if the randomnumber > randomlimit, we clear the hit. Else we keep it
            if (random.nextInt(100000) > randomLimit) sampledBits.clear(i);
          }
        }
        
      } else {
        int countInBin = 0;
        
        int randomIndex = random.nextInt(binsize);

        final DocIdSetIterator it = docIdSet.iterator();
        try {
          for( int doc = it.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = it.nextDoc()) {
            if (++countInBin == binsize) {
              countInBin = 0;
              randomIndex = random.nextInt(binsize);
            }
            if (countInBin == randomIndex) {
              sampledBits.set(doc);
            }
          }
        } catch (IOException e) {
          // should not happen
          throw new RuntimeException(e);
        }
      }
      return sampledBits;
      
    }
    
    @Override
    public void addDoc(int docId) throws IOException {
      this.bits.set(docId);
    }
    
    /**
     * Returns the underlying bitset that contains all the hits before they
     * where sampled
     */
    public DocIdSet getOriginalDocIdSet() {
      return this.bits;
    }
    
    @Override
    public DocIdSet getDocIdSet() {
      // lazy initialize the sampledBits, also preventing
      // re-sampling on each call
      if (this.sampledBits == null) {
        sampledBits = createSample(this.bits);
      }
      return this.sampledBits;
    }
  }
  
  /**
   * Creates a {@link org.apache.lucene.facet.FacetsCollector.Docs} to record
   * hits. The default uses {@link FixedBitSet} to record hits and you can
   * override to e.g. record the docs in your own {@link DocIdSet}. I override
   * this method to created a sampled FixedBitSet
   */
  @Override
  protected Docs createDocs(final int maxDoc) {
    return new SampledDocs(maxDoc, params);
  }
  
  /**
   * Corrects FacetCounts if sampling is used. Uses the {@link SamplingParams}
   * that are used to build this {@link SamplingFacetsCollector}
   */
  public FacetResult correctCountFacetResults(FacetResult res) {
    LabelAndValue[] fixedLabelValues = new LabelAndValue[res.labelValues.length];
    
    double sampleRatio = this.params.sampleRatio;
    
    for (int i = 0; i < res.labelValues.length; i++) {
      fixedLabelValues[i] = new LabelAndValue(res.labelValues[i].label,
          res.labelValues[i].value.floatValue() / sampleRatio);
    }
    return new FacetResult(res.dim, res.path, res.value, fixedLabelValues,
        res.childCount);
    
  }
  
  public static void main(String[] args) throws IOException {
    FixedBitSet bitSet = new FixedBitSet(10_000_000);
    Random r = new Random(0);
    long time = System.currentTimeMillis();
    for(int i = 0; i < bitSet.length(); ++i) {
      if (r.nextInt(4) == 0) {
        bitSet.set(i);
      }
    }
    System.out.println("generation: " + (System.currentTimeMillis() - time));
    System.out.println("num original bits:" + countBits(bitSet));
    
    SamplingParams sp = new SamplingParams();
    sp.sampleRatio = 0.001;
    SampledDocs sd = new SampledDocs(10_000_000, sp);
    
    time = System.currentTimeMillis();
    FixedBitSet sample = sd.createSample(bitSet);
    System.out.println("sample:  " + (System.currentTimeMillis() - time));
    System.out.println("Sampled: "+countBits(sample));
    
    time = System.currentTimeMillis();
    FixedBitSet sample2 = sd.createSample2(bitSet);
    System.out.println("sample2: " + (System.currentTimeMillis() - time));
    
    System.out.println("Sampled2: "+countBits(sample2));
    
  }

  static int countBits(DocIdSet dis) throws IOException {
    int i = 0;
    DocIdSetIterator it = dis.iterator();
    while (it.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
      ++i;
    }
    return i;
  }
}
