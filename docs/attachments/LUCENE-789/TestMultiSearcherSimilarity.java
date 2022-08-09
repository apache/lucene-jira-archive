import java.io.IOException;

import junit.framework.TestCase;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;

/**
 * Makes sure the custom similarity is preserved when using MultiSearcher.
 * See LUCENE-789.
 * @author alexey@sciquest.com
 */
public class TestMultiSearcherSimilarity extends TestCase {

	private static final String FIELD = "field";
	private static final Similarity CUSTOM_SIMILARITY = new FixedScoreSimilarity();

	public void testMultiSearcherSimilarity() throws IOException {
		RAMDirectory directory = new RAMDirectory();

		// Create a simple index
		String[] values = new String[] { "1", "2", "3", "4" };
		IndexWriter writer = new IndexWriter(directory, new WhitespaceAnalyzer(), true);
		for (int i = 0; i < values.length; i++) {
			Document doc = new Document();
			doc.add(new Field(FIELD, values[i], Field.Store.NO, Field.Index.UN_TOKENIZED));
			writer.addDocument(doc);
		}
		writer.close();
		
		Query query = new TermQuery(new Term(FIELD, "1"));
		
		// Create an IndexSearcher with custom similarity
		IndexSearcher indexSearcher = new IndexSearcher(directory);
		indexSearcher.setSimilarity(CUSTOM_SIMILARITY);
		// Get a score from IndexSearcher
		TopDocs topDocs = indexSearcher.search(query, null, 1);
		float originalScore = topDocs.getMaxScore();
		
		// Wrap the IndexSearcher in a MultiSearcher, using the same custom similarity
		MultiSearcher multiSearcher = new MultiSearcher(new Searchable[] {indexSearcher});
		multiSearcher.setSimilarity(CUSTOM_SIMILARITY);
		// Get the score from MultiSearcher
		topDocs = multiSearcher.search(query, null, 1);
		float multiSearcherScore = topDocs.getMaxScore();
		
		// The scores from the IndexSearcher and Multisearcher should be the same
		// if the same similarity is used.
		assertEquals(originalScore, multiSearcherScore, 0);
	}
	
	static class FixedScoreSimilarity extends Similarity {

		@Override
		public float idf(int docFreq, int numDocs) {
			return 100.0f;
		}

		@Override
		public float coord(int overlap, int maxOverlap) {
			return 1.0f;
		}

		@Override
		public float lengthNorm(String fieldName, int numTokens) {
			return 1.0f;
		}

		@Override
		public float queryNorm(float sumOfSquaredWeights) {
			return 1.0f;
		}

		@Override
		public float sloppyFreq(int distance) {
			return 1.0f;
		}

		@Override
		public float tf(float freq) {
			return 1.0f;
		}
	}
}
