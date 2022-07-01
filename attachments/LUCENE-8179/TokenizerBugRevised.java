import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.tartarus.snowball.ext.EnglishStemmer;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TokenizerBugRevised
{
        private static final List<String> STOP_WORDS = Arrays.asList(
			"a","about","above","across","after","afterwards","again",
			"against","all","almost","alone","along","already","also","although","always",
			"am","among","amongst","amoungst","amount","an","and","another",
			"any","anyhow","anyone","anything","anyway","anywhere","are",
			"around","as","at","back","be","became","because","become",
			"becomes","becoming","been","before","beforehand","behind","being",
			"below","beside","besides","between","beyond","bill","both",
			"bottom","but","by","call","can","cannot","cant","co","con",
			"could","couldnt","cry","de","describe","detail","do","done",
			"down","due","during","each","eg","eight","either","eleven","else",
			"elsewhere","empty","enough","etc","even","ever","every","everyone",
			"everything","everywhere","except","few","fifteen","fify","fill",
			"find","fire","first","five","for","former","formerly","forty",
			"found","four","from","front","full","further","get","give","go",
			"had","has","hasnt","have","he","hence","her","here","hereafter",
			"hereby","herein","hereupon","hers","herself","him","himself","his",
			"how","however","hundred","i","ie","if","in","inc","indeed",
			"interest","into","is","it","its","itself","keep","last","latter",
			"latterly","least","less","ltd","made","many","may","me",
			"meanwhile","might","mill","mine","more","moreover","most","mostly",
			"move","much","must","my","myself","name","namely","neither",
			"never","nevertheless","next","nine","no","nobody","none","noone",
			"nor","not","nothing","now","nowhere","of","off","often","on",
			"once","one","only","onto","or","other","others","otherwise","our",
			"ours","ourselves","out","over","own","part","per","perhaps",
			"please","put","rather","re","same","see","seem","seemed",
			"seeming","seems","serious","several","she","should","show","side",
			"since","sincere","six","sixty","so","some","somehow","someone",
			"something","sometime","sometimes","somewhere","still","such",
			"system","take","ten","than","that","the","their","them",
			"themselves","then","thence","there","thereafter","thereby",
			"therefore","therein","thereupon","these","they","thick","thin",
			"third","this","those","though","three","through","throughout",
			"thru","thus","to","together","too","top","toward","towards",
			"twelve","twenty","two","un","under","until","up","upon","us",
			"very","via","was","we","well","were","what","whatever","when",
			"whence","whenever","where","whereafter","whereas","whereby",
			"wherein","whereupon","wherever","whether","which","while","whither",
			"who","whoever","whole","whom","whose","why","will","with",
			"within","without","would","yet","you","your","yours","yourself","yourselves"
			);

	
	public static void test() throws Exception {
		Analyzer analyzer = new EnglishStopAnalyzer();
		List<String> terms = new ArrayList<>();
		String term = "Their system was down";
		StringReader reader = new StringReader(term == null? "" : term.toLowerCase());
		TokenStream ts = analyzer.tokenStream("", reader);
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

	private class EnglishStopAnalyzer extends Analyzer {
		@Override
		protected TokenStreamComponents createComponents(String s, Reader reader) {
			Tokenizer tokenizer = new StandardTokenizer(reader);
			TokenStream filter = new StopFilter(tokenizer, stopWords);
			return new TokenStreamComponents(tokenizer, filter);
		}
	}
	
	public static void main(String[] argv) throws Exception
	{
		test();
	}

}