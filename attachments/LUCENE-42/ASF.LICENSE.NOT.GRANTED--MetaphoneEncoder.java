package org.apache.lucene.search.phonetic;

import org.apache.commons.codec.language.Metaphone;

/**
 * encode using Metaphone algorithym, simple wrapper around apache commons codec
 */
public class MetaphoneEncoder implements PhoneticEncoder {
    Metaphone mp = new Metaphone();
    
    public MetaphoneEncoder(){
        mp.setMaxCodeLen(8);
    }
    
    public String generateKey(String word) {
        return mp.encode(word);
    }
}
