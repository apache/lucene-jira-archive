import java.util.Arrays;

import org.apache.lucene.analysis.CharArraySet;


public class CharArraySetShowBug {

  /**
   * @param args
   */
  public static void main(String[] args) {
    String[] words={"Hello","World","this","is","a","test"};
    char[] findme="xthisy".toCharArray();   
    CharArraySet set=new CharArraySet(10,true);
    set.addAll(Arrays.asList(words));
    
    if(set.contains(findme, 1, 4)) {
      // we should get here, but currently we do not :-(
      System.out.println("First try");
    }
    if(set.contains(new String(findme,1,4))) {
      System.out.println("Second try");
    }
  }
}
