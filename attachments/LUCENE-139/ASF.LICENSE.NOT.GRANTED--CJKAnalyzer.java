/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Lucene" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Lucene", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * $Id: jalopy.xml,v 1.1 2003/04/30 14:36:56 chedong Exp $
 */

package org.apache.lucene.analysis.cjk;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;

import java.io.Reader;

import java.util.Hashtable;


/**
 * Filters CJKTokenizer with StopFilter.
 *
 * @author Che, Dong
 */
public class CJKAnalyzer extends Analyzer {
    //~ Static fields/initializers ---------------------------------------------

    /**
     * An array containing some common English words that are not usually
     * useful for searching. and some double-byte interpunctions.....
     */
    private static String[] stopWords = {
                                            "a", "and", "are", "as", "at", "be",
                                            "but", "by", "for", "if", "in",
                                            "into", "is", "it", "no", "not",
                                            "of", "on", "or", "s", "such", "t",
                                            "that", "the", "their", "then",
                                            "there", "these", "they", "this",
                                            "to", "was", "will", "with", "",
                                            "www"
                                        };

    //~ Instance fields --------------------------------------------------------

    /** stop word list */
    private Hashtable stopTable;

    //~ Constructors -----------------------------------------------------------

    /**
     * Builds an analyzer which removes words in STOP_WORDS.
     */
    public CJKAnalyzer() {
        stopTable = StopFilter.makeStopTable(stopWords);
    }

    /**
     * Builds an analyzer which removes words in the provided array.
     *
     * @param stopWords stop word array
     */
    public CJKAnalyzer(String[] stopWords) {
        stopTable = StopFilter.makeStopTable(stopWords);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * get token stream from input
     *
     * @param fieldName lucene field name
     * @param reader input reader
     *
     * @return TokenStream
     */
    public final TokenStream tokenStream(String fieldName, Reader reader) {
        return new StopFilter(new CJKTokenizer(reader), stopTable);
    }
}
