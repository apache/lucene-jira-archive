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

import junit.framework.TestCase;

import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import java.io.IOException;

/**
 * Unit tests related to {@link Term}.
 *
 * @author danz
 */
public class TestTerm extends TestCase {

    public TestTerm( final String name ) {
        super( name );
    }

    protected void setUp() {
    }

    protected void tearDown() {
    }

    /**
     * Call to {@link Term#compareTo} where the {@link Term#text} of the target
     * is <b>null</b> and the {@link Term#text} of the argument is <b>null</b>.
     */
    public void testCompareToTextNullNull() throws IOException {
        final Term a = new Term( "field" , null , true );
        final Term b = new Term( "field" , null , true );

        assertEquals( "compareTo result" , 0 , a.compareTo( b ) );
    }

    /**
     * Call to {@link Term#compareTo} where the {@link Term#text} of the target
     * is <b>null</b> and the {@link Term#text} of the argument is <b>not null</b>.
     */
    public void testCompareToTextNullNotNull() throws IOException {
        final Term a = new Term( "field" , null , true );
        final Term b = new Term( "field" , "abc" , true );

        assertTrue( "compareTo result LE -1" , a.compareTo( b ) <= -1 );
    }

    /**
     * Call to {@link Term#compareTo} where the {@link Term#text} of the target
     * is <b>not null</b> and the {@link Term#text} of the argument is <b>null</b>.
     */
    public void testCompareToTextNotNullNull() throws IOException {
        final Term a = new Term( "field" , "abc" , true );
        final Term b = new Term( "field" , null , true );

        assertTrue( "compareTo result GE +1" , a.compareTo( b ) >= +1 );
    }

    /**
     * Call to {@link Term#compareTo} where the {@link Term#text} of the target
     * is <b>equal to</b> the {@link Term#text} of the argument.
     */
    public void testCompareToTextEquals() throws IOException {
        final Term a = new Term( "field" , "abc" , true );
        final Term b = new Term( "field" , "abc" , true );

        assertEquals( "compareTo result" , 0 , a.compareTo( b ) );
    }

    /**
     * Call to {@link Term#compareTo} where the {@link Term#text} of the target
     * is <b>before</b> the {@link Term#text} of the argument.
     */
    public void testCompareToTextBefore() throws IOException {
        final Term a = new Term( "field" , "abc" , true );
        final Term b = new Term( "field" , "xyz" , true );

        assertTrue( "compareTo result LE -1" , a.compareTo( b ) <= -1 );
    }

    /**
     * Call to {@link Term#compareTo} where the {@link Term#text} of the target
     * is <b>after</b> the {@link Term#text} of the argument.
     */
    public void testCompareToTextAfter() throws IOException {
        final Term a = new Term( "field" , "xyz" , true );
        final Term b = new Term( "field" , "abc" , true );

        assertTrue( "compareTo result GE +1" , a.compareTo( b ) >= +1 );
    }

} // class
