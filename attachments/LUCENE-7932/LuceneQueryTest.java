package se.logica.archive.bl.access;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * A test class to simulate bug in IndexSearcher class
 * @author balekundrim
 *
 */
public class LuceneQueryTest {

    public static void main(String[] args) throws IOException, ParseException {
	StandardAnalyzer analyzer = new StandardAnalyzer();
	String directory = "C:\\LuceneTest\\Index";

	Path path = Paths.get(directory);
	Directory index = FSDirectory.open(path);

	//Step 1. create the index
//	createIndex(index, analyzer);

	//Step 2. prepare query params for query
	Query q = null;
	String queryParams = null;
	
	//Case:A
//	queryParams = "LocationCode:1 AND Category:a";
	
	//Case:B
//	queryParams = "LocationCode:1 AND Category:A";
	
	//Case:C
	queryParams = "LocationCode:1 AND Category:a*";
	
	//Step 3. Prepare QueryParser object
	q = queryFS(q, analyzer, queryParams);

	//Step 4. Search & display results
	searchFS(index, q);
    }

    /**
     * 
     * @param index
     * @param analyzer
     * @throws IOException
     */
    private static void createIndex(Directory index, StandardAnalyzer analyzer) throws IOException {
	IndexWriterConfig config = new IndexWriterConfig(analyzer);

	IndexWriter w = new IndexWriter(index, config);
	addDoc(w, "1", "b", "README.txt");
	addDoc(w, "1", "bc", "rEADME.txt");
	addDoc(w, "1", "BCD", "abc.txt");
	w.close();
    }

    /**
     * addDoc
     * @param w
     * @param locationCode
     * @param category
     * @param attachment
     * @throws IOException
     */
    private static void addDoc(IndexWriter w, String locationCode, String category, String attachment)
	    throws IOException {
	Document doc = new Document();
	doc.add(new StringField("LocationCode", locationCode, Field.Store.YES));
	doc.add(new TextField("Category", category, Field.Store.YES));
	doc.add(new TextField("Attachment", attachment, Field.Store.YES));
	w.addDocument(doc);
    }

    /**
     * queryFS
     * @param q
     * @param analyzer
     * @param args
     * @return
     * @throws ParseException
     */
    private static Query queryFS(Query q, StandardAnalyzer analyzer, String args) throws ParseException {
	String words = args;
	System.out.println("words:" + words);

	q = new QueryParser(words, analyzer).parse(words);//
	return q;
    }

    /**
     * searchFS
     * @param index
     * @param q
     * @throws IOException
     */
    private static void searchFS(Directory index, Query q) throws IOException {
	int hitsPerPage = 10;
	IndexReader reader = DirectoryReader.open(index);
	IndexSearcher searcher = new IndexSearcher(reader);
	TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
	searcher.search(q, collector);
	ScoreDoc[] hits = collector.topDocs().scoreDocs;
	displayResults(searcher, hits);
	reader.close();
    }

    /**
     * displayResults
     * @param searcher
     * @param hits
     * @throws IOException
     */
    private static void displayResults(IndexSearcher searcher, ScoreDoc[] hits) throws IOException {
	System.out.println("Found " + hits.length + " hits.");
	for (int i = 0; i < hits.length; ++i) {
	    int docId = hits[i].doc;
	    Document d = searcher.doc(docId);
	    List<IndexableField> fields = d.getFields();
	    System.out.print("\n" + (i + 1) + ". ");
	    for (IndexableField indexableField : fields) {
		System.out.print(indexableField.name() + ":" + d.get(indexableField.name()) + "\t");
	    }
	}
    }
}
