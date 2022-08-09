/* $Id$ */

/**
 * Copyright 2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.CharArrayWriter;
import java.io.IOException;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

/**
 * Normalizes extended latin (not greek, cyrillic etc.) umlauts and diacritics to their closest
 * ASCII (basic Latin, up to 0x7f) equivalents. This class ensures that only 7-bit
 * chars are returned.
 *
 * @author  <a href="mailto:ronnie.kolehmainen@ub.uu.se">Ronnie Kolehmainen</a>
 * @version $Revision$, $Date$
 */
public final class ASCIIFilter extends TokenFilter
{

    /**
     * Constructor.
     * @param  in the token stream to normalize.
     */
    public ASCIIFilter(TokenStream in)
    {
        super(in);
    }
    
    /**
     * Normalizes umlauts and diacritics to their closest
     * ASCII equivalents. Strips unknown chars. All chars
     * less the 0x80 are untouched.
     * @return the normalized token, or <tt>null</tt> if input
     *         token from underlying stream is null.
     */
    public final Token next() throws IOException
    {
        if (input == null) {
            throw new RuntimeException("ASCIIFilter::next input == null");
        }
        Token t = input.next();
        if (t == null) {
            return null;
	}
        
        char[] chars = t.termText().toCharArray();
        CharArrayWriter charWriter = null;
        boolean modified = false;
        
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] > 0x7f) {
                // non-ascii
                if (charWriter == null) {
                    charWriter = new CharArrayWriter(chars.length);
                    if (i > 0) {
                        // fill with already scanned (ascii) chars
                        charWriter.write(chars, 0, i);
                    }
                }
                modified = true; // mark as modified to create new Token
                
                switch (chars[i]) {
                    
                case 0x00c0:
                case 0x00c1:
                case 0x00c2:
                case 0x00c3:
                case 0x00c4:
                case 0x00c5:
                case 0x00c6:
                case 0x0100:
                case 0x0102:
                case 0x0104:
                case 0x01cd:
                case 0x01fa:
                case 0x01fc:
                case 0x1ea0:
                case 0x1ea2:
                case 0x1ea4:
                case 0x1ea6:
                case 0x1ea8:
                case 0x1eaa:
                case 0x1eac:
                case 0x1eae:
                case 0x1eb0:
                case 0x1eb2:
                case 0x1eb4:
                case 0x1eb6:
                    charWriter.write('A');
                    break;
                    
                case 0x00e0:
                case 0x00e1:
                case 0x00e2:
                case 0x00e3:
                case 0x00e4:
                case 0x00e5:
                case 0x00e6:
                case 0x0101:
                case 0x0103:
                case 0x0105:
                case 0x01ce:
                case 0x01fb:
                case 0x01fd:
                case 0x1ea1:
                case 0x1ea3:
                case 0x1ea5:
                case 0x1ea7:
                case 0x1ea9:
                case 0x1eab:
                case 0x1ead:
                case 0x1eaf:
                case 0x1eb1:
                case 0x1eb3:
                case 0x1eb5:
                case 0x1eb7:
                    charWriter.write('a');
                    break;
                    
                case 0x00c8:
                case 0x00c9:
                case 0x00ca:
                case 0x00cb:
                case 0x0112:
                case 0x0114:
                case 0x0116:
                case 0x0118:
                case 0x011a:
                case 0x1eb8:
                case 0x1eba:
                case 0x1ebc:
                case 0x1ebe:
                case 0x1ec0:
                case 0x1ec2:
                case 0x1ec4:
                case 0x1ec6:
                    charWriter.write('E');
                    break;
                    
                case 0x00e8:
                case 0x00e9:
                case 0x00ea:
                case 0x00eb:
                case 0x0113:
                case 0x0115:
                case 0x0117:
                case 0x0119:
                case 0x011b:
                case 0x1eb9:
                case 0x1ebb:
                case 0x1ebd:
                case 0x1ebf:
                case 0x1ec1:
                case 0x1ec3:
                case 0x1ec5:
                case 0x1ec7:
                    charWriter.write('e');
                    break;
                    
                case 0x00cc:
                case 0x00cd:
                case 0x00ce:
                case 0x00cf:
                case 0x0128:
                case 0x012a:
                case 0x012c:
                case 0x012e:
                case 0x0130:
                case 0x01cf:
                case 0x1ec8:
                case 0x1eca:
                    charWriter.write('I');
                    break;
                    
                case 0x00ec:
                case 0x00ed:
                case 0x00ee:
                case 0x00ef:
                case 0x0129:
                case 0x012b:
                case 0x012d:
                case 0x012f:
                case 0x0131:
                case 0x01d0:
                case 0x1ec9:
                case 0x1ecb:
                    charWriter.write('i');
                    break;
                    
                case 0x00d2:
                case 0x00d3:
                case 0x00d4:
                case 0x00d5:
                case 0x00d6:
                case 0x00d8:
                case 0x014c:
                case 0x014e:
                case 0x0150:
                case 0x0152:
                case 0x01a0:
                case 0x01d1:
                case 0x01fe:
                case 0x1ecc:
                case 0x1ece:
                case 0x1ed0:
                case 0x1ed2:
                case 0x1ed4:
                case 0x1ed6:
                case 0x1ed8:
                case 0x1eda:
                case 0x1edc:
                case 0x1ede:
                case 0x1ee0:
                case 0x1ee2:
                    charWriter.write('O');
                    break;
                    
                case 0x00f2:
                case 0x00f3:
                case 0x00f4:
                case 0x00f5:
                case 0x00f6:
                case 0x00f8:
                case 0x014d:
                case 0x014f:
                case 0x0151:
                case 0x0153:
                case 0x01a1:
                case 0x01d2:
                case 0x01ff:
                case 0x1ecd:
                case 0x1ecf:
                case 0x1ed1:
                case 0x1ed3:
                case 0x1ed5:
                case 0x1ed7:
                case 0x1ed9:
                case 0x1edb:
                case 0x1edd:
                case 0x1edf:
                case 0x1ee1:
                case 0x1ee3:
                    charWriter.write('o');
                    break;
                    
                case 0x00d9:
                case 0x00da:
                case 0x00db:
                case 0x00dc:
                case 0x0168:
                case 0x016a:
                case 0x016c:
                case 0x016e:
                case 0x0170:
                case 0x0172:
                case 0x01af:
                case 0x01d3:
                case 0x01d5:
                case 0x01d7:
                case 0x01d9:
                case 0x01db:
                case 0x1ee4:
                case 0x1ee6:
                case 0x1ee8:
                case 0x1eea:
                case 0x1eec:
                case 0x1eee:
                case 0x1ef0:
                    charWriter.write('U');
                    break;
                    
                case 0x00f9:
                case 0x00fa:
                case 0x00fb:
                case 0x00fc:
                case 0x0169:
                case 0x016b:
                case 0x016d:
                case 0x016f:
                case 0x0171:
                case 0x0173:
                case 0x01b0:
                case 0x01d4:
                case 0x01d6:
                case 0x01d8:
                case 0x01da:
                case 0x01dc:
                case 0x1ee5:
                case 0x1ee7:
                case 0x1ee9:
                case 0x1eeb:
                case 0x1eed:
                case 0x1eef:
                case 0x1ef1:
                    charWriter.write('u');
                    break;
                    
                case 0x00dd:
                case 0x0176:
                case 0x0178:
                case 0x1ef2:
                case 0x1ef4:
                case 0x1ef6:
                case 0x1ef8:
                    charWriter.write('Y');
                    break;
                    
                case 0x00fd:
                case 0x00ff:
                case 0x0177:
                case 0x1ef3:
                case 0x1ef5:
                case 0x1ef7:
                case 0x1ef9:
                    charWriter.write('y');
                    break;
                    
                case 0x00c7:
                case 0x0106:
                case 0x0108:
                case 0x010a:
                case 0x010c:
                    charWriter.write('C');
                    break;
                    
                case 0x00e7:
                case 0x0107:
                case 0x0109:
                case 0x010b:
                case 0x010d:
                    charWriter.write('c');
                    break;
                    
                case 0x00d0:
                case 0x010e:
                case 0x0110:
                    charWriter.write('D');
                    break;
                    
                case 0x010f:
                case 0x0111:
                    charWriter.write('d');
                    break;
                    
                case 0x011c:
                case 0x011e:
                case 0x0120:
                case 0x0122:
                    charWriter.write('G');
                    break;
                    
                case 0x011d:
                case 0x011f:
                case 0x0121:
                case 0x0123:
                    charWriter.write('g');
                    break;
                    
                case 0x0124:
                case 0x0126:
                    charWriter.write('H');
                    break;
                    
                case 0x0125:
                case 0x0127:
                    charWriter.write('h');
                    break;
                    
                case 0x0134:
                    charWriter.write('J');
                    break;
                    
                case 0x0135:
                    charWriter.write('j');
                    break;
                    
                case 0x0136:
                    charWriter.write('K');
                    break;
                    
                case 0x0137:
                    charWriter.write('k');
                    break;
                    
                case 0x0139:
                case 0x013b:
                case 0x013d:
                case 0x013f:
                case 0x0141:
                    charWriter.write('L');
                    break;
                    
                case 0x013a:
                case 0x013c:
                case 0x013e:
                case 0x0140:
                case 0x0142:
                    charWriter.write('l');
                    break;
                    
                case 0x0143:
                case 0x0145:
                case 0x0147:
                case 0x014a:
                    charWriter.write('N');
                    break;
                    
                case 0x00f1:
                case 0x0144:
                case 0x0146:
                case 0x0148:
                case 0x0149:
                case 0x014b:
                    charWriter.write('n');
                    break;
                    
                case 0x0154:
                case 0x0156:
                case 0x0158:
                    charWriter.write('R');
                    break;
                    
                case 0x0155:
                case 0x0157:
                case 0x0159:
                    charWriter.write('r');
                    break;
                    
                case 0x015a:
                case 0x015c:
                case 0x015e:
                case 0x0160:
                    charWriter.write('S');
                    break;
                    
                case 0x00df:
                case 0x015b:
                case 0x015d:
                case 0x015f:
                case 0x0161:
                    charWriter.write('s');
                    break;
                    
                case 0x0162:
                case 0x0164:
                case 0x0166:
                    charWriter.write('T');
                    break;
                    
                case 0x0163:
                case 0x0165:
                case 0x0167:
                    charWriter.write('t');
                    break;
                    
                case 0x0174:
                case 0x0180:
                case 0x0182:
                case 0x0184:
                    charWriter.write('W');
                    break;
                    
                case 0x0175:
                case 0x0181:
                case 0x0183:
                case 0x0185:
                    charWriter.write('w');
                    break;
                    
                case 0x0179:
                case 0x017b:
                case 0x017d:
                    charWriter.write('Z');
                    break;
                    
                case 0x017a:
                case 0x017c:
                case 0x017e:
                    charWriter.write('z');
                    break;
                    
                default:
                    // prune non-matching chars, i.e. do nothing
                    break;
                }
                
            } else {
                if (charWriter != null) {
                    charWriter.write(chars[i]);
                }
            }
        }
        
        if (modified) {
            // 	    System.out.println("ASCIIFilter modified token: '" + t.termText() +
            // 			       "' to '" + charWriter.toString() + "'");
            return new Token(charWriter.toString(),
                             t.startOffset(),
                             t.endOffset(),
                             t.type());
        } else {
            return t;
        }
    }
}
