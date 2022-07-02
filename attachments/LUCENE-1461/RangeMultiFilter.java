package com.friend.find.lucene;

import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.Filter;

public class RangeMultiFilter extends Filter {
	private String field;
	private String lowerVal;
	private String upperVal;
	private boolean includeLower;
	private boolean includeUpper;

	public RangeMultiFilter(String field, String lowerVal, String upperVal, boolean includeLower, boolean includeUpper) {
		this.field = field;
		this.lowerVal = lowerVal;
		this.upperVal = upperVal;
		this.includeLower = includeLower;
		this.includeUpper = includeUpper;


	}

	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
		// should this initialize the DMF on demand?
		return new RangeMultiFilterDocIdSet(FieldCache.DEFAULT.getStringIndex(reader, field));
	}

	protected class RangeMultiFilterDocIdSet extends DocIdSet {
		private int inclusiveLowerPoint;
		private int inclusiveUpperPoint;
		private FieldCache.StringIndex fcsi;

		public RangeMultiFilterDocIdSet(FieldCache.StringIndex fcsi) {
			this.fcsi = fcsi;
			initialize();
		}
		
		private void initialize() {
			int lowerPoint = Arrays.binarySearch(fcsi.lookup, lowerVal);
			
			if (includeLower && lowerPoint >= 0) {
				inclusiveLowerPoint = lowerPoint;
			} else if (lowerPoint >= 0) {
				inclusiveLowerPoint = lowerPoint+1;
			} else {
				inclusiveLowerPoint = -lowerPoint-1;
			}

			int upperPoint = Arrays.binarySearch(fcsi.lookup, upperVal);
			if (includeUpper && upperPoint >= 0) {
				inclusiveUpperPoint = upperPoint;
			} else if (upperPoint >= 0) {
				inclusiveUpperPoint = upperPoint - 1;
			} else {
				inclusiveUpperPoint = -upperPoint - 2;
			}
		}
		
		public DocIdSetIterator iterator() {
			return new RangeMultiFilterIterator();
		}

		protected class RangeMultiFilterIterator extends DocIdSetIterator {
			private int doc = -1;

			public int doc() {
				return doc;
			}
			public boolean next() {
				try {
					do {
						doc++;
					} while (fcsi.order[doc] > inclusiveUpperPoint || fcsi.order[doc] < inclusiveLowerPoint);
					return true;
				} catch (ArrayIndexOutOfBoundsException e) {
					doc = Integer.MAX_VALUE;
					return false;
				}
			}
			public boolean skipTo(int target) {
				try {
					doc = target;
					while (fcsi.order[doc] > inclusiveUpperPoint || fcsi.order[doc] < inclusiveLowerPoint) { 
						doc++;
					}
					return true;
				} catch (ArrayIndexOutOfBoundsException e) {
					doc = Integer.MAX_VALUE;
					return false;
				}
			}
		}
	}
}
