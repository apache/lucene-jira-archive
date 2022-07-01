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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Contains a collection of Synonyms - only one synonym is allowed to be associated 
 * with any one root term 
 * @author Mark
 *
 */
public class SynonymSet
{
	Map roots=new HashMap();
	Map variants=new HashMap();
	Set uniqueFieldNames=new HashSet();

	public void addSynonym(Synonym synonym)
	{
		roots.put(synonym.getRootTerm(),synonym);
		uniqueFieldNames.add(synonym.getRootTerm().field);
		Term[] v = synonym.getVariants();
		for (int i = 0; i < v.length; i++)
		{
			variants.put(v[i],synonym);
		}
		variants.put(synonym.getRootTerm(),synonym);
		
	}

	public Synonym getSynonym(Term term)
	{
		return (Synonym) variants.get(term);
	}
	
	public boolean covers(String field)
	{
		return uniqueFieldNames.contains(field);
	}
}
