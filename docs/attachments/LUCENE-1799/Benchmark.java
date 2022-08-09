package org.apache.lucene.util;

import java.util.Random;

public class Benchmark {
  public static void main(String args[]) {
    long seed = System.currentTimeMillis();
    
    Random random = new Random(seed);
    char[][] terms = new char[1000][];
    for (int i=0; i<terms.length; i++) {
      int length = _TestUtil.nextInt(random, 3, 7); // pivot around english avg word length=5
      char[] word = new char[length];
      for (int j = 0; j < length; j++) // build a random word
         word[j] = (char) _TestUtil.nextInt(random, 'a', 'z');
      terms[i] = word;
    }

    int ret = 0;
    BytesRef result = new BytesRef();
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < 100000; i++) {
      for (char[] word : terms) {
        UnicodeUtil.UTF16toUTF8(word, 0, word.length, result);
        ret += result.length;
      }
    }
    long endTime = System.currentTimeMillis();
    System.out.println("ret=" + ret + " UTF-8 encode: " + (endTime-startTime));

    ret = 0;
    result = new BytesRef();
    startTime = System.currentTimeMillis();
    for (int i = 0; i < 100000; i++) {
      for (char[] word : terms) {
        BOCUUtil.UTF16toBOCU1(word, 0, word.length, result);
        ret += result.length;
      }
    }
    endTime = System.currentTimeMillis();
    System.out.println("ret=" + ret + "BOCU-1 encode: " + (endTime-startTime));

  }
}