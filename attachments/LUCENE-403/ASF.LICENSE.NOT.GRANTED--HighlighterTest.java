package com.thomsonmedia.lucene;                                                                                                                                                       
                                                                                                                                                                                       
/**                                                                                                                                                                                    
 * Copyright 2002-2004 The Apache Software Foundation                                                                                                                                  
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
                                                                                                                                                                                       
import java.io.IOException;                                                                                                                                                            
import java.io.Reader;                                                                                                                                                                 
import java.util.HashMap;                                                                                                                                                              
import java.util.Map;                                                                                                                                                                  
import java.util.StringTokenizer;                                                                                                                                                      
                                                                                                                                                                                       
import junit.framework.TestCase;                                                                                                                                                       
                                                                                                                                                                                       
import org.apache.lucene.analysis.Analyzer;                                                                                                                                            
import org.apache.lucene.analysis.LowerCaseTokenizer;                                                                                                                                  
import org.apache.lucene.analysis.Token;                                                                                                                                               
import org.apache.lucene.analysis.TokenStream;                                                                                                                                         
import org.apache.lucene.analysis.standard.StandardAnalyzer;                                                                                                                           
import org.apache.lucene.document.Document;                                                                                                                                            
import org.apache.lucene.document.Field;                                                                                                                                               
import org.apache.lucene.index.IndexReader;                                                                                                                                            
import org.apache.lucene.index.IndexWriter;                                                                                                                                            
import org.apache.lucene.queryParser.QueryParser;                                                                                                                                      
import org.apache.lucene.search.Hits;                                                                                                                                                  
import org.apache.lucene.search.IndexSearcher;                                                                                                                                         
import org.apache.lucene.search.Query;                                                                                                                                                 
import org.apache.lucene.search.Searcher;                                                                                                                                              
import org.apache.lucene.store.RAMDirectory;                                                                                                                                           
                                                                                                                                                                                       
/**                                                                                                                                                                                    
 * JUnit Test for Highlighter class.                                                                                                                                                   
 * @author mark@searcharea.co.uk                                                                                                                                                       
 */                                                                                                                                                                                    
public class HighlighterTest extends TestCase                                                                                                                                          
{                                                                                                                                                                                      
	private IndexReader reader;                                                                                                                                                        
	private static final String FIELD_NAME = "contents";                                                                                                                               
	private Query query;                                                                                                                                                               
	RAMDirectory ramDir;                                                                                                                                                               
	public Searcher searcher = null;                                                                                                                                                   
	public Hits hits = null;                                                                                                                                                           
	int numHighlights = 0;                                                                                                                                                             
	Analyzer analyzer=new StandardAnalyzer();                                                                                                                                          
	int numCallsToGetTokenStream=0;                                                                                                                                                    
	                                                                                                                                                                                   
                                                                                                                                                                                       
	String texts[] =                                                                                                                                                                   
		{                                                                                                                                                                              
			"Hello this is a piece of text that is very long and contains too much preamble and the meat is really here which says kennedy has been shot",                             
			"This piece of text refers to Kennedy at the beginning then has a longer piece of text that is very long in the middle and finally ends with another reference to Kennedy",
			"JFK has been shot",                                                                                                                                                       
			"John Kennedy has been shot",                                                                                                                                              
			"This text has a typo in referring to Keneddy" };                                                                                                                          
                                                                                                                                                                                       
	/**                                                                                                                                                                                
	 * Constructor for HighlightExtractorTest.                                                                                                                                         
	 * @param arg0                                                                                                                                                                     
	 */                                                                                                                                                                                
	public HighlighterTest(String arg0)                                                                                                                                                
	{                                                                                                                                                                                  
		super(arg0);                                                                                                                                                                   
	}                                                                                                                                                                                  
                                                                                                                                                                                       
                                                                                                                                                                                       
	public void testSlopFactor() throws Exception                                                                                                                                      
	{                                                                                                                                                                                  
		doSearching("\"shot jfk\"~5");                                                                                                                                                 
		for (int i = 0; i < hits.length(); i++)                                                                                                                                        
		{                                                                                                                                                                              
			String text = hits.doc(i).get(FIELD_NAME);                                                                                                                                 
			    String result = QueryHighlighter.highlight("", query,reader,analyzer,text,"<b>","</b>");                                                                                   
			    System.out.println("slop result="+result);                                                                                                                             
			    String expectedResult="<b>JFK has been shot</b>";                                                                                                                      
			    assertEquals("Slop factor should be tested",result,expectedResult);                                                                                                    
		}		                                                                                                                                                                       
	}                                                                                                                                                                                  
	                                                                                                                                                                                   
	                                                                                                                                                                                   
	public void testAnalyzerUsage() throws Exception                                                                                                                                   
	{                                                                                                                                                                                  
	    //makes too many calls to analyzer (15 where only 5 required)                                                                                                                  
	    //Analyzers can be slllloooooww. Caching token streams would help here.                                                                                                        
	    //- also does not allow choice of field                                                                                                                                        
	    // to be passed to analyzer.                                                                                                                                                   
		doSearching("keneddy~ \"has been shot\"");                                                                                                                                     
		numCallsToGetTokenStream=0;                                                                                                                                                    
		Analyzer a=new Analyzer(){                                                                                                                                                     
		    public TokenStream tokenStream(String f, Reader r)                                                                                                                         
		    {                                                                                                                                                                          
		        numCallsToGetTokenStream++;                                                                                                                                            
		        return analyzer.tokenStream(f,r);                                                                                                                                      
		    }                                                                                                                                                                          
		    };                                                                                                                                                                         
		for (int i = 0; i < hits.length(); i++)                                                                                                                                        
		{                                                                                                                                                                              
			String text = hits.doc(i).get(FIELD_NAME);                                                                                                                                 
			String result = QueryHighlighter.highlight("", query,reader,a,text,"<b>","</b>");                                                                                              
		}                                                                                                                                                                              
		assertEquals("Test num calls to analyzer.tokenStream not excessive",hits.length(),numCallsToGetTokenStream);                                                                   
	}                                                                                                                                                                                  
	                                                                                                                                                                                   
	public void testGetBestFrag() throws Exception                                                                                                                                     
	{                                                                                                                                                                                  
	    //The sandbox highlighter selects the highest scoring sections of a doc                                                                                                        
	                                                                                                                                                                                   
//		doSearching("+Keneddy text"); //this selects best frag (query=rare word, common word)                                                                                          
		doSearching("text +Keneddy"); //this doesnt (query=common word, rare word)                                                                                                     
		//Only one doc has the mis-spelt Keneddy                                                                                                                                       
		for (int i = 0; i < hits.length(); i++)                                                                                                                                        
		{                                                                                                                                                                              
			String text = hits.doc(i).get(FIELD_NAME);                                                                                                                                 
			String bestFrag=QueryHighlighter.highlightFragments("", query,reader,analyzer,text,"<b>","</b>",1,3,"...");                                                                    
			//did we select the best bit of this doc ie the term with highest IDF?                                                                                                     
			System.out.println("bestFrag=\t" + bestFrag);                                                                                                                              
			assertTrue("Must select highest scoring fragment (the one with rarest, highest ranking terms",                                                                             
			        bestFrag.indexOf("Keneddy")>=0                                                                                                                                     
			        );                                                                                                                                                                 
		}                                                                                                                                                                              
	}                                                                                                                                                                                  
	public void testFragmentSummaries() throws Exception                                                                                                                               
	{                                                                                                                                                                                  
		doSearching("kennedy");                                                                                                                                                        
		int maxFragSize=3;                                                                                                                                                             
		int maxOverrun=20;                                                                                                                                                             
		for (int i = 0; i < hits.length(); i++)                                                                                                                                        
		{                                                                                                                                                                              
			String text = hits.doc(i).get(FIELD_NAME);                                                                                                                                 
			if(text.length()>maxFragSize)                                                                                                                                              
			{                                                                                                                                                                          
			    String rs=QueryHighlighter.highlightFragments("", query,reader,analyzer,text,"<b>","</b>",1,maxFragSize,"...");                                                            
			    assertTrue("highlightFragments should only have returned 1 fragment ~ 20 chars in length",                                                                             
			            rs.length()<maxFragSize+maxOverrun);                                                                                                                           
			}                                                                                                                                                                          
		}                                                                                                                                                                              
		//Looks like fragmentsize passed is measured in number of (non stop word) tokens -  not                                                                                        
		// String.length so could produce longer than expected text fragments if text                                                                                                  
		// contains many stop words or "noise" characters                                                                                                                              
		//eg --------------------------                                                                                                                                                
		                                                                                                                                                                               
                                                                                                                                                                                       
	}                                                                                                                                                                                  
                                                                                                                                                                                       
	public void testGetWildCardFragments() throws Exception                                                                                                                            
	{                                                                                                                                                                                  
	    //Works OK                                                                                                                                                                     
		doSearching("K?nnedy");                                                                                                                                                        
		for (int i = 0; i < hits.length(); i++)                                                                                                                                        
		{                                                                                                                                                                              
			String text = hits.doc(i).get(FIELD_NAME);                                                                                                                                 
			String result=QueryHighlighter.highlight("", query,reader,analyzer,text,"<B>","</B>");						                                                                   
			System.out.println("K?nnedyFrag=\t" + result);                                                                                                                             
		}                                                                                                                                                                              
	}                                                                                                                                                                                  
	                                                                                                                                                                                   
	// tests a "complex" analyzer that produces multiple                                                                                                                               
	// overlapping tokens                                                                                                                                                              
	public void testOverlapAnalyzer() throws Exception                                                                                                                                 
	{                                                                                                                                                                                  
	    //works OK                                                                                                                                                                     
		HashMap synonyms = new HashMap();                                                                                                                                              
		synonyms.put("football", "soccer,footie");                                                                                                                                     
		Analyzer analyzer = new SynonymAnalyzer(synonyms);                                                                                                                             
		String srchkey = "football";                                                                                                                                                   
                                                                                                                                                                                       
		String s = "football-soccer in the euro 2004 footie competition";                                                                                                              
		Query query = QueryParser.parse(srchkey, "text", analyzer);                                                                                                                    
                                                                                                                                                                                       
                                                                                                                                                                                       
		String result=QueryHighlighter.highlight("", query,reader,analyzer,s,"<B>","</B>");                                                                                                
		System.out.println("hiOverlap=\t" + result);                                                                                                                                   
		                                                                                                                                                                               
		String expectedResult="<B>football</B>-<B>soccer</B> in the euro 2004 <B>footie</B> competition";                                                                              
		assertEquals("overlapping analyzer should handle highlights OK",result,expectedResult);                                                                                        
	}                                                                                                                                                                                  
	                                                                                                                                                                                   
	public void testNoFragments() throws Exception                                                                                                                                     
	{                                                                                                                                                                                  
	    //works OK                                                                                                                                                                     
		doSearching("AnInvalidQueryWhichShouldYieldNoResults");                                                                                                                        
		for (int i = 0; i < texts.length; i++)                                                                                                                                         
		{                                                                                                                                                                              
			String text = texts[i];                                                                                                                                                    
			String result = QueryHighlighter.highlightFragments("", query,reader,analyzer,text,"<B>","</B>",1,20,"...");                                                                   
			assertEquals("The best highlight frag should be ??? for text with no query terms", "",result);                                                                             
		}                                                                                                                                                                              
	}	                                                                                                                                                                               
                                                                                                                                                                                       
	public void doSearching(String queryString) throws Exception                                                                                                                       
	{                                                                                                                                                                                  
		searcher = new IndexSearcher(ramDir);                                                                                                                                          
		query = QueryParser.parse(queryString, FIELD_NAME, new StandardAnalyzer());                                                                                                    
		//for any multi-term queries to work (prefix, wildcard, range,fuzzy etc) you must use a rewritten query!                                                                       
		query=query.rewrite(reader);                                                                                                                                                   
		System.out.println("Searching for: " + query.toString(FIELD_NAME));                                                                                                            
		hits = searcher.search(query);                                                                                                                                                 
	}                                                                                                                                                                                  
                                                                                                                                                                                       
                                                                                                                                                                                       
	protected void setUp() throws Exception                                                                                                                                            
	{                                                                                                                                                                                  
		ramDir = new RAMDirectory();                                                                                                                                                   
		IndexWriter writer = new IndexWriter(ramDir, new StandardAnalyzer(), true);                                                                                                    
		for (int i = 0; i < texts.length; i++)                                                                                                                                         
		{                                                                                                                                                                              
			addDoc(writer, texts[i]);                                                                                                                                                  
		}                                                                                                                                                                              
		writer.optimize();                                                                                                                                                             
		writer.close();                                                                                                                                                                
		reader = IndexReader.open(ramDir);                                                                                                                                             
		numHighlights = 0;                                                                                                                                                             
	}                                                                                                                                                                                  
                                                                                                                                                                                       
	private void addDoc(IndexWriter writer, String text) throws IOException                                                                                                            
	{                                                                                                                                                                                  
		Document d = new Document();                                                                                                                                                   
		Field f = new Field(FIELD_NAME, text, true, true, true);                                                                                                                       
		d.add(f);                                                                                                                                                                      
		writer.addDocument(d);                                                                                                                                                         
                                                                                                                                                                                       
	}                                                                                                                                                                                  
                                                                                                                                                                                       
	protected void tearDown() throws Exception                                                                                                                                         
	{                                                                                                                                                                                  
		super.tearDown();                                                                                                                                                              
	}                                                                                                                                                                                  
                                                                                                                                                                                       
}                                                                                                                                                                                      
                                                                                                                                                                                       
                                                                                                                                                                                       
//===================================================================                                                                                                                  
//========== BEGIN TEST SUPPORTING CLASSES                                                                                                                                             
//========== THESE LOOK LIKE, WITH SOME MORE EFFORT THESE COULD BE                                                                                                                     
//========== MADE MORE GENERALLY USEFUL.                                                                                                                                               
// TODO - make synonyms all interchangeable with each other and produce                                                                                                                
// a version that does hyponyms - the "is a specialised type of ...."                                                                                                                  
// so that car = audi, bmw and volkswagen but bmw != audi so different                                                                                                                 
// behaviour to synonyms                                                                                                                                                               
//===================================================================                                                                                                                  
                                                                                                                                                                                       
class SynonymAnalyzer extends Analyzer                                                                                                                                                 
{                                                                                                                                                                                      
	private Map synonyms;                                                                                                                                                              
                                                                                                                                                                                       
	public SynonymAnalyzer(Map synonyms)                                                                                                                                               
	{                                                                                                                                                                                  
		this.synonyms = synonyms;                                                                                                                                                      
	}                                                                                                                                                                                  
                                                                                                                                                                                       
	/* (non-Javadoc)                                                                                                                                                                   
	 * @see org.apache.lucene.analysis.Analyzer#tokenStream(java.lang.String, java.io.Reader)                                                                                          
	 */                                                                                                                                                                                
	public TokenStream tokenStream(String arg0, Reader arg1)                                                                                                                           
	{                                                                                                                                                                                  
		return new SynonymTokenizer(new LowerCaseTokenizer(arg1), synonyms);                                                                                                           
	}                                                                                                                                                                                  
}                                                                                                                                                                                      
                                                                                                                                                                                       
/**                                                                                                                                                                                    
 * Expands a token stream with synonyms (TODO - make the synonyms analyzed by choice of analyzer)                                                                                      
 * @author MAHarwood                                                                                                                                                                   
 */                                                                                                                                                                                    
class SynonymTokenizer extends TokenStream                                                                                                                                             
{                                                                                                                                                                                      
	private TokenStream realStream;                                                                                                                                                    
	private Token currentRealToken = null;                                                                                                                                             
	private Map synonyms;                                                                                                                                                              
	StringTokenizer st = null;                                                                                                                                                         
	public SynonymTokenizer(TokenStream realStream, Map synonyms)                                                                                                                      
	{                                                                                                                                                                                  
		this.realStream = realStream;                                                                                                                                                  
		this.synonyms = synonyms;                                                                                                                                                      
	}                                                                                                                                                                                  
	public Token next() throws IOException                                                                                                                                             
	{                                                                                                                                                                                  
		if (currentRealToken == null)                                                                                                                                                  
		{                                                                                                                                                                              
			Token nextRealToken = realStream.next();                                                                                                                                   
			if (nextRealToken == null)                                                                                                                                                 
			{                                                                                                                                                                          
				return null;                                                                                                                                                           
			}                                                                                                                                                                          
			String expansions = (String) synonyms.get(nextRealToken.termText());                                                                                                       
			if (expansions == null)                                                                                                                                                    
			{                                                                                                                                                                          
				return nextRealToken;                                                                                                                                                  
			}                                                                                                                                                                          
			st = new StringTokenizer(expansions, ",");                                                                                                                                 
			if (st.hasMoreTokens())                                                                                                                                                    
			{                                                                                                                                                                          
				currentRealToken = nextRealToken;                                                                                                                                      
			}                                                                                                                                                                          
			return currentRealToken;                                                                                                                                                   
		}                                                                                                                                                                              
		else                                                                                                                                                                           
		{                                                                                                                                                                              
			String nextExpandedValue = st.nextToken();                                                                                                                                 
			Token expandedToken =                                                                                                                                                      
				new Token(                                                                                                                                                             
					nextExpandedValue,                                                                                                                                                 
					currentRealToken.startOffset(),                                                                                                                                    
					currentRealToken.endOffset());                                                                                                                                     
			expandedToken.setPositionIncrement(0);                                                                                                                                     
			if (!st.hasMoreTokens())                                                                                                                                                   
			{                                                                                                                                                                          
				currentRealToken = null;                                                                                                                                               
				st = null;                                                                                                                                                             
			}                                                                                                                                                                          
			return expandedToken;                                                                                                                                                      
		}                                                                                                                                                                              
	}                                                                                                                                                                                  
                                                                                                                                                                                       
}                                                                                                                                                                                      