package org.apache.lucene.index;

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
import org.apache.lucene.store.RAMDirectory;

public class TestMergeIndexDocOrder extends TestCase
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
     * @throws Exception
     */
    public void testFSDirectory()
	throws Exception
    {
        File testFolder = new File(System.getProperty("java.io.tmpdir")
                + System.getProperty("file.separator")
                + "LuceneTest");
        testFolder.mkdir();
        File indexA = new File(testFolder, "a");
        File indexB = new File(testFolder, "b");
	File indexM = new File(testFolder, "merged");
        indexA.mkdir();
        indexB.mkdir();
        indexM.mkdir();
	
	Directory a = FSDirectory.getDirectory(indexA, true);
	Directory b = FSDirectory.getDirectory(indexA, true);
        Directory merged = FSDirectory.getDirectory(indexM, true);

	doTest(a, b, merged);
    }

    public void testRAMDirectory()
	throws Exception
    {
	
	Directory a = new RAMDirectory();
	Directory b = new RAMDirectory();
        Directory merged = new RAMDirectory();

	doTest(a, b, merged);

    }
    
    
    public void doTest(Directory a, Directory b, Directory merged)
	throws Exception
    {

        fillIndex(a, 0, 1000);
        verifyIndex("a", a, 0);
        
        fillIndex(b, 1000, 1000);
        verifyIndex("b", b, 1000);

        IndexWriter writer = getWriter(merged);

        writer.addIndexes(new Directory[]{a, b});
        writer.close();
        merged.close();

        verifyIndex("merged", merged, 0);
    }
    
    private void verifyIndex(String label, Directory directory, int startAt)
	throws Exception
    {
        boolean fail = false;
        IndexReader reader = IndexReader.open(directory);

        int max = reader.maxDoc();
        for (int i = 0; i < max; i++)
        {
            Document temp = reader.document(i);
	    assertEquals("doc " + i + " of index " + label + " is wrong",
			 new Integer(i + startAt),
			 new Integer(temp.getField("count").stringValue()));
        }
    }

    private void fillIndex(Directory dir, int start, int numDocs)
	throws Exception
    {
        IndexWriter writer = getWriter(dir);

        for (int i = start; i < (start + numDocs); i++)
        {
            Document temp = new Document();
            temp.add(new Field("count", i + "",
			       Field.Store.YES, Field.Index.UN_TOKENIZED));

            writer.addDocument(temp);
        }
        writer.close();
        dir.close();
    }

    private IndexWriter getWriter(Directory dir)
	throws Exception
    {
	IndexWriter w = new IndexWriter(dir, new StandardAnalyzer(), true);
	w.setMergeFactor(2);
	return w;
    }
}
