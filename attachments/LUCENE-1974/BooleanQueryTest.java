package org.fulin.search.test;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.RAMDirectory;

/**
 * represent the bug of 
 * 
 * 		BooleanScorer.score(Collector collector, int max, int firstDocID)
 * 
 * Line 273, end=8192, subScorerDocID=11378, then more got false?
 * 
 * @author tangfulin <tangfulin@gmail.com>
 *
 */
public class BooleanQueryTest extends TestCase {

	private static final String FIELD = "name";
	private static RAMDirectory directory = new RAMDirectory();
	private static String[] values = new String[] { "tangfulin" };

	protected void setUp() throws Exception {
		IndexWriter writer = new IndexWriter(directory,
				new WhitespaceAnalyzer(), true,
				IndexWriter.MaxFieldLength.LIMITED);

		for (int i = 0; i < 5137; ++i) {
			Document doc = new Document();
			doc.add(new Field(FIELD, "meaninglessnames", Field.Store.YES,
					Field.Index.NOT_ANALYZED));
			writer.addDocument(doc);
		}

		for (int i = 0; i < values.length; i++) {
			Document doc = new Document();
			doc.add(new Field(FIELD, values[i], Field.Store.YES,
					Field.Index.NOT_ANALYZED));
			writer.addDocument(doc);
		}

		for (int i = 5138; i < 11377; ++i) {
			Document doc = new Document();
			doc.add(new Field(FIELD, "meaninglessnames", Field.Store.YES,
					Field.Index.NOT_ANALYZED));
			writer.addDocument(doc);
		}

		for (int i = 0; i < values.length; i++) {
			Document doc = new Document();
			doc.add(new Field(FIELD, values[i], Field.Store.YES,
					Field.Index.NOT_ANALYZED));
			writer.addDocument(doc);
		}

		writer.close();
	}

	public void testBooleanPrefixQuery() {
		try {
			IndexSearcher indexSearcher = new IndexSearcher(directory, true);
			BooleanQuery query;
			ScoreDoc[] hits;

			PrefixQuery pq = new PrefixQuery(new Term(FIELD, "tang"));
			BooleanQuery booleanQuery1 = new BooleanQuery();
			booleanQuery1.add(pq, BooleanClause.Occur.SHOULD);

			query = new BooleanQuery();
			query.add(booleanQuery1, BooleanClause.Occur.SHOULD);
			hits = indexSearcher.search(query, null, 1000).scoreDocs;

			System.out.println("query: " + query);
			for (ScoreDoc hit : hits) {
				System.out.println(hit + "  doc:" + indexSearcher.doc(hit.doc));
			}

			assertEquals("Number of matched documents", 2, hits.length);

			query = new BooleanQuery();
			query.add(pq, BooleanClause.Occur.SHOULD);

			query.add(new TermQuery(new Term(FIELD, "notexistnames")),
					BooleanClause.Occur.SHOULD);

			hits = indexSearcher.search(query, null, 1000).scoreDocs;

			System.out.println("query: " + query);
			for (ScoreDoc hit : hits) {
				System.out.println(hit + "  doc:" + indexSearcher.doc(hit.doc));
			}

			assertEquals("Number of matched documents", 2, hits.length);

		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

}
