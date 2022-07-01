package org.apache.lucene.collation;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;

import com.ibm.icu.text.Collator;

public class TestTurkishCollation extends TestCase {
  
  public void testTurkish() throws Exception {
    checkToken("DIGY", "dıgy");
  }
  
  void checkToken(String input1, String input2) throws IOException {
    Collator collator = Collator.getInstance(new Locale("tr_TR"));
    collator.setStrength(Collator.PRIMARY);
    
    TokenStream ts1 = new WhitespaceTokenizer(new StringReader(input1));
    ts1 = new ICUCollationKeyFilter(ts1, collator);
    
    TokenStream ts2 = new WhitespaceTokenizer(new StringReader(input2));
    ts2 = new ICUCollationKeyFilter(ts2, collator);
    
    Token token1 = ts1.next();
    Token token2 = ts2.next();
    assertTrue(token1 != null);
    assertTrue(token2 != null);
    assertEquals(token1.term(), token2.term());
    assertTrue(ts1.next() == null);
    assertTrue(ts2.next() == null);
  }
}
