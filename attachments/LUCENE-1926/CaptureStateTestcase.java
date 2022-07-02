package org.apache.lucene.analysis;

import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

public class CaptureStateTestcase extends TestCase {
  /* note i removed things like offsetAtt and posIncAtt to simplify this filter
   */
  private class BigramFilter extends TokenFilter {
    int termCount = 0;
    private TermAttribute termAtt;
    private State previousState;
    
    public BigramFilter(TokenStream input) {
      super(input);
      termAtt = (TermAttribute) addAttribute(TermAttribute.class);
    }
    
    public boolean incrementToken() throws IOException {
      switch(termCount++) {
        case 0: 
          input.incrementToken();
          previousState = captureState();
          return true;
        case 1:
          input.incrementToken();
          return true;
        case 2:
          State tempState = captureState(); // after we capture state here, things get strange.
          String right = termAtt.term(); // when using old consumer API, this value is wrong!!!!
          restoreState(previousState);
          String left = termAtt.term();
          termAtt.setTermBuffer(left + right);
          return true;
        default:
          return false;
      }
    }
    
    public void reset() throws IOException {
      super.reset();
      termCount = 0;
    }
  }
  
  
  public void testCapture() throws IOException {
   TokenStream ts = new BigramFilter(new StandardTokenizer(new StringReader("北方")));
   TermAttribute termAtt = (TermAttribute) ts.addAttribute(TermAttribute.class);
   assertTrue(ts.incrementToken());
   assertEquals("北", termAtt.term());
   assertTrue(ts.incrementToken());
   assertEquals("方", termAtt.term());
   assertTrue(ts.incrementToken());
   assertEquals("北方", termAtt.term());
   assertFalse(ts.incrementToken());
  }
  
  public void testCaptureOldApi() throws IOException {
    TokenStream ts = new BigramFilter(new StandardTokenizer(new StringReader("北方")));
    Token token = ts.next();
    assertEquals("北", token.term());
    token = ts.next();
    assertEquals("方", token.term());
    token = ts.next();
    assertEquals("北方", token.term());
    token = ts.next();
    assertTrue(token == null);
  }
  
}
