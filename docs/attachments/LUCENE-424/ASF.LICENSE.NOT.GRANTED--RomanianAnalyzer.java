package ro.dazoot.indexserver.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WordlistLoader;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

/**
* Analyzer for Romanian language. Supports an external list of stopwords (words that
* will not be indexed at all) and an external list of exclusions (word that will
* not be stemmed, but indexed).
* A default set of stopwords is used unless an alternative list is specified, the
* exclusion list is empty by default.
*
* @author Tiba Daniela
* @author Catalin Constantin
* @version $Id: RomanianAnalyzer.java,v 1.2 2005/08/17 09:38:23 catalin Exp $
*/
public class RomanianAnalyzer extends Analyzer {

	/**
	* List of typical romanian stopwords.
	*/
	public static final String[] ROMANIAN_STOP_WORDS = {
	    "si", "de", "ia", "la", "in", "sau", "cu", "lui", "ce",
	    "cum", "cand", "care", "unde", "cine", "sub",
	    "nu", "da", "daca", "doar", "etc", "i", "ca",
	    "fa", "e", "este", "aici", "ii", "ceva", "ori",
	    "curand", "deci", "fara", "inca", "ei", "face",
		"a", "ro", "http", "www", "o", "pentru", "pe", "sa",
		"un", "s", "va", "al", "prin", "ai", "te", "ne", "are", "l",
		"the", "on", "noi"
  	  };

	/**
	* Contains the stopwords used with the StopFilter.
	*/
	private Set stopSet = new HashSet();
	
	/**
	* Contains words that should be indexed but not stemmed.
	*/
	private Set exclusionSet = new HashSet();
	
	/**
	* Builds an analyzer with the default stop words
	* (<code>ROMANIAN_STOP_WORDS</code>).
	*/
	public RomanianAnalyzer() {
	 stopSet = StopFilter.makeStopSet(ROMANIAN_STOP_WORDS);
	}
	
	/**
	* Builds an analyzer with the given stop words.
	*/
	public RomanianAnalyzer(String[] stopwords) {
	 stopSet = StopFilter.makeStopSet(stopwords);
	}
	
	/**
	* Builds an analyzer with the given stop words.
	*/
	public RomanianAnalyzer(Hashtable stopwords) {
	 stopSet = new HashSet(stopwords.keySet());
	}
	
	/**
	* Builds an analyzer with the given stop words.
	*/
	public RomanianAnalyzer(File stopwords) throws IOException {
	 stopSet = WordlistLoader.getWordSet(stopwords);
	}

	/**
	* Builds an exclusionlist from an array of Strings.
	*/
	public void setStemExclusionTable(String[] exclusionlist) {
	 exclusionSet = StopFilter.makeStopSet(exclusionlist);
	}
	
	/**
	* Builds an exclusionlist from a Hashtable.
	*/
	public void setStemExclusionTable(Hashtable exclusionlist) {
	 exclusionSet = new HashSet(exclusionlist.keySet());
	}
	
	/**
	* Builds an exclusionlist from the words contained in the given file.
	*/
	public void setStemExclusionTable(File exclusionlist) throws IOException {
	 exclusionSet = WordlistLoader.getWordSet(exclusionlist);
	}
	
	/**
	* Creates a TokenStream which tokenizes all the text in the provided Reader.
	*
	* @return A TokenStream build from a StandardTokenizer filtered with
	*         StandardFilter, LowerCaseFilter, StopFilter, GermanStemFilter
	*/
	public TokenStream tokenStream(String fieldName, Reader reader) {
	 TokenStream result = new StandardTokenizer(reader);
	 result = new StandardFilter(result);
	 result = new LowerCaseFilter(result);
	 result = new RomanianStemFilter(result, exclusionSet);
	 result = new StopFilter(result, stopSet);
	 return result;
	}
}
