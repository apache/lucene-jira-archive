package test;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.document.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;

import java.io.*;
import java.util.Date;

public class TestDelete {
    public void runTest() throws Exception {
        testFailed1();
        testFailed2();
        testSuccess();
    }

    public void testFailed1() throws Exception {
        System.out.println("***** testFailed1 *****");
        indexTerm("word1", true);

        // Keeps the reader open and use it to delete a term after a new term
        // is indexed.  This causes the deleted term only effective in memory.
        IndexReader reader1 = IndexReader.open("TestDelete");

        indexTerm("word2", false);
        searchTerm(2, null, "word*");

        reader1.delete(new Term("contents", "word1"));
        // The old reader should not have the new indexed term.  This is fine.
        searchTerm(0, reader1, "word*");
        reader1.close();

        // This search failed.  It still fines the first term "word1".
        searchTerm(1, null, "word*");
        System.out.println();
    }

    public void testFailed2() throws Exception {
        System.out.println("***** testFailed2 *****");
        indexTerm("word1", true);

        // Keeps the reader open and use it to delete a term after a new term
        // is indexed.  This causes the deleted term only effective in memory.
        IndexReader reader1 = IndexReader.open("TestDelete");

        indexTerm("word2", false);
        searchTerm(2, null, "word*");

        reader1.delete(new Term("contents", "word1"));
        // The old reader should not have the new indexed term.  This is fine.
        searchTerm(0, reader1, "word*");

        reader1.close();

        // It doesn't matter what I do here.  The deleted term (word1) is still
        // there.
        FSDirectory.getDirectory("TestDelete", false).close();
        IndexWriter writer = new IndexWriter("TestDelete", new StandardAnalyzer(), false);
        writer.optimize();
        writer.close();

        // This search failed.  It still fines the first term "word1".
        searchTerm(1, null, "word*");
        System.out.println();
    }

    public void testSuccess() throws Exception {
        System.out.println("***** testSuccess *****");
        indexTerm("word1", true);

        indexTerm("word2", false);
        searchTerm(2, null, "word*");

        IndexReader reader1 = IndexReader.open("TestDelete");
        reader1.delete(new Term("contents", "word1"));
        searchTerm(1, reader1, "word*");
        reader1.close();

        searchTerm(1, null, "word*");
        System.out.println();
    }

    public void indexTerm(String term, boolean create) throws Exception {
        Document doc = new Document();
        doc.add(Field.Keyword("contents", term));

        IndexWriter writer = new IndexWriter("TestDelete", new StandardAnalyzer(), create);
        writer.addDocument(doc);

        // optimize() does not have any effect on the problem.
        //writer.optimize();
        writer.close();
    }

    public void searchTerm(int expected, IndexReader reader, String term) throws Exception {
        // If a reader is given, use that reader.  Otherwise open one, and
        // close it at the end.
        IndexReader myReader = reader == null ?
            IndexReader.open("TestDelete") : reader;

        Searcher searcher = new IndexSearcher(myReader);
	Hits hits = searcher.search(new WildcardQuery(new Term("contents", term)));

        if(hits.length() != expected)
            System.out.println("***** this search failed *****");
        System.out.print("Searching for: ["+term+"] expecting: "+expected+
            " terms.  Found:   ");
	for (int i = 0; i < hits.length(); i++) {
            System.out.print(hits.doc(i).get("contents")+" ");
        }
        System.out.println();

        // Close the reader that is opened by us.
        if(reader == null)
            myReader.close();
    }

    public static void main(String[] args) {
        try {
            new TestDelete().runTest();
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }
}