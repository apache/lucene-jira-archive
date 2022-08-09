/*
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
package org.apache.lucene.analysis.miscellaneous;

public class ASCIIFolding {

  private static int[] charMap;
  private static String[] charMapTarget;
  
  static {
    init();
  }
  
  /**
   * Converts characters above ASCII to their ASCII equivalents.  For example,
   * accents are removed from accented characters.
   * @param input     The characters to fold
   * @param inputPos  Index of the first character to fold
   * @param output    The result of the folding. Should be of size &gt;= {@code length * 4}.
   * @param outputPos Index of output where to put the result of the folding
   * @param length    The number of characters to fold
   * @return length of output
   * @lucene.internal
   */
  public static final int foldToASCII(char input[], int inputPos, char output[], int outputPos, int length)
  {
    final int end = inputPos + length;
    for (int pos = inputPos; pos < end ; ++pos) {
      final char c = input[pos];

      // Quick test: if it's not in range then just keep current character
      if (c < '\u0080') {
        output[outputPos++] = c;
      } else {
        // Binary search through charMap
        int left = 0;
        int right = charMap.length / 2 - 1;
        String target = null;
        while (left <= right) {
          final int mid = (left + right) >>> 1;
          final int midVal = charMap[mid * 2];
          if (c < midVal) {
            right = mid - 1;
          } else if (c > midVal) {
            left = mid + 1;
          } else {
            int targetIndex = charMap[mid * 2 + 1];
            target = charMapTarget[targetIndex];
            break;
          }
        }
        
        if (target != null) {
          for (int i = 0; i < target.length(); i++) {
            output[outputPos++] = target.charAt(i);
          }
        } else {
          output[outputPos++] = c;
        }
      }
    }
    
    return outputPos;
  }

  private static void init() {
    /* The data structures below were generated from ASCIIFoldingFilter.java using the Perl script below:
#!/usr/bin/perl

use warnings;
use strict;

my @source_chars = ();
my @source_char_descriptions = ();
my $target = '';
my @targets = ();
my @lines = ();

while (<>) {
  if (/case\s+'(\\u[A-F0-9]+)':\s*\/\/\s*(.*)/i) {
    push @source_chars, $1;
  push @source_char_descriptions, $2;
  next;
  }
  if (/output\[[^\]]+\]\s*=\s*'(\\'|\\\\|.)'/) {
    $target .= $1;
    next;
  }
  if (/break;/) {
    next if $#source_chars == -1;

    $target = "\\\"" if ($target eq '"');
    push @targets, $target;

    for my $source_char_num (0..$#source_chars) {
    #print "// $source_char_descriptions[$source_char_num]\n";
    #print "\"$source_chars[$source_char_num]\" => \"$target\"\n\n";
    push @lines, "'$source_chars[$source_char_num]', $#targets, // $source_char_descriptions[$source_char_num]\n";
  }

    @source_chars = ();
    @source_char_descriptions = ();
    $target = '';
  }
}

print "charMap = new int[] {\n";
for my $line (sort @lines) {
  print $line;
}
print "};\n";

print "charMapTarget = new String[] {\n";
for my $target (@targets) {
  print "\"$target\",\n";
}

print "};\n";
     */
    charMap = new int[] {
        '\u00AB', 193, // «  [LEFT-POINTING DOUBLE ANGLE QUOTATION MARK]
        '\u00B2', 136, // ²  [SUPERSCRIPT TWO]
        '\u00B3', 139, // ³  [SUPERSCRIPT THREE]
        '\u00B9', 133, // ¹  [SUPERSCRIPT ONE]
        '\u00BB', 193, // »  [RIGHT-POINTING DOUBLE ANGLE QUOTATION MARK]
        '\u00C0', 0, // À  [LATIN CAPITAL LETTER A WITH GRAVE]
        '\u00C1', 0, // Á  [LATIN CAPITAL LETTER A WITH ACUTE]
        '\u00C2', 0, // Â  [LATIN CAPITAL LETTER A WITH CIRCUMFLEX]
        '\u00C3', 0, // Ã  [LATIN CAPITAL LETTER A WITH TILDE]
        '\u00C4', 0, // Ä  [LATIN CAPITAL LETTER A WITH DIAERESIS]
        '\u00C5', 0, // Å  [LATIN CAPITAL LETTER A WITH RING ABOVE]
        '\u00C6', 3, // Æ  [LATIN CAPITAL LETTER AE]
        '\u00C7', 18, // Ç  [LATIN CAPITAL LETTER C WITH CEDILLA]
        '\u00C8', 28, // È  [LATIN CAPITAL LETTER E WITH GRAVE]
        '\u00C9', 28, // É  [LATIN CAPITAL LETTER E WITH ACUTE]
        '\u00CA', 28, // Ê  [LATIN CAPITAL LETTER E WITH CIRCUMFLEX]
        '\u00CB', 28, // Ë  [LATIN CAPITAL LETTER E WITH DIAERESIS]
        '\u00CC', 47, // Ì  [LATIN CAPITAL LETTER I WITH GRAVE]
        '\u00CD', 47, // Í  [LATIN CAPITAL LETTER I WITH ACUTE]
        '\u00CE', 47, // Î  [LATIN CAPITAL LETTER I WITH CIRCUMFLEX]
        '\u00CF', 47, // Ï  [LATIN CAPITAL LETTER I WITH DIAERESIS]
        '\u00D0', 21, // Ð  [LATIN CAPITAL LETTER ETH]
        '\u00D1', 71, // Ñ  [LATIN CAPITAL LETTER N WITH TILDE]
        '\u00D2', 77, // Ò  [LATIN CAPITAL LETTER O WITH GRAVE]
        '\u00D3', 77, // Ó  [LATIN CAPITAL LETTER O WITH ACUTE]
        '\u00D4', 77, // Ô  [LATIN CAPITAL LETTER O WITH CIRCUMFLEX]
        '\u00D5', 77, // Õ  [LATIN CAPITAL LETTER O WITH TILDE]
        '\u00D6', 77, // Ö  [LATIN CAPITAL LETTER O WITH DIAERESIS]
        '\u00D8', 77, // Ø  [LATIN CAPITAL LETTER O WITH STROKE]
        '\u00D9', 111, // Ù  [LATIN CAPITAL LETTER U WITH GRAVE]
        '\u00DA', 111, // Ú  [LATIN CAPITAL LETTER U WITH ACUTE]
        '\u00DB', 111, // Û  [LATIN CAPITAL LETTER U WITH CIRCUMFLEX]
        '\u00DC', 111, // Ü  [LATIN CAPITAL LETTER U WITH DIAERESIS]
        '\u00DD', 126, // Ý  [LATIN CAPITAL LETTER Y WITH ACUTE]
        '\u00DE', 104, // Þ  [LATIN CAPITAL LETTER THORN]
        '\u00DF', 100, // ß  [LATIN SMALL LETTER SHARP S]
        '\u00E0', 1, // à  [LATIN SMALL LETTER A WITH GRAVE]
        '\u00E1', 1, // á  [LATIN SMALL LETTER A WITH ACUTE]
        '\u00E2', 1, // â  [LATIN SMALL LETTER A WITH CIRCUMFLEX]
        '\u00E3', 1, // ã  [LATIN SMALL LETTER A WITH TILDE]
        '\u00E4', 1, // ä  [LATIN SMALL LETTER A WITH DIAERESIS]
        '\u00E5', 1, // å  [LATIN SMALL LETTER A WITH RING ABOVE]
        '\u00E6', 10, // æ  [LATIN SMALL LETTER AE]
        '\u00E7', 19, // ç  [LATIN SMALL LETTER C WITH CEDILLA]
        '\u00E8', 29, // è  [LATIN SMALL LETTER E WITH GRAVE]
        '\u00E9', 29, // é  [LATIN SMALL LETTER E WITH ACUTE]
        '\u00EA', 29, // ê  [LATIN SMALL LETTER E WITH CIRCUMFLEX]
        '\u00EB', 29, // ë  [LATIN SMALL LETTER E WITH DIAERESIS]
        '\u00EC', 48, // ì  [LATIN SMALL LETTER I WITH GRAVE]
        '\u00ED', 48, // í  [LATIN SMALL LETTER I WITH ACUTE]
        '\u00EE', 48, // î  [LATIN SMALL LETTER I WITH CIRCUMFLEX]
        '\u00EF', 48, // ï  [LATIN SMALL LETTER I WITH DIAERESIS]
        '\u00F0', 22, // ð  [LATIN SMALL LETTER ETH]
        '\u00F1', 72, // ñ  [LATIN SMALL LETTER N WITH TILDE]
        '\u00F2', 78, // ò  [LATIN SMALL LETTER O WITH GRAVE]
        '\u00F3', 78, // ó  [LATIN SMALL LETTER O WITH ACUTE]
        '\u00F4', 78, // ô  [LATIN SMALL LETTER O WITH CIRCUMFLEX]
        '\u00F5', 78, // õ  [LATIN SMALL LETTER O WITH TILDE]
        '\u00F6', 78, // ö  [LATIN SMALL LETTER O WITH DIAERESIS]
        '\u00F8', 78, // ø  [LATIN SMALL LETTER O WITH STROKE]
        '\u00F9', 112, // ù  [LATIN SMALL LETTER U WITH GRAVE]
        '\u00FA', 112, // ú  [LATIN SMALL LETTER U WITH ACUTE]
        '\u00FB', 112, // û  [LATIN SMALL LETTER U WITH CIRCUMFLEX]
        '\u00FC', 112, // ü  [LATIN SMALL LETTER U WITH DIAERESIS]
        '\u00FD', 127, // ý  [LATIN SMALL LETTER Y WITH ACUTE]
        '\u00FE', 108, // þ  [LATIN SMALL LETTER THORN]
        '\u00FF', 127, // ÿ  [LATIN SMALL LETTER Y WITH DIAERESIS]
        '\u0100', 0, // Ā  [LATIN CAPITAL LETTER A WITH MACRON]
        '\u0101', 1, // ā  [LATIN SMALL LETTER A WITH MACRON]
        '\u0102', 0, // Ă  [LATIN CAPITAL LETTER A WITH BREVE]
        '\u0103', 1, // ă  [LATIN SMALL LETTER A WITH BREVE]
        '\u0104', 0, // Ą  [LATIN CAPITAL LETTER A WITH OGONEK]
        '\u0105', 1, // ą  [LATIN SMALL LETTER A WITH OGONEK]
        '\u0106', 18, // Ć  [LATIN CAPITAL LETTER C WITH ACUTE]
        '\u0107', 19, // ć  [LATIN SMALL LETTER C WITH ACUTE]
        '\u0108', 18, // Ĉ  [LATIN CAPITAL LETTER C WITH CIRCUMFLEX]
        '\u0109', 19, // ĉ  [LATIN SMALL LETTER C WITH CIRCUMFLEX]
        '\u010A', 18, // Ċ  [LATIN CAPITAL LETTER C WITH DOT ABOVE]
        '\u010B', 19, // ċ  [LATIN SMALL LETTER C WITH DOT ABOVE]
        '\u010C', 18, // Č  [LATIN CAPITAL LETTER C WITH CARON]
        '\u010D', 19, // č  [LATIN SMALL LETTER C WITH CARON]
        '\u010E', 21, // Ď  [LATIN CAPITAL LETTER D WITH CARON]
        '\u010F', 22, // ď  [LATIN SMALL LETTER D WITH CARON]
        '\u0110', 21, // Đ  [LATIN CAPITAL LETTER D WITH STROKE]
        '\u0111', 22, // đ  [LATIN SMALL LETTER D WITH STROKE]
        '\u0112', 28, // Ē  [LATIN CAPITAL LETTER E WITH MACRON]
        '\u0113', 29, // ē  [LATIN SMALL LETTER E WITH MACRON]
        '\u0114', 28, // Ĕ  [LATIN CAPITAL LETTER E WITH BREVE]
        '\u0115', 29, // ĕ  [LATIN SMALL LETTER E WITH BREVE]
        '\u0116', 28, // Ė  [LATIN CAPITAL LETTER E WITH DOT ABOVE]
        '\u0117', 29, // ė  [LATIN SMALL LETTER E WITH DOT ABOVE]
        '\u0118', 28, // Ę  [LATIN CAPITAL LETTER E WITH OGONEK]
        '\u0119', 29, // ę  [LATIN SMALL LETTER E WITH OGONEK]
        '\u011A', 28, // Ě  [LATIN CAPITAL LETTER E WITH CARON]
        '\u011B', 29, // ě  [LATIN SMALL LETTER E WITH CARON]
        '\u011C', 39, // Ĝ  [LATIN CAPITAL LETTER G WITH CIRCUMFLEX]
        '\u011D', 40, // ĝ  [LATIN SMALL LETTER G WITH CIRCUMFLEX]
        '\u011E', 39, // Ğ  [LATIN CAPITAL LETTER G WITH BREVE]
        '\u011F', 40, // ğ  [LATIN SMALL LETTER G WITH BREVE]
        '\u0120', 39, // Ġ  [LATIN CAPITAL LETTER G WITH DOT ABOVE]
        '\u0121', 40, // ġ  [LATIN SMALL LETTER G WITH DOT ABOVE]
        '\u0122', 39, // Ģ  [LATIN CAPITAL LETTER G WITH CEDILLA]
        '\u0123', 40, // ģ  [LATIN SMALL LETTER G WITH CEDILLA]
        '\u0124', 42, // Ĥ  [LATIN CAPITAL LETTER H WITH CIRCUMFLEX]
        '\u0125', 43, // ĥ  [LATIN SMALL LETTER H WITH CIRCUMFLEX]
        '\u0126', 42, // Ħ  [LATIN CAPITAL LETTER H WITH STROKE]
        '\u0127', 43, // ħ  [LATIN SMALL LETTER H WITH STROKE]
        '\u0128', 47, // Ĩ  [LATIN CAPITAL LETTER I WITH TILDE]
        '\u0129', 48, // ĩ  [LATIN SMALL LETTER I WITH TILDE]
        '\u012A', 47, // Ī  [LATIN CAPITAL LETTER I WITH MACRON]
        '\u012B', 48, // ī  [LATIN SMALL LETTER I WITH MACRON]
        '\u012C', 47, // Ĭ  [LATIN CAPITAL LETTER I WITH BREVE]
        '\u012D', 48, // ĭ  [LATIN SMALL LETTER I WITH BREVE]
        '\u012E', 47, // Į  [LATIN CAPITAL LETTER I WITH OGONEK]
        '\u012F', 48, // į  [LATIN SMALL LETTER I WITH OGONEK]
        '\u0130', 47, // İ  [LATIN CAPITAL LETTER I WITH DOT ABOVE]
        '\u0131', 48, // ı  [LATIN SMALL LETTER DOTLESS I]
        '\u0132', 49, // Ĳ  [LATIN CAPITAL LIGATURE IJ]
        '\u0133', 51, // ĳ  [LATIN SMALL LIGATURE IJ]
        '\u0134', 52, // Ĵ  [LATIN CAPITAL LETTER J WITH CIRCUMFLEX]
        '\u0135', 53, // ĵ  [LATIN SMALL LETTER J WITH CIRCUMFLEX]
        '\u0136', 55, // Ķ  [LATIN CAPITAL LETTER K WITH CEDILLA]
        '\u0137', 56, // ķ  [LATIN SMALL LETTER K WITH CEDILLA]
        '\u0138', 90, // ĸ  http://en.wikipedia.org/wiki/Kra_(letter)  [LATIN SMALL LETTER KRA]
        '\u0139', 58, // Ĺ  [LATIN CAPITAL LETTER L WITH ACUTE]
        '\u013A', 59, // ĺ  [LATIN SMALL LETTER L WITH ACUTE]
        '\u013B', 58, // Ļ  [LATIN CAPITAL LETTER L WITH CEDILLA]
        '\u013C', 59, // ļ  [LATIN SMALL LETTER L WITH CEDILLA]
        '\u013D', 58, // Ľ  [LATIN CAPITAL LETTER L WITH CARON]
        '\u013E', 59, // ľ  [LATIN SMALL LETTER L WITH CARON]
        '\u013F', 58, // Ŀ  [LATIN CAPITAL LETTER L WITH MIDDLE DOT]
        '\u0140', 59, // ŀ  [LATIN SMALL LETTER L WITH MIDDLE DOT]
        '\u0141', 58, // Ł  [LATIN CAPITAL LETTER L WITH STROKE]
        '\u0142', 59, // ł  [LATIN SMALL LETTER L WITH STROKE]
        '\u0143', 71, // Ń  [LATIN CAPITAL LETTER N WITH ACUTE]
        '\u0144', 72, // ń  [LATIN SMALL LETTER N WITH ACUTE]
        '\u0145', 71, // Ņ  [LATIN CAPITAL LETTER N WITH CEDILLA]
        '\u0146', 72, // ņ  [LATIN SMALL LETTER N WITH CEDILLA]
        '\u0147', 71, // Ň  [LATIN CAPITAL LETTER N WITH CARON]
        '\u0148', 72, // ň  [LATIN SMALL LETTER N WITH CARON]
        '\u0149', 72, // ŉ  [LATIN SMALL LETTER N PRECEDED BY APOSTROPHE]
        '\u014A', 71, // Ŋ  http://en.wikipedia.org/wiki/Eng_(letter)  [LATIN CAPITAL LETTER ENG]
        '\u014B', 72, // ŋ  http://en.wikipedia.org/wiki/Eng_(letter)  [LATIN SMALL LETTER ENG]
        '\u014C', 77, // Ō  [LATIN CAPITAL LETTER O WITH MACRON]
        '\u014D', 78, // ō  [LATIN SMALL LETTER O WITH MACRON]
        '\u014E', 77, // Ŏ  [LATIN CAPITAL LETTER O WITH BREVE]
        '\u014F', 78, // ŏ  [LATIN SMALL LETTER O WITH BREVE]
        '\u0150', 77, // Ő  [LATIN CAPITAL LETTER O WITH DOUBLE ACUTE]
        '\u0151', 78, // ő  [LATIN SMALL LETTER O WITH DOUBLE ACUTE]
        '\u0152', 79, // Œ  [LATIN CAPITAL LIGATURE OE]
        '\u0153', 83, // œ  [LATIN SMALL LIGATURE OE]
        '\u0154', 93, // Ŕ  [LATIN CAPITAL LETTER R WITH ACUTE]
        '\u0155', 94, // ŕ  [LATIN SMALL LETTER R WITH ACUTE]
        '\u0156', 93, // Ŗ  [LATIN CAPITAL LETTER R WITH CEDILLA]
        '\u0157', 94, // ŗ  [LATIN SMALL LETTER R WITH CEDILLA]
        '\u0158', 93, // Ř  [LATIN CAPITAL LETTER R WITH CARON]
        '\u0159', 94, // ř  [LATIN SMALL LETTER R WITH CARON]
        '\u015A', 96, // Ś  [LATIN CAPITAL LETTER S WITH ACUTE]
        '\u015B', 97, // ś  [LATIN SMALL LETTER S WITH ACUTE]
        '\u015C', 96, // Ŝ  [LATIN CAPITAL LETTER S WITH CIRCUMFLEX]
        '\u015D', 97, // ŝ  [LATIN SMALL LETTER S WITH CIRCUMFLEX]
        '\u015E', 96, // Ş  [LATIN CAPITAL LETTER S WITH CEDILLA]
        '\u015F', 97, // ş  [LATIN SMALL LETTER S WITH CEDILLA]
        '\u0160', 96, // Š  [LATIN CAPITAL LETTER S WITH CARON]
        '\u0161', 97, // š  [LATIN SMALL LETTER S WITH CARON]
        '\u0162', 102, // Ţ  [LATIN CAPITAL LETTER T WITH CEDILLA]
        '\u0163', 103, // ţ  [LATIN SMALL LETTER T WITH CEDILLA]
        '\u0164', 102, // Ť  [LATIN CAPITAL LETTER T WITH CARON]
        '\u0165', 103, // ť  [LATIN SMALL LETTER T WITH CARON]
        '\u0166', 102, // Ŧ  [LATIN CAPITAL LETTER T WITH STROKE]
        '\u0167', 103, // ŧ  [LATIN SMALL LETTER T WITH STROKE]
        '\u0168', 111, // Ũ  [LATIN CAPITAL LETTER U WITH TILDE]
        '\u0169', 112, // ũ  [LATIN SMALL LETTER U WITH TILDE]
        '\u016A', 111, // Ū  [LATIN CAPITAL LETTER U WITH MACRON]
        '\u016B', 112, // ū  [LATIN SMALL LETTER U WITH MACRON]
        '\u016C', 111, // Ŭ  [LATIN CAPITAL LETTER U WITH BREVE]
        '\u016D', 112, // ŭ  [LATIN SMALL LETTER U WITH BREVE]
        '\u016E', 111, // Ů  [LATIN CAPITAL LETTER U WITH RING ABOVE]
        '\u016F', 112, // ů  [LATIN SMALL LETTER U WITH RING ABOVE]
        '\u0170', 111, // Ű  [LATIN CAPITAL LETTER U WITH DOUBLE ACUTE]
        '\u0171', 112, // ű  [LATIN SMALL LETTER U WITH DOUBLE ACUTE]
        '\u0172', 111, // Ų  [LATIN CAPITAL LETTER U WITH OGONEK]
        '\u0173', 112, // ų  [LATIN SMALL LETTER U WITH OGONEK]
        '\u0174', 120, // Ŵ  [LATIN CAPITAL LETTER W WITH CIRCUMFLEX]
        '\u0175', 121, // ŵ  [LATIN SMALL LETTER W WITH CIRCUMFLEX]
        '\u0176', 126, // Ŷ  [LATIN CAPITAL LETTER Y WITH CIRCUMFLEX]
        '\u0177', 127, // ŷ  [LATIN SMALL LETTER Y WITH CIRCUMFLEX]
        '\u0178', 126, // Ÿ  [LATIN CAPITAL LETTER Y WITH DIAERESIS]
        '\u0179', 129, // Ź  [LATIN CAPITAL LETTER Z WITH ACUTE]
        '\u017A', 130, // ź  [LATIN SMALL LETTER Z WITH ACUTE]
        '\u017B', 129, // Ż  [LATIN CAPITAL LETTER Z WITH DOT ABOVE]
        '\u017C', 130, // ż  [LATIN SMALL LETTER Z WITH DOT ABOVE]
        '\u017D', 129, // Ž  [LATIN CAPITAL LETTER Z WITH CARON]
        '\u017E', 130, // ž  [LATIN SMALL LETTER Z WITH CARON]
        '\u017F', 97, // ſ  http://en.wikipedia.org/wiki/Long_S  [LATIN SMALL LETTER LONG S]
        '\u0180', 16, // ƀ  [LATIN SMALL LETTER B WITH STROKE]
        '\u0181', 15, // Ɓ  [LATIN CAPITAL LETTER B WITH HOOK]
        '\u0182', 15, // Ƃ  [LATIN CAPITAL LETTER B WITH TOPBAR]
        '\u0183', 16, // ƃ  [LATIN SMALL LETTER B WITH TOPBAR]
        '\u0186', 77, // Ɔ  [LATIN CAPITAL LETTER OPEN O]
        '\u0187', 18, // Ƈ  [LATIN CAPITAL LETTER C WITH HOOK]
        '\u0188', 19, // ƈ  [LATIN SMALL LETTER C WITH HOOK]
        '\u0189', 21, // Ɖ  [LATIN CAPITAL LETTER AFRICAN D]
        '\u018A', 21, // Ɗ  [LATIN CAPITAL LETTER D WITH HOOK]
        '\u018B', 21, // Ƌ  [LATIN CAPITAL LETTER D WITH TOPBAR]
        '\u018C', 22, // ƌ  [LATIN SMALL LETTER D WITH TOPBAR]
        '\u018E', 28, // Ǝ  [LATIN CAPITAL LETTER REVERSED E]
        '\u018F', 0, // Ə  http://en.wikipedia.org/wiki/Schwa  [LATIN CAPITAL LETTER SCHWA]
        '\u0190', 28, // Ɛ  [LATIN CAPITAL LETTER OPEN E]
        '\u0191', 31, // Ƒ  [LATIN CAPITAL LETTER F WITH HOOK]
        '\u0192', 32, // ƒ  [LATIN SMALL LETTER F WITH HOOK]
        '\u0193', 39, // Ɠ  [LATIN CAPITAL LETTER G WITH HOOK]
        '\u0195', 46, // ƕ  [LATIN SMALL LETTER HV]
        '\u0196', 47, // Ɩ  [LATIN CAPITAL LETTER IOTA]
        '\u0197', 47, // Ɨ  [LATIN CAPITAL LETTER I WITH STROKE]
        '\u0198', 55, // Ƙ  [LATIN CAPITAL LETTER K WITH HOOK]
        '\u0199', 56, // ƙ  [LATIN SMALL LETTER K WITH HOOK]
        '\u019A', 59, // ƚ  [LATIN SMALL LETTER L WITH BAR]
        '\u019C', 68, // Ɯ  [LATIN CAPITAL LETTER TURNED M]
        '\u019D', 71, // Ɲ  [LATIN CAPITAL LETTER N WITH LEFT HOOK]
        '\u019E', 72, // ƞ  [LATIN SMALL LETTER N WITH LONG RIGHT LEG]
        '\u019F', 77, // Ɵ  [LATIN CAPITAL LETTER O WITH MIDDLE TILDE]
        '\u01A0', 77, // Ơ  [LATIN CAPITAL LETTER O WITH HORN]
        '\u01A1', 78, // ơ  [LATIN SMALL LETTER O WITH HORN]
        '\u01A4', 86, // Ƥ  [LATIN CAPITAL LETTER P WITH HOOK]
        '\u01A5', 87, // ƥ  [LATIN SMALL LETTER P WITH HOOK]
        '\u01AB', 103, // ƫ  [LATIN SMALL LETTER T WITH PALATAL HOOK]
        '\u01AC', 102, // Ƭ  [LATIN CAPITAL LETTER T WITH HOOK]
        '\u01AD', 103, // ƭ  [LATIN SMALL LETTER T WITH HOOK]
        '\u01AE', 102, // Ʈ  [LATIN CAPITAL LETTER T WITH RETROFLEX HOOK]
        '\u01AF', 111, // Ư  [LATIN CAPITAL LETTER U WITH HORN]
        '\u01B0', 112, // ư  [LATIN SMALL LETTER U WITH HORN]
        '\u01B2', 115, // Ʋ  [LATIN CAPITAL LETTER V WITH HOOK]
        '\u01B3', 126, // Ƴ  [LATIN CAPITAL LETTER Y WITH HOOK]
        '\u01B4', 127, // ƴ  [LATIN SMALL LETTER Y WITH HOOK]
        '\u01B5', 129, // Ƶ  [LATIN CAPITAL LETTER Z WITH STROKE]
        '\u01B6', 130, // ƶ  [LATIN SMALL LETTER Z WITH STROKE]
        '\u01BF', 121, // ƿ  http://en.wikipedia.org/wiki/Wynn  [LATIN LETTER WYNN]
        '\u01C4', 23, // Ǆ  [LATIN CAPITAL LETTER DZ WITH CARON]
        '\u01C5', 24, // ǅ  [LATIN CAPITAL LETTER D WITH SMALL LETTER Z WITH CARON]
        '\u01C6', 27, // ǆ  [LATIN SMALL LETTER DZ WITH CARON]
        '\u01C7', 60, // Ǉ  [LATIN CAPITAL LETTER LJ]
        '\u01C8', 62, // ǈ  [LATIN CAPITAL LETTER L WITH SMALL LETTER J]
        '\u01C9', 64, // ǉ  [LATIN SMALL LETTER LJ]
        '\u01CA', 73, // Ǌ  [LATIN CAPITAL LETTER NJ]
        '\u01CB', 74, // ǋ  [LATIN CAPITAL LETTER N WITH SMALL LETTER J]
        '\u01CC', 76, // ǌ  [LATIN SMALL LETTER NJ]
        '\u01CD', 0, // Ǎ  [LATIN CAPITAL LETTER A WITH CARON]
        '\u01CE', 1, // ǎ  [LATIN SMALL LETTER A WITH CARON]
        '\u01CF', 47, // Ǐ  [LATIN CAPITAL LETTER I WITH CARON]
        '\u01D0', 48, // ǐ  [LATIN SMALL LETTER I WITH CARON]
        '\u01D1', 77, // Ǒ  [LATIN CAPITAL LETTER O WITH CARON]
        '\u01D2', 78, // ǒ  [LATIN SMALL LETTER O WITH CARON]
        '\u01D3', 111, // Ǔ  [LATIN CAPITAL LETTER U WITH CARON]
        '\u01D4', 112, // ǔ  [LATIN SMALL LETTER U WITH CARON]
        '\u01D5', 111, // Ǖ  [LATIN CAPITAL LETTER U WITH DIAERESIS AND MACRON]
        '\u01D6', 112, // ǖ  [LATIN SMALL LETTER U WITH DIAERESIS AND MACRON]
        '\u01D7', 111, // Ǘ  [LATIN CAPITAL LETTER U WITH DIAERESIS AND ACUTE]
        '\u01D8', 112, // ǘ  [LATIN SMALL LETTER U WITH DIAERESIS AND ACUTE]
        '\u01D9', 111, // Ǚ  [LATIN CAPITAL LETTER U WITH DIAERESIS AND CARON]
        '\u01DA', 112, // ǚ  [LATIN SMALL LETTER U WITH DIAERESIS AND CARON]
        '\u01DB', 111, // Ǜ  [LATIN CAPITAL LETTER U WITH DIAERESIS AND GRAVE]
        '\u01DC', 112, // ǜ  [LATIN SMALL LETTER U WITH DIAERESIS AND GRAVE]
        '\u01DD', 29, // ǝ  [LATIN SMALL LETTER TURNED E]
        '\u01DE', 0, // Ǟ  [LATIN CAPITAL LETTER A WITH DIAERESIS AND MACRON]
        '\u01DF', 1, // ǟ  [LATIN SMALL LETTER A WITH DIAERESIS AND MACRON]
        '\u01E0', 0, // Ǡ  [LATIN CAPITAL LETTER A WITH DOT ABOVE AND MACRON]
        '\u01E1', 1, // ǡ  [LATIN SMALL LETTER A WITH DOT ABOVE AND MACRON]
        '\u01E2', 3, // Ǣ  [LATIN CAPITAL LETTER AE WITH MACRON]
        '\u01E3', 10, // ǣ  [LATIN SMALL LETTER AE WITH MACRON]
        '\u01E4', 39, // Ǥ  [LATIN CAPITAL LETTER G WITH STROKE]
        '\u01E5', 39, // ǥ  [LATIN SMALL LETTER G WITH STROKE]
        '\u01E6', 39, // Ǧ  [LATIN CAPITAL LETTER G WITH CARON]
        '\u01E7', 39, // ǧ  [LATIN SMALL LETTER G WITH CARON]
        '\u01E8', 55, // Ǩ  [LATIN CAPITAL LETTER K WITH CARON]
        '\u01E9', 56, // ǩ  [LATIN SMALL LETTER K WITH CARON]
        '\u01EA', 77, // Ǫ  [LATIN CAPITAL LETTER O WITH OGONEK]
        '\u01EB', 78, // ǫ  [LATIN SMALL LETTER O WITH OGONEK]
        '\u01EC', 77, // Ǭ  [LATIN CAPITAL LETTER O WITH OGONEK AND MACRON]
        '\u01ED', 78, // ǭ  [LATIN SMALL LETTER O WITH OGONEK AND MACRON]
        '\u01F0', 53, // ǰ  [LATIN SMALL LETTER J WITH CARON]
        '\u01F1', 23, // Ǳ  [LATIN CAPITAL LETTER DZ]
        '\u01F2', 24, // ǲ  [LATIN CAPITAL LETTER D WITH SMALL LETTER Z]
        '\u01F3', 27, // ǳ  [LATIN SMALL LETTER DZ]
        '\u01F4', 39, // Ǵ  [LATIN CAPITAL LETTER G WITH ACUTE]
        '\u01F5', 40, // ǵ  [LATIN SMALL LETTER G WITH ACUTE]
        '\u01F6', 44, // Ƕ  http://en.wikipedia.org/wiki/Hwair  [LATIN CAPITAL LETTER HWAIR]
        '\u01F7', 120, // Ƿ  http://en.wikipedia.org/wiki/Wynn  [LATIN CAPITAL LETTER WYNN]
        '\u01F8', 71, // Ǹ  [LATIN CAPITAL LETTER N WITH GRAVE]
        '\u01F9', 72, // ǹ  [LATIN SMALL LETTER N WITH GRAVE]
        '\u01FA', 0, // Ǻ  [LATIN CAPITAL LETTER A WITH RING ABOVE AND ACUTE]
        '\u01FB', 1, // ǻ  [LATIN SMALL LETTER A WITH RING ABOVE AND ACUTE]
        '\u01FC', 3, // Ǽ  [LATIN CAPITAL LETTER AE WITH ACUTE]
        '\u01FD', 10, // ǽ  [LATIN SMALL LETTER AE WITH ACUTE]
        '\u01FE', 77, // Ǿ  [LATIN CAPITAL LETTER O WITH STROKE AND ACUTE]
        '\u01FF', 78, // ǿ  [LATIN SMALL LETTER O WITH STROKE AND ACUTE]
        '\u0200', 0, // Ȁ  [LATIN CAPITAL LETTER A WITH DOUBLE GRAVE]
        '\u0201', 1, // ȁ  [LATIN SMALL LETTER A WITH DOUBLE GRAVE]
        '\u0202', 0, // Ȃ  [LATIN CAPITAL LETTER A WITH INVERTED BREVE]
        '\u0203', 1, // ȃ  [LATIN SMALL LETTER A WITH INVERTED BREVE]
        '\u0204', 28, // Ȅ  [LATIN CAPITAL LETTER E WITH DOUBLE GRAVE]
        '\u0205', 29, // ȅ  [LATIN SMALL LETTER E WITH DOUBLE GRAVE]
        '\u0206', 28, // Ȇ  [LATIN CAPITAL LETTER E WITH INVERTED BREVE]
        '\u0207', 29, // ȇ  [LATIN SMALL LETTER E WITH INVERTED BREVE]
        '\u0208', 47, // Ȉ  [LATIN CAPITAL LETTER I WITH DOUBLE GRAVE]
        '\u0209', 48, // ȉ  [LATIN SMALL LETTER I WITH DOUBLE GRAVE]
        '\u020A', 47, // Ȋ  [LATIN CAPITAL LETTER I WITH INVERTED BREVE]
        '\u020B', 48, // ȋ  [LATIN SMALL LETTER I WITH INVERTED BREVE]
        '\u020C', 77, // Ȍ  [LATIN CAPITAL LETTER O WITH DOUBLE GRAVE]
        '\u020D', 78, // ȍ  [LATIN SMALL LETTER O WITH DOUBLE GRAVE]
        '\u020E', 77, // Ȏ  [LATIN CAPITAL LETTER O WITH INVERTED BREVE]
        '\u020F', 78, // ȏ  [LATIN SMALL LETTER O WITH INVERTED BREVE]
        '\u0210', 93, // Ȓ  [LATIN CAPITAL LETTER R WITH DOUBLE GRAVE]
        '\u0211', 94, // ȑ  [LATIN SMALL LETTER R WITH DOUBLE GRAVE]
        '\u0212', 93, // Ȓ  [LATIN CAPITAL LETTER R WITH INVERTED BREVE]
        '\u0213', 94, // ȓ  [LATIN SMALL LETTER R WITH INVERTED BREVE]
        '\u0214', 111, // Ȕ  [LATIN CAPITAL LETTER U WITH DOUBLE GRAVE]
        '\u0215', 112, // ȕ  [LATIN SMALL LETTER U WITH DOUBLE GRAVE]
        '\u0216', 111, // Ȗ  [LATIN CAPITAL LETTER U WITH INVERTED BREVE]
        '\u0217', 112, // ȗ  [LATIN SMALL LETTER U WITH INVERTED BREVE]
        '\u0218', 96, // Ș  [LATIN CAPITAL LETTER S WITH COMMA BELOW]
        '\u0219', 97, // ș  [LATIN SMALL LETTER S WITH COMMA BELOW]
        '\u021A', 102, // Ț  [LATIN CAPITAL LETTER T WITH COMMA BELOW]
        '\u021B', 103, // ț  [LATIN SMALL LETTER T WITH COMMA BELOW]
        '\u021C', 129, // Ȝ  http://en.wikipedia.org/wiki/Yogh  [LATIN CAPITAL LETTER YOGH]
        '\u021D', 130, // ȝ  http://en.wikipedia.org/wiki/Yogh  [LATIN SMALL LETTER YOGH]
        '\u021E', 42, // Ȟ  [LATIN CAPITAL LETTER H WITH CARON]
        '\u021F', 43, // ȟ  [LATIN SMALL LETTER H WITH CARON]
        '\u0220', 71, // Ƞ  [LATIN CAPITAL LETTER N WITH LONG RIGHT LEG]
        '\u0221', 22, // ȡ  [LATIN SMALL LETTER D WITH CURL]
        '\u0222', 81, // Ȣ  http://en.wikipedia.org/wiki/OU  [LATIN CAPITAL LETTER OU]
        '\u0223', 85, // ȣ  http://en.wikipedia.org/wiki/OU  [LATIN SMALL LETTER OU]
        '\u0224', 129, // Ȥ  [LATIN CAPITAL LETTER Z WITH HOOK]
        '\u0225', 130, // ȥ  [LATIN SMALL LETTER Z WITH HOOK]
        '\u0226', 0, // Ȧ  [LATIN CAPITAL LETTER A WITH DOT ABOVE]
        '\u0227', 1, // ȧ  [LATIN SMALL LETTER A WITH DOT ABOVE]
        '\u0228', 28, // Ȩ  [LATIN CAPITAL LETTER E WITH CEDILLA]
        '\u0229', 29, // ȩ  [LATIN SMALL LETTER E WITH CEDILLA]
        '\u022A', 77, // Ȫ  [LATIN CAPITAL LETTER O WITH DIAERESIS AND MACRON]
        '\u022B', 78, // ȫ  [LATIN SMALL LETTER O WITH DIAERESIS AND MACRON]
        '\u022C', 77, // Ȭ  [LATIN CAPITAL LETTER O WITH TILDE AND MACRON]
        '\u022D', 78, // ȭ  [LATIN SMALL LETTER O WITH TILDE AND MACRON]
        '\u022E', 77, // Ȯ  [LATIN CAPITAL LETTER O WITH DOT ABOVE]
        '\u022F', 78, // ȯ  [LATIN SMALL LETTER O WITH DOT ABOVE]
        '\u0230', 77, // Ȱ  [LATIN CAPITAL LETTER O WITH DOT ABOVE AND MACRON]
        '\u0231', 78, // ȱ  [LATIN SMALL LETTER O WITH DOT ABOVE AND MACRON]
        '\u0232', 126, // Ȳ  [LATIN CAPITAL LETTER Y WITH MACRON]
        '\u0233', 127, // ȳ  [LATIN SMALL LETTER Y WITH MACRON]
        '\u0234', 59, // ȴ  [LATIN SMALL LETTER L WITH CURL]
        '\u0235', 72, // ȵ  [LATIN SMALL LETTER N WITH CURL]
        '\u0236', 103, // ȶ  [LATIN SMALL LETTER T WITH CURL]
        '\u0237', 53, // ȷ  [LATIN SMALL LETTER DOTLESS J]
        '\u0238', 26, // ȸ  [LATIN SMALL LETTER DB DIGRAPH]
        '\u0239', 92, // ȹ  [LATIN SMALL LETTER QP DIGRAPH]
        '\u023A', 0, // Ⱥ  [LATIN CAPITAL LETTER A WITH STROKE]
        '\u023B', 18, // Ȼ  [LATIN CAPITAL LETTER C WITH STROKE]
        '\u023C', 19, // ȼ  [LATIN SMALL LETTER C WITH STROKE]
        '\u023D', 58, // Ƚ  [LATIN CAPITAL LETTER L WITH BAR]
        '\u023E', 102, // Ⱦ  [LATIN CAPITAL LETTER T WITH DIAGONAL STROKE]
        '\u023F', 97, // ȿ  [LATIN SMALL LETTER S WITH SWASH TAIL]
        '\u0240', 130, // ɀ  [LATIN SMALL LETTER Z WITH SWASH TAIL]
        '\u0243', 15, // Ƀ  [LATIN CAPITAL LETTER B WITH STROKE]
        '\u0244', 111, // Ʉ  [LATIN CAPITAL LETTER U BAR]
        '\u0245', 115, // Ʌ  [LATIN CAPITAL LETTER TURNED V]
        '\u0246', 28, // Ɇ  [LATIN CAPITAL LETTER E WITH STROKE]
        '\u0247', 29, // ɇ  [LATIN SMALL LETTER E WITH STROKE]
        '\u0248', 52, // Ɉ  [LATIN CAPITAL LETTER J WITH STROKE]
        '\u0249', 53, // ɉ  [LATIN SMALL LETTER J WITH STROKE]
        '\u024A', 89, // Ɋ  [LATIN CAPITAL LETTER SMALL Q WITH HOOK TAIL]
        '\u024B', 90, // ɋ  [LATIN SMALL LETTER Q WITH HOOK TAIL]
        '\u024C', 93, // Ɍ  [LATIN CAPITAL LETTER R WITH STROKE]
        '\u024D', 94, // ɍ  [LATIN SMALL LETTER R WITH STROKE]
        '\u024E', 126, // Ɏ  [LATIN CAPITAL LETTER Y WITH STROKE]
        '\u024F', 127, // ɏ  [LATIN SMALL LETTER Y WITH STROKE]
        '\u0250', 1, // ɐ  [LATIN SMALL LETTER TURNED A]
        '\u0253', 16, // ɓ  [LATIN SMALL LETTER B WITH HOOK]
        '\u0254', 78, // ɔ  [LATIN SMALL LETTER OPEN O]
        '\u0255', 19, // ɕ  [LATIN SMALL LETTER C WITH CURL]
        '\u0256', 22, // ɖ  [LATIN SMALL LETTER D WITH TAIL]
        '\u0257', 22, // ɗ  [LATIN SMALL LETTER D WITH HOOK]
        '\u0258', 29, // ɘ  [LATIN SMALL LETTER REVERSED E]
        '\u0259', 1, // ə  [LATIN SMALL LETTER SCHWA]
        '\u025A', 1, // ɚ  [LATIN SMALL LETTER SCHWA WITH HOOK]
        '\u025B', 29, // ɛ  [LATIN SMALL LETTER OPEN E]
        '\u025C', 29, // ɜ  [LATIN SMALL LETTER REVERSED OPEN E]
        '\u025D', 29, // ɝ  [LATIN SMALL LETTER REVERSED OPEN E WITH HOOK]
        '\u025E', 29, // ɞ  [LATIN SMALL LETTER CLOSED REVERSED OPEN E]
        '\u025F', 53, // ɟ  [LATIN SMALL LETTER DOTLESS J WITH STROKE]
        '\u0260', 40, // ɠ  [LATIN SMALL LETTER G WITH HOOK]
        '\u0261', 40, // ɡ  [LATIN SMALL LETTER SCRIPT G]
        '\u0262', 39, // ɢ  [LATIN LETTER SMALL CAPITAL G]
        '\u0265', 43, // ɥ  [LATIN SMALL LETTER TURNED H]
        '\u0266', 43, // ɦ  [LATIN SMALL LETTER H WITH HOOK]
        '\u0268', 48, // ɨ  [LATIN SMALL LETTER I WITH STROKE]
        '\u026A', 47, // ɪ  [LATIN LETTER SMALL CAPITAL I]
        '\u026B', 59, // ɫ  [LATIN SMALL LETTER L WITH MIDDLE TILDE]
        '\u026C', 59, // ɬ  [LATIN SMALL LETTER L WITH BELT]
        '\u026D', 59, // ɭ  [LATIN SMALL LETTER L WITH RETROFLEX HOOK]
        '\u026F', 69, // ɯ  [LATIN SMALL LETTER TURNED M]
        '\u0270', 69, // ɰ  [LATIN SMALL LETTER TURNED M WITH LONG LEG]
        '\u0271', 69, // ɱ  [LATIN SMALL LETTER M WITH HOOK]
        '\u0272', 72, // ɲ  [LATIN SMALL LETTER N WITH LEFT HOOK]
        '\u0273', 72, // ɳ  [LATIN SMALL LETTER N WITH RETROFLEX HOOK]
        '\u0274', 71, // ɴ  [LATIN LETTER SMALL CAPITAL N]
        '\u0275', 78, // ɵ  [LATIN SMALL LETTER BARRED O]
        '\u0276', 79, // ɶ  [LATIN LETTER SMALL CAPITAL OE]
        '\u027C', 94, // ɼ  [LATIN SMALL LETTER R WITH LONG LEG]
        '\u027D', 94, // ɽ  [LATIN SMALL LETTER R WITH TAIL]
        '\u027E', 94, // ɾ  [LATIN SMALL LETTER R WITH FISHHOOK]
        '\u027F', 94, // ɿ  [LATIN SMALL LETTER REVERSED R WITH FISHHOOK]
        '\u0280', 93, // ʀ  [LATIN LETTER SMALL CAPITAL R]
        '\u0281', 93, // ʁ  [LATIN LETTER SMALL CAPITAL INVERTED R]
        '\u0282', 97, // ʂ  [LATIN SMALL LETTER S WITH HOOK]
        '\u0284', 53, // ʄ  [LATIN SMALL LETTER DOTLESS J WITH STROKE AND HOOK]
        '\u0287', 103, // ʇ  [LATIN SMALL LETTER TURNED T]
        '\u0288', 103, // ʈ  [LATIN SMALL LETTER T WITH RETROFLEX HOOK]
        '\u0289', 112, // ʉ  [LATIN SMALL LETTER U BAR]
        '\u028B', 116, // ʋ  [LATIN SMALL LETTER V WITH HOOK]
        '\u028C', 116, // ʌ  [LATIN SMALL LETTER TURNED V]
        '\u028D', 121, // ʍ  [LATIN SMALL LETTER TURNED W]
        '\u028E', 127, // ʎ  [LATIN SMALL LETTER TURNED Y]
        '\u028F', 126, // ʏ  [LATIN LETTER SMALL CAPITAL Y]
        '\u0290', 130, // ʐ  [LATIN SMALL LETTER Z WITH RETROFLEX HOOK]
        '\u0291', 130, // ʑ  [LATIN SMALL LETTER Z WITH CURL]
        '\u0297', 18, // ʗ  [LATIN LETTER STRETCHED C]
        '\u0299', 15, // ʙ  [LATIN LETTER SMALL CAPITAL B]
        '\u029A', 29, // ʚ  [LATIN SMALL LETTER CLOSED OPEN E]
        '\u029B', 39, // ʛ  [LATIN LETTER SMALL CAPITAL G WITH HOOK]
        '\u029C', 42, // ʜ  [LATIN LETTER SMALL CAPITAL H]
        '\u029D', 53, // ʝ  [LATIN SMALL LETTER J WITH CROSSED-TAIL]
        '\u029E', 56, // ʞ  [LATIN SMALL LETTER TURNED K]
        '\u029F', 58, // ʟ  [LATIN LETTER SMALL CAPITAL L]
        '\u02A0', 90, // ʠ  [LATIN SMALL LETTER Q WITH HOOK]
        '\u02A3', 27, // ʣ  [LATIN SMALL LETTER DZ DIGRAPH]
        '\u02A5', 27, // ʥ  [LATIN SMALL LETTER DZ DIGRAPH WITH CURL]
        '\u02A6', 109, // ʦ  [LATIN SMALL LETTER TS DIGRAPH]
        '\u02A8', 107, // ʨ  [LATIN SMALL LETTER TC DIGRAPH WITH CURL]
        '\u02AA', 66, // ʪ  [LATIN SMALL LETTER LS DIGRAPH]
        '\u02AB', 67, // ʫ  [LATIN SMALL LETTER LZ DIGRAPH]
        '\u02AE', 43, // ʮ  [LATIN SMALL LETTER TURNED H WITH FISHHOOK]
        '\u02AF', 43, // ʯ  [LATIN SMALL LETTER TURNED H WITH FISHHOOK AND TAIL]
        '\u1D00', 0, // ᴀ  [LATIN LETTER SMALL CAPITAL A]
        '\u1D01', 3, // ᴁ  [LATIN LETTER SMALL CAPITAL AE]
        '\u1D02', 10, // ᴂ  [LATIN SMALL LETTER TURNED AE]
        '\u1D03', 15, // ᴃ  [LATIN LETTER SMALL CAPITAL BARRED B]
        '\u1D04', 18, // ᴄ  [LATIN LETTER SMALL CAPITAL C]
        '\u1D05', 21, // ᴅ  [LATIN LETTER SMALL CAPITAL D]
        '\u1D06', 21, // ᴆ  [LATIN LETTER SMALL CAPITAL ETH]
        '\u1D07', 28, // ᴇ  [LATIN LETTER SMALL CAPITAL E]
        '\u1D08', 29, // ᴈ  [LATIN SMALL LETTER TURNED OPEN E]
        '\u1D09', 48, // ᴉ  [LATIN SMALL LETTER TURNED I]
        '\u1D0A', 52, // ᴊ  [LATIN LETTER SMALL CAPITAL J]
        '\u1D0B', 55, // ᴋ  [LATIN LETTER SMALL CAPITAL K]
        '\u1D0C', 58, // ᴌ  [LATIN LETTER SMALL CAPITAL L WITH STROKE]
        '\u1D0D', 68, // ᴍ  [LATIN LETTER SMALL CAPITAL M]
        '\u1D0E', 71, // ᴎ  [LATIN LETTER SMALL CAPITAL REVERSED N]
        '\u1D0F', 77, // ᴏ  [LATIN LETTER SMALL CAPITAL O]
        '\u1D10', 77, // ᴐ  [LATIN LETTER SMALL CAPITAL OPEN O]
        '\u1D14', 83, // ᴔ  [LATIN SMALL LETTER TURNED OE]
        '\u1D15', 81, // ᴕ  [LATIN LETTER SMALL CAPITAL OU]
        '\u1D16', 78, // ᴖ  [LATIN SMALL LETTER TOP HALF O]
        '\u1D17', 78, // ᴗ  [LATIN SMALL LETTER BOTTOM HALF O]
        '\u1D18', 86, // ᴘ  [LATIN LETTER SMALL CAPITAL P]
        '\u1D19', 93, // ᴙ  [LATIN LETTER SMALL CAPITAL REVERSED R]
        '\u1D1A', 93, // ᴚ  [LATIN LETTER SMALL CAPITAL TURNED R]
        '\u1D1B', 102, // ᴛ  [LATIN LETTER SMALL CAPITAL T]
        '\u1D1C', 111, // ᴜ  [LATIN LETTER SMALL CAPITAL U]
        '\u1D20', 115, // ᴠ  [LATIN LETTER SMALL CAPITAL V]
        '\u1D21', 120, // ᴡ  [LATIN LETTER SMALL CAPITAL W]
        '\u1D22', 129, // ᴢ  [LATIN LETTER SMALL CAPITAL Z]
        '\u1D62', 48, // ᵢ  [LATIN SUBSCRIPT SMALL LETTER I]
        '\u1D63', 94, // ᵣ  [LATIN SUBSCRIPT SMALL LETTER R]
        '\u1D64', 112, // ᵤ  [LATIN SUBSCRIPT SMALL LETTER U]
        '\u1D65', 116, // ᵥ  [LATIN SUBSCRIPT SMALL LETTER V]
        '\u1D6B', 114, // ᵫ  [LATIN SMALL LETTER UE]
        '\u1D6C', 16, // ᵬ  [LATIN SMALL LETTER B WITH MIDDLE TILDE]
        '\u1D6D', 22, // ᵭ  [LATIN SMALL LETTER D WITH MIDDLE TILDE]
        '\u1D6E', 32, // ᵮ  [LATIN SMALL LETTER F WITH MIDDLE TILDE]
        '\u1D6F', 69, // ᵯ  [LATIN SMALL LETTER M WITH MIDDLE TILDE]
        '\u1D70', 72, // ᵰ  [LATIN SMALL LETTER N WITH MIDDLE TILDE]
        '\u1D71', 87, // ᵱ  [LATIN SMALL LETTER P WITH MIDDLE TILDE]
        '\u1D72', 94, // ᵲ  [LATIN SMALL LETTER R WITH MIDDLE TILDE]
        '\u1D73', 94, // ᵳ  [LATIN SMALL LETTER R WITH FISHHOOK AND MIDDLE TILDE]
        '\u1D74', 97, // ᵴ  [LATIN SMALL LETTER S WITH MIDDLE TILDE]
        '\u1D75', 103, // ᵵ  [LATIN SMALL LETTER T WITH MIDDLE TILDE]
        '\u1D76', 130, // ᵶ  [LATIN SMALL LETTER Z WITH MIDDLE TILDE]
        '\u1D77', 40, // ᵷ  [LATIN SMALL LETTER TURNED G]
        '\u1D79', 40, // ᵹ  [LATIN SMALL LETTER INSULAR G]
        '\u1D7A', 108, // ᵺ  [LATIN SMALL LETTER TH WITH STRIKETHROUGH]
        '\u1D7B', 47, // ᵻ  [LATIN SMALL CAPITAL LETTER I WITH STROKE]
        '\u1D7C', 48, // ᵼ  [LATIN SMALL LETTER IOTA WITH STROKE]
        '\u1D7D', 87, // ᵽ  [LATIN SMALL LETTER P WITH STROKE]
        '\u1D7E', 111, // ᵾ  [LATIN SMALL CAPITAL LETTER U WITH STROKE]
        '\u1D80', 16, // ᶀ  [LATIN SMALL LETTER B WITH PALATAL HOOK]
        '\u1D81', 22, // ᶁ  [LATIN SMALL LETTER D WITH PALATAL HOOK]
        '\u1D82', 32, // ᶂ  [LATIN SMALL LETTER F WITH PALATAL HOOK]
        '\u1D83', 40, // ᶃ  [LATIN SMALL LETTER G WITH PALATAL HOOK]
        '\u1D84', 56, // ᶄ  [LATIN SMALL LETTER K WITH PALATAL HOOK]
        '\u1D85', 59, // ᶅ  [LATIN SMALL LETTER L WITH PALATAL HOOK]
        '\u1D86', 69, // ᶆ  [LATIN SMALL LETTER M WITH PALATAL HOOK]
        '\u1D87', 72, // ᶇ  [LATIN SMALL LETTER N WITH PALATAL HOOK]
        '\u1D88', 87, // ᶈ  [LATIN SMALL LETTER P WITH PALATAL HOOK]
        '\u1D89', 94, // ᶉ  [LATIN SMALL LETTER R WITH PALATAL HOOK]
        '\u1D8A', 97, // ᶊ  [LATIN SMALL LETTER S WITH PALATAL HOOK]
        '\u1D8C', 116, // ᶌ  [LATIN SMALL LETTER V WITH PALATAL HOOK]
        '\u1D8D', 124, // ᶍ  [LATIN SMALL LETTER X WITH PALATAL HOOK]
        '\u1D8E', 130, // ᶎ  [LATIN SMALL LETTER Z WITH PALATAL HOOK]
        '\u1D8F', 1, // ᶏ  [LATIN SMALL LETTER A WITH RETROFLEX HOOK]
        '\u1D91', 22, // ᶑ  [LATIN SMALL LETTER D WITH HOOK AND TAIL]
        '\u1D92', 29, // ᶒ  [LATIN SMALL LETTER E WITH RETROFLEX HOOK]
        '\u1D93', 29, // ᶓ  [LATIN SMALL LETTER OPEN E WITH RETROFLEX HOOK]
        '\u1D94', 29, // ᶔ  [LATIN SMALL LETTER REVERSED OPEN E WITH RETROFLEX HOOK]
        '\u1D95', 1, // ᶕ  [LATIN SMALL LETTER SCHWA WITH RETROFLEX HOOK]
        '\u1D96', 48, // ᶖ  [LATIN SMALL LETTER I WITH RETROFLEX HOOK]
        '\u1D97', 78, // ᶗ  [LATIN SMALL LETTER OPEN O WITH RETROFLEX HOOK]
        '\u1D99', 112, // ᶙ  [LATIN SMALL LETTER U WITH RETROFLEX HOOK]
        '\u1E00', 0, // Ḁ  [LATIN CAPITAL LETTER A WITH RING BELOW]
        '\u1E01', 1, // ạ  [LATIN SMALL LETTER A WITH RING BELOW]
        '\u1E02', 15, // Ḃ  [LATIN CAPITAL LETTER B WITH DOT ABOVE]
        '\u1E03', 16, // ḃ  [LATIN SMALL LETTER B WITH DOT ABOVE]
        '\u1E04', 15, // Ḅ  [LATIN CAPITAL LETTER B WITH DOT BELOW]
        '\u1E05', 16, // ḅ  [LATIN SMALL LETTER B WITH DOT BELOW]
        '\u1E06', 15, // Ḇ  [LATIN CAPITAL LETTER B WITH LINE BELOW]
        '\u1E07', 16, // ḇ  [LATIN SMALL LETTER B WITH LINE BELOW]
        '\u1E08', 18, // Ḉ  [LATIN CAPITAL LETTER C WITH CEDILLA AND ACUTE]
        '\u1E09', 19, // ḉ  [LATIN SMALL LETTER C WITH CEDILLA AND ACUTE]
        '\u1E0A', 21, // Ḋ  [LATIN CAPITAL LETTER D WITH DOT ABOVE]
        '\u1E0B', 22, // ḋ  [LATIN SMALL LETTER D WITH DOT ABOVE]
        '\u1E0C', 21, // Ḍ  [LATIN CAPITAL LETTER D WITH DOT BELOW]
        '\u1E0D', 22, // ḍ  [LATIN SMALL LETTER D WITH DOT BELOW]
        '\u1E0E', 21, // Ḏ  [LATIN CAPITAL LETTER D WITH LINE BELOW]
        '\u1E0F', 22, // ḏ  [LATIN SMALL LETTER D WITH LINE BELOW]
        '\u1E10', 21, // Ḑ  [LATIN CAPITAL LETTER D WITH CEDILLA]
        '\u1E11', 22, // ḑ  [LATIN SMALL LETTER D WITH CEDILLA]
        '\u1E12', 21, // Ḓ  [LATIN CAPITAL LETTER D WITH CIRCUMFLEX BELOW]
        '\u1E13', 22, // ḓ  [LATIN SMALL LETTER D WITH CIRCUMFLEX BELOW]
        '\u1E14', 28, // Ḕ  [LATIN CAPITAL LETTER E WITH MACRON AND GRAVE]
        '\u1E15', 29, // ḕ  [LATIN SMALL LETTER E WITH MACRON AND GRAVE]
        '\u1E16', 28, // Ḗ  [LATIN CAPITAL LETTER E WITH MACRON AND ACUTE]
        '\u1E17', 29, // ḗ  [LATIN SMALL LETTER E WITH MACRON AND ACUTE]
        '\u1E18', 28, // Ḙ  [LATIN CAPITAL LETTER E WITH CIRCUMFLEX BELOW]
        '\u1E19', 29, // ḙ  [LATIN SMALL LETTER E WITH CIRCUMFLEX BELOW]
        '\u1E1A', 28, // Ḛ  [LATIN CAPITAL LETTER E WITH TILDE BELOW]
        '\u1E1B', 29, // ḛ  [LATIN SMALL LETTER E WITH TILDE BELOW]
        '\u1E1C', 28, // Ḝ  [LATIN CAPITAL LETTER E WITH CEDILLA AND BREVE]
        '\u1E1D', 29, // ḝ  [LATIN SMALL LETTER E WITH CEDILLA AND BREVE]
        '\u1E1E', 31, // Ḟ  [LATIN CAPITAL LETTER F WITH DOT ABOVE]
        '\u1E1F', 32, // ḟ  [LATIN SMALL LETTER F WITH DOT ABOVE]
        '\u1E20', 39, // Ḡ  [LATIN CAPITAL LETTER G WITH MACRON]
        '\u1E21', 40, // ḡ  [LATIN SMALL LETTER G WITH MACRON]
        '\u1E22', 42, // Ḣ  [LATIN CAPITAL LETTER H WITH DOT ABOVE]
        '\u1E23', 43, // ḣ  [LATIN SMALL LETTER H WITH DOT ABOVE]
        '\u1E24', 42, // Ḥ  [LATIN CAPITAL LETTER H WITH DOT BELOW]
        '\u1E25', 43, // ḥ  [LATIN SMALL LETTER H WITH DOT BELOW]
        '\u1E26', 42, // Ḧ  [LATIN CAPITAL LETTER H WITH DIAERESIS]
        '\u1E27', 43, // ḧ  [LATIN SMALL LETTER H WITH DIAERESIS]
        '\u1E28', 42, // Ḩ  [LATIN CAPITAL LETTER H WITH CEDILLA]
        '\u1E29', 43, // ḩ  [LATIN SMALL LETTER H WITH CEDILLA]
        '\u1E2A', 42, // Ḫ  [LATIN CAPITAL LETTER H WITH BREVE BELOW]
        '\u1E2B', 43, // ḫ  [LATIN SMALL LETTER H WITH BREVE BELOW]
        '\u1E2C', 47, // Ḭ  [LATIN CAPITAL LETTER I WITH TILDE BELOW]
        '\u1E2D', 48, // ḭ  [LATIN SMALL LETTER I WITH TILDE BELOW]
        '\u1E2E', 47, // Ḯ  [LATIN CAPITAL LETTER I WITH DIAERESIS AND ACUTE]
        '\u1E2F', 48, // ḯ  [LATIN SMALL LETTER I WITH DIAERESIS AND ACUTE]
        '\u1E30', 55, // Ḱ  [LATIN CAPITAL LETTER K WITH ACUTE]
        '\u1E31', 56, // ḱ  [LATIN SMALL LETTER K WITH ACUTE]
        '\u1E32', 55, // Ḳ  [LATIN CAPITAL LETTER K WITH DOT BELOW]
        '\u1E33', 56, // ḳ  [LATIN SMALL LETTER K WITH DOT BELOW]
        '\u1E34', 55, // Ḵ  [LATIN CAPITAL LETTER K WITH LINE BELOW]
        '\u1E35', 56, // ḵ  [LATIN SMALL LETTER K WITH LINE BELOW]
        '\u1E36', 58, // Ḷ  [LATIN CAPITAL LETTER L WITH DOT BELOW]
        '\u1E37', 59, // ḷ  [LATIN SMALL LETTER L WITH DOT BELOW]
        '\u1E38', 58, // Ḹ  [LATIN CAPITAL LETTER L WITH DOT BELOW AND MACRON]
        '\u1E39', 59, // ḹ  [LATIN SMALL LETTER L WITH DOT BELOW AND MACRON]
        '\u1E3A', 58, // Ḻ  [LATIN CAPITAL LETTER L WITH LINE BELOW]
        '\u1E3B', 59, // ḻ  [LATIN SMALL LETTER L WITH LINE BELOW]
        '\u1E3C', 58, // Ḽ  [LATIN CAPITAL LETTER L WITH CIRCUMFLEX BELOW]
        '\u1E3D', 59, // ḽ  [LATIN SMALL LETTER L WITH CIRCUMFLEX BELOW]
        '\u1E3E', 68, // Ḿ  [LATIN CAPITAL LETTER M WITH ACUTE]
        '\u1E3F', 69, // ḿ  [LATIN SMALL LETTER M WITH ACUTE]
        '\u1E40', 68, // Ṁ  [LATIN CAPITAL LETTER M WITH DOT ABOVE]
        '\u1E41', 69, // ṁ  [LATIN SMALL LETTER M WITH DOT ABOVE]
        '\u1E42', 68, // Ṃ  [LATIN CAPITAL LETTER M WITH DOT BELOW]
        '\u1E43', 69, // ṃ  [LATIN SMALL LETTER M WITH DOT BELOW]
        '\u1E44', 71, // Ṅ  [LATIN CAPITAL LETTER N WITH DOT ABOVE]
        '\u1E45', 72, // ṅ  [LATIN SMALL LETTER N WITH DOT ABOVE]
        '\u1E46', 71, // Ṇ  [LATIN CAPITAL LETTER N WITH DOT BELOW]
        '\u1E47', 72, // ṇ  [LATIN SMALL LETTER N WITH DOT BELOW]
        '\u1E48', 71, // Ṉ  [LATIN CAPITAL LETTER N WITH LINE BELOW]
        '\u1E49', 72, // ṉ  [LATIN SMALL LETTER N WITH LINE BELOW]
        '\u1E4A', 71, // Ṋ  [LATIN CAPITAL LETTER N WITH CIRCUMFLEX BELOW]
        '\u1E4B', 72, // ṋ  [LATIN SMALL LETTER N WITH CIRCUMFLEX BELOW]
        '\u1E4C', 77, // Ṍ  [LATIN CAPITAL LETTER O WITH TILDE AND ACUTE]
        '\u1E4D', 78, // ṍ  [LATIN SMALL LETTER O WITH TILDE AND ACUTE]
        '\u1E4E', 77, // Ṏ  [LATIN CAPITAL LETTER O WITH TILDE AND DIAERESIS]
        '\u1E4F', 78, // ṏ  [LATIN SMALL LETTER O WITH TILDE AND DIAERESIS]
        '\u1E50', 77, // Ṑ  [LATIN CAPITAL LETTER O WITH MACRON AND GRAVE]
        '\u1E51', 78, // ṑ  [LATIN SMALL LETTER O WITH MACRON AND GRAVE]
        '\u1E52', 77, // Ṓ  [LATIN CAPITAL LETTER O WITH MACRON AND ACUTE]
        '\u1E53', 78, // ṓ  [LATIN SMALL LETTER O WITH MACRON AND ACUTE]
        '\u1E54', 86, // Ṕ  [LATIN CAPITAL LETTER P WITH ACUTE]
        '\u1E55', 87, // ṕ  [LATIN SMALL LETTER P WITH ACUTE]
        '\u1E56', 86, // Ṗ  [LATIN CAPITAL LETTER P WITH DOT ABOVE]
        '\u1E57', 87, // ṗ  [LATIN SMALL LETTER P WITH DOT ABOVE]
        '\u1E58', 93, // Ṙ  [LATIN CAPITAL LETTER R WITH DOT ABOVE]
        '\u1E59', 94, // ṙ  [LATIN SMALL LETTER R WITH DOT ABOVE]
        '\u1E5A', 93, // Ṛ  [LATIN CAPITAL LETTER R WITH DOT BELOW]
        '\u1E5B', 94, // ṛ  [LATIN SMALL LETTER R WITH DOT BELOW]
        '\u1E5C', 93, // Ṝ  [LATIN CAPITAL LETTER R WITH DOT BELOW AND MACRON]
        '\u1E5D', 94, // ṝ  [LATIN SMALL LETTER R WITH DOT BELOW AND MACRON]
        '\u1E5E', 93, // Ṟ  [LATIN CAPITAL LETTER R WITH LINE BELOW]
        '\u1E5F', 94, // ṟ  [LATIN SMALL LETTER R WITH LINE BELOW]
        '\u1E60', 96, // Ṡ  [LATIN CAPITAL LETTER S WITH DOT ABOVE]
        '\u1E61', 97, // ṡ  [LATIN SMALL LETTER S WITH DOT ABOVE]
        '\u1E62', 96, // Ṣ  [LATIN CAPITAL LETTER S WITH DOT BELOW]
        '\u1E63', 97, // ṣ  [LATIN SMALL LETTER S WITH DOT BELOW]
        '\u1E64', 96, // Ṥ  [LATIN CAPITAL LETTER S WITH ACUTE AND DOT ABOVE]
        '\u1E65', 97, // ṥ  [LATIN SMALL LETTER S WITH ACUTE AND DOT ABOVE]
        '\u1E66', 96, // Ṧ  [LATIN CAPITAL LETTER S WITH CARON AND DOT ABOVE]
        '\u1E67', 97, // ṧ  [LATIN SMALL LETTER S WITH CARON AND DOT ABOVE]
        '\u1E68', 96, // Ṩ  [LATIN CAPITAL LETTER S WITH DOT BELOW AND DOT ABOVE]
        '\u1E69', 97, // ṩ  [LATIN SMALL LETTER S WITH DOT BELOW AND DOT ABOVE]
        '\u1E6A', 102, // Ṫ  [LATIN CAPITAL LETTER T WITH DOT ABOVE]
        '\u1E6B', 103, // ṫ  [LATIN SMALL LETTER T WITH DOT ABOVE]
        '\u1E6C', 102, // Ṭ  [LATIN CAPITAL LETTER T WITH DOT BELOW]
        '\u1E6D', 103, // ṭ  [LATIN SMALL LETTER T WITH DOT BELOW]
        '\u1E6E', 102, // Ṯ  [LATIN CAPITAL LETTER T WITH LINE BELOW]
        '\u1E6F', 103, // ṯ  [LATIN SMALL LETTER T WITH LINE BELOW]
        '\u1E70', 102, // Ṱ  [LATIN CAPITAL LETTER T WITH CIRCUMFLEX BELOW]
        '\u1E71', 103, // ṱ  [LATIN SMALL LETTER T WITH CIRCUMFLEX BELOW]
        '\u1E72', 111, // Ṳ  [LATIN CAPITAL LETTER U WITH DIAERESIS BELOW]
        '\u1E73', 112, // ṳ  [LATIN SMALL LETTER U WITH DIAERESIS BELOW]
        '\u1E74', 111, // Ṵ  [LATIN CAPITAL LETTER U WITH TILDE BELOW]
        '\u1E75', 112, // ṵ  [LATIN SMALL LETTER U WITH TILDE BELOW]
        '\u1E76', 111, // Ṷ  [LATIN CAPITAL LETTER U WITH CIRCUMFLEX BELOW]
        '\u1E77', 112, // ṷ  [LATIN SMALL LETTER U WITH CIRCUMFLEX BELOW]
        '\u1E78', 111, // Ṹ  [LATIN CAPITAL LETTER U WITH TILDE AND ACUTE]
        '\u1E79', 112, // ṹ  [LATIN SMALL LETTER U WITH TILDE AND ACUTE]
        '\u1E7A', 111, // Ṻ  [LATIN CAPITAL LETTER U WITH MACRON AND DIAERESIS]
        '\u1E7B', 112, // ṻ  [LATIN SMALL LETTER U WITH MACRON AND DIAERESIS]
        '\u1E7C', 115, // Ṽ  [LATIN CAPITAL LETTER V WITH TILDE]
        '\u1E7D', 116, // ṽ  [LATIN SMALL LETTER V WITH TILDE]
        '\u1E7E', 115, // Ṿ  [LATIN CAPITAL LETTER V WITH DOT BELOW]
        '\u1E7F', 116, // ṿ  [LATIN SMALL LETTER V WITH DOT BELOW]
        '\u1E80', 120, // Ẁ  [LATIN CAPITAL LETTER W WITH GRAVE]
        '\u1E81', 121, // ẁ  [LATIN SMALL LETTER W WITH GRAVE]
        '\u1E82', 120, // Ẃ  [LATIN CAPITAL LETTER W WITH ACUTE]
        '\u1E83', 121, // ẃ  [LATIN SMALL LETTER W WITH ACUTE]
        '\u1E84', 120, // Ẅ  [LATIN CAPITAL LETTER W WITH DIAERESIS]
        '\u1E85', 121, // ẅ  [LATIN SMALL LETTER W WITH DIAERESIS]
        '\u1E86', 120, // Ẇ  [LATIN CAPITAL LETTER W WITH DOT ABOVE]
        '\u1E87', 121, // ẇ  [LATIN SMALL LETTER W WITH DOT ABOVE]
        '\u1E88', 120, // Ẉ  [LATIN CAPITAL LETTER W WITH DOT BELOW]
        '\u1E89', 121, // ẉ  [LATIN SMALL LETTER W WITH DOT BELOW]
        '\u1E8A', 123, // Ẋ  [LATIN CAPITAL LETTER X WITH DOT ABOVE]
        '\u1E8B', 124, // ẋ  [LATIN SMALL LETTER X WITH DOT ABOVE]
        '\u1E8C', 123, // Ẍ  [LATIN CAPITAL LETTER X WITH DIAERESIS]
        '\u1E8D', 124, // ẍ  [LATIN SMALL LETTER X WITH DIAERESIS]
        '\u1E8E', 126, // Ẏ  [LATIN CAPITAL LETTER Y WITH DOT ABOVE]
        '\u1E8F', 127, // ẏ  [LATIN SMALL LETTER Y WITH DOT ABOVE]
        '\u1E90', 129, // Ẑ  [LATIN CAPITAL LETTER Z WITH CIRCUMFLEX]
        '\u1E91', 130, // ẑ  [LATIN SMALL LETTER Z WITH CIRCUMFLEX]
        '\u1E92', 129, // Ẓ  [LATIN CAPITAL LETTER Z WITH DOT BELOW]
        '\u1E93', 130, // ẓ  [LATIN SMALL LETTER Z WITH DOT BELOW]
        '\u1E94', 129, // Ẕ  [LATIN CAPITAL LETTER Z WITH LINE BELOW]
        '\u1E95', 130, // ẕ  [LATIN SMALL LETTER Z WITH LINE BELOW]
        '\u1E96', 43, // ẖ  [LATIN SMALL LETTER H WITH LINE BELOW]
        '\u1E97', 103, // ẗ  [LATIN SMALL LETTER T WITH DIAERESIS]
        '\u1E98', 121, // ẘ  [LATIN SMALL LETTER W WITH RING ABOVE]
        '\u1E99', 127, // ẙ  [LATIN SMALL LETTER Y WITH RING ABOVE]
        '\u1E9A', 1, // ả  [LATIN SMALL LETTER A WITH RIGHT HALF RING]
        '\u1E9B', 32, // ẛ  [LATIN SMALL LETTER LONG S WITH DOT ABOVE]
        '\u1E9C', 97, // ẜ  [LATIN SMALL LETTER LONG S WITH DIAGONAL STROKE]
        '\u1E9D', 97, // ẝ  [LATIN SMALL LETTER LONG S WITH HIGH STROKE]
        '\u1E9E', 98, // ẞ  [LATIN CAPITAL LETTER SHARP S]
        '\u1EA0', 0, // Ạ  [LATIN CAPITAL LETTER A WITH DOT BELOW]
        '\u1EA1', 1, // ạ  [LATIN SMALL LETTER A WITH DOT BELOW]
        '\u1EA2', 0, // Ả  [LATIN CAPITAL LETTER A WITH HOOK ABOVE]
        '\u1EA3', 1, // ả  [LATIN SMALL LETTER A WITH HOOK ABOVE]
        '\u1EA4', 0, // Ấ  [LATIN CAPITAL LETTER A WITH CIRCUMFLEX AND ACUTE]
        '\u1EA5', 1, // ấ  [LATIN SMALL LETTER A WITH CIRCUMFLEX AND ACUTE]
        '\u1EA6', 0, // Ầ  [LATIN CAPITAL LETTER A WITH CIRCUMFLEX AND GRAVE]
        '\u1EA7', 1, // ầ  [LATIN SMALL LETTER A WITH CIRCUMFLEX AND GRAVE]
        '\u1EA8', 0, // Ẩ  [LATIN CAPITAL LETTER A WITH CIRCUMFLEX AND HOOK ABOVE]
        '\u1EA9', 1, // ẩ  [LATIN SMALL LETTER A WITH CIRCUMFLEX AND HOOK ABOVE]
        '\u1EAA', 0, // Ẫ  [LATIN CAPITAL LETTER A WITH CIRCUMFLEX AND TILDE]
        '\u1EAB', 1, // ẫ  [LATIN SMALL LETTER A WITH CIRCUMFLEX AND TILDE]
        '\u1EAC', 0, // Ậ  [LATIN CAPITAL LETTER A WITH CIRCUMFLEX AND DOT BELOW]
        '\u1EAD', 1, // ậ  [LATIN SMALL LETTER A WITH CIRCUMFLEX AND DOT BELOW]
        '\u1EAE', 0, // Ắ  [LATIN CAPITAL LETTER A WITH BREVE AND ACUTE]
        '\u1EAF', 1, // ắ  [LATIN SMALL LETTER A WITH BREVE AND ACUTE]
        '\u1EB0', 0, // Ằ  [LATIN CAPITAL LETTER A WITH BREVE AND GRAVE]
        '\u1EB1', 1, // ằ  [LATIN SMALL LETTER A WITH BREVE AND GRAVE]
        '\u1EB2', 0, // Ẳ  [LATIN CAPITAL LETTER A WITH BREVE AND HOOK ABOVE]
        '\u1EB3', 1, // ẳ  [LATIN SMALL LETTER A WITH BREVE AND HOOK ABOVE]
        '\u1EB4', 0, // Ẵ  [LATIN CAPITAL LETTER A WITH BREVE AND TILDE]
        '\u1EB5', 1, // ẵ  [LATIN SMALL LETTER A WITH BREVE AND TILDE]
        '\u1EB6', 0, // Ặ  [LATIN CAPITAL LETTER A WITH BREVE AND DOT BELOW]
        '\u1EB7', 1, // ặ  [LATIN SMALL LETTER A WITH BREVE AND DOT BELOW]
        '\u1EB8', 28, // Ẹ  [LATIN CAPITAL LETTER E WITH DOT BELOW]
        '\u1EB9', 29, // ẹ  [LATIN SMALL LETTER E WITH DOT BELOW]
        '\u1EBA', 28, // Ẻ  [LATIN CAPITAL LETTER E WITH HOOK ABOVE]
        '\u1EBB', 29, // ẻ  [LATIN SMALL LETTER E WITH HOOK ABOVE]
        '\u1EBC', 28, // Ẽ  [LATIN CAPITAL LETTER E WITH TILDE]
        '\u1EBD', 29, // ẽ  [LATIN SMALL LETTER E WITH TILDE]
        '\u1EBE', 28, // Ế  [LATIN CAPITAL LETTER E WITH CIRCUMFLEX AND ACUTE]
        '\u1EBF', 29, // ế  [LATIN SMALL LETTER E WITH CIRCUMFLEX AND ACUTE]
        '\u1EC0', 28, // Ề  [LATIN CAPITAL LETTER E WITH CIRCUMFLEX AND GRAVE]
        '\u1EC1', 29, // ề  [LATIN SMALL LETTER E WITH CIRCUMFLEX AND GRAVE]
        '\u1EC2', 28, // Ể  [LATIN CAPITAL LETTER E WITH CIRCUMFLEX AND HOOK ABOVE]
        '\u1EC3', 29, // ể  [LATIN SMALL LETTER E WITH CIRCUMFLEX AND HOOK ABOVE]
        '\u1EC4', 28, // Ễ  [LATIN CAPITAL LETTER E WITH CIRCUMFLEX AND TILDE]
        '\u1EC5', 29, // ễ  [LATIN SMALL LETTER E WITH CIRCUMFLEX AND TILDE]
        '\u1EC6', 28, // Ệ  [LATIN CAPITAL LETTER E WITH CIRCUMFLEX AND DOT BELOW]
        '\u1EC7', 29, // ệ  [LATIN SMALL LETTER E WITH CIRCUMFLEX AND DOT BELOW]
        '\u1EC8', 47, // Ỉ  [LATIN CAPITAL LETTER I WITH HOOK ABOVE]
        '\u1EC9', 48, // ỉ  [LATIN SMALL LETTER I WITH HOOK ABOVE]
        '\u1ECA', 47, // Ị  [LATIN CAPITAL LETTER I WITH DOT BELOW]
        '\u1ECB', 48, // ị  [LATIN SMALL LETTER I WITH DOT BELOW]
        '\u1ECC', 77, // Ọ  [LATIN CAPITAL LETTER O WITH DOT BELOW]
        '\u1ECD', 78, // ọ  [LATIN SMALL LETTER O WITH DOT BELOW]
        '\u1ECE', 77, // Ỏ  [LATIN CAPITAL LETTER O WITH HOOK ABOVE]
        '\u1ECF', 78, // ỏ  [LATIN SMALL LETTER O WITH HOOK ABOVE]
        '\u1ED0', 77, // Ố  [LATIN CAPITAL LETTER O WITH CIRCUMFLEX AND ACUTE]
        '\u1ED1', 78, // ố  [LATIN SMALL LETTER O WITH CIRCUMFLEX AND ACUTE]
        '\u1ED2', 77, // Ồ  [LATIN CAPITAL LETTER O WITH CIRCUMFLEX AND GRAVE]
        '\u1ED3', 78, // ồ  [LATIN SMALL LETTER O WITH CIRCUMFLEX AND GRAVE]
        '\u1ED4', 77, // Ổ  [LATIN CAPITAL LETTER O WITH CIRCUMFLEX AND HOOK ABOVE]
        '\u1ED5', 78, // ổ  [LATIN SMALL LETTER O WITH CIRCUMFLEX AND HOOK ABOVE]
        '\u1ED6', 77, // Ỗ  [LATIN CAPITAL LETTER O WITH CIRCUMFLEX AND TILDE]
        '\u1ED7', 78, // ỗ  [LATIN SMALL LETTER O WITH CIRCUMFLEX AND TILDE]
        '\u1ED8', 77, // Ộ  [LATIN CAPITAL LETTER O WITH CIRCUMFLEX AND DOT BELOW]
        '\u1ED9', 78, // ộ  [LATIN SMALL LETTER O WITH CIRCUMFLEX AND DOT BELOW]
        '\u1EDA', 77, // Ớ  [LATIN CAPITAL LETTER O WITH HORN AND ACUTE]
        '\u1EDB', 78, // ớ  [LATIN SMALL LETTER O WITH HORN AND ACUTE]
        '\u1EDC', 77, // Ờ  [LATIN CAPITAL LETTER O WITH HORN AND GRAVE]
        '\u1EDD', 78, // ờ  [LATIN SMALL LETTER O WITH HORN AND GRAVE]
        '\u1EDE', 77, // Ở  [LATIN CAPITAL LETTER O WITH HORN AND HOOK ABOVE]
        '\u1EDF', 78, // ở  [LATIN SMALL LETTER O WITH HORN AND HOOK ABOVE]
        '\u1EE0', 77, // Ỡ  [LATIN CAPITAL LETTER O WITH HORN AND TILDE]
        '\u1EE1', 78, // ỡ  [LATIN SMALL LETTER O WITH HORN AND TILDE]
        '\u1EE2', 77, // Ợ  [LATIN CAPITAL LETTER O WITH HORN AND DOT BELOW]
        '\u1EE3', 78, // ợ  [LATIN SMALL LETTER O WITH HORN AND DOT BELOW]
        '\u1EE4', 111, // Ụ  [LATIN CAPITAL LETTER U WITH DOT BELOW]
        '\u1EE5', 112, // ụ  [LATIN SMALL LETTER U WITH DOT BELOW]
        '\u1EE6', 111, // Ủ  [LATIN CAPITAL LETTER U WITH HOOK ABOVE]
        '\u1EE7', 112, // ủ  [LATIN SMALL LETTER U WITH HOOK ABOVE]
        '\u1EE8', 111, // Ứ  [LATIN CAPITAL LETTER U WITH HORN AND ACUTE]
        '\u1EE9', 112, // ứ  [LATIN SMALL LETTER U WITH HORN AND ACUTE]
        '\u1EEA', 111, // Ừ  [LATIN CAPITAL LETTER U WITH HORN AND GRAVE]
        '\u1EEB', 112, // ừ  [LATIN SMALL LETTER U WITH HORN AND GRAVE]
        '\u1EEC', 111, // Ử  [LATIN CAPITAL LETTER U WITH HORN AND HOOK ABOVE]
        '\u1EED', 112, // ử  [LATIN SMALL LETTER U WITH HORN AND HOOK ABOVE]
        '\u1EEE', 111, // Ữ  [LATIN CAPITAL LETTER U WITH HORN AND TILDE]
        '\u1EEF', 112, // ữ  [LATIN SMALL LETTER U WITH HORN AND TILDE]
        '\u1EF0', 111, // Ự  [LATIN CAPITAL LETTER U WITH HORN AND DOT BELOW]
        '\u1EF1', 112, // ự  [LATIN SMALL LETTER U WITH HORN AND DOT BELOW]
        '\u1EF2', 126, // Ỳ  [LATIN CAPITAL LETTER Y WITH GRAVE]
        '\u1EF3', 127, // ỳ  [LATIN SMALL LETTER Y WITH GRAVE]
        '\u1EF4', 126, // Ỵ  [LATIN CAPITAL LETTER Y WITH DOT BELOW]
        '\u1EF5', 127, // ỵ  [LATIN SMALL LETTER Y WITH DOT BELOW]
        '\u1EF6', 126, // Ỷ  [LATIN CAPITAL LETTER Y WITH HOOK ABOVE]
        '\u1EF7', 127, // ỷ  [LATIN SMALL LETTER Y WITH HOOK ABOVE]
        '\u1EF8', 126, // Ỹ  [LATIN CAPITAL LETTER Y WITH TILDE]
        '\u1EF9', 127, // ỹ  [LATIN SMALL LETTER Y WITH TILDE]
        '\u1EFA', 61, // Ỻ  [LATIN CAPITAL LETTER MIDDLE-WELSH LL]
        '\u1EFB', 65, // ỻ  [LATIN SMALL LETTER MIDDLE-WELSH LL]
        '\u1EFC', 115, // Ỽ  [LATIN CAPITAL LETTER MIDDLE-WELSH V]
        '\u1EFE', 126, // Ỿ  [LATIN CAPITAL LETTER Y WITH LOOP]
        '\u1EFF', 127, // ỿ  [LATIN SMALL LETTER Y WITH LOOP]
        '\u2010', 195, // ‐  [HYPHEN]
        '\u2011', 195, // ‑  [NON-BREAKING HYPHEN]
        '\u2012', 195, // ‒  [FIGURE DASH]
        '\u2013', 195, // –  [EN DASH]
        '\u2014', 195, // —  [EM DASH]
        '\u2018', 194, // ‘  [LEFT SINGLE QUOTATION MARK]
        '\u2019', 194, // ’  [RIGHT SINGLE QUOTATION MARK]
        '\u201A', 194, // ‚  [SINGLE LOW-9 QUOTATION MARK]
        '\u201B', 194, // ‛  [SINGLE HIGH-REVERSED-9 QUOTATION MARK]
        '\u201C', 193, // “  [LEFT DOUBLE QUOTATION MARK]
        '\u201D', 193, // ”  [RIGHT DOUBLE QUOTATION MARK]
        '\u201E', 193, // „  [DOUBLE LOW-9 QUOTATION MARK]
        '\u2032', 194, // ′  [PRIME]
        '\u2033', 193, // ″  [DOUBLE PRIME]
        '\u2035', 194, // ‵  [REVERSED PRIME]
        '\u2036', 193, // ‶  [REVERSED DOUBLE PRIME]
        '\u2038', 226, // ‸  [CARET]
        '\u2039', 194, // ‹  [SINGLE LEFT-POINTING ANGLE QUOTATION MARK]
        '\u203A', 194, // ›  [SINGLE RIGHT-POINTING ANGLE QUOTATION MARK]
        '\u203C', 209, // ‼  [DOUBLE EXCLAMATION MARK]
        '\u2044', 218, // ⁄  [FRACTION SLASH]
        '\u2045', 196, // ⁅  [LEFT SQUARE BRACKET WITH QUILL]
        '\u2046', 197, // ⁆  [RIGHT SQUARE BRACKET WITH QUILL]
        '\u2047', 222, // ⁇  [DOUBLE QUESTION MARK]
        '\u2048', 223, // ⁈  [QUESTION EXCLAMATION MARK]
        '\u2049', 210, // ⁉  [EXCLAMATION QUESTION MARK]
        '\u204E', 215, // ⁎  [LOW ASTERISK]
        '\u204F', 220, // ⁏  [REVERSED SEMICOLON]
        '\u2052', 213, // ⁒  [COMMERCIAL MINUS SIGN]
        '\u2053', 228, // ⁓  [SWUNG DASH]
        '\u2070', 132, // ⁰  [SUPERSCRIPT ZERO]
        '\u2071', 48, // ⁱ  [SUPERSCRIPT LATIN SMALL LETTER I]
        '\u2074', 142, // ⁴  [SUPERSCRIPT FOUR]
        '\u2075', 145, // ⁵  [SUPERSCRIPT FIVE]
        '\u2076', 148, // ⁶  [SUPERSCRIPT SIX]
        '\u2077', 151, // ⁷  [SUPERSCRIPT SEVEN]
        '\u2078', 154, // ⁸  [SUPERSCRIPT EIGHT]
        '\u2079', 157, // ⁹  [SUPERSCRIPT NINE]
        '\u207A', 206, // ⁺  [SUPERSCRIPT PLUS SIGN]
        '\u207B', 195, // ⁻  [SUPERSCRIPT MINUS]
        '\u207C', 207, // ⁼  [SUPERSCRIPT EQUALS SIGN]
        '\u207D', 198, // ⁽  [SUPERSCRIPT LEFT PARENTHESIS]
        '\u207E', 200, // ⁾  [SUPERSCRIPT RIGHT PARENTHESIS]
        '\u207F', 72, // ⁿ  [SUPERSCRIPT LATIN SMALL LETTER N]
        '\u2080', 132, // ₀  [SUBSCRIPT ZERO]
        '\u2081', 133, // ₁  [SUBSCRIPT ONE]
        '\u2082', 136, // ₂  [SUBSCRIPT TWO]
        '\u2083', 139, // ₃  [SUBSCRIPT THREE]
        '\u2084', 142, // ₄  [SUBSCRIPT FOUR]
        '\u2085', 145, // ₅  [SUBSCRIPT FIVE]
        '\u2086', 148, // ₆  [SUBSCRIPT SIX]
        '\u2087', 151, // ₇  [SUBSCRIPT SEVEN]
        '\u2088', 154, // ₈  [SUBSCRIPT EIGHT]
        '\u2089', 157, // ₉  [SUBSCRIPT NINE]
        '\u208A', 206, // ₊  [SUBSCRIPT PLUS SIGN]
        '\u208B', 195, // ₋  [SUBSCRIPT MINUS]
        '\u208C', 207, // ₌  [SUBSCRIPT EQUALS SIGN]
        '\u208D', 198, // ₍  [SUBSCRIPT LEFT PARENTHESIS]
        '\u208E', 200, // ₎  [SUBSCRIPT RIGHT PARENTHESIS]
        '\u2090', 1, // ₐ  [LATIN SUBSCRIPT SMALL LETTER A]
        '\u2091', 29, // ₑ  [LATIN SUBSCRIPT SMALL LETTER E]
        '\u2092', 78, // ₒ  [LATIN SUBSCRIPT SMALL LETTER O]
        '\u2093', 124, // ₓ  [LATIN SUBSCRIPT SMALL LETTER X]
        '\u2094', 1, // ₔ  [LATIN SUBSCRIPT SMALL LETTER SCHWA]
        '\u2184', 19, // ↄ  [LATIN SMALL LETTER REVERSED C]
        '\u2460', 133, // ①  [CIRCLED DIGIT ONE]
        '\u2461', 136, // ②  [CIRCLED DIGIT TWO]
        '\u2462', 139, // ③  [CIRCLED DIGIT THREE]
        '\u2463', 142, // ④  [CIRCLED DIGIT FOUR]
        '\u2464', 145, // ⑤  [CIRCLED DIGIT FIVE]
        '\u2465', 148, // ⑥  [CIRCLED DIGIT SIX]
        '\u2466', 151, // ⑦  [CIRCLED DIGIT SEVEN]
        '\u2467', 154, // ⑧  [CIRCLED DIGIT EIGHT]
        '\u2468', 157, // ⑨  [CIRCLED DIGIT NINE]
        '\u2469', 160, // ⑩  [CIRCLED NUMBER TEN]
        '\u246A', 163, // ⑪  [CIRCLED NUMBER ELEVEN]
        '\u246B', 166, // ⑫  [CIRCLED NUMBER TWELVE]
        '\u246C', 169, // ⑬  [CIRCLED NUMBER THIRTEEN]
        '\u246D', 172, // ⑭  [CIRCLED NUMBER FOURTEEN]
        '\u246E', 175, // ⑮  [CIRCLED NUMBER FIFTEEN]
        '\u246F', 178, // ⑯  [CIRCLED NUMBER SIXTEEN]
        '\u2470', 181, // ⑰  [CIRCLED NUMBER SEVENTEEN]
        '\u2471', 184, // ⑱  [CIRCLED NUMBER EIGHTEEN]
        '\u2472', 187, // ⑲  [CIRCLED NUMBER NINETEEN]
        '\u2473', 190, // ⑳  [CIRCLED NUMBER TWENTY]
        '\u2474', 135, // ⑴  [PARENTHESIZED DIGIT ONE]
        '\u2475', 138, // ⑵  [PARENTHESIZED DIGIT TWO]
        '\u2476', 141, // ⑶  [PARENTHESIZED DIGIT THREE]
        '\u2477', 144, // ⑷  [PARENTHESIZED DIGIT FOUR]
        '\u2478', 147, // ⑸  [PARENTHESIZED DIGIT FIVE]
        '\u2479', 150, // ⑹  [PARENTHESIZED DIGIT SIX]
        '\u247A', 153, // ⑺  [PARENTHESIZED DIGIT SEVEN]
        '\u247B', 156, // ⑻  [PARENTHESIZED DIGIT EIGHT]
        '\u247C', 159, // ⑼  [PARENTHESIZED DIGIT NINE]
        '\u247D', 162, // ⑽  [PARENTHESIZED NUMBER TEN]
        '\u247E', 165, // ⑾  [PARENTHESIZED NUMBER ELEVEN]
        '\u247F', 168, // ⑿  [PARENTHESIZED NUMBER TWELVE]
        '\u2480', 171, // ⒀  [PARENTHESIZED NUMBER THIRTEEN]
        '\u2481', 174, // ⒁  [PARENTHESIZED NUMBER FOURTEEN]
        '\u2482', 177, // ⒂  [PARENTHESIZED NUMBER FIFTEEN]
        '\u2483', 180, // ⒃  [PARENTHESIZED NUMBER SIXTEEN]
        '\u2484', 183, // ⒄  [PARENTHESIZED NUMBER SEVENTEEN]
        '\u2485', 186, // ⒅  [PARENTHESIZED NUMBER EIGHTEEN]
        '\u2486', 189, // ⒆  [PARENTHESIZED NUMBER NINETEEN]
        '\u2487', 192, // ⒇  [PARENTHESIZED NUMBER TWENTY]
        '\u2488', 134, // ⒈  [DIGIT ONE FULL STOP]
        '\u2489', 137, // ⒉  [DIGIT TWO FULL STOP]
        '\u248A', 140, // ⒊  [DIGIT THREE FULL STOP]
        '\u248B', 143, // ⒋  [DIGIT FOUR FULL STOP]
        '\u248C', 146, // ⒌  [DIGIT FIVE FULL STOP]
        '\u248D', 149, // ⒍  [DIGIT SIX FULL STOP]
        '\u248E', 152, // ⒎  [DIGIT SEVEN FULL STOP]
        '\u248F', 155, // ⒏  [DIGIT EIGHT FULL STOP]
        '\u2490', 158, // ⒐  [DIGIT NINE FULL STOP]
        '\u2491', 161, // ⒑  [NUMBER TEN FULL STOP]
        '\u2492', 164, // ⒒  [NUMBER ELEVEN FULL STOP]
        '\u2493', 167, // ⒓  [NUMBER TWELVE FULL STOP]
        '\u2494', 170, // ⒔  [NUMBER THIRTEEN FULL STOP]
        '\u2495', 173, // ⒕  [NUMBER FOURTEEN FULL STOP]
        '\u2496', 176, // ⒖  [NUMBER FIFTEEN FULL STOP]
        '\u2497', 179, // ⒗  [NUMBER SIXTEEN FULL STOP]
        '\u2498', 182, // ⒘  [NUMBER SEVENTEEN FULL STOP]
        '\u2499', 185, // ⒙  [NUMBER EIGHTEEN FULL STOP]
        '\u249A', 188, // ⒚  [NUMBER NINETEEN FULL STOP]
        '\u249B', 191, // ⒛  [NUMBER TWENTY FULL STOP]
        '\u249C', 8, // ⒜  [PARENTHESIZED LATIN SMALL LETTER A]
        '\u249D', 17, // ⒝  [PARENTHESIZED LATIN SMALL LETTER B]
        '\u249E', 20, // ⒞  [PARENTHESIZED LATIN SMALL LETTER C]
        '\u249F', 25, // ⒟  [PARENTHESIZED LATIN SMALL LETTER D]
        '\u24A0', 30, // ⒠  [PARENTHESIZED LATIN SMALL LETTER E]
        '\u24A1', 33, // ⒡  [PARENTHESIZED LATIN SMALL LETTER F]
        '\u24A2', 41, // ⒢  [PARENTHESIZED LATIN SMALL LETTER G]
        '\u24A3', 45, // ⒣  [PARENTHESIZED LATIN SMALL LETTER H]
        '\u24A4', 50, // ⒤  [PARENTHESIZED LATIN SMALL LETTER I]
        '\u24A5', 54, // ⒥  [PARENTHESIZED LATIN SMALL LETTER J]
        '\u24A6', 57, // ⒦  [PARENTHESIZED LATIN SMALL LETTER K]
        '\u24A7', 63, // ⒧  [PARENTHESIZED LATIN SMALL LETTER L]
        '\u24A8', 70, // ⒨  [PARENTHESIZED LATIN SMALL LETTER M]
        '\u24A9', 75, // ⒩  [PARENTHESIZED LATIN SMALL LETTER N]
        '\u24AA', 82, // ⒪  [PARENTHESIZED LATIN SMALL LETTER O]
        '\u24AB', 88, // ⒫  [PARENTHESIZED LATIN SMALL LETTER P]
        '\u24AC', 91, // ⒬  [PARENTHESIZED LATIN SMALL LETTER Q]
        '\u24AD', 95, // ⒭  [PARENTHESIZED LATIN SMALL LETTER R]
        '\u24AE', 99, // ⒮  [PARENTHESIZED LATIN SMALL LETTER S]
        '\u24AF', 106, // ⒯  [PARENTHESIZED LATIN SMALL LETTER T]
        '\u24B0', 113, // ⒰  [PARENTHESIZED LATIN SMALL LETTER U]
        '\u24B1', 118, // ⒱  [PARENTHESIZED LATIN SMALL LETTER V]
        '\u24B2', 122, // ⒲  [PARENTHESIZED LATIN SMALL LETTER W]
        '\u24B3', 125, // ⒳  [PARENTHESIZED LATIN SMALL LETTER X]
        '\u24B4', 128, // ⒴  [PARENTHESIZED LATIN SMALL LETTER Y]
        '\u24B5', 131, // ⒵  [PARENTHESIZED LATIN SMALL LETTER Z]
        '\u24B6', 0, // Ⓐ  [CIRCLED LATIN CAPITAL LETTER A]
        '\u24B7', 15, // Ⓑ  [CIRCLED LATIN CAPITAL LETTER B]
        '\u24B8', 18, // Ⓒ  [CIRCLED LATIN CAPITAL LETTER C]
        '\u24B9', 21, // Ⓓ  [CIRCLED LATIN CAPITAL LETTER D]
        '\u24BA', 28, // Ⓔ  [CIRCLED LATIN CAPITAL LETTER E]
        '\u24BB', 31, // Ⓕ  [CIRCLED LATIN CAPITAL LETTER F]
        '\u24BC', 39, // Ⓖ  [CIRCLED LATIN CAPITAL LETTER G]
        '\u24BD', 42, // Ⓗ  [CIRCLED LATIN CAPITAL LETTER H]
        '\u24BE', 47, // Ⓘ  [CIRCLED LATIN CAPITAL LETTER I]
        '\u24BF', 52, // Ⓙ  [CIRCLED LATIN CAPITAL LETTER J]
        '\u24C0', 55, // Ⓚ  [CIRCLED LATIN CAPITAL LETTER K]
        '\u24C1', 58, // Ⓛ  [CIRCLED LATIN CAPITAL LETTER L]
        '\u24C2', 68, // Ⓜ  [CIRCLED LATIN CAPITAL LETTER M]
        '\u24C3', 71, // Ⓝ  [CIRCLED LATIN CAPITAL LETTER N]
        '\u24C4', 77, // Ⓞ  [CIRCLED LATIN CAPITAL LETTER O]
        '\u24C5', 86, // Ⓟ  [CIRCLED LATIN CAPITAL LETTER P]
        '\u24C6', 89, // Ⓠ  [CIRCLED LATIN CAPITAL LETTER Q]
        '\u24C7', 93, // Ⓡ  [CIRCLED LATIN CAPITAL LETTER R]
        '\u24C8', 96, // Ⓢ  [CIRCLED LATIN CAPITAL LETTER S]
        '\u24C9', 102, // Ⓣ  [CIRCLED LATIN CAPITAL LETTER T]
        '\u24CA', 111, // Ⓤ  [CIRCLED LATIN CAPITAL LETTER U]
        '\u24CB', 115, // Ⓥ  [CIRCLED LATIN CAPITAL LETTER V]
        '\u24CC', 120, // Ⓦ  [CIRCLED LATIN CAPITAL LETTER W]
        '\u24CD', 123, // Ⓧ  [CIRCLED LATIN CAPITAL LETTER X]
        '\u24CE', 126, // Ⓨ  [CIRCLED LATIN CAPITAL LETTER Y]
        '\u24CF', 129, // Ⓩ  [CIRCLED LATIN CAPITAL LETTER Z]
        '\u24D0', 1, // ⓐ  [CIRCLED LATIN SMALL LETTER A]
        '\u24D1', 16, // ⓑ  [CIRCLED LATIN SMALL LETTER B]
        '\u24D2', 19, // ⓒ  [CIRCLED LATIN SMALL LETTER C]
        '\u24D3', 22, // ⓓ  [CIRCLED LATIN SMALL LETTER D]
        '\u24D4', 29, // ⓔ  [CIRCLED LATIN SMALL LETTER E]
        '\u24D5', 32, // ⓕ  [CIRCLED LATIN SMALL LETTER F]
        '\u24D6', 40, // ⓖ  [CIRCLED LATIN SMALL LETTER G]
        '\u24D7', 43, // ⓗ  [CIRCLED LATIN SMALL LETTER H]
        '\u24D8', 48, // ⓘ  [CIRCLED LATIN SMALL LETTER I]
        '\u24D9', 53, // ⓙ  [CIRCLED LATIN SMALL LETTER J]
        '\u24DA', 56, // ⓚ  [CIRCLED LATIN SMALL LETTER K]
        '\u24DB', 59, // ⓛ  [CIRCLED LATIN SMALL LETTER L]
        '\u24DC', 69, // ⓜ  [CIRCLED LATIN SMALL LETTER M]
        '\u24DD', 72, // ⓝ  [CIRCLED LATIN SMALL LETTER N]
        '\u24DE', 78, // ⓞ  [CIRCLED LATIN SMALL LETTER O]
        '\u24DF', 87, // ⓟ  [CIRCLED LATIN SMALL LETTER P]
        '\u24E0', 90, // ⓠ  [CIRCLED LATIN SMALL LETTER Q]
        '\u24E1', 94, // ⓡ  [CIRCLED LATIN SMALL LETTER R]
        '\u24E2', 97, // ⓢ  [CIRCLED LATIN SMALL LETTER S]
        '\u24E3', 103, // ⓣ  [CIRCLED LATIN SMALL LETTER T]
        '\u24E4', 112, // ⓤ  [CIRCLED LATIN SMALL LETTER U]
        '\u24E5', 116, // ⓥ  [CIRCLED LATIN SMALL LETTER V]
        '\u24E6', 121, // ⓦ  [CIRCLED LATIN SMALL LETTER W]
        '\u24E7', 124, // ⓧ  [CIRCLED LATIN SMALL LETTER X]
        '\u24E8', 127, // ⓨ  [CIRCLED LATIN SMALL LETTER Y]
        '\u24E9', 130, // ⓩ  [CIRCLED LATIN SMALL LETTER Z]
        '\u24EA', 132, // ⓪  [CIRCLED DIGIT ZERO]
        '\u24EB', 163, // ⓫  [NEGATIVE CIRCLED NUMBER ELEVEN]
        '\u24EC', 166, // ⓬  [NEGATIVE CIRCLED NUMBER TWELVE]
        '\u24ED', 169, // ⓭  [NEGATIVE CIRCLED NUMBER THIRTEEN]
        '\u24EE', 172, // ⓮  [NEGATIVE CIRCLED NUMBER FOURTEEN]
        '\u24EF', 175, // ⓯  [NEGATIVE CIRCLED NUMBER FIFTEEN]
        '\u24F0', 178, // ⓰  [NEGATIVE CIRCLED NUMBER SIXTEEN]
        '\u24F1', 181, // ⓱  [NEGATIVE CIRCLED NUMBER SEVENTEEN]
        '\u24F2', 184, // ⓲  [NEGATIVE CIRCLED NUMBER EIGHTEEN]
        '\u24F3', 187, // ⓳  [NEGATIVE CIRCLED NUMBER NINETEEN]
        '\u24F4', 190, // ⓴  [NEGATIVE CIRCLED NUMBER TWENTY]
        '\u24F5', 133, // ⓵  [DOUBLE CIRCLED DIGIT ONE]
        '\u24F6', 136, // ⓶  [DOUBLE CIRCLED DIGIT TWO]
        '\u24F7', 139, // ⓷  [DOUBLE CIRCLED DIGIT THREE]
        '\u24F8', 142, // ⓸  [DOUBLE CIRCLED DIGIT FOUR]
        '\u24F9', 145, // ⓹  [DOUBLE CIRCLED DIGIT FIVE]
        '\u24FA', 148, // ⓺  [DOUBLE CIRCLED DIGIT SIX]
        '\u24FB', 151, // ⓻  [DOUBLE CIRCLED DIGIT SEVEN]
        '\u24FC', 154, // ⓼  [DOUBLE CIRCLED DIGIT EIGHT]
        '\u24FD', 157, // ⓽  [DOUBLE CIRCLED DIGIT NINE]
        '\u24FE', 160, // ⓾  [DOUBLE CIRCLED NUMBER TEN]
        '\u24FF', 132, // ⓿  [NEGATIVE CIRCLED DIGIT ZERO]
        '\u275B', 194, // ❛  [HEAVY SINGLE TURNED COMMA QUOTATION MARK ORNAMENT]
        '\u275C', 194, // ❜  [HEAVY SINGLE COMMA QUOTATION MARK ORNAMENT]
        '\u275D', 193, // ❝  [HEAVY DOUBLE TURNED COMMA QUOTATION MARK ORNAMENT]
        '\u275E', 193, // ❞  [HEAVY DOUBLE COMMA QUOTATION MARK ORNAMENT]
        '\u2768', 198, // ❨  [MEDIUM LEFT PARENTHESIS ORNAMENT]
        '\u2769', 200, // ❩  [MEDIUM RIGHT PARENTHESIS ORNAMENT]
        '\u276A', 198, // ❪  [MEDIUM FLATTENED LEFT PARENTHESIS ORNAMENT]
        '\u276B', 200, // ❫  [MEDIUM FLATTENED RIGHT PARENTHESIS ORNAMENT]
        '\u276C', 202, // ❬  [MEDIUM LEFT-POINTING ANGLE BRACKET ORNAMENT]
        '\u276D', 203, // ❭  [MEDIUM RIGHT-POINTING ANGLE BRACKET ORNAMENT]
        '\u276E', 193, // ❮  [HEAVY LEFT-POINTING ANGLE QUOTATION MARK ORNAMENT]
        '\u276F', 193, // ❯  [HEAVY RIGHT-POINTING ANGLE QUOTATION MARK ORNAMENT]
        '\u2770', 202, // ❰  [HEAVY LEFT-POINTING ANGLE BRACKET ORNAMENT]
        '\u2771', 203, // ❱  [HEAVY RIGHT-POINTING ANGLE BRACKET ORNAMENT]
        '\u2772', 196, // ❲  [LIGHT LEFT TORTOISE SHELL BRACKET ORNAMENT]
        '\u2773', 197, // ❳  [LIGHT RIGHT TORTOISE SHELL BRACKET ORNAMENT]
        '\u2774', 204, // ❴  [MEDIUM LEFT CURLY BRACKET ORNAMENT]
        '\u2775', 205, // ❵  [MEDIUM RIGHT CURLY BRACKET ORNAMENT]
        '\u2776', 133, // ❶  [DINGBAT NEGATIVE CIRCLED DIGIT ONE]
        '\u2777', 136, // ❷  [DINGBAT NEGATIVE CIRCLED DIGIT TWO]
        '\u2778', 139, // ❸  [DINGBAT NEGATIVE CIRCLED DIGIT THREE]
        '\u2779', 142, // ❹  [DINGBAT NEGATIVE CIRCLED DIGIT FOUR]
        '\u277A', 145, // ❺  [DINGBAT NEGATIVE CIRCLED DIGIT FIVE]
        '\u277B', 148, // ❻  [DINGBAT NEGATIVE CIRCLED DIGIT SIX]
        '\u277C', 151, // ❼  [DINGBAT NEGATIVE CIRCLED DIGIT SEVEN]
        '\u277D', 154, // ❽  [DINGBAT NEGATIVE CIRCLED DIGIT EIGHT]
        '\u277E', 157, // ❾  [DINGBAT NEGATIVE CIRCLED DIGIT NINE]
        '\u277F', 160, // ❿  [DINGBAT NEGATIVE CIRCLED NUMBER TEN]
        '\u2780', 133, // ➀  [DINGBAT CIRCLED SANS-SERIF DIGIT ONE]
        '\u2781', 136, // ➁  [DINGBAT CIRCLED SANS-SERIF DIGIT TWO]
        '\u2782', 139, // ➂  [DINGBAT CIRCLED SANS-SERIF DIGIT THREE]
        '\u2783', 142, // ➃  [DINGBAT CIRCLED SANS-SERIF DIGIT FOUR]
        '\u2784', 145, // ➄  [DINGBAT CIRCLED SANS-SERIF DIGIT FIVE]
        '\u2785', 148, // ➅  [DINGBAT CIRCLED SANS-SERIF DIGIT SIX]
        '\u2786', 151, // ➆  [DINGBAT CIRCLED SANS-SERIF DIGIT SEVEN]
        '\u2787', 154, // ➇  [DINGBAT CIRCLED SANS-SERIF DIGIT EIGHT]
        '\u2788', 157, // ➈  [DINGBAT CIRCLED SANS-SERIF DIGIT NINE]
        '\u2789', 160, // ➉  [DINGBAT CIRCLED SANS-SERIF NUMBER TEN]
        '\u278A', 133, // ➊  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT ONE]
        '\u278B', 136, // ➋  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT TWO]
        '\u278C', 139, // ➌  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT THREE]
        '\u278D', 142, // ➍  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT FOUR]
        '\u278E', 145, // ➎  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT FIVE]
        '\u278F', 148, // ➏  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT SIX]
        '\u2790', 151, // ➐  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT SEVEN]
        '\u2791', 154, // ➑  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT EIGHT]
        '\u2792', 157, // ➒  [DINGBAT NEGATIVE CIRCLED SANS-SERIF DIGIT NINE]
        '\u2793', 160, // ➓  [DINGBAT NEGATIVE CIRCLED SANS-SERIF NUMBER TEN]
        '\u2C60', 58, // Ⱡ  [LATIN CAPITAL LETTER L WITH DOUBLE BAR]
        '\u2C61', 59, // ⱡ  [LATIN SMALL LETTER L WITH DOUBLE BAR]
        '\u2C62', 58, // Ɫ  [LATIN CAPITAL LETTER L WITH MIDDLE TILDE]
        '\u2C63', 86, // Ᵽ  [LATIN CAPITAL LETTER P WITH STROKE]
        '\u2C64', 93, // Ɽ  [LATIN CAPITAL LETTER R WITH TAIL]
        '\u2C65', 1, // ⱥ  [LATIN SMALL LETTER A WITH STROKE]
        '\u2C66', 103, // ⱦ  [LATIN SMALL LETTER T WITH DIAGONAL STROKE]
        '\u2C67', 42, // Ⱨ  [LATIN CAPITAL LETTER H WITH DESCENDER]
        '\u2C68', 43, // ⱨ  [LATIN SMALL LETTER H WITH DESCENDER]
        '\u2C69', 55, // Ⱪ  [LATIN CAPITAL LETTER K WITH DESCENDER]
        '\u2C6A', 56, // ⱪ  [LATIN SMALL LETTER K WITH DESCENDER]
        '\u2C6B', 129, // Ⱬ  [LATIN CAPITAL LETTER Z WITH DESCENDER]
        '\u2C6C', 130, // ⱬ  [LATIN SMALL LETTER Z WITH DESCENDER]
        '\u2C6E', 68, // Ɱ  [LATIN CAPITAL LETTER M WITH HOOK]
        '\u2C6F', 1, // Ɐ  [LATIN CAPITAL LETTER TURNED A]
        '\u2C71', 116, // ⱱ  [LATIN SMALL LETTER V WITH RIGHT HOOK]
        '\u2C72', 120, // Ⱳ  [LATIN CAPITAL LETTER W WITH HOOK]
        '\u2C73', 121, // ⱳ  [LATIN SMALL LETTER W WITH HOOK]
        '\u2C74', 116, // ⱴ  [LATIN SMALL LETTER V WITH CURL]
        '\u2C75', 42, // Ⱶ  [LATIN CAPITAL LETTER HALF H]
        '\u2C76', 43, // ⱶ  [LATIN SMALL LETTER HALF H]
        '\u2C78', 29, // ⱸ  [LATIN SMALL LETTER E WITH NOTCH]
        '\u2C7A', 78, // ⱺ  [LATIN SMALL LETTER O WITH LOW RING INSIDE]
        '\u2C7B', 28, // ⱻ  [LATIN LETTER SMALL CAPITAL TURNED E]
        '\u2C7C', 53, // ⱼ  [LATIN SUBSCRIPT SMALL LETTER J]
        '\u2E28', 199, // ⸨  [LEFT DOUBLE PARENTHESIS]
        '\u2E29', 201, // ⸩  [RIGHT DOUBLE PARENTHESIS]
        '\uA728', 105, // Ꜩ  [LATIN CAPITAL LETTER TZ]
        '\uA729', 110, // ꜩ  [LATIN SMALL LETTER TZ]
        '\uA730', 31, // ꜰ  [LATIN LETTER SMALL CAPITAL F]
        '\uA731', 96, // ꜱ  [LATIN LETTER SMALL CAPITAL S]
        '\uA732', 2, // Ꜳ  [LATIN CAPITAL LETTER AA]
        '\uA733', 9, // ꜳ  [LATIN SMALL LETTER AA]
        '\uA734', 4, // Ꜵ  [LATIN CAPITAL LETTER AO]
        '\uA735', 11, // ꜵ  [LATIN SMALL LETTER AO]
        '\uA736', 5, // Ꜷ  [LATIN CAPITAL LETTER AU]
        '\uA737', 12, // ꜷ  [LATIN SMALL LETTER AU]
        '\uA738', 6, // Ꜹ  [LATIN CAPITAL LETTER AV]
        '\uA739', 13, // ꜹ  [LATIN SMALL LETTER AV]
        '\uA73A', 6, // Ꜻ  [LATIN CAPITAL LETTER AV WITH HORIZONTAL BAR]
        '\uA73B', 13, // ꜻ  [LATIN SMALL LETTER AV WITH HORIZONTAL BAR]
        '\uA73C', 7, // Ꜽ  [LATIN CAPITAL LETTER AY]
        '\uA73D', 14, // ꜽ  [LATIN SMALL LETTER AY]
        '\uA73E', 19, // Ꜿ  [LATIN CAPITAL LETTER REVERSED C WITH DOT]
        '\uA73F', 19, // ꜿ  [LATIN SMALL LETTER REVERSED C WITH DOT]
        '\uA740', 55, // Ꝁ  [LATIN CAPITAL LETTER K WITH STROKE]
        '\uA741', 56, // ꝁ  [LATIN SMALL LETTER K WITH STROKE]
        '\uA742', 55, // Ꝃ  [LATIN CAPITAL LETTER K WITH DIAGONAL STROKE]
        '\uA743', 56, // ꝃ  [LATIN SMALL LETTER K WITH DIAGONAL STROKE]
        '\uA744', 55, // Ꝅ  [LATIN CAPITAL LETTER K WITH STROKE AND DIAGONAL STROKE]
        '\uA745', 56, // ꝅ  [LATIN SMALL LETTER K WITH STROKE AND DIAGONAL STROKE]
        '\uA746', 58, // Ꝇ  [LATIN CAPITAL LETTER BROKEN L]
        '\uA747', 59, // ꝇ  [LATIN SMALL LETTER BROKEN L]
        '\uA748', 58, // Ꝉ  [LATIN CAPITAL LETTER L WITH HIGH STROKE]
        '\uA749', 59, // ꝉ  [LATIN SMALL LETTER L WITH HIGH STROKE]
        '\uA74A', 77, // Ꝋ  [LATIN CAPITAL LETTER O WITH LONG STROKE OVERLAY]
        '\uA74B', 78, // ꝋ  [LATIN SMALL LETTER O WITH LONG STROKE OVERLAY]
        '\uA74C', 77, // Ꝍ  [LATIN CAPITAL LETTER O WITH LOOP]
        '\uA74D', 78, // ꝍ  [LATIN SMALL LETTER O WITH LOOP]
        '\uA74E', 80, // Ꝏ  [LATIN CAPITAL LETTER OO]
        '\uA74F', 84, // ꝏ  [LATIN SMALL LETTER OO]
        '\uA750', 86, // Ꝑ  [LATIN CAPITAL LETTER P WITH STROKE THROUGH DESCENDER]
        '\uA751', 87, // ꝑ  [LATIN SMALL LETTER P WITH STROKE THROUGH DESCENDER]
        '\uA752', 86, // Ꝓ  [LATIN CAPITAL LETTER P WITH FLOURISH]
        '\uA753', 87, // ꝓ  [LATIN SMALL LETTER P WITH FLOURISH]
        '\uA754', 86, // Ꝕ  [LATIN CAPITAL LETTER P WITH SQUIRREL TAIL]
        '\uA755', 87, // ꝕ  [LATIN SMALL LETTER P WITH SQUIRREL TAIL]
        '\uA756', 89, // Ꝗ  [LATIN CAPITAL LETTER Q WITH STROKE THROUGH DESCENDER]
        '\uA757', 90, // ꝗ  [LATIN SMALL LETTER Q WITH STROKE THROUGH DESCENDER]
        '\uA758', 89, // Ꝙ  [LATIN CAPITAL LETTER Q WITH DIAGONAL STROKE]
        '\uA759', 90, // ꝙ  [LATIN SMALL LETTER Q WITH DIAGONAL STROKE]
        '\uA75A', 93, // Ꝛ  [LATIN CAPITAL LETTER R ROTUNDA]
        '\uA75B', 94, // ꝛ  [LATIN SMALL LETTER R ROTUNDA]
        '\uA75E', 115, // Ꝟ  [LATIN CAPITAL LETTER V WITH DIAGONAL STROKE]
        '\uA75F', 116, // ꝟ  [LATIN SMALL LETTER V WITH DIAGONAL STROKE]
        '\uA760', 117, // Ꝡ  [LATIN CAPITAL LETTER VY]
        '\uA761', 119, // ꝡ  [LATIN SMALL LETTER VY]
        '\uA762', 129, // Ꝣ  [LATIN CAPITAL LETTER VISIGOTHIC Z]
        '\uA763', 130, // ꝣ  [LATIN SMALL LETTER VISIGOTHIC Z]
        '\uA766', 104, // Ꝧ  [LATIN CAPITAL LETTER THORN WITH STROKE THROUGH DESCENDER]
        '\uA767', 108, // ꝧ  [LATIN SMALL LETTER THORN WITH STROKE THROUGH DESCENDER]
        '\uA768', 115, // Ꝩ  [LATIN CAPITAL LETTER VEND]
        '\uA779', 21, // Ꝺ  [LATIN CAPITAL LETTER INSULAR D]
        '\uA77A', 22, // ꝺ  [LATIN SMALL LETTER INSULAR D]
        '\uA77B', 31, // Ꝼ  [LATIN CAPITAL LETTER INSULAR F]
        '\uA77C', 32, // ꝼ  [LATIN SMALL LETTER INSULAR F]
        '\uA77D', 39, // Ᵹ  [LATIN CAPITAL LETTER INSULAR G]
        '\uA77E', 39, // Ꝿ  [LATIN CAPITAL LETTER TURNED INSULAR G]
        '\uA77F', 40, // ꝿ  [LATIN SMALL LETTER TURNED INSULAR G]
        '\uA780', 58, // Ꞁ  [LATIN CAPITAL LETTER TURNED L]
        '\uA781', 59, // ꞁ  [LATIN SMALL LETTER TURNED L]
        '\uA782', 93, // Ꞃ  [LATIN CAPITAL LETTER INSULAR R]
        '\uA783', 94, // ꞃ  [LATIN SMALL LETTER INSULAR R]
        '\uA784', 97, // Ꞅ  [LATIN CAPITAL LETTER INSULAR S]
        '\uA785', 96, // ꞅ  [LATIN SMALL LETTER INSULAR S]
        '\uA786', 102, // Ꞇ  [LATIN CAPITAL LETTER INSULAR T]
        '\uA7FB', 31, // ꟻ  [LATIN EPIGRAPHIC LETTER REVERSED F]
        '\uA7FC', 87, // ꟼ  [LATIN EPIGRAPHIC LETTER REVERSED P]
        '\uA7FD', 68, // ꟽ  [LATIN EPIGRAPHIC LETTER INVERTED M]
        '\uA7FE', 47, // ꟾ  [LATIN EPIGRAPHIC LETTER I LONGA]
        '\uA7FF', 68, // ꟿ  [LATIN EPIGRAPHIC LETTER ARCHAIC M]
        '\uFB00', 34, // ﬀ  [LATIN SMALL LIGATURE FF]
        '\uFB01', 37, // ﬁ  [LATIN SMALL LIGATURE FI]
        '\uFB02', 38, // ﬂ  [LATIN SMALL LIGATURE FL]
        '\uFB03', 35, // ﬃ  [LATIN SMALL LIGATURE FFI]
        '\uFB04', 36, // ﬄ  [LATIN SMALL LIGATURE FFL]
        '\uFB06', 101, // ﬆ  [LATIN SMALL LIGATURE ST]
        '\uFF01', 208, // ！  [FULLWIDTH EXCLAMATION MARK]
        '\uFF02', 193, // ＂  [FULLWIDTH QUOTATION MARK]
        '\uFF03', 211, // ＃  [FULLWIDTH NUMBER SIGN]
        '\uFF04', 212, // ＄  [FULLWIDTH DOLLAR SIGN]
        '\uFF05', 213, // ％  [FULLWIDTH PERCENT SIGN]
        '\uFF06', 214, // ＆  [FULLWIDTH AMPERSAND]
        '\uFF07', 194, // ＇  [FULLWIDTH APOSTROPHE]
        '\uFF08', 198, // （  [FULLWIDTH LEFT PARENTHESIS]
        '\uFF09', 200, // ）  [FULLWIDTH RIGHT PARENTHESIS]
        '\uFF0A', 215, // ＊  [FULLWIDTH ASTERISK]
        '\uFF0B', 206, // ＋  [FULLWIDTH PLUS SIGN]
        '\uFF0C', 216, // ，  [FULLWIDTH COMMA]
        '\uFF0D', 195, // －  [FULLWIDTH HYPHEN-MINUS]
        '\uFF0E', 217, // ．  [FULLWIDTH FULL STOP]
        '\uFF0F', 218, // ／  [FULLWIDTH SOLIDUS]
        '\uFF10', 132, // ０  [FULLWIDTH DIGIT ZERO]
        '\uFF11', 133, // １  [FULLWIDTH DIGIT ONE]
        '\uFF12', 136, // ２  [FULLWIDTH DIGIT TWO]
        '\uFF13', 139, // ３  [FULLWIDTH DIGIT THREE]
        '\uFF14', 142, // ４  [FULLWIDTH DIGIT FOUR]
        '\uFF15', 145, // ５  [FULLWIDTH DIGIT FIVE]
        '\uFF16', 148, // ６  [FULLWIDTH DIGIT SIX]
        '\uFF17', 151, // ７  [FULLWIDTH DIGIT SEVEN]
        '\uFF18', 154, // ８  [FULLWIDTH DIGIT EIGHT]
        '\uFF19', 157, // ９  [FULLWIDTH DIGIT NINE]
        '\uFF1A', 219, // ：  [FULLWIDTH COLON]
        '\uFF1B', 220, // ；  [FULLWIDTH SEMICOLON]
        '\uFF1C', 202, // ＜  [FULLWIDTH LESS-THAN SIGN]
        '\uFF1D', 207, // ＝  [FULLWIDTH EQUALS SIGN]
        '\uFF1E', 203, // ＞  [FULLWIDTH GREATER-THAN SIGN]
        '\uFF1F', 221, // ？  [FULLWIDTH QUESTION MARK]
        '\uFF20', 224, // ＠  [FULLWIDTH COMMERCIAL AT]
        '\uFF21', 0, // Ａ  [FULLWIDTH LATIN CAPITAL LETTER A]
        '\uFF22', 15, // Ｂ  [FULLWIDTH LATIN CAPITAL LETTER B]
        '\uFF23', 18, // Ｃ  [FULLWIDTH LATIN CAPITAL LETTER C]
        '\uFF24', 21, // Ｄ  [FULLWIDTH LATIN CAPITAL LETTER D]
        '\uFF25', 28, // Ｅ  [FULLWIDTH LATIN CAPITAL LETTER E]
        '\uFF26', 31, // Ｆ  [FULLWIDTH LATIN CAPITAL LETTER F]
        '\uFF27', 39, // Ｇ  [FULLWIDTH LATIN CAPITAL LETTER G]
        '\uFF28', 42, // Ｈ  [FULLWIDTH LATIN CAPITAL LETTER H]
        '\uFF29', 47, // Ｉ  [FULLWIDTH LATIN CAPITAL LETTER I]
        '\uFF2A', 52, // Ｊ  [FULLWIDTH LATIN CAPITAL LETTER J]
        '\uFF2B', 55, // Ｋ  [FULLWIDTH LATIN CAPITAL LETTER K]
        '\uFF2C', 58, // Ｌ  [FULLWIDTH LATIN CAPITAL LETTER L]
        '\uFF2D', 68, // Ｍ  [FULLWIDTH LATIN CAPITAL LETTER M]
        '\uFF2E', 71, // Ｎ  [FULLWIDTH LATIN CAPITAL LETTER N]
        '\uFF2F', 77, // Ｏ  [FULLWIDTH LATIN CAPITAL LETTER O]
        '\uFF30', 86, // Ｐ  [FULLWIDTH LATIN CAPITAL LETTER P]
        '\uFF31', 89, // Ｑ  [FULLWIDTH LATIN CAPITAL LETTER Q]
        '\uFF32', 93, // Ｒ  [FULLWIDTH LATIN CAPITAL LETTER R]
        '\uFF33', 96, // Ｓ  [FULLWIDTH LATIN CAPITAL LETTER S]
        '\uFF34', 102, // Ｔ  [FULLWIDTH LATIN CAPITAL LETTER T]
        '\uFF35', 111, // Ｕ  [FULLWIDTH LATIN CAPITAL LETTER U]
        '\uFF36', 115, // Ｖ  [FULLWIDTH LATIN CAPITAL LETTER V]
        '\uFF37', 120, // Ｗ  [FULLWIDTH LATIN CAPITAL LETTER W]
        '\uFF38', 123, // Ｘ  [FULLWIDTH LATIN CAPITAL LETTER X]
        '\uFF39', 126, // Ｙ  [FULLWIDTH LATIN CAPITAL LETTER Y]
        '\uFF3A', 129, // Ｚ  [FULLWIDTH LATIN CAPITAL LETTER Z]
        '\uFF3B', 196, // ［  [FULLWIDTH LEFT SQUARE BRACKET]
        '\uFF3C', 225, // ＼  [FULLWIDTH REVERSE SOLIDUS]
        '\uFF3D', 197, // ］  [FULLWIDTH RIGHT SQUARE BRACKET]
        '\uFF3E', 226, // ＾  [FULLWIDTH CIRCUMFLEX ACCENT]
        '\uFF3F', 227, // ＿  [FULLWIDTH LOW LINE]
        '\uFF41', 1, // ａ  [FULLWIDTH LATIN SMALL LETTER A]
        '\uFF42', 16, // ｂ  [FULLWIDTH LATIN SMALL LETTER B]
        '\uFF43', 19, // ｃ  [FULLWIDTH LATIN SMALL LETTER C]
        '\uFF44', 22, // ｄ  [FULLWIDTH LATIN SMALL LETTER D]
        '\uFF45', 29, // ｅ  [FULLWIDTH LATIN SMALL LETTER E]
        '\uFF46', 32, // ｆ  [FULLWIDTH LATIN SMALL LETTER F]
        '\uFF47', 40, // ｇ  [FULLWIDTH LATIN SMALL LETTER G]
        '\uFF48', 43, // ｈ  [FULLWIDTH LATIN SMALL LETTER H]
        '\uFF49', 48, // ｉ  [FULLWIDTH LATIN SMALL LETTER I]
        '\uFF4A', 53, // ｊ  [FULLWIDTH LATIN SMALL LETTER J]
        '\uFF4B', 56, // ｋ  [FULLWIDTH LATIN SMALL LETTER K]
        '\uFF4C', 59, // ｌ  [FULLWIDTH LATIN SMALL LETTER L]
        '\uFF4D', 69, // ｍ  [FULLWIDTH LATIN SMALL LETTER M]
        '\uFF4E', 72, // ｎ  [FULLWIDTH LATIN SMALL LETTER N]
        '\uFF4F', 78, // ｏ  [FULLWIDTH LATIN SMALL LETTER O]
        '\uFF50', 87, // ｐ  [FULLWIDTH LATIN SMALL LETTER P]
        '\uFF51', 90, // ｑ  [FULLWIDTH LATIN SMALL LETTER Q]
        '\uFF52', 94, // ｒ  [FULLWIDTH LATIN SMALL LETTER R]
        '\uFF53', 97, // ｓ  [FULLWIDTH LATIN SMALL LETTER S]
        '\uFF54', 103, // ｔ  [FULLWIDTH LATIN SMALL LETTER T]
        '\uFF55', 112, // ｕ  [FULLWIDTH LATIN SMALL LETTER U]
        '\uFF56', 116, // ｖ  [FULLWIDTH LATIN SMALL LETTER V]
        '\uFF57', 121, // ｗ  [FULLWIDTH LATIN SMALL LETTER W]
        '\uFF58', 124, // ｘ  [FULLWIDTH LATIN SMALL LETTER X]
        '\uFF59', 127, // ｙ  [FULLWIDTH LATIN SMALL LETTER Y]
        '\uFF5A', 130, // ｚ  [FULLWIDTH LATIN SMALL LETTER Z]
        '\uFF5B', 204, // ｛  [FULLWIDTH LEFT CURLY BRACKET]
        '\uFF5D', 205, // ｝  [FULLWIDTH RIGHT CURLY BRACKET]
        '\uFF5E', 228, // ～  [FULLWIDTH TILDE]
        };
        charMapTarget = new String[] {
        "A",
        "a",
        "AA",
        "AE",
        "AO",
        "AU",
        "AV",
        "AY",
        "(a)",
        "aa",
        "ae",
        "ao",
        "au",
        "av",
        "ay",
        "B",
        "b",
        "(b)",
        "C",
        "c",
        "(c)",
        "D",
        "d",
        "DZ",
        "Dz",
        "(d)",
        "db",
        "dz",
        "E",
        "e",
        "(e)",
        "F",
        "f",
        "(f)",
        "ff",
        "ffi",
        "ffl",
        "fi",
        "fl",
        "G",
        "g",
        "(g)",
        "H",
        "h",
        "HV",
        "(h)",
        "hv",
        "I",
        "i",
        "IJ",
        "(i)",
        "ij",
        "J",
        "j",
        "(j)",
        "K",
        "k",
        "(k)",
        "L",
        "l",
        "LJ",
        "LL",
        "Lj",
        "(l)",
        "lj",
        "ll",
        "ls",
        "lz",
        "M",
        "m",
        "(m)",
        "N",
        "n",
        "NJ",
        "Nj",
        "(n)",
        "nj",
        "O",
        "o",
        "OE",
        "OO",
        "OU",
        "(o)",
        "oe",
        "oo",
        "ou",
        "P",
        "p",
        "(p)",
        "Q",
        "q",
        "(q)",
        "qp",
        "R",
        "r",
        "(r)",
        "S",
        "s",
        "SS",
        "(s)",
        "ss",
        "st",
        "T",
        "t",
        "TH",
        "TZ",
        "(t)",
        "tc",
        "th",
        "ts",
        "tz",
        "U",
        "u",
        "(u)",
        "ue",
        "V",
        "v",
        "VY",
        "(v)",
        "vy",
        "W",
        "w",
        "(w)",
        "X",
        "x",
        "(x)",
        "Y",
        "y",
        "(y)",
        "Z",
        "z",
        "(z)",
        "0",
        "1",
        "1.",
        "(1)",
        "2",
        "2.",
        "(2)",
        "3",
        "3.",
        "(3)",
        "4",
        "4.",
        "(4)",
        "5",
        "5.",
        "(5)",
        "6",
        "6.",
        "(6)",
        "7",
        "7.",
        "(7)",
        "8",
        "8.",
        "(8)",
        "9",
        "9.",
        "(9)",
        "10",
        "10.",
        "(10)",
        "11",
        "11.",
        "(11)",
        "12",
        "12.",
        "(12)",
        "13",
        "13.",
        "(13)",
        "14",
        "14.",
        "(14)",
        "15",
        "15.",
        "(15)",
        "16",
        "16.",
        "(16)",
        "17",
        "17.",
        "(17)",
        "18",
        "18.",
        "(18)",
        "19",
        "19.",
        "(19)",
        "20",
        "20.",
        "(20)",
        "\"",
        "\'",
        "-",
        "[",
        "]",
        "(",
        "((",
        ")",
        "))",
        "<",
        ">",
        "{",
        "}",
        "+",
        "=",
        "!",
        "!!",
        "!?",
        "#",
        "$",
        "%",
        "&",
        "*",
        ",",
        ".",
        "/",
        ":",
        ";",
        "?",
        "??",
        "?!",
        "@",
        "\\",
        "^",
        "_",
        "~",
        };
  }
}
