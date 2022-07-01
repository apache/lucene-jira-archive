

import java.io.File;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;


public class Test {

	String indexDir = "E:/index";
	IndexWriter indexWriter;
	
	FieldType fieldType = new FieldType();
	
	
	public static void main(String[] args) {
		new Test().doIndex();
	}
	
	private void doIndex(){
		System.out.println("started");
		
		fieldType.setIndexed(true);
		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_46, new StandardAnalyzer(Version.LUCENE_46));
		conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		  
		try {
			indexWriter = new IndexWriter(FSDirectory.open(new File(indexDir)), conf);
			
			int k = 2;
			for(int i = 0; i < k; i++){
				
				ParsingReader pReader = new ParsingReader(i);
				Document doc = new Document();
				doc.add(new Field("data", pReader, fieldType));
				indexWriter.addDocument(doc);
				pReader.close();
				
			}
			
			indexWriter.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("terminated");
	}
	
	/*
	 * Class to parallel parse documents
	 */
	private class ParsingReader extends Reader implements Runnable{
		
		PipedReader reader = new PipedReader();
		PipedWriter writer = new PipedWriter(reader);
		int num;
		
		public ParsingReader(int num) throws IOException{
			this.num = num;
			new Thread(this).start();
		}
		
		@Override
		public void close() throws IOException {
			reader.close();
		}

		@Override
		public int read(char[] buf, int off, int len) throws IOException {
			System.out.println(num + ": reading data");
			return reader.read(buf, off, len);
		}

		@Override
		public void run() {
			
			try {
				
				writer.write("data data data");
				
				//Add embedded doc as individual doc
				Document doc = new Document();
				StringReader data = new StringReader("data data data");
				doc.add(new Field("data", data, fieldType));
				indexWriter.addDocument(doc);
				
				writer.write("data data data");
				writer.close();
								
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		
		
	}

}
