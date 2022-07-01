package org.apache.lucene.analysis;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Payload;

public class TestCompatibility {
  public static class POSToken extends Token {
    public static final int PROPERNOUN = 1;
    public static final int NO_NOUN = 2;
    
    private int partOfSpeech;
    
    public void setPartOfSpeech(int pos) {
      partOfSpeech = pos;
    }
    
    public int getPartOfSpeech() {
      return this.partOfSpeech;
    }
  }
  
  static class PartOfSpeechTaggingFilter extends TokenFilter {

    protected PartOfSpeechTaggingFilter(TokenStream input) {
      super(input);
    }
    
    public Token next() throws IOException {
      Token t = input.next();
      if (t == null) return null;
      
      POSToken pt = new POSToken();
      pt.reinit(t);
      if (pt.termLength() > 0) {
        if (Character.isUpperCase(pt.termBuffer()[0])) {
          pt.setPartOfSpeech(POSToken.PROPERNOUN);
        } else {
          pt.setPartOfSpeech(POSToken.NO_NOUN);
        }
      }
      return pt;
    }
    
  }

  static class PartOfSpeechAnnotatingFilter extends TokenFilter {
    public final static byte PROPER_NOUN_ANNOTATION = 1;
    
    
    protected PartOfSpeechAnnotatingFilter(TokenStream input) {
      super(input);
    }
    
    public Token next() throws IOException {
      Token t = input.next();
      if (t == null) return null;
      
      if (t instanceof POSToken) {
        POSToken pt = (POSToken) t;
        if (pt.getPartOfSpeech() == POSToken.PROPERNOUN) {
          pt.setPayload(new Payload(new byte[] {PROPER_NOUN_ANNOTATION}));
        }
        return pt;
      } else {
        return t;
      }
    }
    
  }

  
  public static void main(String[] args) throws IOException {
	  System.out.println("Testing old API...");
	  test1();
	  System.out.println("Testing new API...");
	  test2();
  }
  
  private static void test1() throws IOException {
	    String doc = "This is the new TokenStream api";
	    
	    TokenStream stream = new WhitespaceTokenizer(new StringReader(doc));
	    stream = new PartOfSpeechTaggingFilter(stream);
	    stream = new LowerCaseFilter(stream);
	    stream = new StopFilter(stream, new String[] {"is", "the", "this"});
	    
	    Lucene24SinkTokenizer sink = new Lucene24SinkTokenizer();
	    TokenStream stream1 = new PartOfSpeechAnnotatingFilter(sink);
	    
	    stream = new Lucene24TeeTokenFilter(stream, sink);
	    stream = new PartOfSpeechAnnotatingFilter(stream);

	    
	    consumeStreamOldAPI(stream);
	    consumeStreamOldAPI(stream1);
	    consumeStreamOldAPI(sink);	  

  }

  private static void test2() throws IOException {
	    String doc = "This is the new TokenStream api";
	    
	    TokenStream stream = new WhitespaceTokenizer(new StringReader(doc));
	    stream = new PartOfSpeechTaggingFilter(stream);
	    stream = new LowerCaseFilter(stream);
	    stream = new StopFilter(stream, new String[] {"is", "the", "this"});
	    
	    Lucene24SinkTokenizer sink = new Lucene24SinkTokenizer();
	    TokenStream stream1 = new PartOfSpeechAnnotatingFilter(sink);
	    
	    stream = new Lucene24TeeTokenFilter(stream, sink);
	    stream = new PartOfSpeechAnnotatingFilter(stream);

	    
	    consumeStreamNewAPI(stream);
	    consumeStreamNewAPI(stream1);
	    consumeStreamNewAPI(sink);	  

}

  
  private static void consumeStreamOldAPI(TokenStream stream) throws IOException {
      stream.reset();
      Token reusableToken = new Token();
      
      while ((reusableToken = stream.next(reusableToken)) != null) {
        System.out.print(reusableToken.term());
        Payload p = reusableToken.getPayload();
        if (p != null && p.getData().length == 1 && p.getData()[0] == PartOfSpeechAnnotatingFilter.PROPER_NOUN_ANNOTATION) {
          System.out.println(" --> proper noun");
        } else {
          System.out.println("");
        }
      }   
  }

  private static void consumeStreamNewAPI(TokenStream stream) throws IOException {
      stream.reset();
      PayloadAttribute payloadAtt = (PayloadAttribute) stream.addAttribute(PayloadAttribute.class);
      TermAttribute termAtt = (TermAttribute) stream.addAttribute(TermAttribute.class);
      
      while (stream.incrementToken()) {
        System.out.print(termAtt.term());
        Payload p = payloadAtt.getPayload();
        if (p != null && p.getData().length == 1 && p.getData()[0] == PartOfSpeechAnnotatingFilter.PROPER_NOUN_ANNOTATION) {
          System.out.println(" --> proper noun");
        } else {
          System.out.println("");
        }
      }   
  }

  
  private static class Lucene24TeeTokenFilter extends TokenFilter {
	  Lucene24SinkTokenizer sink;

	  public Lucene24TeeTokenFilter(TokenStream input, Lucene24SinkTokenizer sink) {
	    super(input);
	    this.sink = sink;
	  }

	  public Token next(final Token reusableToken) throws IOException {
	    assert reusableToken != null;
	    Token nextToken = input.next(reusableToken);
	    sink.add(nextToken);
	    return nextToken;
	  }

  }
  
  private static class Lucene24SinkTokenizer extends Tokenizer {
	  protected List/*<Token>*/ lst = new ArrayList/*<Token>*/();
	  protected Iterator/*<Token>*/ iter;

	  public Lucene24SinkTokenizer(List/*<Token>*/ input) {
	    this.lst = input;
	    if (this.lst == null) this.lst = new ArrayList/*<Token>*/();
	  }

	  public Lucene24SinkTokenizer() {
	    this.lst = new ArrayList/*<Token>*/();
	  }

	  public Lucene24SinkTokenizer(int initCap){
	    this.lst = new ArrayList/*<Token>*/(initCap);
	  }

	  /**
	   * Get the tokens in the internal List.
	   * <p/>
	   * WARNING: Adding tokens to this list requires the {@link #reset()} method to be called in order for them
	   * to be made available.  Also, this Tokenizer does nothing to protect against {@link java.util.ConcurrentModificationException}s
	   * in the case of adds happening while {@link #next(org.apache.lucene.analysis.Token)} is being called.
	   * <p/>
	   * WARNING: Since this SinkTokenizer can be reset and the cached tokens made available again, do not modify them. Modify clones instead.
	   *
	   * @return A List of {@link org.apache.lucene.analysis.Token}s
	   */
	  public List/*<Token>*/ getTokens() {
	    return lst;
	  }

	  /**
	   * Returns the next token out of the list of cached tokens
	   * @return The next {@link org.apache.lucene.analysis.Token} in the Sink.
	   * @throws IOException
	   */
	  public Token next(final Token reusableToken) throws IOException {
	    assert reusableToken != null;
	    if (iter == null) iter = lst.iterator();
	    // Since this TokenStream can be reset we have to maintain the tokens as immutable
	    if (iter.hasNext()) {
	      Token nextToken = (Token) iter.next();
	      return (Token) nextToken.clone();
	    }
	    return null;
	  }



	  /**
	   * Override this method to cache only certain tokens, or new tokens based
	   * on the old tokens.
	   *
	   * @param t The {@link org.apache.lucene.analysis.Token} to add to the sink
	   */
	  public void add(Token t) {
	    if (t == null) return;
	    lst.add((Token) t.clone());
	  }

	  public void close() throws IOException {
	    //nothing to close
	    input = null;
	    lst = null;
	  }

	  /**
	   * Reset the internal data structures to the start at the front of the list of tokens.  Should be called
	   * if tokens were added to the list after an invocation of {@link #next(Token)}
	   * @throws IOException
	   */
	  public void reset() throws IOException {
	    iter = lst.iterator();
	  }

  }
}
