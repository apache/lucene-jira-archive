package org.apache.lucene.search;

import junit.framework.TestCase;

import java.io.IOException;

import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.RAMDirectory;

import org.apache.lucene.search.spans.*;

/**
 * Tests a sloppy ordered span query against a document with an unordered span
 * within a sloppy ordered span.
 * <br>Derived from TestBasic.java.
 */
public class TestBasics2 extends TestCase {
  private IndexSearcher searcher;

  public void setUp() throws Exception {
    RAMDirectory directory = new RAMDirectory();
    IndexWriter writer
      = new IndexWriter(directory, new WhitespaceAnalyzer(), true);
    //writer.infoStream = System.out;
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < docFields.length; i++) {
      Document doc = new Document();
      doc.add(Field.Text(field, docFields[i]));
      writer.addDocument(doc);
    }
    writer.close();
    searcher = new IndexSearcher(directory);
  }
  
  private String[] docFields = {
    "w1 w2 w3 w4 w5",
    "w1 w3 w2 w3",
    ""
  };
  
  public final String field = "field";
  
  public Term makeTerm(String text) {return new Term(field, text);}
  
  public SpanTermQuery makeSpanTermQuery(String text) {
    return new SpanTermQuery(makeTerm(text));
  }

  public void testSpanNearOrdered02() throws Exception {
    SpanTermQuery w1 = makeSpanTermQuery("w1");
    SpanTermQuery w2 = makeSpanTermQuery("w2");
    SpanTermQuery w3 = makeSpanTermQuery("w3");
    int slop = 0;
    boolean ordered = true;
    SpanNearQuery snq = new SpanNearQuery( new SpanQuery[]{w1,w2,w3}, slop, ordered);
    checkHits(snq, new int[] {0});
  }

  public void testSpanNearOrdered03() throws Exception {
    SpanTermQuery w1 = makeSpanTermQuery("w1");
    SpanTermQuery w2 = makeSpanTermQuery("w2");
    SpanTermQuery w3 = makeSpanTermQuery("w3");
    int slop = 1;
    boolean ordered = true;
    SpanNearQuery snq = new SpanNearQuery( new SpanQuery[]{w1,w2,w3}, slop, ordered);
    checkHits(snq, new int[] {0,1});
  }

  private void checkHits(Query query, int[] results) throws IOException {
    Hits hits = searcher.search(query);

    Set correct = new TreeSet();
    for (int i = 0; i < results.length; i++) {
      correct.add(new Integer(results[i]));
    }

    Set actual = new TreeSet();
    for (int i = 0; i < hits.length(); i++) {
      actual.add(new Integer(hits.id(i)));
    }

    assertEquals(query.toString(field), correct, actual);
  }
}
