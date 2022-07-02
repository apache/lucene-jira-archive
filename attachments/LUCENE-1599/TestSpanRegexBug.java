package testspanregexbug;

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.regex.SpanRegexQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;

@SuppressWarnings("deprecation")
public class TestSpanRegexBug extends TestCase {
    Directory indexStoreA = new RAMDirectory();
    Directory indexStoreB = new RAMDirectory();

    @Override
    protected void setUp() throws Exception {
        createRAMDirectories();
    }

    @Override
    protected void tearDown() throws Exception {
        indexStoreA.close();
        indexStoreB.close();
    }

    private void createRAMDirectories() throws CorruptIndexException,
    LockObtainFailedException, IOException {
        // creating a document to store
        Document lDoc = new Document();
        lDoc.add(new Field("field", "a1 b1",
                Field.Store.NO,
                Field.Index.ANALYZED_NO_NORMS));

        // creating a document to store
        Document lDoc2 = new Document();
        lDoc2.add(new Field("field", "a2 b2", Field.Store.NO,
                Field.Index.ANALYZED_NO_NORMS));

        // creating first index writer
        IndexWriter writerA = new IndexWriter(indexStoreA,
                new StandardAnalyzer(), true,
                IndexWriter.MaxFieldLength.LIMITED);
        writerA.addDocument(lDoc);
        writerA.optimize();
        writerA.close();

        // creating second index writer
        IndexWriter writerB = new IndexWriter(indexStoreB,
                new StandardAnalyzer(), true,
                IndexWriter.MaxFieldLength.LIMITED);
        writerB.addDocument(lDoc2);
        writerB.optimize();
        writerB.close();
    }

    public void testSpanRegexBug() throws CorruptIndexException, IOException {
        SpanRegexQuery srq = new SpanRegexQuery(new Term("field", "a.*"));
        SpanRegexQuery stq = new SpanRegexQuery(new Term("field", "b.*"));
        SpanNearQuery query = new SpanNearQuery(new SpanQuery[] { srq, stq },
                6, true);

        // 1. Search the same store which works
        IndexSearcher[] arrSearcher = new IndexSearcher[2];
        arrSearcher[0] = new IndexSearcher(indexStoreA);
        arrSearcher[1] = new IndexSearcher(indexStoreB);
        MultiSearcher searcher = new MultiSearcher(arrSearcher);
        Hits hits = searcher.search(query);
        arrSearcher[0].close();
        arrSearcher[1].close();

        // Will fail here
        // We expect 2 but only one matched
        // The rewriter function only write it once on the first IndexSearcher
        // So it's using term: a1 b1 to search on the second IndexSearcher
        // As a result, it won't match the document in the second IndexSearcher
        assertEquals(2, hits.length());
    }
}
