import java.util.*;

import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryParser.*;
import org.apache.lucene.search.*;

public class LuceneBug
{
	private IndexWriter writer_;
	private IndexSearcher searcher_;
	public static void main(String[] args) throws Exception
	{
		LuceneBug bug = new LuceneBug();

	}

	public LuceneBug() throws Exception
	{
		//Build a new index
		writer_ = new IndexWriter("C:/temp", new StandardAnalyzer(), true);
		//insert some docs
		addDocs();
		writer_.close();
		//open the index, generate a query
		searcher_ = new IndexSearcher("C:/temp");
		QueryParser parser = new QueryParser("all", new StandardAnalyzer());
		Query query = parser.parse("all:myocardial infarction");
		//run both queries
		searchA(query);
		searchB(query);
	}

	/**
	 * Search the standard way.
	 */
	private void searchA(Query query) throws Exception
	{
		System.out.println("Skip low scoring...");
		Hits hits = searcher_.search(query, (Filter) null);
		for (int i = 0; i < hits.length(); i++)
		{
			System.out.println("Document " + hits.doc(i));
			System.out.println("Score " + hits.score(i));
		}
	}

	/**
	 * Search with my own hit collector
	 * @param query
	 * @throws Exception
	 */
	private void searchB(Query query) throws Exception
	{
		System.out.println("All results...");
		final ArrayList tempHits = new ArrayList();
		searcher_.search(query, (Filter) null, new HitCollector()
		{
			public void collect(int doc, float score)
			{
				tempHits.add(new LuceneHits(doc, score));
			}
		});

		LuceneHits[] luceneHits = (LuceneHits[])tempHits.toArray(new LuceneHits[tempHits.size()]);

		Arrays.sort(luceneHits, new HitComparator());
		for (int i = 0; i < luceneHits.length; i++)
		{
			System.out.println("Document " + searcher_.doc(luceneHits[i].doc));
			System.out.println("Score " + luceneHits[i].score);
		}
	}

	/**
	 * Add some documents to the index
	 */
	private void addDocs() throws Exception
	{
		addDocument("all", "postoperative myocardial infarction (disorder) postoperative myocardial infarction");
		addDocument("all", "ecg: myocardial infarction (finding) ecg: myocardial infarction");
		addDocument("all", "ecg: no myocardial infarction (finding) ecg: no myocardial infarction");
		addDocument("all", "first myocardial infarction (disorder) first myocardial infarction");
		addDocument(
			"all",
			"myocardial infarction with complication (disorder) myocardial infarction with complication");
		addDocument("all", "coronary thrombosis not leading to myocardial infarction aborted myocardial infarction");
		addDocument("all", "a document that will not match");
		addDocument("all", "short document");
		addDocument("all", "anothe document.");
		addDocument("all", "Something else entirely.");
		addDocument(
			"all",
			"myocardial ischemia manifest on stress test status post myocardial infarction (finding) myocardial ischemia manifest on stress test status post myocardial infarction preferred_designation:myocardial ischaemia manifest on stress test status post myocardial infarction");

		//This loop is what is controling (to some extent) the scores of the second method.
		//the bigger the loop, the bigger the scores.
		for (int i = 0; i < 50; i++)
		{
			addDocument("foobar", "something");
		}

	}

	/**
	 * Insert a doc...
	 */
	private void addDocument(String field, String value) throws Exception
	{
		Document temp = new Document();
		temp.add(new Field(field, value, true, true, true));
		writer_.addDocument(temp);
	}

	/**
	 * For collecting the docs and scores with my own hit collector...
	 */
	private class LuceneHits
	{
		public int doc;
		public float score;

		public LuceneHits(int doc, float score)
		{
			this.doc = doc;
			this.score = score;
		}
	}

	/**
	 * For sorting my results...
	 */
	private class HitComparator implements Comparator
	{
		public int compare(Object arg0, Object arg1)
		{
			LuceneHits a = (LuceneHits)arg0;
			LuceneHits b = (LuceneHits)arg1;

			if (a.score < b.score)
				return 1;
			else if (a.score == b.score)
				return 0;
			else
				return -1;
		}
	}
}
