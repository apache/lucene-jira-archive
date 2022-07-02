package org.apache.lucene.index;

/**
 * Copyright 2006 The Apache Software Foundation
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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Similarity;

import java.io.IOException;
import java.io.PrintStream;

/**
 * @author karl wettin <karl.wettin@gmail.com>
 *         Date: May 16, 2006
 *         Time: 1:38:11 AM
 */
public interface InterfaceIndexWriter {
    /**
     * Default value for the write lock timeout (1,000).
     */
    long WRITE_LOCK_TIMEOUT = 1000;
    /**
     * Default value for the commit lock timeout (10,000).
     */
    long COMMIT_LOCK_TIMEOUT = 10000;
    /**
     * Default value is 10,000. Change using {@link #setMaxFieldLength(int)}.
     */
    int DEFAULT_MAX_FIELD_LENGTH = 10000;

    /**
     * Expert: Set the Similarity implementation used by this DirectoryIndexWriter.
     *
     * @see org.apache.lucene.search.Similarity#setDefault(org.apache.lucene.search.Similarity)
     */
    public abstract void setSimilarity(Similarity similarity);

    /**
     * Expert: Return the Similarity implementation used by this DirectoryIndexWriter.
     * <p/>
     * <p>This defaults to the current value of {@link org.apache.lucene.search.Similarity#getDefault()}.
     */
    public abstract Similarity getSimilarity();

    /**
     * The maximum number of terms that will be indexed for a single field in a
     * document.  This limits the amount of memory required for indexing, so that
     * collections with very large files will not crash the indexing process by
     * running out of memory.<p/>
     * Note that this effectively truncates large documents, excluding from the
     * index terms that occur further in the document.  If you know your source
     * documents are large, be sure to set this value high enough to accomodate
     * the expected size.  If you set it to Integer.MAX_VALUE, then the only limit
     * is your memory, but you should anticipate an OutOfMemoryError.<p/>
     * By default, no more than 10,000 terms will be indexed for a field.
     */
    public abstract void setMaxFieldLength(int maxFieldLength);

    /**
     * @see #setMaxFieldLength
     */
    public abstract int getMaxFieldLength();

    /**
     * If non-null, information about merges and a message when
     * maxFieldLength is reached will be printed to this.
     */
    public abstract void setInfoStream(PrintStream infoStream);

    /**
     * @see #setInfoStream
     */
    public abstract PrintStream getInfoStream();

    /**
     * Sets the maximum time to wait for a commit lock (in milliseconds).
     */
    public abstract void setCommitLockTimeout(long commitLockTimeout);

    /**
     * @see #setCommitLockTimeout
     */
    public abstract long getCommitLockTimeout();

    /**
     * Sets the maximum time to wait for a write lock (in milliseconds).
     */
    public abstract void setWriteLockTimeout(long writeLockTimeout);

    /**
     * @see #setWriteLockTimeout
     */
    public abstract long getWriteLockTimeout();

    /**
     * Flushes all changes to an index and closes all associated files.
     */
    public abstract void close() throws IOException;

    /**
     * Returns the analyzer used by this index.
     */
    public abstract Analyzer getAnalyzer();

    /**
     * Returns the number of documents currently in this index.
     */
    public abstract int docCount();

    /**
     * Adds a document to this index.  If the document contains more than
     * {@link #setMaxFieldLength(int)} terms for a given field, the remainder are
     * discarded.
     */
    public abstract void addDocument(Document doc) throws IOException;

    /**
     * Adds a document to this index, using the provided analyzer instead of the
     * value of {@link #getAnalyzer()}.  If the document contains more than
     * {@link #setMaxFieldLength(int)} terms for a given field, the remainder are
     * discarded.
     */
    public abstract void addDocument(Document doc, Analyzer analyzer) throws IOException;

    /**
     * Merges all segments together into a single segment, optimizing an index
     * for search.
     */
    public abstract void optimize() throws IOException;

    /**
     * Determines how often segment indices are merged by addDocument().  With
     * smaller values, less RAM is used while indexing, and searches on
     * unoptimized indices are faster, but indexing speed is slower.  With larger
     * values, more RAM is used during indexing, and while searches on unoptimized
     * indices are slower, indexing is faster.  Thus larger values (> 10) are best
     * for batch index creation, and smaller values (< 10) for indices that are
     * interactively maintained.
     * <p/>
     * <p>This must never be less than 2.  The default value is 10.
     */
    public abstract void setMergeFactor(int mergeFactor);

    /**
     * @see #setMergeFactor
     */
    public abstract int getMergeFactor();
}
