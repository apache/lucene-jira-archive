import org.apache.lucene.analysis.*;
import java.io.StringReader;

public class PerfTest3 {

  public static void main(String[] args) throws Exception {

    String text="Lorem ipsum dolor sit amet, consectetuer sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. "+
      "Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi. Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat volutpat. "+
      "Ut wisi enim ad minim veniam, quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait nulla facilisi.";
    int count = 100000;
    int c = count + 1000;
    long t = 0L;
    for (int i = 0; i < c; i++) {
      if (i==1000) t = System.currentTimeMillis();
      TokenStream tok = new WhitespaceTokenizer(new StringReader(text));
      tok = new LowerCaseFilter(tok);
      tok = new StopFilter(tok, StopAnalyzer.ENGLISH_STOP_WORDS);
      Token reusableToken=new Token();
      int num=0;
      while ((reusableToken=tok.next(reusableToken))!=null) {
        num++;
      }
    }
    t = System.currentTimeMillis()-t;
    System.out.println("Time for "+count+" runs with new instances (old API): "+(double)t / 1000.0+"s");
    
    Tokenizer tz = new WhitespaceTokenizer(new StringReader(text));
    TokenStream tok = new LowerCaseFilter(tz);
    tok = new StopFilter(tok, StopAnalyzer.ENGLISH_STOP_WORDS);
    for (int i = 0; i < c; i++) {
      if (i==1000) t = System.currentTimeMillis();
      tz.reset(new StringReader(text));
      Token reusableToken=new Token();
      int num=0;
      while ((reusableToken=tok.next(reusableToken))!=null) {
        num++;
      }
    }
    t = System.currentTimeMillis()-t;
    System.out.println("Time for "+count+" runs with reused stream (old API): "+(double)t / 1000.0+"s");
    
    try {
      TokenStream.setOnlyUseNewAPI(true);
    } catch (NoSuchMethodError e) {
    }
    try {
      for (int i = 0; i < c; i++) {
        if (i==1000) t = System.currentTimeMillis();
        tok = new WhitespaceTokenizer(new StringReader(text));
        tok = new LowerCaseFilter(tok);
        tok = new StopFilter(tok, StopAnalyzer.ENGLISH_STOP_WORDS);
        int num=0;
        while (tok.incrementToken()) {
          num++;
        }
      }
      t = System.currentTimeMillis()-t;
      System.out.println("Time for "+count+" runs with new instances (new API only): "+(double)t / 1000.0+"s");
      
      tz = new WhitespaceTokenizer(new StringReader(text));
      tok = new LowerCaseFilter(tz);
      tok = new StopFilter(tok, StopAnalyzer.ENGLISH_STOP_WORDS);
      for (int i = 0; i < c; i++) {
        if (i==1000) t = System.currentTimeMillis();
        tz.reset(new StringReader(text));
        Token reusableToken=new Token();
        int num=0;
        while (tok.incrementToken()) {
          num++;
        }
      }
      t = System.currentTimeMillis()-t;
      System.out.println("Time for "+count+" runs with reused stream (new API only): "+(double)t / 1000.0+"s");
    } catch (NoSuchMethodError e) {
    }
  }
}