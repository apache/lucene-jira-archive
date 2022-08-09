package org.apache.lucene.collation;

import java.io.StringReader;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;

import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.util.ULocale;

public class TestGermanCollation extends TestCase {
  
  public void testGerman() throws Exception {
    checkToken("Töne", "Toene");
  }
  
  /*
     The Representative of German interests in supranational standardization
     (Deutsches Institute für Normung, DIN, http://www2.din.de/index.php?lang=en)
     provides two standards:

     1) DIN 5007-1, Publication date:2005-08
     Filing of character strings - Part 1:
     General rules for processing (ABC rules)

     and

     2) DIN 5007-2, Publication date:1996-05
     Rules for alphabetical ordering - Part 2:
     Presentation of names


     The default is DIN 5007-1, this shows how to tailor a collator to get DIN 5007-2 behavior.
     http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4423383
   */
  
  void checkToken(String input1, String input2) throws Exception {
    RuleBasedCollator baseCollator = (RuleBasedCollator) Collator.getInstance(new ULocale("de_DE")) ;
    
    String DIN5007_2_tailorings =
      "& ae , a\u0308 & AE , A\u0308"+
      "& oe , o\u0308 & OE , O\u0308"+
      "& ue , u\u0308 & UE , u\u0308";

    Collator collator = new RuleBasedCollator(baseCollator.getRules() + DIN5007_2_tailorings);
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
