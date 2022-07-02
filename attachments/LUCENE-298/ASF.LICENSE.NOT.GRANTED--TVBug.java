import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

public class TVBug {

  public static void main(String[] args) throws IOException {
    Document document = new Document();
    document.add(new Field("tvtest", "", Field.Store.NO, Field.Index.TOKENIZED,
        Field.TermVector.WITH_OFFSETS));    // throws exception, works with Field.TermVector.NO
    IndexWriter ir = new IndexWriter("/tmp/testindex", new StandardAnalyzer(), true);
    ir.addDocument(document);
    ir.close();
  }
}
