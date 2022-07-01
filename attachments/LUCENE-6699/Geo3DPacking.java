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

import java.util.BitSet;

import org.apache.lucene.util.GeoProjectionUtils;

/**
 * Packs 3 dimensional point values into a 96 bit BitSet. This is done by first converting lat/lon/alt
 * values to Earth-Centered Earth-Fixed (ECEF) using the non-iterative WGS84 based conversion in
 * {@code GeoProjectionUtils.llatToECF}. The result is a 3D spheroid coordinate represented as a 3D
 * cartesian coordinate. The cartesian coordinates are scaled to 32 bits and interleaved. The decoding
 * is performed in the reverse manner using the non-iterative {@code GeoProjectionUtils.ecefToLLA}
 *
 * @lucene.experimental
 */
public class Geo3DPacking {
  private static final int SCALE_FACTOR = Float.floatToRawIntBits(Float.MAX_VALUE);
  private static final short BITS = 32;
  private static final short ENCODED_LENGTH = BITS * 3;

  public static BitSet encode(double lon, double lat, double alt) {
    double[] pt = GeoProjectionUtils.llaToECF(lon, lat, alt, null);
    int[] f = new int[3];
    f[0] = scale(pt[0]);
    f[1] = scale(pt[1]);
    f[2] = scale(pt[2]);
    return interleave(f);
  }

  public static float[] decodeToUnitSpheroid(final BitSet b) {
    int[] i = deinterleave(b);
    return new float[] {Float.intBitsToFloat(i[0]), Float.intBitsToFloat(i[1]), Float.intBitsToFloat(i[2]) };
  }

  public static double[] decodeToLatLon(final BitSet b, double[] d) {
    if (d == null) {
      d = new double[3];
    }

    final int [] f = deinterleave(b);

    d[0] = unscale(f[0]);
    d[1] = unscale(f[1]);
    d[2] = unscale(f[2]);

    return GeoProjectionUtils.ecfToLLA(d[0], d[1], d[2], d);
  }

  private static int scale(double v1) {
    return Float.floatToRawIntBits((float)(v1/SCALE_FACTOR));
  }

  private static double unscale(int v) {
    return (double)(Float.intBitsToFloat(v) * SCALE_FACTOR);
  }

  private static BitSet interleave(int[] vals) {
    BitSet b = new BitSet(ENCODED_LENGTH);
    int MASK = 1;
    for (short i=0; i<ENCODED_LENGTH; i+=3) {
      if ( (vals[0] & MASK) == MASK) {
        b.set(i);
      }
      if ( (vals[1] & MASK) == MASK) {
        b.set(i+1);
      }
      if ( (vals[2] & MASK) == MASK) {
        b.set(i+2);
      }
      MASK <<= 1;
    }
    return b;
  }

  private static int[] deinterleave(final BitSet b) {
    int[] d = new int[3];
    int MASK = 1;
    for (short i=0; i<ENCODED_LENGTH; i+=3) {
      if (b.get(i)) {
        d[0] = (  d[0] | MASK );
      }
      if (b.get(i + 1)) {
        d[1] = (d[1] | MASK );
      }
      if (b.get(i + 2)) {
        d[2] = (d[2] | MASK);
      }
      MASK <<= 1;
    }

    return d;
  }

  public static void main(String[] args) {
    BitSet b = encode(-123.2345643, 43.2342342, 2555.2432234);
    float[] scaled = decodeToUnitSpheroid(b);
    double[] latLon = decodeToLatLon(b, null);

    System.out.println(scaled[0] + ", " + scaled[1] + ", " + scaled[2]);
    System.out.println(latLon[0] + ", " + latLon[1] + ", " + latLon[2]);
  }
}
