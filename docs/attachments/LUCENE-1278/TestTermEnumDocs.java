package org.apache.lucene.index;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

// TODO: test multi segment reader
public class TestTermEnumDocs {
	Directory directory;
	File file = new File("g:\\testtermenumdocs");

	public static void main(String[] args) {
		try {
			new TestTermEnumDocs();
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public TestTermEnumDocs() throws Exception {
		file.mkdirs();
		directory = FSDirectory.getDirectory(file, true);
		makeDocs();
		//readDocs();
		testSpeedTermEnum();
		//testSpeedTermDocs();
	}
  
	public void testSpeedTermEnum() throws Exception {
		String field = "tag".intern();
		long start = System.currentTimeMillis();
		IndexReader indexReader = IndexReader.open(directory);
    TermEnum termEnum = indexReader.terms(new Term(field, ""), true);
    try {
      do {
        Term term = termEnum.term();
        if (term==null || term.field() != field) break;
        int[] docs = termEnum.docs();
      } while (termEnum.next());
    } finally {
      termEnum.close();
    }
		long duration = System.currentTimeMillis() - start;
		System.out.println("duration: "+duration);
	}
	
	public void testSpeedTermDocs() throws Exception {
		String field = "tag".intern();
		long start = System.currentTimeMillis();
		IndexReader indexReader = IndexReader.open(directory);
		TermDocs termDocs = indexReader.termDocs();
    TermEnum termEnum = indexReader.terms(new Term(field, ""));
    try {
      do {
        Term term = termEnum.term();
        if (term==null || term.field() != field) break;
        termDocs.seek(termEnum);
        while (termDocs.next()) {
          int doc = termDocs.doc();
        }
      } while (termEnum.next());
    } finally {
      termDocs.close();
      termEnum.close();
    }
		
		long duration = System.currentTimeMillis() - start;
		System.out.println("duration: "+duration);
	}
	
	public void readDocs() throws Exception {
		IndexReader indexReader = IndexReader.open(directory);
		TermEnum termEnum = indexReader.terms(true);
		String field = "tag".intern();
		do {
			Term term = termEnum.term();
			// if (term == null || term.field() != field) break;

			int[] docs = termEnum.docs();
			String docsString = "";
			if (docs != null) {
				for (int x = 0; x < docs.length; x++) {
					docsString += Integer.toString(docs[x])+",";
				}
			}
			System.out.println("term: " + term + " docs: " + docsString
					+ " docfreq: " + termEnum.docFreq());
		} while (termEnum.next());
		indexReader.close();
	}

	public void makeDocs() throws IOException {
		IndexWriter indexWriter = new IndexWriter(directory,
				new WhitespaceAnalyzer(), true, IndexWriter.MaxFieldLength.UNLIMITED);
		indexWriter.setRAMBufferSizeMB(25.0);
		indexWriter.setMaxBufferedDocs(IndexWriter.DISABLE_AUTO_FLUSH);
		fillIndex(indexWriter);
		//indexWriter.flush(false, true, true);
		//fillIndex(indexWriter);
		//indexWriter.flush(false, true, true);
		//indexWriter.optimize();
		indexWriter.close(true);
		System.out.println("made docs");
	}

	public void fillIndex(IndexWriter indexWriter) throws IOException {
		for (int x = 0; x < 3000000; x++) {
			for (int i = 0; i < 3; i++) {
				Document document = new Document();
				document.add(new Field("tag", "dog" + x, Store.YES, Index.UN_TOKENIZED,
						Field.TermDocs.STORE));
				indexWriter.addDocument(document);
			}
			//Document document = new Document();
			//document.add(new Field("text", "dog" + x, Store.YES, Index.UN_TOKENIZED,
			//		TermDocs.NO));
			//indexWriter.addDocument(document);
		}
	}
}
