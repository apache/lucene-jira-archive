package org.apache.lucene.search.join;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author akitta
 *
 */
public class QueryTimeJoinTest {

  private static final String NORMS = "norms";
  private static final String BYTE = "byte";
  private static final String ARTICLE_ID = "article_id";
  private static final String CONTENT = "content";
  private static final String TITLE = "title";
  private static final String ID = "id";

  /**
   * @param args
   */
  public static void main(final String[] args) {

    testJoinQueryWithLongField();
    System.out.println("\n\n");
    testJoinQueryWithStringField();

  }

  private static void testJoinQueryWithLongField() {

    Directory dir = null;
    IndexWriter w = null;
    DirectoryReader r = null;

    try {

      dir = new RAMDirectory();
      w = new IndexWriter(dir, new IndexWriterConfig(Version.LUCENE_41, new WhitespaceAnalyzer(Version.LUCENE_41)));
      FieldType fieldType = new FieldType();
      fieldType.setIndexed(true);
      fieldType.setTokenized(true);
      fieldType.setOmitNorms(true);
      fieldType.setIndexOptions(FieldInfo.IndexOptions.DOCS_ONLY);
      fieldType.setNumericType(FieldType.NumericType.LONG);
      fieldType.setNumericPrecisionStep(Integer.MAX_VALUE);

      Document d = new Document();
      d.add(new LongField(ID, 1, fieldType));
      d.add(new TextField(TITLE, "Query time joining in Lucene", Store.YES));
      d.add(new TextField(CONTENT, "Recently query time joining has been...", Store.YES));
      w.addDocument(d);

      d = new Document();
      d.add(new LongField(ID, 2, fieldType));
      d.add(new TextField(TITLE, "Simon says: Single byte norms are Dead!", Store.YES));
      d.add(new TextField(CONTENT, "Recently query time joining has been...", Store.YES));
      w.addDocument(d);

      d = new Document();
      d.add(new LongField(ID, 1, fieldType));
      d.add(new LongField(ARTICLE_ID, 1, Store.YES));
      d.add(new TextField(CONTENT, "Why not index the article comment inside the article documents...", Store.YES));
      w.addDocument(d);

      d = new Document();
      d.add(new LongField(ID, 2, fieldType));
      d.add(new LongField(ARTICLE_ID, 2, Store.YES));
      d.add(new TextField(CONTENT, "Hi Simon, This sounds like a really useful feature. If I understand...", Store.YES));
      w.addDocument(d);

      w.commit();
      r = DirectoryReader.open(dir);

      final IndexSearcher searcher = new IndexSearcher(r);

      final String fromField = ID;
      final boolean multipleValuesPerDocument = false;
      final String toField = ARTICLE_ID;
      // This query should yield article with id 2 as result
      final BooleanQuery fromQuery = new BooleanQuery();
      fromQuery.add(new TermQuery(new Term(TITLE, BYTE)), BooleanClause.Occur.MUST);
      fromQuery.add(new TermQuery(new Term(TITLE, NORMS)), BooleanClause.Occur.MUST);
      final Query joinQuery = JoinUtil.createJoinQuery(fromField, multipleValuesPerDocument, toField, fromQuery, searcher, ScoreMode.None);
      final TopDocs topDocs = searcher.search(joinQuery, 10);


      System.out.println("QueryTimeJoinTest.testJoinQueryWithLongField()");
      System.out.println("Hit count: " + topDocs.totalHits);

      for (final ScoreDoc doc : topDocs.scoreDocs) {

        System.out.println("Matching doc: " + searcher.doc(doc.doc));

      }

    } catch (final Exception e) {

      e.printStackTrace();

    } finally {

      close(r);
      close(w);
      close(dir);

    }

  }


  private static void testJoinQueryWithStringField() {

    Directory dir = null;
    IndexWriter w = null;
    DirectoryReader r = null;

    try {

      dir = new RAMDirectory();
      w = new IndexWriter(dir, new IndexWriterConfig(Version.LUCENE_41, new WhitespaceAnalyzer(Version.LUCENE_41)));

      Document d = new Document();
      d.add(new StringField(ID, "1", Store.YES));
      d.add(new TextField(TITLE, "Query time joining in Lucene", Store.YES));
      d.add(new TextField(CONTENT, "Recently query time joining has been...", Store.YES));
      w.addDocument(d);

      d = new Document();
      d.add(new StringField(ID, "2", Store.YES));
      d.add(new TextField(TITLE, "Simon says: Single byte norms are Dead!", Store.YES));
      d.add(new TextField(CONTENT, "Recently query time joining has been...", Store.YES));
      w.addDocument(d);

      d = new Document();
      d.add(new StringField(ID, "1", Store.YES));
      d.add(new StringField(ARTICLE_ID, "1", Store.YES));
      d.add(new TextField(CONTENT, "Why not index the article comment inside the article documents...", Store.YES));
      w.addDocument(d);

      d = new Document();
      d.add(new StringField(ID, "2", Store.YES));
      d.add(new StringField(ARTICLE_ID, "2", Store.YES));
      d.add(new TextField(CONTENT, "Hi Simon, This sounds like a really useful feature. If I understand...", Store.YES));
      w.addDocument(d);

      w.commit();
      r = DirectoryReader.open(dir);

      final IndexSearcher searcher = new IndexSearcher(r);

      final String fromField = ID;
      final boolean multipleValuesPerDocument = false;
      final String toField = ARTICLE_ID;
      // This query should yield article with id 2 as result
      final BooleanQuery fromQuery = new BooleanQuery();
      fromQuery.add(new TermQuery(new Term(TITLE, BYTE)), BooleanClause.Occur.MUST);
      fromQuery.add(new TermQuery(new Term(TITLE, NORMS)), BooleanClause.Occur.MUST);
      final Query joinQuery = JoinUtil.createJoinQuery(fromField, multipleValuesPerDocument, toField, fromQuery, searcher, ScoreMode.None);
      final TopDocs topDocs = searcher.search(joinQuery, 10);


      System.out.println("QueryTimeJoinTest.testJoinQueryWithStringField()");
      System.out.println("Hit count: " + topDocs.totalHits);

      for (final ScoreDoc doc : topDocs.scoreDocs) {

        System.out.println("Matching doc: " + searcher.doc(doc.doc));

      }

    } catch (final Exception e) {

      e.printStackTrace();

    } finally {

      close(r);
      close(w);
      close(dir);

    }

  }

  private static void close(final Closeable closeable) {

    if (null != closeable) {

      try {

        closeable.close();

      } catch (final IOException e) {
        e.printStackTrace();

        if (null != closeable) {

          try {
            closeable.close();
          } catch (final IOException e1) {
            //ignore

          }

        }

      }

    }

  }

}
