package org.apache.lucene.search;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;

public class MatchAllDocsQuery extends Query {

	public MatchAllDocsQuery() {
		super();
	}

	private class MatchAllScorer extends Scorer{
		IndexReader _reader;
		int _count;
		int _maxDoc;
		MatchAllScorer(IndexReader reader,Similarity similarity){
			super(similarity);
			_reader=reader;
			_count=-1;
			_maxDoc=_reader.maxDoc();
		}
		
		public int doc() {		
			return _count;							
		}

		public Explanation explain(int doc) throws IOException {			
		    Explanation explanation = new Explanation();		    
			explanation.setValue(1.0f);
			explanation.setDescription("MatchAllDocsQuery");		    
		    return explanation;
		}

		public boolean next() throws IOException {
			while(_count < (_maxDoc-1)){
				_count++;
				if (!_reader.isDeleted(_count)){
					return true;
				}
			}
			return false;
		}

		public float score() throws IOException {
			return 1.0f;
		}

		public boolean skipTo(int target) throws IOException {
			_count=target;
			return next();
		}
		
	}
	  private class MatchAllDocsWeight implements Weight {
	    private Searcher searcher;	    

	    public MatchAllDocsWeight(Searcher searcher) {
	      this.searcher = searcher;
	    }

	    public String toString() { return "weight(" + MatchAllDocsQuery.this + ")"; }

	    public Query getQuery() { return MatchAllDocsQuery.this; }
	    public float getValue() { return 1.0f; }

	    public float sumOfSquaredWeights() throws IOException {
	      return 1.0f;
	    }

	    public void normalize(float queryNorm) {
	      
	    }

	    public Scorer scorer(IndexReader reader) throws IOException {
			return new MatchAllScorer(reader,getSimilarity(searcher));
	    }

	    public Explanation explain(IndexReader reader, int doc)
	      throws IOException {

	      // explain query weight
	      Explanation queryExpl = new Explanation();
	      queryExpl.setDescription("MatchAllDocsQuery:");

	      Explanation boostExpl = new Explanation(getBoost(), "boost");
	      if (getBoost() != 1.0f)
	        queryExpl.addDetail(boostExpl);	  
		  queryExpl.setValue(boostExpl.getValue());
	      	      	           	    
	      return queryExpl;
	    }
	  }
	  
	  protected Weight createWeight(Searcher searcher) {
	    return new MatchAllDocsWeight(searcher);
	  }

	  /** Prints a user-readable version of this query. */
	  public String toString(String field) {
	    StringBuffer buffer = new StringBuffer();	    
	    buffer.append("MatchAllDocsQuery");
	    if (getBoost() != 1.0f) {
	      buffer.append("^");
	      buffer.append(Float.toString(getBoost()));
	    }
	    return buffer.toString();
	  }

	  /** Returns true iff <code>o</code> is equal to this. */
	  public boolean equals(Object o) {
	    if (!(o instanceof MatchAllDocsQuery))
	      return false;
		MatchAllDocsQuery other = (MatchAllDocsQuery)o;
	    return this.getBoost() == other.getBoost();	      
	  }

	  /** Returns a hash code value for this object.*/
	  public int hashCode() {
	    return Float.floatToIntBits(getBoost());
	  }
}

