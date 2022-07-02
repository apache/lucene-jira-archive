package sl;


import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ar.ArabicNormalizationFilter;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.Reader;

public final class SinhalaAnalyzer extends StopwordAnalyzerBase {

    /**
     * File containing default Arabic stopwords.
     *
     * Default stopword list is from http://members.unine.ch/jacques.savoy/clef/index.html
     * The stopword list is BSD-Licensed.
     */
    public final static String DEFAULT_STOPWORD_FILE = "stopwords.txt";

    /**
     * Returns an unmodifiable instance of the default stop-words set.
     * @return an unmodifiable instance of the default stop-words set.
     */
    public static CharArraySet getDefaultStopSet(){
        return DefaultSetHolder.DEFAULT_STOP_SET;
    }

    /**
     * Atomically loads the DEFAULT_STOP_SET in a lazy fashion once the outer class
     * accesses the static final set the first time.;
     */
    private static class DefaultSetHolder {
        static final CharArraySet DEFAULT_STOP_SET;

        static {
            try {
                DEFAULT_STOP_SET = loadStopwordSet(false, SinhalaAnalyzer.class, DEFAULT_STOPWORD_FILE, "#");
            } catch (IOException ex) {
                // default set should always be present as it is part of the
                // distribution (JAR)
                throw new RuntimeException("Unable to load default stopword set");
            }
        }
    }

    private final CharArraySet stemExclusionSet;

    /**
     * Builds an analyzer with the default stop words: {@link #DEFAULT_STOPWORD_FILE}.
     */
    public SinhalaAnalyzer(Version matchVersion) {
        this(matchVersion, DefaultSetHolder.DEFAULT_STOP_SET);
    }

    /**
     * Builds an analyzer with the given stop words
     *
     * @param matchVersion
     *          lucene compatibility version
     * @param stopwords
     *          a stopword set
     */
    public SinhalaAnalyzer(Version matchVersion, CharArraySet stopwords){
        this(matchVersion, stopwords, CharArraySet.EMPTY_SET);
    }

    /**
     * Builds an analyzer with the given stop word. If a none-empty stem exclusion set is
     * provided this analyzer will add a {@link KeywordMarkerFilter} before
     * {@link SinhalaAnalyzer}.
     *
     * @param matchVersion
     *          lucene compatibility version
     * @param stopwords
     *          a stopword set
     * @param stemExclusionSet
     *          a set of terms not to be stemmed
     */
    public SinhalaAnalyzer(Version matchVersion, CharArraySet stopwords, CharArraySet stemExclusionSet){
        super(matchVersion, stopwords);
        this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(
                matchVersion, stemExclusionSet));
    }

    /**
     * Creates
     * {@link TokenStreamComponents}
     * used to tokenize all the text in the provided {@link Reader}.
     *
     * @return {@link TokenStreamComponents}
     *         built from an {@link StandardTokenizer} filtered with
     *         {@link LowerCaseFilter}, {@link StopFilter},
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName,
                                                              Reader reader) {
        final Tokenizer source = matchVersion.onOrAfter(Version.LUCENE_31) ?
                new StandardTokenizer(matchVersion, reader) : new StandardTokenizer(matchVersion, reader);
        TokenStream result = new LowerCaseFilter(matchVersion, source);
        // the order here is important: the stopword list is not normalized!
        result = new StopFilter( matchVersion, result, stopwords);
        // TODO maybe we should make ArabicNormalization filter also KeywordAttribute aware?!
        result = new ArabicNormalizationFilter(result);
        if(!stemExclusionSet.isEmpty()) {
            result = new KeywordMarkerFilter(result, stemExclusionSet);
        }
        return new TokenStreamComponents(source, (result));
    }

}
