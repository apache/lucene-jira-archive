package org.apache.lucene.search;

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

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.lucene.store.RAMDirectory;

import org.apache.lucene.index.IndexWriter;

import org.apache.lucene.analysis.WhitespaceAnalyzer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;

/** Test performance of scorers implementing a disjunction on 
 * other scorers.
 * This is where the 1.4 "good old" BooleanScorer shines.
 * This test is made to see how close the DisjunctionSumScorer
 * used by BooleanScorer2 can get there.
 * Along the way the new option to BooleanScorer to allow skipTo
 * is exercised.
 */
public class TestDisjunctionPerf1 extends TestCase {
  
  /** A scorer that matches all docs having a document number
   * that is a positive multiple of a given interval, up to a maximum.
   */
  private class NScorer extends Scorer {
    private final int interval;
    private final int maxDoc;
    private final float docScore;
    private int currentDoc;
    
    NScorer(int interval, int maxDoc, float docScore) {
      super(null);
      this.currentDoc = 0;
      this.interval = interval;
      this.maxDoc = maxDoc;
      this.docScore = docScore;
    }
    
    public int doc() {return currentDoc;}
    
    public float score() {return docScore;}
    
    public boolean next() {
      currentDoc += interval;
      return currentDoc <= maxDoc;
    }
    
    public boolean skipTo(int target) { // unused
      if (target <= currentDoc) {
        target = currentDoc + 1;
      }
      int below = (target / interval) * interval;
      if (below < target) {
        target = below + interval;
      } // else below == target
      currentDoc = target;
      return currentDoc <= maxDoc;
    }
    
    public Explanation explain(int docNr) {
      throw new UnsupportedOperationException();
    }
  }
  
  private class Timer {
    Timer() { start(); }
    private long msecStart;
    private long getMSecs() { return System.currentTimeMillis(); }
    void start() { msecStart = getMSecs(); }
    long mSecs() { return getMSecs() - msecStart; }
  }

  private Timer timer = new Timer();
  public void setUp() throws Exception {
    timer.start();
  }
  
  public void tearDown() throws Exception {
    System.out.println("Total " + timer.mSecs() + " msecs for test " + getName());
  }

  void addDisjunctionScorers(Scorer bs, int[] intervals, int maxDoc) {
    for (int i = 0; i < intervals.length; i++) {
      Scorer si = new NScorer(intervals[i], maxDoc, 1.0f / (float) intervals[i]);
      if (bs instanceof BooleanScorer) {
        try {
          ((BooleanScorer)bs).add(si, false, false); // not required, not prohibited
        } catch (IOException ioe) {
          throw new Error(ioe.toString());
        }
      } else if (bs instanceof BooleanScorer2) {
        ((BooleanScorer2)bs).add(si, BooleanClause.Occur.SHOULD);
      } else {
        throw new IllegalArgumentException(bs.toString());
      }
    }
  }
  
  void doTimedScoring(Scorer bs, String mes) {
    System.out.println("Start scoring on " + mes);
    final int[] nrMatches = new int[1];
    nrMatches[0] = 0;
    Timer t = new Timer();
    try {
      bs.score(new HitCollector() {
       public void collect(int doc, float score) {
         nrMatches[0]++;
         //System.out.println("Collecting match: doc " + doc + ", score " + score); 
       }
      });
    } catch (IOException ioe) {
      throw new Error(ioe.toString());
    }
    System.out.println(t.mSecs() + " msecs, " + nrMatches[0] + " matches for " + mes);
    System.out.println();
  }
  
  void tstDisjunctionScorer(
      Scorer bs,
      int[] intervals,
      int maxDoc,
      String mes) {
    addDisjunctionScorers(bs, intervals, maxDoc);
    doTimedScoring(bs, mes);
  }
  
  void tstDisjunctionScorers(int[] intervals, int maxDoc) {
    BooleanScorer b1 = new BooleanScorer(new DefaultSimilarity());
    tstDisjunctionScorer(b1, intervals, maxDoc, "BooleanScorer without skipTo");

    BooleanScorer b2 = new BooleanScorer(new DefaultSimilarity());
    b2.allowSkipTo(); // 1.4 scorer with skipTo
    tstDisjunctionScorer(b2, intervals, maxDoc, "BooleanScorer with skipTo");
    
    tstDisjunctionScorer(new BooleanScorer2(), intervals, maxDoc, "BooleanScorer2");
  }
  
  public void testPerf01() {
    int maxDoc = 10000000;
  // Reported times are without -server and -Xbatch, and with junit fork="yes" and forkmode="once".
  // With -server and -Xbatch the reported times roughly half again after sufficient running.
  // 1st time column is for BooleanScorer without skipTo()
  // 2nd time column is for BooleanScorer with skipTo(), but not enough sorting is done, this has been corrected, see below.
  // 3rd time column is for DisjunctionSumScorer/ScorerDocQueue of 26 March 2005
  // 4th time column is for DisjunctionSumScorer current of 26 March 2005
  //int[] intervals = {3,7,5,11};                   // 645, 644, 1219, 1842 msecs, 5844156 matches
  //int[] intervals = {29, 23, 31, 37, 13, 17, 19}; // 277, 277,  658,  985 msecs, 2842799 matches
  //int[] intervals = {29, 23, 31, 37, 19, 39, 41}; // 209, 209,  464,  724 msecs, 2168785 matches
  //int[] intervals = {29, 47, 31, 37, 43, 39, 41}; // 162, 161,  372,  571 msecs, 1738552 matches
  //int[] intervals = {29, 47};                     //  48,  49,   84,  107 msecs,  550256 matches
  //int[] intervals = {61, 53};                     //  30,  31,   54,   69 msecs,  349520 matches
  //int[] intervals = {11, 7};                      // 199, 200,  365,  442 msecs, 2207791 matches
  
  // Corrected BooleanScorer to sort more often when skipTo is to be used.
  // Added -server and -Xbatch to the 1.5 jvm (-server and -Xbatch roughly half the time of the last repition).
  // Not reporting the slower current DisjunctionSumScorer (missing 4th timing column):
  //int[] intervals = {3,7,5,11};                   // 319, 1725, 653 msecs, 5844156 matches
  //int[] intervals = {29, 23, 31, 37, 13, 17, 19}; // 148,  819, 314 msecs, 2842799 matches
  //int[] intervals = {29, 23, 31, 37, 19, 39, 41}; // 100,  593, 240 msecs, 2168785 matches
  //int[] intervals = {29, 47, 31, 37, 43, 39, 41}; //  83,  472, 167 msecs, 1738552 matches
  //int[] intervals = {29, 47};                     //  26,  123,  31 msecs,  550256 matches
    int[] intervals = {61, 53};                     //  15,   82,  22 msecs,  349520 matches
  //int[] intervals = {11, 7};                      // 103,  573, 166 msecs, 2207791 matches
    
  // With 20 intervals, ie. subscorers:
  //int[] intervals = {29, 47, 31, 37, 43, 39, 41, 61, 53, 17, 11, 71, 73, 79, 83, 91, 97, 101, 103, 107};
                                                    // 196, 1178, 521 msecs, 3827527 matches

    for (int reps = 0; reps < 10; reps++) {
      System.out.println("Test nr " + (reps + 1));
      tstDisjunctionScorers(intervals, maxDoc);
      System.out.println();
    }
  }
  // Conclusions (26 March 2005):
  // - DisjunctionScorer/ScorerDocQueue takes about 65% to 75% of the time of the current DisjunctionScorer.
  // - DisjunctionScorer/ScorerDocQueue has 1.5 to 2.5 times more overhead than BooleanScorer without skipTo()
  //   on top level disjunctions.
  // - BooleanScorer with skipTo() allowed needs work on the sorting: this can probably be done faster.
  // - A 1.5 Sun JVM with -server and -Xbatch is about twice as fast on these tests as without these options.
}
