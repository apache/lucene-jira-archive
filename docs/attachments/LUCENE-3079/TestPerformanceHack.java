package org.apache.lucene.facet.example;

import org.apache.lucene.DocumentBuilder;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.facet.example.simple.SimpleMain;
import org.apache.lucene.facet.example.simple.SimpleUtils;
import org.apache.lucene.facet.index.CategoryDocumentBuilder;
import org.apache.lucene.facet.index.params.DefaultFacetIndexingParams;
import org.apache.lucene.facet.search.FacetsCollector;
import org.apache.lucene.facet.search.params.CountFacetRequest;
import org.apache.lucene.facet.search.params.FacetSearchParams;
import org.apache.lucene.facet.search.results.FacetResult;
import org.apache.lucene.facet.search.results.FacetResultNode;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.lucene.LuceneTaxonomyReader;
import org.apache.lucene.facet.taxonomy.lucene.LuceneTaxonomyWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
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

  public static final String HIERARCHICAL = "deep";
  public static final String ID = "id";     // doc #0 = "0", doc #1 = "1" etc
  public static final String EVEN = "even"; // "true" or "false"
  public static final String ALL = "all"; // all == "all"
  public static final String EVEN_NULL = "evennull"; // odd = random content
  public static final String MULTI = "facet"; // 0-5 of values A to Z

  public static final DecimalFormat ID_FORMAT = new DecimalFormat("00000000");
  public static final String TAGS =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";

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

  public void testAccessIndex_2_2_2()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(2, 2, 2);
  }

  public void testAccessIndex_10000_2_4()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(10000, 2, 4);
  }

  public void testAccessIndex_100000_2_4()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(100000, 2, 4);
  }

  public void testAccessIndex_1000000_2_4()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(1000000, 2, 4);
  }

  public void testAccessIndex_1000000_4_3()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(1000000, 4, 3);
  }

  public void testAccessIndex_1000000_3_4()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(1000000, 3, 4);
  }

  // Requires about 3GB to run on 64bit Java
  public void testAccessIndex_5000000_3_6()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(5000000, 3, 6);
  }

  public void testAccessIndex_1000_5_10()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(1000, 5, 10);
  }

  // 45 unique tags, 47M references, GC exceeded on 1.8GB
  public void testAccessIndex_1000_7_10()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(1000, 7, 10);
  }

  public void testAccessIndex_10000000_2_2()
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    testAccessIndex(10000000, 2, 2);
  }

  public void testAccessIndex(int docs, int maxTags, int maxLevels)
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    createIndex(docs, maxTags, maxLevels);

    performSearch(
        EVEN + ":true", EVEN + ":true", EVEN + ":true", EVEN + ":true");
  }

  public void performSearch(String... queries) throws IOException {
    // create Directories for the search index and for the taxonomy index
    // open readers
    TaxonomyReader taxo = new LuceneTaxonomyReader(FSDirectory.open(TAXO));
    IndexReader ir = IndexReader.open(FSDirectory.open(INDEX), true);
    IndexSearcher searcher = new IndexSearcher(ir);

    long preMem = getMem();
    String lastAnswer = null;
    for (String query: queries) {
      long facetTime = -System.currentTimeMillis();
      List<FacetResult> facetRes = performSearch(ir, searcher, taxo, query, 5);
      facetTime += System.currentTimeMillis();

      int i = 0;
      for (FacetResult facetResult : facetRes) {
        System.out.println(
            "Query '" + query + "' faceted in " + facetTime + "ms");
        lastAnswer = facetResult.toString();
      }
    }
    long postMem = getMem();
    System.out.println(
        "Pre facet mem: " + preMem + "MS, post facet mem: " + postMem + "MB");
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
      IndexSearcher is, TaxonomyReader taxo, String query, int num)
                                                            throws IOException {
    Query baseQuery = new TermQuery(new Term(
        query.split(":")[0], query.split(":")[1]));
    CountFacetRequest facetRequest = new CountFacetRequest(
        new CategoryPath(HIERARCHICAL), num);
    facetRequest.setDepth(5);
    TopScoreDocCollector topDocsCol =
        TopScoreDocCollector.create(num, true);

    FacetSearchParams facetSearchParams =
        new FacetSearchParams(new DefaultFacetIndexingParams());
    facetSearchParams.addFacetRequest(facetRequest);
    FacetsCollector facetsCollector = new FacetsCollector(
        facetSearchParams, ir, taxo);
    is.search(baseQuery, MultiCollector.wrap(topDocsCol, facetsCollector));
    return facetsCollector.getFacetResults();
  }


  public static long createIndex(
      int docCount, int maxTagsPerLevel, int maxLevel)
      throws IOException, DocumentBuilder.DocumentBuilderException {
    if (INDEX.listFiles().length > 0) {
      System.out.println(
          "Index already exists at '" + INDEX + "'. Skipping creation");
      return 0;
    }
    FSDirectory indexDir = FSDirectory.open(INDEX);
    FSDirectory taxoDir = FSDirectory.open(TAXO);
    return createIndex(indexDir, taxoDir, docCount, maxTagsPerLevel, maxLevel);
  }

  public static long createIndex(Directory indexDir, Directory taxoDir,
      int docCount, int maxTagsPerLevel, int maxLevel)
      throws IOException, DocumentBuilder.DocumentBuilderException {
    IndexWriter iw = new IndexWriter(indexDir, new IndexWriterConfig(
        ExampleUtils.EXAMPLE_VER, SimpleUtils.analyzer));
    TaxonomyWriter taxo = new LuceneTaxonomyWriter(
        taxoDir, IndexWriterConfig.OpenMode.CREATE);

    long startTime = System.nanoTime();
    long references = 0;
    Random random = new Random(87);

    int every = docCount > 100 ? docCount / 100 : 1;
    int next = every;

    for (int docID = 0 ; docID < docCount ; docID++) {
      if (docID == next) {
        System.out.print(".");
        next += every;
      }
      Document doc = new Document();

      int levels = random.nextInt(maxLevel+1);

      doc.add(new Field(ID, ID_FORMAT.format(docID),
          Field.Store.YES, Field.Index.NOT_ANALYZED));
      doc.add(new Field(EVEN, docID % 2 == 0 ? "true" : "false",
          Field.Store.YES, Field.Index.NOT_ANALYZED));
      doc.add(new Field(ALL, ALL,
          Field.Store.YES, Field.Index.NOT_ANALYZED));
      references += addHierarchicalTags(
          taxo, doc, docID, random,
          levels, HIERARCHICAL + "/", maxTagsPerLevel, false);
      iw.addDocument(doc);
    }
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
      TaxonomyWriter taxo,
      Document doc, int docID, Random random, int levelsLeft, String prefix,
      int maxTagsPerLevel, boolean addDocID)
                  throws IOException, DocumentBuilder.DocumentBuilderException {
    long references = 0;
    if (levelsLeft == 0) {
      return references;
    }
    int tags = random.nextInt(maxTagsPerLevel+1);
    if (tags == 0) {
      tags = 1;
      levelsLeft = 1;
    }
    String docIDAdder = addDocID ? "_" + docID : "";

    List<CategoryPath> facetList = new ArrayList<CategoryPath>(tags);
    for (int i = 0 ; i < tags ; i++) {
      String tag = levelsLeft == 1 ?
          TAGS.charAt(random.nextInt(TAGS.length())) + docIDAdder :
          "" + TAGS.charAt(random.nextInt(TAGS.length()));
      if (levelsLeft == 1 || random.nextInt(10) == 0) {
/*        System.out.println("***");
        for (String s: (prefix + tag).split("/")) {
          System.out.println("Adding " + s);
        }*/
        facetList.add(new CategoryPath((prefix + tag).split("/")));
//        doc.add(new Field(HIERARCHICAL, prefix + tag,
//            Field.Store.NO, Field.Index.NOT_ANALYZED));
        references++;
      }
      references += addHierarchicalTags(taxo, doc, docID, random, levelsLeft-1,
          prefix + tag + "/", maxTagsPerLevel, addDocID);
    }
    DocumentBuilder categoryDocBuilder = new CategoryDocumentBuilder(taxo).
        setCategoryPaths(facetList);
    categoryDocBuilder.build(doc);

    return references;
  }

}
