/**
 * 
 */
package test;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexSplitter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * @author ivasilev
 *
 */
public class TestIndexSplitter {
	
	private static File INDEX_PATH = new File("E:/Temp/ContribIndexSpliter/index");
	private static File INDEX_SPLIT_PATH = new File("E:/Temp/ContribIndexSpliter/splitIndex");
	
	public static void main(String[] args) throws IOException {
		initDirs();
		createIndex();
		splitIndex();
		deleteFirstDocAndOptimize(INDEX_SPLIT_PATH); // might throw exception
		readIndex(INDEX_SPLIT_PATH); // surely throws Exception
	}
	
	private static void initDirs() {
		initDir(INDEX_PATH);
		initDir(INDEX_SPLIT_PATH);
	}
	
	private static void initDir(File dir) {
		if ( ! dir.exists()) {
			dir.mkdirs();
		}
		for (File currFile : dir.listFiles()) {
			if (currFile.isFile()) {
				currFile.delete();
			}
		}
	}
	
	private static void createIndex() throws IOException {
		IndexWriter iw = null;
		try {
			IndexWriterConfig iwConfig = new IndexWriterConfig(Version.LUCENE_32,
				new StandardAnalyzer(Version.LUCENE_32));
			iwConfig.setOpenMode(OpenMode.CREATE);
			iw = new IndexWriter(FSDirectory.open(INDEX_PATH), iwConfig);
			Document doc = new Document();
			doc.add(new Field("content", "doc 1", Store.YES, Index.ANALYZED_NO_NORMS));
			iw.addDocument(doc);
			doc = new Document();
			doc.add(new Field("content", "doc 2", Store.YES, Index.ANALYZED_NO_NORMS));
			iw.addDocument(doc);
			iw.close();
		} finally {
			if (iw != null) {
				iw.close();
			}
		}
	}
	
	private static void splitIndex() throws IOException {
		IndexSplitter is = new IndexSplitter(INDEX_PATH);
		is.split(INDEX_SPLIT_PATH, new String[] { "_0" });
	}
	
	private static void deleteFirstDocAndOptimize(File indexDir) throws IOException {
		IndexReader ir = null;
		IndexWriter iw = null;
		try {
			ir = IndexReader.open(FSDirectory.open(indexDir), false);
			ir.deleteDocument(0);
			ir.close();
			IndexWriterConfig iwConfig = new IndexWriterConfig(Version.LUCENE_32,
				new StandardAnalyzer(Version.LUCENE_32));
			iw = new IndexWriter(FSDirectory.open(indexDir), iwConfig);
			iw.optimize();
		} finally {
			if (ir != null) {
				ir.close();
			}
			if (iw != null) {
				iw.close();
			}
		}
	}
	
	private static void readIndex(File indexDir) throws IOException {
		IndexReader ir = null;
		try {
			ir = IndexReader.open(FSDirectory.open(indexDir));
			System.out.println(indexDir.getPath() + " index -> ir.numDocs = " + ir.numDocs());
		} finally {
			if (ir != null) {
				ir.close();
			}
		}
	}
	
}
