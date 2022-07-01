/**
 * Test case to demonstrate wildcard searching bug.
 *
 * When my patch is applied, the second line of output should 
 * report 2 results, (it finds metal and metals).
 * 
 * However, without the patch, only one result is reported.
 */
import org.apache.lucene.search.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import java.io.IOException;

public class WildcardTest
{
    public WildcardTest()
        throws IOException
    {
        RAMDirectory indexStore = new RAMDirectory();
        IndexWriter writer = new IndexWriter(indexStore, new SimpleAnalyzer(), true);

        Document doc1 = new Document();
        Document doc2 = new Document();

        doc1.add(Field.Text("body", "metal"));
        doc2.add(Field.Text("body", "metals"));
        writer.addDocument(doc1);
        writer.addDocument(doc2);

        writer.optimize();

        IndexSearcher searcher = new IndexSearcher(indexStore);

        Query query1 = new TermQuery(new Term("body", "metal"));
        Query query2 = new WildcardQuery(new Term("body", "metal*"));

        Hits results1 = searcher.search(query1);
        Hits results2 = searcher.search(query2);

        System.out.println("Searching for metal got " + results1.length() + " results.");
        System.out.println("Searching for metal* got " + results2.length() + " results.");

        writer.close();
    }

    public static void main (String[] args)
        throws IOException
    {
        new WildcardTest();
    }
}
