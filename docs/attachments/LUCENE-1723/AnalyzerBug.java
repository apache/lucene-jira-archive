package lucene;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.KeywordTokenizer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.WeightedTerm;
import org.junit.Test;
import static org.junit.Assert.*;

public class AnalyzerBug {

	@Test
	public void testWithHighlighting() throws IOException {
		String text = "thetext";
		WeightedTerm[] terms = { new WeightedTerm(1.0f, text) };

		Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter(
				"<b>", "</b>"), new QueryScorer(terms));

		Analyzer[] analazers = { new StandardAnalyzer(), new SimpleAnalyzer(),
				new StopAnalyzer(), new WhitespaceAnalyzer(),
				new NewKeywordAnalyzer(), new KeywordAnalyzer() };

		// Analyzer pass except KeywordAnalyzer
		for (Analyzer analazer : analazers) {
			String highighted = highlighter.getBestFragment(analazer,
					"CONTENT", text);
			assertEquals("Failed for " + analazer.getClass().getName(), "<b>"
					+ text + "</b>", highighted);
			System.out.println(analazer.getClass().getName()
					+ " passed, value highlighted: " + highighted);
		}
	}
}

class NewKeywordAnalyzer extends KeywordAnalyzer {

	@Override
	public TokenStream reusableTokenStream(String fieldName, Reader reader)
			throws IOException {
		Tokenizer tokenizer = (Tokenizer) getPreviousTokenStream();
		if (tokenizer == null) {
			tokenizer = new NewKeywordTokenizer(reader);
			setPreviousTokenStream(tokenizer);
		} else
			tokenizer.reset(reader);
		return tokenizer;
	}

	@Override
	public TokenStream tokenStream(String fieldName, Reader reader) {
		return new NewKeywordTokenizer(reader);
	}
}

class NewKeywordTokenizer extends KeywordTokenizer {
	public NewKeywordTokenizer(Reader input) {
		super(input);
	}

	@Override
	public Token next(Token t) throws IOException {
		Token result = super.next(t);
		if (result != null) {
			result.setEndOffset(result.termLength());
		}
		return result;
	}
}
