package org.apache.lucene.search;

import java.util.Random;

import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.OpenBitSetIterator;
import org.apache.lucene.util.OpenBitSetIteratorExperiment;

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

/**
 *
 * @version $Id$
 */
public class TestIteratorPerf  {
  //Random r = new Random(0);
  static final int ITER = 10;
  static final int ITERATOR_SIZE = 10000000;
  static final double DENSITY = 1.0;//Gaussian... 1.0 != 100%
  
 private static final Random random  = new Random(7);
  
  public int doOld(int iter, OpenBitSet bs) {
    int doc=-1;
    for (int i=0; i<iter; i++) {
      OpenBitSetIterator it = new OpenBitSetIterator(bs);
      while(it.next()){
        doc = it.doc();
        if(doc++ <-200) break; //avoid optimization
      }
      
    }
    return doc;
  }

 
  public int doNew(int iter, OpenBitSet bs){
    int doc=-1;
    for (int i=0; i<iter; i++) {
      OpenBitSetIteratorExperiment it = new OpenBitSetIteratorExperiment(bs);
      while(-1!=(doc=it.next())){
        if(doc++ <-200) break; //avoid optimization
      }
      
    }
    return doc;
  }


  public long testOld(OpenBitSet bs) {
    long total=0;
    for (int i=0; i<ITER ; i++) {
      long start = System.currentTimeMillis();
      doOld(ITER, bs );
      long end = System.currentTimeMillis();
      total+=(end-start);
      System.out.println("old  milliseconds="+(end-start));
    }
    System.out.println("old total milliseconds="+total+"\n");
    return total;
  }

  public long testNew(OpenBitSet bs) {
    long total=0;
    for (int i=0; i<ITER ; i++) {
      long start = System.currentTimeMillis();
      doNew(ITER, bs );
      long end = System.currentTimeMillis();
      total+=(end-start);
      System.out.println("new  milliseconds="+(end-start));
    }
    System.out.println("new total milliseconds="+total);
    return total;
  }


  public static void main(String[] argc) throws InterruptedException{
    TestIteratorPerf t = new TestIteratorPerf();
    OpenBitSet bs = new OpenBitSet(ITERATOR_SIZE);
    for(int i=0; i<=ITERATOR_SIZE;i++){
      if(Math.abs(random.nextGaussian())<DENSITY) 
        bs.set(i);
    }
    //System.out.println(bs.cardinality());
    System.gc();
    Thread.sleep(200);
    long newT = t.testNew(bs);
    System.gc();
    Thread.sleep(200);
    long oldT = t.testOld(bs);
    
    System.out.println("New/Old Time " + newT + "/" + oldT +" (" + 100.0f*newT/oldT +"%)");
    
  }

}
