import java.io.*;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.*;
import org.apache.lucene.analysis.pattern.PatternReplaceFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.*;

public final class ArabicAnalyzer extends Analyzer
{
	private final CharArraySet arabicStopSet;
    public ArabicAnalyzer()
	{
		super();
		final String[] ARABIC_STOP_WORDS = {"Ãæ", "æ"};
		arabicStopSet = StopFilter.makeStopSet(Version.LUCENE_40, ARABIC_STOP_WORDS);
	}

    protected TokenStreamComponents createComponents(final String fieldName, final Reader reader)
	{
		final ArabicTokenizer src = new ArabicTokenizer(reader);
        TokenStream result = new StopFilter(Version.LUCENE_40, src, arabicStopSet);
        result = new PatternReplaceFilter(result, Pattern.compile("[\u0650\u064D\u064E\u064B\u064F\u064C\u0652\u0651]"), null, true);
		return new TokenStreamComponents(src, result);
	}
}