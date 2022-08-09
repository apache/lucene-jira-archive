package org.apache.lucene.util;

/**
 * Copyright 2006 The Apache Software Foundation
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import junit.framework.TestCase;
import org.apache.lucene.store.RAMDirectory;

/** Tests for SortExternal class.
 *
 * @author Marvin Humphrey
 */

public class TestSortExternal extends TestCase {

  private RAMDirectory directory;
  private SortExternal sortEx;

  static String[] alphabet = new String[]{ 
      "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
      "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z" 
  };

  public void setUp() throws Exception {
    directory = new RAMDirectory();
  }

  private void shuffleTestStrings(String[] orig) throws Exception {
    ArrayList shuffled = new ArrayList();
    for (int i = 0; i < orig.length; i++) {
      shuffled.add(orig[i]);
    }
    Collections.shuffle(shuffled);
    for (int i = 0; i < orig.length; i++) {
      String s = (String)shuffled.get(i);
      byte[] bytes = s.getBytes("UTF-8");
      sortEx.feed(bytes);
    }
    sortEx.sortAll();
    ArrayList sortOutput = new ArrayList();
    byte[] result;
    while((result = sortEx.fetch()) != null) {
      sortOutput.add(new String(result, "UTF-8"));
    }
    assertEquals(orig.length, sortOutput.size());
    for (int i = 0; i < orig.length; i++) {
      assertEquals(orig[i], (String)sortOutput.get(i));
    }
  }

  private void shuffleTestBytes(byte[][] orig) throws Exception {
    ArrayList shuffled = new ArrayList();
    for (int i = 0; i < orig.length; i++) {
      shuffled.add(orig[i]);
    }
    Collections.shuffle(shuffled);
    for (int i = 0; i < orig.length; i++) {
      byte[] bytes = (byte[])shuffled.get(i);
      sortEx.feed(bytes);
    }
    sortEx.sortAll();
    ArrayList sortOutput = new ArrayList();
    byte[] result;
    while((result = sortEx.fetch()) != null) {
      sortOutput.add(result);
    }
    assertEquals(orig.length, sortOutput.size());
    for (int i = 0; i < orig.length; i++) {
      result = (byte[])sortOutput.get(i);
      assertEquals(orig[i].length, result.length);
      for (int j = 0; j < result.length; j++) {
        assertEquals(orig[i][j], result[j]);
      }
    }
  }

  public void testLetters() throws Exception {
    sortEx = new SortExternal(directory, "_1");
    shuffleTestStrings(alphabet);
  }

  public void testLettersAndEmptyStrings() throws Exception {
    sortEx = new SortExternal(directory, "_1");

    String[] orig = new String[] { 
      "", "", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
      "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
    };
    shuffleTestStrings(orig);
  }

  public void testRepeatingLetters() throws Exception {
    sortEx = new SortExternal(directory, "_1");

    String[] orig = new String[] { 
      "a", "a", "a", "b", "c", "d", "x", "x", "x", "x", "x", "y", "y", "y"
    };
    shuffleTestStrings(orig);
  }

  public void testLoMemThreshold() throws Exception {
    sortEx = new SortExternal(directory, "_1", 30);
    shuffleTestStrings(alphabet);
  }

  public void testUltraLoMemThreshold() throws Exception {
    sortEx = new SortExternal(directory, "_1", 1);
    shuffleTestStrings(alphabet);
  }

  public void testEmptySortSet() throws Exception {
    sortEx = new SortExternal(directory, "_1", 1);
    byte[][] orig = new byte[0][];
    shuffleTestBytes(orig);
  }

  public void testBigEndianInts() throws Exception {
    // set memThreshold to where cache gets flushed a few times
    sortEx = new SortExternal(directory, "_1", 20000);

    byte[][] orig = new byte[11000][];
    for (int i = 0; i < 11000; i++) {
      byte[] serializedInt = new byte[4];
      serializedInt[0] = (byte)(i >> 24);
      serializedInt[1] = (byte)(i >> 16);
      serializedInt[2] = (byte)(i >> 8);
      serializedInt[3] = (byte)(i);
      orig[i] = serializedInt;
    }
    shuffleTestBytes(orig);
  }

  public void testRandomByteSequences() throws Exception {
    // set memThreshold to where cache gets flushed a few times
    sortEx = new SortExternal(directory, "_1", 20000);

    Random rand = new Random(13); // deterministic

    // create an array of random bytestrings, ordered as if using memcmp
    ByteQueue bq = new ByteQueue(1000);
    for (int i = 0; i < 1000; i++) {
      int len = (int)(rand.nextFloat() * 1200); 
      byte[] bytes = new byte[len];
      rand.nextBytes(bytes);
      bq.insert((Object)bytes);
    }
    byte[][] orig = new byte[1000][];
    for (int i = 0; i < 1000; i++) {
      orig[i] = (byte[])bq.pop();
    }

    shuffleTestBytes(orig);
  }

  private final class ByteQueue extends PriorityQueue {
    ByteQueue(int size) {
      initialize(size);
    }
    
    protected final boolean lessThan(Object a, Object b) {
      byte[] bytes1 = (byte[])a;
      byte[] bytes2 = (byte[])b;
      int end = Math.min(bytes1.length, bytes2.length);
      for (int k = 0; k < end; k++) {
        int b1 = (bytes1[k] & 0xFF);
        int b2 = (bytes2[k] & 0xFF);
        if (b1 != b2)
          return (b1 - b2) < 0;
      }
      return (bytes1.length - bytes2.length) < 0;
    }
  }
}

