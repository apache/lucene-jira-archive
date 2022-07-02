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
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/** A Scorer for boolean queries, without a oordination factor.
 * <br>Uses ConjunctionScorer, DisjunctionScorer, ReqOptScorer and ReqExclScorer.
 * <br>Implements skipTo(), and has no limitations on the numbers of added scorers.
 * <p>
 * Implementation notes:<br>
 * When "sum" is used in a name it means score value summing
 * over the matching scorers.
 */
class BooleanScorer2 extends Scorer {

  /** The required scorers */
  protected ArrayList requiredScorers = new ArrayList();

  /** The optional scorers */
  protected ArrayList optionalScorers = new ArrayList();

  /** The prohibited scorers */
  protected ArrayList prohibitedScorers = new ArrayList();

  /** The scorer to which all scoring will be delegated. */
  protected Scorer sumScorer = null;

  /** A default similarity */
  protected static final Similarity DEFAULT_SIMILARITY = new DefaultSimilarity();

  /** Create a BooleanScorer2. No Similarity is used. */
  public BooleanScorer2() {
    super(null);
  }

  /** Create a BooleanScorer2 allowing subclasses to provide a Similarity.
   * @param similarity A Similarity for the subclass. This is not used here.
   */
  protected BooleanScorer2(Similarity similarity) {
    super(similarity);
  }

  /** Add a Scorer for a boolean clause.
   * @param scorer The scorer of the boolean clause.
   * @param occur How the clause should occur in the result.
   */
  public void add(final Scorer scorer, BooleanClause.Occur occur) {
    if (occur == BooleanClause.Occur.MUST) {
      requiredScorers.add(scorer);
    } else if (occur == BooleanClause.Occur.MUST_NOT) {
      prohibitedScorers.add(scorer);
    } else if (occur == BooleanClause.Occur.SHOULD) {
      optionalScorers.add(scorer);
    } else {
      throw new IllegalArgumentException("Unknown occur: " + occur);
    }
  }

  /** Initialize the scorer to which all scoring will be delegated. */
  protected void initSumScorer() {
    sumScorer = makeSumScorer();
  }
  
  /** Return a scorer combining the given required scorer and the
   * prohibited scorers.
   * @param requiredSumScorer The required scorer.
   */
  private Scorer makeReqProhibitedScorer(Scorer requiredSumScorer) {
    return (prohibitedScorers.size() == 0)
           ? requiredSumScorer
           : new ReqExclScorer(
                   requiredSumScorer,
                   ((prohibitedScorers.size() == 1)
                     ? (Scorer) prohibitedScorers.get(0)
                     : new DisjunctionSumScorer(prohibitedScorers)));
  }

  /** Return a scorer for a disjunction.
   * @param scorers The scorers, at least one of which must match in the result.
   *                At least one scorer must be given.
   * @return A DisjunctionScorer over the given scorers.
   *         In case only one scorer is given, it is returned.
   */
  protected Scorer makeDisjunctionScorer(List scorers) {
    return (scorers.size() == 1)
           ? (Scorer) scorers.get(0)
           : new DisjunctionSumScorer(scorers);
  }

  /** Return a scorer for a conjunction.
   * @param requiredScorers The scorers, all of which must match in the result.
   *                        At least one scorer must be given.
   * @return A ConjunctionScorer over the given scorers.
   *         In case only one scorer is given, it is returned.
   */
  protected Scorer makeConjunctionScorer(List requiredScorers) {
    if (requiredScorers.size() == 1) {
      return (Scorer) requiredScorers.get(0);
    } else {
      ConjunctionScorer cs = new ConjunctionScorer(DEFAULT_SIMILARITY);
      Iterator rsi = requiredScorers.iterator();
      while (rsi.hasNext()) {
        cs.add((Scorer) rsi.next());
      }
      return cs;
    }
  }

  /** Return the scorer to which BooleanScorer2 delegates.
   * Uses the required, optional and prohibited scorers that have been added.
   */
  protected Scorer makeSumScorer() {
    return (requiredScorers.size() > 0)
          ? makeSumScorer2( makeConjunctionScorer(requiredScorers),
                            optionalScorers)
          : (optionalScorers.size() > 0)
          /* Without required scorers, (at least one of) the optional scorer(s)
           * is/are required.
           */
          ? makeSumScorer2( makeDisjunctionScorer(optionalScorers),
                            new ArrayList(0)) // no optional scorers left
          : new NonMatchingScorer(); // only prohibited scorers, if any
  }

  /** Return the scorer to which BooleanScorer2 delegates.
   * Uses the arguments and the added prohibited scorers.
   * @param requiredSumScorer A required scorer already built.
   * @param optionalScorers A list of optional scorers, possibly empty.
   */
  private Scorer makeSumScorer2(
      Scorer requiredSumScorer,
      List optionalScorers)
  {
    Scorer reqScorer = makeReqProhibitedScorer(requiredSumScorer);
    return (optionalScorers.size() == 0) 
           ? reqScorer
           : new ReqOptSumScorer( reqScorer,
                                  makeDisjunctionScorer(optionalScorers));
  }

  /** Return the scorer to which this BooleanScorer2 delegates.
   * Use only after all scorers have been added.
   */
  public Scorer getDelegatedScorer() {
    if (sumScorer == null) {
      initSumScorer();
    }
    return sumScorer;
  }

  /** Delegated. */
  public int doc() { return sumScorer.doc(); }

  /** Delegated. */
  public boolean next() throws IOException {
    if (sumScorer == null) {
      initSumScorer();
    }
    return sumScorer.next();
  }

  /** Delegated. */
  public float score() throws IOException { return sumScorer.score(); }

  /** Delegated. */
  public boolean skipTo(int target) throws IOException {
    if (sumScorer == null) {
      initSumScorer();
    }
    return sumScorer.skipTo(target);
  }

  /** Delegated. */
  public Explanation explain(int doc) throws IOException {
    if (sumScorer == null) {
      initSumScorer();
    }
    return sumScorer.explain(doc);
  }
}

