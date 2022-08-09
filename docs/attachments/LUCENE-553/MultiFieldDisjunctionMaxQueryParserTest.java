/*
 * MultiFieldDisjunctionMaxQueryParserTest.java
 * JUnit based test
 *
 * Created on December 14, 2005, 12:38 PM
 */

package org.apache.lucene.queryParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import junit.framework.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

/**
 *
 * @author chuck
 */
public class MultiFieldDisjunctionMaxQueryParserTest extends TestCase {

    boolean show = true;
    
    Directory index = new RAMDirectory();
    String[] defaultFields = {"title", "body"};
    float[] defaultBoosts = {5.0f, 1.0f};
    Analyzer analyzer = new StandardAnalyzer();
    IndexSearcher searcher;
    MultiFieldDisjunctionMaxQueryParser queryParser = new MultiFieldDisjunctionMaxQueryParser(defaultFields, defaultBoosts, 0.1f, analyzer);
    
    public MultiFieldDisjunctionMaxQueryParserTest(String testName) throws IOException {
        super(testName);
    }

    protected void setUp() throws Exception {
        IndexWriter writer = new IndexWriter(index, analyzer, true);
        if(show)
            System.out.println("Collection:");
        writer.addDocument(createDoc("doc1", "plum fruit", "delicious ripe plum"));
        writer.addDocument(createDoc("doc2", "fruit medley", "peach, banana, pear, cherry"));
        writer.addDocument(createDoc("doc3", "fruit", "apple, pear, plum"));
        
        writer.addDocument(createDoc("doc4", "elephant", "elephant"));
        writer.addDocument(createDoc("doc5", "elephant", "albino"));
        
        writer.addDocument(createDoc("doc6", "xxx yyy", "aaa bbb xxx"));
        writer.addDocument(createDoc("doc7", "yyy x2", "eee fff"));
        writer.addDocument(createDoc("doc8", "x2 xxx yyy", "ggg hhh"));
        if (show)
            System.out.println();
        writer.close();
        searcher = new IndexSearcher(index);
    }
    
    private Document createDoc(String uid, String title, String body) {
        Document doc = new Document();
        doc.add(new Field("uid", uid, Store.YES, Index.NO));
        doc.add(new Field("title", title, Store.YES, Index.TOKENIZED));
        doc.add(new Field("body", body, Store.YES, Index.TOKENIZED));
        if (show)
            System.out.println("  " + "uid:{" + uid + "}    title:{" + title + "}    body:{" + body + "}");
        return doc;
    }

    protected void tearDown() throws Exception {
        index.close();
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(MultiFieldDisjunctionMaxQueryParserTest.class);
        
        return suite;
    }
    

    /**
     * Test of parse method, of class org.apache.lucene.queryParser.MultiFieldDisjunctionMaxQueryParser.
     */
    public void test() {
        System.out.println("Test MultiFieldDisjunctionMaxQueryParser");
        
        try {
            List<Document> results = search(getQuery("fruit AND -plum"));
            assertEquals(1, results.size());
            assertEquals("doc2", results.get(0).get("uid"));
            
            results = search(getQuery("albino elephant"));
            assertEquals(results.size(), 2);
            assertEquals("doc5", results.get(0).get("uid"));
            assertEquals("doc4", results.get(1).get("uid"));
            
            results = search(getQuery("x* ggg"));
            assertEquals(results.size(), 3);
            assertEquals("doc8", results.get(0).get("uid"));
            assertEquals("doc6", results.get(1).get("uid"));
            assertEquals("doc7", results.get(2).get("uid"));

        } catch (Exception e) {
            System.out.println(e.getMessage());
            fail("Test caused exception");
        }
    }
    
    private Query getQuery (String querystr) throws ParseException {
        Query query = queryParser.parse(querystr);
        if (show) {
            System.out.println("  Query:{" + querystr + "} ==> " + query);
            System.out.println();
        }
        return query;
    }
    
    private List<Document> search(Query query) throws ParseException, IOException {
        Hits hits = searcher.search(query);
        List<Document> results = new ArrayList<Document>();
        for (int i=0; i<hits.length(); i++) {
            Document doc = hits.doc(i);
            results.add(doc);
            if (show) {
                System.out.println(i + "  " + hits.score(i) + "  title:{" + doc.get("title") + "}    body:{" + doc.get("body") + "}");
                System.out.println();
            }
        }
        return results;    
    }
    
}
