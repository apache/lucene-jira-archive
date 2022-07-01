/*
 * Created on Jul 28, 2005
 *
 */
package org.apache.lucene.analysis;

import java.io.StringReader;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import junit.framework.TestCase;

/**
 * @author skirsch
 *
 * A test class for NGramAnalyzerWrapper as regards queries and scoring.
 * 
 */
public class NGramAnalyzerWrapperTest extends TestCase {

  public IndexSearcher searcher;
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(NGramAnalyzerWrapperTest.class);
  }

  /**
   * Set up a new index in RAM with three test phrases and the supplied Analyzer.
   * 
   * @param analyzer
   * @return an indexSearcher on the test index.
   * @throws Exception
   */
  public IndexSearcher setUpSearcher(Analyzer analyzer) throws Exception {
    Directory dir = new RAMDirectory();
    IndexWriter writer = new IndexWriter(dir, analyzer, true);
    
    Document doc;
    doc = new Document();
    doc.add(Field.Text("content", "please divide this sentence into ngrams"));
    writer.addDocument(doc);
    
    doc = new Document();
    doc.add(Field.Text("content", "just another test sentence"));
    writer.addDocument(doc);
    
    doc = new Document();
    doc.add(Field.Text("content", "a sentence which contains no test"));
    writer.addDocument(doc);
    
    writer.close();
    
    return new IndexSearcher(dir);
  }
  
  protected Hits queryParsingTest(Analyzer analyzer, String qs) throws Exception {
    searcher = setUpSearcher(analyzer);
    
    QueryParser qp = new QueryParser("content", analyzer);
    
    Query q = qp.parse(qs);

    return searcher.search(q);
  }
  
  protected void compareRanks(Hits hits, int[] ranks) throws Exception {
    assertEquals(ranks.length, hits.length());
    for (int i = 0; i < ranks.length; i++) {
      assertEquals(ranks[i], hits.id(i));
    }
  }

/*
 * Will not work on an index without unigrams, since QueryParser automatically
 * tokenizes on whitespace.
 */
  public void testNGramAnalyzerWrapperQueryParsing() throws Exception {
    Hits hits = queryParsingTest(new NGramAnalyzerWrapper(new WhitespaceAnalyzer(), 2),
        "test sentence");
    int[] ranks = new int[] { 1, 2, 0 };
    compareRanks(hits, ranks);
  }

/*
 * This one fails with an exception.
 */
  public void testNGramAnalyzerWrapperPhraseQueryParsingFails() throws Exception {
    Hits hits = queryParsingTest(new NGramAnalyzerWrapper(new WhitespaceAnalyzer(), 2),
        "\"this sentence\"");
    int[] ranks = new int[] { 0 };
    compareRanks(hits, ranks);
  }

  /*
   * This one works, actually.
   */
  public void testNGramAnalyzerWrapperPhraseQueryParsing() throws Exception {
    Hits hits = queryParsingTest(new NGramAnalyzerWrapper(new WhitespaceAnalyzer(), 2),
        "\"test sentence\"");
    int[] ranks = new int[] { 1 };
    compareRanks(hits, ranks);
  }

  /*
   * Same as above, is tokenized without using the analyzer.
   */
  public void testNGramAnalyzerWrapperRequiredQueryParsing() throws Exception {
    Hits hits = queryParsingTest(new NGramAnalyzerWrapper(new WhitespaceAnalyzer(), 2),
        "+test +sentence");
    int[] ranks = new int[] { 1, 2 };
    compareRanks(hits, ranks);
  }

  /*
   * This shows how to construct a phrase query containing ngrams.
   */
  public void testNGramAnalyzerWrapperPhraseQuery() throws Exception {
    Analyzer analyzer = new NGramAnalyzerWrapper(new WhitespaceAnalyzer(), 2);
    searcher = setUpSearcher(analyzer);

    PhraseQuery q = new PhraseQuery();
    
    TokenStream ts = analyzer.tokenStream("content", new StringReader("this sentence"));
    Token token;
    int j = -1;
    while ((token = ts.next()) != null) {
      j += token.getPositionIncrement();
      q.add(new Term("content", token.termText()), j);
    }
    
    Hits hits = searcher.search(q);
    int[] ranks = new int[] { 0 };
    compareRanks(hits, ranks);
  }

  /*
   * How to construct a boolean query with ngrams. A query like this will
   * implicitly score those documents higher that contain the words in the query
   * in the right order and adjacent to each other. 
   */
  public void testNGramAnalyzerWrapperBooleanQuery() throws Exception {
    Analyzer analyzer = new NGramAnalyzerWrapper(new WhitespaceAnalyzer(), 2);
    searcher = setUpSearcher(analyzer);

    BooleanQuery q = new BooleanQuery();
    
    TokenStream ts = analyzer.tokenStream("content", new StringReader("test sentence"));
    Token token;
    while ((token = ts.next()) != null) {
      q.add(new TermQuery(new Term("content", token.termText())), false, false);
    }
    
    Hits hits = searcher.search(q);
    int[] ranks = new int[] { 1, 2, 0 };
    compareRanks(hits, ranks);
  }
}
