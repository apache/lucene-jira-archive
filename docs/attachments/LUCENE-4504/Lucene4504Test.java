package org.apache.lucene.queries.function;

import java.io.IOException;
import junit.framework.TestCase;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queries.function.valuesource.IntFieldSource;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class Lucene4504Test extends TestCase {

  public void testSearchAfterWhenSortingByFunctionValues() throws IOException {

    IndexWriter writer = new IndexWriter(new RAMDirectory(), new IndexWriterConfig(Version.LUCENE_40, null));

    Document doc = new Document();
    Field field = new StringField("value", "", Field.Store.YES);
    doc.add(field);

    // Save docs unsorted (decreasing value n, n-1, ...)
    final int NUM_VALS = 5;
    for (int val = NUM_VALS; val > 0; val--) {
      field.setStringValue(Integer.toString(val));
      writer.addDocument(doc);
    }

    writer.commit();

    // Open index
    IndexReader reader = DirectoryReader.open(writer.getDirectory());
    IndexSearcher searcher = new IndexSearcher(reader);

    // Get ValueSource from FieldCache
    IntFieldSource src = new IntFieldSource("value");
    // ...and make it a sort criterion
    SortField sf = src.getSortField(false).rewrite(searcher);
    Sort orderBy = new Sort(sf);

    // Get hits sorted by our FunctionValues (ascending values)
    Query q = new MatchAllDocsQuery();
    TopDocs hits = searcher.search(q, Integer.MAX_VALUE, orderBy);
    assertEquals(NUM_VALS, hits.scoreDocs.length);
    // Verify that sorting works in general
    int i = 0;
    for (ScoreDoc hit : hits.scoreDocs) {
      int valueFromDoc = Integer.parseInt(reader.document(hit.doc).get("value"));
      assertEquals(++i, valueFromDoc);
    }

    // Now get hits after hit #2 using IS.searchAfter()
    int afterIdx = 1;
    FieldDoc afterHit = (FieldDoc) hits.scoreDocs[afterIdx];
    hits = searcher.searchAfter(afterHit, q, Integer.MAX_VALUE, orderBy);

    // Expected # of hits: NUM_VALS - 2
    assertEquals(NUM_VALS - (afterIdx + 1), hits.scoreDocs.length);

    // Verify that hits are actually "after"
    int afterValue = ((Double) afterHit.fields[0]).intValue();
    for (ScoreDoc hit : hits.scoreDocs) {
      int val = Integer.parseInt(reader.document(hit.doc).get("value"));
      assertTrue(afterValue <= val);
      assertFalse(hit.doc == afterHit.doc);
    }
  }
}
