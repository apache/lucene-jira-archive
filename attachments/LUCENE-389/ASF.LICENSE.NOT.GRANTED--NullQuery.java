package org.apache.lucene.search;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;

public class NullQuery extends Query {

	public NullQuery() {
		super();
	}

	private class NullScorer extends Scorer{
		int[] _docs;
		int count;
		NullScorer(int[] docs,Similarity similarity){
			super(similarity);
			_docs=docs;
			count=-1;
		}
		
		public int doc() {		
			if (count==-1) return _docs[0];
			return _docs[count];							
		}

		public Explanation explain(int doc) throws IOException {			
		    Explanation explanation = new Explanation();		    
			explanation.setValue(1.0f);
			explanation.setDescription("NullQuery");		    
		    return explanation;
		}

		public boolean next() throws IOException {
			boolean retVal=false;
			if (count<_docs.length-1){
				retVal=true;		
				count++;
			}
			return retVal;
		}

		public float score() throws IOException {
			return 1.0f;
		}

		public boolean skipTo(int target) throws IOException {
			if (target<_docs.length){
				count=target;
				return true;
			}
			else{
				return false;
			}
		}
		
	}
	  private class NullWeight implements Weight {
	    private Searcher searcher;	    

	    public NullWeight(Searcher searcher) {
	      this.searcher = searcher;
	    }

	    public String toString() { return "weight(" + NullQuery.this + ")"; }

	    public Query getQuery() { return NullQuery.this; }
	    public float getValue() { return 1.0f; }

	    public float sumOfSquaredWeights() throws IOException {
	      return 1.0f;
	    }

	    public void normalize(float queryNorm) {
	      
	    }

	    public Scorer scorer(IndexReader reader) throws IOException {
			int[] docs=new int[reader.numDocs()];
			int maxdoc=reader.maxDoc();
			int c=0;
			int k=0;
			
			for (int i=0;i<maxdoc;++i){
				if (!reader.isDeleted(i)){
					docs[k++]=i;
				}
			}					
			return new NullScorer(docs,getSimilarity(searcher));
	    }

	    public Explanation explain(IndexReader reader, int doc)
	      throws IOException {

	      // explain query weight
	      Explanation queryExpl = new Explanation();
	      queryExpl.setDescription("NullQuery:");

	      Explanation boostExpl = new Explanation(getBoost(), "boost");
	      if (getBoost() != 1.0f)
	        queryExpl.addDetail(boostExpl);	  
		  queryExpl.setValue(boostExpl.getValue());
	      	      	           	    
	      return queryExpl;
	    }
	  }
	  
	  protected Weight createWeight(Searcher searcher) {
	    return new NullWeight(searcher);
	  }

	  /** Prints a user-readable version of this query. */
	  public String toString(String field) {
	    StringBuffer buffer = new StringBuffer();	    
	    buffer.append("NQ");
	    if (getBoost() != 1.0f) {
	      buffer.append("^");
	      buffer.append(Float.toString(getBoost()));
	    }
	    return buffer.toString();
	  }

	  /** Returns true iff <code>o</code> is equal to this. */
	  public boolean equals(Object o) {
	    if (!(o instanceof NullQuery))
	      return false;
		NullQuery other = (NullQuery)o;
	    return this.getBoost() == other.getBoost();	      
	  }

	  /** Returns a hash code value for this object.*/
	  public int hashCode() {
	    return Float.floatToIntBits(getBoost());
	  }
}

