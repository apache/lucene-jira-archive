package org.apache.lucene.search;

/**
 * Copyright 2004 The Apache Software Foundation
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

import junit.framework.TestCase;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.RAMDirectory;

/**
 * Tests {@link FuzzyQuery}.
 *
 * @author Joerg Henss
 */
public class TestFuzzyQueryError extends TestCase {
	
	int oldMaxClauseCount=0;
	
	public void setUp()
	{
		oldMaxClauseCount=BooleanQuery.getMaxClauseCount();
	 
	}
	
	public void tearDown()
	{
		BooleanQuery.setMaxClauseCount(oldMaxClauseCount);
	}
	
 
  public void testPriorityQueueError() throws Exception {

	  RAMDirectory directory = new RAMDirectory();
	    IndexWriter writer = new IndexWriter(directory, new WhitespaceAnalyzer(), true);
	    addDoc("aaaaaaa", writer);
	    writer.optimize();
	    writer.close();
	    IndexSearcher searcher = new IndexSearcher(directory);

	    FuzzyQuery query;
	    
	    BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
	    // they should be similar
	    query = new FuzzyQuery(new Term("field", "aaaaaaa"), FuzzyQuery.defaultMinSimilarity, 0);   
	    Hits hits = searcher.search(query);
	    assertEquals(1, hits.length());
	    BooleanQuery.setMaxClauseCount(oldMaxClauseCount);
	    

	    searcher.close();
	    directory.close();
	  }
  
  
  
  
  private void addDoc(String text, IndexWriter writer) throws IOException {
    Document doc = new Document();
    doc.add(new Field("field", text, Field.Store.YES, Field.Index.TOKENIZED));
    writer.addDocument(doc);
  }

}
