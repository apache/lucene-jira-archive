import java.io.File;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.*;

public class Make2BTermsIndex {
  public static void main(String[] args) throws Exception {
    Directory dir = FSDirectory.open(new File(args[0]));
    long upto = 0;
    IndexWriter w = new IndexWriter(dir,
                                    new IndexWriterConfig(Version.LUCENE_CURRENT,
                                                          new WhitespaceAnalyzer(Version.LUCENE_CURRENT))
                                    .setRAMBufferSizeMB(256.0));
    w.setUseCompoundFile(false);
    final long limit = ((long) Integer.MAX_VALUE) + 200000000;
    final Document doc = new Document();
    final Field f = new Field("f", "", Field.Store.NO, Field.Index.ANALYZED_NO_NORMS);
    doc.add(f);
    while(upto < limit) {
      StringBuilder b = new StringBuilder();
      for(int i=0;i<1000;i++) {
        b.append(' ');
        b.append(Long.toString(upto++, Character.MAX_RADIX));
      }
      f.setValue(b.toString());
      w.addDocument(doc);
      if (upto % 10000000 == 0) {
        System.out.println(upto + "...");
      }
    }
    System.out.println("optimize");
    w.optimize();
    w.close();
    dir.close();
  }
}