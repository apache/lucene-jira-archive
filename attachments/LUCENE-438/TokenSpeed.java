import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardFilter;

import java.io.StringReader;
import java.io.IOException;
import java.util.Set;

/**
 * @author yonik
 */

class WSTok extends TokenStuff implements Call {
  public  int call(int arg) {
    try {
      String sentence = sentences[arg & 0x3];
      TokenStream ts = analyzer.tokenStream("dummy",new StringReader(sentence));
      return readToks(ts);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

class TokCreate extends TokenStuff implements Call {
  public  int call(int arg) {
    try {
      TokenStream ts = new StrArrayTokenStream(strings, arg & 0x3, strings.length);
      return readToks(ts);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

// Identical to TokCreate, but we create a subclass of Token
// to keep the JVM optimizer on it's toes.
class TokCreate2 extends TokenStuff implements Call {
  /*** uncomment only if final has been removed from Token
  public class OtherTokenClass extends Token {
    long flags;
    public OtherTokenClass(String text, int start, int end, String typ) {
      super(text, start, end, typ);
    }
  }
  Token otherTok = new OtherTokenClass("foo",1,2,"bar");
  ***/

  public  int call(int arg) {
    try {
      TokenStream ts = new StrArrayTokenStream(strings, arg & 0x3, strings.length);
      return readToks(ts);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


}


class TokChain extends TokenStuff implements Call {
  public  int call(int arg) {
    try {
      TokenStream ts = new StrArrayTokenStream(strings, arg & 0x3, strings.length);
      ts = new StandardFilter(ts);
      ts = new LowerCaseFilter(ts);
      ts = new StopFilter(ts, stopSet);
      return readToks(ts);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}


class StrArrayTokenStream extends TokenStream {
  private final String[] strings;
  private final int start;
  private final int end;
  private int idx;

  public StrArrayTokenStream(String[] strings, int start, int end) {
    this.strings = strings;
    this.start = start;
    this.end = end;
    idx=start;
  }

  public Token next() throws IOException {
    if (idx >= end) return null;
    String str = strings[idx++];
    return new Token(str, 0, str.length(), str);
  }
}


class TokenStuff {

  public static String[] strings = {
    "now","is","the","time","for","all","good","men","to",
    "come","to","the","aid","of","their","country",
    "a","b","c","d","e","f","g","h","i","j","k","l","m",
    "n","o","p","q","r","s","t","u","v","w","x","y","z"
  };

  public static String[] sentences = {
    "Now is the time for all good men to come to the aid of their country",
    "Does removing final on a class have any impact these days?",
    "Hopefully these tests will help answer that question.",
    "a b c d e f g h i j k l m n o p q r s t u v w x y z",
  };

  static final WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
  static final Set stopSet = StopFilter.makeStopSet(new String[] {"foobar"});

  public static int readToks(TokenStream ts) throws IOException {
    int ret=0;
    while (true) {
      Token t = ts.next();
      if (t==null) break;
      // use the token so it doesn't get optimized away
      ret += t.getPositionIncrement();
      ret += t.termText().length();
    }

    return ret;
  }


}

