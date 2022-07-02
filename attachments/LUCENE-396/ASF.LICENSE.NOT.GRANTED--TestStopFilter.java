package org.apache.lucene;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.lucene.store.*;
import org.apache.lucene.document.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.*;

public class TestStopFilter extends TestCase {

  private String[] docs = {
    "Indicators of Climate Change in BC",
    "new paradigm of abrupt climate change has been",
    "Adaptation the key to surviving climate change, scientists say",
    "shows contempt for global climate change efforts",
    "Creating a Climate of Change",
    "the current climate of change and uncertainly",
    "see a climate of change in the hearts and minds"
  };

  /** Main for running test case by itself. */
  public static void main(String args[]) {
    TestRunner.run(new TestSuite(TestStopFilter.class));
  }

  public void testStopFilterWithStopWords() throws Exception {

    RAMDirectory directory    = new RAMDirectory();
    StandardAnalyzer analyzer = new StandardAnalyzer();
    IndexWriter writer        = new IndexWriter(directory, analyzer, true);

    for (int j = 0; j < docs.length; j++) {
      Document d = new Document();
      d.add(new Field("field", docs[j], true, true, true));
      writer.addDocument(d);
    }

    writer.close();

    IndexSearcher searcher = new IndexSearcher(directory);

    QueryParser parser = new QueryParser("field", analyzer);
    Query query;
    Hits hits;

    /* Normal query, should return all 7 docs */
    query = parser.parse( "climate change" );
    hits  = searcher.search( query );
    assertEquals(7, hits.length() );

    /*
     * If StopFilter properly increment token position,
     * we should find only 4 docs
     */
    query = parser.parse( "\"climate change\"" );
    hits  = searcher.search( query );
    assertEquals(4, hits.length() );

    /*
     * Since stop words will be removed by anaylzer, 
     * same as 'climate change'. If you need to match
     * phrases like this exactly, then you need to 
     * include stopwords when indexing
     */
    query = parser.parse( "\"climate of change\"" );
    hits  = searcher.search( query );
    assertEquals(4, hits.length() );

    /* Sloppy */
    query = parser.parse( "\"climate change\"~2" );
    hits  = searcher.search( query );
    assertEquals(7, hits.length() );
  }

  public void testStopFilterWithoutStopWords() throws Exception {

    RAMDirectory directory    = new RAMDirectory();
    String[] stopWords        = new String[0];
    StandardAnalyzer analyzer = new StandardAnalyzer(stopWords);
    IndexWriter writer        = new IndexWriter(directory, analyzer, true);

    for (int j = 0; j < docs.length; j++) {
      Document d = new Document();
      d.add(new Field("field", docs[j], true, true, true));
      writer.addDocument(d);
    }

    writer.close();

    IndexSearcher searcher = new IndexSearcher(directory);

    QueryParser parser = new QueryParser("field", analyzer);
    Query query;
    Hits hits;

    /* Normal query, should return all 7 docs */
    query = parser.parse( "climate change" );
    hits  = searcher.search( query );
    assertEquals(7, hits.length() );

    /*
     * we should find only 4 docs
     */
    query = parser.parse( "\"climate change\"" );
    hits  = searcher.search( query );
    assertEquals(4, hits.length() );

    /*
     * Analyzer will not remove stopwords, we should * find 3 docs.
     */
    query = parser.parse( "\"climate of change\"" );
    hits  = searcher.search( query );
    assertEquals(3, hits.length() );
  }
}
