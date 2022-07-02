import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Random;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.FSDirectory;

/**
 * This benchmark generates an index with numeric terms.
 * It runs many iterations over several patterns,
 * where an iteration fills the pattern with random digits.
 */
public class BenchWildcard {
  
  static final String INDEX_PATH = "c:/work/wildcardBench";
  static Random random = new Random();
  
  static char N() {
    return (char) (0x30 + random.nextInt(10));
  }
  
  static String fillPattern(String wildcardPattern) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < wildcardPattern.length(); i++) {
      switch(wildcardPattern.charAt(i)) {
        case 'N':
          sb.append(N());
          break;
        default:
          sb.append(wildcardPattern.charAt(i));
      }
    }
    return sb.toString();
  }
  
  public static void main(String args[]) throws Exception {
    File indexDir = new File(INDEX_PATH);
    createIndex(indexDir);
    random.setSeed(System.currentTimeMillis());
    runTest(indexDir);
  }
  
  static void createIndex(File indexDir) throws Exception {
    System.err.print("creating index... ");
    System.err.flush();
    try {
      File files[] = indexDir.listFiles();
      for (File f : files)
        f.delete();
    } catch (Exception e) {}
    IndexWriter writer = new IndexWriter(FSDirectory.open(indexDir), 
        new KeywordAnalyzer(), IndexWriter.MaxFieldLength.UNLIMITED);
    
    Document doc = new Document();
    Field field = new Field("field", "", Field.Store.NO, Field.Index.ANALYZED);
    doc.add(field);
    
    NumberFormat df = new DecimalFormat("0000000");
    for (int i = 0; i < 10000000; i++) {
      field.setValue(df.format(i));
      writer.addDocument(doc);
    }
    
    writer.optimize();
    writer.close();
    System.err.println("index created");
    System.err.flush();
  }
  
  static void runPattern(Searcher searcher, String pattern, int iterations) throws Exception {
    Date start = new Date();
    long numHits = 0;
    for (int i = 0; i < iterations; i++) {
      Query wq = new WildcardQuery(new Term("field", fillPattern(pattern)));
      TopDocs docs = searcher.search(wq, 25);
      numHits += docs.totalHits;
    }
    Date end = new Date();
    long ms = end.getTime() - start.getTime();
    double avg = (double) ms / (double) iterations;
    double avgHits = (double) numHits / (double) iterations;
    System.out.println(pattern + "\t" + iterations + "\t" + avgHits + "\t" + avg);
  }
  
  static void runTest(File indexDir) throws Exception {
    Searcher searcher = new IndexSearcher(FSDirectory.open(indexDir));
    // warmup
    WildcardQuery wq = new WildcardQuery(new Term("field", "*"));
    searcher.search(wq, 25);
    System.out.println("Pattern\tIter\tAvgHits\tAvgMS");
    runPattern(searcher, "N?N?N?N", 10);
    runPattern(searcher, "?NNNNNN", 10);
    runPattern(searcher, "??NNNNN", 10);
    runPattern(searcher, "???NNNN", 10);
    runPattern(searcher, "????NNN", 10);
    runPattern(searcher, "NN??NNN", 10);
    runPattern(searcher, "NN?N*", 10);
    runPattern(searcher, "?NN*", 10);
    runPattern(searcher, "*N", 10);
    runPattern(searcher, "NNNNN??", 10);
  }
}
