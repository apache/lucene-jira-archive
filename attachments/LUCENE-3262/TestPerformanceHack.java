package org.apache.lucene.facet.example;

import org.apache.lucene.DocumentBuilder;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.facet.example.simple.SimpleUtils;
import org.apache.lucene.facet.index.CategoryDocumentBuilder;
import org.apache.lucene.facet.index.params.DefaultFacetIndexingParams;
import org.apache.lucene.facet.search.FacetsCollector;
import org.apache.lucene.facet.search.params.CountFacetRequest;
import org.apache.lucene.facet.search.params.FacetSearchParams;
import org.apache.lucene.facet.search.results.FacetResult;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.lucene.LuceneTaxonomyReader;
import org.apache.lucene.facet.taxonomy.lucene.LuceneTaxonomyWriter;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.LuceneTestCase;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * A very ugly and hacky performance test, just meant to probe LUCENE-3097.
 */
public class TestPerformanceHack extends LuceneTestCase {

//  public static final String HIERARCHICAL = "deep";
  public static final String ID = "id";     // doc #0 = "0", doc #1 = "1" etc
  public static final String EVEN = "even"; // "true" or "false"
  public static final String ALL = "all";   // all == "all"
  public static final String MOD = "mods";  // all primes up to 101 where
                                            // docID % prime == 0
  public static final int[] PRIMES = new int[]{
      2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67,
      71, 73, 79, 83, 89, 97, 101};

  public static final DecimalFormat ID_FORMAT = new DecimalFormat("00000000");

  private static final File TESTDIR = new File("performancetest.delete");
  private static final File INDEX = new File(TESTDIR, "index");
  private static final File TAXO = new File(TESTDIR, "taxo");

  @Override
  public void setUp() throws Exception {
    super.setUp();
    prepareFolders();
  }

  private void prepareFolders() {
    if (INDEX.exists()) {
      delete(INDEX);
    }
    if (TAXO.exists()) {
      delete(TAXO);
    }
    if (!(INDEX.mkdirs() && TAXO.mkdirs())) {
      System.err.println(
          "Unable to create dirs '" + INDEX + "' and '" + TAXO + "'");
    }
  }

  private void delete(File file) {
    if (file.isDirectory()) {
      for (File sub: file.listFiles()) {
        delete(sub);
      }
    }
    file.delete();
  }

  public void testCreateIndex()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    createIndex(1, 1, 1);
  }

  /*
   * All the testAccessIndex tests were written for an old and simple corpus
   * generation method.
   */

  public void testAccessIndex_2_2_2()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(2, 2, 2, 5, 5);
  }

  public void testAccessIndex_10000_2_4()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(10000, 2, 4, 5, 5);
  }

  public void testAccessIndex_100000_2_4_d5()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(100000, 2, 4, 5, 5);
  }

  public void testAccessIndex_100000_2_4_d1()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(100000, 2, 4, 5, 1);
  }

  public void testAccessIndex_1000000_2_4()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(1000000, 2, 4, 5, 5);
  }

  public void testAccessIndex_1000000_4_3()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(1000000, 4, 3, 5, 5);
  }

  public void testAccessIndex_1000000_3_4_d5()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(1000000, 3, 4, 5, 5);
  }

  // Requires about 3GB to run on 64bit Java
  public void testAccessIndex_5000000_3_6_d6()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(5000000, 3, 6, 5, 6);
  }

  // Requires about 3GB to run on 64bit Java
  public void testAccessIndex_5000000_3_6_d1()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(5000000, 3, 6, 5, 1);
  }

  public void testAccessIndex_1000_5_10()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(1000, 5, 10, 5, 5);
  }

  // 45 unique tags, 47M references, GC exceeded on 1.8GB
  public void testAccessIndex_1000_7_10()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(1000, 7, 10, 5, 5);
  }

  public void testAccessIndex_100000000_1_4_d1()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(100000000, 1, 4, 5, 1);
  }

  public void testAccessIndex(
      int docs, int maxTags, int maxLevels, int num, int depth)
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    createIndex(docs, maxTags, maxLevels);

    performSearch(num, depth,
        EVEN + ":true", EVEN + ":true", EVEN + ":true", EVEN + ":true");
  }

  /*
   * The testBinomial demonstrates some of the possibilities with the
   * CorpusGenerator.
   */

  public void testBinomial()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    Random random = new Random(87);
    final int DOCS = 1000;

    CorpusGenerator corpus = new CorpusGenerator();
    // There's a 10% chance for any given document that it will have a path
    corpus.setPaths(new CorpusGenerator.Binomial(random, 0, 1, 0.1));
    // The paths are 2-6 levels deep, with most paths being 4 levels deep
    corpus.setDepths(new CorpusGenerator.Binomial(random, 2, 6, 0.5));
    // 10 unique tags/level with equal chance for each tag
    corpus.setTags(new CorpusGenerator.SimplePathElementProducer(
            "tag_", new CorpusGenerator.LevelWrapper(
        2, // doc, path, level
          new CorpusGenerator.SimpleRandom(random, 1, 10), // level 0
          new CorpusGenerator.SimpleRandom(random, 1, 10), // level 1
          new CorpusGenerator.SimpleRandom(random, 1, 10), // ...
          new CorpusGenerator.SimpleRandom(random, 1, 10),
          new CorpusGenerator.SimpleRandom(random, 1, 10),
          new CorpusGenerator.SimpleRandom(random, 1, 10)
    )));

    createIndex(DOCS, corpus);
    performSearch(10, 1,
        EVEN + ":true", // Every even
        MOD + ":3", // every third
        MOD + ":101", // every 101th
        EVEN + ":true"); // even again to test speed when fully warmed
  }

  /**
   *
   * @param num maximum number of tags to return.
   * @param depth the maximum depth to perform recursive count for.
   * @param queries the queries to issue.
   * @throws IOException if the faceting search fails.
   */
  public void performSearch(
      int num, int depth, String... queries) throws IOException {
    // create Directories for the search index and for the taxonomy index
    // open readers
    TaxonomyReader taxo = new LuceneTaxonomyReader(FSDirectory.open(TAXO));
    IndexReader ir = IndexReader.open(FSDirectory.open(INDEX), true);
    IndexSearcher searcher = new IndexSearcher(ir);

    long preMem = getMem();
    String lastAnswer = null;
    for (String query: queries) {
      Query lQuery = parseQuery(query);
      long facetTime = -System.currentTimeMillis();

      List<FacetResult> facetRes = performSearch(
          ir, searcher, taxo, lQuery, num, depth);
      facetTime += System.currentTimeMillis();
      int hits = searcher.search(lQuery, 1).totalHits;

      for (FacetResult facetResult : facetRes) {
        System.out.println(
            "Query '" + query + "' got " + hits + " hits and "
            + facetResult.getNumValidDescendants()
            + " valid descendants when faceted in " + facetTime + "ms");
        lastAnswer = facetResult.toString();
      }
    }
    long postMem = getMem();
    System.out.println(
        "Pre facet mem: " + preMem + "MB, post facet mem: " + postMem + "MB");
    System.out.println(lastAnswer);
    taxo.close();
    ir.close();
  }

  // MB
  public static long getMem() {
    System.gc();
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      //noinspection CallToPrintStackTrace
      e.printStackTrace();
    }
    System.gc();
    return (Runtime.getRuntime().totalMemory()
        - Runtime.getRuntime().freeMemory()) / 1048576;
  }

  private List<FacetResult> performSearch(
      IndexReader ir,
      IndexSearcher is, TaxonomyReader taxo, Query query, int num, int depth)
                                                            throws IOException {
    CountFacetRequest facetRequest = new CountFacetRequest(
        new CategoryPath(), num);
    facetRequest.setDepth(depth);
    TopScoreDocCollector topDocsCol =
        TopScoreDocCollector.create(num, true);

    FacetSearchParams facetSearchParams =
        new FacetSearchParams(new DefaultFacetIndexingParams());
    facetSearchParams.addFacetRequest(facetRequest);
    FacetsCollector facetsCollector = new FacetsCollector(
        facetSearchParams, ir, taxo);
    is.search(query, MultiCollector.wrap(topDocsCol, facetsCollector));
    return facetsCollector.getFacetResults();
  }

  // Trivial query parser
  private Query parseQuery(String query) {
    return new TermQuery(new Term(query.split(":")[0], query.split(":")[1]));
  }

  public static long createIndex(
      int docCount, int maxTagsPerLevel, int maxLevel)
      throws IOException, DocumentBuilder.DocumentBuilderException {
    Random random = new Random(87);
    CorpusGenerator corpus = new CorpusGenerator();
    corpus.setDepths(
        new CorpusGenerator.SimpleRandom(random, 0, maxLevel));
    corpus.setPaths(
        new CorpusGenerator.SimpleRandom(random, 0, maxTagsPerLevel));
    corpus.setTags(
        new CorpusGenerator.SimplePathElementProducer(
            "", new CorpusGenerator.SimpleRandom(random, 1, 58)));
    return createIndex(docCount, corpus);
  }

  private static long createIndex(int docCount, CorpusGenerator corpus)
      throws IOException, DocumentBuilder.DocumentBuilderException {
    if (INDEX.listFiles().length > 0) {
      System.out.println(
          "Index already exists at '" + INDEX + "'. Skipping creation");
      return 0;
    }
    FSDirectory indexDir = FSDirectory.open(INDEX);
    FSDirectory taxoDir = FSDirectory.open(TAXO);
    return createIndex(indexDir, taxoDir, docCount, corpus);
  }

  private static long createIndex(FSDirectory indexDir, FSDirectory taxoDir,
                                  int docCount, CorpusGenerator corpus)
      throws IOException, DocumentBuilder.DocumentBuilderException {
    IndexWriter iw = new IndexWriter(indexDir, new IndexWriterConfig(
        ExampleUtils.EXAMPLE_VER, SimpleUtils.analyzer));
    TaxonomyWriter taxo = new LuceneTaxonomyWriter(
        taxoDir, IndexWriterConfig.OpenMode.CREATE);

    long startTime = System.nanoTime();
    long references = 0;

    int every = docCount > 100 ? docCount / 100 : 1;
    int next = every;

    for (int docID = 0 ; docID < docCount ; docID++) {
      if (docID == next) {
        System.out.print(".");
        next += every;
      }
      Document doc = new Document();

      doc.add(new Field(ID, ID_FORMAT.format(docID),
          Field.Store.YES, Field.Index.NOT_ANALYZED));
      doc.add(new Field(EVEN, docID % 2 == 0 ? "true" : "false",
          Field.Store.NO, Field.Index.NOT_ANALYZED));
      doc.add(new Field(ALL, ALL,
          Field.Store.NO, Field.Index.NOT_ANALYZED));
      for (int prime: PRIMES) {
        if (docID % prime == 0) {
          doc.add(new Field(MOD, Integer.toString(prime),
              Field.Store.NO, Field.Index.NOT_ANALYZED));
        }
      }
      references += addHierarchicalTags(taxo, doc, docID, corpus);

      iw.addDocument(doc);
    }
    System.out.print("|");
    taxo.commit();
    iw.commit();

    taxo.close();
    iw.close();
    System.out.println("");
    System.out.println(String.format(
        "Created %d document index with " + references
            + " tag references in %sms", docCount,
        (System.nanoTime() - startTime) / 1000000));
    return references;
  }

  private static long addHierarchicalTags(
      TaxonomyWriter taxo, Document doc, int docID, CorpusGenerator corpus)
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    String[][] paths = corpus.getPaths(docID);
    List<CategoryPath> facetList = new ArrayList<CategoryPath>(paths.length);
    long pathCount = 0;
    for (String[] path: paths) {
      if (path.length == 0) {
        continue;
      }
      facetList.add(new CategoryPath(path));
      pathCount++;
    }
    DocumentBuilder categoryDocBuilder = new CategoryDocumentBuilder(taxo).
        setCategoryPaths(facetList);
    categoryDocBuilder.build(doc);

    return pathCount;
  }
}
