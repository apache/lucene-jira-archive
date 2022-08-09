package org.apache.lucene.search;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

import java.io.IOException;
import java.util.Set;

/**
 */
public class DeletionAwareConstantScoreQuery extends Query {
    protected final Filter filter;
    protected final boolean deletionAware;

    public DeletionAwareConstantScoreQuery(Filter filter) {
        this(filter, false);
    }

    public DeletionAwareConstantScoreQuery(Filter filter, boolean deletionAware) {
        this.filter = filter;
        this.deletionAware = deletionAware;
    }

    /**
     * Returns the encapsulated filter
     */
    public Filter getFilter() {
        return filter;
    }

    @Override
    public Query rewrite(IndexReader reader) throws IOException {
        return this;
    }

    @Override
    public void extractTerms(Set<Term> terms) {
        // OK to not add any terms when used for MultiSearcher,
        // but may not be OK for highlighting
    }

    protected class ConstantWeight extends Weight {
        private Similarity similarity;
        private float queryNorm;
        private float queryWeight;

        public ConstantWeight(Searcher searcher) {
            this.similarity = getSimilarity(searcher);
        }

        @Override
        public Query getQuery() {
            return DeletionAwareConstantScoreQuery.this;
        }

        @Override
        public float getValue() {
            return queryWeight;
        }

        @Override
        public float sumOfSquaredWeights() throws IOException {
            queryWeight = getBoost();
            return queryWeight * queryWeight;
        }

        @Override
        public void normalize(float norm) {
            this.queryNorm = norm;
            queryWeight *= this.queryNorm;
        }

        @Override
        public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
            return new ConstantScorer(similarity, reader, this);
        }

        @Override
        public Explanation explain(IndexReader reader, int doc) throws IOException {

            ConstantScorer cs = new ConstantScorer(similarity, reader, this);
            boolean exists = cs.docIdSetIterator.advance(doc) == doc;

            ComplexExplanation result = new ComplexExplanation();

            if (exists) {
                result.setDescription("ConstantScoreQuery(" + filter
                        + "), product of:");
                result.setValue(queryWeight);
                result.setMatch(Boolean.TRUE);
                result.addDetail(new Explanation(getBoost(), "boost"));
                result.addDetail(new Explanation(queryNorm, "queryNorm"));
            } else {
                result.setDescription("ConstantScoreQuery(" + filter
                        + ") doesn't match id " + doc);
                result.setValue(0);
                result.setMatch(Boolean.FALSE);
            }
            return result;
        }
    }

    protected class ConstantScorer extends Scorer {
        final IndexReader reader;
        final DocIdSetIterator docIdSetIterator;
        final float theScore;

        public ConstantScorer(Similarity similarity, IndexReader reader, Weight w) throws IOException {
            super(similarity);
            this.reader = reader;
            theScore = w.getValue();
            DocIdSet docIdSet = filter.getDocIdSet(reader);
            if (docIdSet == null) {
                docIdSetIterator = DocIdSet.EMPTY_DOCIDSET.iterator();
            } else {
                DocIdSetIterator iter = docIdSet.iterator();
                if (iter == null) {
                    docIdSetIterator = DocIdSet.EMPTY_DOCIDSET.iterator();
                } else {
                    docIdSetIterator = iter;
                }
            }
        }

        @Override
        public int nextDoc() throws IOException {
            if (deletionAware) {
                int nextDoc;
                while ((nextDoc = docIdSetIterator.nextDoc()) != NO_MORE_DOCS) {
                    if (!reader.isDeleted(nextDoc)) {
                        return nextDoc;
                    }
                }
                return nextDoc;
            } else {
                return docIdSetIterator.nextDoc();
            }
        }

        @Override
        public int docID() {
            return docIdSetIterator.docID();
        }

        @Override
        public float score() throws IOException {
            return theScore;
        }

        @Override
        public int advance(int target) throws IOException {
            if (deletionAware) {
                int doc = docIdSetIterator.advance(target);
                if (doc == NO_MORE_DOCS) {
                    return doc;
                }
                if (!reader.isDeleted(doc)) {
                    return doc;
                }
                while ((doc = nextDoc()) < target) {
                    if (!reader.isDeleted(doc)) {
                        return doc;
                    }
                }
                return doc;
            } else {
                return docIdSetIterator.advance(target);
            }
        }
    }

    @Override
    public Weight createWeight(Searcher searcher) {
        return new DeletionAwareConstantScoreQuery.ConstantWeight(searcher);
    }

    /**
     * Prints a user-readable version of this query.
     */
    @Override
    public String toString(String field) {
        return "ConstantScore(" + filter.toString()
                + (getBoost() == 1.0 ? ")" : "^" + getBoost());
    }

    /**
     * Returns true if <code>o</code> is equal to this.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConstantScoreQuery)) return false;
        ConstantScoreQuery other = (ConstantScoreQuery) o;
        return this.getBoost() == other.getBoost() && filter.equals(other.filter);
    }

    /**
     * Returns a hash code value for this object.
     */
    @Override
    public int hashCode() {
        // Simple add is OK since no existing filter hashcode has a float component.
        return filter.hashCode() + Float.floatToIntBits(getBoost());
    }

}

