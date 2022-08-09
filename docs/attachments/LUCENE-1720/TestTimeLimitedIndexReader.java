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
import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TimeLimitingCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class TestTimeLimitedIndexReader
{

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		
		IndexReader r=IndexReader.open(FSDirectory.open(new File("D:/indexes/wikipedia")),true);
//		IndexReader r=IndexReader.open("/Users/Mark/Work/indexes/wikipediaNov2008");
		TimeLimitedIndexReader tlir=new TimeLimitedIndexReader(r);

		//Warmup
		runHeavyTermEnumTermDocsAccess(r,"no timeout limit (warm up)");
		runHeavyTermEnumTermDocsAccess(r,"no timeout limit (warm up)");


		
		runHeavyTermEnumTermDocsAccess(r,"no timeout limit");
		ActivityTimeMonitor.start(30000);
		try
		{
			runHeavyTermEnumTermDocsAccess(tlir, " reader-limited access");
		}
		finally
		{
			ActivityTimeMonitor.stop();
		}
		
		//======================
		int maxTimeForFuzzyQueryFromHell=4000;
		QueryParser qp=new QueryParser(Version.LUCENE_CURRENT,"text", new WhitespaceAnalyzer());
		Query q = qp.parse("f* AND a* AND b*");

		runQueryNoTimeoutProtection(q,r);
		runQueryTimeLimitedCollector(q,r,maxTimeForFuzzyQueryFromHell);
		ActivityTimeMonitor.start(maxTimeForFuzzyQueryFromHell);
		try
		{
			runQueryTimeLimitedReader(q,tlir);
		}
		finally
		{
			ActivityTimeMonitor.stop();
		}
		System.out.println();

		
		q = qp.parse("accomodation~");
		runQueryNoTimeoutProtection(q,r);
		//can timeout significantly later than desired because cost is in fuzzy analysis, not collection
		runQueryTimeLimitedCollector(q,r,maxTimeForFuzzyQueryFromHell); 

		ActivityTimeMonitor.start(maxTimeForFuzzyQueryFromHell);
		try
		{
			//Times out much more readily
			runQueryTimeLimitedReader(q,tlir);
		}
		finally
		{
			ActivityTimeMonitor.stop();
		}
		
		
		
		
		
	}
	private static void runQueryNoTimeoutProtection(Query q, IndexReader r) throws IOException, ParseException
	{
		long start=System.currentTimeMillis();
		IndexSearcher s=new IndexSearcher(r);

		MyHitCollector hc=new MyHitCollector();
		s.search(q, hc);
		long diff=System.currentTimeMillis()-start;
		System.out.println(q+" no time limit matched "+hc.numMatches+" docs in \t"+diff+" ms");
	}

	private static void runQueryTimeLimitedCollector(Query q,IndexReader r, int maxWait) throws IOException, ParseException
	{
		long start=System.currentTimeMillis();
		IndexSearcher s=new IndexSearcher(r);
		MyHitCollector hc=new MyHitCollector();
		s.search(q, new TimeLimitingCollector(hc,maxWait));
		long diff=System.currentTimeMillis()-start;
		System.out.println(q+" time limited collector matched "+hc.numMatches+" docs in \t"+diff+" ms");
	}
	
	private static void runQueryTimeLimitedReader(Query q,IndexReader r) throws IOException, ParseException
	{
		long start=System.currentTimeMillis();
		IndexSearcher s=new IndexSearcher(r);
		MyHitCollector hc=new MyHitCollector();
		s.search(q, hc);
		long diff=System.currentTimeMillis()-start;
		System.out.println(q+" time limited reader matched "+hc.numMatches+" docs in \t"+diff+" ms");
	}	
	
	static class MyHitCollector extends Collector
	{
		int numMatches=0;

		@Override
		public boolean acceptsDocsOutOfOrder()
		{
			return false;
		}

		@Override
		public void collect(int doc) throws IOException
		{
			numMatches++;	
		}

		@Override
		public void setNextReader(IndexReader reader, int docBase)
				throws IOException
		{
		}

		@Override
		public void setScorer(Scorer scorer) throws IOException
		{
		}		
	}

	private static void runHeavyTermEnumTermDocsAccess(IndexReader r, String msg) throws IOException
	{
		long start=System.currentTimeMillis();
		TermEnum te = r.terms();
		int numTerms=0;
		while(te.next())
		{
			TermDocs td = r.termDocs(te.term());
			while(td.next())
			{
				
			}
			numTerms++;
			if(numTerms>200000)
			{
				break;
			}

		}
		long diff=System.currentTimeMillis()-start;
		System.out.println("Read term docs for 200000 terms  in "+diff+" ms using "+msg);
	}	

}
