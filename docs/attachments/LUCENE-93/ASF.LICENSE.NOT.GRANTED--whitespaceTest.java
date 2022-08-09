package com.intraf.test;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.store.RAMDirectory;

import junit.framework.TestCase;

public class whitespaceTest extends TestCase {

	//public static void main(String args[]) throws Exception {
	public void testPhrase() throws Exception {

		RAMDirectory store = new RAMDirectory();
		IndexWriter writer = new IndexWriter(store, new StandardAnalyzer(), true);
    
		Document d1 = new Document();
		d1.add(Field.Text("contents", "Blah blah Weltbank blah"));
		d1.add(Field.Text("identifier", "Weltbank"));
		writer.addDocument(d1);
		Document d2 = new Document();
		d2.add(Field.Text("contents", "Blah blah foobar"));
		d2.add(Field.Text("identifier", "Weltbank"));
		writer.addDocument(d2);
		writer.close();

		Searcher searcher = new IndexSearcher(store);
		Hits hits = null;
		Query query = null;
		
		// see http://nagoya.apache.org/bugzilla/show_bug.cgi?id=18847
		
		query = QueryParser.parse("Weltbank && identifier:Weltbank", "contents", new StandardAnalyzer());
		hits = searcher.search(query); 
		assertEquals(1, hits.length());

		query = QueryParser.parse("Weltbank &&\nidentifier:Weltbank", "contents", new StandardAnalyzer());
		hits = searcher.search(query); 
		assertEquals(1, hits.length());

		query = QueryParser.parse("Weltbank &&\n identifier:Weltbank", "contents", new StandardAnalyzer());
		hits = searcher.search(query); 
		assertEquals(1, hits.length());

		query = QueryParser.parse("Weltbank || identifier:Weltbank", "contents", new StandardAnalyzer());
		hits = searcher.search(query); 
		assertEquals(2, hits.length());

		query = QueryParser.parse("Weltbank ||\n identifier:Weltbank", "contents", new StandardAnalyzer());
		hits = searcher.search(query); 
		assertEquals(2, hits.length());
	}
	
}
