/* $Id$ */
package org.apache.lucene.search.highlight;

/**
 * Copyright 2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.TestCase;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.RAMDirectory;


/**
 * @author  Ronnie Kolehmainen (ronnie.kolehmainen at ub.uu.se)
 * @version $Revision$, $Date$
 */
public class FulltextHighlighterTest
        extends TestCase
{
  RAMDirectory ramDir = null;
  IndexWriter writer = null;
  IndexReader reader = null;
  IndexSearcher searcher = null;
  
  public void setUp()
  throws Exception
  {
    //setup index
    if (searcher == null)
    {
      ramDir = new RAMDirectory();
      writer = new IndexWriter(ramDir, new StandardAnalyzer(), true);
      Document d = new Document();
      Field f = new Field(FIELD_NAME,
              LOREM_IPSUM,
              Field.Store.YES,
              Field.Index.TOKENIZED,
              Field.TermVector.WITH_OFFSETS);
      d.add(f);
      writer.addDocument(d);
      writer.optimize();
      writer.close();
      reader = IndexReader.open(ramDir);
      searcher = new IndexSearcher(reader);
    }
  }
  
  public void testHighlightStart()
  throws Exception
  {
    Query query = new TermQuery(new Term(FIELD_NAME, "lorem"));
    query = query.rewrite(reader);
    Hits hits = searcher.search(query);
    assertTrue("hits.length() == 1", hits.length() == 1);
    String highlightedText = FulltextHighlighter.highlight(reader,
            query,
            FIELD_NAME,
            hits.id(0),
            hits.doc(0),
            "<b>",
            "</b>",
            3,
            40);
    System.out.println("testHighlightStart");
    System.out.println(highlightedText);
    assertEquals("EXPECTED_VALUES[0]", highlightedText, EXPECTED_VALUES[0]);
  }
  
  public void testHighlightEnd()
  throws Exception
  {
    Query query = new TermQuery(new Term(FIELD_NAME, "laoreet"));
    query = query.rewrite(reader);
    Hits hits = searcher.search(query);
    assertTrue("hits.length() == 1", hits.length() == 1);
    String highlightedText = FulltextHighlighter.highlight(reader,
            query,
            FIELD_NAME,
            hits.id(0),
            hits.doc(0),
            "<b>",
            "</b>",
            3,
            40);
    System.out.println("testHighlightEnd");
    System.out.println(highlightedText);
    assertEquals("EXPECTED_VALUES[1]", highlightedText, EXPECTED_VALUES[1]);
  }
  
  public void testHighlightBeginning()
  throws Exception
  {
    Query query = new TermQuery(new Term(FIELD_NAME, "ipsum"));
    query = query.rewrite(reader);
    Hits hits = searcher.search(query);
    assertTrue("hits.length() == 1", hits.length() == 1);
    String highlightedText = FulltextHighlighter.highlight(reader,
            query,
            FIELD_NAME,
            hits.id(0),
            hits.doc(0),
            "<b>",
            "</b>",
            3,
            40);
    System.out.println("testHighlightBeginning");
    System.out.println(highlightedText);
    assertEquals("EXPECTED_VALUES[2]", highlightedText, EXPECTED_VALUES[2]);
  }
  
  public void testHighlightWildcardQuery()
  throws Exception
  {
    Query query = new WildcardQuery(new Term(FIELD_NAME, "a*am"));
    query = query.rewrite(reader);
    Hits hits = searcher.search(query);
    assertTrue("hits.length() == 1", hits.length() == 1);
    String highlightedText = FulltextHighlighter.highlight(reader,
            query,
            FIELD_NAME,
            hits.id(0),
            hits.doc(0),
            "<b>",
            "</b>",
            3,
            40);
    System.out.println("testHighlightWildcardQuery");
    System.out.println(highlightedText);
    assertEquals("EXPECTED_VALUES[3]", highlightedText, EXPECTED_VALUES[3]);
  }
  
  public void testShortHighlight()
  throws Exception
  {
    Query query = new TermQuery(new Term(FIELD_NAME, "ipsum"));
    query = query.rewrite(reader);
    Hits hits = searcher.search(query);
    assertTrue("hits.length() == 1", hits.length() == 1);
    String highlightedText = FulltextHighlighter.highlight(reader,
            query,
            FIELD_NAME,
            hits.id(0),
            hits.doc(0),
            "<b>",
            "</b>",
            10,
            0);
    System.out.println("testShortHighlight");
    System.out.println(highlightedText);
    assertEquals("EXPECTED_VALUES[4]", highlightedText, EXPECTED_VALUES[4]);
  }
  
  public void testShortHighlightEnd()
  throws Exception
  {
    Query query = new TermQuery(new Term(FIELD_NAME, "laoreet"));
    query = query.rewrite(reader);
    Hits hits = searcher.search(query);
    assertTrue("hits.length() == 1", hits.length() == 1);
    String highlightedText = FulltextHighlighter.highlight(reader,
            query,
            FIELD_NAME,
            hits.id(0),
            hits.doc(0),
            "<b>",
            "</b>",
            10,
            0);
    System.out.println("testShortHighlightEnd");
    System.out.println(highlightedText);
    assertEquals("EXPECTED_VALUES[9]", highlightedText, EXPECTED_VALUES[9]);
  }
  
  public void testShortHighlightEnd2()
  throws Exception
  {
    Query query = new TermQuery(new Term(FIELD_NAME, "rhoncus"));
    query = query.rewrite(reader);
    Hits hits = searcher.search(query);
    assertTrue("hits.length() == 1", hits.length() == 1);
    String highlightedText = FulltextHighlighter.highlight(reader,
            query,
            FIELD_NAME,
            hits.id(0),
            hits.doc(0),
            "<b>",
            "</b>",
            10,
            40);
    System.out.println("testShortHighlightEnd2");
    System.out.println(highlightedText);
    //assertEquals("EXPECTED_VALUES[9]", highlightedText, EXPECTED_VALUES[9]);
  }
  
  
  public void testHighlightPrefixQuery()
  throws Exception
  {
    Query query = new PrefixQuery(new Term(FIELD_NAME, "s"));
    query = query.rewrite(reader);
    Hits hits = searcher.search(query);
    assertTrue("hits.length() == 1", hits.length() == 1);
    String highlightedText = FulltextHighlighter.highlight(reader,
            query,
            FIELD_NAME,
            hits.id(0),
            hits.doc(0),
            "<b>",
            "</b>",
            3,
            40);
    System.out.println("testHighlightPrefixQuery");
    System.out.println(highlightedText);
    assertEquals("EXPECTED_VALUES[5]", highlightedText, EXPECTED_VALUES[5]);
  }
  
  public void testShortHighlightBooleanQuery()
  throws Exception
  {
    BooleanQuery bq = new BooleanQuery();
    bq.add(new TermQuery(new Term(FIELD_NAME, "ipsum")), BooleanClause.Occur.SHOULD);
    bq.add(new TermQuery(new Term(FIELD_NAME, "lorem")), BooleanClause.Occur.SHOULD);
    Query query = bq.rewrite(reader);
    Hits hits = searcher.search(query);
    assertTrue("hits.length() == 1", hits.length() == 1);
    String highlightedText = FulltextHighlighter.highlight(reader,
            query,
            FIELD_NAME,
            hits.id(0),
            hits.doc(0),
            "<b>",
            "</b>",
            10,
            0);
    System.out.println("testShortHighlightBooleanQuery");
    System.out.println(highlightedText);
    assertEquals("EXPECTED_VALUES[6]", highlightedText, EXPECTED_VALUES[6]);
  }
  
  public void testShortHighlightBooleanQuery2()
  throws Exception
  {
    BooleanQuery bq = new BooleanQuery();
    bq.add(new TermQuery(new Term(FIELD_NAME, "ipsum")), BooleanClause.Occur.SHOULD);
    bq.add(new TermQuery(new Term(FIELD_NAME, "lorem")), BooleanClause.Occur.SHOULD);
    Query query = bq.rewrite(reader);
    Hits hits = searcher.search(query);
    assertTrue("hits.length() == 1", hits.length() == 1);
    String highlightedText = FulltextHighlighter.highlight(reader,
            query,
            FIELD_NAME,
            hits.id(0),
            hits.doc(0),
            "<b>",
            "</b>",
            10,
            3);
    System.out.println("testShortHighlightBooleanQuery2");
    System.out.println(highlightedText);
    assertEquals("EXPECTED_VALUES[7]", highlightedText, EXPECTED_VALUES[7]);
  }
  
  public void testBestScorers()
  throws Exception
  {
    BooleanQuery bq = new BooleanQuery();
    bq.add(new TermQuery(new Term(FIELD_NAME, "integer")), BooleanClause.Occur.SHOULD);
    bq.add(new TermQuery(new Term(FIELD_NAME, "egestas")), BooleanClause.Occur.SHOULD);
    bq.add(new TermQuery(new Term(FIELD_NAME, "nisl")), BooleanClause.Occur.SHOULD);
    Query query = bq.rewrite(reader);
    Hits hits = searcher.search(query);
    assertTrue("hits.length() == 1", hits.length() == 1);
    String highlightedText = FulltextHighlighter.highlight(reader,
            query,
            FIELD_NAME,
            hits.id(0),
            hits.doc(0),
            "<b>",
            "</b>",
            2,
            40);
    System.out.println("testBestScorers");
    System.out.println(highlightedText);
    assertEquals("EXPECTED_VALUES[8]", highlightedText, EXPECTED_VALUES[8]);
  }
  
  private static final String FIELD_NAME = "fulltext";
  private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Sed eleifend. Suspendisse potenti. Pellentesque nibh ante, facilisis vitae, vulputate mollis, malesuada quis, augue. Maecenas non turpis. Nunc aliquam, urna eu fringilla consequat, lectus risus eleifend ligula, ut sodales sem mauris non libero. Curabitur ultricies nisi a sem. Vestibulum ornare congue est. Donec odio ligula, fringilla vel, venenatis ut, mollis vitae, justo. Aenean erat massa, fermentum eu, vestibulum ut, imperdiet sed, nisl. Vestibulum ac justo. Phasellus enim nibh, cursus nec, tempus sit amet, iaculis a, sapien. Aenean bibendum erat dictum arcu. Nulla urna nisl, laoreet ac, porttitor sit amet, molestie id, mi. Sed sed nunc. Vestibulum turpis urna, vehicula nec, commodo eu, consequat eu, neque. Pellentesque dapibus eros at est. Etiam at orci id enim rutrum condimentum. Donec congue congue ligula. Nulla facilisi. Maecenas non pede a tortor volutpat egestas. Nam augue enim, tempor et, sollicitudin viverra, accumsan vitae, ligula. Aenean mauris metus, interdum at, dapibus eu, venenatis ut, ipsum. Maecenas ornare leo ut nulla. Maecenas nulla arcu, condimentum sed, venenatis non, tempus at, libero. Duis erat. Aliquam erat volutpat. Quisque sed leo sit amet tellus facilisis posuere. Nam pellentesque ullamcorper erat. Vestibulum sagittis, lacus eu congue interdum, magna pede fringilla ipsum, vulputate ullamcorper sapien neque quis tellus. Aliquam elementum velit nec sapien. Quisque imperdiet mauris. Aliquam ut elit. Morbi arcu. Fusce viverra mattis urna. Donec et metus. Aenean ullamcorper lacus. Morbi nec velit. Suspendisse potenti. Quisque vitae enim ut pede porta consectetuer. Nulla facilisi. Aenean eleifend sollicitudin leo. Nam vel enim. Duis nec justo a turpis rhoncus molestie. Aenean pulvinar gravida nisi. Morbi lobortis sapien in dui. Donec scelerisque auctor dolor. Quisque at leo. Proin diam ante, scelerisque quis, semper vel, nonummy ac, nibh. Quisque augue. Duis sodales, magna sodales aliquam rhoncus, magna lectus fringilla magna, sed suscipit ipsum dolor ac sem. Etiam vitae leo. Maecenas semper, augue ut scelerisque fringilla, ligula pede placerat justo, vitae fermentum dui augue a lorem. Fusce egestas tellus quis metus. Pellentesque accumsan. Nulla tortor diam, lacinia nec, feugiat ut, fermentum sit amet, mauris. Donec et ipsum. Fusce ut risus ac lectus adipiscing ornare. Suspendisse ac ipsum. Fusce non nunc. Aenean tempus, risus non sodales egestas, diam nulla blandit quam, id consequat nisi felis ac nisi. Maecenas sagittis convallis velit. Nam pharetra ante aliquet orci. Cras non sapien at ante posuere pulvinar. In adipiscing tortor non arcu. Duis vitae turpis eget lorem sollicitudin ultrices. Nunc augue nulla, ultrices sed, sagittis eleifend, interdum eu, orci. Vestibulum malesuada ligula et eros. Integer egestas nisl eu justo. Sed quis justo aliquam tellus lacinia rhoncus. Fusce laoreet";
  private static final String[] EXPECTED_VALUES = new String[] {
    "<b>Lorem</b> ipsum dolor sit amet, consectetuer...  ...justo, vitae fermentum dui augue a <b>lorem</b>. Fusce egestas tellus quis metus....  ...non arcu. Duis vitae turpis eget <b>lorem</b> sollicitudin ultrices. Nunc augue... ",
            " ...erat dictum arcu. Nulla urna nisl, <b>laoreet</b> ac, porttitor sit amet, molestie id,...  ...aliquam tellus lacinia rhoncus. Fusce <b>laoreet</b>",
            "Lorem <b>ipsum</b> dolor sit amet, consectetuer...  ...interdum at, dapibus eu, venenatis ut, <b>ipsum</b>. Maecenas ornare leo ut nulla....  ...fermentum sit amet, mauris. Donec et <b>ipsum</b>. Fusce ut risus ac lectus adipiscing ornare. Suspendisse ac <b>ipsum</b>. Fusce non nunc. Aenean tempus, risus... ",
            " ...quis, augue. Maecenas non turpis. Nunc <b>aliquam</b>, urna eu fringilla consequat, lectus...  ...non, tempus at, libero. Duis erat. <b>Aliquam</b> erat volutpat. Quisque sed leo sit...  ...ullamcorper sapien neque quis tellus. <b>Aliquam</b> elementum velit nec sapien. Quisque imperdiet mauris. <b>Aliquam</b> ut elit. Morbi arcu. Fusce viverra... ",
            " ...<b>ipsum</b>...  ...<b>ipsum</b>...  ...<b>ipsum</b>...  ...<b>ipsum</b>...  ...<b>ipsum</b>...  ...<b>ipsum</b>... ",
            " ...fermentum eu, vestibulum ut, imperdiet <b>sed</b>, nisl. Vestibulum ac justo. Phasellus enim nibh, cursus nec, tempus <b>sit</b> amet, iaculis a, <b>sapien</b>. Aenean bibendum erat dictum arcu. Nulla urna nisl, laoreet ac, porttitor <b>sit</b> amet, molestie id, mi. <b>Sed</b> <b>sed</b> nunc. Vestibulum turpis urna, vehicula...  ...Maecenas nulla arcu, condimentum <b>sed</b>, venenatis non, tempus at, libero. Duis erat. Aliquam erat volutpat. Quisque <b>sed</b> leo <b>sit</b> amet tellus facilisis posuere. Nam pellentesque ullamcorper erat. Vestibulum <b>sagittis</b>, lacus eu congue interdum, magna pede fringilla ipsum, vulputate ullamcorper <b>sapien</b> neque quis tellus. Aliquam elementum velit nec <b>sapien</b>. Quisque imperdiet mauris. Aliquam ut...  ...pulvinar gravida nisi. Morbi lobortis <b>sapien</b> in dui. Donec <b>scelerisque</b> auctor dolor. Quisque at leo. Proin diam ante, <b>scelerisque</b> quis, <b>semper</b> vel, nonummy ac, nibh. Quisque augue. Duis <b>sodales</b>, magna <b>sodales</b> aliquam rhoncus, magna lectus fringilla magna, <b>sed</b> <b>suscipit</b> ipsum dolor ac <b>sem</b>. Etiam vitae leo. Maecenas <b>semper</b>, augue ut <b>scelerisque</b> fringilla, ligula pede placerat justo,... ",
            "<b>Lorem</b>...  ...<b>ipsum</b>...  ...<b>ipsum</b>...  ...<b>ipsum</b>...  ...<b>ipsum</b>...  ...<b>lorem</b>...  ...<b>ipsum</b>...  ...<b>ipsum</b>...  ...<b>lorem</b>... ",
            "<b>Lorem</b> <b>ipsum</b>...  ...<b>ipsum</b>....  ...<b>ipsum</b>,...  ...<b>ipsum</b>...  ...a <b>lorem</b>....  ...<b>ipsum</b>....  ...<b>ipsum</b>....  ...<b>lorem</b>... ",
            " ...eu, vestibulum ut, imperdiet sed, <b>nisl</b>. Vestibulum ac justo. Phasellus enim...  ...Vestibulum malesuada ligula et eros. <b>Integer</b> <b>egestas</b> <b>nisl</b> eu justo. Sed quis justo aliquam... ",
            " ...<b>laoreet</b>...  ...<b>laoreet</b>"
  };
}
