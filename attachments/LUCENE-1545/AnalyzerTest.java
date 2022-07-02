import java.io.StringReader;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

public class AnalyzerTest
{
	
	public static void test() throws Exception {
		TokenStream ts = new StandardAnalyzer().tokenStream("", new StringReader("moͤchte m mo\u0364chte "));
		Token token = new Token();
		while ((token = ts.next(token)) != null) {
			System.out.println(token);
		}
	}
	
	public static void main(String[] argv) throws Exception
	{
		test();
	}

}
