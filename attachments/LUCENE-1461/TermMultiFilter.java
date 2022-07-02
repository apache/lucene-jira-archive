package com.friend.find.lucene;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;

public class TermMultiFilter extends Filter {
	private Term term;
	private DisjointMultiFilter dmf;
	private int termNumber = 0;
	
	public TermMultiFilter(DisjointMultiFilter dmf, Term term) {
		if (term.field() == null || !term.field().equals(dmf.getField())) {
			throw new IllegalArgumentException("Field " + term.field() + " in term must match field " + dmf.getField() + " in filter");
		}
		this.term = term;
		this.dmf = dmf;
		this.termNumber = dmf.getTermNumber(term.text());
	}
	
	public DocIdSet getDocIdSet(IndexReader reader) {
		if (termNumber == 0) {
			// if the term does not exist in the index there are no results.
			return new TermMultiFilterEmptyDocIdSet();
		} else {
			return new TermMultiFilterDocIdSet();
		}
	}
	
	protected class TermMultiFilterEmptyDocIdSet extends DocIdSet {
		public DocIdSetIterator iterator() {
			return new TermMultiFilterEmptyIterator();
		}		
	}
	
	protected class TermMultiFilterEmptyIterator extends DocIdSetIterator {
		private int doc = Integer.MAX_VALUE;
		public int doc() {
			return doc;
		}
		public boolean next() {
			return false;
		}
		public boolean skipTo(int target) {
			return false;
		}
	}
	
	protected class TermMultiFilterDocIdSet extends DocIdSet {
		public DocIdSetIterator iterator() {
			return new TermMultiFilterIterator();
		}
	}
	
	protected class TermMultiFilterIterator extends DocIdSetIterator {
		private int doc = -1;
		public int doc() {
			return doc;
		}
		public boolean next() {
			doc = dmf.nextDocument(doc+1, termNumber);
			if (doc == -1) {
				doc = Integer.MAX_VALUE;
				return false;
			} else {
				return true;
			}
		}
		public boolean skipTo(int target) {
			doc = dmf.nextDocument(target, termNumber);
			if (doc == -1) {
				doc = Integer.MAX_VALUE;
				return false;
			} else {
				return true;
			}
		}
	}
}
