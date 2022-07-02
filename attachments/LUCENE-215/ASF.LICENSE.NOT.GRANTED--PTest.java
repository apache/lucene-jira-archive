import java.io.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.index.*;
import org.apache.lucene.document.*;

public class PTest {

    static Analyzer analyzer = new StandardAnalyzer();
    static int docId=1;

    public static void main(String[] argv)
	throws IOException
    {
	File index1 = new File ("index1");
	addDocuments (index1);
	File index2 = new File ("index2");
	addDocuments (index2);
	    
	mergeIndexes (index1, index2);
    }

    static void addDocuments (File d)
	throws IOException
    {
	if (!d.exists())
	    d.mkdir();
	IndexWriter writer = new IndexWriter(d, analyzer, true);

	Document doc = new Document();
	doc.add (Field.Keyword("id", "hello"));
	System.err.println ("Added document " + docId);
	docId++;
	writer.addDocument(doc);
	writer.close();
    }

    static void mergeIndexes (File from, File to)
	throws IOException
    {
	IndexReader fromReader = IndexReader.open(from);
	IndexWriter writer = new IndexWriter (to, analyzer, true);
	IndexReader[] readerList = { fromReader };
	writer.addIndexes (readerList);
	writer.close();
	writer = null;
	System.err.println ("Copied " + fromReader.maxDoc() + " new records "
			    + "into index" + to);
	// The following line generates an exception in Lucene-1.4-rc2:
	fromReader.close();
    }

}
