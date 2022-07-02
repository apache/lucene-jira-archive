package org.apache.lucene.search;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.spell.Dictionary;
import org.apache.lucene.search.spell.LuceneDictionary;

import fuzzy.BKTree;
import fuzzy.DistanceImpl;

/**
 * Modified class from Lucene core 3.0 Includes BKTree-based processing
 * 
 * TODO: BKTree is staticaly cached on first call; if you need to update index
 * you will have to restart application
 * 
 * TODO: consider integer parameter to QueryParser instead of float.
 * 
 * @author Fuad Efendi
 * 
 */
public class FuzzyTermEnumNEW extends FilteredTermEnum {

	static Map<String, BKTree<String>> cache = new TreeMap<String, BKTree<String>>();

	private Term searchTerm = null;
	private final String field;

	Map<String, Integer> termMap;
	Iterator<String> termIterator;

	/** the current term */
	protected Term currentTerm = null;

	private final float minimumSimilarity;

	@Override
	public boolean next() throws IOException {
		if (!termIterator.hasNext()) {
			currentTerm = null;
			return false;
		}

		String termString = termIterator.next();
		Term term = new Term(field, termString);
		currentTerm = term;
		return true;
	}

	@Override
	public Term term() {
		return currentTerm;
	}

	/**
	 * Constructor for enumeration of all terms from specified
	 * <code>reader</code> which share a prefix of length
	 * <code>prefixLength</code> with <code>term</code> and which have a
	 * fuzzy similarity &gt; <code>minSimilarity</code>.
	 * <p>
	 * After calling the constructor the enumeration is already pointing to the
	 * first valid term if such a term exists.
	 * 
	 * @param reader
	 *            Delivers terms.
	 * @param term
	 *            Pattern term.
	 * @param minSimilarity
	 *            Minimum required similarity for terms from the reader. Default
	 *            value is 0.5f.
	 * @param prefixLength
	 *            Length of required common prefix. Default value is 0.
	 * @throws IOException
	 */
	public FuzzyTermEnumNEW(IndexReader reader, Term term, float minSimilarity, int prefixLength) throws IOException {
		super();

		if (minSimilarity >= 1.0f)
			throw new IllegalArgumentException("minimumSimilarity cannot be greater than or equal to 1");
		else if (minSimilarity < 0.0f)
			throw new IllegalArgumentException("minimumSimilarity cannot be less than 0");
		if (prefixLength < 0)
			throw new IllegalArgumentException("prefixLength cannot be less than 0");

		this.minimumSimilarity = minSimilarity;

		this.searchTerm = term;
		this.field = searchTerm.field();

		// TODO: Not the best way...
		BKTree<String> bkTree = cache.get(field);
		if (bkTree == null) {
			synchronized (this) {
				bkTree = new BKTree<String>(new DistanceImpl());
				Dictionary dictionary = new LuceneDictionary(reader, field);
				Iterator<String> iterator = dictionary.getWordsIterator();
				while (iterator.hasNext()) {
					bkTree.add(iterator.next());
				}
				cache.put(field, bkTree);
			}
		}

		int searchTextLength = searchTerm.text().length();
		float threshold = (1 - minimumSimilarity) * searchTextLength;
		int t =  (int) threshold;
		//if (t>2) {
		//	System.out.println ("t > 2!");
		//	t=2;
		//}
		this.termMap = bkTree.query(term.text(), t);
		this.termIterator = termMap.keySet().iterator();

	}

	/**
	 * The termCompare method in FuzzyTermEnum uses Levenshtein distance to
	 * calculate the distance between the given term and the comparing term.
	 */
	@Override
	protected final boolean termCompare(Term term) {
		return true;
	}

	public final float difference() {

		int score = termMap.get(currentTerm.text());

		if (score == 0)
			return 1.0f;
		else
			return (float) (1 / (float) score);

	}

	public final boolean endEnum() {
		return !termIterator.hasNext();
	}

	public void close() throws IOException {
		termIterator = null;
		// call super.close() and let the garbage collector do its work.
		super.close();
	}

}
