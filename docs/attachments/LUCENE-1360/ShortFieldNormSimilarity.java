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

/** Expert: Default scoring implementation. */
public class ShortFieldNormSimilarity extends DefaultSimilarity {
  private static float ARR[] = { 0.0f, 1.5f, 1.25f, 1.0f, 0.875f, 0.75f, 0.625f, 0.5f, 0.4375f, 0.375f, 0.3125f};

  /** 
   * Implemented as a lookup for the first 10 counts, then
   * <code>1/sqrt(numTerms)</code>. This is to avoid term counts below
   * 11 from having the same lengthNorm after being stored encoded as
   * a single byte.
   */
  public float lengthNorm(String fieldName, int numTerms) {
    if( numTerms <= 10 ) {
      // this shouldn't be possible, but be safe.
      if( numTerms < 0 ) { numTerms = 0; }

      return ARR[numTerms];
    }
    //else
    return (float)(1.0 / Math.sqrt(numTerms));
  }
}
