/*
 * Created on 14.07.2020
 *
 */


import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.TotalHits.Relation;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.junit.Test;

public class TestUnifiedHighlighter {
  
  
  @Test
  public void test() throws IOException {
    Analyzer analyzer = new WhitespaceAnalyzer();
    String field = "content";
    
    Directory directory = new ByteBuffersDirectory(); 
    IndexWriterConfig con = new IndexWriterConfig(analyzer);
    String[] docs = new String[]{"We need 100 thousand dollars to buy the house.","A 100 fucking dollars wasn't enough to fix it. The next sentence should not be highlighted. We need 100 thousand dollars to buy the house."};
    try (IndexWriter w = new IndexWriter(directory, con)) {
      for (String docText : docs) {
        Document doc = new Document();
        doc.add(new TextField(field, docText, Field.Store.YES));
        w.addDocument(doc);
      }
    }
    
    
    IndexReader reader = DirectoryReader.open(directory); 
    IndexSearcher searcher = new IndexSearcher(reader);
    UnifiedHighlighter highlighter = new UnifiedHighlighter(searcher,analyzer);
    
    SpanQuery tQuery1 = new SpanTermQuery(new Term(field, "100"));
    SpanQuery tQuery2 = new SpanTermQuery(new Term(field, "dollars"));
    SpanQuery tQuery3 = new SpanTermQuery(new Term(field, "thousand"));
    
    SpanQuery hundredDollars = new SpanNearQuery(new SpanQuery[] {tQuery1, tQuery2}, 1, true);
    Query highlightQuery = new SpanNotQuery(hundredDollars, tQuery3);
    
    TopDocs hits = searcher.search(highlightQuery, 10);
    TotalHits shouldFoundHits = new TotalHits(1,Relation.EQUAL_TO);
    assertEquals(shouldFoundHits, hits.totalHits);
    
    String[] fragments = highlighter.highlight(field, highlightQuery, hits, 3);
    assertEquals(1,fragments.length);
    assertEquals("A <b>100</b> fucking <b>dollars</b> wasn't enough to fix it", fragments[0]);
    
     
     
    reader.close();
  }



}
