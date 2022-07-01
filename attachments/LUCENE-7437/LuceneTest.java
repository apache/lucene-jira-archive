import java.io.File;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

// created: mpichler, 20160907
// compile: cd .. ; javac -cp lib/* -d classes src/LuceneTest.java
// run:     cd .. ; java -cp classes;lib/* LuceneTest

public class LuceneTest
{
  // private static final Version LUCENE_VERSION = Version.LUCENE_6_2_0;
  private static final String INDEX_DIR = "c:/prj/test/lucene/testIndex";
  private static final String FIELD_NAME = "f";

  private static void addDocument(IndexWriter iWriter, String fieldText) throws Exception
  {
    Document doc = new Document();
    FieldType fType = new FieldType();
    fType.setStored(true);
    fType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
    doc.add(new Field(FIELD_NAME, fieldText, fType));
    iWriter.addDocument(doc);
  }

  private static void searchTest(IndexSearcher iSearcher, QueryParser queryParser,
    String searchString, int expectedHits) throws Exception
  {
    Query query = queryParser.parse(searchString);
    TopDocs topDocs = iSearcher.search(query, 10);
    int numHits = topDocs.totalHits;
    System.out.println("search: '" + searchString + "', query: '" + query + "', #hits: " + numHits);
    if (expectedHits != numHits)
      System.out.println("  ^^^ expected " + expectedHits + " hit(s), got " + numHits);
  }

  public static void main(String[] args) throws Exception
  {
    System.err.println("'a' is a letter: " + Character.isLetter('a')); // true
    System.err.println("'_' is a letter: " + Character.isLetter('_')); // false

    Directory directory = FSDirectory.open(new File(INDEX_DIR).toPath());
    // LowerCaseTokenizer: isLetter+toLowerCase, '_' is used as separator character
    Analyzer analyzer = new SimpleAnalyzer();
    // Analyzer analyzer = new KeywordAnalyzer(); // single token
    // Analyzer analyzer = new StandardAnalyzer(); // english stop words
    // Analyzer analyzer = new WhitespaceAnalyzer(); // separate at WS

    // write
    IndexWriterConfig iwConfig = new IndexWriterConfig(analyzer);
    iwConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE); // create new index (overwrite existent)
    IndexWriter iWriter = new IndexWriter(directory, iwConfig);
    Document doc = new Document();
    addDocument(iWriter, "qwert_asdfghjkl"); // two words
    iWriter.close();

    // search
    IndexReader iReader = DirectoryReader.open(directory);
    IndexSearcher iSearcher = new IndexSearcher(iReader);
    // parser: AND connected queries
    QueryParser queryParser = new QueryParser(FIELD_NAME, analyzer);
    queryParser.setDefaultOperator(QueryParser.Operator.AND);

    // simple searches (AND connected)
    searchTest(iSearcher, queryParser, "qwert", 1); // first word
    searchTest(iSearcher, queryParser, "asdfghjkl", 1); // second word
    searchTest(iSearcher, queryParser, "asdf", 0); // part of second word
    searchTest(iSearcher, queryParser, "asdf*", 1); // prefix of second word
    searchTest(iSearcher, queryParser, "qwert asdf", 0); // first AND incomplete second word
    // yields a hit when OR is the default operator

    // for normal searches, '_' and ' ' are both interpreted as sepators
    searchTest(iSearcher, queryParser, "qwertasdfghjkl", 0); // w/o separator
    searchTest(iSearcher, queryParser, "qwert asdfghjkl", 1); // both words (space)
    searchTest(iSearcher, queryParser, "qwert_asdfghjkl", 1); // both words (underscore)
    searchTest(iSearcher, queryParser, "qwert%asdfghjkl", 1); // both words (percent)

    // for wildcard searches, '_' is _not_ considered a separator, yielding no results
    searchTest(iSearcher, queryParser, "qwert asdf*", 1); // first AND prefix of second word
    searchTest(iSearcher, queryParser, "qwert_asdf*", 1); // first AND prefix of second word
    searchTest(iSearcher, queryParser, "qwert%asdf*", 1); // first AND prefix of second word
  }

}
