import org.apache.lucene.search.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import java.io.IOException;

public class WildcardQuestionmarkTest
{

    public static void main (String[] args)
        throws IOException
    {
        new WildcardQuestionmarkTest();
    }

    public WildcardQuestionmarkTest()
	throws IOException
    {
        RAMDirectory indexStore = new RAMDirectory();
        IndexWriter writer = new IndexWriter(indexStore, new SimpleAnalyzer(), true); 
        Document doc1 = new Document();
        Document doc2 = new Document();
        Document doc3 = new Document();
        Document doc4 = new Document();
	doc1.add(Field.Text("body", "metal"));
        doc2.add(Field.Text("body", "metals"));
        doc3.add(Field.Text("body", "mXtals"));
        doc4.add(Field.Text("body", "mXtXls"));
        writer.addDocument(doc1);
        writer.addDocument(doc2);
        writer.addDocument(doc3);
        writer.addDocument(doc4);
	writer.optimize();
	IndexSearcher searcher = new IndexSearcher(indexStore);
	Query query1 = new TermQuery(new Term("body", "m?tal"));       // 1
        Query query2 = new WildcardQuery(new Term("body", "metal?"));  // 2
        Query query3 = new WildcardQuery(new Term("body", "metals?")); // 1
        Query query4 = new WildcardQuery(new Term("body", "m?t?ls"));  // 3
	Hits results1 = searcher.search(query1);
        Hits results2 = searcher.search(query2);
        Hits results3 = searcher.search(query3);
        Hits results4 = searcher.search(query4);
	System.out.println("Searching for m?tal got " + results1.length() + " results.");
        System.out.println("Searching for metal? got " + results2.length() + " results.");
        System.out.println("Searching for metals? got " + results3.length() + " results.");
        System.out.println("Searching for m?t?ls got " + results4.length() + " results.");
	writer.close();
    }
}
