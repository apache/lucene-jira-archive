package org.apache.lucene.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.store.RAMDirectory;

import junit.framework.TestCase;

/**
 * @author d.de.wit
 * 
 */
public class TestMultiSearcherAutoSort extends TestCase {
  public void testAutoSort() throws Exception {
    RAMDirectory dir1 = new RAMDirectory();
    RAMDirectory dir2 = new RAMDirectory();

    Analyzer analyzer = new StandardAnalyzer();
    IndexWriter writer1 = new IndexWriter(dir1, analyzer, true);
    for (int i = 0; i < 3; i++) {
      Document doc = new Document();
      doc.add(new Field("displayName1", "obj-" + i, Store.YES, Index.TOKENIZED));
      doc.add(new Field("id1", "" + i, Store.YES, Index.UN_TOKENIZED));
      doc.add(new Field("sortField1", "" + i, Store.NO, Index.UN_TOKENIZED));
      writer1.addDocument(doc);
    }
    writer1.optimize();
    writer1.close();

    IndexWriter writer2 = new IndexWriter(dir2, analyzer, true);
    for (int i = 0; i < 3; i++) {
      Document doc = new Document();
      doc.add(new Field("displayName2", "obj-" + i, Store.YES, Index.TOKENIZED));
      doc.add(new Field("id2", "" + i, Store.YES, Index.UN_TOKENIZED));
      doc.add(new Field("sortField2", "" + i, Store.NO, Index.UN_TOKENIZED));
      writer2.addDocument(doc);
    }
    writer2.optimize();
    writer2.close();

    IndexSearcher searcher1 = new IndexSearcher(dir1);
    IndexSearcher searcher2 = new IndexSearcher(dir2);
    MultiSearcher multiSearcher = new MultiSearcher(new Searchable[] {searcher1, searcher2});
    Sort sort = new Sort("sortField1", true);
    Hits hits = multiSearcher.search(new WildcardQuery(new Term("displayName1", "o*")), sort);

    assertEquals("3", hits.doc(0).get("id"));
    assertEquals("2", hits.doc(1).get("id"));
    assertEquals("1", hits.doc(2).get("id"));
  }
}
