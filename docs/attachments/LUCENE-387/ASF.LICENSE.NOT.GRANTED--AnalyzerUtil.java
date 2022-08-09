package org.apache.lucene.index.memory;

/**
 * Copyright 2005 The Apache Software Foundation
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
import java.io.PrintStream;
import java.io.Reader;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

/**
 * Various utilities avoiding redundant code in several classes.
 * 
 * @author whoschek.AT.lbl.DOT.gov
 */
public class AnalyzerUtil {
	
	private AnalyzerUtil() {};

	/**
	 * Returns a simple analyzer wrapper that logs all tokens produced by the
	 * underlying child analyzer to the given log stream (typically System.err);
	 * Otherwise behaves exactly like the child analyzer, delivering the very
	 * same tokens; useful for debugging purposes on custom indexing and/or
	 * querying.
	 * 
	 * @param child
	 *            the underlying child analyzer
	 * @param log
	 *            the print stream to log to (typically System.err)
	 * @param logName
	 *            a name for this logger (typically "log" or similar)
	 * @return a logging analyzer
	 */
	public static Analyzer getLoggingAnalyzer(final Analyzer child, 
			final PrintStream log, final String logName) {
		
		if (child == null) 
			throw new IllegalArgumentException("child analyzer must not be null");
		if (log == null) 
			throw new IllegalArgumentException("logStream must not be null");

		return new Analyzer() {
			public TokenStream tokenStream(final String fieldName, Reader reader) {
				return new TokenFilter(child.tokenStream(fieldName, reader)) {
					private int position = -1;
					
					public Token next() throws IOException {
						Token token = input.next(); // from filter super class
						log.println(toString(token));
						return token;
					}
					
					private String toString(Token token) {
						if (token == null) return "[" + logName + ":EOS:" + fieldName + "]\n";
						
						position += token.getPositionIncrement();
						return "[" + logName + ":" + position + ":" + fieldName + ":"
								+ token.termText() + ":" + token.startOffset()
								+ "-" + token.endOffset() + ":" + token.type()
								+ "]";
					}					
				};
			}
		};
	}
	
	
	/**
	 * Returns an analyzer wrapper that returns at most the first
	 * <code>maxTokens</code> tokens from the underlying child analyzer,
	 * ignoring all remaining tokens.
	 * 
	 * @param child
	 *            the underlying child analyzer
	 * @param maxTokens
	 *            the maximum number of tokens to return from the underlying
	 *            analyzer (a value of Integer.MAX_VALUE indicates unlimited)
	 * @return an analyzer wrapper
	 */
	public static Analyzer getMaxTokenAnalyzer(final Analyzer child, final int maxTokens) {
		
		if (child == null) 
			throw new IllegalArgumentException("child analyzer must not be null");
		if (maxTokens < 0) 
			throw new IllegalArgumentException("maxTokens must not be negative");
		if (maxTokens == Integer.MAX_VALUE) 
			return child; // no need to wrap
	
		return new Analyzer() {
			public TokenStream tokenStream(String fieldName, Reader reader) {
				return new TokenFilter(child.tokenStream(fieldName, reader)) {
					private int todo = maxTokens;
					
					public Token next() throws IOException {
						return --todo >= 0 ? input.next() : null;
					}
				};
			}
		};
	}
	
	
	/**
	 * Returns an analyzer wrapper that wraps the underlying child analyzer's
	 * token stream into a {@link SynonymTokenFilter}.
	 * 
	 * @param child
	 *            the underlying child analyzer
	 * @param synonyms
	 *            the map used to extract synonyms for terms
	 * @param maxSynonyms
	 *            the maximum number of synonym tokens to return per underlying
	 *            token word (a value of Integer.MAX_VALUE indicates unlimited)
	 */
	public static Analyzer getSynonymAnalyzer(final Analyzer child, 
			final SynonymMap synonyms, final int maxSynonyms) {
		
		if (child == null) 
			throw new IllegalArgumentException("child analyzer must not be null");
		if (synonyms == null)
			throw new IllegalArgumentException("synonyms must not be null");
		if (maxSynonyms < 0) 
			throw new IllegalArgumentException("maxSynonyms must not be negative");
		if (maxSynonyms == 0)
			return child; // no need to wrap
	
		return new Analyzer() {
			public TokenStream tokenStream(String fieldName, Reader reader) {
				return new SynonymTokenFilter(
					child.tokenStream(fieldName, reader), synonyms, maxSynonyms);
			}
		};
	}

	
	// TODO: could use a more general i18n approach ala http://icu.sourceforge.net/docs/papers/text_boundary_analysis_in_java/
	/** (Line terminator followed by zero or more whitespace) two or more times */
	private static final Pattern PARAGRAPHS = Pattern.compile("([\\r\\n\\u0085\\u2028\\u2029][ \\t\\x0B\\f]*){2,}");
	
	/**
	 * Returns at most the first N paragraphs of the given text. Delimiting
	 * characters are excluded from the results. Each returned paragraph is
	 * whitespace-trimmed via String.trim(), potentially an empty string.
	 * 
	 * @param text
	 *            the text to tokenize into paragraphs
	 * @param limit
	 *            the maximum number of paragraphs to return; zero indicates "as
	 *            many as possible".
	 * @return the first N paragraphs
	 */
	public static String[] getParagraphs(String text, int limit) {
		return tokenize(PARAGRAPHS, text, limit);
	}
		
	private static String[] tokenize(Pattern pattern, String text, int limit) {
		String[] tokens = pattern.split(text, limit);
		for (int i=tokens.length; --i >= 0; ) tokens[i] = tokens[i].trim();
		return tokens;
	}
	
	// TODO: don't split on floating point numbers, e.g. 3.1415 (digit before or after '.')
	/** Divides text into sentences; Includes inverted spanish exclamation and question mark */
	private static final Pattern SENTENCES  = Pattern.compile("[!\\.\\?\\xA1\\xBF]+");

	/**
	 * Returns at most the first N sentences of the given text. Delimiting
	 * characters are excluded from the results. Each returned sentence is
	 * whitespace-trimmed via String.trim(), potentially an empty string.
	 * 
	 * @param text
	 *            the text to tokenize into sentences
	 * @param limit
	 *            the maximum number of sentences to return; zero indicates "as
	 *            many as possible".
	 * @return the first N sentences
	 */
	public static String[] getSentences(String text, int limit) {
//		return tokenize(SENTENCES, text, limit); // equivalent but slower
		int len = text.length();
		if (len == 0) return new String[] { text };
		if (limit <= 0) limit = Integer.MAX_VALUE;
		
		// average sentence length heuristic
		String[] tokens = new String[Math.min(limit, 1 + len/40)];
		int size = 0;
		int i = 0;
		
		while (i < len && size < limit) {
			
			// scan to end of current sentence
			int start = i;
			while (i < len && !isSentenceSeparator(text.charAt(i))) i++;
			
			// add sentence (potentially empty)
			if (size == tokens.length) { // grow array
				String[] tmp = new String[tokens.length << 1];
				System.arraycopy(tokens, 0, tmp, 0, size);
				tokens = tmp;
			}
			tokens[size++] = text.substring(start, i).trim();

			// scan to beginning of next sentence
			while (i < len && isSentenceSeparator(text.charAt(i))) i++;
		}
		
		if (size == tokens.length) return tokens;
		String[] tmp = new String[size];
		System.arraycopy(tokens, 0, tmp, 0, size);
		return tmp;
	}

	private static boolean isSentenceSeparator(char c) {
		// regex [!\\.\\?\\xA1\\xBF]
		switch (c) {
			case '!': return true;
			case '.': return true;
			case '?': return true;
			case 0xA1: return true; // spanish inverted exclamation mark
			case 0xBF: return true; // spanish inverted question mark
			default: return false;
		}		
	}
	
}
