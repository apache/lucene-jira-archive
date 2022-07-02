Index: src/test/org/apache/lucene/search/TestNumericRangeQuery.java
===================================================================
--- src/test/org/apache/lucene/search/TestNumericRangeQuery.java	(revision 0)
+++ src/test/org/apache/lucene/search/TestNumericRangeQuery.java	(revision 0)
@@ -0,0 +1,38 @@
+package org.apache.lucene.search;
+
+import org.apache.lucene.analysis.Analyzer;
+import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
+import org.apache.lucene.document.Document;
+import org.apache.lucene.document.NumericField;
+import org.apache.lucene.document.Field.Store;
+import org.apache.lucene.index.IndexWriter;
+import org.apache.lucene.index.IndexWriterConfig;
+import org.apache.lucene.store.Directory;
+import org.apache.lucene.store.RAMDirectory;
+import org.apache.lucene.util.LuceneTestCase;
+import org.apache.lucene.util.Version;
+
+public class TestNumericRangeQuery extends LuceneTestCase {
+
+  static Directory dir = new RAMDirectory();
+  static final String F = "f";
+  static Analyzer analyzer = new WhitespaceAnalyzer( Version.LUCENE_40 );
+  static long[] rangeValues = { 0, Long.MAX_VALUE - 1000, Long.MAX_VALUE };
+  static final int precisionStep = 4;
+  
+  public void testFromLongMaxToLongMax() throws Exception {
+
+    IndexWriterConfig conf = new IndexWriterConfig( Version.LUCENE_40, analyzer );
+    IndexWriter writer = new IndexWriter( dir, conf );
+    for( long v : rangeValues ){
+      Document doc = new Document();
+      doc.add( new NumericField( F, precisionStep, Store.YES, true  ).setLongValue( v ) );
+      writer.addDocument( doc );
+    }
+    writer.close();
+
+    IndexSearcher searcher = new IndexSearcher( dir );
+    Query query = NumericRangeQuery.newLongRange( F, precisionStep, Long.MAX_VALUE, Long.MAX_VALUE, true, true );
+    assertEquals( 1, searcher.search( query, 10 ).totalHits );
+  }
+}
