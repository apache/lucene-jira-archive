package org.apache.lucene.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util._TestUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import com.carrotsearch.randomizedtesting.annotations.Repeat;

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
@Repeat(iterations=1)//000)
public class TestSlowQuery extends LuceneTestCase {
  
  static final class SampleSlowQuery extends SlowQuery {
    private final Term checkingTerm;
    
    SampleSlowQuery(BooleanQuery coreQ, Term checkingTerm) {
      super(coreQ);
      this.checkingTerm = checkingTerm;
    }
    
    @Override
    protected Weight createSlowQueryWeight(final IndexSearcher searcher,
        Weight coreWeight) {
      return new SlowQueryWeight(coreWeight){

        protected SlowQueryScorer createSlowQueryScorer(final AtomicReaderContext context,
            Bits acceptDocs , Scorer coreScorer) {
          return new SlowQueryScorer(this, coreScorer){
            @Override
            protected boolean confirm(int doc) throws IOException {
              int d = random().nextBoolean() ? doc : this.coreScorer.docID();
              return searcher.doc(d+context.docBase).get(checkingTerm.field()).equals(checkingTerm.text());
            }
            
          };
        }};
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result
          + ((checkingTerm == null) ? 0 : checkingTerm.hashCode())
           + ((coreQuery == null) ? 0 : coreQuery.hashCode())
           + Float.floatToIntBits(getBoost());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      SampleSlowQuery other = (SampleSlowQuery) obj;
      if (checkingTerm == null) {
        if (other.checkingTerm != null) return false;
      } else if (!checkingTerm.equals(other.checkingTerm)) return false;
      if (coreQuery == null) {
        if (other.coreQuery != null) return false;
      } else if (!coreQuery.equals(other.coreQuery)) return false;
      if(getBoost()!=other.getBoost()) return false;
      return true;
    }
    
  }

  static final class SlowAwareFilteredQuery extends FilteredQuery {
    SlowAwareFilteredQuery(Query query, Filter filter, FilterStrategy strategy) {
      super(query, filter, strategy);
    }

    @Override
    public Query rewrite(IndexReader reader) throws IOException {
      return SlowQuery.rewriteFilteredQuery(reader, (FilteredQuery)super.rewrite(reader));
    }
  }

  static abstract class SlowQuery extends Query {
    abstract class SlowQueryWeight extends Weight {
      abstract class SlowQueryScorer extends Scorer {
        protected final Scorer coreScorer;
        private final Scorer intermidFilterScorer;
        
        SlowQueryScorer(Weight weight, Scorer childScorer) {
          super(weight);
          assert childScorer!=null;
          final Collection<ChildScorer> subScorers = childScorer.getChildren();
          if(subScorers.size()==1){
            final ChildScorer subS = subScorers.iterator().next();
            if(subS.relationship.equals("FILTERED")){
              coreScorer = subS.child;
              intermidFilterScorer = childScorer;
              return;
            }
          }
          this.coreScorer = childScorer;
          this.intermidFilterScorer = childScorer;
        }
        
        @Override
        public float score() throws IOException {
          return coreScorer.score();
        }
        
        @Override
        public int freq() throws IOException {
          return coreScorer.freq();
        }
        
        @Override
        public final int docID() {
          return coreScorer.docID();
        }
        
        @Override
        public final int nextDoc() throws IOException {
          int d ;
          d = intermidFilterScorer.nextDoc();
          assert coreScorer.docID() == intermidFilterScorer.docID();
          while(d!=NO_MORE_DOCS && !confirm(d)){
            assert coreScorer.docID() == intermidFilterScorer.docID();
            d = intermidFilterScorer.nextDoc();
          }
          return d;
        }
        
        protected abstract boolean confirm(int d) throws IOException ;

        @Override
        public final int advance(int target) throws IOException {
          throw new UnsupportedOperationException(this + " doesn't support advancing");
        }
        
        @Override
        public long cost() {
          return coreScorer.cost();
        }
      }

      protected final Weight coreWeight;
      
      SlowQueryWeight(Weight coreWeight) {
        this.coreWeight = coreWeight;
      }
      
      @Override
      public final Scorer  scorer(AtomicReaderContext context, boolean scoreDocsInOrder,
          boolean topScorer, Bits acceptDocs) throws IOException {
        assert topScorer:"it can be relaxed optionally";
        // you are like a top, but always score in order to let me confirm the match (otherwise BooleanScorer mess it up)
        Scorer coreScorer = this.coreWeight.scorer(context, true, true, acceptDocs);
        if(coreScorer==null){
          return null;
        }
        return createSlowQueryScorer(context, acceptDocs, coreScorer);
      };
      
      protected abstract Scorer createSlowQueryScorer(AtomicReaderContext context,
          Bits acceptDocs, Scorer coreScorer);

      @Override
      public void normalize(float norm, float topLevelBoost) {
        coreWeight.normalize(norm, topLevelBoost);
      }
      
      @Override
      public float getValueForNormalization() throws IOException {
        return coreWeight.getValueForNormalization();
      }
      
      @Override
      public Query getQuery() {
        return SlowQuery.this;
      }
      
      @Override
      public Explanation explain(AtomicReaderContext context, int doc)
          throws IOException {
        return coreWeight.explain(context, doc);
      }
      
    }

    protected Query coreQuery;
    
    public static Query rewriteFilteredQuery(IndexReader reader, FilteredQuery fq) throws IOException{
      if((fq.getQuery() instanceof SlowQuery)){
        //throw new IllegalArgumentException("query should be "+SlowQuery.class.getCanonicalName()+" subclass, however got "+fq.getQuery());
        final SlowQuery c = (SlowQuery) fq.getQuery().clone();
        c.coreQuery = new FilteredQuery(c.coreQuery, fq.getFilter(), fq.getFilterStrategy());
        return c;
      } else {
        return fq;
      }
    }
    
    protected SlowQuery(BooleanQuery coreQ) {
      this.coreQuery = coreQ;
    }
    
    /**
     * start from the code:
     *  {
     *       final Weight coreWeight = coreQ.createWeight(searcher);
     *        return new YourSlowQueryWeight(coreWeight);
     *   }
     * */
    @Override
    public final Weight createWeight(IndexSearcher searcher) throws IOException {
      final Weight coreWeight = this.coreQuery.createWeight(searcher);
      return createSlowQueryWeight(searcher, coreWeight);
    }
    
    protected abstract Weight createSlowQueryWeight(IndexSearcher searcher,
        Weight coreWeight) ;

    @Override
    public String toString(String field) {
      return "slow post filter for "+coreQuery;
    }

    @Override
    public abstract int hashCode() ;

    @Override
    public abstract boolean equals(Object obj) ;
    
    @Override
    public Query rewrite(IndexReader reader) throws IOException {
      final Query coreRewritten = coreQuery.rewrite(reader);
      if(coreQuery!=coreRewritten){
        final SlowQuery c = (SlowQuery) clone();
        c.coreQuery = coreRewritten;
        return c;
      }
      return super.rewrite(reader);
    }
  }

  private static IndexReader indexReader;
  private static IndexSearcher searcher;
  private static Directory directory;
  static private int  whiteListSize = 0;
  
  private int whiteListSizeExpected;
  static private Map<String, String> whiteQuery;
  private static LinkedHashMap<String,String> negativeQuery;
  
  final private static String [] field0Vals = new String[]{"a","b","c","d"};
  final private static String [] field1Vals = new String[]{"e","f","j","h"};
  final private static String [] field2Vals = new String[]{"k","l","m","n"};
  
  private Query query;
  private Filter filter;
  private Query noMatchQuery;
  private Filter noMatchFilter;

  @BeforeClass
  public static void buildIndex() throws Exception {
    directory = newDirectory();
    Analyzer indexerAnalyzer = new MockAnalyzer(random());

    IndexWriterConfig config = new IndexWriterConfig(TEST_VERSION_CURRENT, indexerAnalyzer);
    IndexWriter writer = new IndexWriter(directory, config);
    
    negativeQuery = randomDoc();
    while((whiteQuery = randomDoc()).equals(negativeQuery)){
      ;
    }
    
    for(int w=0; w<atLeast(100); w++){
      LinkedHashMap<String,String> d;
      while((d = randomDoc()).equals(negativeQuery)){
        ;
      }
      if(whiteQuery.equals(d)){
        whiteListSize++;
      }
      final Document doc = toDoc(d);
      writer.addDocument(doc);
      if(rarely()){
        for(int i=0;i<atLeast(1);i++){
         LinkedHashMap<String,String> fake = randomDoc();
         
         final ArrayList<String> keys = new ArrayList<>(fake.keySet());
         fake.remove(keys.get(random().nextInt(keys.size())));
         writer.addDocument(toDoc(fake));
        }
      }
      if(rarely()){
        writer.commit();
      }
    }
    writer.commit();
    writer.close();
    
    indexReader = DirectoryReader.open(directory);
    searcher = newSearcher(indexReader);

  }

  private static Document toDoc(final LinkedHashMap<String,String> d) {
    final Document doc = new Document();
    for(Map.Entry<String,String> e:d.entrySet()){
      doc.add(newStringField(e.getKey(), e.getValue(), Store.YES));
    }
    return doc;
  }

  private static LinkedHashMap<String,String> randomDoc() {
    return new LinkedHashMap<String,String>(){{
      int f=0;
      for(String[] vals:new String[][]{field0Vals,field1Vals,field2Vals}){
        put("field"+(f++), vals[random().nextInt(vals.length)]);
      }
    }};
  }
  
  @Before
  public void createFilterAndQuery() throws IOException{
    final Filter[] filter= new Filter[1];
    this.query = toQuery(whiteQuery, filter);
    this.filter = filter[0];
    
    whiteListSizeExpected = whiteListSize;
    
    noMatchQuery = toQuery(negativeQuery, filter);
    noMatchFilter = filter[0];
  }

  private Query toQuery(final Map<String,String> wq, final Filter[] filter) {
    filter[0]=null;
    return new BooleanQuery(){
      {
        for(Map.Entry<String,String> e: wq.entrySet()){
          final TermQuery q = new TermQuery(new Term(e.getKey(), e.getValue()));
          if(random().nextBoolean() && filter[0]==null){
            filter[0] = new QueryWrapperFilter(q);
            if(random().nextBoolean()){
              filter[0] = new CachingWrapperFilter(filter[0]);
            }
          }else{
            add(q,Occur.MUST);
          }
        }
      }};
  }
  
  public void testHappy(){
    
  }
  
  @Rule public ExpectedException thrown= ExpectedException.none();
  
  public void testNegative(){
    whiteListSizeExpected = whiteListSizeExpected*2+1;
    thrown.expect( AssertionError.class );
  }

  public void testSlow(){
    final ArrayList<BooleanClause> clauses = new ArrayList<BooleanClause>(Arrays.<BooleanClause>asList(((BooleanQuery)query).getClauses()));
    Collections.shuffle(clauses, random());
    final BooleanClause slowClause = clauses.remove(0);
    final Term checkingTerm = ((TermQuery)slowClause.getQuery()).getTerm();
    boolean booleanScorerAttempt = false;
    BooleanQuery coreQ = new BooleanQuery();
    for(BooleanClause bc : clauses){
      // let's try to enforce BS 
      if(clauses.size()==1 && (bc.getOccur()==Occur.MUST || bc.getOccur()==Occur.SHOULD) && random().nextBoolean()){
        bc.setOccur(bc.getOccur()==Occur.MUST  ? Occur.SHOULD: Occur.MUST );
        coreQ.add(bc); // prevents rewrite to TQ
        booleanScorerAttempt = true;
      }
      coreQ.add(bc);
    }
    if(!booleanScorerAttempt &&  random().nextBoolean() ){
      final BooleanQuery bsq = new BooleanQuery();
      bsq.add(coreQ, Occur.SHOULD);
      bsq.add(coreQ, Occur.SHOULD);
      coreQ = bsq;
    }
    query = new SampleSlowQuery(coreQ, checkingTerm);
  }
  
  @After
  public void checkSearch() throws IOException{
    assertNumFound(query, filter, whiteListSizeExpected);
    assertNumFound(noMatchQuery, noMatchFilter, 0);
  }

  private void assertNumFound(final Query q, final Filter f,
      final int expectToFind) throws IOException {
    int n = random().nextInt(expectToFind+1);
    TotalHitCountCollector hits = null ;
    TopScoreDocCollector scores = null ;
    Collector collector = (n==0)? 
        (hits = new TotalHitCountCollector()):
        (scores= TopScoreDocCollector.create(n, random().nextBoolean()));
    if(f!=null) {
        searcher.search(new SlowAwareFilteredQuery(q, f, _TestUtil.randomFilterStrategy(random())),collector);
    } else{
        searcher.search(q,collector);
    }
    assertTrue((hits!=null && hits.getTotalHits()==expectToFind) 
        || (scores!=null && scores.getTotalHits()==expectToFind));
  }
  
  @AfterClass
  public static void stop() throws Exception {
    indexReader.close();
    directory.close();
  }
}
