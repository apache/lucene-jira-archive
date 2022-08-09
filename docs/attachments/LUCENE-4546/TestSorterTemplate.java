package org.apache.lucene.util;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;

public class TestSorterTemplate extends RandomizedTest {
  
  @Test
  public void testCurrentlyFailingQuickSort() {
    final int x[] = new int[] {2, 9, 3, 5, 8, 6, 4, 7, 1};
//    final int x[] = new int[] {7, 6, 3, 5, 8, 9, 4, 2, 1};
//    final int x[] = new int[] {7, 6, 3, 7, 8, 7, 4, 2, 1};
  
    SorterTemplate sorter = new SorterTemplateImpl(x);
    System.out.println(Arrays.toString(x));
    sorter.quickSort(0, x.length-1);
    System.out.println(Arrays.toString(x));
    
    checkSorted(x);
  }
  
  @Test
//  @Repeat(iterations=100)
  public void testQuickSortRandomly() {
    Random r = getRandom();
    final int n = r.nextInt(10) + 1;
    final int x[] = new int[n];
    for (int i = 0; i < n; i++)
      x[i] = r.nextInt();
    
    SorterTemplate sorter = new SorterTemplateImpl(x);
    sorter.quickSort(0, x.length - 1);
    
    checkSorted(x);
  }

  class SorterTemplateImpl extends SorterTemplate {
    private int x[];
    private int pivot;
    
    public SorterTemplateImpl(int x[]) {
      this.x = x;
    }
    
    @Override
    protected void swap(int i, int j) {
      final int tmp = x[i];
      x[i] = x[j];
      x[j] = tmp;
    }
    
    @Override
    protected void setPivot(int i) {
      this.pivot = x[i];
    }
    
    @Override
    protected int comparePivot(int j) {
      return pivot < x[j] ? -1 :
             pivot > x[j] ? +1 : 0;
    }
    
    @Override
    protected int compare(int i, int j) {
      return x[i] < x[j] ? -1 :
             x[i] > x[j] ? +1 : 0;
    }
  };

  public static void checkSorted(int x[]) {
    int before = Integer.MIN_VALUE;
    for (int i = 0; i < x.length; i++) {
      assertTrue("not sorted: element at index " + i
          + " should be larger/equal to previous element", x[i] >= before);
      before = x[i];
    }
  }

}
