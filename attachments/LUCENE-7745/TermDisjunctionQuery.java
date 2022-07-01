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
package org.apache.lucene.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.TermStates;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.PriorityQueue;
import org.apache.lucene.util.SmallFloat;

/**
 * Specialization of a BooleanQuery of SHOULD clauses over term queries
 * when the BM25 similarity should be used.
 */
public final class TermDisjunctionQuery extends Query {

  /** Cache of decoded bytes. */
  private static final float[] LENGTH_TABLE = new float[256];

  static {
    for (int i = 0; i < 256; i++) {
      LENGTH_TABLE[i] = SmallFloat.byte4ToInt((byte) i);
    }
  }

  private final Set<Term> terms;

  public TermDisjunctionQuery(Set<Term> terms) {
    this.terms = Collections.unmodifiableSet(new HashSet<>(Objects.requireNonNull(terms)));
  }

  @Override
  public String toString(String field) {
    return getClass().getSimpleName() + terms;
  }

  @Override
  public boolean equals(Object obj) {
    if (sameClassAs(obj) == false) {
      return false;
    }
    TermDisjunctionQuery that = (TermDisjunctionQuery) obj;
    return terms.equals(that.terms);
  }

  @Override
  public int hashCode() {
    int h = classHash();
    h = 31 * h + terms.hashCode();
    return h;
  }

  private BooleanQuery toBooleanQuery(Term[] terms, TermStates[] termStates) {
    final BooleanQuery.Builder builder = new BooleanQuery.Builder();
    for (int i = 0; i < terms.length; ++i) {
      TermQuery query;
      if (termStates == null) {
        query = new TermQuery(terms[i]);
      } else {
        query = new TermQuery(terms[i], termStates[i]);
      }
      builder.add(query, Occur.SHOULD);
    }
    return builder.build();
  };

  public Weight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
    final Term[] terms = this.terms.toArray(new Term[0]);

    if (scoreMode == ScoreMode.COMPLETE_NO_SCORES || searcher.getSimilarity().getClass() != BM25Similarity.class) {
      return toBooleanQuery(terms, null).createWeight(searcher, scoreMode, boost);
    }

    final BM25Similarity sim = (BM25Similarity) searcher.getSimilarity();
    final TermStates[] termStates = new TermStates[terms.length];
    final float[] weights = new float[terms.length];
    final float[][] cache = new float[terms.length][];
    for (int i = 0; i < terms.length; ++i) {
      Term term = terms[i];
      termStates[i] = TermStates.build(searcher.getIndexReader().getContext(), term, true);
      CollectionStatistics collStats = searcher.collectionStatistics(term.field());
      TermStatistics termStats = searcher.termStatistics(term, termStates[i]);
      float idf = sim.idfExplain(collStats, termStats).getValue().floatValue();
      float k1 = sim.getK1();
      weights[i] = (k1 + 1) * boost * idf;
      float avgdl = (float) (collStats.sumTotalTermFreq() / (double) collStats.docCount());
      float b = sim.getB();
      cache[i] = new float[256];
      for (int j = 0; j < cache[i].length; j++) {
        cache[i][j] = k1 * ((1 - b) + b * LENGTH_TABLE[j] / avgdl);
      }
    }

    final Weight booleanWeight = toBooleanQuery(terms, termStates).createWeight(searcher, scoreMode, boost);

    return new Weight(this) {

      @Override
      public boolean isCacheable(LeafReaderContext ctx) {
        return booleanWeight.isCacheable(ctx);
      }

      @Override
      public void extractTerms(Set<Term> terms) {
        booleanWeight.extractTerms(terms);
      }

      @Override
      public Explanation explain(LeafReaderContext context, int doc) throws IOException {
        return booleanWeight.explain(context, doc);
      }

      @Override
      public Scorer scorer(LeafReaderContext context) throws IOException {
        return booleanWeight.scorer(context);
      }
      
      @Override
      public ScorerSupplier scorerSupplier(LeafReaderContext context) throws IOException {
        return booleanWeight.scorerSupplier(context);
      }

      @Override
      public BulkScorer bulkScorer(LeafReaderContext context) throws IOException {
        List<PostingsEnum> postings = new ArrayList<>();
        List<NumericDocValues> norms = new ArrayList<>();
        float[] weights2 = new float[0];
        List<float[]> cache2 = new ArrayList<>();
        for (int i = 0; i < terms.length; ++i) {
          Term term = terms[i];
          TermState termState = termStates[i].get(context);
          if (termState != null) {
            TermsEnum te = context.reader().terms(term.field()).iterator();
            te.seekExact(term.bytes(), termState);
            PostingsEnum pe = te.postings(null, PostingsEnum.FREQS);
            postings.add(pe);
            norms.add(context.reader().getNormValues(term.field()));
            weights2 = ArrayUtil.grow(weights2, postings.size());
            weights2[postings.size() - 1] = weights[i];
            cache2.add(cache[i]);
          }
        }
        if (postings.isEmpty()) {
          return null;
        }
        return new BS1(
            postings.toArray(new PostingsEnum[0]),
            norms.toArray(new NumericDocValues[0]),
            ArrayUtil.copyOfSubArray(weights2, 0, postings.size()),
            cache2.toArray(new float[0][]));
      }

    };
  }

  private static class PostingsOrd {
    public final PostingsEnum pe;
    public final int ord;

    PostingsOrd(PostingsEnum pe, int ord) {
      this.pe = pe;
      this.ord = ord;
    }
  }

  private static class BS1 extends BulkScorer {

    // We might want to make it a bit larger to help with GPU acceleration
    private static final int WINDOW_SCALE = 11;
    private static final int MASK = (1 << WINDOW_SCALE) - 1;

    private final PriorityQueue<PostingsOrd> pq;
    private final NumericDocValues[] norms;
    private final float[] weights;
    private final float[][] cache;
    private final long cost;

    private final int[] docIDBuffer;
    private final int[] ordsBuffer;
    private final int[] freqBuffer;
    private final byte[] normBuffer;
    private final long[] matches = new long[1 << (WINDOW_SCALE - 6)];
    private final double[] scoreBuffer = new double[1 << WINDOW_SCALE];
    private final FakeScorer scorer = new FakeScorer();

    public BS1(PostingsEnum[] postings, NumericDocValues[] norms, float[] weights, float[][] cache) {
      this.norms = norms;
      this.weights = weights;
      this.cache = cache;
      pq = new PriorityQueue<PostingsOrd>(postings.length) {
        @Override
        protected boolean lessThan(PostingsOrd a, PostingsOrd b) {
          return a.pe.docID() < b.pe.docID();
        }
      };
      long cost = 0;
      for (int i = 0; i < postings.length; ++i) {
        pq.add(new PostingsOrd(postings[i], i));
        cost += postings[i].cost();
      }
      this.cost = cost;
      this.docIDBuffer = new int[postings.length << WINDOW_SCALE];
      this.ordsBuffer = new int[postings.length << WINDOW_SCALE];
      this.freqBuffer = new int[postings.length << WINDOW_SCALE];
      this.normBuffer = new byte[postings.length << WINDOW_SCALE];
    }

    @Override
    public int score(LeafCollector collector, Bits acceptDocs, int min, int max) throws IOException {
      collector.setScorer(scorer);

      while (pq.top().pe.docID() < min) {
        pq.top().pe.advance(min);
        pq.updateTop();
      }

      while (pq.top().pe.docID() < max) {
        scoreWindow(collector, acceptDocs, max);
      }

      return pq.top().pe.docID();
    }

    private void scoreWindow(LeafCollector collector, Bits acceptDocs, int max) throws IOException {
      PostingsOrd top = pq.top();
      int windowBase = top.pe.docID() & ~MASK;
      int windowMax = Math.min(max, windowBase + (1 << WINDOW_SCALE));

      int numMatches = collectMatches(top, acceptDocs, windowMax);

      computeScores(
          docIDBuffer, freqBuffer, normBuffer, ordsBuffer, numMatches,
          matches, scoreBuffer,
          weights, cache);

      replay(windowBase, matches, scoreBuffer, collector);
    }

    private int collectMatches(PostingsOrd top, Bits acceptDocs, int windowMax) throws IOException {
      int numMatches = 0;
      do {
        NumericDocValues normValues = norms[top.ord];
        for (int doc = top.pe.docID(); doc < windowMax; doc = top.pe.nextDoc()) {
          if (acceptDocs == null || acceptDocs.get(doc)) {
            docIDBuffer[numMatches] = doc;
            ordsBuffer[numMatches] = top.ord;
            freqBuffer[numMatches] = top.pe.freq();
            if (normValues == null) {
              normBuffer[numMatches] = (byte) 1L;
            } else {
              boolean found = normValues.advanceExact(doc);
              assert found;
              normBuffer[numMatches] = (byte) normValues.longValue();
            }
            numMatches++;
          }
        }
        top = pq.updateTop();
      } while (top.pe.docID() < windowMax);

      return numMatches;
    }

    private void replay(int windowBase, long[] matches, double[] scores, LeafCollector collector) throws IOException {
      for (int idx = 0; idx < matches.length; idx++) {
        long bits = matches[idx];
        while (bits != 0L) {
          int ntz = Long.numberOfTrailingZeros(bits);
          int windowIndex = idx << 6 | ntz;
          int doc = windowBase | windowIndex;
          scorer.doc = doc;
          scorer.score = (float) scores[windowIndex];
          collector.collect(doc);
          scores[windowIndex] = 0;
          bits ^= 1L << ntz;
        }
      }
      Arrays.fill(matches, 0L);
    }

    @Override
    public long cost() {
      return cost;
    }

  }

  // TODO: GPU
  private static void computeScores(
      int[] docs, int[] freqs, byte[] norms, int[] ords, int length,
      long[] matches, double[] scores,
      float[] weights, float[][] cache) {
    for (int i = 0; i < length; ++i) {
      int doc = docs[i];
      float freq = freqs[i];
      int ord = ords[i];
      double norm = cache[ord][norms[i] & 0xFF];
      float score = weights[ord] * (float) (freq / (freq + norm)); // bm25
      final int windowIndex = doc & BS1.MASK;
      final int matchesIdx = windowIndex >>> 6;
      matches[matchesIdx] |= 1L << windowIndex;
      scores[windowIndex] += score;
    }
  }

}
