package bugs;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class LuceneBug extends TestCase
{

    /**
     * This test class creates a "LuceneTest" folder in your system temp folder. 
     * Then, it creates two FSDirectories, and populates each with 1000 documents.
     * The documents each consist of a single untokenized field - "count".  The 
     * value of this field is the order in which the document was added.
     * 
     * Each individual index is verified, and then they are merged.
     * 
     * After the two indexes are merged into a new, empty index, it verifies 
     * that the document order is still correct.
     * 
     * Incorrectly ordered documents are printed to system.out - and the test fails.
     * @throws IOException
     */
    public void testLucene() throws IOException
    {
        File testFolder = new File(System.getProperty("java.io.tmpdir")
                + System.getProperty("file.separator")
                + "LuceneTest");
        testFolder.mkdir();
        File indexA = new File(testFolder, "a");
        File indexB = new File(testFolder, "b");

        indexA.mkdir();
        indexB.mkdir();

        FSDirectory a = fillIndex(indexA, 0, 1000);
        boolean fail = verifyIndex(a, 0);
        
        if (fail)
        {
            fail("Index a is invalid");
        }
        
        FSDirectory b = fillIndex(indexB, 1000, 1000);
        fail = verifyIndex(b, 1000);
        if (fail)
        {
            fail("Index b is invalid");
        }

        FSDirectory merged = FSDirectory.getDirectory(testFolder, true);

        IndexWriter writer = new IndexWriter(merged, new StandardAnalyzer(), true);
        writer.setMergeFactor(2);

        writer.addIndexes(new Directory[]{a, b});
        writer.close();
        merged.close();

        fail = verifyIndex(merged, 0);

        assertFalse("The merged index is invalid", fail);
    }
    
    private boolean verifyIndex(FSDirectory directory, int startAt) throws IOException
    {
        boolean fail = false;
        IndexReader reader = IndexReader.open(directory);

        int max = reader.maxDoc();
        for (int i = 0; i < max; i++)
        {
            Document temp = reader.document(i);
            //compare the index doc number to the value that it should be
            if (!temp.getField("count").stringValue().equals(i + startAt + ""))
            {
                fail = true;
                System.out.println("Document " + i + startAt + " is returning document " + temp.getField("count").stringValue());
            }
        }
        return fail;
    }

    private FSDirectory fillIndex(File directory, int start, int numDocs) throws IOException
    {
        FSDirectory dir = FSDirectory.getDirectory(directory, true);

        IndexWriter writer = new IndexWriter(dir, new StandardAnalyzer(), true);
        writer.setMergeFactor(2);

        for (int i = start; i < (start + numDocs); i++)
        {
            Document temp = new Document();
            temp.add(new Field("count", i + "", Field.Store.YES, Field.Index.UN_TOKENIZED));

            writer.addDocument(temp);
        }
        writer.close();
        dir.close();
        return dir;
    }
}
