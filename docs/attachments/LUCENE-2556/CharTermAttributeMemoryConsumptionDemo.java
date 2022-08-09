import java.io.StringReader;
import java.util.LinkedList;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.apache.lucene.util.AttributeSource.State;

public class CharTermAttributeMemoryConsumptionDemo {

  public static void main(String[] args) {
    TokenStream ts = new WhitespaceTokenizer(Version.LUCENE_31,
        new StringReader("test"));
    CharTermAttribute charTermAttr = ts.getAttribute(CharTermAttribute.class);

    long initialFreeMemory = Runtime.getRuntime().freeMemory();

    charTermAttr.setEmpty();
    int i = 5000;
    while (--i >= 0) {
      charTermAttr.append("lucene");
    }

    int totalCharCount = charTermAttr.length();
    LinkedList<State> stateList = new LinkedList<State>();
    stateList.add(ts.captureState());

    i = 1000;
    while (--i >= 0) {
      charTermAttr.setEmpty().append("lucene");
      totalCharCount += charTermAttr.length();
      stateList.add(ts.captureState());

    }

    System.out.println("memory consumed by " + stateList.size()
        + " AttributeSource.State(s) holding a total of " + totalCharCount
        + " characters: "
        + (initialFreeMemory - Runtime.getRuntime().freeMemory()));

  }

}
