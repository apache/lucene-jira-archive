package com.rogamore.lucene;

import java.util.ArrayList;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.index.FacetFields;
import org.apache.lucene.facet.params.FacetSearchParams;
import org.apache.lucene.facet.search.CountFacetRequest;
import org.apache.lucene.facet.search.FacetResultNode;
import org.apache.lucene.facet.search.FacetsCollector;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class FacetTest {

	public static void main(String[] args) throws Exception {
		// 0. Specify the analyzer for tokenizing text.
		// The same analyzer should be used for indexing and searching
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_44);

		// 1. create the index and facet
		Directory index = new RAMDirectory();
		Directory taxoIndex = new RAMDirectory();

		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_44, analyzer);

		IndexWriter w = new IndexWriter(index, config);
		TaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoIndex, OpenMode.CREATE);
		FacetFields facetFields = new FacetFields(taxoWriter);

		// add a root document
		Document doc = new Document();
		doc.add(new TextField("title", "All Books", Field.Store.YES));
		ArrayList<CategoryPath> categoryPaths = new ArrayList<CategoryPath>();		
		categoryPaths.add(new CategoryPath("book"));
		facetFields.addFields(doc, categoryPaths );
		w.addDocument(doc);

		// add a leaf document
		doc = new Document();
		doc.add(new TextField("title", "Lucene in Action", Field.Store.YES));
		categoryPaths = new ArrayList<CategoryPath>();		
		categoryPaths.add(new CategoryPath("book|comp", '|'));
		facetFields.addFields(doc, categoryPaths );
		w.addDocument(doc);

		w.close();
		taxoWriter.close();

		// 2. query
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoIndex);

		Query query = new QueryParser(Version.LUCENE_44, "title", analyzer).parse("lucene");

		FacetSearchParams facetSearchParams = new FacetSearchParams(new CountFacetRequest(new CategoryPath("book", '|'), 10));
		FacetsCollector facetsCollector = FacetsCollector.create(facetSearchParams, reader, taxoReader);
		searcher.search(query, facetsCollector );
		//
		FacetResultNode facetNode = facetsCollector.getFacetResults().get(0).getFacetResultNode();
		System.out.println(facetNode.label.toString('|') + ":total:" +  ((int)facetNode.value) );
		
		int subTotal = 0;
		for (FacetResultNode subNode: facetNode.subResults)  {
			subTotal += subNode.value;
		}
		System.out.println(facetNode.label.toString('|') + ":subTotal:" + subTotal);

		taxoReader.close();
		reader.close();

	}
}