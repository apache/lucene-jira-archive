package org.apache.lucene.search;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.index.IndexReader;


/**
 * A boolean query that only matches if a specified number of the contained clauses match. 
 * An example use might be a query that returns a list of books where ANY 2 people 
 * from a list of people were co-authors, eg:
 * "Lucene In Action" would match ("Erik Hatcher" "Otis Gospodneti?" "Mark Harwood" "Doug Cutting") 
 * with a minRequiredOverlap of 2 because Otis and Erik wrote that. 
 * The book "Java Development with Ant" would not match because only 1 element in the list (Erik) 
 * was selected.
 * @author maharwood
 */
public class CoordConstrainedBooleanQuery extends BooleanQuery
{
    static final int DEFAULT_MIN_REQUIRED_OVERLAP=1;
    int minRequiredOverlap;
 
    public CoordConstrainedBooleanQuery(int minRequiredOverlap)
    {
        this.minRequiredOverlap = minRequiredOverlap;
    }
    public int getMinRequiredOverlap()
    {
        return minRequiredOverlap;
    }
    public void setMinRequiredOverlap(int minRequiredOverlap)
    {
        this.minRequiredOverlap = minRequiredOverlap;
    }
    
    protected Weight createWeight(Searcher searcher) throws IOException
    {
        return new CoordConstrainedWeight(searcher);
    }
    
    private class CoordConstrainedWeight implements Weight {
        ArrayList weights=new ArrayList();
        
        private BooleanClause[] clauses;

        private Similarity similarity;

        public CoordConstrainedWeight(Searcher searcher)
          throws IOException {
            this.similarity = getSimilarity(searcher);
            clauses=getClauses();
            for (int i = 0 ; i < clauses.length; i++) {
                weights.add(clauses[i].getQuery().createWeight(searcher));
              }
        }

        /** Use the DisjunctionSumScorer feature that allows definition of number of matchers
         */
        public Scorer scorer(IndexReader reader) throws IOException {            
            ArrayList scorers=new ArrayList();
            for (int i = 0 ; i < clauses.length; i++) 
            {
	            Weight w = (Weight)weights.get(i);
	            Scorer subScorer = w.scorer(reader);
	            if (subScorer != null)
	              scorers.add(subScorer);
            }
            return new DisjunctionSumScorer(scorers,minRequiredOverlap);
        }

        public Query getQuery()
        {
            return CoordConstrainedBooleanQuery.this;
        }

        public float getValue()
        {
            return getBoost();
        }

        public float sumOfSquaredWeights() throws IOException
        {
            float sum = 0.0f;
            for (int i = 0 ; i < weights.size(); i++) {
              Weight w = (Weight)weights.get(i);
              if (!clauses[i].isProhibited())
                sum += w.sumOfSquaredWeights();         // sum sub weights
            }

            sum *= getBoost() * getBoost();             // boost each sub-weight

            return sum ;
        }

        public void normalize(float norm)
        {
            norm *= getBoost();                         // incorporate boost
            for (int i = 0 ; i < weights.size(); i++) {
              Weight w = (Weight)weights.get(i);
              if (!clauses[i].isProhibited())
                w.normalize(norm);
            }
        }
        

        public Explanation explain(IndexReader reader, int doc) throws IOException
        {
            //TODO this was copied from BooleanWeight and probably needs some changes
            Explanation sumExpl = new Explanation();
            sumExpl.setDescription("sum of:");
            int coord = 0;
            int maxCoord = 0;
            float sum = 0.0f;
            for (int i = 0 ; i < weights.size(); i++) {
              Weight w = (Weight)weights.get(i);
              Explanation e = w.explain(reader, doc);
              if (!clauses[i].isProhibited()) maxCoord++;
              if (e.getValue() > 0) {
                if (!clauses[i].isProhibited()) {
                  sumExpl.addDetail(e);
                  sum += e.getValue();
                  coord++;
                } else {
                  return new Explanation(0.0f, "match prohibited");
                }
              } else if (clauses[i].isRequired()) {
                return new Explanation(0.0f, "match required");
              }
            }
            sumExpl.setValue(sum);

            if (coord == 1)                               // only one clause matched
              sumExpl = sumExpl.getDetails()[0];          // eliminate wrapper

            float coordFactor = similarity.coord(coord, maxCoord);
            if (coordFactor == 1.0f)                      // coord is no-op
              return sumExpl;                             // eliminate wrapper
            else {
              Explanation result = new Explanation();
              result.setDescription("product of:");
              result.addDetail(sumExpl);
              result.addDetail(new Explanation(coordFactor,
                                               "coord("+coord+"/"+maxCoord+")"));
              result.setValue(sum*coordFactor);
              return result;
            }
        }
      }
    
}
