package org.apache.lucene.search.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;

public class BoostingNearQuery extends SpanNearQuery {
    protected float payloadScore;
    private int payloadsSeen;
	
	 public BoostingNearQuery(SpanQuery[] clauses, int slop, boolean inOrder) {
		 super(clauses, slop, inOrder);
		 
	 }
	 
	 protected Weight createWeight(Searcher searcher) throws IOException {
		 return new BoostingSpanWeight(this, searcher);
	 }	

	 public class BoostingSpanWeight extends SpanWeight {
		 public BoostingSpanWeight(SpanQuery query, Searcher searcher) throws IOException {
			 super (query, searcher);
		 }
		 public Scorer scorer(IndexReader reader) throws IOException {
			 return new BoostingSpanScorer(query.getSpans(reader), this,
					 similarity,
					 reader.norms(query.getField()));
		 }	
		  public Explanation explain(IndexReader reader, int doc)
		    throws IOException {
		        Explanation result = new Explanation();
		        Explanation nonPayloadExpl = super.explain(reader, doc);
		        result.addDetail(nonPayloadExpl);
		        Explanation payloadBoost = new Explanation();
		        result.addDetail(payloadBoost);
		        float avgPayloadScore =  (payloadsSeen > 0 ? (payloadScore / payloadsSeen) : 1); 
		        payloadBoost.setValue(avgPayloadScore);
		        payloadBoost.setDescription("scorePayload(...)");
		        result.setValue(nonPayloadExpl.getValue() * avgPayloadScore);
		        result.setDescription("btq, product of:");
		        return result;
		  }
	 }
	 
	 public class BoostingSpanScorer extends SpanScorer {
	      //TODO: is this the best way to allocate this?
	      byte[] payload = new byte[256];
	      Spans[] subSpans=null;
	      private ArrayList termsPositions = new ArrayList();
          Similarity similarity = getSimilarity();
          
		  protected BoostingSpanScorer(Spans spans, Weight weight, Similarity similarity, byte[] norms)
		    throws IOException {
			  super (spans, weight, similarity, norms);
			  Spans[] spansArr = new Spans[1];
			  spansArr[0] = spans;
			  getTermsPositions(spansArr, termsPositions);
		  }
		  // Get the TermPositions associated with all underlying subspans
		  public void getTermsPositions(Spans[] subSpans, ArrayList termsPositions) {
			  for (int i=0;i<subSpans.length;i++) {
				  if (subSpans[i] instanceof TermSpans) {
					  termsPositions.add(((TermSpans)subSpans[i]).getPositions());
				  } else if (subSpans[i] instanceof NearSpansOrdered) {
					  getTermsPositions(((NearSpansOrdered)subSpans[i]).getSubSpans(), termsPositions);
				  } else if (subSpans[i] instanceof NearSpansUnordered) {
					  getTermsPositions(((NearSpansUnordered)subSpans[i]).getSubSpans(), termsPositions);
				  }
			  }
			  
		  }		  
		  protected boolean setFreqCurrentDoc() throws IOException {
			  if (!more) {
				  return false;
			  }
			  doc = spans.doc();
			  freq = 0.0f;
			  payloadScore = 0;
			  payloadsSeen = 0;
			  while (more && doc == spans.doc()) {	
			      for (Iterator it = termsPositions.iterator(); it.hasNext();) {
			    	  TermPositions positions = (TermPositions) it.next();				  
					  if(positions.isPayloadAvailable()) {
						  payload = positions.getPayload(payload, 0);
						  payloadScore += similarity.scorePayload(field, payload, 0, positions.getPayloadLength());
						  payloadsSeen++;
					  }
				  }
				  int matchLength = spans.end() - spans.start();
				  freq += getSimilarity().sloppyFreq(matchLength);
				  more = spans.next();
			  }
			  return more || (freq != 0);
		  }		 

		  public float score() throws IOException {
			  return super.score() * (payloadsSeen > 0 ? (payloadScore / payloadsSeen) : 1);
		  }		 

	 }
	 
	 
	 
}
