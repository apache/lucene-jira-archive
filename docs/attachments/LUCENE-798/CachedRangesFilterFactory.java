package org.apache.lucene.search;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.util.PriorityQueue;


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

/**
 * A cache of ranges used to optimize the construction of large RangeFilters. It is difficult to cache
 * RangeFilters individually because it is rare that 2 queries will pick identical start and end terms
 * and therefore have exactly the required Bitset already cached.
 * This class avoids this problem by caching *sections* of ranges and constructing range filters on the fly
 * by combining cached bitsets and hitting disk where necessary to construct an appropriate filter.
 * The factory optimizes the caching by monitoring which ranges get hit most and giving those priority, 
 * swapping out less-used bitsets.
 * 
 * Initial tests (see main method) show increased performance over raw RangeFilter construction but:
 * TODO - thread safety
 * TODO - current logic assumes start/end terms are inclusive - need option for non-inclusive bounds
 * TODO - CachabilityWeighting logic may need tweaking to more quickly "forget" ranges hit hard 
 * historically but not recently required. 
 *    
 * @author Mark Harwood
 *
 */
public class CachedRangesFilterFactory
{
	CachableRange rangeSubdivisions[];
	int maxNumCachedBitsets;
	private int numTermsInField;
	
	
	/** Example demo showing how caching can be applied to sections of a range
	 */
	public static void main(String[] args) throws IOException, ParseException
	{
		IndexReader reader=IndexReader.open("/indexes/enron");
		String field="date";
//		the more ranges - the more likely a cache hit - BUT more memory required
		int numRanges=200; 		
		int megCache=10; 
		SimpleDateFormat indexDateFormat=new SimpleDateFormat("yyyyMMdd");
		CachedRangesFilterFactory cf = new CachedRangesFilterFactory(field, numRanges,reader,megCache);

		int numTests=500;

		//Generate some test queries...
		ArrayList testRanges=new ArrayList();
		for(int i=0;i<numTests;i++)
		{
			Document doc1=reader.document((int) (Math.random()*reader.maxDoc()));
			Document doc2=reader.document((int) (Math.random()*reader.maxDoc()));
			String term1=doc1.get(field);
			String term2=doc2.get(field);
			if(term1.compareTo(term2)<0)
			{
				testRanges.add(new Range(new Term(field,term1),new Term(field,term2)));
			}
			else
			{
				testRanges.add(new Range(new Term(field,term2),new Term(field,term1)));
			}
			
		}
		
		System.out.println(cf.getNumTermsInField()+" terms in "+numRanges+" ranges");
		
		
		//warm up
		for (Iterator iter = testRanges.iterator(); iter.hasNext();)
		{
			Range testRange = (Range) iter.next();
			BitSet rfb2 = new RangeFilter(field,testRange.startTerm.text(),testRange.endTerm.text(),true,true).bits(reader);			
			BitSet crb2 = cf.getFilter(testRange.startTerm,testRange.endTerm,reader).bits(reader);
			if(!rfb2.equals(crb2))
			{
				System.err.println(rfb2.cardinality()+"!="+crb2.cardinality());
			}			
		}

	
		//Time RangeFilter construction
		long rfstart=System.currentTimeMillis();
		for (Iterator iter = testRanges.iterator(); iter.hasNext();)
		{
			Range testRange = (Range) iter.next();
			BitSet rfb2 = new RangeFilter(field,testRange.startTerm.text(),testRange.endTerm.text(),true,true).bits(reader);			
		}
		long rftime=System.currentTimeMillis()-rfstart;
		System.out.println("RangeFilter took "+rftime+" ms");
		
		//Time CachedRangesFilterFactory construction
		long start=System.currentTimeMillis();
		for (Iterator iter = testRanges.iterator(); iter.hasNext();)
		{
			Range testRange = (Range) iter.next();
			BitSet crb2 = cf.getFilter(testRange.startTerm,testRange.endTerm,reader).bits(reader);
		}
		long time=System.currentTimeMillis()-start;
		System.out.println("CachedRangesFilter took "+time+" ms");				
	}
	
	
	public String getFirstTerm()
	{
		return rangeSubdivisions[0].startTerm.text();
	}
	public String getLastTerm()
	{
		return rangeSubdivisions[rangeSubdivisions.length-1].endTerm.text();
	}


	public CachedRangesFilterFactory(String field, int numRangeDivisions, IndexReader reader, int maxRamMeg) throws IOException
	{
		this.maxNumCachedBitsets=(maxRamMeg*1024*1024)/(reader.maxDoc()/8);
		
		//Identify the range blocks considered for caching e.g. Dates 20030101 to 20031231 
		// and 20040101 to 20041231. The range of terms used to define each range block are
		// picked to represent an evenly balanced collection of documents.
		Term startTerm=new Term(field,"");
		ArrayList ranges=new ArrayList();
		//TODO: assumption here for apportioning range subdivisions is that the total DocFreqs of 
		// all terms == reader.maxDoc i.e. every doc has just one date term.
		// To avoid this assumption would need to have additional pass of TermEnum to count
		// totalDf for all terms in this field.
//		int numRemainingDocs=reader.maxDoc();
		//Count how many Doc references are represented by all of the terms for the chosen field.
		// This represents the total amount of bits that could be cached and need to be evenly
		//apportioned between our selection of cachable range blocks 
		int numRemainingDocs=0;
		numTermsInField=0;
		TermEnum countDfEnum=reader.terms(startTerm);
		while(countDfEnum.next())
		{
			Term currentTerm=countDfEnum.term();
			if(currentTerm.field()!=startTerm.field())
			{
				break;
			}
			numTermsInField++;
			numRemainingDocs+=countDfEnum.docFreq();
		}
		
		int docsPerRangeDivision=numRemainingDocs/numRangeDivisions;
		TermEnum te = reader.terms(startTerm);
		CachableRange currentRange=new CachableRange();
		ranges.add(currentRange);
		Term lastTerm = null;
	
		while(te.next())
		{
			Term currentTerm=te.term();
			//check still looking at same field
			if(startTerm.field()!=currentTerm.field())
			{
				break;
			}
			if(currentRange.startTerm==null)
			{
				currentRange.startTerm=currentTerm;
			}
			int termDf=te.docFreq();
			int numLeftInThisRange=docsPerRangeDivision-currentRange.df;
			int potentialCarryOver=termDf-numLeftInThisRange;
			//Check this term doesn't best fit in the next range - 
			//trying to keep the number of docs represented in each range balanced 
			if(potentialCarryOver>numLeftInThisRange)
			{
//				refine estimate for number docsPerRangeDivision
				numRemainingDocs-=currentRange.df; 
				docsPerRangeDivision=numRemainingDocs/(numRangeDivisions-ranges.size());

				currentRange.endTerm=currentTerm;
				currentRange=new CachableRange();
				ranges.add(currentRange);
			}
			currentRange.df+=termDf;
			lastTerm=currentTerm;
		}
		if(currentRange.startTerm==null)
		{
			currentRange.startTerm=lastTerm;
		}
		currentRange.endTerm=lastTerm;

		rangeSubdivisions=(CachableRange[]) ranges.toArray(new CachableRange[ranges.size()]);
	}
	public int getNumTermsInField()
	{
		return numTermsInField;
	}
	/**
	 * (hopefully) efficient mechanism for getting large range filters 
	 * @param queryStartTerm
	 * @param queryEndTerm
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public Filter getFilter(Term queryStartTerm, Term queryEndTerm, IndexReader reader) throws IOException
	{
		Range queryRange=new Range(Range.maxTerm(queryStartTerm, rangeSubdivisions[0].startTerm),
					Range.minTerm(queryEndTerm, rangeSubdivisions[rangeSubdivisions.length-1].endTerm));
		ArrayList containedRanges=new ArrayList();
		Range startFiller=null;
		Range endFiller=null;
		for (int i = 0; i < rangeSubdivisions.length; i++)
		{
			CachableRange currentRange=rangeSubdivisions[i];
			if(!queryRange.intersects(currentRange))
			{
				currentRange.numMisses++;
				continue; //wind forward - query still not in range 
			}
			//within range
			if(queryRange.contains(currentRange))
			{
				currentRange.numFullHits++;
				containedRanges.add(currentRange);
				continue;
			}
			//intersects but does not contain create a partial start "filler" range to take us 
			// up to the first block
			if(queryRange.startsAfter(currentRange))
			{
				currentRange.numPartialHits++;
				startFiller=new Range(queryRange.startTerm,
						Range.minTerm(queryRange.endTerm,currentRange.endTerm));
			}
			//query ends before a block - create a partial end "filler" range
			if(!queryRange.endsAfter(currentRange))
			{
				//TODO could overlap with startFiller? - need "if" test around endFiller construction
				endFiller=new Range(Range.maxTerm(queryRange.startTerm,currentRange.startTerm),
						queryRange.endTerm);
				while(i < rangeSubdivisions.length)
				{
					//mark all remaining rangeDivisions as "misses"
					rangeSubdivisions[i++].numMisses++;					
				}
				break;
			}			
		}///end selection of range blocks

		
		//Identify the new top "most cachable" ranges as a result of recent hit/miss marking above.
		CachedRanges newTop=new CachedRanges(maxNumCachedBitsets);
		CachableRange minRange=null;
		for (int i = 0; i < rangeSubdivisions.length; i++)
		{
			CachableRange r=rangeSubdivisions[i];
			if(newTop.size()<maxNumCachedBitsets)
			{
				newTop.insert(r);
				minRange=(CachableRange) newTop.top();
			}
			else
			{
				if(r.getCachabilityWeighting()>minRange.getCachabilityWeighting())
				{
					if(minRange.bitset!=null)
					{
						minRange.bitset=null; 
//						System.err.println("Evicting cached date range "+minRange);
					}					
					newTop.insert(r);
					minRange=(CachableRange) newTop.top();
				}
			}
		}
		HashSet topCandidateRangesForCaching=new HashSet();
		while(newTop.size()>0)
		{
			CachableRange r=(CachableRange) newTop.pop();
			topCandidateRangesForCaching.add(r);
		}
		
								
		
		//Now populate bitset for current range query
		BitSet finalFilter=new BitSet(reader.maxDoc());
        TermDocs termDocs = reader.termDocs();
        TermEnum te=reader.terms(queryStartTerm);
		Term currentTerm=te.term();
		
		//if query start term does not align exactly with a block start term
		//Need to load a part of a range block - for which we obviously won't have a cached
		//bitset - time to hit the disk..
		if(startFiller!=null)
		{
			while(currentTerm.compareTo(startFiller.endTerm)<=0)
			{
				termDocs.seek(currentTerm);
				while(termDocs.next())
				{
					finalFilter.set(termDocs.doc());
				}
				if(!te.next())
				{
					break;
				}
				currentTerm=te.term();
			}
		}//end for all terms in startFiller
		
		
		//for all range blocks within query range
		for (Iterator iter = containedRanges.iterator(); iter.hasNext();)
		{
			CachableRange cRange = (CachableRange) iter.next();
			if(cRange.bitset!=null)
			{
//				System.out.println("Cache hit on "+cRange);
				finalFilter.or(cRange.bitset);
				currentTerm=cRange.endTerm;
			}
			else
			{
				BitSet thisRangeFilter=finalFilter;
				if(topCandidateRangesForCaching.contains(cRange))
				{
//					System.out.println("Caching date range"+cRange);
					thisRangeFilter=new BitSet(reader.maxDoc());
					cRange.bitset=thisRangeFilter;
				}
				if(currentTerm.compareTo(cRange.startTerm)<0)
				{
					te.skipTo(cRange.startTerm);
					currentTerm=te.term();
				}
				while(currentTerm.compareTo(cRange.endTerm)<=0)
				{
					termDocs.seek(currentTerm);
					while(termDocs.next())
					{
						thisRangeFilter.set(termDocs.doc());
					}
					if(currentTerm.equals(cRange.endTerm))
					{
						break;
					}
					if(!te.next())
					{
						break;
					}
					currentTerm=te.term();
				}
				if(thisRangeFilter!=finalFilter)
				{
					finalFilter.or(thisRangeFilter);
				}
			}
		}//end for all contained ranges
		if(endFiller!=null)
		{
			currentTerm=endFiller.startTerm;
			while(currentTerm.compareTo(endFiller.endTerm)<=0)
			{
				termDocs.seek(currentTerm);
				while(termDocs.next())
				{
					finalFilter.set(termDocs.doc());
				}
				if(!te.next())
				{
					break;
				}
				currentTerm=te.term();
			}
		}//end for all terms in endFiller
		
		termDocs.close();
		
		return new BitSetFilter(finalFilter);
	}
	static class BitSetFilter extends Filter
	{
		BitSet bits;
		
		public BitSetFilter(BitSet bits)
		{
			this.bits = bits;
		}

		public BitSet bits(IndexReader reader) throws IOException
		{
			return bits;
		}		
	}
	static class Range
	{
		public Range(Term startTerm, Term endTerm)
		{
			this.startTerm=startTerm;
			this.endTerm=endTerm;
		}
		public Range()
		{
		}
		Term startTerm;
		Term endTerm;
		boolean intersects(Range other)
		{
			int startStart=startTerm.compareTo(other.startTerm);
			int startEnd=startTerm.compareTo(other.endTerm);
			if(startStart>=0&&startEnd<=0)
			{
				return true;
			}
			int endStart=endTerm.compareTo(other.startTerm);
			int endEnd=endTerm.compareTo(other.endTerm);
			if(endStart>=0&&endEnd<=0)
			{
				return true;
			}
			return startStart<=0&&endEnd>=0;
		}
		
		boolean contains(Range other)
		{
			int startStart=startTerm.compareTo(other.startTerm);
			int endEnd=endTerm.compareTo(other.endTerm);
			return startStart<=0&&endEnd>=0;			
		}
		boolean endsAfter(Range other)
		{
			int endEnd=endTerm.compareTo(other.endTerm);
			return endEnd>0;			
		}
		boolean startsAfter(Range other)
		{
			int startStart=startTerm.compareTo(other.startTerm);
			return startStart>0;			
		}
		public String toString()
		{
			return startTerm+"->"+endTerm;
		}
		public static Term maxTerm(Term a, Term b)
		{
			int diff=a.compareTo(b);
			if(diff>=0)
			{
				return a;
			}
			return b;
		}
		public static Term minTerm(Term a, Term b)
		{
			int diff=a.compareTo(b);
			if(diff<0)
			{
				return a;
			}
			return b;
		}		
	}
	
	static class CachableRange extends Range
	{
		public BitSet bitset;
		//number of times this range has been wholly contained within a query and is therefore
		//a candidate for caching a bitset
		public int numFullHits;
		//Number of times this range has been partially contained within a query and is
		// therefore a candidate for sub-dividing?
		public int numPartialHits;
		//Number of times this range has not been within the range of a query and is therefore
		//not usefully cached.
		public int numMisses;
		
		int df=0;

		public CachableRange()
		{
			super();
		}

		public CachableRange(Term startTerm, Term endTerm)
		{
			super(startTerm, endTerm);
		}
		public int getCachabilityWeighting()
		{
			return numFullHits-numMisses;
		}
		public String toString()
		{
			return startTerm+"->"+endTerm+" (num docs="+df+"\t hits="+numFullHits+"\t misses="+
						numMisses+"\t partials="+numPartialHits+"\tCachability="+
						getCachabilityWeighting()+")";
		}		
	}
	
	static class CachedRanges extends PriorityQueue
	{		
		public CachedRanges(int maxSize)
		{
			initialize(maxSize);
		}

		protected boolean lessThan(Object a, Object b)
		{
			CachableRange ca=(CachableRange) a;
			CachableRange cb=(CachableRange) b;
			return ca.getCachabilityWeighting()<cb.getCachabilityWeighting();
		}		
	}
	

}
