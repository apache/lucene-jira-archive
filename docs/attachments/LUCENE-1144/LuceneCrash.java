import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.LockObtainFailedException;

/**
 * Makes Lucene crash when called with -Xmx10M
 */
public class LuceneCrash {

  public static void main(String[] args) throws Exception,  IOException {    
    LuceneCrash prg = new LuceneCrash();
    prg.myrun();
  }
  
  private void myrun() throws CorruptIndexException, LockObtainFailedException, IOException {
    IndexWriter iw = new IndexWriter("/tmp/lucenetest", new StandardAnalyzer(), true);
    //iw.setRAMBufferSizeMB(5);     -- no crash if you comment this in
    iw.setMaxFieldLength(Integer.MAX_VALUE);
    
    File[] files = new File("/mnt/plain_text").listFiles();
    for (File file : files) {
      if (file.getName().endsWith(".txt")) {
        Document doc = new Document();
        doc.add(new Field("body", new FileReader(file)));
        iw.addDocument(doc);
      }
    }
    iw.optimize();
    iw.close();
  }
  
}
