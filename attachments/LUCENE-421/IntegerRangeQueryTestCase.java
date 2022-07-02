/*
 * Created on 15 avr. 2005
 */
package opsys.lucene.test.search;

import java.io.IOException;

import junit.framework.TestCase;
import opsys.lucene.server.search.IntegerRangeQuery;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.RangeQuery;
import org.apache.lucene.store.RAMDirectory;

public class IntegerRangeQueryTestCase extends TestCase {
    
    private RAMDirectory directory;
    private IndexSearcher searcher;
    
    public void setUp() throws Exception {
        directory = new RAMDirectory();
        
        IndexWriter writer = new IndexWriter(directory, new WhitespaceAnalyzer(), true);
        Document doc1 = new Document();
        doc1.add(new Field("date", "1980", Field.Store.YES, Field.Index.TOKENIZED));
        writer.addDocument(doc1);
        
        Document doc2 = new Document();
        doc2.add(new Field("date", "1982", Field.Store.YES, Field.Index.TOKENIZED));
        writer.addDocument(doc2);
        
        Document doc3 = new Document();
        doc3.add(new Field("date", "1984", Field.Store.YES, Field.Index.TOKENIZED));
        writer.addDocument(doc3);
        writer.close();
        
        searcher = new IndexSearcher(directory);
    }
    
    public void tearDown() throws Exception {
        searcher.close();
    }
    
    /**
     * @throws IOException
     */
    public void testIntegerRangeQuery() throws IOException {
        
        IndexReader ir = IndexReader.open(directory);
        TermEnum te = ir.terms();
        int i = 0;
        while (te.next()) {
            i++;
            System.out.println("term = " + te.term());
        }
        assertEquals(3, i);
        

        RangeQuery q1 = new RangeQuery(new Term("date", "1981"), new Term("date","1983"), true );
        System.out.println("RangeQuery q = " + q1);
        Hits hits = searcher.search(q1);
        for (int j=0; j<hits.length();j++ ) {
            System.out.println(hits.doc(j).toString());
        }
        assertEquals(1, hits.length());
       
        IntegerRangeQuery q2 = new IntegerRangeQuery("date", 1981, 1983, true);
        System.out.println("IntegerRangeQuery q = " + q2);
        hits = searcher.search(q2);
        for (int j=0; j<hits.length();j++ ) {
            System.out.println(hits.doc(j).toString());
        }
        assertEquals(1, hits.length());
    }
    
}
