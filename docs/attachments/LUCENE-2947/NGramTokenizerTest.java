import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;

import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Term;
import org.junit.Test;


public class NGramTokenizerTest {
	@Test public void respectWhitespace() throws IOException {
		NGramTokenizer tokenizer = new NGramTokenizer(new StringReader(" foo bar "),3,3);
		HashSet<String> trigrams = new HashSet<String>();
		while (tokenizer.incrementToken()) {
			trigrams.add(tokenizer.getAttribute(TermAttribute.class).term());
		}
		assertTrue(trigrams.contains(" fo"));
		assertTrue(trigrams.contains("ar "));
	}
	
}
