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

import java.io.IOException;

import org.apache.lucene.util.DocNrSkipper;
import org.apache.lucene.index.IndexReader;

/**
 *  A query filter in the form of a document number skipper.
 */
public interface SkipFilter {
  /**
   * @param  reader  A reader on the index.
   * @return         An DocNrSkipper over document numbers that pass the filter.
   */
  public DocNrSkipper getDocNrSkipper(IndexReader reader) throws IOException;
}

