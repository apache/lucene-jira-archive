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

class BooleanScorer1 extends Scorer {
// Can be used by using BooleanScorer1 instead of BooleanScorer in BooleanQuery.

  private SubScorer scorers = null;

  private int maxCoord = 1;

  private int requiredMask = 0; // bits set for each required subscorer
  private int prohibitedMask = 0; // bits set for each prohibited subscorer

  private int nextMask = 1; // required/prohibited mask bit for next subscorer
  
  private boolean keepTableSorted = false;
  
  private float[] coordFactors;

  BooleanScorer1(Similarity similarity) {
    super(similarity);
  }
  
  /** Allows skipTo() and guarantees that doc() increases after next() returns true again. */
  void allowSkipTo() {
    keepTableSorted = true;
  }

  final void add(Scorer scorer, boolean required, boolean prohibited)
    throws IOException {
    int mask = 0;
    if (required || prohibited) {
      if (nextMask == 0) {
        throw new IndexOutOfBoundsException
          ("More than 32 required/prohibited clauses in query.");
      }
      mask = nextMask;
      nextMask = nextMask << 1;
    } else {
      mask = 0;
    }

    if (!prohibited) {
      maxCoord++;
    }

    if (prohibited) {
      prohibitedMask |= mask;                     // update prohibited mask
    } else if (required) {
      requiredMask |= mask;                       // update required mask
    }

    scorers = new SubScorer(scorer, required, prohibited, mask, scorers);
  }

  public void score(HitCollector hc) throws IOException {
    score(hc, Integer.MAX_VALUE);
  }

  protected boolean score(HitCollector hc, int max) throws IOException {
    if (coordFactors == null) {
      computeCoordFactors();
    }
    do {
      scoreFromCurrent(hc, max);
      if(tableNonEmpty()) {
        return true;
      }
    } while (tableRefillFromScorers() || tableNonEmpty());
    return false;
  }

  public int doc() {
    return current.doc;
  }

  public float score() {
    if (coordFactors == null) {
      computeCoordFactors();
    }
    return current.score * coordFactors[current.coord];
  }

  public boolean next() throws IOException {
    do {
      if (tableContainsNext()) {
        return true;
      }
    } while (tableRefillFromScorers() || tableNonEmpty());
    return false;
  }

  public boolean skipTo(int target) throws IOException {
    if (! keepTableSorted) {
      throw new UnsupportedOperationException();
    }
    if (target < tableEnd) { // Discard earlier computed scores
      while ((currentIndex < maxValidIndex)
             && (buckets[bucketsIndex[currentIndex]].doc < target)) {
        currentIndex++;
      }
    } else { // Discard current table contents completely
      tableEnd = target & ~MASK; // high bits of target, avoid looping over SIZE increments in tableRefillFromScorers in next().
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
      if (! more) {
        return false;
      }
    }
    return next();
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

  // The bucket table:
  private final int SIZE = 1 << 11;
  private final int MASK = SIZE - 1;
  private int tableEnd = 0;

  private int[] bucketsIndex = new int[SIZE]; // valid list == bucketsIndex[currentIndex..maxValidIndex-1]
  private final Bucket[] buckets = new Bucket[SIZE];
  private int currentIndex = 0;
  private int maxValidIndex = 0;

  private Bucket current; // set in checkCurrentBits for doc() and score(), otherwise unused.
  
  private void computeCoordFactors() {
    coordFactors = new float[maxCoord];
    for (int i = 0; i < maxCoord; i++) {
      coordFactors[i] = getSimilarity().coord(i, maxCoord-1);
    }
  }

  private boolean tableNonEmpty() {
    return currentIndex < maxValidIndex;
  }
  
  private void scoreFromCurrent(HitCollector hc, int max) {
    int beyondMaxIndex = 0;
    while (currentIndex < maxValidIndex) {
      if (currentCheckBits()) {
        if (current.doc < max){
          hc.collect(current.doc, score());
        } else {
          bucketsIndex[beyondMaxIndex++] = bucketsIndex[currentIndex]; // reuse later (check bits twice)
        }
      }
      currentIndex++;
    }
    maxValidIndex = beyondMaxIndex;
    currentIndex = 0;
  }

  private boolean tableContainsNext() {
    while (currentIndex < maxValidIndex) {
      if (currentCheckBits()) {
        currentIndex++;
        return true;
      } else {
        currentIndex++;
      }
    }
    return false;
  }
  
  private boolean currentCheckBits() { // check prohibited & required
    current = buckets[bucketsIndex[currentIndex]];
    return ((current.bits & prohibitedMask) == 0 &&
            (current.bits & requiredMask) == requiredMask);
  }

  private boolean tableRefillFromScorers() throws IOException {
    tableEnd += SIZE;
    boolean more = false;
    maxValidIndex = 0;
    currentIndex = 0;
    for (SubScorer sub = scorers; sub != null; sub = sub.next) {
      Scorer scorer = sub.scorer;
      while ((!sub.done) && (scorer.doc() < tableEnd)) {
        collectMasked(scorer.doc(), scorer.score(), sub.mask);
        sub.done = !scorer.next();
      }
      if (!sub.done) {
        more = true;
      }
    }
    if (keepTableSorted) {
      // bucketsIndex contains low bits of doc nrs in SIZE range up to tableEnd:
      Arrays.sort(bucketsIndex, currentIndex, maxValidIndex);
      // Complexity: n * log(n), n = maxValidIndex - currentIndex.
      // Keeping sorted order by scorer (as in DisjunctionSumScorer) is cheaper: n * log(nrScorers).
    }
    return more;
  }

  private void collectMasked(int doc, float score, int mask) {
    final int lowBitsDocNr = doc & MASK;
    Bucket bucket = buckets[lowBitsDocNr];
    if (bucket == null) {
      buckets[lowBitsDocNr] = bucket = new Bucket();
    }

    if (bucket.doc != doc) {                    // invalid bucket
      bucket.doc = doc;                         // set doc
      bucket.score = score;                     // initialize score
      bucket.bits = mask;                       // initialize mask
      bucket.coord = 1;                         // initialize coord
      bucketsIndex[maxValidIndex++] = lowBitsDocNr;
    } else {                                    // valid bucket
      bucket.score += score;                    // increment score
      bucket.bits |= mask;                      // add bits in mask
      bucket.coord++;                           // increment coord
    }
  }

}

class SubScorer {
  Scorer scorer;
  boolean done; // FIXME: remove from scorer list instead of marking done.
  boolean required = false;
  boolean prohibited = false;
  int mask;
  SubScorer next;

  SubScorer(Scorer scorer, boolean required, boolean prohibited,
                   int mask, SubScorer next)
    throws IOException {
    this.scorer = scorer;
    this.done = !scorer.next();
    this.required = required;
    this.prohibited = prohibited;
    this.mask = mask;
    this.next = next;
  }
}

class Bucket {
  int doc = -1;                                 // tells if bucket is valid
  float score;                                  // incremental score
  int bits;                                     // used for bool constraints
  int coord;                                    // count of terms in score
}

