import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.search.ComplexExplanation;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.payloads.PayloadFunction;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.Similarity.SimScorer;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanScorer;
import org.apache.lucene.search.spans.SpanWeight;
import org.apache.lucene.search.spans.TermSpans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;

public class PayloadSpanOrQuery extends SpanOrQuery {

    PayloadFunction function;
    boolean includeSpanScore = true;

    public PayloadSpanOrQuery(PayloadFunction function, boolean includeSpanScore) {
        super();
        this.function = function;
        this.includeSpanScore = includeSpanScore;
    }

    @Override
    public Weight createWeight(IndexSearcher searcher) throws IOException {
        return new PayloadTermWeight(this, searcher);
    }

    protected class PayloadTermWeight extends SpanWeight {

        public PayloadTermWeight(PayloadSpanOrQuery query, IndexSearcher searcher)
                throws IOException {
            super(query, searcher);
        }

        @Override
        public Scorer scorer(AtomicReaderContext context, Bits acceptDocs) throws IOException {
            return new PayloadTermSpanScorer((TermSpans) query.getSpans(context, acceptDocs, termContexts),
                    this, similarity.simScorer(stats, context));
        }

        protected class PayloadTermSpanScorer extends SpanScorer {
            protected BytesRef payload;
            protected float payloadScore;
            protected int payloadsSeen;
            private final TermSpans termSpans;

            public PayloadTermSpanScorer(TermSpans spans, Weight weight, Similarity.SimScorer docScorer) throws IOException {
                super(spans, weight, docScorer);
                termSpans = spans;
            }

            @Override
            protected boolean setFreqCurrentDoc() throws IOException {
                if (!more) {
                    return false;
                }
                doc = spans.doc();
                freq = 0.0f;
                numMatches = 0;
                payloadScore = 0;
                payloadsSeen = 0;
                while (more && doc == spans.doc()) {
                    int matchLength = spans.end() - spans.start();

                    freq += docScorer.computeSlopFactor(matchLength);
                    numMatches++;
                    processPayload(similarity);

                    more = spans.next();// this moves positions to the next match in this
                    // document
                }
                return more || (freq != 0);
            }

            protected void processPayload(Similarity similarity) throws IOException {
                if (termSpans.isPayloadAvailable()) {
                    final DocsAndPositionsEnum postings = termSpans.getPostings();
                    payload = postings.getPayload();
                    if (payload != null) {
                        payloadScore = function.currentScore(doc, getField(),
                                spans.start(), spans.end(), payloadsSeen, payloadScore,
                                docScorer.computePayloadFactor(doc, spans.start(), spans.end(), payload));
                    } else {
                        payloadScore = function.currentScore(doc, getField(),
                                spans.start(), spans.end(), payloadsSeen, payloadScore, 1F);
                    }
                    payloadsSeen++;

                } else {
                    // zero out the payload?
                }
            }

            /**
             *
             * @return {@link #getSpanScore()} * {@link #getPayloadScore()}
             * @throws IOException if there is a low-level I/O error
             */
            @Override
            public float score() throws IOException {

                return includeSpanScore ? getSpanScore() * getPayloadScore()
                        : getPayloadScore();
            }

            /**
             * Returns the SpanScorer score only.
             * <p/>
             * Should not be overridden without good cause!
             *
             * @return the score for just the Span part w/o the payload
             * @throws IOException if there is a low-level I/O error
             *
             * @see #score()
             */
            protected float getSpanScore() throws IOException {
                return super.score();
            }

            /**
             * The score for the payload
             *
             * @return The score, as calculated by
             *         {@link PayloadFunction#docScore(int, String, int, float)}
             */
            protected float getPayloadScore() {
                return function.docScore(doc, getField(), payloadsSeen, payloadScore);
            }
        }

        @Override
        public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
            PayloadTermSpanScorer scorer = (PayloadTermSpanScorer) scorer(context, context.reader().getLiveDocs());
            if (scorer != null) {
                int newDoc = scorer.advance(doc);
                if (newDoc == doc) {
                    float freq = scorer.sloppyFreq();
                    SimScorer docScorer = similarity.simScorer(stats, context);
                    Explanation expl = new Explanation();
                    expl.setDescription("weight("+getQuery()+" in "+doc+") [" + similarity.getClass().getSimpleName() + "], result of:");
                    Explanation scoreExplanation = docScorer.explain(doc, new Explanation(freq, "phraseFreq=" + freq));
                    expl.addDetail(scoreExplanation);
                    expl.setValue(scoreExplanation.getValue());
                    // now the payloads part
                    // QUESTION: Is there a way to avoid this skipTo call? We need to know
                    // whether to load the payload or not
                    // GSI: I suppose we could toString the payload, but I don't think that
                    // would be a good idea
                    String field = ((SpanQuery)getQuery()).getField();
                    Explanation payloadExpl = function.explain(doc, field, scorer.payloadsSeen, scorer.payloadScore);
                    payloadExpl.setValue(scorer.getPayloadScore());
                    // combined
                    ComplexExplanation result = new ComplexExplanation();
                    if (includeSpanScore) {
                        result.addDetail(expl);
                        result.addDetail(payloadExpl);
                        result.setValue(expl.getValue() * payloadExpl.getValue());
                        result.setDescription("btq, product of:");
                    } else {
                        result.addDetail(payloadExpl);
                        result.setValue(payloadExpl.getValue());
                        result.setDescription("btq(includeSpanScore=false), result of:");
                    }
                    result.setMatch(true); // LUCENE-1303
                    return result;
                }
            }

            return new ComplexExplanation(false, 0.0f, "no matching term");
        }
    }
}
