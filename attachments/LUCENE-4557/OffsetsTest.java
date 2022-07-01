package org.apache.lucene.index;

import org.junit.Test;
import org.junit.Assert;

import java.util.List;
import java.util.ArrayList;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;


public class OffsetsTest {
  private static final String FIELD = "text";

  public void addDocument(IndexWriter writer, String text, IndexOptions options) throws Exception {
    final FieldType type = new FieldType();
    type.setIndexed(true);
    type.setStored(true);
    type.setTokenized(true);
    type.setIndexOptions(options);

    final List<Field> doc = new ArrayList<Field>();
    doc.add( new Field(FIELD, text, type) );
    writer.addDocument(doc);
  }

  @Test
  public void test() throws Exception {
    final IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, new SimpleAnalyzer(Version.LUCENE_40));
    
    final Directory dir = new RAMDirectory();
    final IndexWriter writer = new IndexWriter(dir, config);
    addDocument(writer, "some sample text", IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
    writer.commit();
    addDocument(writer, "some more sample text", IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
    writer.commit();
    writer.forceMerge(1, true);
    writer.close();

    final IndexReader reader = DirectoryReader.open(dir);
    Assert.assertEquals("Optimized", 1, reader.leaves().size());

    for (AtomicReaderContext segment : reader.leaves()) {
      final Terms terms = segment.reader().terms(FIELD);
      Assert.assertNotNull("Has Field", terms);
      Assert.assertTrue("Has Offsets", terms.hasOffsets());
    }
  }
}
