package com.carrotsearch.lingo4g.internal.hppc.extras;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.carrotsearch.hppc.IntArrayList;

/**
 * Utilities to traverse a {@link ByteBlockList} in the key-order.
 */
final class ByteBlockListSorter {
  static class Buffers {
    final int [] counts = new int [256 + 1];
    final int [] piles  = new int [256 + 1];

    final byte [] fission;
    final int [] redistribution;
    
    public Buffers(int fissionBufferSize, int redistributionBufferSize) {
      this.fission = new byte [fissionBufferSize];
      this.redistribution = new int [redistributionBufferSize];
    }
  }

  static class SortJob {
    final int from;
    final int to;
    final int column;

    SortJob(int from, int to, int column) {
      this.from = from;
      this.to = to;
      this.column = column;
    }

    /**
     * Implements optimized radix sort on strings.
     */
    void compute(Deque<SortJob> subTasks, ByteBlockList bbl, int[] ords, Buffers buffers) {
      final int range = to - from;
      final int column = this.column;

      // Compute distribution histograms.
      computeCounts(bbl, ords, range, buffers);

      // All inputs already shorter than the column we're sorting on, everything is sorted.
      final int [] counts = buffers.counts;
      if (counts[0] == range) {
        return;
      }

      // Prepare pile boundaries from counts.
      final int [] piles = buffers.piles;
      System.arraycopy(counts, 0,
                       piles, 0, counts.length);

      // Redistribute ords according to the byte at 'column'
      final int [] redistributionBuffer = buffers.redistribution;
      if (range > redistributionBuffer.length) {
        redistributeInPlace(bbl, column, ords, counts, piles);
      } else {
        redistributeWithBuffer(bbl, column, ords, piles, redistributionBuffer);
      }

      // Append recursive sorts as separate jobs (to avoid stack recursion).
      for (int i = 0, subFrom = from; i < counts.length; i++) {
        int bucketSize = counts[i];
        if (bucketSize > 1) {
          int subTo = subFrom + bucketSize;
          if (bucketSize < 20) {
            insertionSort(bbl, ords, subFrom, subTo, column + 1);
          } else {
            subTasks.add(new SortJob(subFrom, subTo, column + 1));
          }
        }
        subFrom += bucketSize;
      }
    }

    void redistributeWithBuffer(ByteBlockList bbl, int column, 
                                int[] ords, int[] piles, int[] redistributionBuffer) {
      // Accumulate positions. Terminated strings come first.
      for (int i = 0, offset = 0; i < piles.length; i++) {
        int v = piles[i];
        piles[i] += offset;
        offset += v;
      }

      // Redistribute to dst.
      int [] dst = redistributionBuffer;
      for (int i = from, max = to; i < max; i++) {
        int ord = ords[i];
        int slot = (bbl.length(ord) <= column ? 0 : 1 + (bbl.byteAt(ord, column) & 0xff));
        dst[--piles[slot]] = ord;
      }

      // Copy back to src.
      System.arraycopy(
           dst,    0,
          ords, from, (to - from));
    }

    void redistributeInPlace(ByteBlockList bbl, int column, 
                             int[] ords, int[] counts, int[] piles) {
      // Accumulate positions. Terminated strings come first.
      for (int i = 0, offset = from; i < piles.length; i++) {
        int v = piles[i];
        piles[i] += offset;
        offset += v;
      }
      
      // Redistribute (in place). After "Engineering Radix Sort", P. McIlroy, D. McIlroy
      // 
      // This renders the sorting unstable (equal input ords may be shuffled).
      // 
      // This is slightly slower than rearranging using a spare array, but we don't need
      // additional memory, which can be a significant ratio if the input strings are 
      // relatively short (which they typically are). 
      for (int i = from, max = to; i < max;) {
        int ord = ords[i];
        while (true) {
          int slot = (bbl.length(ord) <= column ? 0 : 1 + (bbl.byteAt(ord, column) & 0xff));
          int t = --piles[slot];
          if (t <= i) {
            ords[i] = ord;
            i += counts[slot];
            break;
          }
          int tmp = ords[t]; ords[t] = ord; ord = tmp; // swap(ords[t], ord)
        }
      }
    }

    private void computeCounts(ByteBlockList bbl, int[] ords, final int range, Buffers buffers) {
      final int [] counts = buffers.counts;
      Arrays.fill(counts, 0);

      final byte[] fissionBuffer = buffers.fission;
      final int column = this.column;
      final int to = this.to;

      int terminated = 0;
      if (range < buffers.fission.length * 2) {
        for (int i = from; i < to;) {
          final int e = ords[i++];
          if (bbl.length(e) <= column) {
            terminated++;
          } else {
            counts[1 + (bbl.byteAt(e, column) & 0xff)]++;
          }
        }
      } else {
        for (int i = from; i < to;) {
          final int max = Math.min(fissionBuffer.length, to - i);
          int z = 0;
          for (int j = 0; j < max; j++) {
            final int e = ords[i++];
            if (bbl.length(e) <= column) {
              terminated++;
            } else {
              fissionBuffer[z++] = bbl.byteAt(e, column);
            }
          }

          for (int j = 0; j < z;) {
            counts[1 + (fissionBuffer[j++] & 0xff)]++;
          }
        }
      }

      counts[0] = terminated;
    }

    private static void insertionSort(ByteBlockList bbl, int[] ords, int from, int to, int column) {
      for (int i = from + 1; i < to; i++) {
        final int v = ords[i];
        int j = i, t;
        while (j > from && bbl.compareAssumingEqualPrefix(t = ords[j - 1], v, column) > 0) {
          ords[j--] = t;
        }
        ords[j] = v;
      }
    }
  }

  private ByteBlockListSorter() {
  }

  /**
   * Single-threaded version.
   */
  static void sortOrdsByKey(IntArrayList ords, final ByteBlockList bbl) {
    final int length = ords.size();

    final Buffers buffers = new Buffers(
        Math.min(ords.size(),  4 * 1024), 
        Math.min(ords.size(), 32 * 1024));

    ArrayDeque<SortJob> q = new ArrayDeque<>();
    q.add(new SortJob(0, length, 0));
    while (!q.isEmpty()) {
      q.removeLast().compute(q, bbl, 
                             ords.buffer, 
                             buffers);
    }
  }

  /**
   * Multithreaded version.
   */
  static void sortOrdsByKey(IntArrayList ords, final ByteBlockList bbl, int threads) {
    if (threads < 0) throw new IllegalArgumentException();
    if (threads == 1) {
      sortOrdsByKey(ords, bbl);
      return;
    }

    class WorkerThreadWithBuffer extends ForkJoinWorkerThread {
      final Buffers buffers;

      public WorkerThreadWithBuffer(ForkJoinPool pool, Buffers buffers) {
        super(pool);
        this.buffers = buffers;
      }
    }

    final ForkJoinWorkerThreadFactory threadFactory = new ForkJoinWorkerThreadFactory() {
      @Override
      public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        return new WorkerThreadWithBuffer(pool, new Buffers(
            Math.min(ords.size(),  4 * 1024), 
            Math.min(ords.size(), 64 * 1024)));
      }
    };
    final ForkJoinPool pool = new ForkJoinPool(threads, threadFactory, null , false);
    try {
      final int length = ords.size();
  
      @SuppressWarnings("serial")
      class SortTask extends RecursiveAction {
        private final SortJob job;

        SortTask(SortJob job) {
          this.job = job;
        }

        @Override
        protected void compute() {
          ArrayDeque<SortJob> recurse = new ArrayDeque<>();
          job.compute(recurse, bbl, ords.buffer, 
              ((WorkerThreadWithBuffer) Thread.currentThread()).buffers);
          if (!recurse.isEmpty()) {
            invokeAll(recurse.stream()
                             .map((job) -> new SortTask(job))
                             .collect(Collectors.toList()));
          }
        }
      }
      pool.submit(new SortTask(new SortJob(0, length, 0))).join();
    } finally {
      pool.shutdown();
      try {
        if (!pool.awaitTermination(1, TimeUnit.MINUTES)) {
          throw new RuntimeException("The pool didn't shutdown in time.");
        }
      } catch (InterruptedException e) {
        throw new RuntimeException("Unexpected interruption.", e);
      }
    }
  }
}
