package org.apache.lucene.search;

import java.util.Random;
import java.util.BitSet;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

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
public class TestScorerPerformance extends TestCase {
  Random r = new Random(0);
  
  BitSet[] sets;
  Term[] terms;
  IndexSearcher s;

  public void createDummySearcher() throws Exception {
      // Create a dummy index with nothing in it.
    // This could possibly fail if Lucene starts checking for docid ranges...
    RAMDirectory rd = new RAMDirectory();
    IndexWriter iw = new IndexWriter(rd,new WhitespaceAnalyzer(), true);
    iw.close();
    s = new IndexSearcher(rd);
  }

  public void createRandomTerms(int nDocs, int nTerms, double power, Directory dir) throws Exception {
    int[] freq = new int[nTerms];
    terms = new Term[nTerms];
    for (int i=0; i<nTerms; i++) {
      int f = (nTerms+1)-i;  // make first terms less frequent
      freq[i] = (int)Math.ceil(Math.pow(f,power));
      terms[i] = new Term("f",Character.toString((char)('A'+i)));
    }

    IndexWriter iw = new IndexWriter(dir,new WhitespaceAnalyzer(), true);
    for (int i=0; i<nDocs; i++) {
      Document d = new Document();
      for (int j=0; j<nTerms; j++) {
        if (r.nextInt(freq[j]) == 0) {
          d.add(new Field("f", terms[j].text(), Field.Store.NO, Field.Index.UN_TOKENIZED));
          //System.out.println(d);
        }
      }
      iw.addDocument(d);
    }
    iw.optimize();
    iw.close();
  }


  public BitSet randBitSet(int sz, int numBitsToSet) {
    BitSet set = new BitSet(sz);
    for (int i=0; i<numBitsToSet; i++) {
      set.set(r.nextInt(sz));
    }
    return set;
  }

  public BitSet[] randBitSets(int numSets, int setSize) {
    BitSet[] sets = new BitSet[numSets];
    for (int i=0; i<sets.length; i++) {
      sets[i] = randBitSet(setSize, r.nextInt(setSize));
    }
    return sets;
  }

  public static class BitSetFilter extends Filter {
    public BitSet set;
    public BitSetFilter(BitSet set) {
      this.set = set;
    }
    public BitSet bits(IndexReader reader) throws IOException {
      return set;
    }
  }

  public static class CountingHitCollector extends HitCollector {
    int count=0;
    int sum=0;

    public void collect(int doc, float score) {
      count++;
      sum += doc;  // use it to avoid any possibility of being optimized away
    }

    public int getCount() { return count; }
    public int getSum() { return sum; }
  }


 
  BitSet addClause(BooleanQuery bq, BitSet result, BooleanClause.Occur occur) {
    BitSet rnd = sets[r.nextInt(sets.length)];
    Query q = new ConstantScoreQuery(new BitSetFilter(rnd));
    bq.add(q, occur);
    return result;
  }


  public int doConjunctions(int iter, int maxClauses) throws IOException {
    int ret=0;

    for (int i=0; i<iter; i++) {
      int nClauses = r.nextInt(maxClauses-1)+2; // min 2 clauses
      BooleanQuery bq = new BooleanQuery();
      BitSet result=null;
      for (int j=0; j<nClauses; j++) {
        result = addClause(bq,result,BooleanClause.Occur.MUST);
      }

      CountingHitCollector hc = new CountingHitCollector();
      s.search(bq, hc);
      ret += hc.getSum();
    }
    
    return ret;
  }

  public int doNestedConjunctions(int iter, int maxOuterClauses, int maxClauses) throws IOException {
    int ret=0;
    long nMatches=0;

    for (int i=0; i<iter; i++) {
      int oClauses = r.nextInt(maxOuterClauses-1)+2;
      BooleanQuery oq = new BooleanQuery();
      BitSet result=null;

      for (int o=0; o<oClauses; o++) {

      int nClauses = r.nextInt(maxClauses-1)+2; // min 2 clauses
      BooleanQuery bq = new BooleanQuery();
      for (int j=0; j<nClauses; j++) {
        result = addClause(bq,result, BooleanClause.Occur.MUST);
      }

      oq.add(bq, BooleanClause.Occur.MUST);
      } // outer

      CountingHitCollector hc = new CountingHitCollector();
      s.search(oq, hc);
      nMatches += hc.getCount();
      ret += hc.getSum();
    }
    System.out.println("Average number of matches="+(nMatches/iter));
    return ret;
  }

  

  public int doDisjunctions(int iter, int maxClauses) throws IOException {
    int ret=0;

    for (int i=0; i<iter; i++) {
      int nClauses = r.nextInt(maxClauses-1)+2; // min 2 clauses
      BooleanQuery bq = new BooleanQuery();
      BitSet result=null;
      for (int j=0; j<nClauses; j++) {
        result = addClause(bq,result,BooleanClause.Occur.SHOULD);
      }
      if(nClauses>3) bq.setMinimumNumberShouldMatch(2);
      CountingHitCollector hc = new CountingHitCollector();
      s.search(bq, hc);
      ret += hc.getSum();
    }
    
    return ret;
  }
  
  
  
  public int doNestedDisjunctions(int iter, int maxOuterClauses, int maxClauses) throws IOException {
    int ret=0;
    long nMatches=0;

    for (int i=0; i<iter; i++) {
      int oClauses = r.nextInt(maxOuterClauses-1)+2;
      BooleanQuery oq = new BooleanQuery();
      BitSet result=null;

      for (int o=0; o<oClauses; o++) {

      int nClauses = r.nextInt(maxClauses-1)+2; // min 2 clauses
      BooleanQuery bq = new BooleanQuery();
      for (int j=0; j<nClauses; j++) {
        result = addClause(bq,result, BooleanClause.Occur.SHOULD);
      }

      oq.add(bq, BooleanClause.Occur.SHOULD);
      } // outer

      CountingHitCollector hc = new CountingHitCollector();
      s.search(oq, hc);
      nMatches += hc.getCount();
      ret += hc.getSum();
    }
    System.out.println("Average number of matches="+(nMatches/iter));
    return ret;
  }

  
  
  int bigIter=10;

  public void testConjunctionPerf() throws Exception {
    createDummySearcher();
   
    sets=randBitSets(32,1000000);
    for (int i=0; i<bigIter; i++) {
      long start = System.currentTimeMillis();
      doConjunctions(500,6);
      long end = System.currentTimeMillis();
      System.out.println("milliseconds="+(end-start));
    }
    s.close();
  }

  public void testNestedConjunctionPerf() throws Exception {
    createDummySearcher();
    sets=randBitSets(32,1000000);
    for (int i=0; i<bigIter; i++) {
      long start = System.currentTimeMillis();
      doNestedConjunctions(500,3,3);
      long end = System.currentTimeMillis();
      System.out.println("milliseconds="+(end-start));
    }
    s.close();
  }

  
  public void testDisjunctionPerf() throws Exception {
    createDummySearcher();
    sets = randBitSets(32,1000000);
    for (int i=0; i<bigIter; i++) {
      long start = System.currentTimeMillis();
      doDisjunctions(500,6);
      long end = System.currentTimeMillis();
      System.out.println("milliseconds="+(end-start));
    }
    s.close();
  }

  public void testNestedDisjunctionPerf() throws Exception {
    createDummySearcher();
    sets=randBitSets(32,1000000);
    for (int i=0; i<bigIter; i++) {
      long start = System.currentTimeMillis();
      doNestedDisjunctions(500,3,3);
      long end = System.currentTimeMillis();
      System.out.println("milliseconds="+(end-start));
    }
    s.close();
  }
  
  
}
