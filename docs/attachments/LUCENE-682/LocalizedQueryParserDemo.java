import java.util.Locale;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

public class LocalizedQueryParserDemo{
    public static void main(String[] args) throws ParseException{
        QueryParser qp = new QueryParser("field", new SimpleAnalyzer());
        qp.setLocale(Locale.FRENCH);
        qp.setUseLocalizedOperators(true);
        Query query = qp.parse("a ET b");
        String result = query.toString("field");
        System.out.println(result); // should print: +a +b
    }

}
