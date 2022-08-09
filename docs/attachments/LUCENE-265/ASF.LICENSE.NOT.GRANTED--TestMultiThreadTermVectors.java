/*
 * Created on 15.08.2004
 *
 */
package org.apache.lucene.search;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.English;

/**
 * @author Bernhard Messer
 * @version $rcs = ' $Id: Exp $ ' ;
 */
public class TestMultiThreadTermVectors extends TestCase {
	private IndexReader reader;
  private RAMDirectory directory = new RAMDirectory();
	
  public TestMultiThreadTermVectors(String s) {
    super(s);
  }
  
  public void setUp() throws Exception {
  	IndexWriter writer
            = new IndexWriter(directory, new SimpleAnalyzer(), true);
  	//writer.setUseCompoundFile(true);
  	//writer.infoStream = System.out;
  	for (int i = 0; i < 1000; i++) {
    	Document doc = new Document();
    	Field fld = new Field("field", English.intToEnglish(i), true, true, false, true);
    	doc.add(fld);
    	writer.addDocument(doc);
  	}
  	writer.close();
  	
  	reader = IndexReader.open(directory);
	}
	
	public void test() {
  	assertTrue(reader != null);
  	
  	testTermPositionVectors(1);
  	testTermPositionVectors(2);
  	testTermPositionVectors(4);
  	testTermPositionVectors(6);
  	testTermPositionVectors(8);
  	testTermPositionVectors(10);
  	
  	/** close the opened reader */
		try {
			reader.close();
		}
		catch (IOException ioe) {
			fail(ioe.getMessage());
		}
	}
	
	public void testTermPositionVectors(int threadCount) {
		MultiThreadTermVectorsReader[] mtr = new MultiThreadTermVectorsReader[threadCount];
		for (int i = 0; i < threadCount; i++) {
			mtr[i] = new MultiThreadTermVectorsReader();
			mtr[i].init(reader);
		}
		
		
		/** run until all threads finished */ 
		int threadsAlive = mtr.length;
		while (threadsAlive > 0) {
			try {
				//System.out.println("Threads alive");
				Thread.sleep(10);
				threadsAlive = mtr.length;
				for (int i = 0; i < mtr.length; i++) {
					if (mtr[i].isAlive() == true) {
						break;
					}
					
					threadsAlive--; 
					
			}
				
			} catch (InterruptedException ie) {} 
		}
		
		long totalTime = 0L;
		for (int i = 0; i < mtr.length; i++) {
			totalTime += mtr[i].timeElapsed;
			mtr[i] = null;
		}
		
		//System.out.println("threadcount: " + mtr.length + " average term vector time: " + totalTime/mtr.length);
		
	}
	
}

class MultiThreadTermVectorsReader implements Runnable {
	
	private IndexReader reader = null;
	private Thread t = null;
	
	private final int runsToDo = 100;
	long timeElapsed = 0;
	
	
	public void init(IndexReader reader) {
		this.reader = reader;
		timeElapsed = 0;
		t=new Thread(this);
		t.start();
	}
		
	public boolean isAlive() {
		if (t == null) return false;
		
		return t.isAlive();
	}
	
	public void run() {
			try {
				// run the test 100 times
				for (int i = 0; i < runsToDo; i++)
					testTermVectors();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			return;
	}
	
	private void testTermVectors() throws Exception {
		// check:
		int numDocs = reader.numDocs();
		long start = 0L;
		for (int docId = 0; docId < numDocs; docId++) {
			start = System.currentTimeMillis();
			TermFreqVector [] vectors = reader.getTermFreqVectors(docId);
			timeElapsed += System.currentTimeMillis()-start;
			
			// verify vectors result
			verifyVectors(vectors, docId);
			
			start = System.currentTimeMillis();
			TermFreqVector vector = reader.getTermFreqVector(docId, "field");
			timeElapsed += System.currentTimeMillis()-start;
			
			vectors = new TermFreqVector[1];
			vectors[0] = vector;
			verifyVectors(vectors, docId);
			
		}
	}
	
	private void verifyVectors(TermFreqVector[] vectors, int num) {
		StringBuffer temp = new StringBuffer();
		String[] terms = null;
		for (int i = 0; i < vectors.length; i++) {
			terms = vectors[i].getTerms();
			for (int z = 0; z < terms.length; z++) {
				temp.append(terms[z]);
			}
		}
		
		if (!English.intToEnglish(num).trim().equals(temp.toString().trim()))
				System.out.println("worng term result");
	}
}
