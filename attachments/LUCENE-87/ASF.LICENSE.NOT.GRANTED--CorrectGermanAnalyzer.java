import org.apache.lucene.analysis.de.GermanStemFilter;
import org.apache.lucene.analysis.de.WordlistLoader;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;

import java.io.Reader;
import java.io.File;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: meister
 * Date: 27.03.2003
 * Time: 15:26:55
 * To change this template use Options | File Templates.
 */
public class CorrectGermanAnalyzer extends Analyzer {

    /**
     * List of typical german stopwords.
     */
    private String[] GERMAN_STOP_WORDS = {
        "einer", "eine", "eines", "einem", "einen",
        "der", "die", "das", "dass", "daß",
        "du", "er", "sie", "es",
        "was", "wer", "wie", "wir",
        "und", "oder", "ohne", "mit",
        "am", "im", "in", "aus", "auf",
        "ist", "sein", "war", "wird",
        "ihr", "ihre", "ihres",
        "als", "für", "von", "mit",
        "dich", "dir", "mich", "mir",
        "mein", "sein", "kein",
        "durch", "wegen", "wird"
    };

    /**
     * Contains the stopwords used with the StopFilter.
     */
    private Hashtable stoptable = new Hashtable();

    /**
     * Contains words that should be indexed but not stemmed.
     */
    private Hashtable excltable = new Hashtable();

    /**
     * Builds an analyzer.
     */
    public CorrectGermanAnalyzer()
    {
        stoptable = StopFilter.makeStopTable( GERMAN_STOP_WORDS );
    }

    /**
     * Builds an analyzer with the given stop words.
     */
    public CorrectGermanAnalyzer( String[] stopwords )
    {
        stoptable = StopFilter.makeStopTable( stopwords );
    }

    /**
     * Builds an analyzer with the given stop words.
     */
    public CorrectGermanAnalyzer( Hashtable stopwords )
    {
        stoptable = stopwords;
    }

    /**
     * Builds an analyzer with the given stop words.
     */
    public CorrectGermanAnalyzer( File stopwords )
    {
        stoptable = WordlistLoader.getWordtable( stopwords );
    }

    /**
     * Builds an exclusionlist from an array of Strings.
     */
    public void setStemExclusionTable( String[] exclusionlist )
    {
        excltable = StopFilter.makeStopTable( exclusionlist );
    }

    /**
     * Builds an exclusionlist from a Hashtable.
     */
    public void setStemExclusionTable( Hashtable exclusionlist )
    {
        excltable = exclusionlist;
    }

    /**
     * Builds an exclusionlist from the words contained in the given file.
     */
    public void setStemExclusionTable( File exclusionlist )
    {
        excltable = WordlistLoader.getWordtable( exclusionlist );
    }

    /**
     * Creates a TokenStream which tokenizes all the text in the provided Reader.
     *
     * @return  A TokenStream build from a StandardTokenizer filtered with
     *		StandardFilter, StopFilter, GermanStemFilter
     */
    public TokenStream tokenStream( String fieldName, Reader reader )
    {
        TokenStream result = new StandardTokenizer( reader );
        result = new StandardFilter( result );
        result = new StopFilter( result, stoptable );
        result = new GermanStemFilter( result, excltable );
        ((GermanStemFilter)result).setStemmer(new CorrectGermanStemmer());
        return result;
    }
}
