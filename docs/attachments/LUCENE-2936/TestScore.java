package sample;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class TestScore {
  
  static Directory dir = new RAMDirectory();
  static Analyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_40);
  static final String F1 = "f1";
  static final String F2 = "f2";
  static final String F3 = "f3";

  public static void main(String[] args) throws Exception {
    makeIndex();
    searchIndex();
  }

  static void makeIndex() throws IOException {
    IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_40, analyzer);
    IndexWriter writer = new IndexWriter(dir, conf);
    Document doc = new Document();
    addField(doc,F1,"note book",1);
    addField(doc,F2,"note book",0);
    addField(doc,F3,"memo book",1);
    writer.addDocument(doc);
    writer.close();
  }
  
  static void addField(Document doc, String field, String val, float boost) throws IOException {
    Field f = new Field(field, val, Store.YES, Index.ANALYZED);
    f.setBoost(boost);
    doc.add(f);
  }
  
  static void searchIndex() throws Exception {
    IndexSearcher searcher = new IndexSearcher(dir);
    QueryParser qp = new QueryParser(Version.LUCENE_40, F1, analyzer);
    Query query = qp.parse("f1:\"note book\" f2:\"note book\" f3:\"note book\"");
    TopDocs docs = searcher.search(query, 10);
    float score = docs.scoreDocs[0].score;
    int doc = docs.scoreDocs[0].doc;
    System.out.println( "** score = " + score );
    Explanation exp = searcher.explain(query, doc);
    System.out.println( "** explain\n" + exp.toString() );
    searcher.close();
  }
}
