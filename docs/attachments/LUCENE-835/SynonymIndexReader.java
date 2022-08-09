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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;

public class SynonymIndexReader extends IndexReader
{
	private IndexReader delegate;
	private SynonymSet synonymSet;

	public SynonymIndexReader(IndexReader delegate, SynonymSet synonymSet)
	{
		super(delegate.directory());
		this.delegate=delegate;
		this.synonymSet=synonymSet;
	}

	public TermFreqVector[] getTermFreqVectors(int docNumber)
			throws IOException
	{
		//TODO not yet implemented
		throw new UnsupportedOperationException("Not yet implemented");
	}

	public TermFreqVector getTermFreqVector(int docNumber, String field)
			throws IOException
	{		
		TermFreqVector tfv = delegate.getTermFreqVector(docNumber,field);
		if(synonymSet.covers(field))
		{
			//wrap it
			tfv= new SynonymTermFreqVector(tfv, synonymSet);
		}
		return tfv;
		
	}

	public int numDocs()
	{		
		return delegate.numDocs();
	}

	public int maxDoc()
	{
		return delegate.maxDoc();
	}

	public Document document(int n, FieldSelector fieldSelector)
			throws CorruptIndexException, IOException
	{
		return delegate.document(n,fieldSelector);
	}

	public boolean isDeleted(int n)
	{
		return delegate.isDeleted(n);
	}

	public boolean hasDeletions()
	{
		return delegate.hasDeletions();
	}

	public byte[] norms(String field) throws IOException
	{
		return delegate.norms(field);
	}

	public void norms(String field, byte[] bytes, int offset)
			throws IOException
	{
			delegate.norms(field,bytes,offset);
	}

	protected void doSetNorm(int doc, String field, byte value)
			throws CorruptIndexException, IOException
	{
		delegate.doSetNorm(doc,field,value);
	}

	public TermEnum terms() throws IOException
	{		
		return new SynonymTermEnum(delegate.terms(),synonymSet);
	}

	public TermEnum terms(Term t) throws IOException
	{
		TermEnum result = delegate.terms(t);
		if(synonymSet.covers(t.field))
		{
			//wrap it
			result= new SynonymTermEnum(result,synonymSet);
		}
		return result;
	}

	public int docFreq(Term t) throws IOException
	{
		Synonym syn=synonymSet.getSynonym(t);
		if(syn!=null)
		{
			return syn.getDocFreq();
		}
		return delegate.docFreq(t);
	}

	public TermDocs termDocs() throws IOException
	{
		return new SynonymTermDocs(delegate.termDocs(),synonymSet);
	}

	public TermPositions termPositions() throws IOException
	{
		return new SynonymTermPositions(delegate.termDocs(),synonymSet);
	}

	protected void doDelete(int docNum) throws CorruptIndexException,
			IOException
	{
		delegate.doDelete(docNum);
	}

	protected void doUndeleteAll() throws CorruptIndexException, IOException
	{
		delegate.doUndeleteAll();
	}

	protected void doCommit() throws IOException
	{
		delegate.doCommit();
	}

	protected void doClose() throws IOException
	{
		delegate.doClose();
	}

	public Collection getFieldNames(FieldOption fldOption)
	{
		return delegate.getFieldNames(fldOption);
	}
	
	
	/**
	 * A wrapper for a regular term enum which uses synonyms to combine variations into a single term
	 * @author Mark
	 *
	 */
	class SynonymTermEnum extends TermEnum
	{

		private SynonymSet synonymSet;
		private TermEnum delegateTermEnum;
		Term currentTerm=null;
		int synPos=0;
		private int currentDf;
		private Synonym currentSynonym;

		public SynonymTermEnum(TermEnum termEnum,SynonymSet synonymSet)
		{
			this.synonymSet=synonymSet;
			this.delegateTermEnum=termEnum;
		}

		public boolean next() throws IOException
		{
			while(true)
			{
				boolean result=delegateTermEnum.next();
				if(!result)
				{
					//hit end of term enum
					return result;
				}
				currentTerm=delegateTermEnum.term();
				currentDf=delegateTermEnum.docFreq();
				currentSynonym=synonymSet.getSynonym(currentTerm);
				if(currentSynonym==null)
				{
					//has no synonyms - return untouched term
					return result;
				}
				if(currentTerm.equals(currentSynonym.getRootTerm()))
				{
					//Is a root term - return the synonym's DF.
					currentDf=currentSynonym.getDocFreq();
					return true;
				}
				else
				{
					//currentTerm is a variant - ignore and move on to next term
				}
			}
		}

		public Term term()
		{
			return currentTerm;
		}

		public int docFreq()
		{
			return currentDf;
		}

		public void close() throws IOException
		{
			delegateTermEnum.close();
			currentSynonym=null;
			currentTerm=null;
			currentDf=0;
		}	
	}
	
	class SynonymTermPositions extends SynonymTermDocs implements TermPositions 
	{
		int minDocPos=0;
		int []positions=null;
		public SynonymTermPositions(TermDocs docs, SynonymSet synonymSet)
		{
			super(docs,synonymSet);
		}
		protected TermDocs internalGetTermDocs() throws IOException
		{
			TermPositions result = delegate.termPositions();
			//initialize positions
			positions=null;
			return result;
		}
		
		public boolean next() throws IOException
		{
			//initialize positions
			positions=null;
			return super.next();
		}
		
		public int nextPosition() throws IOException 
		{
			if(positions==null)
			{
				//initialize an array of all positions for TermDocs that are for this doc
				ArrayList docPositions=new ArrayList();
				for (int i = 0; i < readerTermDocs.length; i++)
				{
					if(docIds[i]==minDocId)
					{
						TermPositions tp=(TermPositions) readerTermDocs[i];
						for (int j = 0; j < freqs[i]; j++)
						{
							docPositions.add(new Integer(tp.nextPosition()));							
						}
					}
				}
				positions=new int[docPositions.size()];
				int i=0;
				for (Iterator iter = docPositions.iterator(); iter.hasNext();)
				{
					Integer pos= (Integer) iter.next();
					positions[i++]=pos.intValue();
				}
				Arrays.sort(positions);
				minDocPos=0;
			}
			return positions[minDocPos++]; 
		}

	}	
	
	class SynonymTermDocs implements TermDocs
	{
		private TermDocs docs;
		private SynonymSet synonymSet;
		private Term currentTerm;
		private Synonym currentSyn;
		private Term[] currentVariants;
		TermDocs[] readerTermDocs;
//		private int pointer;
		int[] docIds;
		int[] freqs;
		int minDocId=Integer.MAX_VALUE;

		public SynonymTermDocs(TermDocs docs, SynonymSet synonymSet)
		{
			this.docs=docs;
			this.synonymSet=synonymSet;
		}

		public void seek(Term term) throws IOException
		{
			currentTerm=term;
			currentSyn=synonymSet.getSynonym(term);
			if(currentSyn!=null)
			{
				currentVariants=currentSyn.getVariants();
								
				//Add termDocs for root term and all variants
				readerTermDocs = new TermDocs[currentVariants.length+1];
				docIds=new int[currentVariants.length+1];
				freqs=new int[currentVariants.length+1];
				for (int i = 0; i < currentVariants.length; i++)
				{
					readerTermDocs[i]=internalGetTermDocs();
					readerTermDocs[i].seek(currentVariants[i]);
				}
				readerTermDocs[readerTermDocs.length-1]=internalGetTermDocs();
				readerTermDocs[readerTermDocs.length-1].seek(currentSyn.getRootTerm());
				
			}
			else
			{
				readerTermDocs=new TermDocs[1];
				readerTermDocs[0]=internalGetTermDocs();
				readerTermDocs[0].seek(currentTerm);

				docIds=new int[1];
				freqs=new int[1];
			}
		}

		protected TermDocs internalGetTermDocs() throws IOException
		{
			return delegate.termDocs();
		}

		public void seek(TermEnum termEnum) throws IOException
		{
		    seek(termEnum.term());			
		}

		public int doc()
		{
			return minDocId;
		}

		public int freq()
		{
			//scan all TermDocs - if any positioned on same DocId add to freq
			int totalFreq=0;
			for(int i=0;i<docIds.length;i++)
			{
				if(docIds[i]==minDocId)
				{
					totalFreq+=freqs[i];
				}
			}
			return totalFreq;
		}

		public boolean next() throws IOException
		{
			int oldMinDocId=minDocId;
			for (int i = 0; i < readerTermDocs.length; i++)
			{
				if ( 
						(minDocId==Integer.MAX_VALUE) //first time round
							||
						(docIds[i]==minDocId)
						)
				{
					if(readerTermDocs[i].next())
					{
						docIds[i]=readerTermDocs[i].doc();
						freqs[i]=readerTermDocs[i].freq();
					}
					else
					{
						docIds[i]=Integer.MAX_VALUE;
					}
				}
			}

			//reset lowest docid
			minDocId=docIds[0];
			for (int i = 0; i < readerTermDocs.length; i++)
			{
				minDocId=Math.min(minDocId,docIds[i]);
			}
			
			if(minDocId==Integer.MAX_VALUE)
			{
				//All TermDocs at end
				return false;
			}
			return minDocId!=oldMinDocId;
		}

		public int read(int[] docs, int[] freqs) throws IOException
		{
			int numRead=0;
			for(int i=0;i<docs.length;i++)
			{
				if(next())
				{
					docs[i]=doc();
					freqs[i]=freq();
					numRead++;
				}
			}
			return numRead;
		}

		public boolean skipTo(int target) throws IOException
		{
			int doc=doc();
			while(doc<target)
			{
				if(next())
				{
					doc=doc();
				}
				else
				{
					return false;
				}
			}
			return true;
		}

		public void close() throws IOException
		{
			for (int i = 0; i < readerTermDocs.length; i++)
			{
				readerTermDocs[i].close();
			}			
		}
		
	}
	class SynonymTermFreqVector implements TermFreqVector
	{

		private TermFreqVector delegateTermFreqVector;
		private SynonymSet synonymSet;
		private int[] normalizedTfs;
		private String[] normalizedTerms;

		public SynonymTermFreqVector(TermFreqVector termFreqVector, SynonymSet synonymSet)
		{
			this.delegateTermFreqVector=termFreqVector;
			this.synonymSet=synonymSet;
			String[]texts=delegateTermFreqVector.getTerms();
			int[]tfs=delegateTermFreqVector.getTermFrequencies();
			Term templateTerm=new Term(delegateTermFreqVector.getField(),"");
			//TODO could avoid cost of putting non-synonyms into results map as a NormalizedTermCount
			HashMap results=new HashMap();
			for (int i = 0; i < tfs.length; i++)
			{
				Term term=templateTerm.createTerm(texts[i]);
				Synonym syn = synonymSet.getSynonym(term);
				if(syn==null)
				{
					results.put(term,new NormalizedTermCount(term.text,tfs[i]));
				}
				else
				{
					NormalizedTermCount ntc=(NormalizedTermCount) results.get(term);
					if(ntc==null)
					{
						ntc=new NormalizedTermCount(syn.getRootTerm().text,tfs[i]);
						results.put(syn.getRootTerm(),ntc);
					}
					else
					{
						ntc.totalTfs+=tfs[i];
					}
				}
			}
			NormalizedTermCount[] ntcs = (NormalizedTermCount[]) results.values().toArray(new NormalizedTermCount[results.values().size()]);
			Arrays.sort(ntcs);
			normalizedTfs=new int[ntcs.length];
			normalizedTerms=new String[ntcs.length];
			for (int i = 0; i < ntcs.length; i++)
			{
				normalizedTfs[i]=ntcs[i].totalTfs;
				normalizedTerms[i]=ntcs[i].rootTermText;
			}
		}

		public String getField()
		{
			return delegateTermFreqVector.getField();
		}

		public int size()
		{
			return normalizedTerms.length;
		}

		public String[] getTerms()
		{
			return normalizedTerms;
		}

		public int[] getTermFrequencies()
		{
			return normalizedTfs;
		}

		public int indexOf(String term)
		{
			int pos=Arrays.binarySearch(normalizedTerms,term);
			if((pos<0 )||(pos>=normalizedTerms.length))
				return -1;
			if(normalizedTerms[pos].equals(term))
				return pos;
			return -1;
		}

		public int[] indexesOf(String[] terms, int start, int len)
		{
			throw new IllegalStateException("Not yet implemented!");
		}		
	}
	static class NormalizedTermCount implements Comparable
	{
		String rootTermText;
		int totalTfs;
		public NormalizedTermCount(String text, int tfs)
		{
			rootTermText = text;
			totalTfs = tfs;
		}
		public int compareTo(Object o)
		{
			NormalizedTermCount other=(NormalizedTermCount) o;			
			return rootTermText.compareTo(other.rootTermText);
		}
		
		
	}
}
