package org.apache.lucene.util;

/**
 * Copyright 2006 The Apache Software Foundation
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
import java.util.Vector;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Directory;

/** N-way external merge sort.
 * 
 * @author Marvin Humphrey
 */

public class SortExternal { 
  // a guess at how much RAM overhead a byte[] requires
  static final int PER_ITEM_OVERHEAD = 12;

  // item cache, both incoming and outgoing
  private byte[][] cache = new byte[0][]; 
  private int cacheBytes = 0;  // bytes of RAM occupied by cache 
  private int cacheElems = 0;  // number of byte[] elements in cache
  private int cachePos   = 0;  // index of current element in cache

  // scratch space for use by merge sort
  private byte[][] scratch = new byte[0][];

  private int memThreshold;    // bytes of RAM allowed for cache
  private int runCacheLimit;   // bytes of RAM allowed each run cache

  private Vector runs = new Vector();

  private IndexInput input;
  private IndexOutput output;
  private Directory directory;
  private String segment;

  private boolean fetchEnabled = false;

  /** Construct an external sorter.
   *
   * @param dir the index directory
   * @param seg the name of the segment
   * @param memThresh flush the cache when it occupies this many bytes
   */ 
  public SortExternal(Directory dir, String seg, int memThresh) 
                      throws Exception {
    growCache(1000);
    memThreshold = memThresh;
    runCacheLimit = memThresh / 2;
    segment = seg;
    directory = dir;
    output = directory.createOutput(segment + ".srt");
  }

  /** Construct an external sorter with a 16 MB cache.
   *
   * @param dir the index directory
   * @param seg the name of the segment
   */ 
  public SortExternal(Directory dir, String seg) throws Exception {
    this(dir, seg, (int)Math.pow(2, 24));
  }

  /* Expand the cache capacity, if necessary.
   */
  private void growCache(int capacity) {
    if (cache.length >= capacity)
      return;

    byte[][] newCache;
    if (scratch != null && scratch.length >= capacity) {
      newCache = scratch;
      scratch = null;
    }
    else {
      newCache = new byte[capacity][];
    }
    for (int i = 0; i < cacheElems; i++) {
      newCache[i] = cache[i];
    }
    cache = newCache;
  }

  /* Remove all items from the cache and reset counter and pointer variables.
   */
  private void clearCache() {
    for (int i = 0; i < cacheElems; i++)
      cache[i] = null;
    cacheBytes = 0;
    cacheElems = 0;
    cachePos   = 0;
  }
  
  /* Expand the scratch space, if necessary.
   */
  private void growScratch(int capacity) {
    if (scratch == null || scratch.length < capacity)
      scratch = new byte[capacity][];
  }

  /* Close all streams and delete the temporary file.
   */
  public void close() throws IOException {
    if (input != null)
      input.close();
    if (output != null) 
      output.close();
    if (directory.fileExists(segment + ".srt"))
      directory.deleteFile(segment + ".srt");
  }

  /** Prepare to begin fetching sorted items.
   */
  public void sortAll() throws IOException {
    if (runs.size() == 0) // sort in-memory if we've never flushed
      sortCache();
    else 
      sortRun();

    // done adding elements, so close file and reopen as an instream
    output.close();
    input = directory.openInput(segment + ".srt");

    fetchEnabled = true;
  }

  /** Add an item to the sortpool.
   *
   * @param item a byte[] array.
   */
  public void feed(byte[] item) throws IOException {
    // add room for more cache elements if needed
    if (cacheElems == cache.length) {
      // add 100, plus a fraction of the current capacity
      int newCapacity = cache.length + 100 + (cache.length / 8);
      growCache(newCapacity);
    }

    // add a copy of the input item
    int len = item.length;
    byte[] copy = new byte[len];
    for (int i = 0; i < len; i++)
      copy[i] = item[i];
    cache[cacheElems++] = copy;

    // track memory occupied by cache
    cacheBytes += len;
    cacheBytes += PER_ITEM_OVERHEAD;

    // flush the cache whenever it gets too big
    if (cacheBytes >= memThreshold)
      sortRun();
  }

  /** Retrieve an item from the sortpool.  
   * (sortAll() must be called before fetch() is called.)
   *
   * @param item a byte[] array.
   */
  public byte[] fetch() throws Exception {
    if (fetchEnabled == false)
      throw new Exception("SortExternal not in fetching mode");
    if (cachePos >= cacheElems)
      refillCache();
    if (cacheElems > 0)
      return cache[cachePos++];
    else 
      return null;
  }

  /* Sort and flush cache contents to disk.  Track the sorted run by creating
   * a SortExternalRun object to represent it.
   */
  private void sortRun() throws IOException {
    // bail if there's nothing in the cache
    if (cacheBytes == 0)
      return;

    // mark start of run
    long runStart = output.getFilePointer();
    
    // write sorted items to file
    sortCache();
    for (int i = 0; i < cacheElems; i++) {
      byte[] item = cache[i];
      output.writeVInt(item.length);
      output.writeBytes(item, item.length);
    }

    clearCache();

    // mark end of run and build a new SortExRun object
    long runEnd = output.getFilePointer();
    SortExternalRun run = new SortExternalRun(runStart, runEnd);
    runs.add(run);

    // recalculate the size allowed for each run's cache
    runCacheLimit = Math.max(65536, ((memThreshold / 2) / runs.size()));
  }


  /* Refill the main cache, drawing from the caches of all SortExternalRun
   * objects.
   */
  private void refillCache() throws IOException {
    // free all the existing items, as they've been fetched by now
    clearCache();

    // make sure all runs have at least one item in their cache
    for (int i = 0; i < runs.size(); i++) {
      SortExternalRun run = (SortExternalRun)runs.get(i);
      if (!run.refillRun(input, runCacheLimit))
        runs.remove(i); // discard empty runs
    }

    if (runs.isEmpty())
      return;

    // transfer as many items as possible into the main cache
    byte[] endpost = findEndpost();
    int total = 0;
    for (int i = 0; i < runs.size(); i++) {
      SortExternalRun run = (SortExternalRun)runs.get(i);
      total += run.defineSlice(endpost);
    }
    growCache(total);
    growScratch(total);
    mergeRuns(total);
}

  /* Merge all "in-range" items from their SortExternalRun objects into the
   * main cache.
   */
  private void mergeRuns(int elemsPending) throws RuntimeException {
    if (cacheElems > 0)
      throw new RuntimeException("mergeRuns() called when cache not empty");

    int numRuns = runs.size();
    int numSlices = 0;
    int sliceStart = 0;
    int[] sliceStarts = new int[numRuns];
    int[] sliceSizes = new int[numRuns];

    // copy items into main cache (TODO: eliminate this step);
    for (int i = 0, j = 0; i < runs.size(); i++) {
      SortExternalRun run = (SortExternalRun) runs.get(i);
      // skip runs that don't have any elements in range
      if (run.sliceSize == 0)
        continue;

      // remember where each slice begins and how many elems it has
      sliceStarts[j] = sliceStart;
      sliceSizes[j] = run.sliceSize;

      byte[][] runCache = run.cache;
      int limit = run.cachePos + run.sliceSize;
      for (int k = run.cachePos; k < limit; k++) {
        cache[sliceStart++] = runCache[k];
      }
      // now that we've transfered some cache elems, move the run's pointer
      run.cachePos = limit;
        
      numSlices = ++j;
    }

    // exploit previous sorting, rather than sort cache naively
    while (numSlices > 1) {
      // leave the first slice intact if the number of slices is odd
      int i = 0, j = 0;
      while (i < numSlices) {
        if (numSlices - i >= 2) {
          // merge two slices
          int combinedSize = sliceSizes[i] + sliceSizes[i+1];
          merge(cache, sliceStarts[i],   sliceSizes[i],
                cache, sliceStarts[i+1], sliceSizes[i+1]);
          sliceSizes[j] = combinedSize;
          sliceStarts[j] = sliceStarts[i];
          for (int x = 0, y = sliceStarts[j]; x < combinedSize; x++, y++) { 
            cache[y] = scratch[x];
          } 
          i += 2;
          j += 1;
        }
        else if (numSlices - i >= 1) {
          // move single slice pointer
          sliceSizes[j] = sliceSizes[i];
          sliceStarts[j] = sliceStarts[i];
          i++;
          j++;
        }
      }
      numSlices = j;
    }
    cacheElems = elemsPending;
  }

  /* Poll all the runs to find the item which is highest in sort order, but
   * which we can guarantee is lower in sort order than any item which has yet
   * to enter a run cache. 
   */
  byte[] findEndpost() throws RuntimeException {
    byte[] candidate, endpost = null;
    for (int i = 0; i < runs.size(); i++) {
      // get a run and verify no errors
      SortExternalRun run = (SortExternalRun)runs.get(i);
      if (run.cachePos == run.cacheElems || run.cacheElems < 1)
        throw new RuntimeException("findEndpost encountered an empty run cache");

      // get the last item in this run's cache
      candidate = run.cache[ (run.cacheElems - 1) ];

      // if it's the first run, the item is automatically the new endpost
      if (i == 0) {
        endpost = candidate;
        continue;
      }
      // if it's less than the current endpost, it's the new endpost
      else if (compareBytes(candidate, endpost) < 0) {
        endpost = candidate;
      }
    }

    return endpost;
  }

  /* Sort any items currently in the cache.
   */
  private void sortCache() {
    if (cacheElems == 0)
      return;
    growScratch(cacheElems);
    mSort(0, cacheElems - 1);
  }

  /* Standard mergesorting function.
   */
  private void mSort (int left, int right) {
    if (right > left) {
      int mid = ( (right+left)/2 ) + 1;
      mSort(left, mid - 1);
      mSort(mid, right);
      merge(cache, left, (mid - left), cache, mid, (right - mid + 1));
      int i = 0;
      while (left <= right) {
        cache[left++] = scratch[i++];
      }
    }
  }

  /* Standard mergesort merge function, with a twist: this variant is capable
   * of merging two discontiguous source arrays.  Copying elements from the
   * scratch to the destination is left for the caller.
   */ 
  private void merge(byte[][] leftBuf,  int left,  int leftSize,
                     byte[][] rightBuf, int right, int rightSize) { 
    int leftLimit  = left  + leftSize;
    int rightLimit = right + rightSize;
    int n = 0;

    while (left < leftLimit && right < rightLimit) {
      if (compareBytes(leftBuf[left], rightBuf[right]) < 1)
        scratch[n++] = leftBuf[left++];
      else 
        scratch[n++] = rightBuf[right++];
    }
    while (left < leftLimit) {
      scratch[n++] = leftBuf[left++];
    }
    while (right < rightLimit) {
      scratch[n++] = rightBuf[right++];
    }
  }

  /* Compare two byte arrays as though the bytes were unsigned (like the C
   * function memcmp).
   */
  private static final int compareBytes(byte[] bytes1, byte[] bytes2) {
    int end = Math.min(bytes1.length, bytes2.length);
    for (int k = 0; k < end; k++) {
      int b1 = (bytes1[k] & 0xFF);
      int b2 = (bytes2[k] & 0xFF);
      if (b1 != b2)
        return b1 - b2;
    }
    return bytes1.length - bytes2.length;
  }

  private class SortExternalRun {
    private long start;
    private long filePos;
    private long end;
  
    private byte[][] cache = new byte[0][];
    private int cacheElems = 0;      
    private int cachePos   = 0;     
    private int sliceSize;      // number of elements currently "in-range"
  
    SortExternalRun(long runStart, long runEnd) {
      growCache(100);
      start   = runStart;
      filePos = runStart;
      end     = runEnd;
    }
  
    private void growCache(int capacity) {
      if (cache.length >= capacity)
        return;
  
      byte[][] newCache = new byte[capacity][];
      for (int i = 0; i < cacheElems; i++) {
        newCache[i] = cache[i];
      }
      cache = newCache;
    }
  
    private void clearCache() {
      for (int i = 0; i < cacheElems; i++)
        cache[i] = null;
      cacheElems = 0;
      cachePos   = 0;
    }
  
    /* Recover sorted items from disk, up to the allowable memory limit. 
     */
    private boolean refillRun(IndexInput input, int runCacheLimit) 
                              throws IOException {
      // bail unless we actually need to refill
      if (cacheElems > cachePos)
        return true;
  
      clearCache();
  
      input.seek(filePos);
  
      int numRecovered = 0;  // number of items recovered from disk
      int runCacheBytes = 0; // bytes of RAM occupied by recovered items
      int itemLen;           // length of a recovered item in bytes
  
      while (true) {
        // bail if we've read everything in this run
        if (input.getFilePointer() >= end) {
          // make sure we haven't read too much
          if (input.getFilePointer() > end) {
            long pos = input.getFilePointer();
            throw new IOException("Read past end of run: " + pos + " " + end);
          }
          break;
        }
  
        // bail if we've hit the ceiling for this run's cache
        if (runCacheBytes > runCacheLimit)
          break;
  
        // retrieve an item 
        int len = input.readVInt();
        byte[] item = new byte[len];
        input.readBytes(item, 0, len);
  
        // add the item to the run's cache
        if (cacheElems == cache.length) {
          int newCapacity = cache.length + 100 + (cache.length / 8);
          growCache(newCapacity);
        }
        cache[cacheElems++] = item;
  
        // track how much we've read so far
        runCacheBytes += len + PER_ITEM_OVERHEAD;
      }

      // remember file position for next refill
      filePos = input.getFilePointer();

      // if there aren't any elems after trying to refill, this run is done
      if (cacheElems > 0)
        return true;
      else
        return false;
    }
  
    /* Record the number of items in the run's cache which are lexically
     * less than or equal to the endpost.
     */
    int defineSlice(byte[] endpost) {
      // operate on a slice of the cache
      int lo = cachePos - 1;
      int hi = cacheElems;
  
      // binary search
      while (hi - lo > 1) {
        int mid = (lo + hi) / 2;
        if (compareBytes(cache[mid], endpost) > 0) 
          hi = mid;
        else
          lo = mid;
      }
  
      sliceSize = lo == -1 
        ? 0 
        : (lo - cachePos) + 1;
  
      return sliceSize;
    }
  }

}

