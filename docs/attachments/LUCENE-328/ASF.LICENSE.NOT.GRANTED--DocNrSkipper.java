package org.apache.lucene.util;
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

/**
 *  Iterate over document numbers, possibly skipping some.
 *  <p>Inspired by java.lang.BitSet.nextSetBit().
 */
public interface DocNrSkipper {
  /**
   * @param docNr A document number that is bigger than the last returned one,
   *              or zero at first call.
   * @return The smallest document number in the iteration that is
   * bigger than or equal to given document number. <br>
   * When there is no more document number in the iteration -1 is returned.
   */
  int nextDocNr(int docNr);
}

