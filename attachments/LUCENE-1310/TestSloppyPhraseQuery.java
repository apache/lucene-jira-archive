package org.apache.lucene.search;

import junit.framework.TestCase;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.store.RAMDirectory;

public class TestSloppyPhraseQuery extends TestCase {

    private static final Document DOC_1 = makeDocument("X A A A Y");
    private static final Document DOC_2 = makeDocument("X A 1 2 3 A 4 5 6 A Y");

    private static final PhraseQuery QUERY_1 = makePhraseQuery( "A", "A", "A" );
    private static final PhraseQuery QUERY_2 = makePhraseQuery( "A", "1", "2", "3", "A", "4", "5", "6", "A" );


    // Test DOC_1 and QUERY_1.
    // QUERY_1 has an exact match to DOC_1, so all slop values should succeed.
    // Currently, a slop value of 1 does not succeed.

    public void testDoc1_Query1_Slop0() throws Exception {
        checkPhraseQuery(DOC_1, QUERY_1, 0);
    }

    public void testDoc1_Query1_Slop1() throws Exception {
        checkPhraseQuery(DOC_1, QUERY_1, 1);
    }

    public void testDoc1_Query1_Slop2() throws Exception {
        checkPhraseQuery(DOC_1, QUERY_1, 2);
    }

    // Test DOC_2 and QUERY_1.
    // 6 should be the minimum slop to make QUERY_1 match DOC_2.
    // Currently, 7 is the minimum.

    public void testDoc2_Query1_Slop6() throws Exception {
        checkPhraseQuery(DOC_2, QUERY_1, 6);
    }

    public void testDoc2_Query1_Slop7() throws Exception {
        checkPhraseQuery(DOC_2, QUERY_1, 7);
    }

    // Test DOC_2 and QUERY_2.
    // QUERY_2 has an exact match to DOC_2, so all slop values should succeed.
    // Currently, 0 succeeds, 1 through 7 fail, and 8 or greater succeeds.

    public void testDoc2_Query2_Slop0() throws Exception {
        checkPhraseQuery(DOC_2, QUERY_2, 0);
    }

    public void testDoc2_Query2_Slop1() throws Exception {
        checkPhraseQuery(DOC_2, QUERY_2, 1);
    }

    public void testDoc2_Query2_Slop7() throws Exception {
        checkPhraseQuery(DOC_2, QUERY_2, 7);
    }

    public void testDoc2_Query2_Slop8() throws Exception {
        checkPhraseQuery(DOC_2, QUERY_2, 8);
    }

    private void checkPhraseQuery(Document doc, PhraseQuery query, int slop) throws Exception {
        query.setSlop(slop);

        RAMDirectory ramDir = new RAMDirectory();
        WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
        IndexWriter writer = new IndexWriter(ramDir, analyzer);
        writer.addDocument(doc);
        writer.close();

        IndexSearcher searcher = new IndexSearcher(ramDir);
        Hits hits = searcher.search(query);
        assertEquals("number of hits", 1, hits.length());

        searcher.close();
        ramDir.close();
    }

    private static Document makeDocument(String docText) {
        Document doc = new Document();
        doc.add(new Field("f", docText, Field.Store.NO, Field.Index.TOKENIZED));
        return doc;
    }

    private static PhraseQuery makePhraseQuery(String... terms) {
        PhraseQuery query = new PhraseQuery();
        for (String term : terms) {
            query.add(new Term("f", term));
        }
        return query;
    }

}
