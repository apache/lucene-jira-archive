/*
   Copyright 2011 Santiago M. Mola <santiago.mola@bitsnbrains.net>

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package net.bitsnbrains.lucene.analysis;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import java.io.IOException;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

/**
 * 
 * @author Santiago M. Mola <santiago.mola@bitsnbrains.net>
 */
public class PhoneFilter extends TokenFilter {

    private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
    private PhoneNumberUtil pnu;
    private String defaultCountry;
    
    public PhoneFilter(TokenStream input, String defaultCountry) {
        super(input);
        pnu = PhoneNumberUtil.getInstance();
        this.defaultCountry = defaultCountry;
    }
    
    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            try {
                PhoneNumber pn = pnu.parse(termAtt.toString(), defaultCountry);
                String result = pnu.format(pn, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
                termAtt.copyBuffer(result.toCharArray(), 0, result.length());
                typeAtt.setType("<PHONE>");
            } catch (NumberParseException ex) {
            }
            return true;
        }
        return false;
    }
    
}
