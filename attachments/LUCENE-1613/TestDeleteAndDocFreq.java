package org.apache.lucene.index;

import java.io.IOException;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.LuceneTestCase;

public class TestDeleteAndDocFreq extends LuceneTestCase {
	Directory dir = new RAMDirectory();

  public void testTermEnum() throws IOException
  {
    IndexWriter writer = null;

    writer  = new IndexWriter(dir, new WhitespaceAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);

    for (int i = 0; i < 100; i++) {
      Document doc = new Document();
      doc.add(new Field("content", "test", Field.Store.NO, Field.Index.ANALYZED));
      writer.addDocument(doc);
    }

    writer.close();

    assertEquals(100, getDocFreq());
    
    // merge segments by optimizing the index
    writer = new IndexWriter(dir, new WhitespaceAnalyzer(), false, IndexWriter.MaxFieldLength.LIMITED);
    writer.deleteDocuments(new Term("contents","test"));
    writer.close();

    assertEquals(0, getDocFreq());
    
    writer  = new IndexWriter(dir, new WhitespaceAnalyzer(), true, IndexWriter.MaxFieldLength.LIMITED);

    for (int i = 0; i < 100; i++) {
      Document doc = new Document();
      doc.add(new Field("content", "test", Field.Store.NO, Field.Index.ANALYZED));
      writer.addDocument(doc);
    }
    
    writer.close();

    assertEquals(100, getDocFreq());
  }
  
  private int getDocFreq() throws IOException{
    IndexReader reader = IndexReader.open(dir);
    TermEnum termEnum = null;
	termEnum = reader.terms(new Term("contents","test"));
	int df = termEnum.docFreq();
	termEnum.close();
	return df;
  }
}
