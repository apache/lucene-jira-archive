package tests;

import java.util.HashSet;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;

public class LuceneBooleanQuery extends TestCase {
	
	
	private final static String storePathname = "testLuceneMmap";
	public void testMmapIndex() throws Exception {
		FSDirectory storeDirectory;
		storeDirectory = FSDirectory.getDirectory(storePathname, true);

		// plan to add a set of useful stopwords, consider changing some of the
		// interior filters.
		StandardAnalyzer analyzer = new StandardAnalyzer(new HashSet());
		// TODO: something about lock timeouts and leftover locks.
		IndexWriter writer = new IndexWriter(storeDirectory, analyzer, true);
		
		String f = "george bush";
		Document doc = new Document();
		doc.add(new Field("data", f, Field.Store.YES, Field.Index.TOKENIZED));	
		writer.addDocument(doc);

		IndexSearcher searcher = new IndexSearcher(storePathname);
		BooleanQuery bq = new BooleanQuery();
		TermQuery tq;
		Term t = new Term("data", "george");
		tq = new TermQuery(t);
		bq.add(tq, BooleanClause.Occur.SHOULD);
		t = new Term("data", "bush");
		tq = new TermQuery(t);
		bq.add(tq, BooleanClause.Occur.SHOULD);
		Hits hits = searcher.search(bq);
		assertEquals(1, hits.length());
		
		searcher.close();
	}

}
