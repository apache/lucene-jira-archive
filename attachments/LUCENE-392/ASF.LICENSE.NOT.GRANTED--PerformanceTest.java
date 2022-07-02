import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

/*
 * Created on 26.05.2005
 */

/**
 * @author dnaber
 */
public class PerformanceTest {

  public static void main(String[] args) throws IOException {
    Directory d = new RAMDirectory();
    IndexWriter iw = new IndexWriter(d, new StandardAnalyzer(), true);
    iw.setMaxBufferedDocs(200);
    long t1 = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
      iw.addDocument(makeDoc());
    }
    iw.close();
    System.out.println(System.currentTimeMillis()-t1+ "ms");
  }
  
  private static Document makeDoc() {
    Document doc = new Document();
    doc.add(new Field("body", new StringReader("laber")));
    doc.add(new Field("body2", new StringReader("blubb")));
    doc.add(new Field("body3", new StringReader("bla")));
    //doc.add(new Field("body2", "blubb", Field.Store.YES, Field.Index.NO));
    //doc.add(new Field("body3", "murks", Field.Store.YES, Field.Index.NO));
    return doc;
  }

}
