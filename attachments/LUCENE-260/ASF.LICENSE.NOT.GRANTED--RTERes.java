package ie_rte_search_test;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.*;

public class RTERes {
    
    
    
    
    public static void main(String[] args) {
        for (int j=0; j<500; j++){
            System.out.println(j);
          
            boolean error = false;                  //used to control flow for error messages
            String indexName = "/opt/lucene/index2";       //local copy of the configuration variable
            IndexSearcher searcher = null;          //the searcher used to open/search the index
            Query query = null;                     //the Query created by the QueryParser
            Hits hits = null;                       //the search results
            int startindex = 0;                     //the first index displayed on this page
            int maxpage    = 10;                    //the maximum items displayed on this page
            String queryString = "RTE";              //the query entered in the previous page
            int maxresults  = 10;              		//string version of maxpage
            int thispage = 0;                       //used for the for/next either maxpage or
            String SortDate = "true";
            
            try {
                searcher = new IndexSearcher(IndexReader.open(indexName));     //create an indexSearcher for our page
                
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
            //did we open the index?
            
            Analyzer analyzer = new StopAnalyzer();               //construct our usual analyzer
            try {
                
                String[] fields = {"contents", "level", "title"};
                int[] flags = {MultiFieldQueryParser.NORMAL_FIELD, MultiFieldQueryParser.NORMAL_FIELD, MultiFieldQueryParser.NORMAL_FIELD};
                
                query = MultiFieldQueryParser.parse(queryString, fields, flags, analyzer);
                query = query.rewrite(IndexReader.open(indexName));
                
                
                
                hits = searcher.search(query,new Sort(new SortField("byNumber",3)));
                //hits = searcher.search(query);
                
            }
            catch (Exception e) {System.out.println(e.getMessage());};
            
            for (int i = 0; i < 10; i++) {  // for each element
                
                try{
                    Document doc = hits.doc(i);                    //get the next document 
                    //System.out.println(doc.get("url"));
                    //System.out.println(doc.get("title"));
                    //System.out.println(doc.get("date"));
                    //System.out.println(doc.get("contents"));      
                    
                   
                    
                }catch (Exception e) {System.out.println(e.getMessage());};
                
            }
            try{
                searcher.close();
            }
            catch (Exception e) {System.out.println(e.getMessage());}
            
            
        }
        
    }
}

