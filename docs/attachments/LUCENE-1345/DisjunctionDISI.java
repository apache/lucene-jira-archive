package org.apache.lucene.search;

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

import java.util.List;
import java.util.Iterator;
import java.io.IOException;

import org.apache.lucene.util.DisiDocQueue;

/* Derived from org.apache.lucene.search.DisjunctionSumScorer of July 2008 */

/** A disjunction of <code>DocIdSetIterator</code>s.
 */
class DisjunctionDISI extends DocIdSetIterator {
  private final int nrDisis;
  
  protected final List subDisis;
  
  /** The minimum number of DocIdSetIterators that should match. */
  private final int minimumNrMatchers;
  private final DocIdSetIterator[] requiredDisis; // one less than minimumNrMatcher elements
  
  /** The disiDocQueue contains all DocIdSetIterators ordered by their current doc(),
   * with the minimum at the top.
   * <br>The disiDocQueue is initialized the first time next() or skipTo() is called.
   * <br>An exhausted DocIdSetIterator is immediately removed from the disiDocQueue.
   * <br>If less than the minimumNrMatchers DocIdSetIterators
   * remain in the disiDocQueue next() and skipTo() return false.
   * <p>
   * After each to call to next() or skipTo()
   * <code>nrMatchers</code> is the number of matching DocIdSetIterators,
   * and all DocIdSetIterators are after the matching doc, or are exhausted.
   */
  private DisiDocQueue disiDocQueue;
  
  /** The document number of the current match. */
  private int currentDoc = -1;

  /** The number of subscorers that provide the current match. */
  protected int nrMatchers = -1;

  private float currentScore = Float.NaN;
  
  /** Construct a <code>DisjunctionScorer</code>.
   * @param subScorers A collection of at least two subscorers.
   * @param minimumNrMatchers The positive minimum number of subscorers that should
   * match to match this query.
   * <br>When <code>minimumNrMatchers</code> is bigger than
   * the number of <code>subScorers</code>,
   * no matches will be produced.
   * <br>When minimumNrMatchers equals the number of subScorers,
   * it more efficient to use <code>ConjunctionScorer</code>.
   */
  public DisjunctionDISI(List subDisis, int minimumNrMatchers) {
    nrDisis = subDisis.size();

    if (minimumNrMatchers <= 0) {
      throw new IllegalArgumentException("Minimum nr of matchers must be positive");
    }
    if (nrDisis <= 1) {
      throw new IllegalArgumentException("There must be at least 2 DocIdSetIterators");
    }

    this.minimumNrMatchers = minimumNrMatchers;
    this.subDisis = subDisis;
    this.requiredDisis = new DocIdSetIterator[Integer.minimum(minimumNrMatchers, nrDisis)];
    initDisiDocQueue();
  }
  
  /** Construct a <code>DisjunctionDISI</code>, using one as the minimum number
   * of matching DocIdSetIterators.
   */
  public DisjunctionDISI(List subDisis) {
    this(subDisis, 1);
  }

  /** Called to initialize <code>disiDocQueue</code>. */
  private void initDisiDocQueue() throws IOException {
    Iterator disisIt = subDisis.iterator();
    disiDocQueue = new DisiDocQueue(nrDisis);
    while (disisIt.hasNext()) {
      DocIdSetIterator disi = (DocIdSetIterator) disisIt.next();
      if (disi.next()) { // doc() method will be used in scorerDocQueue.
        disiDocQueue.insert(disi);
      }
    }
    int nrDisisInRequired = 0;
    requiredDisis = new DocIdSetIterator[minimumNrMatchers - 1];
    while (nrDisisInRequired < minimumNrMatchers - 1) && (disiDocQueue.size() > 0) {
      requiredDisis[nrDisisInRequired++] = disiDocQueue.pop();
    }
    minReq = 0;
    maxReq = nrDisisInRequired - 1;
  }

  public boolean next() throws IOException {
    return (disiDocQueue.size() >= 1) && advanceAfterCurrent();
  }


  /** Advance all sub DocIdSetIterators after the current document determined by the
   * top of the <code>disiDocQueue</code>, except for 1 less than minimumNrOfMatchers.
   * <br>On entry the <code>disiDocQueue</code> has at least 1 available.
   * At least minimumNrOfMatchers DocIdSetIterators will be advanced.
   * @return true iff there is a match.
   * <br>In case there is a match, </code>currentDoc</code>
   * and </code>nrMatchers</code> describe the match.
   */
  protected boolean advanceAfterCurrent() throws IOException {
    if (minimumNrMatchers >= 2) { // put docs of requiredDisis[0 .. minimumNrMatchers - 1] at queue top:
      while (requiredDisis[minReq].doc() != disiDocQueue.topDoc()) {
        if (! requiredDisis[minReq].skipTo(disiDocQueue.topDoc())) {
          requiredDisis[minReq] = disiDocQueue.pop(requiredDisis[minReq]);
          if (disiDocQueue.size() == 0) {
            return false; // not enough disis left.
          }
        }
        if (requiredDisis[minReq].doc() > disiDocQueue.topDoc()) {
          // CHECKME: sth like replaceTop() was added to PriorityQueue, add it to disiDocQueue and use that:
          requiredDisis[minReq] = disiDocQueue.replaceTop(requiredDisis[minReq]);
        }
        if (++minReq == minNrMatchers - 1) minReq = 0;
      }
    }
    currentDoc = disiDocQueue.topDoc();
    nrMatchers = minimumNrMatchers;
    do { // Until all disis in disiDocQueue after currentDoc
      if (! disiDocQueue.topNextAndAdjustElsePop()) {
        if (disiDocQueue.size() == 0) {
          return true; // nothing more to advance in the queue
        }
      }
      if (disiDocQueue.topDoc() != currentDoc) {
        return true; // all remaining disis are after currentDoc.
      }
      nrMatchers++;
    } while (true);
  }
  
  public int doc() { return currentDoc; }

  /** Returns the number of subscorers matching the current document.
   * Initially invalid, until {@link #next()} is called the first time.
   */
  public int nrMatchers() {
    return nrMatchers;
  }

  /** Skips to the first match beyond the current whose document number is
   * greater than or equal to a given target.
   * <br>When this method is used the {@link #explain(int)} method should not be used.
   * @param target The target document number.
   * @return true iff there is such a match.
   */
  public boolean skipTo(int target) throws IOException {
    if (disiDocQueue.size() == 0) {
      return false;
    }
    if (target <= currentDoc) { // CHECKME: skipTo() semantics?
      return true;
    }
    do {
      if (disiDocQueue.topDoc() >= target) {
        return advanceAfterCurrent();
      } else if (! disiDocQueue.topSkipToAndAdjustElsePop(target)) {
        if (disiDocQueue.size() < minimumNrMatchers) {
          return false;
        }
      }
    } while (true);
  }
}
