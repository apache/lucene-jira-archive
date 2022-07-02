
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Query;

public class ColonTest {

	public static void main(String[] args) throws Throwable {
		String queryWithColon = "test abc:";
		String phraseQueryEscaped = "\"" + QueryParserUtil.escape(queryWithColon) + "\"";

		StandardQueryParser queryParserHelper = new StandardQueryParser(new KeywordAnalyzer());
		Query parsedWithStandardQueryParser = queryParserHelper.parse(phraseQueryEscaped, "someField");
		System.out.println("parsed with StandardQueryParser: " + parsedWithStandardQueryParser);

		ComplexPhraseQueryParser parser = new ComplexPhraseQueryParser("someField", new KeywordAnalyzer());
		//throws:
		//Exception in thread "main" org.apache.lucene.queryparser.classic.ParseException: Cannot parse 'test abc:': Encountered "<EOF>" at line 1, column 9.
		Query parsedWithComplexPhraseQueryParser = parser.parse(phraseQueryEscaped);
		System.out.println("parsed with ComplexPhraseQueryParser: " + parsedWithComplexPhraseQueryParser);
	}

}
