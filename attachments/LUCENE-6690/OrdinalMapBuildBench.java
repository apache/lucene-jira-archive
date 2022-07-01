import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiDocValues.OrdinalMap;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.TestUtil;
import org.apache.lucene.util.packed.PackedInts;


public class OrdinalMapBuildBench {

  public static void main(String[] args) throws IOException {
    Directory dir = FSDirectory.open(new File("/tmp/a").toPath());
    DirectoryReader reader = DirectoryReader.open(dir);
    System.out.println(reader.leaves().size());
    SortedDocValues[] vals = new SortedDocValues[reader.leaves().size()];
    for (int i = 0; i < vals.length; ++i) {
      vals[i] = reader.leaves().get(i).reader().getSortedDocValues("f");
    }
    for (int i = 0; i < 20; ++i) {
      long start = System.nanoTime();
      OrdinalMap map = OrdinalMap.build(null, vals, PackedInts.DEFAULT);
      System.out.println((System.nanoTime() - start) / 1000000 + " " + map.getValueCount());
    }
  }

  // Run this main method first to build the index
  public static void main1(String[] args) throws IOException {
    Directory dir = FSDirectory.open(new File("/tmp/a").toPath());
    IndexWriterConfig c = new IndexWriterConfig(null);
    c.setMaxBufferedDocs(1000);
    IndexWriter w = new IndexWriter(dir, c);
    Document doc = new Document();
    SortedDocValuesField f = new SortedDocValuesField("f", new BytesRef());
    doc.add(f);
    Random r = new Random(0);
    for (int i = 0; i < 10000000; ++i) {
      f.setBytesValue(new BytesRef(TestUtil.randomSimpleString(r, 20)));
      w.addDocument(doc);
    }
    w.commit();
    w.close();
    dir.close();
  }

}
