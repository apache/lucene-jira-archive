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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.solr.analysis.BaseTokenFilterFactory;
import org.apache.solr.analysis.ISOLatin1AccentFilterFactory;
import org.apache.solr.analysis.WordDelimiterFilterFactory;

public class UnicodeNormalizationFilterFactory extends BaseTokenFilterFactory 
{
	public TokenStream create(TokenStream input) 
	{
		try
        {
            return new UnicodeNormalizationFilter(input, icu4jVersion, composedForm, removeDiacritics, removeSpacingModifiers, fold);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return(input);
         }
	}

	private boolean icu4jVersion;
    private boolean composedForm;
    private boolean removeDiacritics;
    private boolean removeSpacingModifiers;
    private boolean fold;
	
	public void init(Map<String, String> args)
	{
	    super.init(args);	    
	    String version = args.get("version");
        composedForm = getBoolean("composed", false);
        removeDiacritics = getBoolean("remove_diacritics", true);
        removeSpacingModifiers = getBoolean("remove_modifiers", false);
        fold = getBoolean("fold", false);
	    if (version.equalsIgnoreCase("icu4j")) icu4jVersion = true;
	    else                                   icu4jVersion = false;
	}
	
	public static void main(String args[])
	{
	    Map<String, String> argMap = new HashMap<String, String>();
	    argMap.put("version", "icu4j");
        argMap.put("composed", "false");
        argMap.put("remove_diacritics", "true");
        argMap.put("remove_modifiers", "true");
        argMap.put("fold", "true");
	    String filename = null;
	    for (int i = 0; i < args.length; i++)
	    {
	        String arg = args[i];
	        String parts[] = arg.split(" *= *");
            if (parts[0].equals("version"))  
            {  
                parts[1] = parts[1].replaceAll("\"", ""); 
                argMap.put(parts[0], parts[1]); 
            }
            else if (parts[0].equals("composed")) 
            {  
                parts[1] = parts[1].replaceAll("\"", ""); 
                argMap.put(parts[0], parts[1]); 
            }
            else if (parts[0].equals("remove_diacritics")) 
            {  
                parts[1] = parts[1].replaceAll("\"", ""); 
                argMap.put(parts[0], parts[1]); 
            }
            else if (parts[0].equals("remove_modifiers")) 
            {  
                parts[1] = parts[1].replaceAll("\"", ""); 
                argMap.put(parts[0], parts[1]); 
            }
            else if (parts[0].equals("fold")) 
            {  
                parts[1] = parts[1].replaceAll("\"", ""); 
                argMap.put(parts[0], parts[1]); 
            }
            else
            {  
                filename = parts[0];
            }
	    }
	    TokenStream ts = null;
	    
	    if (filename != null)
        {
            try
            {
                BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
                String line;
                StringBuffer sb = new StringBuffer();
                while ((line = reader.readLine()) != null)
                {   
                    line = line.replaceFirst("#.*", "");
                    line = line.trim();
                    if (line.length() == 0) continue;
                    String pieces[] = line.split("[ ;\t]+");
                    for (int i = 0; i < pieces.length; i++)
                    {
                        int val = Integer.parseInt(pieces[i], 16);
                        char c = (char)(val);
                        if (i == pieces.length -1) sb.append(" = ");
                        sb.append(c);                        
                    }
                    sb.append("\n");
                }
                ts = new WhitespaceTokenizer(new StringReader(sb.toString()));
            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (ts == null)
	    {
	        ts = new WhitespaceTokenizer(new StringReader("Sorcie\u0300re sorcière Pe\u0301rez, Matilde Pérez A\u0308\uFB03ne Äffine " + 
	                                                              "Ra\u0308ume Räume Zarin\u0326s\u030c, Zarin\u0326\u0161, "+
	                                                            "Marg\u0313eris Ocherki sot\u0361sial\u02b9noi\u0306 sot\u0361sial\u02b9no\u012d  e\u0307mbriologii "+
	                                                            "\u0141adyzhenskii\u0306 \u0141adyzhenskii\u0306, A. M. Zarin\u0326s\u030c, Marg\u0313eris"));
	    }
	    if (!argMap.get("version").equals("raw"))
	    {
	        UnicodeNormalizationFilterFactory factory = new UnicodeNormalizationFilterFactory();
	        factory.init(argMap);
	        TokenStream normalFilter = factory.create(ts);
//        <filter class="solr.WordDelimiterFilterFactory" generateWordParts="0" generateNumberParts="0" catenateWords="1" catenateNumbers="1" catenateAll="0"/>
    	    ISOLatin1AccentFilterFactory IsoLatinFilterFactory = new ISOLatin1AccentFilterFactory();
    	    IsoLatinFilterFactory.init(new HashMap<String,String>());
    	    WordDelimiterFilterFactory wordDelimitFilterFactory = new WordDelimiterFilterFactory();
            Map<String, String> argMap2 = new HashMap<String, String>();
            argMap2.put("generateWordParts", "1");
            argMap2.put("generateNumberParts", "1");
            argMap2.put("catenateWords", "1");
            argMap2.put("catenateNumbers", "1");
            argMap2.put("catenateAll", "0");
            wordDelimitFilterFactory.init(argMap2);
            TokenStream latinFilter = normalFilter;
            TokenStream delimitFilter;
    	    if (factory.composedForm) 
    	    { 
    	        latinFilter = IsoLatinFilterFactory.create(normalFilter);
    	        delimitFilter = wordDelimitFilterFactory.create(latinFilter);
    	    }
    	    else
    	    {
    	        delimitFilter = wordDelimitFilterFactory.create(normalFilter);
    	    }
    	    ts = delimitFilter;
	    }
        if (filename != null)
        {
            Token t1;
            Token t2;
            do {
                try
                {
                    t1 = ts.next();
                    t2 = ts.next();
                }
                catch (IOException e)
                {
                    t1 = null;
                    t2 = null;
                    e.printStackTrace();
                }
                if (t1 != null ) 
                {
                      System.out.println("Token = "+ t1.termText());
                }
              if (t1 != null && t2 != null && !t1.termText().equals(t2.termText())) 
              {
                  System.out.println("Token differ= "+ t1.termText()+" -- "+ t2.termText());
              }
              else if (t1 != null && t2 != null )
              {
                  System.out.println("Token match= "+ t1.termText()+" -- "+ t2.termText());
              }
            } while (t1 != null && t2 != null);
        }
        else
        {
            Token t1;
            do {
                try
                {
                    t1 = ts.next();
                }
                catch (IOException e)
                {
                    t1 = null;
                    e.printStackTrace();
                }
                if (t1 != null ) 
                {
                      System.out.println("Token = "+ t1.termText());
                }
            } while (t1 != null);
        }
	}
}
