package org.apache.lucene.search;

import java.io.IOException;
import java.util.Comparator;
import java.util.PriorityQueue;

public class MultiTermScorer extends Scorer {
	
	private PriorityQueue<Scorer> queue;
	
	private int docNo = Integer.MAX_VALUE;
	
	private float score = 0.0F;


        private class TermScorerComparator implements Comparator<Scorer> {

	        public int compare(Scorer t0, Scorer t1) {
		        int doc0 = t0.doc();
		        int doc1 = t1.doc();
		        return doc0 - doc1;
	        }

        }

	public MultiTermScorer(Similarity similarity) {
		super(similarity);
		queue = new PriorityQueue<Scorer>(10, new TermScorerComparator());
	}
	
	public void add(Scorer scorer)
	{
		try {
			scorer.next();
		} catch (IOException e) {
			IllegalArgumentException exc = new IllegalArgumentException("IO error with term scorer");
			exc.initCause(e);
			throw exc;
		}
		queue.offer(scorer);
	}

	@Override
	public boolean next() throws IOException {
		score = 0.0F;
		if (queue.size() == 0) return false;
		docNo = queue.peek().doc();
		if (docNo == Integer.MAX_VALUE) return false;
		
		int matchCount = 0;
		while (queue.peek().doc() == docNo) {
			Scorer ts = queue.remove();
			//score += ts.score();
			score = Math.max(score, ts.score());
			matchCount++;
			ts.next();
			queue.offer(ts);
		}
		//score *= (float)matchCount/ (float)queue.size();
		return docNo != Integer.MAX_VALUE;
	}

	@Override
	public int doc() {
		return docNo;
	}

	// TODO - implement private hit collector to avoid removing/reinserting scorers on the queue
	@Override
	public float score() throws IOException {
		return score;
	}

	@Override
	public boolean skipTo(int target) throws IOException {
		if (queue.size() == 0 || queue.peek().doc() == Integer.MAX_VALUE) return false;
		
		while (queue.peek().doc() < target) {
			Scorer ts = queue.remove();
			ts.skipTo(target);
			queue.add(ts);
		}
		return next();
	}

	@Override
	public Explanation explain(int doc) throws IOException {
	    throw new UnsupportedOperationException();
	}

}
