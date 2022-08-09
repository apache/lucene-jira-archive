package com.mhs.indexaccessor;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.indexaccessor.IndexAccessor;
import org.apache.lucene.indexaccessor.IndexAccessorFactory;
import org.apache.lucene.indexaccessor.MultiIndexAccessor;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NoLockFactory;

/**
 * After using a write operation you must call commit in a finally block.
 * 
 */
public class SimpleSearchServer {
  private IndexAccessorFactory factory = null;
  private IndexAccessor indexAccessor = null;
  private static LockFactory lockFactory = new NoLockFactory();
  private File indexDir;
  IndexWriter writer = null;

  public SimpleSearchServer() {
    factory = IndexAccessorFactory.getInstance();
  }

  public static void createIndex(File indexPath) throws IOException {
    Analyzer analyzer = new WhitespaceAnalyzer();
    FSDirectory dir = FSDirectory.getDirectory(indexPath, lockFactory);
    IndexAccessorFactory.getInstance().createAccessor(dir, analyzer);

  }

  public static Hits search(Set<File> indexes, Query query) throws IOException {
    IndexAccessorFactory factory = IndexAccessorFactory.getInstance();
    MultiIndexAccessor multiIndexAccessor = factory.getMultiIndexAccessor();
    Searcher searcher = multiIndexAccessor.getMultiSearcher(indexes);
    Hits hits = null;

    try {
      hits = searcher.search(query);
    } finally {
      multiIndexAccessor.release(searcher);
    }

    return hits;
  }

  public void addDoc(Document doc) throws IOException {
    if (writer == null) {
      writer = indexAccessor.getWriter(false);
    }

    writer.addDocument(doc);
  }

  /**
   * Call after performing a batch of index write operations.
   */
  public void commitWriter() {
    if (writer != null) {
      indexAccessor.release(writer);
    }
  }

  public void open(File indexPath) {
    this.indexDir = indexPath;
    indexAccessor = factory.getAccessor(indexDir);
  }

  public void removeDocs(Term term) throws IOException {
    if (writer == null) {
      writer = indexAccessor.getWriter(false);
    }

    writer.deleteDocuments(term);
  }

  public Hits search(Query query) throws IOException {
    Searcher searcher = indexAccessor.getSearcher();
    Hits hits = null;

    try {
      hits = searcher.search(query);
    } finally {
      indexAccessor.release(searcher);
    }

    return hits;
  }

  public void shutdown() {
    factory.closeAllAccessors();
  }
}
