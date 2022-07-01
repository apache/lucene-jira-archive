/*
 * Created on Jul 16, 2005
 */
package org.apache.lucene.search;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.RAMDirectory;

import junit.framework.TestCase;

public class TestMultiPhraseQuery2 extends TestCase {

    private RAMDirectory indexStore;

    protected void setUp() throws Exception {
        indexStore = new RAMDirectory();
        IndexWriter writer = new IndexWriter(indexStore, new StandardAnalyzer(new String[]{}), true);
        add("This is a test","object",writer);
        add("a note","note",writer);
        writer.close();
    }

    protected void tearDown() throws Exception {
    }
    
    public void testPhrasePrefixWithBooleanQuery() throws IOException {
        IndexSearcher searcher = new IndexSearcher(indexStore);
        
        // This query will be equivalent to +type:note +body:"a t*"
        BooleanQuery q = new BooleanQuery();
        q.add(new TermQuery(new Term("type", "note")), BooleanClause.Occur.MUST);

        MultiPhraseQuery trouble = new MultiPhraseQuery();
        trouble.add(new Term("body", "a"));
        trouble.add(new Term[] { new Term("body", "test"),
                                 new Term("body", "this")});
        q.add(trouble, BooleanClause.Occur.MUST);
        
        // exception will be thrown here without fix
        Hits hits = searcher.search(q);

        assertEquals("Wrong number of hits", 0, hits.length());
        searcher.close();
    }
    
    private void add(String s, String type, IndexWriter writer) throws IOException {
      Document doc = new Document();
      doc.add(new Field("body", s, Field.Store.YES, Field.Index.TOKENIZED));
      doc.add(new Field("type", type, Field.Store.YES, Field.Index.UN_TOKENIZED));
      writer.addDocument(doc);
    }
}
