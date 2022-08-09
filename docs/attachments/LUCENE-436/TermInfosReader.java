package org.apache.lucene.index;

/**
 * Copyright 2004 The Apache Software Foundation
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

import org.apache.lucene.store.Directory;

/** This stores a monotonically increasing set of <Term, TermInfo> pairs in a
 * Directory.  Pairs are accessed either by Term or by ordinal position the
 * set.  */

final class TermInfosReader {
  private Directory directory;
  private String segment;
  private FieldInfos fieldInfos;

  private SegmentTermEnum origEnum;
  private long size;

  private Term[] indexTerms = null;
  private TermInfo[] indexInfos;
  private long[] indexPointers;
  
  private SegmentTermEnum indexEnum;
  
  private ThreadLocal enumerators = new ThreadLocal() {
      public Object initialValue() {
          return origEnum.clone();
      }
  };
  
  TermInfosReader(Directory dir, String seg, FieldInfos fis) throws IOException {
    directory = dir;
    segment = seg;
    fieldInfos = fis;

    origEnum = new SegmentTermEnum(directory.openInput(segment + ".tis"),
                                   fieldInfos, false);
    size = origEnum.size;

    indexEnum = new SegmentTermEnum(directory.openInput(segment + ".tii"),
			  fieldInfos, true);
  }

  public int getSkipInterval() {
     return origEnum.skipInterval;
  }

  final void close() throws IOException {
    if (origEnum != null)
      origEnum.close();
    if (indexEnum != null)
      indexEnum.close();
  }
  
  /** Returns the number of term/value pairs in the set. */
  final long size() {
    return size;
  }

  private synchronized void ensureIndexIsRead() throws IOException {
    if (indexTerms != null)                       // index already read
      return;                                     // do nothing
    try {
      int indexSize = (int)indexEnum.size;        // otherwise read index

      indexTerms = new Term[indexSize];
      indexInfos = new TermInfo[indexSize];
      indexPointers = new long[indexSize];
        
      for (int i = 0; indexEnum.next(); i++) {
        indexTerms[i] = indexEnum.term();
        indexInfos[i] = indexEnum.termInfo();
        indexPointers[i] = indexEnum.indexPointer;
      }
    } finally {
        indexEnum.close();
        indexEnum = null;
    }
  }

  /** Returns the offset of the greatest index entry which is less than or equal to term.*/
  private final int getIndexOffset(Term term) {
    int lo = 0;					  // binary search indexTerms[]
    int hi = indexTerms.length - 1;

    while (hi >= lo) {
      int mid = (lo + hi) >> 1;
      int delta = term.compareTo(indexTerms[mid]);
      if (delta < 0)
	hi = mid - 1;
      else if (delta > 0)
	lo = mid + 1;
      else
	return mid;
    }
    return hi;
  }
  
  TermInfo get(Term term) throws IOException {
      if (size == 0) return null;

      ensureIndexIsRead();

      // optimize sequential access: first try scanning cached enum w/o seeking
      SegmentTermEnum enumerator = getEnum();
      
      Term enumTerm = enumerator.term();
      
      if(term.equals(enumTerm)) {
          return enumerator.termInfo();
      }
      
      boolean needsSeek = true;
      if (enumTerm != null) {
          if(term.compareTo(enumTerm)>0) {
              // term is greater than current enumerator term
              int enumOffset = (int) (enumerator.position / enumerator.indexInterval) + 1;
              if (indexTerms.length == enumOffset // but before end of block
                      || term.compareTo(indexTerms[enumOffset]) < 0) {
                  needsSeek=false;
              }
          }
      }
      if(needsSeek) {
          int indexOffset = getIndexOffset(term);
          enumerator.seek(indexPointers[indexOffset],
                  (indexOffset * enumerator.indexInterval) - 1,
                  indexTerms[indexOffset], indexInfos[indexOffset]);
      }
      
      enumerator.scanTo(term);
      
      if(term.equals(enumerator.term()))
          return enumerator.termInfo();
      
      return null;
  }


  /** Returns an enumeration of all the Terms and TermInfos in the set. */
  public SegmentTermEnum terms() {
    return (SegmentTermEnum)origEnum.clone();
  }
  
  private SegmentTermEnum getEnum() {
      return (SegmentTermEnum) enumerators.get();
  }

  /** Returns an enumeration of terms starting at or after the named term. */
  public SegmentTermEnum terms(Term term) throws IOException {
      get(term);
      return (SegmentTermEnum) getEnum().clone();
  }
}
