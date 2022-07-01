package org.apache.lucene.search.spans;

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

import java.util.Arrays;
import java.util.Comparator;

import org.apache.lucene.index.IndexReader;

/** A Spans that is formed from the ordered subspans of a SpanNearQuery
 * where the subspans do not overlap and have a maximum slop between them.<br>
 * <p>
 * The formed spans only contains minimum slop matches.<br>
 * The formed spans may contain overlaps when the slop is at least 1.
 * For example, when querying using
 * <pre>t1 t2 t3</pre>
 * with slop at least 1, the fragment:
 * <pre>t1 t2 t1 t3 t2 t3</pre>
 * matches twice:
 * <pre>t1 t2 .. t3      </pre>
 * <pre>      t1 .. t2 t3</pre>
 */
class NearSpansOrdered implements Spans {
  private final int allowedSlop;
  private boolean firstTime = true;
  private boolean more = false;

  private final Spans[] subSpans;
  private boolean inSameDoc = false; // indicates all subSpans have same doc()

  private int matchDoc = -1;
  private int matchStart = -1;
  private int matchEnd = -1;

  private final Spans[] subSpansByDoc;
  private final Comparator spanDocComparator = new Comparator() {
    public int compare(Object o1, Object o2) {
      return ((Spans)o1).doc() - ((Spans)o2).doc();
    }
  };
  
  public NearSpansOrdered(SpanNearQuery spanNearQuery, IndexReader reader)
  throws IOException {
    if (spanNearQuery.getClauses().length < 2) {
      throw new IllegalArgumentException("Less than 2 clauses: "
                                         + spanNearQuery);
    }
    allowedSlop = spanNearQuery.getSlop();
    SpanQuery[] clauses = spanNearQuery.getClauses();
    subSpans = new Spans[clauses.length];
    subSpansByDoc = new Spans[clauses.length];
    for (int i = 0; i < clauses.length; i++) {
      subSpans[i] = clauses[i].getSpans(reader);
      subSpansByDoc[i] = subSpans[i]; // used in toSameDoc()
    }
  }

  /** Returns the document number of the current match.  Initially invalid. */
  public int doc() { return matchDoc; }

  /** Returns the start position of the current match.  Initially invalid. */
  public int start() { return matchStart; }

  /** Returns the end position of the current match.  Initially invalid. */
  public int end() { return matchEnd; }

  /** Move to the next match, returning true iff any such exists. */
  public boolean next() throws IOException {
    if (firstTime) {
      firstTime = false;
      for (int i = 0; i < subSpans.length; i++) {
        if (! subSpans[i].next()) {
          more = false;
          return false;
        }
      }
      more = true;
    }
    return advanceAfterOrdered();
  }

  /** Skips to the first match beyond the current, whose document number is
   * greater than or equal to <i>target</i>.
   */
  public boolean skipTo(int target) throws IOException {
    if (firstTime) {
      firstTime = false;
      for (int i = 0; i < subSpans.length; i++) {
        if (! subSpans[i].skipTo(target)) {
          more = false;
          return false;
        }
      }
      more = true;
    } else if (more && (subSpans[0].doc() < target)) {
      if (subSpans[0].skipTo(target)) {
        inSameDoc = false;
      } else {
        more = false;
        return false;
      }
    }
    return advanceAfterOrdered();
  }

  /** Advances the subSpans to just after an ordered match with small enough slop.
   * Returns true iff there is such a match.
   * The start and end of this match have the minimum possible slop.
   */
  private boolean advanceAfterOrdered() throws IOException {
    while (more) {
      if (! (inSameDoc || toSameDoc())) {
        more = false;
        return false;
      }
      inSameDoc = true;
      // Order the subSpans by making all later spans start
      // after the previous spans end.
      matchDoc = subSpans[0].doc();
      for (int i = 1; inSameDoc && (i < subSpans.length); i++) {
        while (inSameDoc && (subSpans[i-1].end() > subSpans[i].start())) {
          if (! subSpans[i].next()) {
            more = false;
            return false;
          } else if (matchDoc != subSpans[i].doc()) {
            inSameDoc = false;
          }
        }
      }
      if (inSameDoc) {
        // The subSpans are ordered in the same doc.
        // Compute the slop while making it as small as possible by advancing
        // all subSpans except the last one.
        Spans lastSpans = subSpans[subSpans.length - 1];
        matchStart = lastSpans.start();
        matchEnd = lastSpans.end();
        int matchSlop = 0;
        for (int i = subSpans.length - 2; i >= 0; i--) {
          Spans prevSpans = subSpans[i];
          int prevStart = prevSpans.start();
          int prevEnd = prevSpans.end();
          assert (prevEnd <= matchStart)
                : ("subSpans" + i + ": " + prevEnd + " after " + matchStart);
          while (true) {
            if (! prevSpans.next()) {
              more = false;
              break; // no return, last match still possible
            } else if (matchDoc != prevSpans.doc()) {
              inSameDoc = false;
              break;
            } else if (matchStart < prevSpans.end()) {
              break;
            } else { // prevSpans still before matchStart
              prevStart = prevSpans.start();
              prevEnd = prevSpans.end();
            }
          }
          matchSlop += matchStart - prevEnd;
          matchStart = prevStart;
        }
        if (matchSlop <= allowedSlop) {
          return true; // ordered match, small enough slop.
        }
      }
    }
    return false; // no more matches
  }

  private boolean toSameDoc() throws IOException {
    Arrays.sort(subSpansByDoc, spanDocComparator);
    int firstIndex = 0;
    int maxDoc = subSpansByDoc[subSpansByDoc.length - 1].doc();
    while (subSpansByDoc[firstIndex].doc() != maxDoc) {
      if (! subSpansByDoc[firstIndex].skipTo(maxDoc)) {
        return false;
      }
      maxDoc = subSpansByDoc[firstIndex].doc();
      if (++firstIndex == subSpansByDoc.length) {
        firstIndex = 0;
      }
    }
    return true;
  }

}

