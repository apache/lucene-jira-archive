package org.apache.lucene.analysis;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * A filter that replaces accented characters in the ISO Latin 1 character set 
 * (ISO-8859-1) by their unaccented equivalent. The case will not be altered.
 * <p>
 * For instance, '&agrave;' will be replaced by 'a'.
 * <p>
 */
public class ISOLatin1AccentFilterAlt extends TokenFilter {
  private final static char [] substitutions;
  static {
      substitutions = new char[Character.MAX_VALUE];
    
      // Initialize to identity.
      for (char i = 0; i < Character.MAX_VALUE; i++) {
          substitutions[i] = i;
      }

      // Add maps for accented characters.
      substitutions['\u00C0'] = 'A'; // À
      substitutions['\u00C1'] = 'A'; // Á
      substitutions['\u00C2'] = 'A'; // Â
      substitutions['\u00C3'] = 'A'; // Ã
      substitutions['\u00C4'] = 'A'; // Ä
      substitutions['\u00C5'] = 'A'; // Å
      substitutions['\u00C6'] =   0; // Æ (double) 
      substitutions['\u00C7'] = 'C'; // Ç 
      substitutions['\u00C8'] = 'E'; // È
      substitutions['\u00C9'] = 'E'; // É
      substitutions['\u00CA'] = 'E'; // Ê
      substitutions['\u00CB'] = 'E'; // Ë
      substitutions['\u00CC'] = 'I'; // Ì
      substitutions['\u00CD'] = 'I'; // Í
      substitutions['\u00CE'] = 'I'; // Î
      substitutions['\u00CF'] = 'I'; // Ï
      substitutions['\u00D0'] = 'D'; // Ð
      substitutions['\u00D1'] = 'N'; // Ñ
      substitutions['\u00D2'] = 'O'; // Ò
      substitutions['\u00D3'] = 'O'; // Ó
      substitutions['\u00D4'] = 'O'; // Ô
      substitutions['\u00D5'] = 'O'; // Õ
      substitutions['\u00D6'] = 'O'; // Ö
      substitutions['\u00D8'] = 'O'; // Ø
      substitutions['\u0152'] =   0; // Œ (double)
      substitutions['\u00DE'] =   0; // Þ (double)
      substitutions['\u00D9'] = 'U'; // Ù
      substitutions['\u00DA'] = 'U'; // Ú
      substitutions['\u00DB'] = 'U'; // Û
      substitutions['\u00DC'] = 'U'; // Ü
      substitutions['\u00DD'] = 'Y'; // Ý
      substitutions['\u0178'] = 'Y'; // Ÿ
      substitutions['\u00E0'] = 'a'; // à
      substitutions['\u00E1'] = 'a'; // á
      substitutions['\u00E2'] = 'a'; // â
      substitutions['\u00E3'] = 'a'; // ã
      substitutions['\u00E4'] = 'a'; // ä
      substitutions['\u00E5'] = 'a'; // å
      substitutions['\u00E6'] =   0; // æ (double)
      substitutions['\u00E7'] = 'c'; // ç
      substitutions['\u00E8'] = 'e'; // è
      substitutions['\u00E9'] = 'e'; // é
      substitutions['\u00EA'] = 'e'; // ê
      substitutions['\u00EB'] = 'e'; // ë
      substitutions['\u00EC'] = 'i'; // ì
      substitutions['\u00ED'] = 'i'; // í
      substitutions['\u00EE'] = 'i'; // î
      substitutions['\u00EF'] = 'i'; // ï
      substitutions['\u00F0'] = 'd'; // ð
      substitutions['\u00F1'] = 'n'; // ñ
      substitutions['\u00F2'] = 'o'; // ò
      substitutions['\u00F3'] = 'o'; // ó
      substitutions['\u00F4'] = 'o'; // ô
      substitutions['\u00F5'] = 'o'; // õ
      substitutions['\u00F6'] = 'o'; // ö
      substitutions['\u00F8'] = 'o'; // ø
      substitutions['\u0153'] =   0; // œ (double oe)
      substitutions['\u00DF'] =   0; // ß (double ss)
      substitutions['\u00FE'] =   0; // þ (double th)
      substitutions['\u00F9'] = 'u'; // ù
      substitutions['\u00FA'] = 'u'; // ú
      substitutions['\u00FB'] = 'u'; // û
      substitutions['\u00FC'] = 'u'; // ü
      substitutions['\u00FD'] = 'y'; // ý
      substitutions['\u00FF'] = 'y'; // ÿ
  }

  public ISOLatin1AccentFilterAlt(TokenStream input) {
    super(input);
  }

  private char[] output = new char[256];

  public final Token next(Token result) throws java.io.IOException {
    result = input.next(result);
    if (result != null) {
      final char[] buffer = result.termBuffer();
      final int length = result.termLength();
      // If no characters actually require rewriting then we
      // just return token as-is:
      for(int i=0;i<length;i++) {
        final char c = buffer[i];
        if (c >= '\u00c0' && c <= '\u0178') {
          final int outputPos = removeAccents(buffer, length);
          result.setTermBuffer(output, 0, outputPos);
          break;
        }
      }
      return result;
    } else
      return null;
  }

  /**
   * To replace accented characters in a String by unaccented equivalents.
   */
  public final int removeAccents(final char[] input, final int length) {
    // Worst-case length required:
    final int maxSizeNeeded = 2 * length;

    int size = output.length;
    // No greedy doubling strategy.
    if (size < maxSizeNeeded) {
      size = maxSizeNeeded;
      output = new char[size];
    }

    int outputPos = 0;
    for (int i = 0; i < length; i++) {
      final char in = input[i];
      final char c = substitutions[in];

      if (c != 0) {
          output[outputPos++] = c;
      } else {
        // double-letter case.
        switch (in) {
        case '\u00C6' : // Æ
          output[outputPos++] = 'A';
          output[outputPos++] = 'E';
          break;
        case '\u0152' : // Œ
          output[outputPos++] = 'O';
          output[outputPos++] = 'E';
          break;
        case '\u00DE' : // Þ
          output[outputPos++] = 'T';
          output[outputPos++] = 'H';
          break;
        case '\u00E6' : // æ
            output[outputPos++] = 'a';
            output[outputPos++] = 'e';
            break;
        case '\u0153' : // œ
          output[outputPos++] = 'o';
          output[outputPos++] = 'e';
          break;
        case '\u00DF' : // ß
          output[outputPos++] = 's';
          output[outputPos++] = 's';
          break;
        case '\u00FE' : // þ
          output[outputPos++] = 't';
          output[outputPos++] = 'h';
          break;
        case 0:         // the marker.
          output[outputPos++] = 0;
        default:
          // Unreachable block.
          throw new RuntimeException();
        }
      }
    }

    return outputPos;
  }
}
