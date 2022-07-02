package org.apache.lucene.analysis;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Payload;
import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.LuceneTestCase;

public class TestAPIBackwardsCompatibility extends LuceneTestCase { 
  public void testSimpleTokenCaching() throws IOException {
    TokenStream stream = new TokenStream() {
      boolean done = false;
      
      public Token next() {
        if (!done) {
          done = true;
          POSToken t = new POSToken();
          t.setTermText("Test");
          return t;
        }
        return null;
      }
    };
    
    stream = new Lucene24CachingTokenFilter(stream);
    stream = new LowerCaseFilter(stream);
    stream = new CastingTokenFilter(stream);
    
    stream.reset();
    assertNotNull(stream.next());
    assertNull(stream.next());
    stream.reset();
    assertTrue(stream.incrementToken());
    assertFalse(stream.incrementToken());
  } 
  
  private static class CastingTokenFilter extends TokenFilter {

    protected CastingTokenFilter(TokenStream input) {
      super(input);
    }
    
    public Token next() throws IOException {
      Token next = input.next();
      if (next != null) {
        assertTrue(next instanceof POSToken);
      }
      return next;
    }
    
  }
  
  private static class Lucene24CachingTokenFilter extends TokenFilter {
    private List cache;
    private Iterator iterator;
    
    public Lucene24CachingTokenFilter(TokenStream input) {
      super(input);
    }
    
    public Token next(final Token reusableToken) throws IOException {
      assert reusableToken != null;
      if (cache == null) {
        // fill cache lazily
        cache = new LinkedList();
        fillCache(reusableToken);
        iterator = cache.iterator();
      }
      
      if (!iterator.hasNext()) {
        // the cache is exhausted, return null
        return null;
      }
      // Since the TokenFilter can be reset, the tokens need to be preserved as immutable.
      Token nextToken = (Token) iterator.next();
      return (Token) nextToken.clone();
    }
    
    public void reset() throws IOException {
      if(cache != null) {
        iterator = cache.iterator();
      }
    }
    
    private void fillCache(final Token reusableToken) throws IOException {
      for (Token nextToken = input.next(reusableToken); nextToken != null; nextToken = input.next(reusableToken)) {
        cache.add(nextToken.clone());
      }
    }

  }

  
  private TokenFilter getPartOfSpeechAnnotatingFilter(TokenStream in, int mode) {
    if (mode == 0) {
      return new PartOfSpeechAnnotatingFilterWithOldAPI(in);
    } else if (mode == 1) {
      return new PartOfSpeechAnnotatingFilterWithNewAPI(in);
    } else {
      return new PartOfSpeechAnnotatingFilterWithBothAPIs(in);
    }
  }
  
  private TokenFilter getPartOfSpeechTaggingFilter(TokenStream in, int mode) {
    if (mode == 0) {
      return new PartOfSpeechTaggingFilterWithOldAPI(in);
    } else if (mode == 1) {
      return new PartOfSpeechTaggingFilterWithNewAPI(in);
    } else {
      return new PartOfSpeechTaggingFilterWithBothAPIs(in);
    }    
  }

  public void test1OldAPI() throws IOException {
    String doc = "This is the new TokenStream api";

    TokenStream stream = new WhitespaceTokenizer(new StringReader(doc));
    stream = getPartOfSpeechTaggingFilter(stream, 0);
    stream = new LowerCaseFilter(stream);
    stream = new StopFilter(stream, new String[] { "is", "the", "this" });

    Lucene24SinkTokenizer sink = new Lucene24SinkTokenizer();
    TokenStream stream1 = getPartOfSpeechAnnotatingFilter(sink, 0);

    stream = new Lucene24TeeTokenFilter(stream, sink);
    stream = getPartOfSpeechAnnotatingFilter(stream, 0);


    consumeStreamOldAPI(stream, true);
    consumeStreamOldAPI(stream1, true);
    consumeStreamOldAPI(sink, false);
  }

  public void test1NewAPI() throws IOException {
    for (int mode = 0; mode < 3; mode++) {
      System.out.println("Test mode " + mode);
      
      String doc = "This is the new TokenStream api";
  
      TokenStream stream = new WhitespaceTokenizer(new StringReader(doc));
      stream = getPartOfSpeechTaggingFilter(stream, mode);
      stream = new LowerCaseFilter(stream);
      stream = new StopFilter(stream, new String[] { "is", "the", "this" });
  
      Lucene24SinkTokenizer sink = new Lucene24SinkTokenizer();
      TokenStream stream1 = getPartOfSpeechAnnotatingFilter(sink, mode);
  
      stream = new Lucene24TeeTokenFilter(stream, sink);
      stream = getPartOfSpeechAnnotatingFilter(stream, mode);
  
      consumeStreamNewAPI(stream, true);
      consumeStreamNewAPI(stream1, true);
      consumeStreamNewAPI(sink, false);
      
    }
  }

  
  public void test2OldAPI() throws IOException {
    String doc = "This is the new TokenStream api";

    TokenStream stream = new WhitespaceTokenizer(new StringReader(doc));
    stream = getPartOfSpeechTaggingFilter(stream, 0);
    stream = new LowerCaseFilter(stream);
    stream = new StopFilter(stream, new String[] { "is", "the", "this" });

    SinkTokenizer sink = new SinkTokenizer();
    TokenStream stream1 = getPartOfSpeechAnnotatingFilter(sink, 0);

    stream = new TeeTokenFilter(stream, sink);
    stream = getPartOfSpeechAnnotatingFilter(stream, 0);

    consumeStreamOldAPI(stream, true);
    consumeStreamOldAPI(stream1, true);
    consumeStreamOldAPI(sink, false);
  }

  public void test2NewAPI() throws IOException {
    for (int mode = 0; mode < 3; mode++) {
      System.out.println("Test mode " + mode);
      
      String doc = "This is the new TokenStream api";
  
      TokenStream stream = new WhitespaceTokenizer(new StringReader(doc));
      stream = getPartOfSpeechTaggingFilter(stream, mode);
      stream = new LowerCaseFilter(stream);
      stream = new StopFilter(stream, new String[] { "is", "the", "this" });
  
      SinkTokenizer sink = new SinkTokenizer();
      TokenStream stream1 = getPartOfSpeechAnnotatingFilter(sink, mode);
  
      stream = new TeeTokenFilter(stream, sink);
      stream = getPartOfSpeechAnnotatingFilter(stream, mode);
  
      consumeStreamNewAPI(stream, true);
      consumeStreamNewAPI(stream1, true);
      consumeStreamNewAPI(sink, false);      
    }
  }

  
  private static void consumeStreamOldAPI(TokenStream stream,
      boolean checkPayload) throws IOException {
    stream.reset();
    Token reusableToken = new Token();

    reusableToken = stream.next(reusableToken);
    assertNotNull(reusableToken);
    assertEquals("new", reusableToken.term());
    assertNull(reusableToken.getPayload());

    reusableToken = stream.next(reusableToken);
    assertNotNull(reusableToken);
    assertEquals("tokenstream", reusableToken.term());
    if (checkPayload) {
      Payload p = reusableToken.getPayload();
      assertTrue(p != null
          && p.getData().length == 1
          && p.getData()[0] == PartOfSpeechAnnotatingFilterWithOldAPI.PROPER_NOUN_ANNOTATION);
    }

    reusableToken = stream.next(reusableToken);
    assertNotNull(reusableToken);
    assertEquals("api", reusableToken.term());
    assertNull(reusableToken.getPayload());

    reusableToken = stream.next(reusableToken);
    assertNull(reusableToken); // end of stream
  }

  private static void consumeStreamNewAPI(TokenStream stream,
      boolean checkPayload) throws IOException {
    stream.reset();
    PayloadAttribute payloadAtt = (PayloadAttribute) stream
        .addAttribute(PayloadAttribute.class);
    TermAttribute termAtt = (TermAttribute) stream
        .addAttribute(TermAttribute.class);

    assertTrue(stream.incrementToken());
    assertEquals("new", termAtt.term());
    assertNull(payloadAtt.getPayload());

    assertTrue(stream.incrementToken());
    assertEquals("tokenstream", termAtt.term());
    if (checkPayload) {
      Payload p = payloadAtt.getPayload();
      assertTrue(p != null
          && p.getData().length == 1
          && p.getData()[0] == PartOfSpeechAnnotatingFilterWithOldAPI.PROPER_NOUN_ANNOTATION);
    }

    assertTrue(stream.incrementToken());
    assertEquals("api", termAtt.term());
    assertNull(payloadAtt.getPayload());

    assertFalse(stream.incrementToken());
  }

  private static class Lucene24TeeTokenFilter extends TokenFilter {
    Lucene24SinkTokenizer sink;

    public Lucene24TeeTokenFilter(TokenStream input, Lucene24SinkTokenizer sink) {
      super(input);
      this.sink = sink;
    }

    public Token next() throws IOException {
      Token nextToken = input.next();
      sink.add(nextToken);
      return nextToken;
    }

  }

  private static class Lucene24SinkTokenizer extends Tokenizer {
    protected List/* <Token> */lst = new ArrayList/* <Token> */();
    protected Iterator/* <Token> */iter;

    public Lucene24SinkTokenizer(List/* <Token> */input) {
      this.lst = input;
      if (this.lst == null)
        this.lst = new ArrayList/* <Token> */();
    }

    public Lucene24SinkTokenizer() {
      this.lst = new ArrayList/* <Token> */();
    }

    public Lucene24SinkTokenizer(int initCap) {
      this.lst = new ArrayList/* <Token> */(initCap);
    }

    /**
     * Get the tokens in the internal List.
     * <p/>
     * WARNING: Adding tokens to this list requires the {@link #reset()} method
     * to be called in order for them to be made available. Also, this Tokenizer
     * does nothing to protect against
     * {@link java.util.ConcurrentModificationException}s in the case of adds
     * happening while {@link #next(org.apache.lucene.analysis.Token)} is being
     * called.
     * <p/>
     * WARNING: Since this SinkTokenizer can be reset and the cached tokens made
     * available again, do not modify them. Modify clones instead.
     * 
     * @return A List of {@link org.apache.lucene.analysis.Token}s
     */
    public List/* <Token> */getTokens() {
      return lst;
    }

    /**
     * Returns the next token out of the list of cached tokens
     * 
     * @return The next {@link org.apache.lucene.analysis.Token} in the Sink.
     * @throws IOException
     */
    public Token next() throws IOException {
      if (iter == null)
        iter = lst.iterator();
      // Since this TokenStream can be reset we have to maintain the tokens as
      // immutable
      if (iter.hasNext()) {
        Token nextToken = (Token) iter.next();
        return (Token) nextToken.clone();
      }
      return null;
    }

    /**
     * Override this method to cache only certain tokens, or new tokens based on
     * the old tokens.
     * 
     * @param t
     *          The {@link org.apache.lucene.analysis.Token} to add to the sink
     */
    public void add(Token t) {
      if (t == null)
        return;
      lst.add((Token) t.clone());
    }

    public void close() throws IOException {
      // nothing to close
      input = null;
      lst = null;
    }

    /**
     * Reset the internal data structures to the start at the front of the list
     * of tokens. Should be called if tokens were added to the list after an
     * invocation of {@link #next(Token)}
     * 
     * @throws IOException
     */
    public void reset() throws IOException {
      iter = lst.iterator();
    }

  }
  
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

  public static interface POSAttribute extends Attribute {
    public void setPartOfSpeech(int pos);

    public int getPartOfSpeech();
  }

  public static class POSAttributeImpl extends AttributeImpl implements
      POSAttribute {
    private int partOfSpeech;

    public void setPartOfSpeech(int pos) {
      partOfSpeech = pos;
    }

    public int getPartOfSpeech() {
      return this.partOfSpeech;
    }

    public void clear() {
      // TODO Auto-generated method stub

    }

    public void copyTo(AttributeImpl target) {
      // TODO Auto-generated method stub

    }

    public boolean equals(Object other) {
      // TODO Auto-generated method stub
      return false;
    }

    public int hashCode() {
      // TODO Auto-generated method stub
      return 0;
    }
  }

  static class PartOfSpeechTaggingFilterWithBothAPIs extends TokenFilter {
    private TermAttribute termAtt;
    private POSAttribute posAtt;

    protected PartOfSpeechTaggingFilterWithBothAPIs(TokenStream input) {
      super(input);
      termAtt = (TermAttribute) addAttribute(TermAttribute.class);
      posAtt = (POSAttribute) addAttribute(POSAttribute.class);
    }

    public boolean incrementToken() throws IOException {
      if (!input.incrementToken())
        return false;
      if (termAtt.termLength() > 0) {
        if (Character.isUpperCase(termAtt.termBuffer()[0])) {
          posAtt.setPartOfSpeech(POSToken.PROPERNOUN);
        } else {
          posAtt.setPartOfSpeech(POSToken.NO_NOUN);
        }
      }
      return true;
    }

    public Token next() throws IOException {
      Token t = input.next();
      if (t == null)
        return null;

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

  static class PartOfSpeechTaggingFilterWithOldAPI extends TokenFilter {
    protected PartOfSpeechTaggingFilterWithOldAPI(TokenStream input) {
      super(input);
    }

    public Token next() throws IOException {
      Token t = input.next();
      if (t == null)
        return null;

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

  static class PartOfSpeechTaggingFilterWithNewAPI extends TokenFilter {
    private TermAttribute termAtt;
    private POSAttribute posAtt;

    protected PartOfSpeechTaggingFilterWithNewAPI(TokenStream input) {
      super(input);
      termAtt = (TermAttribute) addAttribute(TermAttribute.class);
      posAtt = (POSAttribute) addAttribute(POSAttribute.class);
    }

    public boolean incrementToken() throws IOException {
      if (!input.incrementToken())
        return false;
      if (termAtt.termLength() > 0) {
        if (Character.isUpperCase(termAtt.termBuffer()[0])) {
          posAtt.setPartOfSpeech(POSToken.PROPERNOUN);
        } else {
          posAtt.setPartOfSpeech(POSToken.NO_NOUN);
        }
      }
      return true;
    }
  }

  static class PartOfSpeechAnnotatingFilterWithOldAPI extends TokenFilter {
    public final static byte PROPER_NOUN_ANNOTATION = 1;

    protected PartOfSpeechAnnotatingFilterWithOldAPI(TokenStream input) {
      super(input);
    }

    public Token next() throws IOException {
      Token t = input.next();
      if (t == null)
        return null;

      if (t instanceof POSToken) {
        POSToken pt = (POSToken) t;
        if (pt.getPartOfSpeech() == POSToken.PROPERNOUN) {
          pt.setPayload(new Payload(new byte[] { PROPER_NOUN_ANNOTATION }));
        }
        return pt;
      } else {
        return t;
      }
    }

  }

  static class PartOfSpeechAnnotatingFilterWithNewAPI extends TokenFilter {
    public final static byte PROPER_NOUN_ANNOTATION = 1;
    private POSAttribute posAtt;
    private PayloadAttribute payloadAtt;

    protected PartOfSpeechAnnotatingFilterWithNewAPI(TokenStream input) {
      super(input);
      payloadAtt = (PayloadAttribute) addAttribute(PayloadAttribute.class);
      posAtt = (POSAttribute) addAttribute(POSAttribute.class);

    }

    public boolean incrementToken() throws IOException {
      if (!input.incrementToken())
        return false;

      if (posAtt.getPartOfSpeech() == POSToken.PROPERNOUN) {
        payloadAtt
            .setPayload(new Payload(new byte[] { PROPER_NOUN_ANNOTATION }));
      }
      return true;
    }

  }

  static class PartOfSpeechAnnotatingFilterWithBothAPIs extends TokenFilter {
    public final static byte PROPER_NOUN_ANNOTATION = 1;
    private POSAttribute posAtt;
    private PayloadAttribute payloadAtt;

    protected PartOfSpeechAnnotatingFilterWithBothAPIs(TokenStream input) {
      super(input);
      payloadAtt = (PayloadAttribute) addAttribute(PayloadAttribute.class);
      posAtt = (POSAttribute) addAttribute(POSAttribute.class);

    }

    public boolean incrementToken() throws IOException {
      if (!input.incrementToken())
        return false;

      if (posAtt.getPartOfSpeech() == POSToken.PROPERNOUN) {
        payloadAtt
            .setPayload(new Payload(new byte[] { PROPER_NOUN_ANNOTATION }));
      }
      return true;
    }

    public Token next() throws IOException {
      Token t = input.next();
      if (t == null)
        return null;

      if (t instanceof POSToken) {
        POSToken pt = (POSToken) t;
        if (pt.getPartOfSpeech() == POSToken.PROPERNOUN) {
          pt.setPayload(new Payload(new byte[] { PROPER_NOUN_ANNOTATION }));
        }
        return pt;
      } else {
        return t;
      }
    }

  }
}
