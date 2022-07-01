package org.apache.lucene.geoviz;

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

import java.util.Random;

/**
 * Morton encodes a 3D point into 96 bits
 */
public class MortonEncoding3D {
  private int upper;
  private long lower;

  // performance
  static long start;
  static long end;
  static double avgTime = 0.0d;
  static long trials = 0l;

  // magic numbers for bit interleaving
  private static final long MAGIC[] = {
      0x5555555555555555L, 0x3333333333333333L,
      0x0F0F0F0F0F0F0F0FL, 0x00FF00FF00FF00FFL,
      0x0000FFFF0000FFFFL, 0x00000000FFFFFFFFL,
      0xAAAAAAAAAAAAAAAAL,
      0x1249249249249249L,
      0x30C30C30C30C30C3L, 0x100F00F00F00F00FL,
      0x001F0000FF0000FFL, 0x001F00000000FFFFL,
      0x00000000001FFFFFL
  };

  private static final int MAGIC_UPPER[] = {
      0x24924924, 0x0C30C30C, 0x00F00F00, 0x0000FF00, 0x00000000, 0x00000000
  };

  private static final long MAGIC_LOWER[] = {
      0x9249249249249249L, 0x30C30C30C30C30C3L, 0xF00F00F00F00F00FL, 0x00FF0000FF0000FFL, 0xFFFF00000000FFFFL, 0x00000000FFFFFFFFL
  };

  // shift values for bit interleaving
  private static final short SHIFT[] = {1, 2, 4, 8, 16, 32};

  private static final short MIN_LON = -180;
  private static final short MIN_LAT = -90;
  public static final short BITS = 32;
  private static final double LON_SCALE = ((0x1L<<BITS)-1)/360.0D;
  private static final double LAT_SCALE = ((0x1L<<BITS)-1)/180.0D;

  public MortonEncoding3D(double lon, double lat, float alt) {
    interleave(scaleLon(lon), scaleLat(lat), alt);
  }

  private static long scaleLon(final double val) {
    return (long) ((val-MIN_LON) * LON_SCALE);
  }

  private static long scaleLat(final double val) {
    return (long) ((val-MIN_LAT) * LAT_SCALE);
  }

  private static double unscaleLon(final long val) {
    return (val / LON_SCALE) + MIN_LON;
  }

  private static double unscaleLat(final long val) {
    return (val / LAT_SCALE) + MIN_LAT;
  }

  private void interleave(long v1, long v2, float v3 ) {
    int[] vH = {0, 0, 0};
    long[] vL = {v1, v2, (long)(Float.floatToIntBits(v3))};

    int tempH = 0;
    long tempL = 0;
    for (int i=0; i<3; ++i) {
      for (int s=5; s>0; --s) {
        // left shift (upper s bits of 'lower' become lower s bits of 'upper')
        tempH = (vH[i]<<SHIFT[s]) | (int) (vL[i] >>> (64-SHIFT[s]));
        tempL = vL[i] << SHIFT[s];
        // or with previous value
        vH[i] |= tempH;
        vL[i] |= tempL;
        // and with the magic number
        vH[i] &= MAGIC_UPPER[s-1];
        vL[i] &= MAGIC_LOWER[s-1];
      }
      // left shift
      vH[i] <<= i;
      vL[i] <<= i;
      // final result is the or of all three
      upper |= vH[i];
      lower |= vL[i];
    }
  }

  public double unhashLon() {
    return unscaleLon(deInterleave(upper, lower));
  }

  public double unhashLat() {
    // shift right by 1
    long shiftedLower = (upper<<(64-SHIFT[0]))|(lower >>> SHIFT[0]);
    int shiftedUpper = upper >>> SHIFT[0];
    return unscaleLat(deInterleave(shiftedUpper, shiftedLower));
  }

  public float unhashAlt() {
    // shift right by 2
    long shiftedLower = (upper<<(64-SHIFT[1]))|(lower >>> SHIFT[1]);
    int shiftedUpper = upper >>> SHIFT[1];
    return Float.intBitsToFloat((int)(deInterleave(shiftedUpper, shiftedLower)));
  }

  private long deInterleave(int origUpper, long origLower) {
    int tempH = origUpper;
    long tempL = origLower;

    tempH &= MAGIC_UPPER[0];
    tempL &= MAGIC_LOWER[0];
    int returnH = tempH;
    long returnL = tempL;
    for (int s=1; s<6; ++s) {
      // shift right
      tempL = (((long)tempH)<<(64-SHIFT[s]))|(tempL >>> SHIFT[s]);
      tempH >>>= SHIFT[s];
      // xor with previous value
      tempH ^= returnH;
      tempL ^= returnL;
      // and with magic number
      tempH &= MAGIC_UPPER[s];
      tempL &= MAGIC_LOWER[s];
      returnH = tempH;
      returnL = tempL;
    }
    return returnL;
  }

  public static void main(String[] args) {
    // validate encoding
//    double lat = 32.234322343256765;
//    double lon = -123.34565436876656;
//    float alt = 552.43243534f;
//
//    MortonEncoding3D hash = new MortonEncoding3D(lon, lat, alt);
//
//    double dLat = hash.unhashLat();
//    double dLon = hash.unhashLon();
//    float dAlt = hash.unhashAlt();
//
//    System.out.println(dLon + ", " + dLat + ", " + dAlt);

    Random r = new Random();

    float alt;

    for (double lon=-180.0; lon<=180.0; lon+=0.0001) {
      for (double lat = -90.0; lat <= 90.0; lat += 0.0001, ++trials) {
        // generate a random alt
        alt = (float)(5000.0 * r.nextFloat());

        start = System.nanoTime();
        final MortonEncoding3D hash = new MortonEncoding3D(lon, lat, alt);
        final double dLon = hash.unhashLon();
        final double dLat = hash.unhashLat();
        final double dAlt = hash.unhashAlt();
        end = System.nanoTime();
        avgTime += (end - start);

        if (trials % 5000000 == 0)
          System.out.println("Avg computation: " + avgTime / trials + " ns  Trials: " + trials + "  Total time: " + avgTime / 1000000.0 + " ms");
      }
    }
  }
}
