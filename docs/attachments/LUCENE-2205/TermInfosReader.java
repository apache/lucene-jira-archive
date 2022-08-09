package org.apache.lucene.index;

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

import java.io.IOException;

import org.apache.lucene.store.Directory;
import org.apache.lucene.util.cache.Cache;
import org.apache.lucene.util.cache.SimpleLRUCache;
import org.apache.lucene.util.CloseableThreadLocal;

/** This stores a monotonically increasing set of <Term, TermInfo> pairs in a
 * Directory.  Pairs are accessed either by Term or by ordinal position the
 * set.  */

final class TermInfosReader {
  private final Directory directory;
  private final String segment;
  private final FieldInfos fieldInfos;

  private final CloseableThreadLocal<ThreadResources> threadResources = new CloseableThreadLocal<ThreadResources>();
  private final SegmentTermEnum origEnum;
  private final long size;

  private TermInfosReaderIndex index;
  private int indexLength;
  
  private final int totalIndexInterval;

  private final static int DEFAULT_CACHE_SIZE = 1024;
  
  /**
   * Per-thread resources managed by ThreadLocal
   */
  private static final class ThreadResources {
    SegmentTermEnum termEnum;
    
    // Used for caching the least recently looked-up Terms
    Cache<Term,TermInfo> termInfoCache;
  }
  
  TermInfosReader(Directory dir, String seg, FieldInfos fis, int readBufferSize, int indexDivisor)
       throws CorruptIndexException, IOException {
    boolean success = false;

    if (indexDivisor < 1 && indexDivisor != -1) {
      throw new IllegalArgumentException("indexDivisor must be -1 (don't load terms index) or greater than 0: got " + indexDivisor);
    }

    try {
      directory = dir;
      segment = seg;
      fieldInfos = fis;

      origEnum = new SegmentTermEnum(directory.openInput(segment + "." + IndexFileNames.TERMS_EXTENSION,
          readBufferSize), fieldInfos, false);
      size = origEnum.size;


      if (indexDivisor != -1) {
        // Load terms index
        totalIndexInterval = origEnum.indexInterval * indexDivisor;
        final SegmentTermEnum indexEnum = new SegmentTermEnum(directory.openInput(segment + "." + IndexFileNames.TERMS_INDEX_EXTENSION,
                                                                                  readBufferSize), fieldInfos, true);

        try {
        	String str = System.getProperty(getClass().getName(), "small");
            if (str.equals("default")) {
          	  index = new TermInfosReaderIndexDefault();
            } else if (str.equals("small")) {
          	  index = new TermInfosReaderIndexSmall();
            } else {
          	  throw new IllegalArgumentException("[" + str +
          	  		"] invalid, only \"default\" or \"small\" valid");
            }
            index.build(indexEnum, indexDivisor, (int) dir.fileLength(segment + "." + IndexFileNames.TERMS_INDEX_EXTENSION));
            indexLength = index.length();
        } finally {
          indexEnum.close();
        }
      } else {
        // Do not load terms index:
        totalIndexInterval = -1;
        index = null;
      }
      success = true;
    } finally {
      // With lock-less commits, it's entirely possible (and
      // fine) to hit a FileNotFound exception above. In
      // this case, we want to explicitly close any subset
      // of things that were opened so that we don't have to
      // wait for a GC to do so.
      if (!success) {
        close();
      }
    }
  }

  public int getSkipInterval() {
    return origEnum.skipInterval;
  }
  
  public int getMaxSkipLevels() {
    return origEnum.maxSkipLevels;
  }

  final void close() throws IOException {
    if (origEnum != null)
      origEnum.close();
    threadResources.close();
  }

  /** Returns the number of term/value pairs in the set. */
  final long size() {
    return size;
  }

  private ThreadResources getThreadResources() {
    ThreadResources resources = threadResources.get();
    if (resources == null) {
      resources = new ThreadResources();
      resources.termEnum = terms();
      // Cache does not have to be thread-safe, it is only used by one thread at the same time
      resources.termInfoCache = new SimpleLRUCache<Term,TermInfo>(DEFAULT_CACHE_SIZE);
      threadResources.set(resources);
    }
    return resources;
  }


  /** Returns the offset of the greatest index entry which is less than or equal to term.*/
  private final int getIndexOffset(Term term) {
    return index.getIndexOffset(term);
  }

  private final void seekEnum(SegmentTermEnum enumerator, int indexOffset) throws IOException {
    index.seekEnum(enumerator, indexOffset, totalIndexInterval);
  }

  /** Returns the TermInfo for a Term in the set, or null. */
  TermInfo get(Term term) throws IOException {
    return get(term, true);
  }
  
  /** Returns the TermInfo for a Term in the set, or null. */
  private TermInfo get(Term term, boolean useCache) throws IOException {
    if (size == 0) return null;

    ensureIndexIsRead();

    TermInfo ti;
    ThreadResources resources = getThreadResources();
    Cache<Term,TermInfo> cache = null;
    
    if (useCache) {
      cache = resources.termInfoCache;
      // check the cache first if the term was recently looked up
      ti = cache.get(term);
      if (ti != null) {
        return ti;
      }
    }
    
    // optimize sequential access: first try scanning cached enum w/o seeking
    SegmentTermEnum enumerator = resources.termEnum;
    if (enumerator.term() != null                 // term is at or past current
	&& ((enumerator.prev() != null && term.compareTo(enumerator.prev())> 0)
	    || term.compareTo(enumerator.term()) >= 0)) {
      int enumOffset = (int)(enumerator.position/totalIndexInterval)+1;
      if (indexLength == enumOffset	  // but before end of block
    		  || index.compareTo(term,enumOffset) < 0) {
       // no need to seek

        int numScans = enumerator.scanTo(term);
        if (enumerator.term() != null && term.compareTo(enumerator.term()) == 0) {
          ti = enumerator.termInfo();
          if (cache != null && numScans > 1) {
            // we only  want to put this TermInfo into the cache if
            // scanEnum skipped more than one dictionary entry.
            // This prevents RangeQueries or WildcardQueries to 
            // wipe out the cache when they iterate over a large numbers
            // of terms in order
            cache.put(term, ti);
          }
        } else {
          ti = null;
        }

        return ti;
      }  
    }

    // random-access: must seek
    seekEnum(enumerator, getIndexOffset(term));
    enumerator.scanTo(term);
    if (enumerator.term() != null && term.compareTo(enumerator.term()) == 0) {
      ti = enumerator.termInfo();
      if (cache != null) {
        cache.put(term, ti);
      }
    } else {
      ti = null;
    }
    return ti;
  }

  /** Returns the nth term in the set. */
  final Term get(int position) throws IOException {
    if (size == 0) return null;

    SegmentTermEnum enumerator = getThreadResources().termEnum;
    if (enumerator.term() != null &&
        position >= enumerator.position &&
	position < (enumerator.position + totalIndexInterval))
      return scanEnum(enumerator, position);      // can avoid seek

    seekEnum(enumerator, position/totalIndexInterval); // must seek
    return scanEnum(enumerator, position);
  }

  private final Term scanEnum(SegmentTermEnum enumerator, int position) throws IOException {
    while(enumerator.position < position)
      if (!enumerator.next())
	return null;

    return enumerator.term();
  }

  private void ensureIndexIsRead() {
    if (index == null) {
      throw new IllegalStateException("terms index was not loaded when this reader was created");
    }
  }

  /** Returns the position of a Term in the set or -1. */
  final long getPosition(Term term) throws IOException {
    if (size == 0) return -1;

    ensureIndexIsRead();
    int indexOffset = getIndexOffset(term);
    
    SegmentTermEnum enumerator = getThreadResources().termEnum;
    seekEnum(enumerator, indexOffset);

    while(term.compareTo(enumerator.term()) > 0 && enumerator.next()) {}

    if (term.compareTo(enumerator.term()) == 0)
      return enumerator.position;
    else
      return -1;
  }

  /** Returns an enumeration of all the Terms and TermInfos in the set. */
  public SegmentTermEnum terms() {
    return (SegmentTermEnum)origEnum.clone();
  }

  /** Returns an enumeration of terms starting at or after the named term. */
  public SegmentTermEnum terms(Term term) throws IOException {
    // don't use the cache in this call because we want to reposition the
    // enumeration
    get(term, false);
    return (SegmentTermEnum)getThreadResources().termEnum.clone();
  }
}
