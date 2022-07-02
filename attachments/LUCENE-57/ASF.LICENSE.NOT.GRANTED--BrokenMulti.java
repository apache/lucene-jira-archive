import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.document.*;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.*;
import java.io.*;

import java.io.File;
import java.util.Date;


// Simply compile and run.

class BrokenMulti {
    // Simple index with one entry 
    public static void createIndex(String name,String val) throws IOException {
      IndexWriter writer = new IndexWriter(name, new StopAnalyzer(), true);
      
      // make a new, empty document
      Document doc = new Document();
      
      // Add the path of the file as a field named "path".  Use a Text field, so
      // that the index stores the path, and so that the path is searchable
      doc.add(Field.Text("path", new StringReader(val)));

      writer.addDocument(doc);
      writer.optimize();
      writer.close();
    }
    
    public static void main(String[] args) {
	Hits hits;
	Query query;
	try {
	    // First crate two indexes.
	    // The terms contained are very similar, but do not contain
	    // any similar elements.
	    createIndex("index","abc axc ayc azc");
	    createIndex("index2","acc adc aec afc");
	    
	    // Search index separately using WildCard (subclass of MultiTermQuery).
	    // returns 1 hit
	    query = QueryParser.parse("a*c", "path", new StopAnalyzer());
	    Searcher trySearch = new IndexSearcher("index");
	    hits = trySearch.search(query);
	    System.out.println("index gives "+
			       hits.length() + 
			       " total matching documents");
	    
	    // Search index2 separately 
	    // returns 1 hit
	    query = QueryParser.parse("a*c", "path", new StopAnalyzer());
	    Searcher trySearch2 = new IndexSearcher("index2");
	    hits = trySearch2.search(query);
	    System.out.println("index2 gives "+
			       hits.length() + 
			       " total matching documents");
	    
	    // Combined..
	    // Note that here only 1 hit is returned, HOWEVER the 
	    // two searches on the indexes (index index2) separately 
	    // returned one hit each.
	    query = QueryParser.parse("a*c", "path", new StopAnalyzer());
	    Searcher[] searchers = new Searcher[2];
	    searchers[0] = new IndexSearcher("index");
	    searchers[1] = new IndexSearcher("index2");
	    MultiSearcher mSearcher = new MultiSearcher(searchers);
	    hits = mSearcher.search(query);
	    System.out.println("index combined with index2 gives "+
			       hits.length() + 
			       " total matching documents");

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}    
