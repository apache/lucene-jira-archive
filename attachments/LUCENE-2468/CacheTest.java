package test;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.OpenBitSetDISI;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class CacheTest {

    public static void main(String[] args) throws Exception {
        RAMDirectory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new StandardAnalyzer(Version.LUCENE_CURRENT), IndexWriter.MaxFieldLength.UNLIMITED);
        IndexReader reader = writer.getReader();
        IndexSearcher searcher = new IndexSearcher(reader);

        // add a doc, refresh the reader, and check that its there
        Document doc = new Document();
        doc.add(new Field("id", "1", Field.Store.YES, Field.Index.NOT_ANALYZED));
        writer.addDocument(doc);

        reader = refreshReader(reader);
        searcher = new IndexSearcher(reader);

        TopDocs docs = searcher.search(new MatchAllDocsQuery(), 1);
        if (docs.totalHits != 1) {
            System.err.println("Should find a hit...");
        }

        TermsFilter termFilter = new TermsFilter();
        termFilter.addTerm(new Term("id", "1"));

        Filter filter = termFilter;
        filter = new CachingWrapperFilter(filter);
        
        docs = searcher.search(new MatchAllDocsQuery(), filter, 1);
        if (docs.totalHits != 1) {
            System.err.println("[query + filter] Should find a hit...");
        }
        ConstantScoreQuery constantScore = new ConstantScoreQuery(filter);
        docs = searcher.search(constantScore, 1);
        if (docs.totalHits != 1) {
            System.err.println("[just filter] Should find a hit...");
        }

        // now delete the doc, refresh the reader, and see that its not there
        writer.deleteDocuments(new Term("id", "1"));

        reader = refreshReader(reader);
        searcher = new IndexSearcher(reader);
        
        docs = searcher.search(new MatchAllDocsQuery(), filter, 1);
        if (docs.totalHits > 0) {
            System.err.println("[query + filter] Should *not* find a hit...");
        }
        docs = searcher.search(constantScore, 1);
        if (docs.totalHits > 0) {
            System.err.println("[just filter] Should *not* find a hit...");
        }
    }

    private static IndexReader refreshReader(IndexReader reader) throws IOException {
        IndexReader oldReader = reader;
        reader = reader.reopen();
        if (reader != oldReader) {
            oldReader.close();
        }
        return reader;
    }


    public static class CachingWrapperFilter extends Filter {
        Filter filter;

        /**
         * A transient Filter cache (package private because of test)
         */
        transient Map<Object, DocIdSet> cache;

        private final ReentrantLock lock = new ReentrantLock();

        /**
         * @param filter Filter to cache results of
         */
        public CachingWrapperFilter(Filter filter) {
            this.filter = filter;
        }

        /**
         * Provide the DocIdSet to be cached, using the DocIdSet provided
         * by the wrapped Filter.
         * <p>This implementation returns the given {@link DocIdSet}, if {@link DocIdSet#isCacheable}
         * returns <code>true</code>, else it copies the {@link org.apache.lucene.search.DocIdSetIterator} into
         * an {@link org.apache.lucene.util.OpenBitSetDISI}.
         */
        protected DocIdSet docIdSetToCache(DocIdSet docIdSet, IndexReader reader) throws IOException {
            if (docIdSet.isCacheable()) {
                return docIdSet;
            } else {
                final DocIdSetIterator it = docIdSet.iterator();
                // null is allowed to be returned by iterator(),
                // in this case we wrap with the empty set,
                // which is cacheable.
                return (it == null) ? DocIdSet.EMPTY_DOCIDSET : new OpenBitSetDISI(it, reader.maxDoc());
            }
        }

        @Override
        public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
            lock.lock();
            try {
                if (cache == null) {
                    cache = new WeakHashMap<Object, DocIdSet>();
                }

                final DocIdSet cached = cache.get(reader.getFieldCacheKey());
                if (cached != null) return cached;
            } finally {
                lock.unlock();
            }

            final DocIdSet docIdSet = docIdSetToCache(filter.getDocIdSet(reader), reader);
            if (docIdSet != null) {
                lock.lock();
                try {
                    cache.put(reader.getFieldCacheKey(), docIdSet);
                } finally {
                    lock.unlock();
                }
            }

            return docIdSet;
        }

        @Override
        public String toString() {
            return "CachingWrapperFilter(" + filter + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CachingWrapperFilter)) return false;
            return this.filter.equals(((CachingWrapperFilter) o).filter);
        }

        @Override
        public int hashCode() {
            return filter.hashCode() ^ 0x1117BF25;
        }
    }
}
