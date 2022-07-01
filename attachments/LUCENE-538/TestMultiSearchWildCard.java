import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.RAMDirectory;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * <p>
 * Set of Unit tests to illustrate problem with adding a WildcardQuery with a 
 * Boolean MUST_NOT clause to an overall BooleanQuery and searching across multiple lucene indices
 * with the Searcher object created from the MultiSearcher constructor.
 * </p>
 * <p>
 * In this example we have two indices. Each index contains a set of documents with fields for 'title', 
 * 'section' and 'index'. The final aim is to do a keyword search, across both indices,
 *  on the title field and be able to exclude documents from certain sections (and their subsections) using a
 *  WildcardQuery on the section field.
 *  
 *  e.g. return documents from both indices which have the string 'xyzpqr' in their title but which do not lie
 *  in the news section or its subsections (section = /news/*).
 *  </p>
 *  <p>
 *  The first unit test (testExcludeSectionsWildCard) fails trying to do this.
 *  If we relax any of the constraints made above, tests pass:
 *  <ol>
 *  <li>Don't use WildcardQuery, but pass in the news section and it's child section to exclude explicitly (testExcludeSectionsExplicit)</li>
 *  <li> Exclude results from just one section, not it's children too i.e. don't use WildcardQuery(testExcludeSingleSection)</li>
 *  <li> Do use WildcardQuery, and exclude a section and its children, but just use one index thereby using the simple
 *  	 IndexReader and IndexSearcher objects (testExcludeSectionsOneIndex).</li>
 * <li> Try the boolean MUST clause rather than MUST_NOT using the WildcardQuery i.e. only include results from the /news/ section
 *  	and its children.</li>
 * 
 * @author Helen Warren
 * 
 */

public class TestMultiSearchWildCard extends TestCase {
	private RAMDirectory testDirectoryOne = new RAMDirectory();
	private RAMDirectory testDirectoryTwo = new RAMDirectory();
	
	private IndexReader reader;
	private Searcher searcher;
		
	BooleanQuery overallQuery;
	
	/**
	 * Try to search across two indices, excluding sections based on a wildcarded string.
	 * Test is not going to work as explained in documentation for class.
	 * @throws Exception
	 */
	
	public void testExcludeSectionsWildCard() throws Exception {
		System.err.println("");
		System.err.println("Test: ExcludeSectionsWildCard..............");

		setUp(true,true);
		
		Hits results = doSearch("xyzpqr","/news/*",false);
		System.err.println("Documents in result set: ");
		for (int i = 0; i < results.length(); i++) {
			Document doc = results.doc(i);
			System.err.println("Title: "+doc.get("title")+". Section: "+doc.get("section"));
		}
		
		//should return 4 results - 2 from index one and 2 from index two. 
		//2 results from index one are excluded - the first one is in the /news/ section
		//and the second is in the /news/research section.
		
		assertEquals(4,results.length());
	}
	
	/**
	 * Relax constraint of using wildcard and pass list of sections to exclude explicity.
	 * Still search across both indices.
	 * @throws Exception
	 */
	public void testExcludeSectionsExplicit() throws Exception {
		System.err.println("");
		System.err.println("Test: ExcludeSectionsExplicit..............");

		setUp(true, true);
		Hits results = doSearch("xyzpqr", "/news/;/news/research/",false);
		
		System.err.println("Documents in result set: ");
		for (int i = 0; i < results.length(); i++) {
			Document doc = results.doc(i);
			System.err.println("Title: "+doc.get("title")+". Section: "+doc.get("section"));
		}
		
		//Because we are not building a WildcardQuery in buildExcludeSections, this should now work.
		//and we should get 4 results (as explained in testExcludeSections).
		assertEquals(4,results.length());
	}
	
	
	/**
	 * Exclude a single section, i.e. again relax wildcard constraint.
	 * Still search across two indices.
	 */
	public void testExcludeSingleSection() throws Exception {
		System.err.println("");
		System.err.println("Test: ExcludeSingleSection..............");

		setUp(true,true);
		
		Hits results = doSearch("xyzpqr","/news/",false);
		System.err.println("Documents in result set: ");
		for (int i = 0; i < results.length(); i++) {
			Document doc = results.doc(i);
			System.err.println("Title: "+doc.get("title")+". Section: "+doc.get("section"));
		}
		
		//should return 5 results - 3 from index one and 2 from index two. 
		//1 result from index one is excluded because it is in the news section.
		
		assertEquals(5,results.length());
	}
	
	/**
	 * Test the wildcard query added as a MUST_NOT, but this time
	 * use a single index. i.e. using simple IndexSearcher object.
	 * @throws Exception
	 */
	public void testExcludeSectionsOneIndex() throws Exception {
		System.err.println("");
		System.err.println("Test: ExcludeSectionsOneIndex..............");

		setUp(true,false);
		Hits results = doSearch("xyzpqr", "/news/*",false);
		
		System.err.println("Documents in result set: ");
		for (int i = 0; i < results.length(); i++) {
			Document doc = results.doc(i);
			System.err.println("Title: "+doc.get("title")+". Section: "+doc.get("section"));
		}
		
		//We've excluded the second index, so will be using IndexReader and IndexSearcher rather than 
		//creating the searcher from the MultiSearcher constructor.
		
		//this should now return 2 results.
		assertEquals(2,results.length());
		
	}
	
	/**
	 * Like testExcludeSectionsWildCard but using MUST boolean clause to add the WildcardQuery in
	 * @throws Exception
	 */
	
	public void testIncludeSectionsWildCard() throws Exception {
		System.err.println("");
		System.err.println("Test: IncludeSectionsWildCard..............");
		setUp(true,true);
		
		Hits results = doSearch("xyzpqr","/news/*",true);
		
		System.err.println("Documents in result set: ");
		for (int i = 0; i < results.length(); i++) {
			Document doc = results.doc(i);
			System.err.println("Title: "+doc.get("title")+". Section: "+doc.get("section"));
		}

		//should return 2 results - both from index one.
		assertEquals(2,results.length());
	}

	/**
	 * Generate the test lucene indices
	 * Choose whether to use one or both indexes by passing in true or false for
	 * one and two
	 * @param one Should we prepare and open index one?
	 * @param two Should we prepare and open index two?
	 */
	protected void setUp(boolean one, boolean two) throws Exception {
		super.setUp();
		
		overallQuery = new BooleanQuery();
		
		if (one) {
			setUpOne();
		}
		if (two) {
			setUpTwo();
		}
		
		
		//Set things up for the reader, which we'll be using
		if (one && two) {
 		
			IndexReader[] readers = new IndexReader[2];
			readers[0] = IndexReader.open(testDirectoryOne);
			readers[1] = IndexReader.open(testDirectoryTwo);
			
			IndexSearcher[] searchers = new IndexSearcher[2];
			
			searchers[0] = new IndexSearcher(readers[0]);
			searchers[1] = new IndexSearcher(readers[1]);
	
			// Create the multi versions
			reader = new MultiReader(readers);
			searcher = new MultiSearcher(searchers);
		}
		else if (one) {
			reader = IndexReader.open(testDirectoryOne);
			searcher = new IndexSearcher(reader);
			
		}
		else if (two) {
			reader = IndexReader.open(testDirectoryTwo);
			searcher = new IndexSearcher(reader);
		}

	}
	
	/**
	 * Create the lucene index number one in RAM and add some documents to it
	 * @throws IOException
	 */
	private void setUpOne() throws IOException {
		
		// Create our test writer for index one
		IndexWriter writerOne = new IndexWriter(testDirectoryOne, new StandardAnalyzer(), true);
		
		// Add our test documents to the index
		
		writerOne.addDocument(buildDoc("Copyright xyzpqr","/news/","one"));
		writerOne.addDocument(buildDoc("Citizen's Advice Bureau xyzpqr","/news/research/","one"));
		writerOne.addDocument(buildDoc("All about avocados xyzpqr","/home/fruit/","one"));
		writerOne.addDocument(buildDoc("Ten easy steps to thesis writing xyzpqr","/research/articles/","one"));

		writerOne.close();
		
	}
	
	/**
	 * Create the lucene index number two in RAM and add some documents to it
	 * @throws IOException
	 */
	private void setUpTwo() throws IOException {
		//Create our test writer for index two
		IndexWriter writerTwo = new IndexWriter(testDirectoryTwo, new StandardAnalyzer(), true);
		
		//Add our test docs to the  index
		writerTwo.addDocument(buildDoc("Key issues: Business and economics xyzpqr","/projects/business/economics/","two"));
		writerTwo.addDocument(buildDoc("Making hot cross buns xyzpqr", "/projects/cooking/buns/","two"));		
		
		writerTwo.close();
	}

	/**
	 * Add a document to the lucene index. Index one and index two have the same structure.
	 * @param title
	 * @param section
	 * @param index
	 * @return
	 */
	private Document buildDoc(String title, String section, String index) {
		Document doc = new Document();

		if (title != null) {
			doc.add(new Field("title",title,Field.Store.YES,Field.Index.TOKENIZED,Field.TermVector.YES));
		}
		if(section != null) {
			doc.add(new Field("section",section,Field.Store.YES,Field.Index.UN_TOKENIZED,Field.TermVector.YES));
		}
		if (index != null){
			doc.add(new Field("index", index, Field.Store.YES,Field.Index.UN_TOKENIZED));
		}

		return doc;
	}
	
	/**
	 * Exclude specific  sections.
	 * @param sections: semi-colon list of sections to exclude. Can be wildcarded
	 * @param one: whether index one is being searched
	 * @param two: whether index two is being searched 
	 */
	private void buildSectionsQuery(String sections, boolean include) {	
		StringTokenizer ste = new StringTokenizer(sections,";");
		
		while (ste.hasMoreTokens()) {
			Term excludeSectionT = null;
			org.apache.lucene.search.Query excludeSectionQ = null;
		
			String excludeSection = ste.nextToken();
			excludeSectionT = new Term("section",excludeSection);
			
			if ((excludeSection.indexOf("*") == -1) && (excludeSection.indexOf("?") == -1)) {
				excludeSectionQ = new TermQuery(excludeSectionT);
			}
			else {
				excludeSectionQ = new WildcardQuery(excludeSectionT);	
			}

			
			if (include) {
				overallQuery.add(excludeSectionQ, BooleanClause.Occur.MUST);
			}
			else { 
				overallQuery.add(excludeSectionQ, BooleanClause.Occur.MUST_NOT);
			}
		}
	}
	
	private void buildKeyWordQuery(String queryString) throws ParseException {

		Query titleQ = null;

		// Make an appropriate analyzer
		
		Analyzer analyzer = new WhitespaceAnalyzer();
		QueryParser qp = new QueryParser("title",analyzer);
		titleQ = qp.parse(queryString);

		overallQuery.add(titleQ,BooleanClause.Occur.MUST);
	}
	
	private Hits doSearch(String queryString, String sections, boolean include) throws ParseException, IOException {
		if (queryString != null && queryString.length() > 0) {
			buildKeyWordQuery(queryString);
		}
		if (sections != null && sections.length() > 0) {
			buildSectionsQuery(sections, include);
		}
		
		if ((queryString == null || queryString.length() == 0) && (sections == null || sections.length() == 0)) {
			throw new IOException("No search criteria given");
		}
		
		
		System.err.println("Executing query: "+overallQuery.rewrite(reader));
		Hits results = searcher.search(overallQuery);
		
		return results;
	}
	
	public static void main(String[] args) {
		TestRunner.run(suite());
		
	}
	
    public static Test suite() {

        TestSuite suite = new TestSuite();
	
        suite.addTestSuite(TestMultiSearchWildCard.class);

        return suite;
    }

}
