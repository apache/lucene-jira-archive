import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

/**
 * A test for bug http://issues.apache.org/jira/browse/LUCENE-638.
 * The bug manifests by throwing a FileNotFoundException.
 */
public final class LuceneTest {

  /**
   * @param args Ignored.
   * @throws IOException
   * @throws ParseException
   */
  public static void main(final String[] args) throws IOException, ParseException {
    File indexFile = createIndexFile();
    createIndex(indexFile);
    createNonLuceneDirectory(indexFile);
    readIndex(indexFile);
  }

  /**
   * Create a CVS directory.
   * @param indexFile The parent directory.
   * @throws IOException
   */
  private static void createNonLuceneDirectory(final File indexFile) throws IOException {
    File dir = new File(indexFile, "CVS");
    if (dir.mkdir()) {
      System.out.println("Created directory " + dir.getCanonicalPath());
    }
    else {
      throw new IOException("Failed to create the directory");
    }
  }

  /**
   * Attempt to read the index.
   * @param indexFile The index directory.
   * @throws IOException The bug manifests by throwing a FileNotFoundException here.
   * @throws ParseException
   */
  private static void readIndex(final File indexFile) throws IOException, ParseException {
    RAMDirectory dir = new RAMDirectory(indexFile); // This is where it goes wrong.
    IndexSearcher searcher = new IndexSearcher(dir);
    QueryParser queryParser = new QueryParser("default", new SimpleAnalyzer());
    Hits hits = searcher.search(queryParser.parse("value"));
    for (int i = 0; i < hits.length(); ++i) {
      Document doc = hits.doc(i);
      System.out.println(doc.get("default"));
    }
    searcher.close();
  }

  /**
   * Create a directory in which to store the Lucene index.
   * @return The directory.
   * @throws IOException
   */
  private static File createIndexFile() throws IOException {
    File indexDir = File.createTempFile("luceneTest", null);
    System.out.println("The index will be stored at " + indexDir.getCanonicalPath());
    indexDir.delete();
    indexDir.mkdir();
    return indexDir;
  }

  /**
   * Create a Lucene index containing a sample document.
   * @param indexDir The directory in which to write the index.
   * @throws IOException
   */
  private static void createIndex(final File indexDir) throws IOException {
    FSDirectory dir = FSDirectory.getDirectory(indexDir, true);
    IndexWriter writer = new IndexWriter(dir, new SimpleAnalyzer(), true);
    Document doc = new Document();
    doc.add(new Field("default", "If the bug does not manifest, this value should be printed to stdout.", Store.YES, Index.TOKENIZED));
    writer.addDocument(doc);
    writer.optimize();
    writer.close();
  }

}
