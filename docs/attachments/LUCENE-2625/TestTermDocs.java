import junit.framework.Assert;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class TestTermDocs {
	
	@Test
	public void testTermDocs() throws Exception {
		IndexWriter iw = new IndexWriter(new RAMDirectory(), new KeywordAnalyzer(), MaxFieldLength.UNLIMITED);
		
		Document doc = new Document();
		doc.add(new Field("field", "value", Store.YES, Index.NOT_ANALYZED));
		
		iw.addDocument(doc);
		iw.commit();
		
		IndexReader reader = iw.getReader();
		TermDocs termDocs = reader.termDocs(new Term("field", "value"));
		
		Assert.assertTrue(termDocs.next());
		
		reader = iw.getReader();
		termDocs = reader.termDocs(null);
		
		Assert.assertTrue(termDocs.next());
		
		IndexSearcher searcher = new IndexSearcher(iw.getReader());
		TopDocs topDocs = searcher.search(new MatchAllDocsQuery(), 1);
		
		Assert.assertEquals(1, topDocs.scoreDocs.length);
		
		reader = iw.getReader();
		termDocs = reader.termDocs();
		
		Assert.assertTrue(termDocs.next());
		
	}

}
