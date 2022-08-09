/*
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

import java.text.BreakIterator;
import java.text.CharacterIterator;

/** Just produces one single fragment for the entire
 *  string.
 *
 *  NOTE: this is NOT general purpose; it's almost certainly
 *  buggy and only known to work with PostingsHighlighter!
 **/

class WholeBreakIterator extends BreakIterator {

  private int len;

  @Override
  public int current() {
    return 0;
  }

  @Override
  public int first() {
    return 0;
  }

  @Override
  public int following(int pos) {
    return DONE;
  }

  @Override
  public CharacterIterator getText() {
    return null;
  }

  @Override
  public int last() {
    return len;
  }

  @Override
  public int next() {
    return len;
  }

  @Override
  public int next(int n) {
    return len;
  }

  @Override
  public int preceding(int pos) {
    return 0;
  }

  @Override
  public int previous() {
    return 0;
  }

  @Override
  public void setText(CharacterIterator newText) {
    len = newText.getEndIndex();
  }
}
