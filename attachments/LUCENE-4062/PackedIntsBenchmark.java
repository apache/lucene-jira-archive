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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.lucene.util.packed.PackedInts.Mutable;

/*
PackedIntsBenchmark that extracts the best performances from the different
iterations as opposed to the mean.
Rationale: The mean is highly vulnerable to changing load on the machine and
to random garbage collections. With best performance out of #iterations, such
disturbances must happen at the same time each iterations in order to skew the
results.
 */
public class PackedIntsBenchmark {
  public enum METHOD {direct, threeBlocks, contiguous, padded, doNothing}
  private static long[] buf;

  public static void main(String[] args) throws Exception {
    // Defaults
    int values = 100000000;
    int changes = -1;
    List<METHOD> methods = new ArrayList<METHOD>();
    List<Integer> bpvs = new ArrayList<Integer>(64);

    // Parse
    String mode = null;
    for (String argument: new ArrayList<String>(Arrays.asList(args))) {
      if (argument.startsWith("-")) {
        mode = argument;
        continue;
      }
      if ("-h".equals(mode)) {
        usage();
        return;
      }
      if ("-m".equals(mode)) {
        methods.add(METHOD.valueOf(argument));
      } else if ("-v".equals(mode)) {
        values = Integer.parseInt(argument);
      } else if ("-c".equals(mode)) {
        changes = Integer.parseInt(argument);
      } else if ("-b".equals(mode)) {
        bpvs.add(Integer.valueOf(argument));
      } else {
        System.err.println("Unknown argument: " + argument + "\n");
        usage();
        return;
      }
    }
    if (changes == -1) {
      changes = values;
    }
    if (methods.size() == 0) {
      methods.addAll(Arrays.asList(METHOD.values()));
    }
    if (bpvs.size() == 0) {
      for (int bpv = 1 ; bpv <= 64 ; bpv++) {
        bpvs.add(bpv);
      }
    }

    performMeasurement(methods, values, bpvs, changes);
  }

  private static void usage() {
    System.out.println("Usage: PackedIntsBenchmark [-h] [-m method*] [-v count]"
        + "[-c count] [-b bpv*]");
    System.out.println("\n-h: Help");
    System.out.println("-m: Space separated list of methods to test. Valid"
        + " methods are contiguous, padded, direct, threeBlocks and doNothing."
        + " Default: All methods except doNothing");
    System.out.println("-v: The number of values in the PackedInts. "
        + "Default: 10000000");
    System.out.println("-c: The number of randoms changes (gets and sets) to "
        + "perform on the values. Default: Same as -v");
    System.out.println("-b: Space separated list of bits per value to test. "
        + "Valid bpv's are 1 to 64, inclusive. Default is all bpvs from 1-64");
  }

  public static void performMeasurement(
      List<METHOD> methods, final int valueCount,
      List<Integer> bpvs, final int changes)
      throws InterruptedException {
    System.out.print(String.format("valueCount=%d, changes=%d, methods=",
        valueCount, changes));
    for (METHOD method: methods) {
      System.out.print(method.toString() + " ");
    }
    System.out.print(", bpvs=");
    for (Integer bpv: bpvs) {
      System.out.print(Integer.toString(bpv) + " ");
    }
    System.out.println();

    int[] offsets = new int[changes];
    long[] values = new long[changes];
    buf = new long[valueCount];

    long[][] sets = new long[methods.size()][65];
    long[][] gets = new long[methods.size()][65];
    long[][] bsets = new long[methods.size()][65];
    long[][] bgets = new long[methods.size()][65];

    System.out.println();
    LongRandom random = new LongRandom();
    List<Mutable> mutables = new ArrayList<Mutable>();
    // first run to warm the JVM, then record
    for (int k = 0; k < 6; ++k) {
      if (k == 1) {
        // clean garbage from the first run
        System.gc();
        Thread.sleep(1000);
      }
      for (int bpv: bpvs) {
        System.out.print("Iteration " + k + ", bits per value " + bpv
            + " (Optimization-disabling output: ");
        updateMutables(methods, mutables, valueCount, bpv);
        if (k >= 1) {
          // run the GC preventively so that garbage collection does not affects the tests too much
          System.gc();
          Thread.sleep(200);
        }
        
        for (int p = 0; p < mutables.size(); ++p) {
          Arrays.fill(buf, 0);
          for (int i = 0; i < changes; ++i) {
            offsets[i] = random.nextInt(valueCount);
            values[i] = random.nextLong(bpv);
          }
          Mutable mutable = mutables.get(p);
          if (mutable == null) {
            continue;
          }

          // sets
          long start = System.nanoTime();
          for (int i = 0; i < changes; ++i) {
            mutable.set(offsets[i], values[i]);
          }
          long end = System.nanoTime();
          if (k >= 1)
            sets[p][bpv] = Math.max(sets[p][bpv],
                changes * 1000000000L / (end - start));
          Arrays.fill(buf, 0);

          // gets
          start = System.nanoTime();
          long jitTrick = 0;
          for (int i = 0; i < changes; ++i) {
            jitTrick += mutable.get(offsets[i]);
          }
          end = System.nanoTime();
          System.out.print(jitTrick);
          if (k >= 1)
            gets[p][bpv] = Math.max(gets[p][bpv],
                changes * 1000000000L / (end - start));
          mutable.clear();

          // bsets
          int written = 0;
          start = System.nanoTime();
          while (written < valueCount) {
            written += mutable.set(written, buf, written, valueCount - written);
          }
          end = System.nanoTime();
          if (k >= 1)
            bsets[p][bpv] = Math.max(bsets[p][bpv],
                valueCount * 1000000000L / (end - start));
          Arrays.fill(buf, 0);

          // bgets
          int read = 0;
          start = System.nanoTime();
          while (read < valueCount) {
            read += mutable.get(read, buf, read, valueCount - read);
          }
          end = System.nanoTime();
          if (k >= 1)
            bgets[p][bpv] = Math.max(bgets[p][bpv],
                valueCount * 1000000000L / (end - start));
          System.out.print(sum(buf));
        }
        System.out.println(")");
      }
      if (k>=1){
        print("SET", sets, bpvs, k);
        print("GET", gets, bpvs, k);
        print("BULKSET", bsets, bpvs, k);
        print("BULKGET", bgets, bpvs, k);
      }
    }
  }

  private static void print(
      String designation, long[][] measurements, List<Integer> bpvs,
      int iteration) {
    System.out.println(designation);
    for (int bpv: bpvs) {
      System.out.print('[');
      System.out.print(bpv);
      for (int p = 0; p < measurements.length; ++p) {
        System.out.print(',');
        System.out.print(measurements[p][bpv]);
      }
      System.out.println("],");
    }
  }

  private static void updateMutables(
      List<METHOD> methods, List<Mutable> mutables, int valueCount, int bpv) {
    mutables.clear();
    for (METHOD method: methods) {
      switch (method) {
        case contiguous:
          mutables.add(new Packed64(valueCount, bpv));
          break;
        case direct:
          if (bpv <= 8) {
            mutables.add(new Direct8(valueCount));
          } else if (bpv <= 16) {
            mutables.add(new Direct16(valueCount));
          } else if (bpv <= 32) {
            mutables.add(new Direct32(valueCount));
          } else {
            mutables.add(new Direct64(valueCount));
          }
          break;
        case doNothing:
          mutables.add(new PackedZero(valueCount, bpv));
          break;
        case padded:
          if (bpv <= 32) {
            int nbpv = bpv;
            while (!Packed64SingleBlock.isSupported(nbpv)) {
              nbpv++;
            }
            mutables.add(Packed64SingleBlock.create(valueCount, nbpv));
          } else {
            mutables.add(null);
          }
          break;
        case threeBlocks:
          if (bpv <= 24) {
            mutables.add(new Packed8ThreeBlocks(valueCount));
          } else if (bpv <= 48) {
            mutables.add(new Packed16ThreeBlocks(valueCount));
          } else {
            mutables.add(null);
          }
          break;
        default: throw new UnsupportedOperationException(
            "The METHOD " + method + " is not supported");
      }
    }
  }

  @SuppressWarnings("serial")
  private static class LongRandom extends Random {
    public long nextLong(int bits) {
      if (bits > 64) {
        throw new IllegalArgumentException();
      }
      if (bits <= 32) {
        return next(bits) & 0xffffffffL;
      } else {
        int bits1 = bits / 2;
        int bits2 = bits - bits1;
        return (nextLong(bits1) << bits2) | nextLong(bits2);
      }
    }
  }

  private static long sum(long[] arr) {
    long r = 0;
    for (long l : arr) {
      r += l;
    }
    return r;
  }


}
