package org.apache.lucene.search;
/**
 * Copyright 2004-2005 The Apache Software Foundation
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

/** Derived from TermScorer of August 2004. */

import java.io.IOException;

import org.apache.lucene.index.TermDocs;

/** Expert: A <code>Scorer</code> with density score for documents matching
 * a <code>Term</code>.
 */
public class DensityTermScorer extends TermScorer {
  /* needs non final TermScorer, with protected:
   * freqs, pointer, weightValue, norms, doc, docs, pointerMax, termDocs,
   * weight
   */

  /** Construct a <code>DensityTermScorer</code>.
   * @param weight     The weight of the <code>Term</code> in the query.
   * @param td         An iterator over the documents matching the <code>Term</code>.
   * @param similarity The </code>Similarity</code> implementation to be used.
   * @param norms      The field norms of the document fields for the <code>Term</code>.
   */
  DensityTermScorer(Weight weight,
                    TermDocs td,
                    Similarity similarity,
                    byte[] norms) {
    super(weight, td, similarity, norms);
  }
  
  /** Returns weighted density of the query in the current document.
   * Initially invalid, until {@link #next()} is called the first time.
   * @todo FIXME: add a cache for score factors similar to TermScorer.
   */
  public float score() {
    float res = freqs[pointer] * weightValue
                * DensitySimilarity.inverseFieldLength(norms[doc]);
    return res;
  }
  
  protected boolean score(HitCollector c, int end) throws IOException {
    /* Adapted from TermScorer: removed cache usage
     * and call score() for the actual score
     */
    while (doc < end) {                           // for docs in window
      c.collect(doc, score());                    // collect score

      /* original code from TermScorer: refactor into method advancePointer() ? */
      if (++pointer >= pointerMax) {
        pointerMax = termDocs.read(docs, freqs);  // refill buffers
        if (pointerMax != 0) {
          pointer = 0;
        } else {
          termDocs.close();                       // close stream
          doc = Integer.MAX_VALUE;                // set to sentinel value
          return false;
        }
      } 
      doc = docs[pointer];
    }
    return true;
  }

  /** Returns a string representation of this. */
  public String toString() { return "DensityTermScorer(" + weight + ")"; }
}
