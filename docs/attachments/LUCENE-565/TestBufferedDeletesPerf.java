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
 * Check that bufferred improves performance over IndexModifier 
 * @author Doron Cohen
 */
public class TestBufferedDeletesPerf extends TestCase {
	private int nextDocId = 0;
	private IndexModifier modifier;
	private NewIndexModifier newModifier;
	private File workDir;
	private int numAddsPerIteration;
	private int numIterations;
	private int numDeletesPerIteration;
	private int deleteGap;
	private int numMeasurements;
	private int maxBufferredDocs;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	    String tempDir = System.getProperty("java.io.tmpdir");
	    if (tempDir == null)
	      throw new IOException("java.io.tmpdir undefined, cannot run test");
	    workDir = new File(tempDir,"test.perf");
		// default setting
		numIterations = 1;          // do that many iterations 
		numAddsPerIteration = 10;    // in each iteration, add such many docs
		numDeletesPerIteration = 5;  // then delete that many docs
		deleteGap = 2;               // this is the gap between deleted docs ids
		numMeasurements = 2;         // number of times each test is run, before taking the minimum time
		maxBufferredDocs = 10;    
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
	
	public void testBufferedDeletesPerfCase1() throws Exception {
		// use default values
		doTestBufferedDeletesPerf();
	}
	
	public void testBufferedDeletesPerfCase2() throws Exception {
		numIterations = 100;          // do that many iterations 
		numAddsPerIteration = 10;    // in each iteration, add such many docs
		numDeletesPerIteration = 5;  // then delete that many docs
		deleteGap = 2;               // this is the gap between deleted docs ids
		numMeasurements = 2;         // number of times each test is run, before taking the minimum time
		maxBufferredDocs = 10;    
		doTestBufferedDeletesPerf();
	}

	public void testBufferedDeletesPerfCase3() throws Exception {
		numIterations = 100;          // do that many iterations 
		numAddsPerIteration = 10;    // in each iteration, add such many docs
		numDeletesPerIteration = 5;  // then delete that many docs
		deleteGap = 2;               // this is the gap between deleted docs ids
		numMeasurements = 2;         // number of times each test is run, before taking the minimum time
		maxBufferredDocs = 1000;    
		doTestBufferedDeletesPerf();
	}

	public void testBufferedDeletesPerfCase4() throws Exception {
		numIterations = 100;          // do that many iterations 
		numAddsPerIteration = 50;    // in each iteration, add such many docs
		numDeletesPerIteration = 10;  // then delete that many docs
		deleteGap = 2;               // this is the gap between deleted docs ids
		numMeasurements = 2;         // number of times each test is run, before taking the minimum time
		maxBufferredDocs = 5000;    
		doTestBufferedDeletesPerf();
	}

	public void testBufferedDeletesPerfCase5() throws Exception {
		numIterations = 100;          // do that many iterations 
		numAddsPerIteration = 200;    // in each iteration, add such many docs
		numDeletesPerIteration = 20;  // then delete that many docs
		deleteGap = 2;               // this is the gap between deleted docs ids
		numMeasurements = 2;         // number of times each test is run, before taking the minimum time
		maxBufferredDocs = 10000;    
		doTestBufferedDeletesPerf();
	}

	public void testBufferedDeletesPerfCase6() throws Exception {
		numIterations = 1000;          // do that many iterations 
		numAddsPerIteration = 300;    // in each iteration, add such many docs
		numDeletesPerIteration = 30;  // then delete that many docs
		deleteGap = 2;               // this is the gap between deleted docs ids
		numMeasurements = 2;         // number of times each test is run, before taking the minimum time
		maxBufferredDocs = 50000;    
		doTestBufferedDeletesPerf();
	}

	public void testBufferedDeletesPerfCase7() throws Exception {
		numIterations = 2000;          // do that many iterations 
		numAddsPerIteration = 400;    // in each iteration, add such many docs
		numDeletesPerIteration = 40;  // then delete that many docs
		deleteGap = 4;               // this is the gap between deleted docs ids
		numMeasurements = 2;         // number of times each test is run, before taking the minimum time
		maxBufferredDocs = 50000;    
		doTestBufferedDeletesPerf();
	}

	private void doTestBufferedDeletesPerf() throws Exception {
		printTestSetup();
		
		long t1 = Long.MAX_VALUE;
		long t2 = Long.MAX_VALUE;
		for (int i = 1; i <= numMeasurements; i++) {
			setStartModifier();
			long t1a = measureInterleavedAddRemove();
			System.out.println(t1a+" millis for IndexModifier (take "+i+")" );
			t1 = Math.min(t1, t1a);

			setStartNewModifier();
			long t2a = measureInterleavedAddRemove();
			System.out.println(t2a+" millis for NewIndexModifier (take "+i+")" );
			t2 = Math.min(t2, t2a);
		}

		long dt = t1-t2;
		int improvement = 100 - (int)((t2*100)/t1);
		System.out.println();
		if (dt>0) {
			System.out.println("New code is better by: "+dt+" millis, that is, by "+improvement+"%" );
		} else {
			System.out.flush();System.err.flush();
			Thread.sleep(500);
			System.err.println("New code is worse by: "+(-dt)+" millis, that is, by "+(-improvement)+"%" );
			System.err.flush();System.out.flush();
			Thread.sleep(500);
		}
		
		assertTrue("Orig IndexModifier faster by more than 1 second!",dt > -1000);
	}
	
	private void printTestSetup() {
		StringBuffer sb = new StringBuffer("Start: ");
		sb.append("max-buf-docs="+maxBufferredDocs+" - ");    
		sb.append(numIterations+" iterations: ");
		sb.append("each adding "+numAddsPerIteration+" docs ");
		sb.append("and deleting "+numDeletesPerIteration+" docs ");
		sb.append("(gap between deleted ids is "+ deleteGap);
		sb.append(". "+ numMeasurements+" measures taken;");
		System.out.println();
		System.out.println(sb);
		System.out.println();
	}

	private void setStartModifier() throws Exception {
		Directory dir = newDirectory();
		modifier = new IndexModifier(dir,new StandardAnalyzer(), true);
		modifier.setMaxBufferedDocs(maxBufferredDocs);
	}

	private static int nextDirectory = 0; 
	private FSDirectory newDirectory() throws IOException {
		return FSDirectory.getDirectory(new File(workDir,"index_"+(nextDirectory++)),true);
	}

	private void setStartNewModifier() throws Exception {
		Directory dir = newDirectory();
		newModifier = new NewIndexModifier(dir,new StandardAnalyzer(), true);
		newModifier.setMaxBufferedDocs(maxBufferredDocs);
	}

	private void doClose() throws Exception {
		if (modifier!=null) {
			modifier.close();
			modifier = null;
		} else if (newModifier!=null) {
			newModifier.close();
			newModifier = null;
		} else { 
			fail("both modifier and newModifier are null!!!???");
		}
	}

	private long measureInterleavedAddRemove() throws Exception {
		long startTime = System.currentTimeMillis(); 
		for (int i = 0; i < numIterations; i++) {
			doAddDocs();
			doDeleteDocs();
		}
		doOptimize();
		doClose();
		return System.currentTimeMillis() - startTime;
	}
	
	private void doAddDocs() throws Exception {
		for (int i = 0; i < numAddsPerIteration; i++) {
			doAddDoc();
		}
		//System.out.println(numAddsPerIteration+" documents were added.");
	}
	
	private void doAddDoc() throws Exception {
		if (modifier!=null) {
			modifier.addDocument(newDocument());
		} else if (newModifier!=null) {
			newModifier.addDocument(newDocument());
		} else { 
			fail("both modifier and newModifier are null!!!???");
		}
	}

	private void doDeleteDocs() throws Exception {
		int nDeleted = 0;
		int deletedId = nextDocId-1; 
		for (int i = 0; i < numDeletesPerIteration; i++) {
			deletedId -= deleteGap;
			if (deletedId<0) {
				break;
			}
			doDeleteDoc(new Term("docid", ""+deletedId));
			nDeleted ++;
		}
		//System.out.println(nDeleted+" documents were deleted.");
	}

	private void doDeleteDoc(Term term) throws Exception {
		if (modifier!=null) {
			modifier.deleteDocuments(term);
		} else if (newModifier!=null) {
			newModifier.deleteDocuments(term);
		} else { 
			fail("both modifier and newModifier are null!!!???");
		}
	}

	private void doOptimize() throws Exception {
		if (modifier!=null) {
			modifier.optimize();
		} else if (newModifier!=null) {
			newModifier.optimize();
		} else { 
			fail("both modifier and newModifier are null!!!???");
		}
	}
	
}
