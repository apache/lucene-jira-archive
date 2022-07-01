package org.apache.lucene.util.packed;

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

import org.apache.lucene.store.DataInput;
import org.apache.lucene.util.RamUsageEstimator;

import java.io.IOException;
import java.util.Arrays;

/**
 * Space optimized random access capable array of values with a fixed number of
 * bits/value. Values are packed contiguously.
 * </p><p>
 * The implementation strives to perform af fast as possible under the
 * constraint of contiguous bits, by avoiding expensive operations. This comes
 * at the cost of code clarity.
 * </p><p>
 * Technical details: This implementation is a refinement of a non-branching
 * version. The non-branching get and set methods meant that 2 or 4 atomics in
 * the underlying array were always accessed, even for the cases where only
 * 1 or 2 were needed. Even with caching, this had a detrimental effect on
 * performance.
 * Related to this issue, the old implementation used lookup tables for shifts
 * and masks, which also proved to be a bit slower than calculating the shifts
 * and masks on the fly.
 * See https://issues.apache.org/jira/browse/LUCENE-4062 for details.
 *
 */
abstract class Packed64Strategy extends PackedInts.MutableImpl {
  static final int BLOCK_SIZE = 64; // 32 = int, 64 = long
  static final int BLOCK_BITS = 6; // The #bits representing BLOCK_SIZE
  static final int MOD_MASK = BLOCK_SIZE - 1; // x % BLOCK_SIZE

  /**
   * Values are stored contiguously in the blocks array.
   */
  protected final long[] blocks;
  /**
   * A right-aligned mask of width BitsPerValue used by {@link #get(int)}.
   */
  protected final long maskRight;
  /**
   * Optimization: Saves one lookup in {@link #get(int)}.
   */
  protected final int bpvMinusBlockSize;


  /**
   * Creates an array with content retrieved from the given DataInput.
   * @param in       a DataInput, positioned at the start of Packed64-content.
   * @param valueCount  the number of elements.
   * @param bitsPerValue the number of bits available for any given value.
   * @throws java.io.IOException if the values for the backing array could not
   *                             be retrieved.
   */
  public static Packed64Strategy create(
      DataInput in, int valueCount, int bitsPerValue) throws IOException {
    Packed64Strategy reader = create(valueCount, bitsPerValue);
    for (int i = 0; i < reader.blocks.length; ++i) {
      reader.blocks[i] = in.readLong();
    }
    return reader;
  }

  /**
   * Creates an array backed by the given blocks.
   * </p><p>
   * Note: The blocks are used directly, so changes to the given block will
   * affect the Packed64-structure.
   * @param blocks   used as the internal backing array. Not that the last
   *                 element cannot be addressed directly.
   * @param valueCount the number of values.
   * @param bitsPerValue the number of bits available for any given value.
   */
  public static Packed64Strategy create(
      long[] blocks, int valueCount, int bitsPerValue) {
    switch (bitsPerValue) {
      case 1: return new Packed64_1(blocks, valueCount);
      case 2: return new Packed64_2(blocks, valueCount);
      case 4: return new Packed64_4(blocks, valueCount);
      case 8: return new Packed64_8(blocks, valueCount);
      case 16: return new Packed64_16(blocks, valueCount);
      case 32: return new Packed64_32(blocks, valueCount);
      case 64: return new Packed64_64(blocks, valueCount);
      default: return new Packed64All(blocks, valueCount, bitsPerValue);
    }
  }

  /**
   * Creates an array with the internal structures adjusted for the given
   * limits and initialized to 0.
   * @param valueCount   the number of elements.
   * @param bitsPerValue the number of bits available for any given value.
   */
  private Packed64Strategy(int valueCount, int bitsPerValue) {
    // NOTE: block-size was previously calculated as
    // valueCount * bitsPerValue / BLOCK_SIZE + 1
    // due to memory layout requirements dictated by non-branching code
    this(new long[(int)((long)size(valueCount, bitsPerValue))],
            valueCount, bitsPerValue);
  }

  /**
   * Creates an array backed by the given blocks.
   * </p><p>
   * Note: The blocks are used directly, so changes to the given block will
   * affect the Packed64-structure.
   * @param blocks   used as the internal backing array. Not that the last
   *                 element cannot be addressed directly.
   * @param valueCount the number of values.
   * @param bitsPerValue the number of bits available for any given value.
   */
  private Packed64Strategy(long[] blocks, int valueCount, int bitsPerValue) {
    super(valueCount, bitsPerValue);
    this.blocks = blocks;
    maskRight = ~0L << (BLOCK_SIZE-bitsPerValue) >>> (BLOCK_SIZE-bitsPerValue);
    bpvMinusBlockSize = bitsPerValue - BLOCK_SIZE;
  }

  private static int size(int valueCount, int bitsPerValue) {
    final long totBitCount = (long) valueCount * bitsPerValue;
    return (int)(totBitCount/64 + ((totBitCount % 64 == 0 ) ? 0:1));
  }

  @Override
  public String toString() {
    return "Packed64(bitsPerValue=" + bitsPerValue + ", size="
            + size() + ", elements.length=" + blocks.length + ")";
  }

  @Override
  public long ramBytesUsed() {
    return RamUsageEstimator.sizeOf(blocks);
  }

  @Override
  public void fill(int fromIndex, int toIndex, long val) {
    assert PackedInts.bitsRequired(val) <= getBitsPerValue();
    assert fromIndex <= toIndex;

    // minimum number of values that use an exact number of full blocks
    final int nAlignedValues = 64 / pgcd(64, bitsPerValue);
    final int span = toIndex - fromIndex;
    if (span <= 3 * nAlignedValues) {
      // there needs be at least 2 * nAlignedValues aligned values for the
      // block approach to be worth trying
      super.fill(fromIndex, toIndex, val);
      return;
    }

    // fill the first values naively until the next block start
    final int fromIndexModNAlignedValues = fromIndex % nAlignedValues;
    if (fromIndexModNAlignedValues != 0) {
      for (int i = fromIndexModNAlignedValues; i < nAlignedValues; ++i) {
        set(fromIndex++, val);
      }
    }
    assert fromIndex % nAlignedValues == 0;

    // compute the long[] blocks for nAlignedValues consecutive values and
    // use them to set as many values as possible without applying any mask
    // or shift
    final int nAlignedBlocks = (nAlignedValues * bitsPerValue) >> 6;
    final long[] nAlignedValuesBlocks;
    {
      Packed64Strategy values = 
          Packed64Strategy.create(nAlignedValues, bitsPerValue);
      for (int i = 0; i < nAlignedValues; ++i) {
        values.set(i, val);
      }
      nAlignedValuesBlocks = values.blocks;
      assert nAlignedBlocks <= nAlignedValuesBlocks.length;
    }
    final int startBlock = (int) (((long) fromIndex * bitsPerValue) >>> 6);
    final int endBlock = (int) (((long) toIndex * bitsPerValue) >>> 6);
    for (int  block = startBlock; block < endBlock; ++block) {
      final long blockValue = nAlignedValuesBlocks[block % nAlignedBlocks];
      blocks[block] = blockValue;
    }

    // fill the gap
    for (int i = (int) (((long) endBlock << 6) / bitsPerValue); i < toIndex; ++i) {
      set(i, val);
    }
  }
  private static int pgcd(int a, int b) {
    if (a < b) {
      return pgcd(b, a);
    } else if (b == 0) {
      return a;
    } else {
      return pgcd(b, a % b);
    }
  }

  @Override
  public void clear() {
    Arrays.fill(blocks, 0L);
  }

  /* Subclasses for the different bitsPerValue below */
  public static Packed64Strategy create(int valueCount, int bitsPerValue) {
    switch (bitsPerValue) {
      case 1: return new Packed64_1(valueCount);
      case 2: return new Packed64_2(valueCount);
      case 4: return new Packed64_4(valueCount);
      case 8: return new Packed64_8(valueCount);
      case 16: return new Packed64_16(valueCount);
      case 32: return new Packed64_32(valueCount);
      case 64: return new Packed64_64(valueCount);
      default: return new Packed64All(valueCount, bitsPerValue);
    }
  }

  static class Packed64_1 extends Packed64Strategy {
    Packed64_1(int valueCount) {
      super(valueCount, 1);
    }
    Packed64_1(long[] blocks, int valueCount) {
      super(blocks, valueCount, 1);
    }
    @Override
    public final long get(int index) {
      return (blocks[index >>> 6] >>> (index & 63)) & 1L;
    }
    @Override
    public final void set(int index, long value) {
      final int pos = index >>> 6;
      final int shift = index & 63;
      blocks[pos] = blocks[pos] & ~(1L << shift) | value << shift;
    }
  }
  static class Packed64_2 extends Packed64Strategy {
    Packed64_2(int valueCount) {
      super(valueCount, 2);
    }
    Packed64_2(long[] blocks, int valueCount) {
      super(blocks, valueCount, 2);
    }
    @Override
    public final long get(int index) {
      return (blocks[index >>> 5] >>> ((index & 31) << 1)) & 3L;
    }
    @Override
    public final void set(int index, long value) {
      final int pos = index >>> 5;
      final int shift = (index & 31) << 1;
      blocks[pos] = blocks[pos] & ~(3L << shift) | value << shift;
    }
  }
  static class Packed64_4 extends Packed64Strategy {
    Packed64_4(int valueCount) {
      super(valueCount, 4);
    }
    Packed64_4(long[] blocks, int valueCount) {
      super(blocks, valueCount, 4);
    }
    @Override
    public final long get(int index) {
      return (blocks[index >>> 4] >>> ((index & 15) << 2)) & 15L;
    }
    @Override
    public final void set(int index, long value) {
      final int pos = index >>> 4;
      final int shift = (index & 15) << 2;
      blocks[pos] = blocks[pos] & ~(15L << shift) | value << shift;
    }
  }
  static class Packed64_8 extends Packed64Strategy {
    Packed64_8(int valueCount) {
      super(valueCount, 8);
    }
    Packed64_8(long[] blocks, int valueCount) {
      super(blocks, valueCount, 8);
    }
    @Override
    public final long get(int index) {
      return (blocks[index >>> 3] >>> ((index &  7) << 3)) & 255L;
    }
    @Override
    public final void set(int index, long value) {
      final int pos = index >>> 3;
      final int shift = (index & 7) << 3;
      blocks[pos] = blocks[pos] & ~(255L << shift) | value << shift;
    }
  }
  static class Packed64_16 extends Packed64Strategy {
    Packed64_16(int valueCount) {
      super(valueCount, 16);
    }
    Packed64_16(long[] blocks, int valueCount) {
      super(blocks, valueCount, 16);
    }
    @Override
    public final long get(int index) {
      return (blocks[index >>> 2] >>> ((index &  3) << 4)) & 65535L;
    }
    @Override
    public final void set(int index, long value) {
      final int pos = index >>> 2;
      final int shift = (index & 3) << 4;
      blocks[pos] = blocks[pos] & ~(65535L << shift) | value << shift;
    }
  }
  static class Packed64_32 extends Packed64Strategy {
    Packed64_32(int valueCount) {
      super(valueCount, 32);
    }
    Packed64_32(long[] blocks, int valueCount) {
      super(blocks, valueCount, 32);
    }
    @Override
    public final long get(int index) {
      return (blocks[index >>> 1] >>> ((index &  1) << 5)) & 4294967295L;
    }
    @Override
    public final void set(int index, long value) {
      final int pos = index >>> 1;
      final int shift = (index & 1) << 5;
      blocks[pos] = blocks[pos] & ~(4294967295L << shift) | value << shift;
    }
  }
  static class Packed64_64 extends Packed64Strategy {
    Packed64_64(int valueCount) {
      super(valueCount, 64);
    }
    Packed64_64(long[] blocks, int valueCount) {
      super(blocks, valueCount, 64);
    }
    @Override
    public final long get(int index) {
      return blocks[index];
    }
    @Override
    public final void set(int index, long value) {
      blocks[index] = value;
    }
  }
  static class Packed64All extends Packed64Strategy {
    Packed64All(int valueCount, int bitsPerValue) {
      super(valueCount, bitsPerValue);
    }

    Packed64All(long[] blocks, int valueCount, int bitsPerValue) {
      super(blocks, valueCount, bitsPerValue);
    }

    @Override
    public final long get(int index) {
      // The abstract index in a bit stream
      final long majorBitPos = (long)index * bitsPerValue;
      // The index in the backing long-array
      final int elementPos = (int)(majorBitPos >>> BLOCK_BITS);
      // The number of value-bits in the second long
      final long endBits = (majorBitPos & MOD_MASK) + bpvMinusBlockSize;

      if (endBits <= 0) { // Single block
        return (blocks[elementPos] >>> -endBits) & maskRight;
      }
      // Two blocks
      return ((blocks[elementPos] << endBits)
          | (blocks[elementPos+1] >>> (BLOCK_SIZE - endBits)))
          & maskRight;
    }
    @Override
    public final void set(int index, long value) {
      // The abstract index in a contiguous bit stream
      final long majorBitPos = (long)index * bitsPerValue;
      // The index in the backing long-array
      final int elementPos = (int)(majorBitPos >>> BLOCK_BITS); // / BLOCK_SIZE
      // The number of value-bits in the second long
      final long endBits = (majorBitPos & MOD_MASK) + bpvMinusBlockSize;

      if (endBits <= 0) { // Single block
        blocks[elementPos] = blocks[elementPos] &  ~(maskRight << -endBits)
            | (value << -endBits);
        return;
      }
      // Two blocks
      blocks[elementPos] = blocks[elementPos] &  ~(maskRight >>> endBits)
          | (value >>> endBits);
      blocks[elementPos+1] = blocks[elementPos+1] &  (~0L >>> endBits)
          | (value << (BLOCK_SIZE - endBits));
    }
  }
}
