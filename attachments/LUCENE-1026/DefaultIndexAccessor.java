package org.apache.lucene.indexaccessor;

/**
 * Derived from code by subshell GmbH
 *
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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.store.Directory;

import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default IndexAccessor implementation.
 * 
 */
public class DefaultIndexAccessor implements IndexAccessor {
  private final static Logger logger = Logger
      .getLogger(DefaultIndexAccessor.class.getPackage().getName());
  private Analyzer analyzer;
  private IndexReader cachedReadingReader = null;
  private final Map<Similarity, Searcher> cachedSearchers;
  private IndexWriter cachedWriter = null;
  private IndexReader cachedWritingReader = null;
  private boolean closed = true;
  private Directory directory;
  private int readingReaderUseCount = 0;
  private int searcherUseCount = 0;
  private int writerUseCount = 0;
  private int writingReaderUseCount = 0;

  /**
   * @param dir
   * @param analyzer
   */
  public DefaultIndexAccessor(Directory dir, Analyzer analyzer) {
    this.directory = dir;
    this.analyzer = analyzer;
    cachedSearchers = new HashMap<Similarity, Searcher>();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.IndexAccessor#activeSearchers()
   */
  public int activeSearchers() {
    int searchers;

    synchronized (this) {
      searchers = this.searcherUseCount;
    }

    return searchers;
  }

  /**
   * Throws an Exception if IndexAccessor is closed.
   * 
   * This method is invoked in a synchronized context.
   */
  private void checkClosed() {
    if (closed) {
      throw new IllegalStateException("index accessor has been closed");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.IndexAccessor#close()
   */
  public void close() {
    synchronized (this) {
      if (closed) {
        throw new IllegalStateException("IndexAccessor is already closed");
      }

      while ((readingReaderUseCount > 0) || (searcherUseCount > 0)
          || (writingReaderUseCount > 0) || (writerUseCount > 0)) {
        try {
          wait();
        } catch (final InterruptedException e) {
        }
      }

      closeCachedReadingReader();
      closeCachedSearchers();

      closeCachedWritingReader();
      closeCachedWriter();

      closed = true;
    }
  }

  /**
   * Closes the cached reading Reader if it has been created.
   * 
   * This method is invoked in a synchronized context.
   */
  private void closeCachedReadingReader() {
    if (cachedReadingReader != null) {
      if (logger.isLoggable(Level.FINE)) {
        logger.fine("closing cached reading reader");
      }

      try {
        cachedReadingReader.close();
      } catch (IOException e) {
        logger.log(Level.SEVERE, "error closing cached reading Reader", e);
      } finally {
        cachedReadingReader = null;
      }
    }
  }

  /**
   * Closes all of the Searchers in the Searcher cache.
   * 
   * This method is invoked in a synchronized context.
   */
  private void closeCachedSearchers() {
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("closing cached searchers (" + cachedSearchers.size() + ")");
    }

    for (Iterator<Searcher> i = cachedSearchers.values().iterator(); i
        .hasNext();) {
      Searcher searcher = i.next();

      try {
        searcher.close();
      } catch (IOException e) {
        logger.log(Level.SEVERE, "error closing cached Searcher", e);
      }
    }

    cachedSearchers.clear();
  }

  /**
   * Closes the cached Writer if it has been created.
   * 
   * This method is invoked in a synchronized context.
   */
  private void closeCachedWriter() {
    if (cachedWriter != null) {
      if (logger.isLoggable(Level.FINE)) {
        logger.fine("closing cached writer");
      }

      try {
        cachedWriter.close();
      } catch (IOException e) {
        logger.log(Level.SEVERE, "error closing cached Writer", e);
      } finally {
        cachedWriter = null;
      }
    }
  }

  /**
   * Closes the cache writing Reader if it has been created.
   * 
   * This method is invoked in a synchronized context.
   */
  private void closeCachedWritingReader() {
    if (cachedWritingReader != null) {
      if (logger.isLoggable(Level.FINE)) {
        logger.fine("closing cached writing reader");
      }

      try {
        cachedWritingReader.close();
      } catch (IOException e) {
        logger.log(Level.SEVERE, "error closing cached writing Reader", e);
      } finally {
        cachedWritingReader = null;
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.IndexAccessor#getDirectoryPath()
   */
  public String getDirectoryPath() {
    String dir = null;

    dir = this.directory.toString();

    return dir;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.IndexAccessor#getReader(boolean)
   */
  public IndexReader getReader(boolean write) throws IOException {
    return write ? getWritingReader() : getReadingReader();
  }

  /**
   * @return
   * @throws IOException
   */
  private IndexReader getReadingReader() throws IOException {
    IndexReader reader;

    synchronized (this) {
      checkClosed();

      if (cachedReadingReader != null) {
        if (logger.isLoggable(Level.FINE)) {
          logger.fine("returning cached reading reader");
        }

        reader = cachedReadingReader;
        readingReaderUseCount++;
      } else {
        if (logger.isLoggable(Level.FINE)) {
          logger.fine("opening new reading reader and caching it");
        }

        reader = IndexReader.open(directory);

        cachedReadingReader = reader;
        readingReaderUseCount = 1;
      }

      notifyAll();
    }

    return reader;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.IndexAccessor#getSearcher()
   */
  public Searcher getSearcher() throws IOException {
    return getSearcher(Similarity.getDefault(), null);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.IndexAccessor#getSearcher(org.apache.lucene.index.IndexReader)
   */
  public Searcher getSearcher(IndexReader indexReader) throws IOException {
    return getSearcher(Similarity.getDefault(), indexReader);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.IndexAccessor#getSearcher(org.apache.lucene.search.Similarity,
   *      org.apache.lucene.index.IndexReader)
   */
  public Searcher getSearcher(Similarity similarity, IndexReader indexReader)
      throws IOException {
    Searcher searcher = null;

    synchronized (this) {
      checkClosed();

      searcher = cachedSearchers.get(similarity);

      if (searcher != null) {
        if (logger.isLoggable(Level.FINE)) {
          logger.fine("returning cached searcher");
        }

        searcherUseCount++;
      } else {
        if (logger.isLoggable(Level.FINE)) {
          logger.fine("opening new searcher and caching it");
        }

        if (indexReader != null) {
          searcher = new IndexSearcher(indexReader);
        } else {
          searcher = new IndexSearcher(directory);
        }

        searcher.setSimilarity(similarity);
        cachedSearchers.put(similarity, searcher);

        searcherUseCount++;
      }

      notifyAll();
    }

    return searcher;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.indexaccessor.IndexAccessor#getWriter()
   */
  public IndexWriter getWriter() throws IOException {
    return getWriter(true);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.IndexAccessor#getWriter(boolean, boolean)
   */
  public IndexWriter getWriter(boolean autoCommit) throws IOException {
    IndexWriter writer;

    synchronized (this) {
      checkClosed();

      while (writingReaderUseCount > 0) {
        try {
          wait();
        } catch (InterruptedException e) {
        }
      }

      if (cachedWriter != null) {
        if (logger.isLoggable(Level.FINE)) {
          logger.fine("returning cached writer");
        }

        writer = cachedWriter;
        writerUseCount++;
      } else {
        if (logger.isLoggable(Level.FINE)) {
          logger.fine("opening new writer and caching it");
        }

        writer = new IndexWriter(directory, autoCommit, analyzer);
        cachedWriter = writer;
        writerUseCount = 1;
      }

      notifyAll();
    }

    return writer;
  }

  /**
   * @return
   * @throws CorruptIndexException
   * @throws IOException
   */
  private IndexReader getWritingReader() throws CorruptIndexException,
      IOException {
    IndexReader reader;

    synchronized (this) {
      checkClosed();

      while (writerUseCount > 0) {
        try {
          wait();
        } catch (InterruptedException e) {
        }
      }

      if (cachedWritingReader != null) {
        if (logger.isLoggable(Level.FINE)) {
          logger.fine("returning cached writing reader");
        }

        reader = cachedWritingReader;
        writingReaderUseCount++;
      } else {
        if (logger.isLoggable(Level.FINE)) {
          logger.fine("opening new writing reader");
        }

        reader = IndexReader.open(directory);
        cachedWritingReader = reader;
        writingReaderUseCount = 1;
      }

      notifyAll();
    }

    return reader;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.IndexAccessor#isOpen()
   */
  public boolean isOpen() {
    synchronized (this) {
      return !closed;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.IndexAccessor#open()
   */
  public void open() {
    synchronized (this) {
      if (!closed) {
        throw new IllegalStateException("index accessor is already open");
      }

      closed = false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.IndexAccessor#readingReadersOut()
   */
  public int readingReadersOut() {
    return readingReaderUseCount;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.IndexAccessor#release(org.apache.lucene.index.IndexReader,
   *      boolean)
   */
  public void release(IndexReader reader, boolean write) {
    if (reader != null) {
      if (write) {
        releaseWritingReader(reader);
      } else {
        releaseReadingReader(reader);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.IndexAccessor#release(org.apache.lucene.index.IndexWriter)
   */
  public void release(IndexWriter writer) {
    if (writer != null) {
      synchronized (this) {
        try {
          if (writer != cachedWriter) {
            throw new IllegalArgumentException(
                "writer not opened by this index accessor");
          }

          writerUseCount--;

          if (writerUseCount == 0) {
            if (logger.isLoggable(Level.FINE)) {
              logger.fine("closing cached writer");
            }

            try {
              cachedWriter.close();
            } catch (IOException e) {
              logger.log(Level.SEVERE, "error closing cached Writer", e);
            } finally {
              cachedWriter = null;
            }
          }
        } finally {
          notifyAll();
        }

        if (writerUseCount == 0) {
          waitForReadersAndCloseCached();
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.IndexAccessor#release(org.apache.lucene.search.Searcher)
   */
  public void release(Searcher searcher) {
    if (searcher != null) {
      synchronized (this) {
        if (!cachedSearchers.containsValue(searcher)) {
          throw new IllegalArgumentException(
              "searcher not opened by this index accessor");
        }

        searcherUseCount--;
        notifyAll();
      }
    }
  }

  /**
   * @param reader
   */
  private void releaseReadingReader(IndexReader reader) {
    if (reader == null) {
      return;
    }

    synchronized (this) {
      if (reader != cachedReadingReader) {
        throw new IllegalArgumentException(
            "reading reader not opened by this index accessor");
      }

      readingReaderUseCount--;
      notifyAll();
    }
  }

  /**
   * @param reader
   */
  private void releaseWritingReader(IndexReader reader) {
    if (reader == null) {
      return;
    }

    synchronized (this) {
      try {
        if (reader != cachedWritingReader) {
          throw new IllegalArgumentException(
              "writing Reader not opened by this index accessor");
        }

        writingReaderUseCount--;

        if (writingReaderUseCount == 0) {
          if (logger.isLoggable(Level.FINE)) {
            logger.fine("closing cached writing Reader");
          }

          try {
            cachedWritingReader.close();
          } catch (IOException e) {
          } finally {
            cachedWritingReader = null;
          }
        }
      } finally {
        notifyAll();
      }

      if (writingReaderUseCount == 0) {
        waitForReadersAndCloseCached();
      }
    }
  }

  /**
   * this method is invoked in a synchronized context
   */
  protected void waitForReadersAndCloseCached() {
    while ((readingReaderUseCount > 0) || (searcherUseCount > 0)) {
      try {
        wait();
      } catch (InterruptedException e) {
      }
    }

    closeCachedReadingReader();
    closeCachedSearchers();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.IndexAccessor#writersOut()
   */
  public int writersOut() {
    int writers;

    synchronized (this) {
      writers = this.writerUseCount;
    }

    return writers;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.mhs.indexaccessor.IndexAccessor#writingReadersOut()
   */
  public int writingReadersOut() {
    int writingReaders;

    synchronized (this) {
      writingReaders = this.writingReaderUseCount;
    }

    return writingReaders;
  }
}
