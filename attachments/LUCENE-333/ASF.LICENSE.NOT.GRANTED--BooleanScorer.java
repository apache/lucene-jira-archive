package org.apache.lucene.search;

/**
 * Copyright 2004-2005 Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.util.Arrays;
import java.util.Comparator;

final class BooleanScorer extends Scorer {
  private SubScorer scorers = null;
  private BucketTable bucketTable = new BucketTable(this);

  private int maxCoord = 1;
  private float[] coordFactors = null;

  private int requiredMask = 0;
  private int prohibitedMask = 0;
  private int nextMask = 1;
  
  private boolean keepSorted = false;

  BooleanScorer(Similarity similarity) {
    super(similarity);
  }
  
  /** Allows skipTo() and guarantees that doc() increases after next() returns true. */
  void allowSkipTo() {
    keepSorted = true;
  }

  static final class SubScorer {
    public Scorer scorer;
    public boolean done; // FIXME: remove from next list instead of marking done.
    public boolean required = false;
    public boolean prohibited = false;
    public HitCollector collector;
    public SubScorer next;

    public SubScorer(Scorer scorer, boolean required, boolean prohibited,
                     HitCollector collector, SubScorer next)
      throws IOException {
      this.scorer = scorer;
      this.done = !scorer.next();
      this.required = required;
      this.prohibited = prohibited;
      this.collector = collector;
      this.next = next;
    }
  }

  final void add(Scorer scorer, boolean required, boolean prohibited)
    throws IOException {
    int mask = 0;
    if (required || prohibited) {
      if (nextMask == 0)
        throw new IndexOutOfBoundsException
          ("More than 32 required/prohibited clauses in query.");
      mask = nextMask;
      nextMask = nextMask << 1;
    } else
      mask = 0;

    if (!prohibited)
      maxCoord++;

    if (prohibited)
      prohibitedMask |= mask;                     // update prohibited mask
    else if (required)
      requiredMask |= mask;                       // update required mask

    scorers = new SubScorer(scorer, required, prohibited,
                            bucketTable.newCollector(mask), scorers);
  }

  private final void computeCoordFactors() {
    coordFactors = new float[maxCoord];
    for (int i = 0; i < maxCoord; i++)
      coordFactors[i] = getSimilarity().coord(i, maxCoord-1);
  }

  private int end;
  private Bucket current;

  public void score(HitCollector hc) throws IOException {
    next();
    score(hc, Integer.MAX_VALUE);
  }

  protected boolean score(HitCollector hc, int max) throws IOException {
    if (coordFactors == null)
      computeCoordFactors();

    boolean more;
    Bucket tmp;

    do {
      bucketTable.first = null;

      while (current != null) {         // more queued

        // check prohibited & required
        if ((current.bits & prohibitedMask) == 0 &&
            (current.bits & requiredMask) == requiredMask) {

          if (current.doc >= max){
            tmp = current;
            current = current.next;
            tmp.next = bucketTable.first;
            bucketTable.first = tmp;
            continue;
          }

          hc.collect(current.doc, current.score * coordFactors[current.coord]);
        }

        current = current.next;         // pop the queue
      }

      if( bucketTable.first != null){
        current = bucketTable.first;
        bucketTable.first = current.next;
        return true;
      }

      // refill the queue
      more = false;
      end += BucketTable.SIZE;
      for (SubScorer sub = scorers; sub != null; sub = sub.next) {
        if (!sub.done) {
          sub.done = !sub.scorer.score(sub.collector, end);
          if (!sub.done)
            more = true;
        }
      }

      if (keepSorted) {
        bucketTable.sortValidListByDocNr();
      }

      current = bucketTable.first;

    } while (current != null || more);

    return false;
  }

  public int doc() { return current.doc; }

  public boolean next() throws IOException {
    boolean more;
    do {
      while (bucketTable.first != null) {         // more queued
        current = bucketTable.first;
        bucketTable.first = current.next;         // pop the queue

        // check prohibited & required
        if ((current.bits & prohibitedMask) == 0 &&
            (current.bits & requiredMask) == requiredMask) {
          return true;
        }
      }

      // refill the queue
      more = false;
      end += BucketTable.SIZE;
      for (SubScorer sub = scorers; sub != null; sub = sub.next) {
        Scorer scorer = sub.scorer;
        while (!sub.done && scorer.doc() < end) {
          sub.collector.collect(scorer.doc(), scorer.score());
          sub.done = !scorer.next();
        }
        if (!sub.done) {
          more = true;
        }
      }

      if (keepSorted) {
        bucketTable.sortValidListByDocNr();
      }
    } while (bucketTable.first != null || more);

    return false;
  }

  public float score() {
    if (coordFactors == null)
      computeCoordFactors();
    return current.score * coordFactors[current.coord];
  }

  public boolean skipTo(int target) throws IOException {
    if (! keepSorted) {
      throw new UnsupportedOperationException();
    }
    
    if (target < end) {
      bucketTable.removeSmallerDocsValidList(target);
      current = bucketTable.first;
      return (current != null) || next();
    } else {
      bucketTable.first = null; // clear valid list
      boolean more = false;
      for (SubScorer sub = scorers; sub != null; sub = sub.next) {
        Scorer scorer = sub.scorer;
        if (!sub.done) {
          if (target <= scorer.doc()) {
            more = true;
          } else if (scorer.skipTo(target)) {
            more = true;
          } else {
            sub.done = true;
          }
        }
      }
      return more && next();
    }
  }

  static final class Bucket {
    int doc = -1;                                 // tells if bucket is valid
    float       score;                            // incremental score
    int bits;                                     // used for bool constraints
    int coord;                                    // count of terms in score
    Bucket      next;                             // next valid bucket
  }

  /** A simple hash table of document scores within a range. */
  private class BucketTable {
    public static final int SIZE = 1 << 11;
    public static final int MASK = SIZE - 1;

    final Bucket[] buckets = new Bucket[SIZE];
    Bucket first = null;                          // head of valid list

    private BooleanScorer scorer;

    public BucketTable(BooleanScorer scorer) {
      this.scorer = scorer;
    }

    public final int size() { return SIZE; }

    public HitCollector newCollector(int mask) {
      return new Collector(mask, this);
    }

    void removeSmallerDocsValidList(int target) {
      if (! keepSorted) { // Might discard docs that should have been scored:
        throw new UnsupportedOperationException();
      }
  //System.out.println(this + " removing bucket docs smaller than " + target);
      /* Discard earlier computed scores, sigh. */
      while ((first != null) && (first.doc < target)) {
        first = first.next;
      }
    }

    private Bucket[] tmpBuckets = new Bucket[SIZE];
    void sortValidListByDocNr() {
      Bucket b = first;
      int nrValidBuckets = 0;
      while (b != null) {
        tmpBuckets[nrValidBuckets++] = b;
        b = b.next;
      }
  //System.out.println(this + " sorting " + nrValidBuckets + " buckets"); sorting is shown only once, why?
      Arrays.sort(tmpBuckets, 0, nrValidBuckets,
        new Comparator() {
          public int compare(Object b1, Object b2) {
            return ((Bucket) b1).doc - ((Bucket) b2).doc;
          }
        }
      );
      if (nrValidBuckets > 0) {
        first = tmpBuckets[0];
        b = tmpBuckets[0];
        for (int i = 1; i < nrValidBuckets; i++) {
          //if (b.doc >= tmpBuckets[i].doc) {
          //  throw new AssertionError("tmpBuckets not sorted");
          //}
          b.next = tmpBuckets[i];
          b = tmpBuckets[i];
        }
        b.next = null;
      }
    }
  }

  static final class Collector extends HitCollector {
    private BucketTable bucketTable;
    private int mask;
    public Collector(int mask, BucketTable bucketTable) {
      this.mask = mask;
      this.bucketTable = bucketTable;
    }
    public final void collect(final int doc, final float score) {
      final BucketTable table = bucketTable;
      final int i = doc & BucketTable.MASK;
      Bucket bucket = table.buckets[i];
      if (bucket == null)
        table.buckets[i] = bucket = new Bucket();

      if (bucket.doc != doc) {                    // invalid bucket
        bucket.doc = doc;                         // set doc
        bucket.score = score;                     // initialize score
        bucket.bits = mask;                       // initialize mask
        bucket.coord = 1;                         // initialize coord

        bucket.next = table.first;                // push onto valid list
        table.first = bucket;
      } else {                                    // valid bucket
        bucket.score += score;                    // increment score
        bucket.bits |= mask;                      // add bits in mask
        bucket.coord++;                           // increment coord
      }
    }
  }

  public Explanation explain(int doc) {
    throw new UnsupportedOperationException();
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("boolean(");
    for (SubScorer sub = scorers; sub != null; sub = sub.next) {
      buffer.append(sub.scorer.toString());
      buffer.append(" ");
    }
    buffer.append(")");
    return buffer.toString();
  }

}
