package org.apache.lucene.index;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.LuceneTestCase;

public class TestRollback extends LuceneTestCase {
  
  private Directory dir = new RAMDirectory();
	private int numDocsToInsertInitially = 5;
	private int numUpdates = 3;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
    IndexWriter w = new IndexWriter(dir, new WhitespaceAnalyzer(), true, MaxFieldLength.UNLIMITED);
    for (int i = 0; i < numDocsToInsertInitially; i++) {
      Document doc = new Document();
      doc.add(new Field("pk", Integer.toString(i), Store.YES, Index.ANALYZED_NO_NORMS));
      w.addDocument(doc);
    }
    w.close();
	}
	
	// This test fails because doc buffer is less than number of updates
	public void testRollbackIntegrityWithBufferFlush() throws Exception {
		assertEquals("index should contain required number of docs ", numDocsToInsertInitially, numDocs());
		updateThenRollbackIndex(numUpdates, numUpdates - 1);
		assertEquals("index should contain same number of docs post rollback", numDocsToInsertInitially, numDocs());
	}
	
	//This test passes because doc buffer is greater than number of updates
	public void testRollbackIntegrityWithNoBufferFlush() throws Exception {
		assertEquals("index should contain required number of docs ", numDocsToInsertInitially,numDocs());
		updateThenRollbackIndex(numUpdates,numUpdates + 1);
		assertEquals("index should contain same number of docs post rollback", numDocsToInsertInitially,numDocs());
	}
	
	public int numDocs() throws Exception {
		IndexReader r = IndexReader.open(dir, true);
		try {
		  return r.numDocs();
		} finally {
		  r.close();
		}
	}

	// updates a selection of docs
 	private void updateThenRollbackIndex(int numDocsToUpdate, int docBufferSize) throws Exception {
		IndexWriter w = new IndexWriter(dir, new WhitespaceAnalyzer(), false, MaxFieldLength.UNLIMITED);
		// If buffer size is small enough to cause a flush, errors ensue...
		w.setMaxBufferedDocs(docBufferSize);
		Term pkTerm = new Term("pk", "");
		for (int i = 0; i < numDocsToUpdate; i++) {
			Document doc = new Document();
			String value = Integer.toString(i);
			doc.add(new Field("pk", value, Store.YES, Index.ANALYZED_NO_NORMS));
			doc.add(new Field("text", "foo", Store.YES, Index.ANALYZED_NO_NORMS));
			w.updateDocument(pkTerm.createTerm(value), doc);
		}
		w.rollback();
		w.close();
	}

}
