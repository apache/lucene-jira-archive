package org.company.solr.highlight;

import java.io.IOException;
import java.util.Map;
import org.apache.lucene.analysis.CachingTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.*;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.index.IndexReader;



import org.apache.solr.highlight.DefaultSolrHighlighter;


public class CustomHighlighter extends DefaultSolrHighlighter {
  public static Logger log = LoggerFactory.getLogger(CustomHighlighter.class);

  public CustomHighlighter() {
  }
  
  public CustomHighlighter(SolrCore solrCore) {
    super(solrCore);
  }
  
    @Override 
      public NamedList doHighlighting(DocList docs, Query query, SolrQueryRequest req, String[] defaultFields) throws IOException { 

    	  NamedList highlightedSnippets = super.doHighlighting(docs, query, req, defaultFields); 
          
              IndexSearcher searcher =  req.getSearcher();
              IndexReader reader = searcher.getIndexReader();
             
              String[] fieldNames = getHighlightFields(query, req, defaultFields);
              for (String fieldName : fieldNames)
              {
                  QueryScorer scorer = new QueryScorer(query, null);
                  scorer.setExpandMultiTermQuery(true);
                  scorer.setMaxDocCharsToAnalyze(51200);
                  
                  DocIterator iterator = docs.iterator();
                  for (int i = 0; i < docs.size(); i++) 
                  {
                    int docId = iterator.nextDoc();
                    System.out.println("DocId: " + docId);
                    
                    TokenStream tokenStream = TokenSources.getTokenStream(reader, docId, fieldName);
                    CachingTokenFilter tstream = new CachingTokenFilter(tokenStream);
            		
                    WeightedSpanTermExtractor wste = new WeightedSpanTermExtractor(fieldName);
                    wste.setExpandMultiTermQuery(true);
                    wste.setWrapIfNotCachingTokenFilter(true);
                    wste.setMaxDocCharsToAnalyze(51200);
                    
            	    Map<String,WeightedSpanTerm> weightedSpanTerms  = wste.getWeightedSpanTerms(query, tokenStream, fieldName);
            	    System.out.println("weightedSpanTerms: " + weightedSpanTerms.values());
                     
                  }
              }

              return highlightedSnippets; 
     }
     
}
