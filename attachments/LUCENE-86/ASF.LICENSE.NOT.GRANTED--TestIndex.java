package org.apache.lucene.index;

import junit.framework.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.document.*;
import org.apache.lucene.store.*;
import org.apache.lucene.search.*;
import java.io.*;

public class TestIndex extends TestCase {

  public TestIndex(String s) {
    super(s);
  }

  public void testError() throws IOException {
    Directory directory = new RAMDirectory();

    Analyzer a = new WhitespaceAnalyzer();
    Document idoc;
    IndexWriter iw;
    IndexReader ir;

    // empty the directory
    iw = new IndexWriter(directory, a, true);
    iw.close();

    for( int k=0; k<50; k++ ) {
      System.out.println( "run # " + k );
      // add a document
      iw = new IndexWriter(directory, a, false);
      try {
        idoc = new Document();
        idoc.add( Field.Text("number", "abc") );
        iw.addDocument(idoc);
        iw.close();

//         iw = new IndexWriter(directory, a, false);
//         iw.optimize();
//         iw.close();

        ir = IndexReader.open(directory);
        for( int i=0; i<ir.maxDoc(); i++ )
          ir.delete(i);
        ir.close();

        iw = new IndexWriter(directory, a, false);
        iw.optimize();
      } finally {
        iw.close();
      }
    }

  }

}
