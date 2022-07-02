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
package org.apache.lucene.util;

import java.util.Arrays;

public final class MSBRadixSorter {

  private static final int INSERTION_SORT_THRESHOLD = 50;
  private static final int HISTOGRAM_SIZE = 256;

  private static void buildHistogram(int[] array, int off, int len, int[] histogram, int shift) {
    for (int i = 0; i < len; ++i) {
      final int v = array[off + i];
      final int b = (v >>> shift) & 0xFF;
      histogram[b] += 1;
    }
  }

  private static void sumHistogram(int[] histogram) {
    int accum = 0;
    for (int i = 0; i < HISTOGRAM_SIZE; ++i) {
      final int count = histogram[i];
      histogram[i] = accum;
      accum += count;
    }
  }

  private static void reorder(int[] array, int off, int len, int[] histogram, int[] limits, int shift) {
    // reorder in place, like the dutch flag problem
    for (int i = 0; i < HISTOGRAM_SIZE; ++i) {
      final int limit = limits[i];
      for (int h = histogram[i]; h < limit; h = histogram[i]) {
        final int v = array[off + h];
        final int b = (v >>> shift) & 0xFF;
        // swap
        final int k = histogram[b]++;
        array[off + h] = array[off + k];
        array[off + k] = v;
      }
    }
  }

  private static void insertionSort(int[] array, int off, int len) {
    for (int i = off + 1, end = off + len; i < end; ++i) {
      for (int j = i; j > off; --j) {
        if (array[j - 1] > array[j]) {
          int tmp = array[j - 1];
          array[j - 1] = array[j];
          array[j] = tmp;
        } else {
          break;
        }
      }
    }
  }

  private final int[][] histograms;
  private final int[] limits;

  public MSBRadixSorter() {
    histograms = new int[4][];
    for (int i = 0; i < 4; ++i) {
      histograms[i] = new int[HISTOGRAM_SIZE];
    }
    limits = new int[HISTOGRAM_SIZE];
  }

  /** Sort {@code array[0:len]} in place.
   * @param numBits how many bits are required to store any of the values in
   *                {@code array[0:len]}. Pass {@code 32} if unknown. */
  public void sort(int numBits, final int[] array, int len) {
    sort(array, 0, len, Math.max(numBits - 8, 0), 0);
  }

  private void sort(int[] array, int off, int len, int shift, int level) {
    if (len < INSERTION_SORT_THRESHOLD) {
      insertionSort(array, off, len);
      return;
    }
    final int[] histogram = histograms[level];
    final int[] limits = this.limits;
    Arrays.fill(histogram, 0);
    buildHistogram(array, off, len, histogram, shift);
    sumHistogram(histogram);
    System.arraycopy(histogram, 1, limits, 0, HISTOGRAM_SIZE - 1);
    limits[HISTOGRAM_SIZE - 1] = len;
    reorder(array, off, len, histogram, limits, shift);
    if (shift > 0) {
      // recurse
      for (int prev = 0, i = 0; i < HISTOGRAM_SIZE; ++i) {
        int h = histogram[i];
        sort(array, off + prev, h - prev, Math.max(0, shift - 8), level + 1);
        prev = h;
      }
    }
  }

}
