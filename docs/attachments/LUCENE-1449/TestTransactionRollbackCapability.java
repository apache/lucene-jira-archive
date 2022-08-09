package org.apache.lucene.index;
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

/**
 * Test class to highlight issues in using IndexDeletionPolicy to provide multi-level rollback capability.
 * This test case creates an index of records 1 to 100, introducing a commit point every 10 records.
 * 
 * A "keep all" deletion policy is used to ensure we keep all commit points for testing purposes
 * 
 * 
 * 
 * @author MAHarwood
 *
 */
public class TestTransactionRollbackCapability extends TestCase {
	
	private static final String FIELD_RECORD_ID = "record_id";
	private Directory dir;
	
	/**
	 * Currently highlights an issue with the first rollback - we call IndexCommit.delete on the right
	 * commit point (the one that added records 91-100) but index still contains records 1-100
	 */
	public void testFirstRollBack() throws Exception {
		
		assertEquals("Index should have records up to #100",100,getLastRecordId());
//		showAvailableCommitPoints();
		
		//Roll back last commit point (which added records 91-100)
		IndexWriter w = new IndexWriter(dir,new WhitespaceAnalyzer(),
				new RollbackDeletionPolicy(),MaxFieldLength.UNLIMITED);
		w.close();
		assertEquals("Index should have records up to #90",90,getLastRecordId());
	}	
	
	/**
	 * Highlights an issue with all "latest commit point" rollbacks - the segments.gen
	 * file is not updated to regress back to segments_N-1.  
	 * 
	 * @throws Exception
	 */
	public void testRepeatedRollBacksAcknowledgingFirstRollbackBug() throws Exception {		
		assertEquals("Index should have records up to #100",100,getLastRecordId());
		IndexWriter w = new IndexWriter(dir,new WhitespaceAnalyzer(),
				new RollbackDeletionPolicy(),MaxFieldLength.UNLIMITED);
		w.close();
		assertEquals("While we still have a bug - the first rollback should not work: #100",100,getLastRecordId());

		int expectedLastRecordId=100;
		while(IndexReader.listCommits(dir).size()>1)
		{
			expectedLastRecordId-=10;
			w = new IndexWriter(dir,new WhitespaceAnalyzer(),
					new RollbackDeletionPolicy(),MaxFieldLength.UNLIMITED);
			w.close();
			Exception readError=null;
			try
			{
				assertEquals("Index should have records up to #"+expectedLastRecordId,expectedLastRecordId,getLastRecordId());
			}
			catch(FileNotFoundException e)
			{
				readError=e;
			}
			assertEquals("Should not get a missing file error - " +
					"segments.gen needs to be updated with previous segment_N id if" +
					" the latest commit point is deleted ", null,readError);				
			
//			showAvailableCommitPoints();			
		}
	}
	/**
	 * Acknowledging and coding around the issues highlighted in previous tests (first rollback fails/segments.gen
	 * not updated) this test shows it is possible to rollback to previous versions.
	 * 
	 * @throws Exception
	 */
	public void testRepeatedRollBacksAcknowledgingFirstRollbackAndSegmentsGenBug() throws Exception {		
		assertEquals("Index should have records up to #100",100,getLastRecordId());
		IndexWriter w = new IndexWriter(dir,new WhitespaceAnalyzer(),
				new RollbackDeletionPolicy(),MaxFieldLength.UNLIMITED);
		w.close();
//		//Segments.gen is invalidated by deleting topmost commitpoint - need to delete it 
		deleteSegementsGen();
		
		assertEquals("While we still have a bug - the first rollback should not work: #100",100,getLastRecordId());

		int expectedLastRecordId=100;
		while(IndexReader.listCommits(dir).size()>1)
		{
			expectedLastRecordId-=10;
			w = new IndexWriter(dir,new WhitespaceAnalyzer(),
					new RollbackDeletionPolicy(),MaxFieldLength.UNLIMITED);
			w.close();
			assertEquals("Index should have records up to #"+expectedLastRecordId,expectedLastRecordId,getLastRecordId());
			
//			//Segments.gen is invalidated by deleting topmost commitpoint - need to delete it 
			deleteSegementsGen();
		
//			showAvailableCommitPoints();			
		}
	}
	
	void deleteSegementsGen()
	{
		try
		{
			dir.deleteFile("segments.gen");
		}
		catch(Exception ignore){}
	}
	

	private int getLastRecordId() throws Exception{
		IndexReader r = IndexReader.open(dir);
		
		//Perhaps not the most efficient approach but meets our needs here.
		int biggestNum=0;
		for (int i = 0; i < r.maxDoc(); i++) 
		{
			if(!r.isDeleted(i))
			{
				int val=Integer.parseInt(r.document(i).get(FIELD_RECORD_ID));
				biggestNum=Math.max(biggestNum, val);				
			}
		}
		r.close();
		return biggestNum;
	}

	private void showAvailableCommitPoints() throws Exception {
		Collection commits = IndexReader.listCommits(dir);
		for (Iterator iterator = commits.iterator(); iterator.hasNext();) {
			IndexCommit comm = (IndexCommit) iterator.next();
			System.out.print("\t Available commit point:["+comm.getUserData()+"] files=");
			Collection files = comm.getFileNames();
			for (Iterator iterator2 = files.iterator(); iterator2.hasNext();) {
				String filename = (String) iterator2.next();
				System.out.print(filename+", ");				
			}
			System.out.println();
		}
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	protected void setUp() throws Exception {
		dir=new RAMDirectory();
		
		//Build index, of records 1 to 100, committing after each batch of 10
		IndexDeletionPolicy sdp=new KeepAllDeletionPolicy();
		IndexWriter w=new IndexWriter(dir,new WhitespaceAnalyzer(),sdp,MaxFieldLength.UNLIMITED);
		int firstRecordIdInThisTransaction=1;
		for(int currentRecordId=1;currentRecordId<=100;currentRecordId++)
		{
			Document doc=new Document();
			doc.add(new Field(FIELD_RECORD_ID,""+currentRecordId,Field.Store.YES,Field.Index.ANALYZED));
			w.addDocument(doc);
			
			if(currentRecordId%10==0)
			{
				String userData="records 1-"+currentRecordId;
//				System.out.println("Committing "+userData);
				w.commit(userData);
				w.optimize();
			}
		}
		w.close();
		
	}

	//Rolls back to previous commit point
	class RollbackDeletionPolicy implements IndexDeletionPolicy
	{
		public void onCommit(List commits) throws IOException {
		}

		public void onInit(List commits) throws IOException {
			IndexCommit commit=(IndexCommit)commits.get(commits.size()-1);
			
//			System.out.print("\tRolling back last commit point:" +
//					" UserData="+commit.getUserData() +")  ("+(commits.size()-1)+" commit points left) files=");
//			Collection files = commit.getFileNames();
//			for (Iterator iterator = files.iterator(); iterator.hasNext();) {
//				System.out.print(" "+iterator.next());				
//			}
//			System.out.println();
			
			commit.delete();			
		}		
	}
	
	//Keeps all commit points (used to build index)
	class KeepAllDeletionPolicy implements IndexDeletionPolicy{
		public void onCommit(List commits) throws IOException {	}	
		public void onInit(List commits) throws IOException {}
	}
	
	
}
