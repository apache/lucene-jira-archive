/*
 * Created on 15.08.2004
 *
 */
package org.apache.lucene.search;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.store.RAMDirectory;

/**
 * @author Bernhard Messer
 * @version $rcs = ' $Id: Exp $ ' ;
 */
public class TestDateSort  extends TestCase {
	
	private RAMDirectory directory = new RAMDirectory();
	
	private IndexSearcher searcher = null;
	
	
	public void setUp() throws Exception {
  	IndexWriter writer
            = new IndexWriter(directory, new SimpleAnalyzer(), true);
  	
    Document doc = new Document();
    Field fld = new Field("last-modified", "200409030517", true, true, false, false);
    doc.add(fld);
    doc.add(Field.Keyword("contents", "a"));
    writer.addDocument(doc);
    
    doc = new Document();
    fld = new Field("last-modified", "200408022206", true, true, false, false);
    doc.add(fld);
    doc.add(Field.Keyword("contents", "a"));
    writer.addDocument(doc);
    
    doc = new Document();
    fld = new Field("last-modified", "200407302321", true, true, false, false);
    doc.add(fld);
    doc.add(Field.Keyword("contents", "a"));
    writer.addDocument(doc);
    
    doc = new Document();
    fld = new Field("last-modified", "200408170345", true, true, false, false);
    doc.add(fld);
    doc.add(Field.Keyword("contents", "a"));
    writer.addDocument(doc);
    
    doc = new Document();
    fld = new Field("last-modified", "200408030456", true, true, false, false);
    doc.add(fld);
    doc.add(Field.Keyword("contents", "a"));
    writer.addDocument(doc);
  	
  	writer.close();
  	
	}
	
	public void test ()
		throws IOException, ParseException {
		
		searcher = new IndexSearcher(directory);
		
		QueryParser queryParser = new QueryParser("contents", new SimpleAnalyzer());
		Query query = queryParser.parse("a");
		
		Sort sort = new Sort(new SortField[]{new SortField("last-modified", SortField.STRING, true)});
		Hits hits = searcher.search(query, sort);
		
		for (int i = 0; i < hits.length(); i++) {
			System.out.println(hits.doc(i).getField("last-modified"));
		}
		
		searcher.close();
	}
	
	
	public static void main(String[] args) {
		TestDateSort t = new TestDateSort();
		try {
			t.setUp();
			t.test();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
