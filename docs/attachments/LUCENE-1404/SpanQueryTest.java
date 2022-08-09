package com.attivio.lucene;

import org.junit.Test;
import org.junit.Assert;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.*;
import org.apache.lucene.search.spans.*;
import org.apache.lucene.store.*;
import org.apache.lucene.index.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * Test broken payload handling for SpanNearQuery.
 */
public class SpanQueryTest {
  private final Analyzer analyzer = new StandardAnalyzer(new String[0]);

  private void add(IndexWriter writer, String id, String text) throws IOException {
    final Document doc = new Document();
    doc.add( new Field("id", id, Field.Store.YES, Field.Index.UN_TOKENIZED) );
    doc.add( new Field("text", text, Field.Store.YES, Field.Index.TOKENIZED) );
    writer.addDocument(doc, analyzer);
  }


  @Test
  public void test() throws Exception {
    final Directory directory = new RAMDirectory();
    final IndexWriter writer = new IndexWriter(directory, analyzer, true);

    // Add documents
    add(writer, "1", "the big dogs went running to the market");
    add(writer, "2", "the cat chased the mouse, then the cat ate the mouse quickly");
    
    // Commit
    writer.close();
    
    // Get searcher
    final IndexReader reader = IndexReader.open( directory );
    final IndexSearcher searcher = new IndexSearcher(reader);

    // Dump the indexed terms (debugging)
    final TermEnum termEnum = reader.terms();      
    try {
      do {
        final Term term = termEnum.term();
        if (term != null) {
          System.err.println(term);
        }
      } while (termEnum.next());
    } finally {
      termEnum.close();
    }

    // Control (make sure docs indexed)
    Assert.assertEquals( 2, searcher.search( new TermQuery( new Term("text", "the") ) ).length() );
    Assert.assertEquals( 1, searcher.search( new TermQuery( new Term("text", "cat") ) ).length() );
    Assert.assertEquals( 1, searcher.search( new TermQuery( new Term("text", "dogs") ) ).length() );
    Assert.assertEquals( 0, searcher.search( new TermQuery( new Term("text", "rabbit") ) ).length() );
    
    // This throws exception (it shouldn't)
    assertHitCount( searcher, 
                    createSpan(0, true,                                 
                               createSpan(4, false, "chased", "cat" ),
                               createSpan("ate") ),
                    1 );
  }

  public void assertHitCount(IndexSearcher searcher, Query query, int count) throws IOException {
    Assert.assertEquals(count, search(searcher, query));  
  }

  public int search(IndexSearcher searcher, Query query) throws IOException {
    return searcher.search( query ).length(); 
  }

  public SpanQuery createSpan(String value) {
    return new SpanTermQuery( new Term("text", value) );
  }                     
  
  public SpanQuery createSpan(int slop, boolean ordered, SpanQuery... clauses) {
    return new SpanNearQuery(clauses, slop, ordered);
  }

  public SpanQuery createSpan(int slop, boolean ordered, String... terms) {
    SpanQuery[] clauses = new SpanQuery[ terms.length ];
    for (int i = 0; i < terms.length; ++i) {
      clauses[i] = createSpan(terms[i]);
    }
    return createSpan(slop, ordered, clauses);
  }
}