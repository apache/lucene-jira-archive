import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class SortByDouble {

	public static void main(String[] args) throws Exception {
		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_33,
				new KeywordAnalyzer());
		RAMDirectory d = new RAMDirectory();
		IndexWriter w = new IndexWriter(d, conf);
		
		// 1st doc, value 3.5d
		Document doc = new Document();
		doc.add(new NumericField("f", Store.YES, true).setDoubleValue(3.5d));
		w.addDocument(doc);
		
		// 2nd doc, value of -10d
		doc = new Document();
		doc.add(new NumericField("f", Store.YES, true).setDoubleValue(-10d));
		w.addDocument(doc);
		
		// 3rd doc, no value at all
		w.addDocument(new Document());
		w.close();

		IndexSearcher s = new IndexSearcher(d);
		Sort sort = new Sort(new SortField("f", SortField.DOUBLE, true));
		TopDocs td = s.search(new MatchAllDocsQuery(), 10, sort);
		for (ScoreDoc sd : td.scoreDocs) {
			System.out.println(sd.doc + ": " + s.doc(sd.doc).get("f"));
		}
		s.close();
		d.close();
	}
}