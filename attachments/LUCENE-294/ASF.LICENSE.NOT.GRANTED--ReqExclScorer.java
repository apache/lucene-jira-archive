package org.apache.lucene.search;

/**
 * Copyright 2005 The Apache Software Foundation
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


/** A Scorer for queries with a required subscorer and an excluding (prohibited) subscorer.
 * <br>
 * This <code>Scorer</code> implements {@link Scorer#skipTo(int)},
 * and it uses the skipTo() on the given scorers.
 */
class ReqExclScorer extends Scorer {
  private Scorer reqScorer;
  private Scorer exclScorer;
  private boolean firstTimeExcl = true;

  /** Construct a <code>ReqExclScorer</code>.
   * @param reqScorer The scorer that must match, except where
   * @param exclScorer indicates exclusion.
   */
  public ReqExclScorer(
      Scorer reqScorer,
      Scorer exclScorer) {
    super(null); // No similarity used.
    this.reqScorer = reqScorer;
    this.exclScorer = exclScorer;
  }

  public boolean next() throws IOException {
    return reqScorer.next() && toNonExcluded();
  }

  /** Skips to the first match beyond the current whose document number is
   * greater than or equal to a given target.
   * <br>When this method is used the {@link #explain(int)} method should not be used.
   * @param target The target document number.
   * @return true iff there is such a match.
   */
  public boolean skipTo(int target) throws IOException {
    return reqScorer.skipTo(target) && toNonExcluded();
  }
  
  /** Advance to non excluded doc.
   *  Advance while excluded. <br>
   *  On entry reqScorer was advanced once successfully via next() or skipTo(),
   *  and exclScorer.doc() may still exclude reqScorer.doc().
   *
   * @return                  true iff there is a non excluded required doc.
   */
  private boolean toNonExcluded() throws IOException {
    if (firstTimeExcl) {
      firstTimeExcl = false;
      if (! exclScorer.skipTo(reqScorer.doc())) {
        exclScorer = null; // exhausted at start
      }
    }
    if (exclScorer == null) {
      return true; // reqScorer.doc() not excluded.
    }
    int reqDoc = reqScorer.doc(); // may be excluded
    do {
      int exclDoc = exclScorer.doc();
      if (reqDoc < exclDoc) {
        return true; // reqScorer advanced to before exclScorer: not excluded
      }
      if (reqDoc == exclDoc) { // may not hold only first time in do/while.
        if (! reqScorer.next()) {
          return false;
        }
        reqDoc = reqScorer.doc();
        // assert reqDoc > exclDoc;
      }
    } while (exclScorer.skipTo(reqDoc));
    exclScorer = null; // exhausted, no more exclusions
    return true;
  }

  /**
   * @return    The value of doc() on the required scorer. <br>
   *      next() or skipTo() should already have returned true.
   */
  public int doc() {
    return reqScorer.doc(); // reqScorer may be null when next() or skipTo() already return false
  }

  /** Returns the score of the current document matching the query.
   * Initially invalid, until {@link #next()} is called the first time.
   * @return The score of the required scorer.
   */
  public float score() throws IOException {
    return reqScorer.score(); // reqScorer may be null when next() or skipTo() already return false
  }
  
  public Explanation explain(int doc) throws IOException {
    Explanation res = new Explanation();
    if (exclScorer.skipTo(doc) && (exclScorer.doc() == doc)) {
      res.setDescription("excluded");
    } else {
      res.setDescription("not excluded");
      res.addDetail(reqScorer.explain(doc));
    }
    return res;
  }
}
