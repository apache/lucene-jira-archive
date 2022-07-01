package org.my.company;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.apache.lucene.analysis.CachingTokenFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.lucene.search.vectorhighlight.FragListBuilder;
import org.apache.lucene.search.vectorhighlight.FragmentsBuilder;
import org.apache.lucene.util.AttributeSource.State;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.plugin.PluginInfoInitialized;


import org.apache.lucene.search.vectorhighlight.FieldFragList;
import org.apache.lucene.search.vectorhighlight.FieldFragList.WeightedFragInfo;
import org.apache.lucene.search.vectorhighlight.FieldFragList.WeightedFragInfo.SubInfo;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList.WeightedPhraseInfo;
import org.apache.lucene.search.vectorhighlight.FieldPhraseList.WeightedPhraseInfo.Toffs;
import org.apache.lucene.search.vectorhighlight.FieldTermStack;
import org.apache.lucene.search.vectorhighlight.FieldTermStack.TermInfo;
import org.apache.solr.common.params.CommonParams;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.QParserPlugin;
import org.apache.lucene.queryParser.ParseException;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.IntRange;
import org.apache.commons.lang.math.Range;


public class CustomSolrHighlighter extends DefaultSolrHighlighter {
    
  public CustomSolrHighlighter() {
  }
  
  public CustomSolrHighlighter(SolrCore solrCore) {
    super(solrCore);
  }
  
  
    @Override
    protected void doHighlightingByFastVectorHighlighter( FastVectorHighlighter highlighter, FieldQuery fieldQuery,
          SolrQueryRequest req, NamedList docSummaries, int docId, Document doc,
          String fieldName ) throws IOException {
        SolrParams params = req.getParams(); 
        SolrFragmentsBuilder solrFb = getSolrFragmentsBuilder( fieldName, params );
        String[] snippets = highlighter.getBestFragments( fieldQuery, req.getSearcher().getReader(), docId, fieldName,
            params.getFieldInt( fieldName, HighlightParams.FRAGSIZE, 100 ),
            params.getFieldInt( fieldName, HighlightParams.SNIPPETS, 1 ),
            getFragListBuilder( fieldName, params ),
            getFragmentsBuilder( fieldName, params ),
            solrFb.getPreTags( params, fieldName ),
            solrFb.getPostTags( params, fieldName ),
            getEncoder( fieldName, params ) );


        // Get query object and check type of query
        // If query is Phrase query then use FieldFragList to return FragInfos that contain the position offsets(Toffs) 
	// Otherwise use FieldTermStack to return position offsets

        List positionOffsets = new ArrayList<String[]>();
        String q = req.getParams().get(CommonParams.Q);
        String defType = params.get(QueryParsing.DEFTYPE, QParserPlugin.DEFAULT_QTYPE);
        Query query = null;
	 
	   try{
		   QParser parser = QParser.getParser(q, defType, req); 
		   query = parser.getQuery();    
	   }
	   catch (ParseException e){
		   throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, e);
	   }

        FragListBuilder fragListBuilder = getFragListBuilder( fieldName, params );
        FieldTermStack fieldTermStack = new FieldTermStack( req.getSearcher().getReader(), docId, fieldName, fieldQuery );

        if( query instanceof PhraseQuery ) { // Use fragInfos to get position offsets

             FieldPhraseList fieldPhraseList = new FieldPhraseList( fieldTermStack, fieldQuery );
             FieldFragList   fieldFragList = fragListBuilder.createFieldFragList( fieldPhraseList, 18 );
             List<WeightedFragInfo> fragInfos = fieldFragList.getFragInfos();

            for (WeightedFragInfo fragInfo : fragInfos) {
                List<SubInfo> subInfos = fragInfo.getSubInfos();
                for (SubInfo subInfo : subInfos) {

                  List<Toffs> termOffsets = subInfo.getTermsOffsets();
                  for (Toffs termOffset : termOffsets) {
                       positionOffsets.add(termOffset.getPositionOffset());
                  }
                }
            }
        }
        else {        // return position offset of all terms in fieldTermStack   
           while( !fieldTermStack.isEmpty() ) {
                TermInfo ti = fieldTermStack.pop();
                positionOffsets.add(Integer.toString(ti.getPosition()));
           } 
        }

        StringBuilder hitPositionOffsets = new StringBuilder();
        for (Object position : positionOffsets) {
            if (hitPositionOffsets.length() == 0)
                hitPositionOffsets.append(position);
            else
                hitPositionOffsets.append(',').append(position);
        }


        if( snippets != null && snippets.length > 0 ) {
           docSummaries.add( fieldName, snippets );
           docSummaries.add( "positionOffsets", hitPositionOffsets.toString());
        }
        else {
          alternateField( docSummaries, params, doc, fieldName );
        }

      }
  
}
