import java.io.IOException;
import java.util.Enumeration;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

public class FieldTest {

		private static String INDEXDIR = "/home/dnaber/testindex";
		
		public static void main(String[] args) throws IOException {
				Analyzer analyzer = new StandardAnalyzer();
				IndexWriter writer = new IndexWriter(INDEXDIR, analyzer, true);

				Document doc = new Document();
				doc.add(new Field("storedfield", "value1", true, true, true));
				doc.add(new Field("unstoredfield", "value2", false, true, true));
				System.out.println("fields after doc.add():");
				printFields(doc);
				writer.addDocument(doc);
				writer.close();
				
				IndexReader ir = IndexReader.open(INDEXDIR);
				Document doc2 = ir.document(0);
				System.out.println("fields after closing and opening the index:");
				printFields(doc2);
				System.out.println("Done");
		}

		private static void printFields(Document d2) {
			Enumeration enum = d2.fields();
			while (enum.hasMoreElements()) {
				Field f = (Field)enum.nextElement();
				System.out.println("  name() = "+f.name() + ", isStored() = "+f.isStored());
			}
		}
}
