package com.friend.find.lucene;

import java.io.IOException;
import java.util.TreeMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;

public class DisjointMultiFilter {
		/** This uses an int for the term id. If there are less terms, a byte or short would
		 *  save space
		 */
		
		private String fieldName;
		private int[] docContents;
		private TreeMap<String,Integer> termNumbers;
		
		public DisjointMultiFilter(IndexReader reader, String fieldName) throws IOException {
			this.fieldName = fieldName.intern();
			initialize(reader);
		}
		
		public void initialize(IndexReader reader) throws IOException {
			docContents = new int[reader.maxDoc()];
			termNumbers = new TreeMap<String,Integer>();
			// 0 is never actually used; it represents a non-matching field.
			int termNumber = 0;
			//System.out.println("Numdocs: " +reader.maxDoc());
			TermEnum enumerator = reader.terms(new Term(fieldName,""));
			try {
				if (enumerator.term() == null)
					return;

				TermDocs termDocs = reader.termDocs();
				try {
					do {
						Term term = enumerator.term();
						if (!term.field().equals(fieldName))
							break;
						termNumbers.put(term.text(),++termNumber);
						//System.out.println(fieldName + ": Adding term " + term.text() + " number " + termNumber);
						int count = 0;
						if (term != null && term.field().equals(fieldName)) {
							termDocs.seek(term);
							while (termDocs.next()) {
								docContents[termDocs.doc()] = termNumber;
								count++;
							}							
						}
						//System.out.println(fieldName + ": Added term " + term.text() + " docs " + count);
					} while (enumerator.next());
				} finally {
					termDocs.close();
				}
			} finally {
				enumerator.close();
			}
		}
		
		public String getField() {
			return fieldName;
		}
		public int[] getUnderlyingArray() {
			return docContents;
		}
		
		// most efficient in a loop; 
		public boolean matchesDocument(int doc, int termNumber) {
			return (docContents[doc] == termNumber);
		}
		public int getTermNumber(String term) {
			Integer termNumber = termNumbers.get(term);
			if (termNumber != null)
				return termNumber;
			return 0;
		}
		public String getCeilingTerm(String term) {
			return termNumbers.ceilingKey(term);
		}
		public String getFloorTerm(String term) {
			return termNumbers.floorKey(term);
		}
		public String getHigherTerm(String term) {
			return termNumbers.higherKey(term);
		}
		public String getLowerTerm(String term) {
			return termNumbers.lowerKey(term);
		}
		
		// much less efficient.
		public boolean matchesDocument(int doc, String term) {
			int termNumber = getTermNumber(term);
			if (termNumber == 0) {
				return false;
			}
			return matchesDocument(doc,termNumber);
		}
		public int nextDocument(int doc, int termNumber) {
			try {
				while (docContents[doc] != termNumber) {
					doc++;
				}
				return doc;
			} catch (ArrayIndexOutOfBoundsException e) {
				return -1;
			}
			
		}
	}

