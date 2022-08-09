import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.simple.SimpleQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class MSMarcoPassages {

  public static void main(String[] args) throws Exception {
    //index();
    query();
  }

  private static void index() throws IOException {
    IndexWriterConfig cfg = new IndexWriterConfig()
        .setOpenMode(OpenMode.CREATE)
        .setRAMBufferSizeMB(200);
    Directory dir = FSDirectory.open(Paths.get("/data/tmp/index-9"));
    try (BufferedReader rd = new BufferedReader(new FileReader(Paths.get("/home/jpountz/Downloads/collection.tsv").toFile()));
        IndexWriter w = new IndexWriter(dir, cfg)) {
      for (String line = rd.readLine(); line != null; line = rd.readLine()) {
        String[] splits = line.split("\t");
        if (splits.length != 2) {
          throw new Error();
        }
        Document doc = new Document();
        doc.add(new StringField("id", splits[0], Store.YES));
        doc.add(new TextField("body", splits[1], Store.YES));
        w.addDocument(doc);
      }
      w.forceMerge(1);
    }
  }

  private static void query() throws Exception {
    final int numQueries = 1000;
    long[] qTimes = new long[numQueries];
    Arrays.fill(qTimes, Long.MAX_VALUE);
    long[] collectedCounts = new long[numQueries];
    for (int iter = 0; iter < 3; ++iter) {
      try (BufferedReader rd = new BufferedReader(new FileReader(Paths.get("/home/jpountz/Downloads/queries.eval.tsv").toFile()));
          Directory dir = FSDirectory.open(Paths.get("/data/tmp/index-9"));
          DirectoryReader reader = DirectoryReader.open(dir)) {
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setQueryCache(null);
        SimpleQueryParser qp = new SimpleQueryParser(new StandardAnalyzer(), "body");
        int i = 0;
        for (String line = rd.readLine(); line != null && i < numQueries; line = rd.readLine()) {
          String[] splits = line.split("\t");
          Query q = qp.parse(splits[1]);
          long start = System.nanoTime();
          TopDocsCollector<ScoreDoc> collector = TopScoreDocCollector.create(10, null, 10);
          searcher.search(q, collector);
          TopDocs td = collector.topDocs();
          qTimes[i] = Math.min(qTimes[i], System.nanoTime() - start);
          collectedCounts[i] = td.totalHits.value;
          i++;
        }
      }
    }
    Arrays.sort(qTimes);
    Arrays.sort(collectedCounts);
    System.out.println("AVG: " + Arrays.stream(qTimes).average().getAsDouble());
    System.out.println("Median: " + qTimes[qTimes.length / 2]);
    System.out.println("P75: " + qTimes[3 * qTimes.length / 4]);
    System.out.println("P90: " + qTimes[9 * qTimes.length / 10]);
    System.out.println("P95: " + qTimes[19 * qTimes.length / 20]);
    System.out.println("P99: " + qTimes[99 * qTimes.length / 100]);
    System.out.println("Collected AVG: " + Arrays.stream(collectedCounts).average().getAsDouble());
    System.out.println("Collected Median: " + collectedCounts[collectedCounts.length / 2]);
    System.out.println("Collected P75: " + collectedCounts[3 * collectedCounts.length / 4]);
    System.out.println("Collected P90: " + collectedCounts[9 * collectedCounts.length / 10]);
    System.out.println("Collected P95: " + collectedCounts[19 * collectedCounts.length / 20]);
    System.out.println("Collected P99: " + collectedCounts[99 * collectedCounts.length / 100]);
  }

}
