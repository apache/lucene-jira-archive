package org.apache.lucene.store.instantiated;

import junit.framework.TestCase;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

public class TestWithMultiReader extends TestCase {

	public void test() throws Exception {
		Directory dir = new RAMDirectory();
		IndexWriter writer = new IndexWriter(dir, new SimpleAnalyzer(),
				MaxFieldLength.LIMITED);

		Document doc = new Document();
		doc.add(new Field("f", "a", Field.Store.NO, Field.Index.NOT_ANALYZED));
		writer.addDocument(doc);
		writer.commit();

		IndexReader reader = IndexReader.open(dir);

		InstantiatedIndex index = new InstantiatedIndex();
		InstantiatedIndexReader ireader = new InstantiatedIndexReader(index);

		MultiReader multiReader = new MultiReader(new IndexReader[] { reader,
				ireader });

		IndexSearcher searcher = new IndexSearcher(multiReader);

		assertEquals(1,
				searcher.search(new TermQuery(new Term("f", "a")), 1).totalHits);
	}

}
