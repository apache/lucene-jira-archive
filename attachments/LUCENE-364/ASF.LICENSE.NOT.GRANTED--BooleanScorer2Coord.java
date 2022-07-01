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

/** A Scorer for boolean queries, with a coordination factor.
 * <p>
 * Implementation notes:<br>
 * Extends BooleanScorer2 with match counting for the coordination factor.<br>
 * When "counting" is used in a name it means counting the number
 * of matching scorers.<br>
 * When "sum" is used in a name it means score value summing
 * over the matching scorers.
 */
class BooleanScorer2Coord extends BooleanScorer2 {

  /** Computes the coordination factors.
   * Counts the number of matching subscorers.
   * Uses the Similarity of the surrounding class to compute
   * a coordination factor based on the number of matchers 
   * and the maximum number of matchers.
   */
  private class Coordinator {
    int maxCoord = 0; // to be increased for each non prohibited scorer

    private float[] coordFactors = null;

    void init() { // use after all scorers have been added.
      coordFactors = new float[maxCoord + 1];
      Similarity sim = getSimilarity();
      for (int i = 0; i <= maxCoord; i++) {
        coordFactors[i] = sim.coord(i, maxCoord);
      }
    }

    int nrMatchers; // to be increased by score() of match counting scorers.

    void initDoc() {
      nrMatchers = 0;
    }
    
    float coordFactor() {
      return coordFactors[nrMatchers];
    }
  }
  
  /* To do: override makeSumScorer()
   * for special cases, eg. for a single ConjunctionScorer
   * and return these in getDelegatedScorer().
   */

  /** The Coordinator for this BooleanScorer2Coord */
  private final Coordinator coordinator;

  /** Create a BooleanScorer2Coord.
   * @param similarity A Similarity of which only the
   *                   {@link Similarity#coord(int,int)} method is used.
   */
  public BooleanScorer2Coord(Similarity similarity) {
    super(similarity);
    coordinator = new Coordinator();
  }

  /** Add a Scorer for a boolean clause.
   * @param scorer The scorer of the boolean clause.
   * @param occur true How the clause should occur in the result.
   */
  public void add(final Scorer scorer, BooleanClause.Occur occur) {
    if (occur != BooleanClause.Occur.MUST_NOT) {
      coordinator.maxCoord++;
    }
    super.add(scorer, occur);
  }

  /** Initialize the match counting scorer that sums all the scores. */
  protected void initSumScorer() {
    coordinator.init();
    super.initSumScorer();
  }

  /** Count a scorer as a single match. */
  private class SingleMatchScorer extends Scorer {
    private Scorer scorer;
    SingleMatchScorer(Scorer scorer) {
      super(scorer.getSimilarity());
      this.scorer = scorer;
    }
    public float score() throws IOException {
      coordinator.nrMatchers++;
      return scorer.score();
    }
    public int doc() {
      return scorer.doc();
    }
    public boolean next() throws IOException {
      return scorer.next();
    }
    public boolean skipTo(int docNr) throws IOException {
      return scorer.skipTo(docNr);
    }
    public Explanation explain(int docNr) throws IOException {
      coordinator.nrMatchers++;
      return scorer.explain(docNr);
    }
    public String toString() {
      return getClass().getName() + " on: " + scorer;
    }
  }

  /** Return a match counting scorer for a disjunction.
   * @param scorers The scorers, at least one of which must match in the result.
   *                At least one scorer must be given.
   * @return A Scorer over the given scorers
   *         that counts the number of matchers in the {@link #coordinator}.
   */
  protected Scorer makeDisjunctionScorer(List scorers) {
    if (scorers.size() == 1) {
       return new SingleMatchScorer((Scorer) scorers.get(0));
    } else {
      return new DisjunctionSumScorer(scorers) {
        public float score() throws IOException {
          coordinator.nrMatchers += super.nrMatchers;
          return super.score();
        }
        public Explanation explain(int docNr) throws IOException {
          // DisjunctionSumScorer.explain() does not call score():
          coordinator.nrMatchers += super.nrMatchers;
          return super.explain(docNr);
        }
      };
    }
  }
  
  /** Return a match counting for a conjunction.
   * @param requiredScorers The scorers, all of which must match in the result.
   *                At least one scorer must be given.
   * @return A Scorer over the given scorers
   *         that counts the number of matchers in the {@link #coordinator}.
   */
  protected Scorer makeConjunctionScorer(List requiredScorers) {
    if (requiredScorers.size() == 1) {
      return new SingleMatchScorer((Scorer) requiredScorers.get(0));
    } else {
      final int requiredNrMatchers = requiredScorers.size();
      ConjunctionScorer cs = new ConjunctionScorer(DEFAULT_SIMILARITY) {
        public float score() throws IOException {
          coordinator.nrMatchers += requiredNrMatchers;
          /* Here a ConjunctionScorer without coordination factor could be used.
           * But as all scorers match, DEFAULT_SIMILARITY super.score() always
           * is 1, so the sum of the scores of the requiredScorers
           * is used as score:
           */
          return super.score();
        }
        public Explanation explain(int docNr) throws IOException {
          // ConjunctionScorer.explain() does not call score():
          coordinator.nrMatchers += requiredNrMatchers;
          return super.explain(docNr);
        }
      };
      Iterator rsi = requiredScorers.iterator();
      while (rsi.hasNext()) {
        cs.add((Scorer) rsi.next());
      }
      return cs;
    }
  }

  /** Return this BooleanScorer2Coord. */
  public Scorer getDelegatedScorer() {
    return this;
  }

  /** Return the score of the current document.
   * The score is the sum of all matching scorers multiplied
   * by the coordination factor.
   */
  public float score() throws IOException {
    coordinator.initDoc();
    float sum = sumScorer.score();
    return sum * coordinator.coordFactor();
  }

  /** Explain the score of a given document. */
  public Explanation explain(int doc) throws IOException {
    if (sumScorer == null) {
      initSumScorer();
    }
    Explanation res = new Explanation();
    coordinator.initDoc();
    Explanation csExpl = sumScorer.explain(doc);
    float coordFac = coordinator.coordFactor();
    res.setDescription("coordination factor " + coordFac + " times");
    res.setValue(coordFac * csExpl.getValue());
    res.addDetail(csExpl);
    return res;
  }
}

