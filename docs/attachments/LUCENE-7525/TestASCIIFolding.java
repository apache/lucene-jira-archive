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

import org.apache.lucene.util.LuceneTestCase;

import junit.framework.Assert;

public class TestASCIIFolding extends LuceneTestCase {

  public void testSimpleString() {
    Assert.assertEquals("The quick brown fox jumped over the lazy dog", testQuickFold("The quick brown fox jumped over the lazy dog"));
  }
  
  public void testAccentedString() {
    Assert.assertEquals("Des mot cles", testQuickFold("Des mot clés"));
  }
  
  public void testExtremes() {
    Assert.assertEquals("\"", testQuickFold("\u00AB"));
    Assert.assertEquals("~", testQuickFold("\uFF5E"));
  }
  
  public void testLongerString() {
    Assert.assertEquals("Des mot cles A LA CHAINE A A A A A A AE C E E E E I I I I IJ D N O O O O O O OE TH U U U U Y Y a a a a a a ae c e e e e i i i i ij d n o o o o o o oe ss th u u u u y y fi fl", 
        testQuickFold("Des mot clés À LA CHAÎNE À Á Â Ã Ä Å Æ Ç È É Ê Ë Ì Í Î Ï Ĳ Ð Ñ"
      +" Ò Ó Ô Õ Ö Ø Œ Þ Ù Ú Û Ü Ý Ÿ à á â ã ä å æ ç è é ê ë ì í î ï ĳ"
      +" ð ñ ò ó ô õ ö ø œ ß þ ù ú û ü ý ÿ ﬁ ﬂ"));
  }
  
  private String testQuickFold(String input) {
    char[] output = new char[input.length() * 4];
    int length = ASCIIFolding.foldToASCII(input.toCharArray(), 0, output, 0, input.length());
    return new String(output, 0, length);
  }
  
}
