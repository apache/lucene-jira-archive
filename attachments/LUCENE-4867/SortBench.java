import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.apache.lucene.util.ArrayUtil;


public class SortBench {

  private static final int SIZE = 50000000;
  @SuppressWarnings("unused")
  private static int dummy;

  public static void main(String[] args) {
    System.out.println("Don't take into account the first values because of JVM warm up");
    final Integer[] arr = new Integer[SIZE];
    Random r = new Random(0);
    for (int i = 0; i < SIZE; ++i) {
      arr[i] = -100 + r.nextInt(200);
    }
    long start;
    for (int i = 0; i < 5; ++i) {
      Collections.shuffle(Arrays.asList(arr));
      start = System.nanoTime();
      ArrayUtil.quickSort(arr);
      dummy = arr[r.nextInt(SIZE)];
      System.out.println("quicksort: " + (System.nanoTime() - start) / 1000 / 1000 + "ms");

      Collections.shuffle(Arrays.asList(arr));
      start = System.nanoTime();
      ArrayUtil.mergeSort(arr);
      dummy = arr[r.nextInt(SIZE)];
      System.out.println("mergesort: " + (System.nanoTime() - start) / 1000 / 1000 + "ms");
    }
  }

}
