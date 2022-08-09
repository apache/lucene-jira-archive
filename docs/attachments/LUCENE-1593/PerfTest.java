import java.io.File;
import java.util.Random;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.function.CustomScoreQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NativeFSLockFactory;

public class PerfTest {

	private static class RandomScoreMatchAllDocsQuery extends CustomScoreQuery {

		private Random r;
		
		public RandomScoreMatchAllDocsQuery() {
			super(new MatchAllDocsQuery());
			r = new Random(12);
		}
		
		public float customScore(int doc, float subQueryScore, float valSrcScore) {
			return r.nextFloat();
		}
		
	}
	
	public static void main(String[] args) throws Exception {

		if (args.length == 0) {
			System.out.println("usage: PerfTest <indexDir> [sort]");
			System.out.println("sort: optional, if defined the results will be sorted by a field");
			return;
		}
		File indexDir = new File(args[0]);
		Directory dir = new FSDirectory(indexDir, new NativeFSLockFactory(indexDir));

		// Create the index if it does not exist.
		if (!indexDir.exists() || indexDir.list().length == 0) {
			Random r = new Random(17);
			int numDocs = 10000000;
			IndexWriter writer = new IndexWriter(dir, null, MaxFieldLength.UNLIMITED);
			writer.setMergeFactor(50);
			writer.setMaxBufferedDocs(numDocs/100);
			System.out.println("populating index");
			long time = System.currentTimeMillis();
			for (int i = 0; i < numDocs; i++) {
				Document doc = new Document();
				doc.add(new Field("c", "test", Store.NO, Index.NOT_ANALYZED));
				doc.add(new Field("i", r.nextInt()+"", Store.NO, Index.NOT_ANALYZED));
				writer.addDocument(doc);
			}
			writer.close(false);
			System.out.println("time=" + (System.currentTimeMillis() - time));
		}

		System.out.println("searching");
		IndexSearcher searcher = new IndexSearcher(dir);
		System.out.println("numSegments=" + searcher.getIndexReader().getSequentialSubReaders().length);
		Sort sort = args.length == 2 ? new Sort(new SortField("i", SortField.INT)) : null;
		Query q = new RandomScoreMatchAllDocsQuery();
		// warm-up query
		if (sort == null) {
			searcher.search(q, 10);
		} else {
			searcher.search(q, null, 10, sort);
		}
		long time = System.currentTimeMillis();
		double numQueries = 20;
		long bestTime = Long.MAX_VALUE;
		int sum = 0;
		if (sort != null) {
			for (int i = 0; i < numQueries; i++) {
				final long t0 = System.nanoTime();
				sum += searcher.search(q, null, 10, sort).totalHits;
				long t = System.nanoTime()-t0;
				//System.out.println((t1-t0) + " millis");
				if (t < bestTime) {
					bestTime = t;
				}
			}
		} else {
			for (int i = 0; i < numQueries; i++) {
				final long t0 = System.nanoTime();
				sum += searcher.search(q, 10).totalHits;
				long t = System.nanoTime()-t0;
				//System.out.println((t1-t0) + " millis");
				if (t < bestTime) {
					bestTime = t;
				}
			}
		}
		time = System.currentTimeMillis() - time;
		//System.out.println("avg. time=" + (time / numQueries)
		//+ " ms");
		System.out.println("best time=" + (bestTime/1000000.0) + " ms; avg=" + (time/numQueries) + " ms; sum=" + (sum/1000000) + " M");
		searcher.close();
	}

}
