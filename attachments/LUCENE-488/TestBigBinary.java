package org.apache.lucene.document;

import java.io.File;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.document.*;


public class TestBigBinary extends TestCase {

    public static int NUM_ITERS = 100;
    public static int MIN_MB = 4;
    public static int MAX_MB = 10;
    public static Field.Store STORE_TYPE = Field.Store.YES;

    public static int RAND_EXEC = (new Random()).nextInt(99999999);
    
    public void testBigBinaryFields() throws Exception {

	for (int t = MIN_MB; t < MAX_MB; t++) {

	    testBigBinaryFields(NUM_ITERS, t);
	}
    }
    
    public void testBigBinaryFields(final int iters, final int mb) throws Exception {
	
        String tmp = System.getProperty("java.io.tmpdir");
        if (null == tmp) {
            throw new RuntimeException
                ("can't continue without java.io.tmpdir");
        }
        File indexDir = new File(tmp,
				 this.getClass().getName() + "." + RAND_EXEC + 
				 "." + iters + "iters." + mb + "mb");
				 

	System.out.println("NOTE: directory will not be cleaned up automatically...");
	System.out.println("Dir: " + indexDir.getAbsolutePath());
	    
	/** add the doc to a ram index */
	Directory dir = FSDirectory.getDirectory(indexDir, true);
	IndexWriter writer = new IndexWriter(dir,
					     new StandardAnalyzer(), true);

	int i = 0;
	int totalBytes = 0;
	try {
	    for ( ; i < iters; i++) {
		
		Document doc = new Document();
		doc.add(Field.Keyword("id",i+""));
		
		byte[] data = new byte[1024 * 1024 * mb];
		totalBytes+=data.length;
		
		for (int b = 0; b < data.length; b += 7) {
		    data[b] = (new Integer(i+b)).byteValue();
		}
		doc.add(new Field("data", data, STORE_TYPE));
		
		writer.addDocument(doc);
		
	    }
	} finally {
	    System.out.println("iters completed: " + i);
	    System.out.println("totalBytes Allocated: " + totalBytes);
	    writer.close();
	}
    
	/** open a reader and fetch the document */ 
	dir = FSDirectory.getDirectory(indexDir, false);
	IndexReader reader = IndexReader.open(dir);
	int count = reader.numDocs();

	assertEquals(count, iters);
	reader.close();
	    
    }
    
}
