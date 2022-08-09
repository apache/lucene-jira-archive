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

public class PhraseTest extends TestCase {

	public void testPhrase() throws Exception {

		RAMDirectory store = new RAMDirectory();
		IndexWriter writer = new IndexWriter(store, new StandardAnalyzer(), true);
    
		Document d1 = new Document();
		d1.add(Field.Text("contents", "He said Apache is cool"));
		writer.addDocument(d1);
		writer.close();

		Searcher searcher = new IndexSearcher(store);
		Hits hits = null;
		Query query = null;
		
		// stopwords are ignored completly:
		query = QueryParser.parse("\"Apache cool\"", "contents", new StandardAnalyzer());
		hits = searcher.search(query); 
		assertEquals(hits.length(), 1);

		query = QueryParser.parse("\"Apache is cool\"", "contents", new StandardAnalyzer());
		hits = searcher.search(query); 
		assertEquals(hits.length(), 1);

		query = QueryParser.parse("\"Apache be cool\"", "contents", new StandardAnalyzer());
		hits = searcher.search(query); 
		assertEquals(hits.length(), 1);

		// "foobar" is no stopword, so it's not ignored:
		query = QueryParser.parse("\"Apache foobar cool\"", "contents", new StandardAnalyzer());
		hits = searcher.search(query); 
		assertEquals(hits.length(), 0);
	}
	
}
