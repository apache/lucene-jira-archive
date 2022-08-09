package sample;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.Scorer.ScorerVisitor;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class Test2LUCENE2590 {
  
  static Analyzer analyzer = new WhitespaceAnalyzer( Version.LUCENE_31 );
  static Directory dir = new RAMDirectory();
  static final String F1 = "title";
  static final String F2 = "body";

  public static void main(String[] args) throws IOException {
    makeIndex();
    searchIndex();
  }

  static void makeIndex() throws IOException {
    IndexWriterConfig config = new IndexWriterConfig( Version.LUCENE_31, analyzer );
    IndexWriter writer = new IndexWriter( dir, config );
    //writer.addDocument( doc( "lucene", "lucene is a very popular search engine library. lucene runs overall in the world. lucene is great!" ) );
    writer.addDocument( doc( "lucene", "lucene is a very popular search engine library" ) );
    writer.addDocument( doc( "solr", "solr is a very popular search server and is using lucene" ) );
    writer.addDocument( doc( "nutch", "nutch is an internet search engine with web crawler and is using lucene and hadoop" ) );
    writer.close();
  }
  
  static Document doc( String v1, String v2 ){
    Document doc = new Document();
    if( v1 != null )
      doc.add( field( F1, v1 ) );
    if( v2 != null )
      doc.add( field( F2, v2 ) );
    return doc;
  }

  static Fieldable field( String field, String value ){
    return new Field( field, value, Store.YES, Index.ANALYZED );
  }
  
  static void searchIndex() throws IOException {
    IndexSearcher searcher = new IndexSearcher( dir );
    printResult( searcher, query( new Term( F1, "lucene"), new Term( F2, "lucene" ), new Term( F2, "search" ) ) );
    searcher.close();
  }
  
  static Query query( Term... ts ){
    if( ts == null || ts.length == 0 ){
      throw new IllegalArgumentException();
    }
    if( ts.length == 1 )
      return new TermQuery( ts[0] );
    BooleanQuery bq = new BooleanQuery();
    for( Term t : ts ){
      bq.add( new TermQuery( t ), Occur.SHOULD );
    }
    return bq;
  }
  
  static void printResult( IndexSearcher searcher, Query query ) throws IOException {
    MyCollector collector = new MyCollector();
    searcher.search( query, collector );
    TopDocs docs = collector.topDocs();
    for( ScoreDoc scoreDoc : docs.scoreDocs ){
      Document doc = searcher.doc( scoreDoc.doc );
      float score = scoreDoc.score;
      System.out.println( score + " : " + doc.get( F1 ) + " / " + doc.get( F2 ) );
      System.out.println( "  freq : " + collector.freq( scoreDoc.doc) );
    }
  }
  
  static class MyCollector extends Collector {
    
    private TopDocsCollector<ScoreDoc> collector;
    private int docBase;

    public final Map<Integer,Integer> docCounts = new HashMap<Integer,Integer>();

    private final Set<TermQueryScorer> tqsSet = new HashSet<TermQueryScorer>();
    private final ScorerVisitor<Query, Query, Scorer> visitor = new MockScorerVisitor();
    private final EnumSet<Occur> collect;
    
    MyCollector(){
      collector = TopScoreDocCollector.create( 10, true );
      collect = EnumSet.allOf( Occur.class );
    }

    @Override
    public boolean acceptsDocsOutOfOrder() {
      return false;
    }

    @Override
    public void collect(int doc) throws IOException {
      int freq = 0;
      for( TermQueryScorer tqs : tqsSet ){
        Scorer scorer = tqs.scorer;
        int matchId = scorer.docID();
        if( matchId == doc ){
          freq += scorer.freq();
        }
      }
      docCounts.put(doc + docBase, freq);
      collector.collect(doc);
    }

    @Override
    public void setNextReader(IndexReader reader, int docBase)
        throws IOException {
      this.docBase = docBase;
      collector.setNextReader( reader, docBase );
    }

    @Override
    public void setScorer(Scorer scorer) throws IOException {
      collector.setScorer( scorer );
      scorer.visitScorers( visitor );
    }
    
    public TopDocs topDocs(){
      return collector.topDocs();
    }
    
    public int freq( int doc ) throws IOException {
      return docCounts.get( doc );
    }

    private class MockScorerVisitor extends ScorerVisitor<Query, Query, Scorer> {

      @Override
      public void visitOptional(Query parent, Query child, Scorer scorer) {
        if (collect.contains(Occur.SHOULD) && child instanceof TermQuery)
          tqsSet.add( new TermQueryScorer( (TermQuery)child, scorer ) );
      }

      @Override
      public void visitProhibited(Query parent, Query child, Scorer scorer) {
        if (collect.contains(Occur.MUST_NOT) && child instanceof TermQuery)
          tqsSet.add( new TermQueryScorer( (TermQuery)child, scorer ) );
      }

      @Override
      public void visitRequired(Query parent, Query child, Scorer scorer) {
        if (collect.contains(Occur.MUST) && child instanceof TermQuery)
          tqsSet.add( new TermQueryScorer( (TermQuery)child, scorer ) );
      }
    }
    
    private static class TermQueryScorer {
      private TermQuery query;
      private Scorer scorer;
      public TermQueryScorer( TermQuery query, Scorer scorer ){
        this.query = query;
        this.scorer = scorer;
      }
    }
  }
}
