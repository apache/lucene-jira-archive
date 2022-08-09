package org.apache.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util._TestUtil;

public class TestAddIndexes extends LuceneTestCase {

	private String hotIndexPath;
	private String coldIndexPath;

	// not important
	private int hotDocCount = 0;

	// as long as cold doc count is greater than
	// IndexWriter.getMaxBufferedDocs(), then
	// IndexWriter.AddIndexes(IndexReader[])
	// will optimize prior to adding indexes
	// and the optimize will deadlock
	private int coldDocCount = 12;

	public void setUp() throws Exception {
		super.setUp();

		String tmpPath = System.getProperty("tempDir", "");
		coldIndexPath = new java.io.File(tmpPath, "TestDoug2-cold")
				.getCanonicalPath();
		hotIndexPath = new java.io.File(tmpPath, "TestDoug2-hot")
				.getCanonicalPath();

		// in case test thread is killed and tearDown() was never called
		if (new java.io.File(coldIndexPath).exists())
			_TestUtil.rmDir(coldIndexPath);
		if (new java.io.File(hotIndexPath).exists())
			_TestUtil.rmDir(hotIndexPath);
	}

	public void tearDown() throws Exception {
		super.tearDown();
		_TestUtil.rmDir(coldIndexPath);
		_TestUtil.rmDir(hotIndexPath);
	}

	public void testAddIndexesByIndexReader() throws Exception {
		//
		// DEADLOCK:
		//
		// 1) AddIndexes(IndexReader[]) acquires the write lock,
		// then begins optimization of destination index.
		//
		// 2) Main thread starts a ConcurrentMergeScheduler.MergeThread
		// to merge the 2 segments.
		//
		// 3) Merging thread tries to acquire the read lock at
		// IndexWriter.blockAddIndexes(boolean) in
		// IndexWriter.StartCommit(), but cannot as...
		//
		// 4) Main thread still holds the write lock, and is
		// waiting for the runningMerges data structure
		// to be devoid of merges with their optimize flag
		// set.
		//

		createIndex(coldIndexPath, 100000, coldDocCount);
		createIndex(hotIndexPath, 200000, hotDocCount);

		IndexWriter writer = getWriter(coldIndexPath);
		writer.setInfoStream(System.out);

		IndexReader[] readers = new IndexReader[] { IndexReader
				.open(hotIndexPath) };

		try {
			writer.addIndexes(readers);
		} finally {
			writer.close();
			readers[0].close();
		}

		_TestUtil.checkIndex(FSDirectory.getDirectory(coldIndexPath));

		IndexReader reader = IndexReader.open(coldIndexPath);
		assertEquals("Index should contain " + (coldDocCount + hotDocCount)
				+ "{0} documents.", coldDocCount + hotDocCount, reader
				.numDocs());
		reader.close();
	}

	public void testAddIndexesByIndexReaderWithExplicitOptimize()
			throws Exception {

		createIndex(coldIndexPath, 100000, coldDocCount);
		createIndex(hotIndexPath, 200000, hotDocCount);

		IndexWriter writer = getWriter(coldIndexPath);
		writer.setInfoStream(System.out);

		IndexReader[] readers = new IndexReader[] { IndexReader
				.open(hotIndexPath) };

		try {
			writer.optimize();
			writer.addIndexes(readers);
		} finally {
			writer.close();
			readers[0].close();
		}

		_TestUtil.checkIndex(FSDirectory.getDirectory(coldIndexPath));

		IndexReader reader = IndexReader.open(coldIndexPath);
		assertEquals("Index should contain " + (coldDocCount + hotDocCount)
				+ "{0} documents.", coldDocCount + hotDocCount, reader
				.numDocs());
		reader.close();
	}

	public void testAddIndexesByDirectory() throws Exception {

		createIndex(coldIndexPath, 100000, coldDocCount);
		createIndex(hotIndexPath, 200000, hotDocCount);

		IndexWriter writer = getWriter(coldIndexPath);
		writer.setInfoStream(System.out);

		Directory[] directories = new Directory[] { FSDirectory
				.getDirectory(hotIndexPath) };

		try {
			writer.addIndexes(directories);
		} finally {
			writer.close();
			directories[0].close();
		}

		_TestUtil.checkIndex(FSDirectory.getDirectory(coldIndexPath));

		IndexReader reader = IndexReader.open(coldIndexPath);
		assertEquals("Index should contain " + (coldDocCount + hotDocCount)
				+ "{0} documents.", coldDocCount + hotDocCount, reader
				.numDocs());
		reader.close();
	}

	public void testAddIndexesNoOptimize() throws Exception {

		createIndex(coldIndexPath, 100000, coldDocCount);
		createIndex(hotIndexPath, 200000, hotDocCount);

		IndexWriter writer = getWriter(coldIndexPath);
		writer.setInfoStream(System.out);

		Directory[] directories = new Directory[] { FSDirectory
				.getDirectory(hotIndexPath) };

		try {
			writer.addIndexesNoOptimize(directories);
		} finally {
			writer.close();
			directories[0].close();
		}

		_TestUtil.checkIndex(FSDirectory.getDirectory(coldIndexPath));

		IndexReader reader = IndexReader.open(coldIndexPath);
		assertEquals("Index should contain " + (coldDocCount + hotDocCount)
				+ "{0} documents.", coldDocCount + hotDocCount, reader
				.numDocs());
		reader.close();
	}

	private IndexWriter getWriter(String directory) throws Exception {
        // Note that the deadlock only occurs when autocommit == true
		// (deprecated constructor)
		IndexWriter writer = new IndexWriter(directory, new StandardAnalyzer());
		writer.setMaxBufferedDocs(coldDocCount - 1);
		return writer;
	}

	private void createIndex(String path, int baseId, int count)
			throws Exception {
		IndexWriter writer = getWriter(path);
		for (int i = 1; i <= count; i++)
			writer.addDocument(createDocument(baseId + i));
		writer.close();
	}

	private Document createDocument(int id) {
		Document doc = new Document();
		doc.add(new Field("field", id + "", Field.Store.YES,
				Field.Index.ANALYZED, Field.TermVector.NO));
		return doc;
	}

}