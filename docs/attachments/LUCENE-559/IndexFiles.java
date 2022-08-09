package tr.com.meteksan.pdocs.fullTextSearch;




import org.apache.lucene.analysis.tr.TurkishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.struts.upload.FormFile;

import tr.com.meteksan.pdocs.fileUtils.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

public class IndexFiles {
	
  private IndexFiles() {}
  static final File INDEX_DIR = new File("FTS_INDEX");
  public static void index(FormFile file,String id) {
    

    try {
		String fileName=System.currentTimeMillis() + "temp.dat";
		File file_=FileUtils.uploadFile(file,fileName);
			System.out.println(fileName);
				
      IndexWriter writer = new IndexWriter (INDEX_DIR, new TurkishAnalyzer(), false);
	  //IndexWriter writer = new IndexWriter (INDEX_DIR, new StandardAnalyzer(), true);
      System.out.println("Indexing to directory '" +INDEX_DIR+ "'...");
      indexDocs(writer, file_,id);
      System.out.println("Optimizing...");
      writer.optimize();
      writer.close();
    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
       "\n with message: " + e.getMessage());
    }
  }

  static void indexDocs(IndexWriter writer, File file,String id)
    throws IOException {
 
            
        
        try {
          writer.addDocument(FileDocument.Document(file,id));
        }

        catch (FileNotFoundException fnfe) {
          ;
        }
      }
    
  
  
}
