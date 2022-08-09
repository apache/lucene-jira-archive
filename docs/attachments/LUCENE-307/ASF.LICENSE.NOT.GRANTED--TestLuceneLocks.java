import java.util.Date;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

/*******************************************************************************
 * This class demonstrates how an IndexReader and IndexWriter can cause a 
 * "java.io.IOException: Lock obtain timed out" error.
 */
public class TestLuceneLocks {

    final static String indexFolder = "/TestIndex";
    static boolean mStop = false;
    final static String field = "foo";
    final static String value = "bar";

    public static void main(String[] args) throws Exception {

        // read-only access to index
        Thread readThread = new Thread(new Runnable() {

            public void run() {

                try {
                    int runNumber = 0;
                    while ( !mStop) {
                        System.out.println( ++runNumber
                            + ":\tOpening read-only...");
                        IndexReader indexReader = IndexReader.open(indexFolder);
                        indexReader.close();
                    }
                }
                catch (Exception e) {
                    mStop = true;
                    e.printStackTrace();
                }
            }
        });

        // write access to index
        final Thread writeThread = new Thread(new Runnable() {

            public void run() {

                try {
                    int runNumber = 0;
                    while ( !mStop) {
                        // add a document
                        System.out.println( ++runNumber
                            + ":\tAdding documents...");
                        IndexWriter indexWriter = new IndexWriter(
                                                                  indexFolder,
                                                                  new StandardAnalyzer(),
                                                                  false);
                        Document document = new Document();
                        document.add(new Field(field, value, true, true, true));
                        indexWriter.addDocument(document);
                        indexWriter.close();
                        System.out.println(runNumber
                            + ":\tFinished adding documents");
                    }
                }
                catch (Exception e) {
                    mStop = true;
                    e.printStackTrace();
                }
            }
        });

        final Date startTime = new Date();

        // create the index
        IndexWriter indexWriter = new IndexWriter(indexFolder,
                                                  new StandardAnalyzer(), true);
        Document document = new Document();
        document.add(new Field(field, value, true, true, true));
        indexWriter.addDocument(document);
        indexWriter.close();

        // start the threads
        writeThread.start();
        readThread.start();

        // wait for the threads to finish
        writeThread.join();
        readThread.join();

        // display time
        final Date endTime = new Date();
        long time = (endTime.getTime() - startTime.getTime()) / 1000;
        System.out.println("Total time: " + time + "secs");
    }
}