import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.KeepOnlyLastCommitDeletionPolicy;
import org.apache.lucene.index.SnapshotDeletionPolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Random;

public class CommitReuseTest {

  private final File path = new File("indexDir");
  private IndexWriter writer;
  private final SnapshotDeletionPolicy snapshotter = new SnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy());
  
  @Before
  public void initIndex() throws Exception {
    path.mkdirs();
    IndexWriterConfig idxWriterCfg = new IndexWriterConfig(Version.LUCENE_46, null);
    idxWriterCfg.setIndexDeletionPolicy(snapshotter);
    
    Directory dir = FSDirectory.open(path);
    writer = new IndexWriter(dir, idxWriterCfg);
    
    writer.commit(); // make sure all index metadata is written out
  }
  
  @After
  public void stop() throws Exception {
    writer.close();
  }

  @Test
  public void test() throws Exception {
    Document doc;
    String fName = "foo";
    
    // Index some data
    for (int i = 0; i < 1000; i++) {
      doc = new Document();
      doc.add(new StringField("key", String.valueOf(i % 1000), Store.YES));
      doc.add(new IntField(fName, i, Store.YES));
      writer.addDocument(doc);
    }
    
    writer.commit();
    
    IndexCommit ic1 = snapshotter.snapshot();
    
    int i = new Random().nextInt(1000);
    doc = new Document();
    String key = String.valueOf(i % 1000);
    doc.add(new StringField("key", key, Store.YES));
    doc.add(new IntField(fName, i, Store.YES));
    writer.updateDocument(new Term("key", key), doc);
    
    writer.commit();
    
    IndexCommit ic2 = snapshotter.snapshot();
    DirectoryReader latest = DirectoryReader.open(ic2);
    
    // This reader will be used for searching against commit point 1
    DirectoryReader searchReader = DirectoryReader.openIfChanged(latest, ic1);
    
    latest.close();
    searchReader.close();
    
    snapshotter.release(ic1);
    snapshotter.release(ic2);
  }

}


