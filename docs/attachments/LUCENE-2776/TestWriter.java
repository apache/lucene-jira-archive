import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class TestWriter {

  public static void main(String[] args) throws Exception {

    Directory dir = new RAMDirectory();
    IndexWriter indexWriter = new IndexWriter(dir, new StandardAnalyzer(Version.LUCENE_CURRENT), true, IndexWriter.MaxFieldLength.UNLIMITED);
    indexWriter.setUseCompoundFile(false);
    indexWriter.setRAMBufferSizeMB(.01);
   //  IndexReader reader = IndexReader.open(indexWriter);

    String BIG="alskjhlaksjghlaksjfhalksvjepgjioefgjnsdfjgefgjhelkgjhqewlrkhgwlekgrhwelkgjhwelkgrhwlkejg";
    BIG=BIG+BIG+BIG+BIG;


    for (int i=0; i<2; i++) {
      Document doc = new Document();
      doc.add(new Field("id", Integer.toString(i)+BIG, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
      doc.add(new Field("str", Integer.toString(i)+BIG, Field.Store.YES, Field.Index.NOT_ANALYZED));
      doc.add(new Field("str2", Integer.toString(i)+BIG, Field.Store.YES, Field.Index.ANALYZED));
      doc.add(new Field("str3", Integer.toString(i)+BIG, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
      indexWriter.addDocument(doc);
    }

    indexWriter.close();
    System.out.println(Arrays.asList(dir.listAll()));
    for (String file : dir.listAll()) {
      if (file.endsWith(".tvx")) {
        System.out.println("Found TermVector info: " + file);
      }
    }
  }
}

