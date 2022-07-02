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
import java.util.logging.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;

/**
 * Implements search over a single IndexReader, but remains open even if close() is called. This way it can be shared by 
 * multiple objects that need to search the index without being aware of the keep-the-index-open-until-it-changes logic.<br>
 * <br>
 * Basic use (works, but can slow all searcher while opening a new Searcher instance:
 * <pre>
 * class SearcherFactory {
 *     SearcherFactory(Directory directory){
 *         currentSearcher=new DelayCloseIndexSearcher(directory);
 *     }
 *     
 *     public synchronized IndexSearcher createSearcher() {
 *         if (!currentSearcher.isCurrent()) {
 *             currentSearcher.closeWhenDone();
 *             currentSearcher=new DelayCloseIndexSearcher(directory);
 *         }
 *         
 *         currentSearcher.open();
 *         return currentSearcher;
 *     }
 *     
 *     void synchronized close() {
 *         currentSearcher.closeWhenDone();
 *     }
 *     
 *     private final Directory directory;
 *     
 *     private DelayCloseIndexSearcher currentSearcher;
 * }
 * </pre>
 *
 * Objects that need to search the index do the following:<br>
 * <pre>
 * //searcherFactory is created once at startup
 * 
 * IndexSearcher indexSearcher=searcherFactory.createSearcher();
 * Hits hist=indexSearcher.search(query, filter, sort);
 * ... // handle results
 * indexSearcher.close();
 * 
 * // when the application shuts down
 * searcherFactory.close();
 * </pre>
 *
 * @author Luc Vanlerberghe
 */
public class DelayCloseIndexSearcher extends IndexSearcher {
    /**
     * Creates a DelayCloseIndexSearcher searching the index in the provided directory..
     *
     * @param directory containing the index.
     * @throws IOException if an I/O error occurs.
     */
    public DelayCloseIndexSearcher(Directory directory) throws IOException {
        super(directory);
        
        // TODO 20050728: obsolete after Lucene version 1.4.3
        // Not strictly correct, since the index might have changed in the mean time
        // in trunk, this can be obtained from the IndexReader by calling getVersion();
        this.version=IndexReader.getCurrentVersion(directory);
        
        // TODO 20050728: obsolete after Lucene version 1.4.3
        // in trunk, this can be obtained from the IndexReader by calling directory().
        this.directory=directory;
        
        this.usageCount=0;
        this.shouldCloseWhenDone=false;
        
        logger.finer("<init>");
    }
    
    /**
     * This should be called whenever this instances is passed as a new IndexSearcher.
     * Only when each call to open() is balanced with a call to close(), and closeWhenDone has been called, 
     * will super.close() be called.
     */
    public void open() {
        synchronized (this) {
            if (shouldCloseWhenDone) {
                throw new IllegalStateException("closeWhenDone() already called");
            }
            
            ++usageCount;
        }
        
        logger.finest("open()");
    }
    
    /**
     * Signals that this instance may really close when all open() calls have been balanced with a call to close().
     *
     * @throws IOException if an I/O error occurs.
     */
    public void closeWhenDone() throws IOException {
        logger.finer("closeWhenDone()");
        
        boolean doClose; // Keep the actual super.close() out of the synchronized block
        
        synchronized (this) {
            if (shouldCloseWhenDone) {
                throw new IllegalStateException("closeWhenDone() already called");
            }
            
            shouldCloseWhenDone=true;
            
            doClose=(usageCount==0);
        }
        
        if (doClose) {
            logger.finer("super.close()");
            
            super.close();
        }
    }
    
    /**
     * Returns whether the underlying IndexSearcher instance still works on a current version of the index.
     * If it returns false, closeWhenDone() should be called and another instance created to handle further search requests.
     * TODO: From Lucene version 1.9 onwards, this can be replaced by a call getIndexReader().isCurrent().
     * The version and directory fields can then be deleted too.
     * 
     * @return whether the underlying IndexSearcher instance still works on a current version of the index
     * @throws IOException if an I/O error occurs.
     */
    public boolean isCurrent() throws IOException {
        return version==IndexReader.getCurrentVersion(directory);
    }
    
    /**
     * Returns wether the underlying IndexSearcher has really been closed.
     * If it is true, this instance can no longer be used.
     *
     * @return whether the underlying IndexSearcher has really been closed.
     */
    public boolean isClosed() {
        synchronized (this) {
            return shouldCloseWhenDone && usageCount==0;
        }
    }
    
    //
    // IndexSearcher overrides
    //
    
    /**
     * Should be called once for every call to open().
     * If the usageCount drops to zero and closeWhenDone() was called, super.close() will be called.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        logger.finest("close()");
        
        boolean doClose; // Keep the actual super.close() out of the synchronized block
        
        synchronized (this) {
            if (usageCount<=0) {
                throw new IllegalStateException("usageCount<=0");
            }
            
            doClose=(--usageCount==0 && shouldCloseWhenDone);
        }
        
        if (doClose) {
            logger.finer("super.close()");
            
            super.close();
        }
    }
    
    //
    // private members
    //
    
    /**
     * The Directory used to construct the IndexSearcher super instance.
     * Will become obsolete after Lucene version 1.4.3
     */
    private final Directory directory;
    
    /**
     * The version of the index opened by the IndexSearcher super instance.
     * Will become obsolete after Lucene version 1.4.3
     */
    private final long version;
    
    /**
     * The number of open() calls minus the number of close() calls.
     * If this drops to zero and closeWhenDone() is true, super.close() is called.
     */
    private int usageCount;
    
    /**
     * Indicates if closeWhenDone() was called.
     * If true and usageCount is zero, super.close() is called.
     */
    private boolean shouldCloseWhenDone;
    
    /**
     * The Logger instance for this Class.
     */
    private static final Logger logger=Logger.getLogger(DelayCloseIndexSearcher.class.getName());
}
