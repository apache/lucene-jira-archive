import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.Before;
import org.junit.Test;


public class MultiTermFlatSpanNearTest {
	private static final String FIELD_NM = "text";

	private Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_45, CharArraySet.EMPTY_SET);
	private Directory directory;

	@Before
	public void setup() throws Exception {
		directory = new RAMDirectory();
	}
	
	protected static void addDocument(IndexWriter writer, String field, String value) throws IOException {
		Document document = new Document();
		document.add(new TextField(field, value, Store.YES));
		writer.addDocument(document);
	}

	@Test
	public void testSpanNear() throws Exception {
		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_45, analyzer);
		IndexWriter indexWriter = new IndexWriter(directory, conf);
		addDocument(indexWriter, FIELD_NM, "a b c d e f g");
		indexWriter.close();

		assertQueryTermsReturnsHit(1, "b c d g");
		assertQueryTermsReturnsHit(1, "b d g");
	}

	private void assertQueryTermsReturnsHit(int expectedHits, String queryTerms) throws IOException {
		Query query = createSpanQuery(queryTerms.split(" "));
		System.out.println(query);
		IndexReader reader = DirectoryReader.open(directory);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopDocs topDocs = searcher.search(query, expectedHits);
		assertEquals(expectedHits, topDocs.totalHits);
		assertEquals(1, topDocs.totalHits);
	}

	private Query createSpanQuery(String... queryStringTerms) {
		int slop = 2;
		boolean inOrder = false;
		SpanQuery[] clauses = new SpanQuery[queryStringTerms.length];
		for (int i=0; i<queryStringTerms.length; i++) {
			clauses[i] = new SpanTermQuery(new Term(FIELD_NM, queryStringTerms[i]));
		}
		
		return new SpanNearQuery(clauses, slop, inOrder);
	}
}

