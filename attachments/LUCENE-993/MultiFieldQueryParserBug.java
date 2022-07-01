import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;

public class MultiFieldQueryParserBug {
  public static void main(String[] argv) {
    try
    {
      System.out.println(MultiFieldQueryParser.parse("allowed:value",
          new String[]{"allowed", "restricted"},
          new BooleanClause.Occur[]{BooleanClause.Occur.SHOULD, BooleanClause.Occur.MUST_NOT},
          new SnowballAnalyzer("English")));
      // Output is:
      // allowed:valu -allowed:valu
    }
    catch (ParseException e)
    {
      e.printStackTrace();  // generated
    }
  }
}
