import static org.junit.Assert.assertEquals;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import org.junit.Before;
import org.junit.Test;

public class PhraseQueryWithLeadingStopwordsTest {
    private static final Version VERSION = Version.LUCENE_40;
    private final Directory indexDirectory = new RAMDirectory();

    @Before
    public void setUp() throws Exception {
        IndexWriter writer = new IndexWriter(indexDirectory,
            new IndexWriterConfig(VERSION, new StandardAnalyzer(VERSION)));

        Document document = new Document();
        document.add(new TextField("field1", "hello president of the united states", Field.Store.NO));
        document.add(new TextField("field2", "president of the united states", Field.Store.NO));
        writer.addDocument(document);
        writer.close();
    }

    @Test
    public void testPhraseQueryWithLeadingStopwords() throws Exception{
        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(indexDirectory));
        Query query = new QueryParser(VERSION, "field1", new StandardAnalyzer(VERSION))
            .parse("\"the president of the united states\"");
        TopDocs results = searcher.search(query, 10);
        assertEquals(1, results.totalHits);

        query = new QueryParser(VERSION, "field2", new StandardAnalyzer(VERSION))
            .parse("\"the president of the united states\"");
        results = searcher.search(query, 10);
        assertEquals(1, results.totalHits);
    }

}
