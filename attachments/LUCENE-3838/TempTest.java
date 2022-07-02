import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;

public class TempTest {

    private Analyzer _analyzer = new KeywordAnalyzer();

    @Test
    public void testIndex() throws Exception {
	File indexDir = new File("sample-index");
	if (indexDir.exists()) {
	    indexDir.delete();
	}

	FSDirectory index = FSDirectory.open(indexDir);

	Document doc;

	IndexWriter writer = createWriter(index, true);
	try {
	    doc = new Document();
	    doc.add(new Field("field", "text0", Field.Store.YES,
		    Field.Index.ANALYZED));
	    writer.addDocument(doc);

	    doc = new Document();
	    doc.add(new Field("field", "text1", Field.Store.YES,
		    Field.Index.ANALYZED));
	    writer.addDocument(doc);

	    doc = new Document();
	    doc.add(new Field("field", "text2", Field.Store.YES,
		    Field.Index.ANALYZED));
	    writer.addDocument(doc);

	    writer.commit();
	} finally {
	    writer.close();
	}

	IndexReader reader = IndexReader.open(index, false);
	try {
	    reader.deleteDocument(1);
	} finally {
	    reader.close();
	}

	writer = createWriter(index, false);
	try {
	    for (int i = 3; i < 100; i++) {
		doc = new Document();
		doc.add(new Field("field", "text" + i, Field.Store.YES,
			Field.Index.ANALYZED));
		writer.addDocument(doc);

		writer.commit();
	    }
	} finally {
	    writer.close();
	}

	boolean deleted;
	String text;

	reader = IndexReader.open(index, true);
	try {
	    deleted = reader.isDeleted(1);
	    text = reader.document(1).get("field");
	} finally {
	    reader.close();
	}

	assertTrue(deleted); // This line breaks
	assertEquals("text1", text);
    }

    private MergePolicy createEngineMergePolicy() {
	LogDocMergePolicy mergePolicy = new LogDocMergePolicy();

	mergePolicy.setCalibrateSizeByDeletes(false);
	mergePolicy.setUseCompoundFile(true);
	mergePolicy.setNoCFSRatio(1.0);

	return mergePolicy;
    }

    private IndexWriter createWriter(Directory index, boolean create)
	    throws Exception {
	IndexWriterConfig iwConfig = new IndexWriterConfig(Version.LUCENE_35,
		_analyzer);

	iwConfig.setOpenMode(create ? IndexWriterConfig.OpenMode.CREATE
		: IndexWriterConfig.OpenMode.APPEND);
	iwConfig.setMergePolicy(createEngineMergePolicy());
	iwConfig.setMergeScheduler(new ConcurrentMergeScheduler());

	return new IndexWriter(index, iwConfig);
    }

}
