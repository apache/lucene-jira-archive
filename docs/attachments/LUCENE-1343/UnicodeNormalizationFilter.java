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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;


/**
 * Simple filter that runs all tokens through icu4j unicode normalizer 
 * transforming them into the Unicode Composed Normalized Form.
 * Changing letters followed by Combining characters for accent marks into
 * a single character consisting of the accented form of the letter.
 *
 */
public class UnicodeNormalizationFilter extends TokenFilter 
{
    private Method normalize;
    private Object mode;
    private boolean composed;
    private boolean removeDiacritics;
    private boolean removeSpacingModifiers;
    private boolean fold;
    
 	public UnicodeNormalizationFilter(TokenStream in, boolean icu4jVersion, boolean composedForm, 
 	                                  boolean removeDia, boolean removeMod, boolean fold) 
 	       throws ClassNotFoundException, SecurityException, NoSuchMethodException, 
 	       NoSuchFieldException, IllegalArgumentException, IllegalAccessException, InvocationTargetException 
	{
		super(in);
        composed = composedForm;
        removeDiacritics = removeDia;
        removeSpacingModifiers = removeMod;
        this.fold = fold;
        if (icu4jVersion)
        {
            Class cl = Class.forName("com.ibm.icu.text.Normalizer");
            Field field = cl.getField(composed ? "NFKC" : "NFKD");
            mode = field.get(null);
            normalize = cl.getMethod("normalize", String.class, field.getType());
        }
        else
        {
            Class cl = Class.forName("java.text.Normalizer");
            Class cl1 = Class.forName("java.text.Normalizer$Form");
            normalize = cl.getMethod("normalize", CharSequence.class, cl1);
            Method getMode = cl1.getMethod("valueOf", String.class);
            mode = getMode.invoke(null, composed ? "NFKC" : "NFKD");
        }
 	}
	/**
	 * Uses the static <i>normalize</i> method from the Normalizer class
	 * to convert tokens to a standard format for searching/faceting. 
	 */
	public Token next() throws IOException 
	{
		final Token t = input.next();
		if (t == null)  return null;
		
        // see Normalizer.Form enum to choose a normalization form
        String termtext = t.termText();
        String normtext;
        
        try
        {
            normtext = normalize.invoke(null, termtext, mode).toString();
            if (removeDiacritics || removeSpacingModifiers || fold)
            {
                StringBuffer newNormText = new StringBuffer();
                for (int i = 0; i < normtext.length(); i++)
                {
                    char c = normtext.charAt(i);
                    char foldC;
                    if (removeDiacritics && UnicodeCharUtil.isCombiningCharacter(c))
                    {
                                    
                    }
                    else if (removeSpacingModifiers && UnicodeCharUtil.isSpacingModifier(c))
                    {
                    
                    }
                    else if (fold && (foldC = UnicodeCharUtil.foldNonDiacriticChar(c)) != 0x00)
                    {
                        newNormText.append(foldC);
                    }
                    else
                    {
                        newNormText.append(c);
                    }
                }
                normtext = newNormText.toString();
            }
        }
        catch (Exception e)
        {
            return(t);
        }
        
        if (!normtext.equals(termtext))
        {
            t.setTermText(normtext);
        }
        return(t);
	}		

}
