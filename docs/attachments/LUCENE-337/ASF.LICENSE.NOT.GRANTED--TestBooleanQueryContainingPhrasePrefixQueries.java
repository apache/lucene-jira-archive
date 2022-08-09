package org.apache.lucene.search;

import junit.framework.TestCase;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;

/**
 * This test elicits the bug described in <a href="http://issues.apache.org/bugzilla/show_bug.cgi?id=33161">33161</a>.
 */
public class TestBooleanQueryContainingPhrasePrefixQueries extends TestCase {
    private IndexSearcher searcher;

    protected void setUp() throws Exception {
        // derived from TestPhrasePrefixQuery
        RAMDirectory indexStore = new RAMDirectory();
        IndexWriter writer = new IndexWriter(indexStore, new SimpleAnalyzer(), true);
        addDocWithField(writer, new Field("body", "blueberry pie", true, true, true));
        addDocWithField(writer, new Field("body", "blueberry strudel", true, true, true));
        addDocWithField(writer, new Field("body", "blueberry pizza", true, true, true));
        addDocWithField(writer, new Field("body", "blueberry chewing gum", true, true, true));
        addDocWithField(writer, new Field("body", "piccadilly circus", true, true, true));
        addDocWithField(writer, new Field("body", "blue raspberry pie", true, true, true));
        writer.optimize();
        writer.close();

        searcher = new IndexSearcher(indexStore);
    }

    private void addDocWithField(IndexWriter writer, Field field) throws IOException {
        Document doc = new Document();
        doc.add(field);
        writer.addDocument(doc);
    }

    public void testBooleanQueryContainingSingleTermPrefixQuery() throws IOException {
        // In order to cause the bug, the outer query must have more than one term and all terms required.
        // The contained PhrasePrefixQuery must contain exactly one term array.

        // This query will be equivalent to +body:pie +body:"blue*"
        BooleanQuery q = new BooleanQuery();
        q.add(new TermQuery(new Term("body", "pie")), true, false);

        PhrasePrefixQuery trouble = new PhrasePrefixQuery();
        trouble.add(new Term[] {
            new Term("body", "blueberry"),
            new Term("body", "blue"),
        });
        q.add(trouble, true, false);

        // exception will be thrown here without fix
        Hits hits = searcher.search(q);

        assertEquals("Wrong number of hits", 2, hits.length());
    }
}
