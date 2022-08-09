import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class TestCase {
  // run me on java 5
  public static void main2(String args[]) throws Exception {
    // read in all the [:letter:], lowercase([:letter:]) pairs from java 1.4, and check on java 1.5
    // check that each one lowercases to the same thing.
    BufferedReader r = new BufferedReader(new FileReader(new File("/java14.txt")));
    String line = null;
    while ((line = r.readLine()) != null) {
      String chars[] = line.split(",");
      char c1 = (char) Integer.parseInt(chars[0], 16);
      char c2 = (char) Integer.parseInt(chars[1], 16);
      if (Character.toLowerCase(c1) != c2) {
        System.out.println("for character: " + c1 + " expected: " + c2 + " found " + Character.toLowerCase(c1));
      }
    }
  }

  // run me on java 4, > output to /java14.txt
  public static void main(String args[]) throws Exception {
    // a unicode set containing all the [:letter:] of jvm 1.4
    UnicodeSet standardLetterSet = new UnicodeSet();
    for (char ch = 0; ch < 0xFFFF; ch++) {
      if (Character.isLetter(ch)) // jvm 1.4 isLetter
        standardLetterSet.add(ch);
    }

    UnicodeSetIterator iter = new UnicodeSetIterator(standardLetterSet);
    // print all the [:letter:], lowercase([:letter:]) pairs, comma separated.
    while (iter.next()) {
      if (iter.codepoint != UnicodeSetIterator.IS_STRING) {
        System.out.println("" + Integer.toHexString(iter.codepoint) + "," + 
            Integer.toHexString(Character.toLowerCase((char)iter.codepoint)));
      }
    }
  }
}
