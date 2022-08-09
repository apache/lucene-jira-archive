package test;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Random;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NGramPhraseQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class PerfTest {

  static final String INDEX = "myindex";
  static Directory dir;
  static final String F2 = "f2";
  static final String F3 = "f3";
  static Analyzer a2, a3;
  static PerFieldAnalyzerWrapper pfaw;
  static final int NUM_DOCS = 1000000;
  static final int FIELD_LEN_2 = 30;
  static final int FIELD_LEN_3 = 60;
  
  public static void main(String[] args) throws Exception {
    a2 = new NGramAnalyzer(2);
    a3 = new NGramAnalyzer(3);
    pfaw = new PerFieldAnalyzerWrapper(a2);
    pfaw.addAnalyzer(F3, a3);
    dir = FSDirectory.open(new File(INDEX));

    QueryParser qp2 = new QueryParser(Version.LUCENE_40, F2, a2);
    qp2.setAutoGeneratePhraseQueries(true);
    QueryParser qp2opt = new QueryParser(Version.LUCENE_40, F2, a2){
      @Override
      protected PhraseQuery newPhraseQuery(){
        return new NGramPhraseQuery(2);
      }
    };
    qp2opt.setAutoGeneratePhraseQueries(true);

    QueryParser qp3 = new QueryParser(Version.LUCENE_40, F3, a3);
    qp3.setAutoGeneratePhraseQueries(true);
    QueryParser qp3opt = new QueryParser(Version.LUCENE_40, F3, a3){
      @Override
      protected PhraseQuery newPhraseQuery(){
        return new NGramPhraseQuery(3);
      }
    };
    qp3opt.setAutoGeneratePhraseQueries(true);

    makeIndex();

    //------------------
    // bi-gram test
    //------------------
    searchIndex(F2, qp2, null, 2, 100); // warm up
    // query length=3 (no difference between opt and non-opt)
    searchIndex(F2, qp2, qp2opt, 3, 1000);
    // query length=4
    searchIndex(F2, qp2, qp2opt, 4, 1000);
    // query length=6
    searchIndex(F2, qp2, qp2opt, 6, 1000);

    //------------------
    // tri-gram test
    //------------------
    searchIndex(F3, qp3, null, 3, 100); // warm up
    // query length=4 (no difference between opt and non-opt)
    searchIndex(F3, qp3, qp3opt, 4, 2000);
    // query length=5
    searchIndex(F3, qp3, qp3opt, 5, 2000);
    // query length=6
    searchIndex(F3, qp3, qp3opt, 6, 2000);
  }

  static Random r = new Random(System.currentTimeMillis());
  
  static String getRandomNumString(int len){
    StringBuilder sb = new StringBuilder();
    for(int i = 0; i < len; i++){
      sb.append(r.nextInt(10));
    }
    return sb.toString();
  }
  
  static void makeIndex() throws IOException {
    IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_40, pfaw);
    IndexWriter writer = new IndexWriter(dir, conf);
    for(int i = 0; i < NUM_DOCS; i++){
      Document doc = new Document();
      doc.add(new Field(F2, TextField.TYPE_STORED, getRandomNumString(FIELD_LEN_2)));
      doc.add(new Field(F3, TextField.TYPE_STORED, getRandomNumString(FIELD_LEN_3)));
      writer.addDocument(doc);
      if(i % 100000 == 0){
        System.out.printf("%d docs added...\n", i);
      }
    }
    writer.close();
  }
  
  static void searchIndex(String field, QueryParser parser, QueryParser opt, int len, int loop) throws Exception {
    long pqTotal = 0;
    long optTotal = 0;
    IndexSearcher searcher = new IndexSearcher(dir);

    for(int i = 0; i < loop; i++){
      String q = getRandomNumString(len);

      int totalHits = -1;
      if(opt != null){
        Query q1 = opt.parse(q);
        long t1 = System.currentTimeMillis();
        TopDocs docs = searcher.search(q1, 10);
        long elapse = System.currentTimeMillis() - t1;
        optTotal += elapse;
        totalHits = docs.totalHits;
      }

      Query q2 = parser.parse(q);
      long t1 = System.currentTimeMillis();
      TopDocs docs = searcher.search(q2, 10);
      long elapse = System.currentTimeMillis() - t1;
      pqTotal += elapse;
      
      if(opt != null && docs.totalHits != totalHits)
        throw new RuntimeException("total hits doesn't match!!!");
    }

    searcher.close();
    
    if(opt != null){
      float sup = ((float)pqTotal / (float)optTotal - 1F) * 100F;
      System.out.printf("field=%s q-len=%d : pqTotal=%,dms optTotal=%,dms (%2.1f up)\n",
          field, len, pqTotal, optTotal, sup);
    }
  }

  static class NGramAnalyzer extends Analyzer {
    private final int n;
    public NGramAnalyzer(int n){
      this.n = n;
    }
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new NGramTokenizer(reader, n, n);
    }
  }
}
