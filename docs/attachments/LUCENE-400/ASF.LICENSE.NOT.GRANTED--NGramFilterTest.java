/*
 * Created on Jun 8, 2005
 *
 */
package org.apache.lucene.analysis;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * 
 * @author Sebastian Kirsch <skirsch@sebastian-kirsch.org>
 *
 */
public class NGramFilterTest extends TestCase {
  
  /**
   * @author Sebastian Kirsch <skirsch@sebastian-kirsch.org>
   */
  public class TestTokenStream extends TokenStream {
    
    protected int index = 0;
    protected Token[] testToken;
    
    public TestTokenStream(Token[] testToken) {
      super();
      this.testToken = testToken;
    }
    
    /* (non-Javadoc)
     * @see org.apache.lucene.analysis.TokenStream#next()
     */
    public Token next() throws IOException {
      if (index < testToken.length) {
        return testToken[index++];
      } else {
        return null;
      }
    }
    
  }
  public static void main(String[] args) {
    junit.textui.TestRunner.run(NGramFilterTest.class);
  }
  
  public static final Token[] testToken = new Token[] {
      new Token("please", 0, 6),
      new Token("divide", 7, 13),
      new Token("this", 14, 18),
      new Token("sentence", 19, 27),
      new Token("into", 28, 32),
      new Token("ngrams", 33, 39),
  };
  
  public static Token[] testTokenWithHoles;
  
  public static final Token[] biGramTokens = new Token[] {
      new Token("please", 0, 6),
      new Token("please divide", 0, 13),
      new Token("divide", 7, 13),
      new Token("divide this", 7, 18),
      new Token("this", 14, 18),
      new Token("this sentence", 14, 27),
      new Token("sentence", 19, 27),
      new Token("sentence into", 19, 32),
      new Token("into", 28, 32),
      new Token("into ngrams", 28, 39),
      new Token("ngrams", 33, 39),
  };
  public static final int[] biGramPositionIncrements = new int[] {
      1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1
  };
  public static final String[] biGramTypes = new String[] {
      "word", "ngram", "word", "ngram", "word", "ngram", "word", "ngram", "word", "ngram", "word"
  };
  
  public static final Token[] biGramTokensWithHoles = new Token[] {
      new Token("please", 0, 6),
      new Token("please divide", 0, 13),
      new Token("divide", 7, 13),
      new Token("divide _", 7, 19),
      new Token("_", 19, 19),
      new Token("_ sentence", 19, 27),
      new Token("sentence", 19, 27),
      new Token("sentence _", 19, 33),
      new Token("_", 33, 33),
      new Token("_ ngrams", 33, 39),
      new Token("ngrams", 33, 39),
  };
  
  public static final int[] biGramPositionIncrementsWithHoles = new int[] {
      1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1
  };
  
  public static final Token[] triGramTokens = new Token[] {
      new Token("please", 0, 6),
      new Token("please divide", 0, 13),
      new Token("please divide this", 0, 18),
      new Token("divide", 7, 13),
      new Token("divide this", 7, 18),
      new Token("divide this sentence", 7, 27),
      new Token("this", 14, 18),
      new Token("this sentence", 14, 27),
      new Token("this sentence into", 14, 32),
      new Token("sentence", 19, 27),
      new Token("sentence into", 19, 32),
      new Token("sentence into ngrams", 19, 39),
      new Token("into", 28, 32),
      new Token("into ngrams", 28, 39),
      new Token("ngrams", 33, 39)
  };
  public static final int[] triGramPositionIncrements = new int[] {
      1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1
  };
  public static final String[] triGramTypes = new String[] {
      "word", "ngram", "ngram", 
      "word", "ngram", "ngram", 
      "word", "ngram", "ngram", 
      "word", "ngram", "ngram", 
      "word", "ngram", 
      "word"
  };
  
  
  protected void setUp() throws Exception {
    super.setUp();
    testTokenWithHoles = new Token[] {
        new Token("please", 0, 6),
        new Token("divide", 7, 13),
        new Token("sentence", 19, 27),
        new Token("ngrams", 33, 39),
    };
    
    testTokenWithHoles[2].setPositionIncrement(2);
    testTokenWithHoles[3].setPositionIncrement(2);
  }
  
  /*
   * Class under test for void NGramFilter(TokenStream, int)
   */
  public void testBiGramFilter() throws IOException {
    this.nGramFilterTest(2, testToken, biGramTokens, biGramPositionIncrements, biGramTypes);
  }
  
  public void testBiGramFilterWithHoles() throws IOException {
    this.nGramFilterTest(2, testTokenWithHoles, biGramTokensWithHoles, biGramPositionIncrements, biGramTypes);
  }
  
  public void testTriGramFilter() throws IOException {
    this.nGramFilterTest(3, testToken, triGramTokens, triGramPositionIncrements, triGramTypes);
  }
  
  protected void nGramFilterTest(int n,
      Token[] testToken,
      Token[] tokens,
      int[] positionIncrements,
      String[] types) throws IOException {
    TokenStream filter = new NGramFilter(new TestTokenStream(testToken), n);
    Token token;
    int i = 0;
    //		System.err.println();
    while ((token = filter.next()) != null) {
      //			System.err.println("Token:    " + token.termText() + " (" + token.startOffset()
      //					+ "-" + token.endOffset() + ", position +" + token.getPositionIncrement() + ")");
      //			System.err.println("Expected: " + tokens[i].termText() + " (" + tokens[i].startOffset()
      //					+ "-" + tokens[i].endOffset() + ", position +" + positionIncrements[i] + ")");
      assertEquals("Wrong termText",
          tokens[i].termText(), token.termText());
      assertEquals("Wrong startOffset for token \"" + token.termText() + "\"",
          tokens[i].startOffset(), token.startOffset());
      assertEquals("Wrong endOffset for token \"" + token.termText() + "\"", 
          tokens[i].endOffset(), token.endOffset());
      assertEquals("Wrong positionIncrement for token \"" + token.termText() + "\"", 
          positionIncrements[i], token.getPositionIncrement());
      assertEquals("Wrong type for token \"" + token.termText() + "\"", 
          types[i], token.type());
      i++;
    }
  }
}
