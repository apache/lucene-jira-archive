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
 *  Store and iterate sorted integers in compressed form in RAM. <br>
 *  Code for compressing ascending integers borrowed from
 *  org.apache.lucene.store InputStream and OutputStream.
 */
 
import java.util.BitSet;

public class SortedVIntList {
  private int size;
  private byte[] bytes;
  private int lastBytePos;
  private final int MAX_BYTES_PER_INT = (31 / 7) + 1;


  /**
   *  Create a SortedVIntList from an array of integers.
   *
   * @param  sortedInts  An array of sorted non negative integers.
   * @param  inputSize   The number of integers to be used from the array.
   */
  public SortedVIntList(int[] sortedInts, int inputSize) {
    initBytes();

    int lastInt = 0;
    for (int i = 0; i < inputSize; i++) {
      add(sortedInts[i], lastInt);
      lastInt = sortedInts[i];
    }

    resizeBytes(lastBytePos);
  }


  /**
   *  Create a SortedVIntList from a BitSet.
   *
   * @param  bits  A bit set representing a set of integers.
   */
  public SortedVIntList(BitSet bits) {
    initBytes();

    int lastInt = 0;
    int nextInt = bits.nextSetBit(lastInt);
    while (nextInt != -1) {
      add(nextInt, lastInt);
      lastInt = nextInt;
      nextInt = bits.nextSetBit(lastInt + 1);
    }

    resizeBytes(lastBytePos);
  }

  private void initBytes() {
    size = 0;
    bytes = new byte[128]; // initial byte size
    lastBytePos = 0;
  }

  private void resizeBytes(int newSize) {
    if (newSize != bytes.length) {
      byte[] newBytes = new byte[newSize];
      System.arraycopy(bytes, 0, newBytes, 0, lastBytePos);
      bytes = newBytes;
    }
  }

  private void add(int e, int lastInt) {
    if ((lastBytePos + MAX_BYTES_PER_INT) > bytes.length) {
      // biggest possible int does not fit
      resizeBytes(lastBytePos * 2 + MAX_BYTES_PER_INT);
    }

    int diff = e - lastInt;
    if (diff < 0) {
      throw new IllegalArgumentException(
          "Input not sorted or first element negative.");
    }
    lastInt = e;

    while ((diff & ~0x7F) != 0) {
      // See org.apache.lucene.store.OutputStream.writeVInt()
      bytes[lastBytePos++] = (byte) ((diff & 0x7f) | ~0x7F);
      diff >>>= 7;
    }
    bytes[lastBytePos++] = (byte) diff;
    size++;
  }


  /**
   * @return    The total number of sorted integers.
   */
  public int size() {
    return size;
  }


  /**
   * @return    The size of the byte array storing the compressed sorted
   *      integers.
   */
  public int getByteSize() {
    return bytes.length;
  }


  /**
   * @return    A DocNrSkipper over the sorted integers.
   */
  public DocNrSkipper getDocNrSkipper() {
    return new DocNrSkipper() {
      private int bytePos = 0;
      private int lastInt = 0;

      public int nextDocNr(int docNr) {
        while (bytePos < lastBytePos) {
          // See org.apache.lucene.store.InputStream.readVInt()
          byte b = bytes[bytePos++];
          lastInt += b & 0x7F;
          for (int shift = 7; (b & ~0x7F) != 0; shift += 7) {
            b = bytes[bytePos++];
            lastInt += (b & 0x7F) << shift;
          }
          if (lastInt >= docNr) {
            return lastInt;
          }
        }
        return -1;
      }
    };
  }
}

