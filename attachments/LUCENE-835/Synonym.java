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
import java.util.Arrays;
import java.util.BitSet;

/**
 * Represents multiple variations of the same root term. All terms must be from the same field. 
 * @author Mark
 *
 */
public class Synonym
{
	private Term rootTerm;
	private Term[] variants;
	int df=0;

	public Synonym(Term rootTerm, Term variants[], IndexReader properReader) throws IOException
	{
		this.rootTerm=rootTerm;
		this.variants=variants;
		Arrays.sort(variants);
		BitSet bits=new BitSet(properReader.maxDoc());
		for (int i = 0; i < variants.length; i++)
		{
			if(rootTerm.field!=variants[i].field)
			{
				throw new IllegalArgumentException("All variants must be of same field type: "+
						rootTerm.field+" != "+variants[i].field);
			}
			TermDocs td=properReader.termDocs(variants[i]);
			if(td!=null)
			{
				while(td.next())
				{
					bits.set(td.doc());
				}
			}
		}
		TermDocs td=properReader.termDocs(rootTerm);
		if(td!=null)
		{
			while(td.next())
			{
				bits.set(td.doc());
			}
		}
		df=bits.cardinality();
	}

	public Term getRootTerm()
	{
		return rootTerm;
	}

	public Term[] getVariants()
	{
		return variants;
	}

	public int getDocFreq()
	{
		return df;
	}
	
}
