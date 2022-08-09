package org.apache.lucene.search.highlight;                                              

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
 
import java.io.*;                                                                        
import java.util.*;                                                                      
import org.apache.lucene.analysis.*;                                                     
import org.apache.lucene.document.*;                                                     
import org.apache.lucene.index.*;                                                        
import org.apache.lucene.search.*;                                                       
import org.apache.lucene.store.*;                                                        
                                                                                         
/**                                                                                      
 * Highlights query terms in a search result.                                            
 */                                                                                      
public class QueryHighlighter {                                                          
                                                                                         
	/**                                                                                  
	 * Highlights query terms.                                                           
	 *                                                                                   
	 * @param query The Query to highlight terms from.                                   
	 * @param indexreader The IndexReader for the index the search was done on.          
	 * @param analyzer The Analyzer for the index the search was done on.                
	 * @param text The text to highlight terms in.                                       
	 * @param beforehighlight The text to insert before each highlighted term or         
	 *            phrase.                                                                
	 * @param afterhighlight The text to insert after each highlighted term or           
	 *            phrase.                                                                
	 */                                                                                  
	public static final String highlight(String fieldName, Query query,                  
			IndexReader indexreader, Analyzer analyzer, String text,                     
			String beforehighlight, String afterhighlight) {                             
		String retval = text;                                                            
		// Get the list of start/end markers to highlight                                
		ArrayList startindexes = new ArrayList();                                        
		ArrayList endindexes = new ArrayList();                                          
		getHighlightMarkers(fieldName, query, indexreader, analyzer, text,               
				startindexes, endindexes, new ArrayList(), new ArrayList());             
		removeOverlappingMarkers(startindexes, endindexes);                              
		// Highlights                                                                    
		while (startindexes.size() > 0) {                                                
			// Finds the last text to highlight (otherwise the markers will be           
			// off)                                                                      
			int maxstart = -1;                                                           
			int maxstartindex = -1;                                                      
			for (int i = 0; i < startindexes.size(); i++) {                              
				int startindex = ((Integer) startindexes.get(i)).intValue();             
				if (startindex > maxstart) {                                             
					maxstart = startindex;                                               
					maxstartindex = i;                                                   
				}                                                                        
			}                                                                            
			int startindex = ((Integer) startindexes.remove(maxstartindex))              
					.intValue();                                                         
			int endindex = ((Integer) endindexes.remove(maxstartindex))                  
					.intValue();                                                         
			// Checks if the highlighted text is an HTML entity (e.g. "&amp;")           
			// Note: If you check for both the startindex & endindex at the same         
			// time, this                                                                
			// doesn't work (because it could be a phrase)                               
			try {                                                                        
				if (retval.charAt(startindex - 1) == '&') {                              
					startindex--;                                                        
				}                                                                        
			}                                                                            
			catch (Exception e) {}                                                       
			try {                                                                        
				if (retval.charAt(endindex) == ';') {                                    
					endindex++;                                                          
				}                                                                        
			}                                                                            
			catch (Exception e) {}                                                       
			// Inserts the highlight text                                                
			String beforehighlighttext = "";                                             
			try {                                                                        
				beforehighlighttext = retval.substring(0, startindex);                   
			}                                                                            
			catch (Exception e) {}                                                       
			String highlighttext = "";                                                   
			try {                                                                        
				highlighttext = retval.substring(startindex, endindex);                  
			}                                                                            
			catch (Exception e) {}                                                       
			String afterhighlighttext = "";                                              
			try {                                                                        
				afterhighlighttext = retval.substring(endindex);                         
			}                                                                            
			catch (Exception e) {}                                                       
			retval = new StringBuffer().append(beforehighlighttext).append(              
					beforehighlight).append(highlighttext).append(                       
					afterhighlight).append(afterhighlighttext).toString();               
		}                                                                                
		return retval;                                                                   
	}                                                                                    
                                                                                         
	/**                                                                                  
	 * Highlights fragments. This is used to get the best fragments from a               
	 * search result, combine them, and highlight them.                                  
	 *                                                                                   
	 * @param query The Query to highlight terms from.                                   
	 * @param indexreader The IndexReader for the index the search was done on.          
	 * @param analyzer The Analyzer for the index the search was done on.                
	 * @param text The text to highlight terms in.                                       
	 * @param beforehighlight The text to insert before each highlighted term or         
	 *            phrase.                                                                
	 * @param afterhighlight The text to insert after each highlighted term or           
	 *            phrase.                                                                
	 * @param maxfragments The max number of fragments to include. (There may be         
	 *            less if there weren't many matches in the text.)                       
	 * @param fragmentsize The size returned fragments should be. (Fragments may         
	 *            be larger if multiple fragments are combined.)                         
	 * @param fragmentseperator The text to put between fragments. (e.g. "...")          
	 */                                                                                  
	public static final String highlightFragments(String fieldName,                      
			Query query, IndexReader indexreader, Analyzer analyzer,                     
			String text, String beforehighlight, String afterhighlight,                  
			int maxfragments, int fragmentsize, String fragmentseperator) {              
		// Get the list of start/end markers to highlight                                
		ArrayList startindexes = new ArrayList();                                        
		ArrayList endindexes = new ArrayList();                                          
		getHighlightMarkers(fieldName, query, indexreader, analyzer, text,               
				startindexes, endindexes, new ArrayList(), new ArrayList());             
		removeOverlappingMarkers(startindexes, endindexes);                              
		// Get the tokens in the text                                                    
		ArrayList tokens = new ArrayList();                                              
		TokenStream tokenstream = null;                                                  
		try {                                                                            
			tokenstream = analyzer.tokenStream(fieldName,                                
					new StringReader(text));                                             
			for (Token token = tokenstream.next(); token != null; token = tokenstream    
					.next()) {                                                           
				tokens.add(token);                                                       
			}                                                                            
		}                                                                                
		catch (IOException e) {                                                          
			try {                                                                        
				tokenstream.close();                                                     
			}                                                                            
			catch (Exception e2) {}                                                      
			throw new RuntimeException(e);                                               
		}                                                                                
		try {                                                                            
			tokenstream.close();                                                         
		}                                                                                
		catch (Exception e) {}                                                           
		// Gets all the possible fragments                                               
		ArrayList allfragments = new ArrayList();                                        
		ArrayList allfragmentsstartindexes = new ArrayList();                            
		ArrayList allfragmentsendindexes = new ArrayList();                              
		ArrayList allfragmentsscores = new ArrayList();                                  
		for (int i = 0; i < startindexes.size(); i++) {                                  
			// Get the start/end indexes for this match                                  
			int startindex = ((Integer) startindexes.get(i)).intValue();                 
			int endindex = ((Integer) endindexes.get(i)).intValue();                     
			// Find the start/end token indexes for this match                           
			int starttokenindex = -1;                                                    
			int endtokenindex = -1;                                                      
			int tokennum = 0;                                                            
			for (Iterator j = tokens.iterator(); j.hasNext(); tokennum++) {              
				Token token = (Token) j.next();                                          
				if (token.startOffset() == startindex) {                                 
					starttokenindex = tokennum;                                          
				}                                                                        
				if (token.endOffset() == endindex) {                                     
					endtokenindex = tokennum;                                            
				}                                                                        
			}                                                                            
			// Expand the match to have the correct number of tokens                     
			while (endtokenindex - starttokenindex < fragmentsize) {                     
				if (starttokenindex > 0)                                                 
					starttokenindex--;                                                   
				if (endtokenindex < tokens.size() - 1)                                   
					endtokenindex++;                                                     
				if (starttokenindex == 0 && endtokenindex == tokens.size() - 1)          
					break;                                                               
			}                                                                            
			// Get the start/end indexes for the first/last tokens                       
			int fragmentstartindex = -1;                                                 
			int fragmentendindex = -1;                                                   
			tokennum = 0;                                                                
			for (Iterator j = tokens.iterator(); j.hasNext();) {                         
				Token token = (Token) j.next();                                          
				if (tokennum == starttokenindex) {                                       
					fragmentstartindex = token.startOffset();                            
				}                                                                        
				if (tokennum == endtokenindex) {                                         
					fragmentendindex = token.endOffset();                                
				}                                                                        
				tokennum++;                                                              
			}                                                                            
			// Get the fragment                                                          
			String fragment = text.substring(fragmentstartindex,                         
					fragmentendindex);                                                   
			// Remove newline chars from fragments                                       
			fragment = fragment.replaceAll("[\r\n]", "");                                
			// Remove HTML tags from fragments                                           
			fragment = fragment.replaceAll("<.*?>", "");                                 
			fragment = fragment.replaceAll("<.*", "");                                   
			fragment = fragment.replaceAll(".*>", "");                                   
			// Add this fragment to the list                                             
			allfragments.add(fragment);                                                  
			allfragmentsstartindexes.add(new Integer(fragmentstartindex));               
			allfragmentsendindexes.add(new Integer(fragmentendindex));                   
			allfragmentsscores.add(new Float(getFragmentScore(fieldName, query,          
					indexreader, analyzer, fragment)));                                  
		}                                                                                
		// If we have too many fragments, remove some                                    
		while (allfragments.size() > maxfragments) {                                     
			int minfragmentindex = -1;                                                   
			float minfragmentscore = Float.MAX_VALUE;                                    
			for (int fragmentnum = 0; fragmentnum < allfragments.size(); fragmentnum++) {
				String fragment = (String) allfragments.get(fragmentnum);                
				float fragmentscore = ((Float) allfragmentsscores                        
						.get(fragmentnum)).floatValue();                                 
				if (fragmentscore <= minfragmentscore) { // This is "<="                 
															// because if there          
															// are 2 fragments           
															// tied, we want the         
															// 1st one                   
					minfragmentindex = fragmentnum;                                      
					minfragmentscore = fragmentscore;                                    
				}                                                                        
			}                                                                            
			allfragments.remove(minfragmentindex);                                       
			allfragmentsstartindexes.remove(minfragmentindex);                           
			allfragmentsendindexes.remove(minfragmentindex);                             
			allfragmentsscores.remove(minfragmentindex);                                 
		}                                                                                
		// If there are overlapping fragments, combine them                              
		for (int fragmentnum = 0; fragmentnum < allfragments.size() - 1; fragmentnum++) {
			int startindex = ((Integer) allfragmentsstartindexes                         
					.get(fragmentnum)).intValue();                                       
			int endindex = ((Integer) allfragmentsendindexes.get(fragmentnum))           
					.intValue();                                                         
			for (int fragmentnum2 = fragmentnum + 1; fragmentnum2 < allfragments         
					.size(); fragmentnum2++) {                                           
				int startindex2 = ((Integer) allfragmentsstartindexes                    
						.get(fragmentnum2)).intValue();                                  
				int endindex2 = ((Integer) allfragmentsendindexes                        
						.get(fragmentnum2)).intValue();                                  
				if (((startindex <= startindex2) && (startindex2 <= endindex))           
						|| ((startindex2 <= startindex) && (startindex <= endindex2))) { 
					startindex = Math.min(startindex, startindex2);                      
					endindex = Math.min(endindex, endindex2);                            
					allfragments.remove(fragmentnum2);                                   
					allfragmentsstartindexes.remove(fragmentnum2);                       
					allfragmentsendindexes.remove(fragmentnum2);                         
					fragmentnum2--;                                                      
				}                                                                        
			}                                                                            
		}                                                                                
		// Put the result together                                                       
		StringBuffer retvalbuf = new StringBuffer();                                     
		boolean first = true;                                                            
		for (Iterator i = allfragments.iterator(); i.hasNext();) {                       
			String fragment = (String) i.next();                                         
			if (!first)                                                                  
				retvalbuf.append(fragmentseperator);                                     
			retvalbuf.append(fragment.trim());                                           
			first = false;                                                               
		}                                                                                
		// Highlight the result                                                          
		String retval = retvalbuf.toString();                                            
		retval = highlight(fieldName, query, indexreader, analyzer, retval,              
				beforehighlight, afterhighlight);                                        
		return retval;                                                                   
	}                                                                                    
                                                                                         
	/**                                                                                  
	 * Gets the score of a fragment. This is used to find the best fragments.            
	 *                                                                                   
	 * @param query The query.                                                           
	 * @param tokens The tokens in the fragment.                                         
	 * @return The score.                                                                
	 */                                                                                  
	private static final float getFragmentScore(String fieldName, Query query,           
			IndexReader indexreader, Analyzer analyzer, String fragmenttext) {           
		ArrayList highlightedwords = new ArrayList();                                    
		getHighlightMarkers(fieldName, query, indexreader, analyzer, fragmenttext,       
				new ArrayList(), new ArrayList(), new ArrayList(),                       
				highlightedwords);                                                       
		return highlightedwords.size();                                                  
	}                                                                                    
                                                                                         
	/**                                                                                  
	 * Gets the start/end indexes for each term/phrase to highlight. The given           
	 * ArrayLists are used to add the indexes.                                           
	 *                                                                                   
	 * @param query The Query to highlight terms from.                                   
	 * @param indexreader The IndexReader for the index the search was done on.          
	 * @param analyzer The Analyzer for the index the search was done on.                
	 * @param text The text to highlight terms in.                                       
	 * @param startindexes The list of indexes where highlighting should start.          
	 * @param endindexes The list of indexes where highlighting should end.              
	 * @param highlightedwords The list of words that have already been                  
	 *            highlighted (so that they don't get run again).                        
	 * @param highlightedwords2 The same as highlightedwords, except words that          
	 *            aren't found are not added here.                                       
	 */                                                                                  
	private static final void getHighlightMarkers(String fieldName,                      
			Query query, IndexReader indexreader, Analyzer analyzer,                     
			String text, ArrayList startindexes, ArrayList endindexes,                   
			ArrayList highlightedwords, ArrayList highlightedwords2) {                   
		if (query instanceof BooleanQuery) {                                             
			// For boolean queries, recurse through the subqueries                       
			BooleanClause[] queryclauses = ((BooleanQuery) query).getClauses();          
			for (int i = 0; i < queryclauses.length; i++) {                              
				getHighlightMarkers(fieldName, queryclauses[i].query,                    
						indexreader, analyzer, text, startindexes, endindexes,           
						highlightedwords, highlightedwords2);                            
			}                                                                            
		}                                                                                
		else if (query instanceof PhraseQuery) {                                         
			// For phrase queries, find matches only where all of the terms in           
			// the phrase are together                                                   
			Term[] terms = ((PhraseQuery) query).getTerms();                             
			if (((PhraseQuery) query).getSlop() > 0) {                                   
				for (int i = 0; i < terms.length; i++) {                                 
					getHighlightMarkers(fieldName, new TermQuery(terms[i]),              
							indexreader, analyzer, text, startindexes,                   
							endindexes, highlightedwords, highlightedwords2);            
				}                                                                        
				return;                                                                  
			}                                                                            
			String[] stringterms = new String[terms.length];                             
			for (int i = 0; i < terms.length; i++) {                                     
				stringterms[i] = terms[i].text();                                        
			}                                                                            
			if (highlightedwords.contains(convertToList(stringterms))) { return; }       
			TokenStream tokenstream = analyzer.tokenStream(fieldName,                    
					new StringReader(text));                                             
			boolean foundone = false;                                                    
			try {                                                                        
				int termnum = 0;                                                         
				ArrayList tokens = new ArrayList();                                      
				for (Token token = tokenstream.next(); token != null; token = tokenstream
						.next()) {                                                       
					if (token.termText().equalsIgnoreCase(                               
							terms[tokens.size()].text())) {                              
						tokens.add(token);                                               
						if (terms.length == tokens.size()) {                             
							startindexes.add(new Integer(                                
									((Token) tokens.get(0)).startOffset()));             
							endindexes.add(new Integer(((Token) tokens                   
									.get(tokens.size() - 1)).endOffset()));              
							termnum = 0;                                                 
							tokens.clear();                                              
							foundone = true;                                             
						}                                                                
					}                                                                    
					else {                                                               
						termnum = 0;                                                     
						tokens.clear();                                                  
					}                                                                    
				}                                                                        
			}                                                                            
			catch (IOException e) {}                                                     
			try {                                                                        
				tokenstream.close();                                                     
			}                                                                            
			catch (IOException e) {}                                                     
			highlightedwords.add(convertToList(stringterms));                            
			if (foundone)                                                                
				highlightedwords2.add(convertToList(stringterms));                       
		}                                                                                
		else if (query instanceof TermQuery) {                                           
			// For term queries, find matching terms                                     
			Term term = ((TermQuery) query).getTerm();                                   
			if (highlightedwords.contains(convertToList(new String[]{term                
					.text()})))                                                          
				return;                                                                  
			TokenStream tokenstream = analyzer.tokenStream(fieldName,                    
					new StringReader(text));                                             
			boolean foundone = false;                                                    
			try {                                                                        
				for (Token token = tokenstream.next(); token != null; token = tokenstream
						.next()) {                                                       
					if (token.termText().equalsIgnoreCase(term.text())) {                
						startindexes.add(new Integer(token.startOffset()));              
						endindexes.add(new Integer(token.endOffset()));                  
						foundone = true;                                                 
					}                                                                    
				}                                                                        
			}                                                                            
			catch (IOException e) {}                                                     
			try {                                                                        
				tokenstream.close();                                                     
			}                                                                            
			catch (IOException e) {}                                                     
			highlightedwords.add(convertToList(new String[]{term.text()}));              
			if (foundone)                                                                
				highlightedwords2.add(convertToList(new String[]{term.text()}));         
		}                                                                                
		else {                                                                           
			// For other queries, try rewriting the query to see if it's one of          
			// the known types                                                           
			try {                                                                        
				query = query.rewrite(indexreader);                                      
			}                                                                            
			catch (IOException e) {}                                                     
			if (query instanceof BooleanQuery || query instanceof PhraseQuery            
					|| query instanceof TermQuery) {                                     
				// Rewriting worked - recurse to highlight again                         
				getHighlightMarkers(fieldName, query, indexreader, analyzer,             
						text, startindexes, endindexes, highlightedwords,                
						highlightedwords2);                                              
			}                                                                            
		}                                                                                
	}                                                                                    
                                                                                         
	/**                                                                                  
	 * Removes overlapping start/end highlight markers.                                  
	 *                                                                                   
	 * @param startindexes The start indexes.                                            
	 * @param endindexes The end indexes.                                                
	 */                                                                                  
	private static final void removeOverlappingMarkers(ArrayList startindexes,           
			ArrayList endindexes) {                                                      
		for (int i = 0; i < startindexes.size() - 1; i++) {                              
			int startindex = ((Integer) startindexes.get(i)).intValue();                 
			int endindex = ((Integer) endindexes.get(i)).intValue();                     
			for (int j = i + 1; j < startindexes.size(); j++) {                          
				int startindex2 = ((Integer) startindexes.get(j)).intValue();            
				int endindex2 = ((Integer) endindexes.get(j)).intValue();                
				if ((startindex <= startindex2 && startindex2 <= endindex)               
						|| (startindex2 <= startindex && startindex <= endindex2)) {     
					startindexes.set(i, new Integer(Math.min(startindex,                 
							startindex2)));                                              
					startindexes.remove(j);                                              
					endindexes.set(i,                                                    
							new Integer(Math.max(endindex, endindex2)));                 
					endindexes.remove(j);                                                
					j--;                                                                 
				}                                                                        
			}                                                                            
		}                                                                                
	}                                                                                    
                                                                                         
	/**                                                                                  
	 * Converts an Object[] to an ArrayList.                                             
	 *                                                                                   
	 * @param array An Object[].                                                         
	 * @return An ArrayList.                                                             
	 */                                                                                  
	private static final ArrayList convertToList(Object[] array) {                       
		ArrayList retval = new ArrayList(array.length);                                  
		for (int i = 0; i < array.length; i++) {                                         
			retval.add(array[i]);                                                        
		}                                                                                
		return retval;                                                                   
	}                                                                                    
}                                                                                        