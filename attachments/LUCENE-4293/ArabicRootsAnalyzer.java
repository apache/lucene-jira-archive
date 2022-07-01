import java.io.*;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.*;
import org.apache.lucene.analysis.pattern.PatternReplaceFilter;
import org.apache.lucene.analysis.util.*;
import org.apache.lucene.util.*;

public final class ArabicRootsAnalyzer extends StopwordAnalyzerBase
{
    private static class DefaultSetHolder
    {
        static final CharArraySet DEFAULT_STOP_SET;

        static
        {
            try
            {
                DEFAULT_STOP_SET = loadStopwordSet(new File("ArabicTokens.txt"), Version.LUCENE_40);
            }
            catch (IOException ex){throw new RuntimeException("Unable to load default stopword set");}
        }
    }

    public ArabicRootsAnalyzer()
    {
        super(Version.LUCENE_40, DefaultSetHolder.DEFAULT_STOP_SET);
    }

    protected TokenStreamComponents createComponents(final String fieldName, final Reader reader)
    {
        final ArabicTokenizer src = new ArabicTokenizer(reader);
        TokenStream result = new StopFilter(Version.LUCENE_40, src, DefaultSetHolder.DEFAULT_STOP_SET);
        result = new PatternReplaceFilter(result, Pattern.compile("[\u0650\u064D\u064E\u064B\u064F\u064C\u0652\u0651]"), null, true);
        result = new ArabicRootFilter(result);
        return new TokenStreamComponents(src, result);
    }
}