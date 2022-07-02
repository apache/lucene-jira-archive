package schema;
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

public class UnicodeCharUtil
{
    private UnicodeCharUtil() {}
    
    static private boolean isInRange ( char c, int l, int h) 
    {
        return ( c >= l && c <= h );
    }
    
    static public char foldNonDiacriticChar ( char c ) 
    {
         switch (c) {
             case 0x0181:  return(0x0042);    //  LATIN CAPITAL LETTER B WITH HOOK -> LATIN CAPITAL LETTER B
             case 0x0182:  return(0x0042);    //  LATIN CAPITAL LETTER B WITH TOPBAR -> LATIN CAPITAL LETTER B
             case 0x0187:  return(0x0043);    //  LATIN CAPITAL LETTER C WITH HOOK -> LATIN CAPITAL LETTER C
             case 0x0110:  return(0x0044);    //  LATIN CAPITAL LETTER D WITH STROKE -> LATIN CAPITAL LETTER D
             case 0x018A:  return(0x0044);    //  LATIN CAPITAL LETTER D WITH HOOK -> LATIN CAPITAL LETTER D
             case 0x018B:  return(0x0044);    //  LATIN CAPITAL LETTER D WITH TOPBAR -> LATIN CAPITAL LETTER D
             case 0x0191:  return(0x0046);    //  LATIN CAPITAL LETTER F WITH HOOK -> LATIN CAPITAL LETTER F
             case 0x0193:  return(0x0047);    //  LATIN CAPITAL LETTER G WITH HOOK -> LATIN CAPITAL LETTER G
             case 0x01E4:  return(0x0047);    //  LATIN CAPITAL LETTER G WITH STROKE -> LATIN CAPITAL LETTER G
             case 0x0126:  return(0x0048);    //  LATIN CAPITAL LETTER H WITH STROKE -> LATIN CAPITAL LETTER H
             case 0x0197:  return(0x0049);    //  LATIN CAPITAL LETTER I WITH STROKE -> LATIN CAPITAL LETTER I
             case 0x0198:  return(0x004B);    //  LATIN CAPITAL LETTER K WITH HOOK -> LATIN CAPITAL LETTER K
             case 0x0141:  return(0x004C);    //  LATIN CAPITAL LETTER L WITH STROKE -> LATIN CAPITAL LETTER L
             case 0x019D:  return(0x004E);    //  LATIN CAPITAL LETTER N WITH LEFT HOOK -> LATIN CAPITAL LETTER N
             case 0x0220:  return(0x004E);    //  LATIN CAPITAL LETTER N WITH LONG RIGHT LEG -> LATIN CAPITAL LETTER N
             case 0x00D8:  return(0x004F);    //  LATIN CAPITAL LETTER O WITH STROKE -> LATIN CAPITAL LETTER O
             case 0x019F:  return(0x004F);    //  LATIN CAPITAL LETTER O WITH MIDDLE TILDE -> LATIN CAPITAL LETTER O
             case 0x01FE:  return(0x004F);    //  LATIN CAPITAL LETTER O WITH STROKE AND ACUTE -> LATIN CAPITAL LETTER O
             case 0x01A4:  return(0x0050);    //  LATIN CAPITAL LETTER P WITH HOOK -> LATIN CAPITAL LETTER P
             case 0x0166:  return(0x0054);    //  LATIN CAPITAL LETTER T WITH STROKE -> LATIN CAPITAL LETTER T
             case 0x01AC:  return(0x0054);    //  LATIN CAPITAL LETTER T WITH HOOK -> LATIN CAPITAL LETTER T
             case 0x01AE:  return(0x0054);    //  LATIN CAPITAL LETTER T WITH RETROFLEX HOOK -> LATIN CAPITAL LETTER T
             case 0x01B2:  return(0x0056);    //  LATIN CAPITAL LETTER V WITH HOOK -> LATIN CAPITAL LETTER V
             case 0x01B3:  return(0x0059);    //  LATIN CAPITAL LETTER Y WITH HOOK -> LATIN CAPITAL LETTER Y
             case 0x01B5:  return(0x005A);    //  LATIN CAPITAL LETTER Z WITH STROKE -> LATIN CAPITAL LETTER Z
             case 0x0224:  return(0x005A);    //  LATIN CAPITAL LETTER Z WITH HOOK -> LATIN CAPITAL LETTER Z
             case 0x0180:  return(0x0062);    //  LATIN SMALL LETTER B WITH STROKE -> LATIN SMALL LETTER B
             case 0x0183:  return(0x0062);    //  LATIN SMALL LETTER B WITH TOPBAR -> LATIN SMALL LETTER B
             case 0x0253:  return(0x0062);    //  LATIN SMALL LETTER B WITH HOOK -> LATIN SMALL LETTER B
             case 0x0188:  return(0x0063);    //  LATIN SMALL LETTER C WITH HOOK -> LATIN SMALL LETTER C
             case 0x0255:  return(0x0063);    //  LATIN SMALL LETTER C WITH CURL -> LATIN SMALL LETTER C
             case 0x0111:  return(0x0064);    //  LATIN SMALL LETTER D WITH STROKE -> LATIN SMALL LETTER D
             case 0x018C:  return(0x0064);    //  LATIN SMALL LETTER D WITH TOPBAR -> LATIN SMALL LETTER D
             case 0x0221:  return(0x0064);    //  LATIN SMALL LETTER D WITH CURL -> LATIN SMALL LETTER D
             case 0x0256:  return(0x0064);    //  LATIN SMALL LETTER D WITH TAIL -> LATIN SMALL LETTER D
             case 0x0257:  return(0x0064);    //  LATIN SMALL LETTER D WITH HOOK -> LATIN SMALL LETTER D
             case 0x0192:  return(0x0066);    //  LATIN SMALL LETTER F WITH HOOK -> LATIN SMALL LETTER F
             case 0x01E5:  return(0x0067);    //  LATIN SMALL LETTER G WITH STROKE -> LATIN SMALL LETTER G
             case 0x0260:  return(0x0067);    //  LATIN SMALL LETTER G WITH HOOK -> LATIN SMALL LETTER G
             case 0x0127:  return(0x0068);    //  LATIN SMALL LETTER H WITH STROKE -> LATIN SMALL LETTER H
             case 0x0266:  return(0x0068);    //  LATIN SMALL LETTER H WITH HOOK -> LATIN SMALL LETTER H
             case 0x0268:  return(0x0069);    //  LATIN SMALL LETTER I WITH STROKE -> LATIN SMALL LETTER I
             case 0x029D:  return(0x006A);    //  LATIN SMALL LETTER J WITH CROSSED-TAIL -> LATIN SMALL LETTER J
             case 0x0199:  return(0x006B);    //  LATIN SMALL LETTER K WITH HOOK -> LATIN SMALL LETTER K
             case 0x0142:  return(0x006C);    //  LATIN SMALL LETTER L WITH STROKE -> LATIN SMALL LETTER L
             case 0x019A:  return(0x006C);    //  LATIN SMALL LETTER L WITH BAR -> LATIN SMALL LETTER L
             case 0x0234:  return(0x006C);    //  LATIN SMALL LETTER L WITH CURL -> LATIN SMALL LETTER L
             case 0x026B:  return(0x006C);    //  LATIN SMALL LETTER L WITH MIDDLE TILDE -> LATIN SMALL LETTER L
             case 0x026C:  return(0x006C);    //  LATIN SMALL LETTER L WITH BELT -> LATIN SMALL LETTER L
             case 0x026D:  return(0x006C);    //  LATIN SMALL LETTER L WITH RETROFLEX HOOK -> LATIN SMALL LETTER L
             case 0x0271:  return(0x006D);    //  LATIN SMALL LETTER M WITH HOOK -> LATIN SMALL LETTER M
             case 0x019E:  return(0x006E);    //  LATIN SMALL LETTER N WITH LONG RIGHT LEG -> LATIN SMALL LETTER N
             case 0x0235:  return(0x006E);    //  LATIN SMALL LETTER N WITH CURL -> LATIN SMALL LETTER N
             case 0x0272:  return(0x006E);    //  LATIN SMALL LETTER N WITH LEFT HOOK -> LATIN SMALL LETTER N
             case 0x0273:  return(0x006E);    //  LATIN SMALL LETTER N WITH RETROFLEX HOOK -> LATIN SMALL LETTER N
             case 0x00F8:  return(0x006F);    //  LATIN SMALL LETTER O WITH STROKE -> LATIN SMALL LETTER O
             case 0x01FF:  return(0x006F);    //  LATIN SMALL LETTER O WITH STROKE AND ACUTE -> LATIN SMALL LETTER O
             case 0x01A5:  return(0x0070);    //  LATIN SMALL LETTER P WITH HOOK -> LATIN SMALL LETTER P
             case 0x02A0:  return(0x0071);    //  LATIN SMALL LETTER Q WITH HOOK -> LATIN SMALL LETTER Q
             case 0x027C:  return(0x0072);    //  LATIN SMALL LETTER R WITH LONG LEG -> LATIN SMALL LETTER R
             case 0x027D:  return(0x0072);    //  LATIN SMALL LETTER R WITH TAIL -> LATIN SMALL LETTER R
             case 0x0282:  return(0x0073);    //  LATIN SMALL LETTER S WITH HOOK -> LATIN SMALL LETTER S
             case 0x0167:  return(0x0074);    //  LATIN SMALL LETTER T WITH STROKE -> LATIN SMALL LETTER T
             case 0x01AB:  return(0x0074);    //  LATIN SMALL LETTER T WITH PALATAL HOOK -> LATIN SMALL LETTER T
             case 0x01AD:  return(0x0074);    //  LATIN SMALL LETTER T WITH HOOK -> LATIN SMALL LETTER T
             case 0x0236:  return(0x0074);    //  LATIN SMALL LETTER T WITH CURL -> LATIN SMALL LETTER T
             case 0x0288:  return(0x0074);    //  LATIN SMALL LETTER T WITH RETROFLEX HOOK -> LATIN SMALL LETTER T
             case 0x028B:  return(0x0076);    //  LATIN SMALL LETTER V WITH HOOK -> LATIN SMALL LETTER V
             case 0x01B4:  return(0x0079);    //  LATIN SMALL LETTER Y WITH HOOK -> LATIN SMALL LETTER Y
             case 0x01B6:  return(0x007A);    //  LATIN SMALL LETTER Z WITH STROKE -> LATIN SMALL LETTER Z
             case 0x0225:  return(0x007A);    //  LATIN SMALL LETTER Z WITH HOOK -> LATIN SMALL LETTER Z
             case 0x0290:  return(0x007A);    //  LATIN SMALL LETTER Z WITH RETROFLEX HOOK -> LATIN SMALL LETTER Z
             case 0x0291:  return(0x007A);    //  LATIN SMALL LETTER Z WITH CURL -> LATIN SMALL LETTER Z
             case 0x025A:  return(0x0259);    //  LATIN SMALL LETTER SCHWA WITH HOOK -> LATIN SMALL LETTER SCHWA
             case 0x0286:  return(0x0283);    //  LATIN SMALL LETTER ESH WITH CURL -> LATIN SMALL LETTER ESH
             case 0x01BA:  return(0x0292);    //  LATIN SMALL LETTER EZH WITH TAIL -> LATIN SMALL LETTER EZH
             case 0x0293:  return(0x0292);    //  LATIN SMALL LETTER EZH WITH CURL -> LATIN SMALL LETTER EZH
             case 0x0490:  return(0x0413);    //  CYRILLIC CAPITAL LETTER GHE WITH UPTURN -> CYRILLIC CAPITAL LETTER GHE
             case 0x0492:  return(0x0413);    //  CYRILLIC CAPITAL LETTER GHE WITH STROKE -> CYRILLIC CAPITAL LETTER GHE
             case 0x0494:  return(0x0413);    //  CYRILLIC CAPITAL LETTER GHE WITH MIDDLE HOOK -> CYRILLIC CAPITAL LETTER GHE
             case 0x0496:  return(0x0416);    //  CYRILLIC CAPITAL LETTER ZHE WITH DESCENDER -> CYRILLIC CAPITAL LETTER ZHE
             case 0x0498:  return(0x0417);    //  CYRILLIC CAPITAL LETTER ZE WITH DESCENDER -> CYRILLIC CAPITAL LETTER ZE
             case 0x048A:  return(0x0419);    //  CYRILLIC CAPITAL LETTER SHORT I WITH TAIL -> CYRILLIC CAPITAL LETTER SHORT I
             case 0x049A:  return(0x041A);    //  CYRILLIC CAPITAL LETTER KA WITH DESCENDER -> CYRILLIC CAPITAL LETTER KA
             case 0x049C:  return(0x041A);    //  CYRILLIC CAPITAL LETTER KA WITH VERTICAL STROKE -> CYRILLIC CAPITAL LETTER KA
             case 0x049E:  return(0x041A);    //  CYRILLIC CAPITAL LETTER KA WITH STROKE -> CYRILLIC CAPITAL LETTER KA
             case 0x04C3:  return(0x041A);    //  CYRILLIC CAPITAL LETTER KA WITH HOOK -> CYRILLIC CAPITAL LETTER KA
             case 0x04C5:  return(0x041B);    //  CYRILLIC CAPITAL LETTER EL WITH TAIL -> CYRILLIC CAPITAL LETTER EL
             case 0x04CD:  return(0x041C);    //  CYRILLIC CAPITAL LETTER EM WITH TAIL -> CYRILLIC CAPITAL LETTER EM
             case 0x04A2:  return(0x041D);    //  CYRILLIC CAPITAL LETTER EN WITH DESCENDER -> CYRILLIC CAPITAL LETTER EN
             case 0x04C7:  return(0x041D);    //  CYRILLIC CAPITAL LETTER EN WITH HOOK -> CYRILLIC CAPITAL LETTER EN
             case 0x04C9:  return(0x041D);    //  CYRILLIC CAPITAL LETTER EN WITH TAIL -> CYRILLIC CAPITAL LETTER EN
             case 0x04A6:  return(0x041F);    //  CYRILLIC CAPITAL LETTER PE WITH MIDDLE HOOK -> CYRILLIC CAPITAL LETTER PE
             case 0x048E:  return(0x0420);    //  CYRILLIC CAPITAL LETTER ER WITH TICK -> CYRILLIC CAPITAL LETTER ER
             case 0x04AA:  return(0x0421);    //  CYRILLIC CAPITAL LETTER ES WITH DESCENDER -> CYRILLIC CAPITAL LETTER ES
             case 0x04AC:  return(0x0422);    //  CYRILLIC CAPITAL LETTER TE WITH DESCENDER -> CYRILLIC CAPITAL LETTER TE
             case 0x04B2:  return(0x0425);    //  CYRILLIC CAPITAL LETTER HA WITH DESCENDER -> CYRILLIC CAPITAL LETTER HA
             case 0x04B3:  return(0x0425);    //  CYRILLIC SMALL LETTER HA WITH DESCENDER -> CYRILLIC CAPITAL LETTER HA
             case 0x0491:  return(0x0433);    //  CYRILLIC SMALL LETTER GHE WITH UPTURN -> CYRILLIC SMALL LETTER GHE
             case 0x0493:  return(0x0433);    //  CYRILLIC SMALL LETTER GHE WITH STROKE -> CYRILLIC SMALL LETTER GHE
             case 0x0495:  return(0x0433);    //  CYRILLIC SMALL LETTER GHE WITH MIDDLE HOOK -> CYRILLIC SMALL LETTER GHE
             case 0x0497:  return(0x0436);    //  CYRILLIC SMALL LETTER ZHE WITH DESCENDER -> CYRILLIC SMALL LETTER ZHE
             case 0x0499:  return(0x0437);    //  CYRILLIC SMALL LETTER ZE WITH DESCENDER -> CYRILLIC SMALL LETTER ZE
             case 0x048B:  return(0x0439);    //  CYRILLIC SMALL LETTER SHORT I WITH TAIL -> CYRILLIC SMALL LETTER SHORT I
             case 0x049B:  return(0x043A);    //  CYRILLIC SMALL LETTER KA WITH DESCENDER -> CYRILLIC SMALL LETTER KA
             case 0x049D:  return(0x043A);    //  CYRILLIC SMALL LETTER KA WITH VERTICAL STROKE -> CYRILLIC SMALL LETTER KA
             case 0x049F:  return(0x043A);    //  CYRILLIC SMALL LETTER KA WITH STROKE -> CYRILLIC SMALL LETTER KA
             case 0x04C4:  return(0x043A);    //  CYRILLIC SMALL LETTER KA WITH HOOK -> CYRILLIC SMALL LETTER KA
             case 0x04C6:  return(0x043B);    //  CYRILLIC SMALL LETTER EL WITH TAIL -> CYRILLIC SMALL LETTER EL
             case 0x04CE:  return(0x043C);    //  CYRILLIC SMALL LETTER EM WITH TAIL -> CYRILLIC SMALL LETTER EM
             case 0x04A3:  return(0x043D);    //  CYRILLIC SMALL LETTER EN WITH DESCENDER -> CYRILLIC SMALL LETTER EN
             case 0x04C8:  return(0x043D);    //  CYRILLIC SMALL LETTER EN WITH HOOK -> CYRILLIC SMALL LETTER EN
             case 0x04CA:  return(0x043D);    //  CYRILLIC SMALL LETTER EN WITH TAIL -> CYRILLIC SMALL LETTER EN
             case 0x04A7:  return(0x043F);    //  CYRILLIC SMALL LETTER PE WITH MIDDLE HOOK -> CYRILLIC SMALL LETTER PE
             case 0x048F:  return(0x0440);    //  CYRILLIC SMALL LETTER ER WITH TICK -> CYRILLIC SMALL LETTER ER
             case 0x04AB:  return(0x0441);    //  CYRILLIC SMALL LETTER ES WITH DESCENDER -> CYRILLIC SMALL LETTER ES
             case 0x04AD:  return(0x0442);    //  CYRILLIC SMALL LETTER TE WITH DESCENDER -> CYRILLIC SMALL LETTER TE
             case 0x04B9:  return(0x0447);    //  CYRILLIC SMALL LETTER CHE WITH VERTICAL STROKE -> CYRILLIC SMALL LETTER CHE
             case 0x047C:  return(0x0460);    //  CYRILLIC CAPITAL LETTER OMEGA WITH TITLO -> CYRILLIC CAPITAL LETTER OMEGA
             case 0x047D:  return(0x0461);    //  CYRILLIC SMALL LETTER OMEGA WITH TITLO -> CYRILLIC SMALL LETTER OMEGA
             case 0x04B0:  return(0x04AE);    //  CYRILLIC CAPITAL LETTER STRAIGHT U WITH STROKE -> CYRILLIC CAPITAL LETTER STRAIGHT U
             case 0x04B1:  return(0x04AF);    //  CYRILLIC SMALL LETTER STRAIGHT U WITH STROKE -> CYRILLIC SMALL LETTER STRAIGHT U
             case 0x04B6:  return(0x04BC);    //  CYRILLIC CAPITAL LETTER CHE WITH DESCENDER -> CYRILLIC CAPITAL LETTER ABKHASIAN CHE
             case 0x04B7:  return(0x04BC);    //  CYRILLIC SMALL LETTER CHE WITH DESCENDER -> CYRILLIC CAPITAL LETTER ABKHASIAN CHE
             case 0x04B8:  return(0x04BC);    //  CYRILLIC CAPITAL LETTER CHE WITH VERTICAL STROKE -> CYRILLIC CAPITAL LETTER ABKHASIAN CHE
             case 0x04BE:  return(0x04BC);    //  CYRILLIC CAPITAL LETTER ABKHASIAN CHE WITH DESCENDER -> CYRILLIC CAPITAL LETTER ABKHASIANCHE
             case 0x04BF:  return(0x04BC);    //  CYRILLIC SMALL LETTER ABKHASIAN CHE WITH DESCENDER -> CYRILLIC CAPITAL LETTER ABKHASIAN CHE
             case 0x04CB:  return(0x04BC);    //  CYRILLIC CAPITAL LETTER KHAKASSIAN CHE -> CYRILLIC CAPITAL LETTER ABKHASIAN CHE
             case 0x04CC:  return(0x04BC);    //  CYRILLIC SMALL LETTER KHAKASSIAN CHE -> CYRILLIC CAPITAL LETTER ABKHASIAN CHE
             default:      return(0x00);
         }
    }
    
    static public boolean isSpacingModifier ( char c ) 
    {
         int cval = (int )c;
//         String cvalStr = Integer.toHexString(cval);
         int hiByte = cval >>> 8;
         switch (hiByte) {
             case 0x02 : // SPACING MODIFIERS
                 return isInRange(c,0x02B0,0x02FF);

             case 0x03 : // GREEK
                 return c == 0x0374 || c == 0x037A;
                 
             case 0x05 : // ARMENIAN
                 return c == 0x0559;
            
             case 0x06 : // ARABIC
                 return c == 0x0640 || c == 0x06E5 ||  c == 0x06E6;

             case 0x07 : // NKO 
                 return c == 0x07F4 || c == 0x07F5 ||  c == 0x07FA;

             case 0x09 : // DEVANAGARI
                 return   c == 0x0971;
             
             case 0x0E :  // THAI
                 return  c == 0x0E46 ||
                         // LAO
                         c == 0x0EC6;
             
             case 0x10 : // GEORGIAN
                 return   c == 0x10FC;
                 
             case 0x17 : // KHMER
                 return  c == 0x17D7;

             case 0x18 : // MONGOLIAN    
                 return  c == 0x1843;
                 
             case 0x1C : // OL CHIKI  
                 return  isInRange(c,0x1C78,0x1C7D);

             case 0x1D :  // Additional Modifiers
                 return  isInRange(c,0x1D2C,0x1D61) || 
                         c == 0x1D78 ||
                         isInRange(c,0x1D9B,0x1DBF) ||
                         isInRange(c,0x20E8,0x20F0);
                 
             case 0x20 :  
                 return  isInRange(c,0x2090,0x2094);
                 
             case 0x2C :  
                 return  c == 0x2C7D;
                 
             case 0x2D :  // TIFINAGH 
                 return  c == 0x2D6F;
                 
             case 0x2E :  
                 return  c == 0x2E2F;
                 
             case 0x30 :
                 return  c == 0x3005 || 
                         isInRange(c,0x3031,0x3035) || 
                         c == 0x303B|| 
                         isInRange(c,0x309D,0x309E)|| 
                         isInRange(c,0x30FC,0x30FE);
             
             default :
                 return false;
         }
    }
         
    static public boolean isCombiningCharacter ( char c ) 
    {
        int cval = (int )c;
//        String cvalStr = Integer.toHexString(cval);
        int hiByte = cval >>> 8;
        switch (hiByte) {
        
        case 0x03 : // LATIN
            return isInRange(c,0x0300,0x034E) ||
                   isInRange(c,0x0350,0x0362) ;
            
        case 0x04 : // CYRILLIC 
            return isInRange(c,0x0483,0x0487);
        
        case 0x05 : // HEBREW
            return  isInRange(c,0x0591,0x05BD) || 
                    c == 0x05BF || 
                    isInRange(c,0x05C1,0x05C2) || 
                    isInRange(c,0x05C4,0x05C5) || 
                    c == 0x05C7;
        
        case 0x06 :  // ARABIC 
            return isInRange(c,0x064B,0x0652) || 
                    isInRange(c,0x0657,0x0658) || 
                    isInRange(c,0x06DF,0x06E0) || 
                    isInRange(c,0x06EA,0x06EC) ;
        
        case 0x07 : //SYRIAC
            return isInRange(c,0x0730,0x074A) || 
                    // THAANA 
                   isInRange(c,0x07A6,0x07B0) ||
                    // NKO 
                   isInRange(c,0x07EB,0x07F3);
            
        case 0x09 : // DEVANAGARI
            return  isInRange(c,0x0901,0x0902) || 
                    c == 0x093C ||
                    isInRange(c,0x0941,0x0948) || 
                    c == 0x094D ||
                    isInRange(c,0x0951,0x0954) || 
                    isInRange(c,0x0962,0x0963) || 
                    // BENGALI 
                    c == 0x0981 ||
                    c == 0x09BC ||
                    isInRange(c,0x09C1,0x09C4) || 
                    c == 0x09CD ||
                    isInRange(c,0x09E1,0x09E3);

        
        case 0x0A : // GURMUKHI
            return  isInRange(c,0x0A01,0x0A02) || 
                    c == 0x0A3C ||
                    isInRange(c,0x0A41,0x0A42) || 
                    isInRange(c,0x0A47,0x0A48) || 
                    isInRange(c,0x0A4B,0x0A4D) || 
                    c == 0x0A51 ||
                    isInRange(c,0x0A70,0x0A71) || 
                    c == 0x0A75 || 
                    // GUJARATI 
                    isInRange(c,0x0A81,0x0A82) || 
                    c == 0x0ABC ||
                    isInRange(c,0x0AC1,0x0AC5) || 
                    isInRange(c,0x0AC7,0x0AC8) || 
                    c == 0x0ACD ||
                    isInRange(c,0x0AE2,0x0AE3);
         
        case 0x0B : // ORIYA
            return  c == 0x0B01 ||
                    c == 0x0B3C ||
                    c == 0x0B3F ||
                    isInRange(c,0x0B41,0x0B44) || 
                    c == 0x0B4D || 
                    c == 0x0B56 || 
                    isInRange(c,0x0B62,0x0B63) || 
                    // TAMIL
                    c == 0x0B82 ||
                    c == 0x0BC0 ||
                    c == 0x0BCD;
         
        case 0x0C : // TELUGU 
            return  isInRange(c,0x0C3E,0x0C40) || 
                    isInRange(c,0x0C46,0x0C48) ||
                    isInRange(c,0x0C4A,0x0C4D) ||
                    isInRange(c,0x0C55,0x0C56) ||
                    isInRange(c,0x0C62,0x0C63) ||
                    // KANNADA 
                    c == 0x0CBC || 
                    c == 0x0CBF || 
                    c == 0x0CC6 || 
                    isInRange(c,0x0CCC,0x0CCD) ||
                    isInRange(c,0x0CE2,0x0CE3);
        
        case 0x0D : // MALAYALAM 
            return  isInRange(c,0x0D41,0x0D44) || 
                    c == 0x0D4D || 
                    isInRange(c,0x0D62,0x0D63) ||
                    // SINHALA 
                    c == 0x0DCA ||                    
                    isInRange(c,0x0DD2,0x0DD4) ||
                    c == 0x0DD6;                    
        
        case 0x0E :  // THAI
            return  c == 0x0E31 ||
                    isInRange(c,0x0E34,0x0E3A) || 
                    isInRange(c,0x0E47,0x0E4E) || 
                    // LAO
                    c == 0x0EB1 ||
                    isInRange(c,0x0EB4,0x0E39) || 
                    isInRange(c,0x0EBB,0x0EBC) || 
                    isInRange(c,0x0EC8,0x0ECD);
            
        case 0x0F : // TIBETAN
            return isInRange(c,0x0F18,0x0F19) || 
                    c == 0x0F35 || 
                    c == 0x0F37 || 
                    c == 0x0F39 || 
                    isInRange(c,0x0F71,0x0F7E) || 
                    c == 0x0F3E || 
                    c == 0x0F3F || 
                    isInRange(c,0x0F80,0x0F84) || 
                    isInRange(c,0x0F86,0x0F87) || 
                    c == 0x0FC6; 
        
        case 0x10 : // MYANMAR
            return  isInRange(c,0x102D,0x1030) || 
                    isInRange(c,0x1032,0x1037) || 
                    isInRange(c,0x1039,0x103A) || 
                    isInRange(c,0x103D,0x103E) || 
                    isInRange(c,0x1058,0x1059) || 
                    isInRange(c,0x105E,0x1060) || 
                    isInRange(c,0x1071,0x1074) || 
                    c == 0x1082 ||
                    isInRange(c,0x1085,0x1086) || 
                    c == 0x108D ;
            
        case 0x13 : // ETHIOPIC 
            return  c == 0x135F ; 
            
        case 0x17 : // TAGALOG   
            return  isInRange(c,0x1712,0x1714) ||
                    // HANUNOO
                    isInRange(c,0x1732,0x1734) || 
                    // BUHID
                    isInRange(c,0x1752,0x1753) || 
                    // TAGBANWA
                    isInRange(c,0x1772,0x1773) || 
                    // KHMER
                    isInRange(c,0x17B7,0x17BD) || 
                    c == 0x17C6 || 
                    isInRange(c,0x17C9,0x17D3) || 
                    c == 0x17DD ;
            
        case 0x18 : // MONGOLIAN    
            return  isInRange(c,0x180B,0x180D);

        case 0x19 : // LIMBU
            return  isInRange(c,0x1920,0x1922) ||            
                    isInRange(c,0x1927,0x1928) || 
                    c == 0x1932 || 
                    isInRange(c,0x1939,0x193B) ;
            
        case 0x1A : // MONGOLIAN    
            return  isInRange(c,0x1A17,0x1A8);
        
        case 0x1B : // BALINESE 
            return  isInRange(c,0x1B00,0x1B03) ||
                    c == 0x1B34 ||
                    isInRange(c,0x1B36,0x1B3A) ||
                    c == 0x1B3C ||
                    c == 0x1B42 ||
                    // SUNDANESE 
                    isInRange(c,0x1B80,0x1B81) ||
                    isInRange(c,0x1BA2,0x1BA5) ||
                    isInRange(c,0x1BA8,0x1BA9);

        case 0x1C : // LEPCHA  
            return  isInRange(c,0x1C2C,0x1C33) ||
                    isInRange(c,0x1C36,0x1C37);
            
        case 0x1D :  // Additional Diacritics
            return  isInRange(c,0x1DC0,0x1DFF) || 
                    c == 0x20E1 ||
                    isInRange(c,0x20E5,0x20E7) ||
                    isInRange(c,0x20E8,0x20F0);
        
        case 0x20 :  
            return  isInRange(c,0x20D0,0x20DC) || 
                    c == 0x20E1 ||
                    isInRange(c,0x20E5,0x20E7) ||
                    isInRange(c,0x20E8,0x20F0);
            
        case 0x30 :
            return isInRange(c,0x302A,0x302F) || 
                    c == 0x3099 || 
                    c == 0x309A ;
            
        default :
            return false;
        }
    }
    
}
