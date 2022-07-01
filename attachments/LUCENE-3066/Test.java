package org.apache.lucene.index;

import java.io.IOException;
import java.util.Collection;


import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.LockFactory;



public class Test 
{
	public static void main(String[] args) throws Exception {
		Test tt = new Test();
		tt.test();
    }

	boolean ExposeBug = true;
	int ManyIterations = 100000000;
	SegmentInfo Si = null;
	
	public void test() throws Exception {
		Directory dir = new MyDirectory();

		Si = new SegmentInfo("name", 0, dir, true, true, true, true);
		
		
		if( ExposeBug) {
			Thread one = new MyThread(1);
			Thread two = new MyThread(2);
			one.start();
			two.start();
			one.join();
			two.join();
		}
		else {
			int fewIterations = ManyIterations/100;
			for(int i = 0 ;i < fewIterations; i++) {
				Si.sizeInBytes(false);
				Si.setHasVectors(true);
				Si.setHasVectors(true);
				Si.sizeInBytes(false);
				Si.setHasVectors(true);
			}
		}
	}
	public class MyThread extends Thread {
		int _tid;
		public MyThread(int tid) {
			_tid = tid;
		}

		public void run() {
			try {
				for(int i = 0; i < ManyIterations;i++)
				{
					if(_tid == 1) {
						Si.sizeInBytes(false);
					}
					if (_tid == 2) {
						Si.setHasVectors(true);
					}
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
  }
  
  
  class MyDirectory extends Directory {
		 public String[] listAll() throws IOException {
			 String[] rez = {};
			 return rez;
		 }
		 public boolean fileExists(String name) throws IOException {
			 return true;
		 }
		 public long fileModified(String name) throws IOException {
			 return 1;
		 }
		 public void touchFile(String name) throws IOException {}
		 public void deleteFile(String name) throws IOException {}
		 public long fileLength(String name) throws IOException {
			 return 1;
		 }
		 public IndexOutput createOutput(String name) throws IOException {
			 assert false: "ERR: not yet implemented";
			 return null;
		 }
		 public void sync(Collection<String> names) throws IOException {}
		 public IndexInput openInput(String name) throws IOException {
			 assert false: "ERR: not implemented";
			 return null;
		 }
		 public IndexInput openInput(String name, int bufferSize) throws IOException {
			 assert false: "ERR: not implemented";
			 return null;
		 }
		 public void clearLock(String name) throws IOException {}
		 public void close() throws IOException {}
		 public void setLockFactory(LockFactory lockFactory) throws IOException {}
		 public String getLockID() {
			 assert false: "ERR: not implemented";
			 return null;
		 }
		 public String toString() {
			 return "a b c";
		 }
		 public void copy(Directory to, String src, String dest) throws IOException {}
  }
  
}
