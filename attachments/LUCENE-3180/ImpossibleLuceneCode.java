import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;


public class ImpossibleLuceneCode {

    public static void main(String[] args) {

        Directory directory = null;
        IndexWriter writer = null;
        IndexReader reader = null;
        IndexSearcher searcher = null;
        try {
            directory = FSDirectory.open(new File("lucene"));

            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_31, new SimpleAnalyzer(Version.LUCENE_31));
            config.setOpenMode(OpenMode.APPEND);
            config.setRAMBufferSizeMB(800.0);
            
            writer = new IndexWriter(directory, config);
            reader = IndexReader.open(writer, true);
            
            // reader = IndexReader.open(directory, false);
            /*
                org.apache.lucene.store.LockObtainFailedException: Lock obtain timed out: NativeFSLock@S:\Java\Morpheum\lucene\write.lock
                    at org.apache.lucene.store.Lock.obtain(Lock.java:84)
                    at org.apache.lucene.index.DirectoryReader.acquireWriteLock(DirectoryReader.java:765)
                    at org.apache.lucene.index.IndexReader.deleteDocument(IndexReader.java:1067)
                    at de.morpheum.morphy.ImpossibleLuceneCode.main(ImpossibleLuceneCode.java:69)
             */
            // reader = IndexReader.open(writer, true);
            /*
                Exception in thread "main" java.lang.UnsupportedOperationException: This IndexReader cannot make any changes to the index (it was opened with readOnly = true)
                    at org.apache.lucene.index.ReadOnlySegmentReader.noWrite(ReadOnlySegmentReader.java:23)
                    at org.apache.lucene.index.ReadOnlyDirectoryReader.acquireWriteLock(ReadOnlyDirectoryReader.java:43)
                    at org.apache.lucene.index.IndexReader.deleteDocument(IndexReader.java:1067)
                    at de.morpheum.morphy.ImpossibleLuceneCode.main(ImpossibleLuceneCode.java:60)
             */
            
            searcher = new IndexSearcher(reader);
            Query query = new MatchAllDocsQuery();

            ScoreDoc[] hits = searcher.search(query, 10).scoreDocs;

            for (ScoreDoc scoreDoc : hits) {
                Document document = reader.document(scoreDoc.doc);

                // update document
                // ...
                
                // update at index
                reader.deleteDocument(scoreDoc.doc);
                writer.addDocument(document);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (writer != null) {
                    writer.optimize();
                    writer.commit();
                    writer.close();
                }
            }
            catch (IOException e) {
            }
            
            try {
                if (searcher != null) {
                    searcher.close();
                }
                if (reader != null) {
                    reader.close();
                }
                if (directory != null) {
                    directory.close();
                }
            }
            catch (IOException e) {
            }
        }
    }
}
