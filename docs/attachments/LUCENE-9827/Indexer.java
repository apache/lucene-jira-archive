import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.lucene.codecs.lucene90.Lucene90Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.store.FSDirectory;

public class Indexer {
  public static void main(String args[]) throws Exception {    
    FSDirectory dir = FSDirectory.open(Paths.get("/tmp/index"));
    for (String file : dir.listAll()) {
      dir.deleteFile(file);
    }
    IndexWriterConfig iwc = new IndexWriterConfig();
    iwc.setCodec(new Lucene90Codec(Lucene90Codec.Mode.BEST_COMPRESSION));
    iwc.setMergeScheduler(new SerialMergeScheduler());
    IndexWriter iw = new IndexWriter(dir, iwc);
    LineNumberReader reader = new LineNumberReader(new InputStreamReader(Files.newInputStream(Paths.get("/home/rmuir/workspace/IndexGeonames/src/allCountries.txt"))));
    long t0 = System.currentTimeMillis();
    indexDocs(iw, reader);
    System.out.println("time: " + (System.currentTimeMillis() - t0));
    reader.close();
    iw.close();
    dir.close();
  }
  
  static void indexDocs(IndexWriter iw, LineNumberReader reader) throws Exception {
    Document doc = new Document();
    Field fields[] = new Field[19];
    for (int i = 0; i < fields.length; i++) {
      fields[i] = new StoredField("field " + i, "");
      doc.add(fields[i]);
    }
    
    String line = null;
    while ((line = reader.readLine()) != null) {
      if (reader.getLineNumber() % 1000 == 0) {
        System.out.println("doc: " + reader.getLineNumber());
      }
      if (reader.getLineNumber() == 100000) {
        break;
      }
      String values[] = line.split("\t");
      if (values.length != fields.length) {
        throw new RuntimeException("bogus: " + values);
      }
      for (int i = 0; i < values.length; i++) {
        fields[i].setStringValue(values[i]);
      }
      iw.addDocument(doc);
      iw.flush();
    }
  }
}
