package org.apache.lucene.index;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;


/**
 * Check that indexing does not fail when add and remove are interleaved. 
 * @author Doron Cohen
 */
public class TestInterleavedAddAndRemoves extends TestCase {
	private int nextDocId = 0;
	private IndexModifier modifier;
	private Directory dir;
	private int nIterations [] = {  1, 100,  100,   100,   100,  1000 };
	private int nAdds [] =       { 10,  10,   10,    50,   200,   300 };
	private int nDeletes [] =    {  5,   5,    5,    10,    20,    30 };
	private int maxBufAdd [] =   { 10,  10, 1000,  5000, 10000, 50000 }; 
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	private static final String DOC_TEXT = // from a public first aid info at http://firstaid.ie.eu.org 
		"Well it may be a little dramatic but sometimes it true. " +
		"If you call the emergency medical services to an incident, " +
		"your actions have started the chain of survival. " +
		"You have acted to help someone you may not even know. " +
		"First aid is helping, first aid is making that call, " +
		"putting a Band-Aid on a small wound, controlling bleeding in large " +
		"wounds or providing CPR for a collapsed person whose not breathing " +
		"and heart has stopped beating. You can help yourself, your loved " +
		"ones and the stranger whose life may depend on you being in the " +
		"right place at the right time with the right knowledge.";
	
	private Document newDocument() throws IOException {
		Document doc = new Document();
		doc.add(new Field("content", DOC_TEXT, Field.Store.NO, Field.Index.TOKENIZED));
		doc.add(new Field("docid", ""+nextDocId++, Field.Store.NO, Field.Index.UN_TOKENIZED));
		return doc;
	}
	
	public void testInterleavedFsDirectoryOtherDisk() throws Exception {
	    File otherDisk = new File("D:","tmp");
	    dir = FSDirectory.getDirectory(new File(otherDisk, "test.interleaved"),true);
	    for (int i = 0; i < nAdds.length; i++) {
	    	doTestInterleaved(i);
	    	doTestInterleaved(i);
	    }
	}

	public void testInterleavedFsDirectoryTempDisk() throws Exception {
	    String tempDir = System.getProperty("java.io.tmpdir");
	    if (tempDir == null)
	      throw new IOException("java.io.tmpdir undefined, cannot run test");
	    dir = FSDirectory.getDirectory(new File(tempDir, "test.interleaved"),true);
	    for (int i = 0; i < nAdds.length; i++) {
	    	doTestInterleaved(i);
	    	doTestInterleaved(i);
	    }
	}
	
	private void doTestInterleaved(int i) throws Exception {
		printTestSetup(i);
		modifier = new IndexModifier(dir,new StandardAnalyzer(), true);
		modifier.setMaxBufferedDocs(maxBufAdd[i]);
		for (int j = 0; j < nIterations[i]; j++) {
			doAddDocs(nAdds[i]);
			doDeleteDocs(nDeletes[i]);
		}
		modifier.optimize();
		modifier.close();
	}
	
	private void doAddDocs(int n) throws Exception {
		for (int i = 0; i < n; i++) {
			modifier.addDocument(newDocument());
		}
	}

	private void doDeleteDocs(int n) throws Exception {
		int deletedId = nextDocId-1; 
		for (int i=0; i<n && deletedId>=0; i++) {
			modifier.deleteDocuments(new Term("docid", ""+deletedId));
			deletedId -= 2;
		}
	}

	private void printTestSetup(int i) {
		StringBuffer sb = new StringBuffer("Start: ");
		sb.append("max-buf-docs="+maxBufAdd[i]+" - ");    
		sb.append(nIterations[i]+" iterations; ");
		sb.append("each adding "+nAdds[i]+" docs and deleting ");
		sb.append(nDeletes[i]+" docs.");
		System.out.println(sb);
	}

}
