package org.apache.lucene.search;

import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

// Lucene 2.2.0
// 5 documents are added to the index.
// 2 fields per document:
//    text: contains a short string.
//    dateTime: contains a date/time string.
// The DateTools class is used to convert the time to a string.
// The testReverseDateSort method below fails. 
public class TestDateSort extends TestCase {

  private static final String TEXT_FIELD = "text";
  private static final String DATE_TIME_FIELD = "dateTime";

  private static Directory directory;

  public void setUp() throws Exception {
    // Create an index writer.
    directory = new RAMDirectory();
    IndexWriter writer = new IndexWriter(directory, new WhitespaceAnalyzer(), true);

    // oldest doc:
    // Add the first document.  text = "Document 1"  dateTime = Oct 10 03:25:22 EDT 2007
    writer.addDocument(createDocument("Document 1", 1192001122000L));
    // Add the second document.  text = "Document 2"  dateTime = Oct 10 03:25:26 EDT 2007 
    writer.addDocument(createDocument("Document 2", 1192001126000L));
    // Add the third document.  text = "Document 3"  dateTime = Oct 11 07:12:13 EDT 2007 
    writer.addDocument(createDocument("Document 3", 1192101133000L));
    // Add the fourth document.  text = "Document 4"  dateTime = Oct 11 08:02:09 EDT 2007
    writer.addDocument(createDocument("Document 4", 1192104129000L));
    // latest doc:
    // Add the fifth document.  text = "Document 5"  dateTime = Oct 12 13:25:43 EDT 2007
    writer.addDocument(createDocument("Document 5", 1192209943000L));

    writer.optimize();
    writer.close();
  }

  public void testReverseDateSort() throws Exception {
    IndexSearcher searcher = new IndexSearcher(directory);

    // Create a Sort object.  reverse is set to true.
    // problem occurs only with SortField.AUTO:
    Sort sort = new Sort(new SortField(DATE_TIME_FIELD, SortField.AUTO, true));

    QueryParser queryParser = new QueryParser(TEXT_FIELD, new WhitespaceAnalyzer());
    Query query = queryParser.parse("Document");

    // Execute the search and process the search results.
    String[] actualOrder = new String[5];
    Hits hits = searcher.search(query, sort);
    for (int i = 0; i < hits.length(); i++) {
      Document document = hits.doc(i);
      String text = document.get(TEXT_FIELD);
      actualOrder[i] = text;
    }
    searcher.close();

    // Set up the expected order (i.e. Document 5, 4, 3, 2, 1).
    String[] expectedOrder = new String[5];
    expectedOrder[0] = "Document 5";
    expectedOrder[1] = "Document 4";
    expectedOrder[2] = "Document 3";
    expectedOrder[3] = "Document 2";
    expectedOrder[4] = "Document 1";

    assertEquals(Arrays.asList(expectedOrder), Arrays.asList(actualOrder));
  }

  private static Document createDocument(String text, long time) {
    Document document = new Document();

    // Add the text field.
    Field textField = new Field(TEXT_FIELD, text, Field.Store.YES, Field.Index.TOKENIZED);
    document.add(textField);

    // Add the date/time field.
    String dateTimeString = DateTools.timeToString(time, DateTools.Resolution.SECOND);
    Field dateTimeField = new Field(DATE_TIME_FIELD, dateTimeString, Field.Store.YES,
        Field.Index.UN_TOKENIZED);
    document.add(dateTimeField);

    return document;
  }

}
