import junit.framework.TestCase;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;

/**
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 */
public class TestIsCurrent extends TestCase
{

   private IndexWriter writer;

   private Directory directory;

   private IndexReader reader;

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();
      // initialize directory
      directory = new RAMDirectory();
      writer = new IndexWriter(directory, new KeywordAnalyzer(), IndexWriter.MaxFieldLength.UNLIMITED);

      // get reader
      reader = writer.getReader();
      // write document
      Document doc = new Document();
      doc.add(new Field("UUID", "1", Store.YES, Index.ANALYZED));
      writer.addDocument(doc);
      writer.commit();
   }

   /**
    * Failing testcase showing the trouble
    * 
    * @throws IOException
    */
   public void testDeleteByTermIsCurrent() throws IOException
   {
      // get reader
      reader = writer.getReader();
      // assert index has a document and reader is up2date 
      assertEquals("One document should be in the index", 1, writer.numDocs());
      assertTrue("Document added, reader should be stale ", reader.isCurrent());

      // remove document
      Term idTerm = new Term("UUID", "1");
      writer.deleteDocuments(idTerm);
      writer.commit();

      // assert document has been deleted (index changed), reader is stale
      assertEquals("Document should be removed", 0, writer.numDocs());
      assertFalse("Reader should be stale", reader.isCurrent());
   }

   /**
    * Testcase for example to show that writer.deleteAll() is working as expected
    * 
    * @throws IOException
    */
   public void testDeleteAllIsCurrent() throws IOException
   {
      // get reader
      reader = writer.getReader();
      // assert index has a document and reader is up2date 
      assertEquals("One document should be in the index", 1, writer.numDocs());
      assertTrue("Document added, reader should be stale ", reader.isCurrent());

      // remove all documents
      writer.deleteAll();
      writer.commit();

      // assert document has been deleted (index changed), reader is stale
      assertEquals("Document should be removed", 0, writer.numDocs());
      assertFalse("Reader should be stale", reader.isCurrent());
   }
}
