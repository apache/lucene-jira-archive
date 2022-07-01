package org.apache.lucene.search;

/**
 * Copyright 2004 Paul Elschot
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

import java.util.Collection;

/** A DisjunctionScorer that adds all subscores and applies coord().
 */
public class DisjunctionSumCoordScorer extends DisjunctionScorer {
  public DisjunctionSumCoordScorer( Collection subScorers, int minimumNrMatchers, Similarity similarity) {
    super(subScorers, minimumNrMatchers, similarity);
  }

  public DisjunctionSumCoordScorer( Collection subScorers, Similarity similarity) {
    super(subScorers, similarity);
  }
  
  protected float combineScores() {
    float sumScore = currentScores[0];
    for (int i = 1; i < nrMatchers; i++) {
      sumScore += currentScores[i];
    }
    return getSimilarity().coord(nrMatchers, currentScores.length) * sumScore;
  }
}
