package org.apache.lucene.util.packed;

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

import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.util.RamUsageEstimator;

import java.io.IOException;
import java.util.Arrays;

/**
 * Space optimized random access capable array of values with a fixed number of
 * bits. For 32 bits/value and less, performance on 32 bit machines is not
 * optimal. Consider using {@link org.apache.lucene.util.packed.Packed32} for such a setup.
 * </p><p>
 * The implementation strives to avoid conditionals and expensive operations,
 * sacrificing code clarity to achieve better performance.
 */

class Packed64calc extends PackedInts.MutableImpl {
  static final int BLOCK_SIZE = 64; // 32 = int, 64 = long
  static final int BLOCK_BITS = 6; // The #bits representing BLOCK_SIZE
  static final int MOD_MASK = BLOCK_SIZE - 1; // x % BLOCK_SIZE

  private static final int ENTRY_SIZE = BLOCK_SIZE + 1;
  static final int FAC_BITPOS = 3;

  private static int pgcd(int a, int b) {
    if (a < b) {
      return pgcd(b, a);
    } else if (b == 0) {
      return a;
    } else {
      return pgcd(b, a % b);
    }
  }

  /* The bits */
  private final long[] blocks;
  private final long MASK_LEFT;
  private final long MASK_RIGHT;
  private final int BPV_MINUS_BLOCK_SIZE;

  // Cached calculations
  private int maxPos;      // blocks.length * BLOCK_SIZE / elementBits - 1

  /**
   * Creates an array with the internal structures adjusted for the given
   * limits and initialized to 0.
   * @param valueCount   the number of elements.
   * @param bitsPerValue the number of bits available for any given value.
   */
  public Packed64calc(int valueCount, int bitsPerValue) {
    // TODO: Test for edge-cases (2^31 values, 63 bitsPerValue)
    // +2 due to the avoid-conditionals-trick. The last entry is always 0
    this(new long[(int)((long)valueCount * bitsPerValue / BLOCK_SIZE + 2)],
            valueCount, bitsPerValue);
  }


  /**
   * Creates an array backed by the given blocks.
   * </p><p>
   * Note: The blocks are used directly, so changes to the given block will
   * affect the Packed32-structure.
   * @param blocks   used as the internal backing array. Not that the last
   *                 element cannot be addressed directly.
   * @param valueCount the number of values.
   * @param bitsPerValue the number of bits available for any given value.
   */
  public Packed64calc(long[] blocks, int valueCount, int bitsPerValue) {
    super(valueCount, bitsPerValue);
    this.blocks = blocks;
    updateCached();
    MASK_LEFT = ~0L >>> (BLOCK_SIZE-bitsPerValue) << (BLOCK_SIZE-bitsPerValue);
    MASK_RIGHT = ~0L << (BLOCK_SIZE-bitsPerValue) >>> (BLOCK_SIZE-bitsPerValue);
    BPV_MINUS_BLOCK_SIZE = bitsPerValue - BLOCK_SIZE;
  }

  /**
   * Creates an array with content retrieved from the given DataInput.
   * @param in       a DataInput, positioned at the start of Packed64-content.
   * @param valueCount  the number of elements.
   * @param bitsPerValue the number of bits available for any given value.
   * @throws java.io.IOException if the values for the backing array could not
   *                             be retrieved.
   */
  public Packed64calc(DataInput in, int valueCount, int bitsPerValue)
                                                            throws IOException {
    super(valueCount, bitsPerValue);
    int size = size(valueCount, bitsPerValue);
    blocks = new long[size+1]; // +1 due to non-conditional tricks
    // TODO: find a faster way to bulk-read longs...
    for(int i=0;i<size;i++) {
      blocks[i] = in.readLong();
    }
    updateCached();
    MASK_LEFT = ~0L >>> (BLOCK_SIZE-bitsPerValue) << (BLOCK_SIZE-bitsPerValue);
    MASK_RIGHT = ~0L << (BLOCK_SIZE-bitsPerValue) >>> (BLOCK_SIZE-bitsPerValue);
    BPV_MINUS_BLOCK_SIZE = bitsPerValue - BLOCK_SIZE;
  }

  private static int size(int valueCount, int bitsPerValue) {
    final long totBitCount = (long) valueCount * bitsPerValue;
    return (int)(totBitCount/64 + ((totBitCount % 64 == 0 ) ? 0:1));
  }

  private void updateCached() {
    maxPos = (int)((((long)blocks.length) * BLOCK_SIZE / bitsPerValue) - 2);
  }

  /**
   * @param index the position of the value.
   * @return the value at the given index.
   */
  @Override
  public long get(final int index) {
    final long majorBitPos = (long)index * bitsPerValue;
    final int elementPos = (int)(majorBitPos >>> BLOCK_BITS); // / BLOCK_SIZE
//    final int bitPos =     (int)(majorBitPos & MOD_MASK); // % BLOCK_SIZE);

    // Bits in the second block
//    final long endBits = (majorBitPos & MOD_MASK) + bitsPerValue - BLOCK_SIZE;
    final long endBits = (majorBitPos & MOD_MASK) + BPV_MINUS_BLOCK_SIZE;

    if (endBits <= 0) { // Single block
      return (blocks[elementPos] >>> -endBits) & MASK_RIGHT;
    }
    // Two blocks
    return ((blocks[elementPos] << endBits)
        | (blocks[elementPos+1] >>> (BLOCK_SIZE - endBits)))
        & MASK_RIGHT;
    }

  @Override
  public void set(final int index, final long value) {
    final long majorBitPos = (long)index * bitsPerValue;
    final int elementPos = (int)(majorBitPos >>> BLOCK_BITS); // / BLOCK_SIZE
    final int bitPos =     (int)(majorBitPos & MOD_MASK); // % BLOCK_SIZE);

    final long valLeft = value << (BLOCK_SIZE - bitsPerValue); // left aligned
    blocks[elementPos] = blocks[elementPos] & ~(MASK_LEFT >>> bitPos)
        | (valLeft >>> bitPos);

//    final int endBits = bitPos + BPV_MINUS_BLOCK_SIZE;
//    if (endBits <= 0) {
    final int leftShift = BLOCK_SIZE - bitPos;
    if (leftShift >= bitsPerValue) {
      return;
    }
    //final int mask1 =  ~(MASK_LEFT << endBits);

    blocks[elementPos+1] = blocks[elementPos+1] & ~(MASK_LEFT << leftShift)
            | (valLeft << leftShift);
/*    blocks[elementPos+1] = blocks[elementPos+1]
        & ~(MASK_LEFT << (bitsPerValue - endBits))
            | (valLeft << (bitsPerValue - endBits));*/
  }


  @Override
  public String toString() {
    return "Packed64(bitsPerValue=" + bitsPerValue + ", size="
            + size() + ", maxPos=" + maxPos
            + ", elements.length=" + blocks.length + ")";
  }

  @Override
  public long ramBytesUsed() {
    return RamUsageEstimator.sizeOf(blocks);
  }

  @Override
  protected int getFormat() {
    return super.getFormat();    // TODO: Implement this
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
      Packed64calc values = new Packed64calc(nAlignedValues, bitsPerValue);
      for (int i = 0; i < nAlignedValues; ++i) {
        values.set(i, val);
      }
      nAlignedValuesBlocks = (long[])values.getArray();
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

  @Override
  public void clear() {
    Arrays.fill(blocks, 0L);
  }

  @Override
  public Object getArray() {
    return blocks;
  }
}
