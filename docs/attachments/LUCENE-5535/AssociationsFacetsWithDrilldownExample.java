package org.apache.lucene.demo.facet;

/*
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.FloatAssociationFacetField;
import org.apache.lucene.facet.taxonomy.IntAssociationFacetField;
import org.apache.lucene.facet.taxonomy.TaxonomyFacetSumFloatAssociations;
import org.apache.lucene.facet.taxonomy.TaxonomyFacetSumIntAssociations;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

/** Shows example usage of category associations. */
public class AssociationsFacetsWithDrilldownExample {

  private final Directory indexDir = new RAMDirectory();
  private final Directory taxoDir = new RAMDirectory();
  
  private final Directory indexDir2 = new RAMDirectory();
  private final Directory taxoDir2 = new RAMDirectory();
  
  private final FacetsConfig config;

  /** Empty constructor */
  public AssociationsFacetsWithDrilldownExample() {
    config = new FacetsConfig();
    config.setMultiValued("tags", true);
    config.setIndexFieldName("tags", "$tags");
    config.setMultiValued("genre", true);
    config.setIndexFieldName("genre", "$genre");
  }
  
  /** Build the example index. */
  private void index() throws IOException {
    IndexWriterConfig iwc = new IndexWriterConfig(FacetExamples.EXAMPLES_VER, 
                                                  new WhitespaceAnalyzer(FacetExamples.EXAMPLES_VER));
    IndexWriter indexWriter = new IndexWriter(indexDir, iwc);

    // Writes facet ords to a separate directory from the main index
    DirectoryTaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir);

    Document doc = new Document();
    // 3 occurrences for tag 'lucene'
    doc.add(new IntAssociationFacetField(3, "tags", "lucene"));
    // 87% confidence level of genre 'computing'
    doc.add(new FloatAssociationFacetField(0.87f, "genre", "computing"));
    indexWriter.addDocument(config.build(taxoWriter, doc));

    doc = new Document();
    // 1 occurrence for tag 'lucene'
    doc.add(new IntAssociationFacetField(1, "tags", "lucene"));
    // 2 occurrence for tag 'solr'
    doc.add(new IntAssociationFacetField(2, "tags", "solr"));
    // 75% confidence level of genre 'computing'
    doc.add(new FloatAssociationFacetField(0.75f, "genre", "computing"));
    // 34% confidence level of genre 'software'
    doc.add(new FloatAssociationFacetField(0.34f, "genre", "software"));
    indexWriter.addDocument(config.build(taxoWriter, doc));

    indexWriter.close();
    taxoWriter.close();
  }
  
  /** Build the example index. */
  private void index2() throws IOException {
    IndexWriterConfig iwc = new IndexWriterConfig(FacetExamples.EXAMPLES_VER, 
                                                  new WhitespaceAnalyzer(FacetExamples.EXAMPLES_VER));
    IndexWriter indexWriter = new IndexWriter(indexDir2, iwc);

    // Writes facet ords to a separate directory from the main index
    DirectoryTaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir2);

    Document doc = new Document();
    // 3 occurrences for tag 'lucene'
    doc.add(new FacetField("tags", "lucene"));
    // 87% confidence level of genre 'computing'
    doc.add(new FacetField("genre", "computing"));
    indexWriter.addDocument(config.build(taxoWriter, doc));

    doc = new Document();
    // 1 occurrence for tag 'lucene'
    doc.add(new FacetField( "tags", "lucene"));
    // 2 occurrence for tag 'solr'
    doc.add(new FacetField( "tags", "solr"));
    // 75% confidence level of genre 'computing'
    doc.add(new FacetField( "genre", "computing"));
    // 34% confidence level of genre 'software'
    doc.add(new FacetField( "genre", "software"));
    indexWriter.addDocument(config.build(taxoWriter, doc));

    indexWriter.close();
    taxoWriter.close();
  }

  /** User runs a query and aggregates facets by summing their association values. */
  private List<FacetResult> sumAssociations() throws IOException {
    DirectoryReader indexReader = DirectoryReader.open(indexDir);
    IndexSearcher searcher = new IndexSearcher(indexReader);
    TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);
    
    FacetsCollector fc = new FacetsCollector();
    
    // 
    DrillDownQuery q =new DrillDownQuery(config);
    q.add("tags", "lucene");
    
    FacetsCollector.search(searcher, q, 10, fc);
    
    Facets tags = new TaxonomyFacetSumIntAssociations("$tags", taxoReader, config, fc);
    Facets genre = new TaxonomyFacetSumFloatAssociations("$genre", taxoReader, config, fc);

    // Retrieve results
    List<FacetResult> results = new ArrayList<FacetResult>();
    results.add(tags.getTopChildren(10, "tags"));
    results.add(genre.getTopChildren(10, "genre"));

    indexReader.close();
    taxoReader.close();
    
    return results;
  }
  /** User runs a query and aggregates facets by summing their association values. */
  private List<FacetResult> count() throws IOException {
    DirectoryReader indexReader = DirectoryReader.open(indexDir2);
    IndexSearcher searcher = new IndexSearcher(indexReader);
    TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir2);
    
    FacetsCollector fc = new FacetsCollector();
    
    // 
    DrillDownQuery q =new DrillDownQuery(config);
    q.add("tags", "lucene");
    
    FacetsCollector.search(searcher, q, 10, fc);
    
    Facets tags = new FastTaxonomyFacetCounts("$tags", taxoReader, config, fc);

    // Retrieve results
    List<FacetResult> results = new ArrayList<FacetResult>();
    results.add(tags.getTopChildren(10, "tags"));

    indexReader.close();
    taxoReader.close();
    
    return results;
  }
  
  /** Runs summing association example. */
  public List<FacetResult> runSumAssociations() throws IOException {
    index();
    return sumAssociations();
  }
  
  /** Runs summing association example. */
  public List<FacetResult> runCout() throws IOException {
    index2();
    return count();
  }
  
  /** Runs the sum int/float associations examples and prints the results. */
  public static void main(String[] args) throws Exception {
    System.out.println("Sum associations example:");
    System.out.println("-------------------------");
    List<FacetResult> results = new AssociationsFacetsWithDrilldownExample().runSumAssociations();
    System.out.println("tags: " + results.get(0));
    System.out.println("genre: " + results.get(1));
    System.out.println("\n\n");
    System.out.println("Count withouth associations:");
    System.out.println("-------------------------");
    List<FacetResult> results2 = new AssociationsFacetsWithDrilldownExample().runCout();
    System.out.println("tags: " + results2.get(0));
  }
}
