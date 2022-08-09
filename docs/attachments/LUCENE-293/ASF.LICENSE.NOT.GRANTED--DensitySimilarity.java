package org.apache.lucene.search;
/**
 * Copyright 2005 Apache Software Foundation
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

/** A <code>Similarity</code> that helps to compute densities
 * of query matches in indexed fields.
 */
public abstract class DensitySimilarity extends Similarity {

  /** Cache of decoded squared byte norms. */
  private static final float[] INVERSE_LENGTHS = new float[256];

  static {
    for (int i = 0; i < 256; i++) {
      float sqrtNorm = decodeNorm((byte)i);
      INVERSE_LENGTHS[i] = sqrtNorm * sqrtNorm;
    }
  }

  /** Computes the inverse field length from a stored normalization factor.
   * <br>The implementation uses the decoded stored norm {@link #decodeNorm(byte)}
   * and returns its square.
   * <br>It is assumed here that the decoded norm is an approximation of the inverse
   * square root of the index field length.
   * @param b The field norm as stored in the index, see {@link #encodeNorm(float)}.
   */
  public static float inverseFieldLength(byte b) {
    return INVERSE_LENGTHS[b & 0xFF];  // & 0xFF maps negative bytes to positive above 127
  }
}
