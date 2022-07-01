/*
 * Created on 22.08.2004
 */
package tester;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import junit.framework.TestCase;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;

/**
 * @author SoftCulture
 */
public class GermanAnalyzerTest extends TestCase
{
  public void testGermanAnalyzer ( ) throws IOException, ParseException
  {
    System.out.println("Testing GermanAnalyzer...");
    //Create index
    System.out.println ("Started indexing ...");
    ArrayList docs = new ArrayList ( );
    docs.add ("Öffentlichkeit Ärger grün");
    docs.add ("hieß eröffnet Straße");
    docs.add ("Müll Einkaufsatmosphäre Baulöwen");
    docs.add ("Eröffnung Geschäftskonzept Grundstück");
    docs.add ("Läden Sperrmüll Großkino");
    docs.add ("städtische Geschoßfläche Übungen");
    docs.add ("Außerdem Kämmerer Schlüsselzuweisungen");
    docs.add ("Bündnis Kinogrundstück Heimstätte");
    docs.add ("Verkaufsgeschäfte getätigt zusätzlich");
    docs.add ("Käufer Verkaufsverträge Straßenreinigungsgebühren");
    Date start = new Date ( );
    Analyzer analyzer = new GermanAnalyzer ( );
    IndexWriter writer = new IndexWriter ("index", analyzer, true);
    int count = 0;
    for (Iterator iter = docs.iterator ( ); iter.hasNext ( );)
    {
      count++;
      String contents = (String) iter.next ( );
      writer.addDocument (createDocument (count, contents));
    }
    writer.optimize ( );
    writer.close ( );
    System.out.println ("Finished indexing ...");
    //Test searches
    System.out.println ("Started searching ...");
    String [ ] searchWords = {"Öffentlichkeit", "Ärger", "grün", "hieß", "eröffnet", "Straße", "Müll",
        "Einkaufsatmosphäre", "Baulöwen", "Eröffnung", "Geschäftskonzept", "Grundstück", "Läden", "Sperrmüll",
        "Großkino", "städtische", "Geschoßfläche", "Übungen", "Außerdem", "Kämmerer", "Schlüsselzuweisungen",
        "Bündnis", "Kinogrundstück", "Heimstätte", "Verkaufsgeschäfte", "getätigt", "zusätzlich", "Käufer",
        "Verkaufsverträge", "Straßenreinigungsgebühren"};
    Searcher searcher = new IndexSearcher ("index");
    for (int i = 0; i < searchWords.length; i++)
    {
      String word = searchWords [i];
      System.out.println ("Word: " + word);
      Query query = QueryParser.parse (word, "contents", analyzer);
      System.out.println ("Searching for: " + query.toString ("contents"));
      Hits hits = searcher.search (query);
      assertTrue (hits.length ( ) == 1);
      for (int begin = 0; begin < hits.length ( ); begin++)
      {
        Document doc = hits.doc (begin);
        String id = doc.get ("id");
        if (id != null)
        {
          System.out.println ("ID: " + id);
        }
        String contents = doc.get ("contents");
        System.out.println ("Contents: " + contents);
      }
    }
    searcher.close ( );
    System.out.println("Finished searching!");
  }
  public void testGermanAnalyzerWithWildcards ( ) throws IOException, ParseException
  {
    System.out.println("Testing GermanAnalyzer with wildcard searches ...");
    //Create index
    System.out.println ("Started indexing ...");
    ArrayList docs = new ArrayList ( );
    docs.add ("Öffentlichkeit Ärger grün");
    docs.add ("hieß eröffnet Straße");
    docs.add ("Müll Einkaufsatmosphäre Baulöwen");
    docs.add ("Eröffnung Geschäftskonzept Grundstück");
    docs.add ("Läden Sperrmüll Großkino");
    docs.add ("städtische Geschoßfläche Übungen");
    docs.add ("Außerdem Kämmerer Schlüsselzuweisungen");
    docs.add ("Bündnis Kinogrundstück Heimstätte");
    docs.add ("Verkaufsgeschäfte getätigt zusätzlich");
    docs.add ("Käufer Verkaufsverträge Straßenreinigungsgebühren");
    Date start = new Date ( );
    Analyzer analyzer = new GermanAnalyzer ( );
    IndexWriter writer = new IndexWriter ("index", analyzer, true);
    int count = 0;
    for (Iterator iter = docs.iterator ( ); iter.hasNext ( );)
    {
      count++;
      String contents = (String) iter.next ( );
      writer.addDocument (createDocument (count, contents));
    }
    writer.optimize ( );
    writer.close ( );
    System.out.println ("Finished indexing ...");
    //Test searches
    System.out.println ("Started searching ...");
    String [ ] searchWords = {"Öffent*", "Ärg*", "grü?", "hie*", "eröff*", "Straß*", "Mü*",
        "Einkaufs*", "Baulö*", "Eröf*", "Geschäftsk*", "Grund*", "Läd*", "Sperr*",
        "Großk*", "städt*", "Geschoß*", "Üb*", "Außer*", "Kämm*", "Schlüssel*",
        "Bün*", "Kino*", "Heim*", "Verkauf*", "getä*", "zusät*", "Käu*",
        "Verka*", "Straßenrein*"};
    Searcher searcher = new IndexSearcher ("index");
    for (int i = 0; i < searchWords.length; i++)
    {
      String word = searchWords [i];
      System.out.println ("Word: " + word);
      Query query = QueryParser.parse (word, "contents", analyzer);
      System.out.println ("Searching for: " + query.toString ("contents"));
      Hits hits = searcher.search (query);
      assertTrue (hits.length ( ) > 0);
      for (int begin = 0; begin < hits.length ( ); begin++)
      {
        Document doc = hits.doc (begin);
        String id = doc.get ("id");
        if (id != null)
        {
          System.out.println ("ID: " + id);
        }
        String contents = doc.get ("contents");
        System.out.println ("Contents: " + contents);
      }
    }
    searcher.close ( );
    System.out.println("Finished searching!");
  }
  public static Document createDocument (int id, String contents) throws java.io.FileNotFoundException
  {
    // make a new, empty document
    Document doc = new Document ( );
    doc.add (Field.Keyword ("id", String.valueOf (id)));
    doc.add (Field.Text ("contents", contents));
    // return the document
    return doc;
  }
}