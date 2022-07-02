package org.apache.lucene.index;
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

import junit.framework.TestCase;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.RAMDirectory;

public class TestSynonymIndexReader extends TestCase
{

	private RAMDirectory rd = null;
	private IndexReader synReader;
	private IndexSearcher synSearcher;
	private Analyzer analyzer = new SimpleAnalyzer();

	private String testDocContents[] = { 
			"deborah harry", 
			"debbie harry",
			"When harry met debs" };

	
	public void testIndexReaderApproachToSynonyms() throws Exception
	{
		QueryParser qp = new QueryParser("contents", analyzer);

		// basic term test
		Query q = qp.parse("deborah");
		int matches = synSearcher.search(q).length();
		assertEquals("Should find both all Deborahs ", 3, matches);

		// positional test
		q = qp.parse("\"deborah harry\""); // create phrase query
		matches = synSearcher.search(q).length();
		assertEquals("Should find both Deb Harrys ", 2, matches);

		// index stats test
		assertEquals("Deborah's doc frequency should be 3", 3, synReader
				.docFreq(new Term("contents", "deborah")));
	}
	
	

	protected void setUp() throws Exception
	{
		super.setUp();
		if (rd == null)
		{
			//Create the index used in the tests
			rd = new RAMDirectory();
			IndexWriter w = new IndexWriter(rd, analyzer, true);
			for (int i = 0; i < testDocContents.length; i++)
			{
				Document doc = new Document();
				doc.add(new Field("contents", testDocContents[i], Field.Store.YES,
						Field.Index.TOKENIZED, Field.TermVector.YES));
				w.addDocument(doc);
			}
			w.optimize();
			w.close();
			IndexReader reader=IndexReader.open(rd);
			
			
			
			//Now define the synonyms to be used in the test
			SynonymSet ss = new SynonymSet();
			Term rootTerm=new Term("contents", "deborah");
			Term[] variants = { 
					rootTerm.createTerm("debbie"),
					rootTerm.createTerm("debs"), 
					rootTerm.createTerm("deb") 
				};
			ss.addSynonym(new Synonym(rootTerm, variants,
					reader));
			//.. add any other terms + variations to the synonyms set here
			
			
			//Wrap the real indexReader with a synonym-based one to provide a new perspective
			synReader= new SynonymIndexReader(reader, ss);			
			synSearcher = new IndexSearcher(synReader);
		}

	}
}
