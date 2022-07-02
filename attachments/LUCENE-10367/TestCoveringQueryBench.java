package org.apache.lucene.sandbox.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LongValuesSource;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.tests.util.LuceneTestCase;

public class TestCoveringQueryBench extends LuceneTestCase {


    public void testRandomBench() throws IOException {
        Directory dir = newDirectory();
        IndexWriter w = new IndexWriter(dir, newIndexWriterConfig());
        int numDocs = 1000000;
        for (int i = 0; i < numDocs; ++i) {
            Document doc = new Document();
            if (random().nextBoolean()) {
                doc.add(new StringField("field", "A", Store.NO));
            }
            if (random().nextBoolean()) {
                doc.add(new StringField("field", "B", Store.NO));
            }
            if (random().nextDouble() > 0.9) {
                doc.add(new StringField("field", "C", Store.NO));
            }
            if (random().nextDouble() > 0.1) {
                doc.add(new StringField("field", "D", Store.NO));
            }

            doc.add(new NumericDocValuesField("min_match", random().nextInt(1)));
            w.addDocument(doc);
        }

        IndexReader r = DirectoryReader.open(w);
        IndexSearcher searcher = new IndexSearcher(r);
        w.close();
        long begineTime, endTime;
        int iters = 1;
        long sumWAND = 0;
        long sumNOWAND = 0;

        for (int iter = 0; iter < iters; ++iter) {
            List<Query> queries = new ArrayList<>();
            queries.add(new TermQuery(new Term("field", "A")));
            queries.add(new TermQuery(new Term("field", "B")));
            queries.add(new TermQuery(new Term("field", "C")));
            queries.add(new TermQuery(new Term("field", "D")));
            queries.add(new TermQuery(new Term("field", "E")));

            begineTime = System.currentTimeMillis();
            Query q = new CoveringQuery(queries, LongValuesSource.constant(1));
            ((CoveringQuery)q).setUseWand(false);
            TopScoreDocCollector collector = TopScoreDocCollector.create(50, 100);
            searcher.search(q, collector);
            endTime = System.currentTimeMillis();
            sumNOWAND += endTime - begineTime;
        }

        for (int iter = 0; iter < iters; ++iter) {
            List<Query> queries = new ArrayList<>();
            queries.add(new TermQuery(new Term("field", "A")));
            queries.add(new TermQuery(new Term("field", "B")));
            queries.add(new TermQuery(new Term("field", "C")));
            queries.add(new TermQuery(new Term("field", "D")));
            queries.add(new TermQuery(new Term("field", "E")));

            begineTime = System.currentTimeMillis();
            Query q = new CoveringQuery(queries, LongValuesSource.constant(1));
            ((CoveringQuery)q).setUseWand(true);
            TopScoreDocCollector collector = TopScoreDocCollector.create(50,100);
            searcher.search(q, collector);
            endTime = System.currentTimeMillis();
            sumWAND += endTime - begineTime;
        }

        System.out.println("TEST: WAND elapsed " + sumWAND + "ms");
        System.out.println("TEST: NOWAND elapsed " + sumNOWAND + "ms");

        r.close();
        dir.close();
    }
}
