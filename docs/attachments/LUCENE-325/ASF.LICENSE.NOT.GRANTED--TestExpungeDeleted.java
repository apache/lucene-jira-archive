/*
 * Created on Dec 14, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.apache.lucene.index;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * @author jwang
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class TestExpungeDeleted {
	private File srcDir;

	private File indexDir;

	/**
	 *  
	 */
	public TestExpungeDeleted() {
		super();
		// TODO Auto-generated constructor stub
	}

	private void indexDocs(IndexWriter iw, File file) throws IOException{
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (int i = 0; i < files.length; ++i) {
				indexDocs(iw, files[i]);
			}
		} else {
			try{
				Document doc = new Document();
				doc.add(Field.Keyword("key",file.getAbsolutePath()));
				doc.add(Field.Text("content",new FileReader(file)));
				iw.addDocument(doc);
			}
			catch(FileNotFoundException fe){
				return;
			}
		}
	}

	public void setUp() throws IOException {
		srcDir = new File("C:/entopia/content/unit_tests/smaller.latimes");

		indexDir = new File("C:/entopia/home/testexp/");
		indexDir.mkdirs();
	}
	
	private void doIndexing(boolean create) throws IOException{
		IndexWriter iw=null;
		try{
			iw=new IndexWriter(indexDir,new StandardAnalyzer(),create);
			System.out.println("indexing docs...");
			indexDocs(iw,srcDir);
			System.out.println("done indexing docs...");
			
		}
		finally{
			if (iw!=null){
				iw.close();
			}
		}
	}

	public void testExpungeDeleted() throws IOException{
		doIndexing(true);
		doIndexing(false);	// do it again to create more segments

		IndexReader ir=IndexReader.open(indexDir);
		int maxDoc=ir.maxDoc();
		System.out.println("maxdoc before delete: "+maxDoc);
		ir.delete(0);	// delete in first segment
		ir.delete(maxDoc-2);	// delete in last segment
		ir.close();		
		
		IndexWriter iw=new IndexWriter(indexDir,new StandardAnalyzer(),false);
		iw.expungeDeleted();
		iw.close();
		
		ir=IndexReader.open(indexDir);
		maxDoc=ir.maxDoc();
		System.out.println("maxdoc before after: "+maxDoc);
		ir.close();
	}

	public static void main(String[] args) throws Exception {
		/** Main for running test case by itself. */

		TestExpungeDeleted test=new TestExpungeDeleted();
		test.setUp();
		test.testExpungeDeleted();
	}

}
