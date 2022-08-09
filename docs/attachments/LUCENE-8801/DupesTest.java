import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DupesTest {

    @Test
    public void testSearchResultDupes() throws Exception {
        File path = File.createTempFile("lucene-dupe-test", "");
        path.delete();

        Directory dir = MMapDirectory.open(path.toPath());
        try {
            IndexWriterConfig conf = new IndexWriterConfig(new EnglishAnalyzer());
            try (IndexWriter w = new IndexWriter(dir, conf)) {
                for(int i = 0; i < 100000; i++) {
                    String id = String.valueOf(i);
                    
                    Document doc = new Document();
                    
                    doc.add(new StoredField("id", id));
                    
                    w.updateDocument(new Term("id", id), doc);
                }
            }
            
            try (DirectoryReader reader = DirectoryReader.open(dir)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                
                HashSet docIds = new HashSet();

                searcher.search(new MatchAllDocsQuery(), new SimpleCollector() {
                    @Override
                    public void collect(int doc) throws IOException {
                        assertTrue(docIds.add(doc), "duplicate document returned (#" + doc + " after " + docIds.size() + " other documents)");
                    }

                    @Override
                    public ScoreMode scoreMode() {
                        return ScoreMode.COMPLETE_NO_SCORES;
                    }

                });
            }

        }
        finally {
            dir.close();
            
            for (File f : path.listFiles()) {
                f.delete();
            }
            path.delete();
        }
    }
    
}
