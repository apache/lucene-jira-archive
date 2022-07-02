package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;

public class TestCase {

	@Test
	public void testStopwordQuery24() {
		doTest(Version.LUCENE_24);
	}
	
	@Test
	public void testStopwordQuery29() {
		doTest(Version.LUCENE_29);
	}
	
	@Test
	public void testStopwordQuery30() {
		doTest(Version.LUCENE_30);
	}
	
	private void doTest(Version ver) {
		Directory dir = new RAMDirectory();
		Analyzer a = new StandardAnalyzer(ver);
		try {
			IndexWriter writer = new IndexWriter(dir, a, 
					IndexWriter.MaxFieldLength.LIMITED);
			Document doc = new Document();
			// The field contains a stopword
			doc.add(new Field("message", 
					"Owner document not found for: IOLanguageVariantPK:io=aaaaf26c9ca25d653914188c01c0ff66:version=1:language=--:variant=--", 
					Field.Store.YES, Field.Index.ANALYZED));
			writer.addDocument(doc);
			writer.close();
			
			IndexReader reader = IndexReader.open(dir, true);
			IndexSearcher searcher = new IndexSearcher(reader);
			
			// Search 1st try
			PhraseQuery phrase = new PhraseQuery();
			for (Term term : tokenize(a, 
					"message", "Owner document")) {
				phrase.add(term);
			}
			TopDocs td = searcher.search(phrase, 10);
			assertEquals(1, td.totalHits);
			
			phrase = new PhraseQuery();
			for (Term term : tokenize(a, 
					"message", "Owner document not found")) {
				phrase.add(term);
			}
			td = searcher.search(phrase, 10);
			assertEquals(1, td.totalHits);
		} catch (Exception e) {
			fail("should not throw exception");
		}
	}
	
	private List<Term> tokenize(Analyzer analyzer, String fld, String val) throws IOException {
		List<Term> terms = new ArrayList<Term>();
		TokenStream ts = analyzer.tokenStream(fld, new StringReader(val));
		try {
			// Iterate over tokens and treat each token as term
			while (ts.incrementToken()) {
				TermAttribute t = (TermAttribute) ts.getAttribute(TermAttribute.class);
				terms.add(new Term(fld, t.term()));
			}
			// End-of-stream clean-up
			ts.end();
		} finally {
			ts.close();
		}
		return terms;
	}

}
