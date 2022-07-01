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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;

/**
 * IndexReader that checks if client threads using this class are not over-running a maximum allotted time.
 * Clients must use the {@link ActivityTimeMonitor} class's start and stop methods to define scope and this class
 * wraps all calls to the underlying index with calls to ActivityTimeMonitor's checkForTimeout method which will
 * throw a runtime exeption {@link ActivityTimedOutException} in the event of an activity over-run.
 * 
 */
public class TimeLimitedIndexReader extends FilterIndexReader
{


	private boolean isSubReader;

	public TimeLimitedIndexReader(IndexReader in)
	{
		super(in);
	}

	public TimeLimitedIndexReader(IndexReader indexReader, boolean isSubReader)
	{
		super(indexReader);
		this.isSubReader=isSubReader;
	}

	@Override
	public int docFreq(Term t) throws IOException
	{
		ActivityTimeMonitor.checkForTimeout();
		return super.docFreq(t);
	}

	@Override
	public Document document(int n, FieldSelector fieldSelector)
			throws CorruptIndexException, IOException
	{
		ActivityTimeMonitor.checkForTimeout();
		return super.document(n, fieldSelector);
	}

	@Override
	public TermDocs termDocs() throws IOException
	{
		return new TimeLimitedTermDocs(super.termDocs());
	}
	
	
	@Override
	public TermPositions termPositions(Term term) throws IOException
	{
		ActivityTimeMonitor.checkForTimeout();
		return new TimeLimitedTermPositions(super.termPositions(term));
	}

	


	@Override
	public Document document(int n) throws CorruptIndexException, IOException
	{
		ActivityTimeMonitor.checkForTimeout();
		return super.document(n);
	}




	//Term Docs with timedout safety checks
	class TimeLimitedTermDocs implements TermDocs
	{

		private TermDocs termDocs;

		public TimeLimitedTermDocs(TermDocs termDocs)
		{
			this.termDocs=termDocs;
		}

		public void close() throws IOException
		{
			termDocs.close();
		}

		public int doc()
		{
			ActivityTimeMonitor.checkForTimeout();
			return termDocs.doc();
		}

		public int freq()
		{
			ActivityTimeMonitor.checkForTimeout();
			return termDocs.freq();
		}

		public boolean next() throws IOException
		{
			ActivityTimeMonitor.checkForTimeout();
			return termDocs.next();
		}

		public int read(int[] docs, int[] freqs) throws IOException
		{
			ActivityTimeMonitor.checkForTimeout();
			return termDocs.read(docs, freqs);
		}

		public void seek(Term term) throws IOException
		{
			ActivityTimeMonitor.checkForTimeout();
			termDocs.seek(term);
		}

		public void seek(TermEnum termEnum) throws IOException
		{
			ActivityTimeMonitor.checkForTimeout();
			termDocs.seek(termEnum);
		}

		public boolean skipTo(int target) throws IOException
		{
			ActivityTimeMonitor.checkForTimeout();
			return termDocs.skipTo(target);
		}
	}//End TimeLimitedTermDocs class

	@Override
	public TermDocs termDocs(Term term) throws IOException
	{
		return new TimeLimitedTermDocs(super.termDocs(term));
	}

	@Override
	public TermPositions termPositions() throws IOException
	{
		return new TimeLimitedTermPositions(super.termPositions());
	}
	
	class TimeLimitedTermPositions extends TimeLimitedTermDocs implements TermPositions
	{

		private TermPositions tp;

		public TimeLimitedTermPositions(TermPositions termPositions)
		{
			super (termPositions);
			this.tp=termPositions;
		}

		public byte[] getPayload(byte[] data, int offset) throws IOException
		{
			ActivityTimeMonitor.checkForTimeout();
			return tp.getPayload(data, offset);
		}

		public int getPayloadLength()
		{
			ActivityTimeMonitor.checkForTimeout();
			return tp.getPayloadLength();
		}

		public boolean isPayloadAvailable()
		{
			ActivityTimeMonitor.checkForTimeout();
			return tp.isPayloadAvailable();
		}

		public int nextPosition() throws IOException
		{
			ActivityTimeMonitor.checkForTimeout();
			return tp.nextPosition();
		}
	} //end TimeLimitedTermPositions class

	@Override
	public TermEnum terms() throws IOException
	{
		return new TimeLimitedTermEnum(super.terms());
	}
	
	
	
	@Override
	public IndexReader[] getSequentialSubReaders()
	{
		if(isSubReader) return null;
		IndexReader[] results= super.getSequentialSubReaders();
		IndexReader[] tlResults=new IndexReader[results.length];
		for (int i = 0; i < results.length; i++)
		{
			tlResults[i]=new TimeLimitedIndexReader(results[i],true);
		}
		return tlResults;
	}


	class TimeLimitedTermEnum extends TermEnum
	{

		private TermEnum terms;

		public TimeLimitedTermEnum(TermEnum terms)
		{
			this.terms=terms;
		}

		@Override
		public void close() throws IOException
		{
			terms.close();
		}

		@Override
		public int docFreq()
		{
			ActivityTimeMonitor.checkForTimeout();
			return terms.docFreq();
		}

		@Override
		public boolean next() throws IOException
		{
			ActivityTimeMonitor.checkForTimeout();
			return terms.next();
		}

		//Deprecated in 2.9 but then removed in 3.0 - default base class impl calls next() repeatedly so covered (see above)
//		@Override
//		public boolean skipTo(Term target) throws IOException
//		{
//			ActivityTimeMonitor.checkForTimeout();
//			return terms.skipTo(target);
//		}

		@Override
		public Term term()
		{
			ActivityTimeMonitor.checkForTimeout();
			return terms.term();
		}
		
	}//end class TimeLimitedTermEnum

	@Override
	public TermEnum terms(Term t) throws IOException
	{
		return new TimeLimitedTermEnum(super.terms(t));
	}
	

	
}
