import java.nio.file.Paths;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.automaton.*;

public class RegExpNPE {
  final static String REGEXP = ".*";

  // replace these as appropriate
  final static String INDEX = "../marple/src/test/resources/index";
  final static String FIELD = "text";

  public static void main(String[] args) throws Exception {
    Directory directory = FSDirectory.open(Paths.get(INDEX));
    IndexReader reader = DirectoryReader.open(directory);
    Fields fields = MultiFields.getFields(reader);
    Terms terms = fields.terms(FIELD);
    TermsEnum termsEnum = terms.iterator();
    CompiledAutomaton automaton = new CompiledAutomaton(new RegExp(REGEXP).toAutomaton());
    TermsEnum termsEnum2 = new AutomatonTermsEnum(termsEnum, automaton);
  }
}
