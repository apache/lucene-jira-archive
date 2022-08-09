package test;

import java.io.File;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;

public class Lucene5367 {

	public static void main(String[] args) throws Exception {
		Directory indexDirectory = new NIOFSDirectory(new File("~/index"));
		Directory taxonomyDirectory = new NIOFSDirectory(new File("~/taxonomy"));
		IndexWriter writer = new IndexWriter(indexDirectory,
				new IndexWriterConfig(Version.LUCENE_50, new KeywordAnalyzer()));
		DirectoryTaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(
				taxonomyDirectory);
		FacetsConfig config = new FacetsConfig();
		config.setHierarchical("Publish Date", true);
		Document doc = new Document();
		doc.add(new FacetField("Author", "Bob"));
		doc.add(new FacetField("Publish Date", "2010", "10", "15"));

		writer.addDocument(config.build(taxoWriter, doc));
		writer.addDocument(config.build(taxoWriter, doc));

		writer.close();
		taxoWriter.close();
	}
}