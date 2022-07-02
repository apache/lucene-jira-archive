package org.apache.lucene.search;

/**
 * Copyright 2004 The Apache Software Foundation
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
import junit.framework.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;



/**
 *
 * @author Luc
 */
public class DelayCloseIndexSearcherTest extends TestCase {
    public DelayCloseIndexSearcherTest(String testName) {
        super(testName);
    }
    
    protected void setUp() throws Exception {
        directory=new RAMDirectory();
        analyzer=new SimpleAnalyzer();
        IndexWriter indexWriter=new IndexWriter(directory, analyzer, true);
        
        indexWriter.close();
    }

    protected void tearDown() throws Exception {
    }
    
    private Directory directory;
    private Analyzer analyzer;
    
    public static Test suite() {
        TestSuite suite = new TestSuite(DelayCloseIndexSearcherTest.class);
        
        return suite;
    }
    
    private void updateIndex() throws IOException {
        IndexWriter indexWriter=new IndexWriter(directory, analyzer, false);
        
        Document document=new Document();
        
        indexWriter.addDocument(document);
        indexWriter.close();
    }
    
    //
    // Test cases
    //
    
    /**
     * Construct and call closeWhenDone() immediately.  isClosed() should return true.
     */
    public void testConstructCloseWhenDone() throws IOException {
        DelayCloseIndexSearcher dcis=new DelayCloseIndexSearcher(directory);
        assertFalse(dcis.isClosed());
        
        dcis.closeWhenDone();
        assertTrue(dcis.isClosed());
        
        try {
            dcis.closeWhenDone(); // Should throw IllegalStateException
            
            fail("Double call to closeWhenDone() succeeded");
        } catch (IllegalStateException ignore) {
            // expected case
        }
    }
    
    /**
     * Construct and call open(), close() and closeWhenDone().  isClosed() should return true.
     */
    public void testConstructOpenCloseCloseWhenDone() throws IOException {
        DelayCloseIndexSearcher dcis=new DelayCloseIndexSearcher(directory);
        assertFalse(dcis.isClosed());
        
        dcis.open();
        assertFalse(dcis.isClosed());
        
        dcis.close();
        assertFalse(dcis.isClosed());
        
        dcis.closeWhenDone();
        assertTrue(dcis.isClosed());
    }
    
    /**
     * Construct and call open(), closeWhenDone() and close().  isClosed() should return true.
     */
    public void testConstructOpenCloseWhenDoneClose() throws IOException {
        DelayCloseIndexSearcher dcis=new DelayCloseIndexSearcher(directory);
        assertFalse(dcis.isClosed());
        
        dcis.open();
        assertFalse(dcis.isClosed());
        
        dcis.closeWhenDone();
        assertFalse(dcis.isClosed());
        
        dcis.close();
        assertTrue(dcis.isClosed());
        
        try {
            dcis.close(); // Should throw IllegalStateException
            
            fail("call to close() after isClosed()==true succeeded");
        } catch (IllegalStateException ignore) {
            // expected case
        }
    }
    
    /**
     * Construct and call isCurrent(), update the index and call isCurrent() again.
     */
    public void testConstructIsCurrent() throws IOException {
        DelayCloseIndexSearcher dcis=new DelayCloseIndexSearcher(directory);
        assertFalse(dcis.isClosed());
        
        assertTrue(dcis.isCurrent());
        
        updateIndex();
        
        assertFalse(dcis.isCurrent());
        
        dcis.closeWhenDone();
        assertTrue(dcis.isClosed());
    }
}
