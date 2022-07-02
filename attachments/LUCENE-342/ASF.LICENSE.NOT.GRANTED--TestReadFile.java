import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;

/*
 * Created on Feb 3, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/**
 * @author lmiller
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TestReadFile {

	public static void main(String[] args) throws IOException, ParseException {
		
		String fileName;
		String offsetStr;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		if (args.length < 1) {
			System.out.println("Enter in file name to read:");
			fileName = br.readLine();
		} else {
			fileName = args[0];
		}
		
		// Delete current index
		File indexDir = new File("C:\\index");
		indexDir.mkdirs();
		File[] indexFiles = indexDir.listFiles();
		for (int i=0; i<indexFiles.length; i++)
		{
			indexFiles[i].delete();
		}

		// Create index
	    IndexWriter writer = new IndexWriter(indexDir, new StandardAnalyzer(), true);
	    writer.close();
	    
	    // Populate index
		File file = new File(fileName);
		System.out.println(fileName + " is " + file.length() + " bytes.");
		InputStreamReader in = new InputStreamReader(new FileInputStream(file), "UTF-8");
//		Document doc = EmailDocument.Document(1, in, "o");
		writer = new IndexWriter(indexDir, new StandardAnalyzer(), false);
//		writer.addDocument(doc);
        writer.addDocument(FileDocument.Document(file));
		writer.close();
		
		// Search index
		System.out.println("Enter in text to search for:");
		String searchStr = br.readLine();
//		Query query = QueryParser.parse(searchStr, "c", new StandardAnalyzer());
		Query query = QueryParser.parse(searchStr, "contents", new StandardAnalyzer());
	    Searcher searcher = new IndexSearcher(indexDir.getAbsolutePath());
		Hits hits = searcher.search(query);
		System.out.println("Found " + hits.length() + " hits.");
		
		br.close();
	}
}
