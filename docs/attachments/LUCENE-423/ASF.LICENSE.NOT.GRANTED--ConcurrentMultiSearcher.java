package org.apache.lucene.search;

/**
 * Copyright 2004 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Implements search over a set of <code>Searchables</code>.
 * 
 * <p>
 * Applications usually need only call the inherited {@link #search(Query)} or
 * {@link #search(Query,Filter)} methods.
 */
public class ConcurrentMultiSearcher extends MultiSearcher
{
    private ThreadPoolExecutor executor;

    private BlockingQueue<Runnable> workQueue;

    private Searchable[] searchables;

    // / Thread local storage used so that this method can be called within
    // other threads
    // / each runnable stores the index into this array that it uses for its
    // output
    private static ThreadLocal<TopDocs[]> docs = new ThreadLocal<TopDocs[]>();

    // / Again thread local to make this method callable from multiple threads
    // concurrently
    // / if the runnable catches an exception it stores it at its index location
    // in this array
    // / then the original search method rethrows the exception (actually only
    // one of them)
    private static ThreadLocal<IOException[]> exceptions = new ThreadLocal<IOException[]>();

    /** Creates a searcher which searches <i>searchables</i>. */
    public ConcurrentMultiSearcher(Searchable[] searchables) throws IOException
    {
        super(searchables);
        this.searchables = searchables; // because upper class forgot to make it
        // accessible
        workQueue = new ArrayBlockingQueue<Runnable>(5 * searchables.length);
        executor = new ThreadPoolExecutor(searchables.length,
                searchables.length, 10, TimeUnit.SECONDS, workQueue);
    }

    class Tally
    {
        private int taskCnt;

        private int completeCnt;

        private Object alock;

        /**
         * @brief - construct a new (evanescent) Tally synchronization object
         * @param taskCnt - the number of threads that must call this before a waiter is awaken
         */
        public Tally(int taskCnt)
        {
            this.taskCnt = taskCnt;
            this.completeCnt = 0;
            this.alock = new Object();
        }

        /**
         * @brief - called as each thread completes its part of the work on a query
         *
         */
        public void taskComplete()
        {
            synchronized (alock)
            {
                if (++completeCnt == taskCnt) alock.notify();
            }
        }

        /**
         * @brief - used to block until all segments of a query have completed
         * @throws InterruptedException
         */
        public void await() throws InterruptedException
        {
            synchronized (alock)
            {
                while (completeCnt != taskCnt)
                {
                    alock.wait();
                }
            }

        }
    }

    class RunnableSearch implements Runnable
    {
        private Searchable searcher;

        private Filter filter;

        private int nDocs;

        private TopDocs[] docs;

        private IOException[] ioes;

        private int idx;

        private Query query;

        private Tally tally;

        private Sort sort;
        
        /**
         * @brief - alternate query request constructor that uses default sort mechanism
         * @param searcher - the index instance used for the query
         * @param idx - the index within the output array to store query matches
         * @param query - the query to be executed
         * @param filter - an optional filter to be applied to query results
         * @param nDocs - the maximum number of documents to return for the query
         * @param docs - the destination array of TopDocs in which the results are stored
         * @param ioes - an array to store any exception that occurs during query processing
         * @param tally - a synchronization object used to determine when a query is processed by all threads
         */
        public RunnableSearch(Searchable searcher, int idx, Query query,
                Filter filter, int nDocs, TopDocs[] docs, IOException[] ioes,
                Tally tally)
        {
            if (docs.length <= idx)
                throw new ArrayIndexOutOfBoundsException(idx);
            this.searcher = searcher;
            this.query = query;
            this.filter = filter;
            this.nDocs = nDocs;
            this.docs = docs;
            this.ioes = ioes;
            this.idx = idx;
            this.tally = tally;
            this.sort = null;

        }

        /**
         * @brief - alternate query request constructor that permits specification of the sort component
         * @param searcher - the index instance used for the query
         * @param idx - the index within the output array to store query matches
         * @param query - the query to be executed
         * @param filter - an optional filter to be applied to query results
         * @param sort - a sorting component to order the final results
         * @param nDocs - the maximum number of documents to return for the query
         * @param docs - the destination array of TopDocs in which the results are stored
         * @param ioes - an array to store any exception that occurs during query processing
         * @param tally - a synchronization object used to determine when a query is processed by all threads
         */
        public RunnableSearch(Searchable searcher, int idx, Query query,
                Filter filter, Sort sort, int nDocs, TopDocs[] docs,
                IOException[] ioes, Tally tally)
        {
            if (docs.length <= idx)
                throw new ArrayIndexOutOfBoundsException(idx);
            this.searcher = searcher;
            this.query = query;
            this.filter = filter;
            this.nDocs = nDocs;
            this.docs = docs;
            this.ioes = ioes;
            this.idx = idx;
            this.tally = tally;
            this.sort = sort;

        }

        /**
         * @brief - method run within the thread pool for each request issued to the pool executor
         *
         */
        public void run()
        {
            try
            {
                if (sort == null)
                {
                    docs[idx] = searcher.search(query, filter, nDocs);
                }
                else
                {
                    docs[idx] = searcher.search(query, filter, nDocs, sort);
                }

            }
            catch (IOException e)
            {
                ioes[idx] = e;
            }
            finally
            {
                tally.taskComplete();
            }

        }
    }
    
    /**
     * @brief - helper function to manipulate thread local storage for TopDocs array
     * @return - calling threads array of TopDocs (filled in by Runnable in the thread pool)
     */
    protected TopDocs[] getDocs()
    {
        TopDocs[] ldocs = docs.get();
        if (ldocs == null)
        {
            ldocs = new TopDocs[searchables.length];
            docs.set(ldocs);
        }
        return ldocs;
    }

    /**
     * @brief - helper function to manipulate thread local storage for exception array
     * @return - calling threads array of exception instances (maintained as nulls between calls)
     */
    protected IOException[] getExceptions()
    {
        IOException[] ioes = exceptions.get();
        if (ioes == null)
        {
            ioes = new IOException[searchables.length];
            exceptions.set(ioes);
        }
        return ioes;
    }

    /*
     * @brief - search the underlying searchables applying an optional filter and sorter
     * 
     * @param query - the query to be executed against each searchable; it is assumed that the query is read only
     *                hence thread safe
     *                
     * @param filter - may be null; if present used to filter query results.  The filter calls are isolated within a
     *                 separate thread so, unless the filter is making peculiar updates to some writable cache in an
     *                 unsafe manner, it should be thread safe in this context but let the user beware
     *                 
     * @param nDocs - maximum number of documents to return in the result set, the full query must still be executed but
     *            this value can be used to speed up the sorting afterwards
     * @returns - at most nDocs query matches in canonically sorted order
     * @throws - derived IOException from thread interruption (which should never arise)
     */
    @Override
    public TopDocs search(Query query, Filter filter, int nDocs)
            throws IOException
    {
        HitQueue hq = new HitQueue(nDocs);
        int totalHits = 0;

        TopDocs[] ldocs = getDocs();
        IOException[] ioes = getExceptions();

        RunnableSearch[] searchers = new RunnableSearch[searchables.length];
        Tally tally = new Tally(searchers.length);
        for (int i = 0; i < searchers.length; i++)
        {
            searchers[i] = new RunnableSearch(searchables[i], i, query, filter,
                    nDocs, ldocs, ioes, tally);
            executor.execute(searchers[i]);
        }
        
        try {
            tally.await();
            
        } catch (InterruptedException ie) {
            throw new IOException(ie.getClass().toString() + ":" + ie.getMessage());
        }

        int[] starts = getStarts();

        IOException ioe = null;

        for (int i = 0; i < searchers.length; i++)
        { // search each searcher
            if (ioes[i] != null)
            {
                ioe = ioes[i];
                ioes[i] = null;
            }
            else
            {
                totalHits += ldocs[i].totalHits; // update totalHits
                ScoreDoc[] scoreDocs = ldocs[i].scoreDocs;
                for (int j = 0; j < scoreDocs.length; j++)
                { // merge scoreDocs into hq
                    ScoreDoc scoreDoc = scoreDocs[j];
                    scoreDoc.doc += starts[i]; // convert doc
                    if (!hq.insert(scoreDoc)) break; // no more scores >
                    // minScore
                }
            }
        }

        if (ioe != null) throw ioe;

        ScoreDoc[] scoreDocs = new ScoreDoc[hq.size()];
        for (int i = hq.size() - 1; i >= 0; i--)
            // put docs in array
            scoreDocs[i] = (ScoreDoc) hq.pop();

        return new TopDocs(totalHits, scoreDocs);
    }
    
    /*
     * @brief - search the underlying searchables applying an optional filter and sorter
     * 
     * @param query - the query to be executed against each searchable; it is assumed that the query is read only
     *                hence thread safe
     *                
     * @param filter - may be null; if present used to filter query results.  The filter calls are isolated within a
     *                 separate thread so, unless the filter is making peculiar updates to some writable cache in an
     *                 unsafe manner, it should be thread safe in this context but let the user beware
     *                 
     * @param n - maximum number of documents to return in the result set, the full query must still be executed but
     *            this value can be used to speed up the sorting afterwards
     *            
     * @param sort - the component used to sort the search results; this is done after threaded searching so should be
     *               safe in the context of threading
     * 
     * @see org.apache.lucene.search.MultiSearcher#close()
     * @returns - at most 'n' query matches sorted as implemented by the sort parameter
     * @throws - derived IOException from thread interruption (which should never arise)
     */
    @Override
    public TopFieldDocs search(Query query, Filter filter, int n, Sort sort)
            throws IOException
    {
        FieldDocSortedHitQueue hq = null;
        int totalHits = 0;

        TopDocs[] ldocs = getDocs();
        IOException[] ioes = getExceptions();
        RunnableSearch[] searchers = new RunnableSearch[searchables.length];
        Tally tally = new Tally(searchers.length);
        for (int i = 0; i < searchers.length; i++)
        {
            searchers[i] = new RunnableSearch(searchables[i], i, query, filter,
                    sort, n, ldocs, ioes, tally);
            executor.execute(searchers[i]);
        }
        try {
            tally.await();
            
        } catch (InterruptedException ie) {
            throw new IOException(ie.getClass().toString() + ":" + ie.getMessage());
        }

        int[] starts = getStarts();

        IOException ioe = null;

        for (int i = 0; i < searchers.length; i++)
        { // search each searcher
            TopFieldDocs docs = (TopFieldDocs) ldocs[i];
            if (hq == null) hq = new FieldDocSortedHitQueue(docs.fields, n);
            if (ioes[i] != null)
            {
                ioe = ioes[i];
                ioes[i] = null;

            }
            else
            {
                totalHits += docs.totalHits; // update totalHit
                ScoreDoc[] scoreDocs = docs.scoreDocs;
                for (int j = 0; j < scoreDocs.length; j++)
                { // merge scoreDocs into hq
                    ScoreDoc scoreDoc = scoreDocs[j];
                    scoreDoc.doc += starts[i]; // convert doc
                    if (!hq.insert(scoreDoc)) break; // hq is full and no
                    // more
                    // scores > minScore
                }
            }
        }

        if (ioe != null) throw ioe;

        ScoreDoc[] scoreDocs = new ScoreDoc[hq.size()];
        for (int i = hq.size() - 1; i >= 0; i--)
            // put docs in array
            scoreDocs[i] = (ScoreDoc) hq.pop();

        return new TopFieldDocs(totalHits, scoreDocs, hq.getFields());
    }

    /*
     * @brief - shuts down the internal thread pool
     * 
     * @see org.apache.lucene.search.MultiSearcher#close()
     * @throws - derived IOException from thread interruption (which should never arise)
     */
    @Override
    public void close() throws IOException
    {
        super.close();
        executor.shutdown();
        try
        {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
        catch (InterruptedException ie)
        {
            throw new IOException(ie.getClass().toString() + ":" + ie.getMessage());
        }
    }

}
