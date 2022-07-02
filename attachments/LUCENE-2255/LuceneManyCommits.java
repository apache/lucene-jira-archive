import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.util.Collections;

/**
 * Example program that fails at adding 1000 documents with the error:
 * <pre>
 * Exception in thread "main" java.io.FileNotFoundException: /home/mikkel/Projects/higgla/lucenemanycommits/_3an.fdx (Too many open files)
	at java.io.RandomAccessFile.open(Native Method)
	at java.io.RandomAccessFile.<init>(RandomAccessFile.java:212)
	at org.apache.lucene.store.SimpleFSDirectory$SimpleFSIndexOutput.<init>(SimpleFSDirectory.java:180)
	at org.apache.lucene.store.NIOFSDirectory.createOutput(NIOFSDirectory.java:74)
	at org.apache.lucene.index.FieldsWriter.<init>(FieldsWriter.java:86)
	at org.apache.lucene.index.StoredFieldsWriter.initFieldsWriter(StoredFieldsWriter.java:66)
	at org.apache.lucene.index.StoredFieldsWriter.finishDocument(StoredFieldsWriter.java:144)
	at org.apache.lucene.index.StoredFieldsWriter$PerDoc.finish(StoredFieldsWriter.java:193)
	at org.apache.lucene.index.DocumentsWriter$WaitQueue.writeDocument(DocumentsWriter.java:1443)
	at org.apache.lucene.index.DocumentsWriter$WaitQueue.add(DocumentsWriter.java:1462)
	at org.apache.lucene.index.DocumentsWriter.finishDocument(DocumentsWriter.java:1082)
	at org.apache.lucene.index.DocumentsWriter.updateDocument(DocumentsWriter.java:776)
	at org.apache.lucene.index.DocumentsWriter.addDocument(DocumentsWriter.java:751)
	at org.apache.lucene.index.IndexWriter.addDocument(IndexWriter.java:1928)
	at org.apache.lucene.index.IndexWriter.addDocument(IndexWriter.java:1902)
	at LuceneManyCommits.main(LuceneManyCommits.java:53)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
	at java.lang.reflect.Method.invoke(Method.java:597)
	at com.intellij.rt.execution.application.AppMain.main(AppMain.java:110)
 * </pre>
 *
 * This is because indexWriter.getReader() leaks file handles.
 */
public class LuceneManyCommits {

    public static void main (String[] args)throws Exception {
        IndexWriter indexWriter = new IndexWriter(
                           FSDirectory.open(new File("lucenemanycommits")),
                           new StandardAnalyzer(Version.LUCENE_CURRENT,
                                                Collections.EMPTY_SET),
                           IndexWriter.MaxFieldLength.LIMITED);

        for (int i = 0; i < 1000; i++) {
            // Leak file handles - move getReader() outside the loop to fix
            IndexReader liveReader = indexWriter.getReader();

            String fieldValue = "value_"+i;            

            Document doc = new Document();
            doc.add(new Field("testfield", fieldValue,
                              Field.Store.YES, Field.Index.NOT_ANALYZED));
            indexWriter.addDocument(doc);
            indexWriter.commit();

            // This close() also fixes the problem, if commented in
            //liveReader.close();
        }
        indexWriter.close();
        System.out.println("All good");
    }

}
