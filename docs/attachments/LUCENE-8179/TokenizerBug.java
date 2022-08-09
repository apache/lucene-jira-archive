import java.io.StringReader;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.analysis.tokenattributes.CharTermAttribute;

public class TokenizerBug
{
	
	public static void test() throws Exception {
		TokenStream ts = new StandardAnalyzer().tokenStream("", new StringReader("Their system was down"));
		CharTermAttribute termAttr = ts.addAttribute(CharTermAttribute.class);
			try {
				ts.reset();
				while(ts.incrementToken()) {
					System.out.println(termAttr.toString());
					terms.add(termAttr.toString());
				}
				ts.end();
			} finally {
				ts.close();
			}
	}
	
	public static void main(String[] argv) throws Exception
	{
		test();
	}

}