import java.io.File;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

public class TestOOM {

  /**
   * Run with a single argument: IndexDir
   */
  public static void main(String[] args) throws Exception {
    if (args.length<1) {
      System.err.println("Usage: java TestOOM <IndexDir>");
      System.exit(1);
    }
    String indexDir = args[0];
    IndexWriter writer = new IndexWriter(new File(indexDir), new StandardAnalyzer(), true);
    int nAdded = 0;
    printMem(nAdded);
    for (int i = 0; i < 1000; i++) {
      for (int j = 0; j < 1000; j++) {
        nAdded++;
        writer.addDocument(getDoc());
      }
      printMem(nAdded);
    }
  }

  private static Document getDoc() {
    Document document = new Document();
    document.add(new Field("foo", "foo bar", Field.Store.NO, Field.Index.TOKENIZED));
    return document;
  }

  private static void printMem(int nAdded) {
    Runtime rt = Runtime.getRuntime();
    long tot = rt.totalMemory();
    long free = rt.freeMemory();
    long used = tot-free;
    float m1 = 1000000;
    System.out.println("after adding "+nAdded+" docs, mem:: tot:"+tot/m1+" , free:"+free/m1+" , used:"+used/m1);
  }
}
