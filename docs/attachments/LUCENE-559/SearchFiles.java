package tr.com.meteksan.pdocs.fullTextSearch;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.tr.TurkishAnalyzer;
import org.apache.lucene.analysis.tr.TurkishStemmer;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Hits;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
public class SearchFiles {
  private static class OneNormsReader extends FilterIndexReader {
    private String field;

    public OneNormsReader(IndexReader in, String field) {
      super(in);
      this.field = field;
    }

    public byte[] norms(String field) throws IOException {
      return in.norms(this.field);
    }
  }

  private SearchFiles() {}

  public static ArrayList search(String queries) throws IOException, ParseException {
   
   

    String index = "FTS_INDEX";
    String field = "contents";
    
   
    ArrayList resultSet=new ArrayList();
    IndexReader reader = IndexReader.open(index);


//queries=TS.stem(queries);
System.out.println("queries="+queries);
    Searcher searcher = new IndexSearcher(reader);
    Analyzer analyzer = new TurkishAnalyzer();
	//Analyzer analyzer = new StandardAnalyzer();


   TurkishStemmer TS=new TurkishStemmer();
   queries=TS.stem(queries);
    

     

      if (!(queries == null || queries.length() == -1))
       {
//System.out.println();
  System.out.println(queries);
      Query query = QueryParser.parse(queries, field, analyzer);
		

      System.out.println("Searching for: " + query.toString(field));
System.out.println("XXXX:"+field);
System.out.println("XXXX:"+query.toString());

      Hits hits = searcher.search(query);
      
    if(hits!=null)
    {
   
	 System.out.println(hits.length());
    for(int i=0;i< hits.length();i++)
    {
    	Document doc=hits.doc(i);
    	
		resultSet.add( hits.doc(i));
    }
	}
       }

      
      
    
    reader.close();
    return resultSet;
  }
}

