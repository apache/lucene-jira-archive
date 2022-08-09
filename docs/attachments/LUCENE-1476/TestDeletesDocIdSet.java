package org.apache.lucene.index;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NativeFSLockFactory;

public class TestDeletesDocIdSet {
  Directory dir;
  int max = 15000;
  
  public static void main(String[] args) throws Exception {
    new TestDeletesDocIdSet();
  }

  public TestDeletesDocIdSet() throws Exception {
    String tempDir = System.getProperty("java.io.tmpdir");
    File file = new File(tempDir, "tddis");
    dir = new FSDirectory(file, new NativeFSLockFactory(file));
    IndexReader ir = IndexReader.open(dir);
    System.out.println("numDeletedDocs: "+ir.numDeletedDocs()+" numDocs: "+ir.numDocs());
    ir.close();
    //createIndex();
    //createDeletes();
    searchAll("warmup");
    searchAll("final");
  }
  
  public void searchAll(String name) throws Exception {
    long startTime = System.currentTimeMillis();
    search(10000);
    long duration = System.currentTimeMillis() - startTime;
    System.out.println(name+" search duration: "+duration);
  }
  
  public void search(int n) throws Exception {
    IndexReader ir = IndexReader.open(dir);
    IndexSearcher searcher = new IndexSearcher(ir);
    for (int x=0; x < n; x++) {
      Query q = new TermQuery(new Term("text", "text"));
      TopDocs td = searcher.search(q, 10);
      //System.out.println("search totalHits: "+td.totalHits);
    }
    ir.close();
  }
  
  public void createDeletes() throws Exception {
    IndexReader ir = IndexReader.open(dir);
    for (int x=0; x < max; x++) {
      if (x % 8 == 0) {
        ir.deleteDocument(x);
      }
    }
    //System.out.println("numDeletedDocs: "+ir.numDeletedDocs());
    ir.close();
  }
  
  public void createIndex() throws Exception {
    IndexWriter writer = new IndexWriter(dir, new WhitespaceAnalyzer(),
        IndexWriter.MaxFieldLength.LIMITED);
    for(int j=0;j<max;j++) {
      addDocWithIndex(writer, 25+j);
    }
    writer.commit();
    writer.close();
  }
  
  private void addDocWithIndex(IndexWriter writer, int index)
      throws IOException {
    Document doc = new Document();
    doc.add(new Field("content", "aaa " + index, Field.Store.YES,
        Field.Index.ANALYZED));
    doc.add(new Field("id", "" + index, Field.Store.YES, Field.Index.ANALYZED));
    doc.add(new Field("text", "text " + index, Field.Store.YES, Field.Index.ANALYZED));
    writer.addDocument(doc);
  }
}
