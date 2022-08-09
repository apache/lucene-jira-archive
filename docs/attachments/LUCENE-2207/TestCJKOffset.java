package termVector;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.index.TermVectorOffsetInfo;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class TestCJKOffset {
  
  static final String F = "f";

  public static void main(String[] args) throws Exception {
    printIndexedTermOffsets( new WhitespaceAnalyzer() );
    printIndexedTermOffsets( new CJKAnalyzer( Version.LUCENE_CURRENT ) );
    printIndexedTermOffsets( new BasicNGramAnalyzer() );
  }

  static void printIndexedTermOffsets( Analyzer analyzer ) throws Exception {
    System.out.println( "=== " + analyzer.getClass().getSimpleName() + " ===" );
    Directory dir = new RAMDirectory();
    IndexWriter writer = new IndexWriter( dir, analyzer, true, MaxFieldLength.LIMITED );
    Document doc = new Document();
    doc.add( new Field( F, "あい", Store.YES, Index.ANALYZED, TermVector.WITH_OFFSETS ) );
    doc.add( new Field( F, "うえお", Store.YES, Index.ANALYZED, TermVector.WITH_OFFSETS ) );
    writer.addDocument( doc );
    writer.close();
    
    IndexReader reader = IndexReader.open( dir, true );
    TermPositionVector tpv = (TermPositionVector)reader.getTermFreqVector( 0, F );
    for( int i = 0; i < tpv.size(); i++ ){
      TermVectorOffsetInfo[] tvois = tpv.getOffsets( i );
      for( int j = 0; j < tvois.length; j++ ){
        int s = tvois[j].getStartOffset();
        int e = tvois[j].getEndOffset();
        System.out.println( tpv.getTerms()[i] + "(" + s + "," + e + ")" );
      }
    }
    reader.close();
  }

  /*
   * An analyzer which uses BasicNGramTokenizer. BasicNGramTokenizer
   * is used in FastVectorHighlighter test code. It works as a 2-gram
   * tokenizer for not only CJK but also ASCII.
   */
  static class BasicNGramAnalyzer extends Analyzer {
    @Override
    public TokenStream tokenStream(String fieldName, Reader input) {
      return new BasicNGramTokenizer( input );
    }
    
    @Override
    public TokenStream reusableTokenStream(String fieldName,
                                           final Reader reader) throws IOException {
      if (overridesTokenStreamMethod) {
        return tokenStream(fieldName, reader);
      }
      Tokenizer tokenizer = (Tokenizer) getPreviousTokenStream();
      if (tokenizer == null) {
        tokenizer = new BasicNGramTokenizer(reader);
        setPreviousTokenStream(tokenizer);
      } else
        tokenizer.reset(reader);
      return tokenizer;
    }
  }
  
  static class BasicNGramTokenizer extends Tokenizer {

    public static final int DEFAULT_N_SIZE = 2;
    public static final String DEFAULT_DELIMITERS = " \t\n.,";
    private final int n;
    private final String delimiters;
    private int startTerm;
    private int lenTerm;
    private int startOffset;
    private int nextStartOffset;
    private int ch;
    private String snippet;
    private StringBuilder snippetBuffer;
    private static final int BUFFER_SIZE = 4096;
    private char[] charBuffer;
    private int charBufferIndex;
    private int charBufferLen;
    
    public BasicNGramTokenizer( Reader in ){
      this( in, DEFAULT_N_SIZE );
    }
    
    public BasicNGramTokenizer( Reader in, int n ){
      this( in, n, DEFAULT_DELIMITERS );
    }
    
    public BasicNGramTokenizer( Reader in, String delimiters ){
      this( in, DEFAULT_N_SIZE, delimiters );
    }
    
    public BasicNGramTokenizer( Reader in, int n, String delimiters ){
      super(in);
      this.n = n;
      this.delimiters = delimiters;
      startTerm = 0;
      nextStartOffset = 0;
      snippet = null;
      snippetBuffer = new StringBuilder();
      charBuffer = new char[BUFFER_SIZE];
      charBufferIndex = BUFFER_SIZE;
      charBufferLen = 0;
      ch = 0;
    }

    TermAttribute termAtt = addAttribute(TermAttribute.class);
    OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
    @Override
    public boolean incrementToken() throws IOException {
      if( !getNextPartialSnippet() )
        return false;
      
      termAtt.setTermBuffer(snippet, startTerm, lenTerm);
      offsetAtt.setOffset(correctOffset(startOffset), correctOffset(startOffset + lenTerm));
      return true;
    }

    private int getFinalOffset() {
      return nextStartOffset;
    }
    
    @Override
    public final void end(){
      offsetAtt.setOffset(getFinalOffset(),getFinalOffset());
    }
    
    protected boolean getNextPartialSnippet() throws IOException {
      if( snippet != null && snippet.length() >= startTerm + 1 + n ){
        startTerm++;
        startOffset++;
        lenTerm = n;
        return true;
      }
      return getNextSnippet();
    }
    
    protected boolean getNextSnippet() throws IOException {
      startTerm = 0;
      startOffset = nextStartOffset;
      snippetBuffer.delete( 0, snippetBuffer.length() );
      while( true ){
        if( ch != -1 )
          ch = readCharFromBuffer();
        if( ch == -1 ) break;
        else if( !isDelimiter( ch ) )
          snippetBuffer.append( (char)ch );
        else if( snippetBuffer.length() > 0 )
          break;
        else
          startOffset++;
      }
      if( snippetBuffer.length() == 0 )
        return false;
      snippet = snippetBuffer.toString();
      lenTerm = snippet.length() >= n ? n : snippet.length();
      return true;
    }
    
    protected int readCharFromBuffer() throws IOException {
      if( charBufferIndex >= charBufferLen ){
        charBufferLen = input.read( charBuffer );
        if( charBufferLen == -1 ){
          return -1;
        }
        charBufferIndex = 0;
      }
      int c = (int)charBuffer[charBufferIndex++];
      nextStartOffset++;
      return c;
    }
    
    protected boolean isDelimiter( int c ){
      return delimiters.indexOf( c ) >= 0;
    }
    
    public void reset( Reader input ) throws IOException {
      super.reset( input );
      reset();
    }
    
    public void reset() throws IOException {
      startTerm = 0;
      nextStartOffset = 0;
      snippet = null;
      snippetBuffer.setLength( 0 );
      charBufferIndex = BUFFER_SIZE;
      charBufferLen = 0;
      ch = 0;
    }
  }
}
