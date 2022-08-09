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
package org.apache.lucene.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.LuceneTestCase;

public class TestMinShouldMatchSpan extends LuceneTestCase {

  private static final List<String> terms = Arrays.asList("a","b","c","d", "e", "f"
      );
  int minShouldMatch=3;

  public void test() throws IOException {
    
    Directory dir = newDirectory();
    RandomIndexWriter w = new RandomIndexWriter(random(), dir);
    int hits = 0;
    
    List<String> terms2 = terms;
    for(int i=0;i<10;i++) {
      int termNum = random().nextInt(terms2.size());
      String t = terms2.get(termNum);
      int sequenceLength=0;
      boolean hit = false;
      StringBuilder term = new StringBuilder(t);
      for(int wNum=0; wNum<terms2.size()-1 /*or more*/; wNum++) {
        int nextTermNum = (termNum + 1 +random().nextInt(terms2.size()-1))
                          % terms2.size();
        assert nextTermNum!=termNum;
        if (nextTermNum>termNum) {
          sequenceLength++;
          if (sequenceLength+1 == minShouldMatch) {
            hit=true;
          }
        } else {
          sequenceLength = 0;
        }
        term.append(" ");
        term.append(terms2.get(nextTermNum));
        termNum = nextTermNum;
      }
      Document doc = new Document();
      doc.add(newTextField("field", 
          term.toString(), Field.Store.YES));
      w.addDocument(doc);
      if (hit) {
        hits++;
        System.out.println(term.toString());
      } else {
        System.out.println(term.toString()+" DOESN'T match");
      }
    }

    DirectoryReader reader = w.getReader();
    IndexSearcher searcher = newSearcher(reader);
    {
      TermAutomatonQuery automat =
          // treeTraversalGenerator(terms2, minShouldMatch);
          dynamicGenerator(terms2, minShouldMatch);
      System.out.println(automat.toDot());


      TopDocs result = searcher.search(automat, reader.maxDoc());
      System.out.println();
      for (ScoreDoc r : result.scoreDocs) {
        System.out.println(searcher.doc(r.doc).get("field"));
      }

      assertEquals(hits, result.totalHits);
    }
    {
      List<String> star = repeat(terms2, minShouldMatch);
      TermAutomatonQuery withRepetitions = dynamicGenerator(star, minShouldMatch);
      System.out.println(withRepetitions.toDot());

      TopDocs searchResult = searcher.search(withRepetitions, reader.maxDoc());
      assertEquals(hits, searchResult.totalHits);
      reader.close();
      w.deleteAll();

      {
        Document doc = new Document();
        doc.add(newTextField("field",
            "x a a a ", Field.Store.YES));
        w.addDocument(doc);
      }
      
      {
        Document doc = new Document();
        doc.add(newTextField("field",
            "e b a a ", Field.Store.YES));
        w.addDocument(doc);
      }
      
      reader = w.getReader();
      searcher = newSearcher(reader);

      TopDocs result = searcher.search(withRepetitions, reader.maxDoc());

      assertEquals(1, result.totalHits);
    }
    
    w.close();
    reader.close();
    dir.close();
  }

  private static List<String> repeat(List<String> in, int times){
    ArrayList<String> res = new ArrayList<>(in.size()*times);
    for (String s : in) {
      for (int i = 0; i < times; i++) {
        res.add(s);
      }
    }
    
    return  res;
  }
  
  private TermAutomatonQuery treeTraversalGenerator(List<String> terms2, int minShouldMatch) {
    TermAutomatonQuery automat = new TermAutomatonQuery("field");
    int init = automat.createState();
    generate(automat, init, terms2, minShouldMatch);
    automat.finish();
    return automat;
  }
  
  public void generate(TermAutomatonQuery q,int prevState, 
                      List<String> terms, int minShouldMatch) {
    if (minShouldMatch==0){
      q.setAccept(prevState, true);
      return;
    }
    if (minShouldMatch>terms.size()) {
      return;
    }
    
    String head = terms.get(0);
    int afterHead = q.createState();
    q.addTransition(prevState, afterHead, new BytesRef(head));
    List<String> tail = terms.subList(1, terms.size());
    generate(q, afterHead, tail, minShouldMatch-1);
    generate(q, prevState, tail, minShouldMatch);
  }
  
  private TermAutomatonQuery dynamicGenerator(List<String> terms2, int minShouldMatch) {
    TermAutomatonQuery automat = new TermAutomatonQuery("field");
    int init = automat.createState();
    
    State r = new State();
    r.terms = terms2;
    r.minShouldMatch = minShouldMatch;
    
    Map<State, Integer> seen = new HashMap<>();
    LinkedList<State> seed = new LinkedList<>();
    seed.add(r);
    seen.put(r, init);
    
    State s = null;
    for(; !seed.isEmpty(); ) {
      s = seed.pop();
      Integer incomingState = seen.get(s);
      do{
        String matchingTerm = s.peekTransitionTerm();
        State match = s.createMatch();
        Integer seenState = seen.get(match);
        if (seenState!=null) {
          automat.addTransition(incomingState,
              seenState, matchingTerm);
        } else {
          int me = automat.createState();
          automat.addTransition(incomingState,
                              me, matchingTerm);
          seen.put(match, me);
          if (match.minShouldMatch==0) {
            automat.setAccept(me, true);
          } else { 
            seed.add(match);
          }
        }
      }while((s = s.doesntMatch())!=null);
    }
    
    automat.finish();
    return automat;
  }
  
  static class State{
    
    List<String> terms;
    int minShouldMatch;
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + minShouldMatch;
      result = prime * result + ((terms == null) ? 0 : terms.hashCode());
      return result;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      State other = (State) obj;
      if (minShouldMatch != other.minShouldMatch) return false;
      if (terms == null) {
        if (other.terms != null) return false;
      } else if (!terms.equals(other.terms)) return false;
      return true;
    }
    
    
    @Override
    public String toString() {
      return "" + terms + "/" + minShouldMatch;
    }

    String peekTransitionTerm(){
      return terms.get(0);
    }
    
    State createMatch() {
      State matchingState = new State();
      matchingState.minShouldMatch = minShouldMatch -1;
      matchingState.terms = matchingState.minShouldMatch==0 //if it's win, we don't care about remaining tail 
          ? Collections.emptyList():
              terms.subList(1, terms.size());
      return matchingState;
    }
    
    
    State doesntMatch() {
      if (minShouldMatch == terms.size()) {
        return null;
      }
      
      State notMatchingState = new State();
      notMatchingState.terms = terms.subList(1, terms.size());
      notMatchingState.minShouldMatch = minShouldMatch;
      return notMatchingState;
    }
    
  }
}
