

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.facet.index.CategoryDocumentBuilder;
import org.apache.lucene.facet.search.FacetsCollector;
import org.apache.lucene.facet.search.params.CountFacetRequest;
import org.apache.lucene.facet.search.params.FacetSearchParams;
import org.apache.lucene.facet.search.results.FacetResult;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.Assert;
import org.junit.Test;

public class LuceneFacetTest {

	@Test
	public void testDuplicateFacetRequest() throws Exception {
		IndexWriter writer = new IndexWriter(new RAMDirectory(), new IndexWriterConfig(Version.LUCENE_36, new StandardAnalyzer(Version.LUCENE_36)));
		Directory taxoDir = new RAMDirectory();
		TaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoDir, OpenMode.CREATE);

		Document doc = new Document();
		doc.add(new Field("title", "simple text title", Store.YES, org.apache.lucene.document.Field.Index.ANALYZED));
		List<CategoryPath> categories = new ArrayList<CategoryPath>();
		categories.add(new CategoryPath("author", "Mark Twain"));
		categories.add(new CategoryPath("year", "2010"));
		CategoryDocumentBuilder categoryDocBuilder = new CategoryDocumentBuilder(taxoWriter);
		categoryDocBuilder.setCategoryPaths(categories);
		categoryDocBuilder.build(doc);
		writer.addDocument(doc);
		writer.commit();
		taxoWriter.commit();

		IndexReader indexReader = IndexReader.open(writer, true);
		IndexSearcher searcher = new IndexSearcher(indexReader);
		TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDir);
		Query q = new TermQuery(new Term("title", "text"));

		TopScoreDocCollector tdc = TopScoreDocCollector.create(10, true);

		FacetSearchParams facetSearchParams = new FacetSearchParams();
		facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("author"), 10));
		facetSearchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("author"), 10));

		FacetsCollector facetsCollector = new FacetsCollector(facetSearchParams, indexReader, taxoReader);
		searcher.search(q, MultiCollector.wrap(tdc, facetsCollector));
		List<FacetResult> res = facetsCollector.getFacetResults();

		Assert.assertEquals("Only Mark Twain should be returned as result", 1, res.get(0).getNumValidDescendants());
		Assert.assertEquals("Only Mark Twain should be returned as result", 1, res.get(1).getNumValidDescendants());
	}
}
