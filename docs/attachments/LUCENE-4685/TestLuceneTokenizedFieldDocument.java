package com.data2act.domain.search;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

/**
 * Test Basic Lucene Searches
 * <P>
 * 
 * @author raudenaerde
 */
public class TestLuceneTokenizedFieldDocument extends TestCase
{

	private Directory index;

	private SearcherManager sm;

	private IndexWriter w;

	private static final String FIELD1 = "field1";

	/**
	 * Creates a RamDirectory that can be used to search.
	 * <P>
	 * <ul>
	 * <li>1, field1=aap-noot</li>
	 * </ul>
	 * <p>
	 * Also opens a searcher
	 */
	@Override
	public void setUp() throws Exception
	{
		StandardAnalyzer analyzer = new StandardAnalyzer( Version.LUCENE_40 );
		this.index = new RAMDirectory();

		IndexWriterConfig config = new IndexWriterConfig( Version.LUCENE_40, analyzer );

		this.w = new IndexWriter( this.index, config );
		addDoc( this.w, new String[] { "id", "1" }, new String[] { FIELD1, "aap-noot" } );
		this.w.commit();
		this.sm = new SearcherManager( this.w, true, new SearcherFactory() );
		this.sm.maybeRefreshBlocking();
	}

	@Override
	protected void tearDown() throws Exception
	{
		this.index.close();
	}

	public void testFieldsSimple() throws IOException
	{
		Term t = new Term( FIELD1, "aap-noot" );
		IndexSearcher s = this.sm.acquire();
		Query q = new TermQuery( t );

		TopScoreDocCollector collector = TopScoreDocCollector.create( 10, true );
		s.search( q, collector );
		TestCase.assertEquals( "#Matches correct?", 1, collector.getTotalHits() );

		Document d = s.doc( collector.topDocs().scoreDocs[0].doc );

		TestCase.assertEquals( "Field set to tokenized?", false, d.getField( FIELD1 ).fieldType().tokenized() );

		this.sm.release( s );
	}

	/**
	 * Helper that adds documents
	 * 
	 * @param w
	 * @param fields
	 * @throws IOException
	 */
	private static void addDoc( IndexWriter w, String[]... fields ) throws IOException
	{
		Document doc = new Document();
		for ( String[] field : fields )
		{
			doc.add( new StringField( field[0], field[1], Field.Store.YES ) );
		}
		w.addDocument( doc );
	}
}
