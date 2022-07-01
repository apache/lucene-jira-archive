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

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import java.util.BitSet;

public class TestSortedVIntList extends TestCase {
  /** Main for running test case by itself. */
  public static void main(String args[]) {
    TestRunner.run(new TestSuite(TestSortedVIntList.class));
  }
  
  void tstDocNrSkipper(
          SortedVIntList vintList,
          int[] ints) {
    for (int i = 0; i < ints.length; i++) {
      if ((i > 0) && (ints[i-1] == ints[i])) {
        return; // DocNrSkipper should not skip to same document.
      }
    }
    DocNrSkipper dns = vintList.getDocNrSkipper();
    int nextDoc = dns.nextDocNr(0);
    for (int i = 0; i < ints.length; i++) {
      assertTrue("No end of DocNrSkipper at: " + i, nextDoc != -1);
      assertEquals(ints[i], nextDoc);
      nextDoc = dns.nextDocNr(nextDoc + 1);
    }
    assertTrue("End of DocNrSkipper", nextDoc == -1);
  }

  void tstVIntList(
          SortedVIntList vintList,
          int[] ints,
          int expectedByteSize) {
    assertEquals("Size", ints.length, vintList.size());
    assertEquals("Byte size", expectedByteSize, vintList.getByteSize());
    tstDocNrSkipper(vintList, ints);
  }

  public void tstViaBitSet(int [] ints, int expectedByteSize) {
    final int MAX_INT_FOR_BITSET = 1024 * 1024;
    BitSet bs = new BitSet();
    for (int i = 0; i < ints.length; i++) {
      if (ints[i] > MAX_INT_FOR_BITSET) {
        return; // BitSet takes too much memory
      }
      if ((i > 0) && (ints[i-1] == ints[i])) {
        return; // BitSet cannot store duplicate.
      }
      bs.set(ints[i]);
    }
    tstVIntList(new SortedVIntList(bs), ints, expectedByteSize);
  }

  public void tst(int [] ints, int expectedByteSize) {
    tstVIntList(new SortedVIntList(ints, ints.length), ints, expectedByteSize);
    tstViaBitSet(ints, expectedByteSize);
  }

  public void tstIAExc(int [] ints) {
    try {
      new SortedVIntList(ints, ints.length);
    }
    catch (IllegalArgumentException e) {
      return;
    }
    fail("Expected IllegalArgumentException");    
  }
  
  public void test01() {
    int[] ia = new int[] {}; tst(ia, ia.length);
  }
  public void test02() {
    int[] ia = new int[] {0}; tst(ia, ia.length);
  }
  public void test03() {
    int[] ia = new int[] {0,Integer.MAX_VALUE}; tst(ia, 1 + 5);
  }
  public void test04a() {
    int[] ia = new int[] {0, 16384-1}; tst(ia, ia.length + 1);
  }
  public void test04b() {
    int[] ia = new int[] {0, 16384}; tst(ia, ia.length + 2);
  }
  public void test05() { 
   int[] ia = new int[] {0,1,1,2,3,5,8}; tst(ia, ia.length);
  }
  public void test06() {
    int[] ia = new int[]
        {0,100,101,200,300,500/*+1*/,8000/*+1*/,8001,30000/*+2*/};
    tst(ia, ia.length + 4);
  }
  
  public void test10() {
    int[] ia = new int[] {-1}; tstIAExc(ia);
  }
  public void test11() {
    int[] ia = new int[] {1,0}; tstIAExc(ia);
  }
  public void test12() { 
   int[] ia = new int[] {0,1,1,2,3,5,8,0}; tstIAExc(ia);
  }
}
