package org.apache.lucene.search;

/**
 * Copyright 2005 Apache Software Foundation
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

import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.DateField;

import java.io.IOException;
import java.util.Random;
import java.util.BitSet;

import junit.framework.TestCase;

/**
 * Unit test that demonstrates the use of the
 * MissingStringLastComparatorSource, and demonstrates when *not* to use it.
 */
public class TestMissingStringLastComparatorSource extends TestCase {

    public static final boolean F = false;
    public static final boolean T = true;

    public static String[] data = new String [] {
        "A",
        "Z",
        null,
        "B",
        "Y",
        null,
        "C",
        "X"
    };

    
    RAMDirectory index = new RAMDirectory();
    IndexReader r;
    IndexSearcher s;
    Query ALL = new TermQuery(new Term("all","all"));
    
    public TestMissingStringLastComparatorSource(String name) {
	super(name);
    }
    public TestMissingStringLastComparatorSource() {
        super();
    }

    public void setUp() throws Exception {

        /* build an index */
        IndexWriter writer = new IndexWriter(index,
                                             new SimpleAnalyzer(), T);
        
        for (int i = 0; i < data.length; i++) {
            Document doc = new Document();
            doc.add(Field.Keyword("id",String.valueOf(i)));
            doc.add(Field.Keyword("all","all"));
            if (null != data[i]) {
                doc.add(Field.Keyword("data",data[i]));
            }
            writer.addDocument(doc);
        }
        
        writer.optimize();
        writer.close();

        r = IndexReader.open(index);
        s = new IndexSearcher(r);

    }

    /**
     * An example of doing an ascending sort, but keeping records which
     * do not have the sort field at the end of the results.
     */
    public void testAscSort()
        throws Exception {
        
        Hits result;

        /* this will sort on the data column, in lexical order.
         * items with no value should come last
         */
        Sort order = new Sort
            (new SortField("data", new MissingStringLastComparatorSource()));

        result = s.search(ALL, order);
        
        assertEquals("didn't get all", data.length, result.length());
        
        assertEquals("wrong order 0", "0", result.doc(0).get("id"));
        assertEquals("wrong order 1", "3", result.doc(1).get("id"));
        assertEquals("wrong order 2", "6", result.doc(2).get("id"));
        assertEquals("wrong order 3", "7", result.doc(3).get("id"));
        assertEquals("wrong order 4", "4", result.doc(4).get("id"));
        assertEquals("wrong order 5", "1", result.doc(5).get("id"));

                     
    }


    /**
     * same as testAscSort, but uses a secondary sort on the items
     * that are missing values from the primary sort.
     */
    public void testAscSortAndSecondary()
        throws Exception {
        
        Hits result;
        
        /* this will sort on the data column, in lexical order.
         * items with no value should come last
         */
        Sort order = new Sort(new SortField[] {
            new SortField("data", new MissingStringLastComparatorSource()),
            new SortField(null, SortField.DOC)
        });

        result = s.search(ALL, order);
        
        assertEquals("didn't get all", data.length, result.length());
        
        assertEquals("wrong order 0", "0", result.doc(0).get("id"));
        assertEquals("wrong order 1", "3", result.doc(1).get("id"));
        assertEquals("wrong order 2", "6", result.doc(2).get("id"));
        assertEquals("wrong order 3", "7", result.doc(3).get("id"));
        assertEquals("wrong order 4", "4", result.doc(4).get("id"));
        assertEquals("wrong order 5", "1", result.doc(5).get("id"));
            
        assertEquals("wrong order 6", "2", result.doc(6).get("id"));
        assertEquals("wrong order 7", "5", result.doc(7).get("id"));
        
    }

    /**
     * this test demonstrates how to sort records descending, leaving the
     * records without a value at the end -- in this case, we don't use
     * the MissingStringLastComparatorSource at all, becausing missing
     * items are already considered "low" and appear at the end of a
     * reversed sort.
     */
    public void testDscSort() throws Exception {
        
        Hits result;

        /* this should sort on the data column, in reverse lexical order.
         * items with no value should come last
         *
         * if we used MissingStringLastComparatorSource here, the
         * "reverse=true" option on the Sort constructor, would result
         * in the records with no value for the field comming first.
         */
        Sort order = new Sort("data", true);
        

        result = s.search(ALL, order);
        
        assertEquals("didn't get all", data.length, result.length());

        assertEquals("wrong order 0", "1", result.doc(0).get("id"));
        assertEquals("wrong order 1", "4", result.doc(1).get("id"));
        assertEquals("wrong order 2", "7", result.doc(2).get("id"));
        assertEquals("wrong order 3", "6", result.doc(3).get("id"));
        assertEquals("wrong order 4", "3", result.doc(4).get("id"));
        assertEquals("wrong order 5", "0", result.doc(5).get("id"));
                     
    }

}
