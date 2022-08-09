import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import junit.framework.TestCase;

public class IndexReaderTest extends TestCase {

    public void testIndexReader() throws Exception {
        Directory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new StandardAnalyzer(),
                IndexWriter.MaxFieldLength.UNLIMITED);
        writer.addDocument(createDocument("a"));
        writer.addDocument(createDocument("b"));
        writer.addDocument(createDocument("c"));
        writer.close();
        IndexReader reader = IndexReader.open(dir);
        reader.deleteDocuments(new Term("id", "a"));
        reader.flush();
        reader.deleteDocuments(new Term("id", "b"));
        reader.close();
        IndexReader.open(dir).close();
    }

    private Document createDocument(String id) {
        Document doc = new Document();
        doc.add(new Field("id", id, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
        return doc;
    }
}
