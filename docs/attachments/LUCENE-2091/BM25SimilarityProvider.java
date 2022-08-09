package org.apache.lucene.search.similarity.dfr;

import java.io.IOException;

import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Stats.DocFieldStats;
import org.apache.lucene.search.DefaultSimilarityProvider;
import org.apache.lucene.search.similarity.AggregatesProvider;
import org.apache.lucene.util.BytesRef;

/**
 * BM25*
 */
public class BM25SimilarityProvider extends DefaultSimilarityProvider {
  private final float k1;
  private final float b;
  private final AggregatesProvider aggs;
  
  public BM25SimilarityProvider(AggregatesProvider aggs, float k1, float b) {
    this.aggs = aggs;
    this.k1 = k1;
    this.b = b;
  }
  
  public BM25SimilarityProvider(AggregatesProvider aggs) {
    this(aggs, 2.0F, 0.75F);
  }

  private class BM25TermScorer extends BoostBytesFieldDocScorer {
    private final float queryWeight;
    private final DocsEnum docsEnum;
    public BM25TermScorer(IndexReader segment, float queryWeight,
        TermAndPostings termAndPostings, final BM25FieldSimilarity fieldSim) throws IOException {
      super(segment, queryWeight, termAndPostings, fieldSim,
          new ComputesLengthNorm() {
            @Override
            protected float lengthNorm(int docID, DocFieldStats stats) {
              // nocommit: incorporate doc boosting into this
              float norm = k1 * ((1 - b) + b * (stats.termCount) / (fieldSim.avgTermLength));
              return norm;
            }
          });
      this.queryWeight = queryWeight * fieldSim.idf(termAndPostings.term);
      this.docsEnum = termAndPostings.docsEnum;
    }
    
    @Override
    public float score() {
      final float tf = docsEnum.freq();
      return queryWeight * tf / (tf + getDocBoost(docsEnum.docID()));
    }
  }

  public class BM25FieldSimilarity extends DefaultFieldSimilarity {
    final IndexReader topReader;
    final float avgTermLength;

    public BM25FieldSimilarity(IndexReader topReader, String field, double avgTermLength) throws IOException {
      super(topReader, field);
      this.topReader = topReader;
      this.avgTermLength = (float) avgTermLength;
    }
    
    @Override
    public FieldDocScorer getTermScorer(float queryWeight, TermAndPostings termsAndPostings, IndexReader segment)
        throws IOException {
      return new BM25TermScorer(segment, queryWeight, termsAndPostings, this);
    }

    @Override
    public float idf(BytesRef term) throws IOException {
      final float dfj = topReader.docFreq(field, term);
      final float n = topReader.maxDoc();
      return (float) Math.log(1 + ((n - dfj + 0.5F)/(dfj + 0.5F)));
    }
  }

  @Override
  public FieldSimilarity getField(IndexReader topReader, String field) throws IOException {
    return new BM25FieldSimilarity(topReader, field, aggs.getAvgTermLength(topReader, field));
  }
}
