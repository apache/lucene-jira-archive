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

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.HashMap;

/** 
 * CharDelimiterTokenizer is a tokenizer that divides text with delimter char.
 * Delimiter chars are configurable.
 */
public class CharDelimiterTokenizer extends Tokenizer {
  private int pos;
  
  boolean whitespaceDelimiter=false;
//  // XXX: When we move to Java5, we should use GENERICS here.
//  Map<Character,Boolean> delimiterMap;
  protected Map delimiterMap;
  
  public CharDelimiterTokenizer(Reader input){
    super(input);
    this.pos=0;
    this.delimiterMap=new HashMap();
  }
  
  /**
   * Specifies a char as a delimiter.
   * @param c Char to add as a delimter
   */
  public void addDelimiter(char c){
    this.delimiterMap.put(new Character(c),new Boolean(true));
  }
  
  /**
   * Sets white space char as deimiter
   *
   * By default, whitespaces are not delimters.
   * Whitespaces are Unicode space characters and some additional chars 
   * which we can test by java.lang.Characer.isWhitespace(char).
   * 
   * @see java.lang.Character#isWhitespace(char)
   * @param flag True to switch whitespace as delimiter
   */
  public void setWhitespaceDelimiter(boolean flag){
    this.whitespaceDelimiter=flag;
  }
  
  protected boolean isTokenChar(char c){
    if(this.whitespaceDelimiter && Character.isWhitespace(c)) return false;
    
    Boolean flag=(Boolean)this.delimiterMap.get(new Character(c));
    if(flag != null) return !flag.booleanValue();
    
    return true;
  }
  
  public Token next(Token token) throws IOException {
    boolean gotChar=false;
    StringBuffer sb=new StringBuffer();
    int pos_start=this.pos;
    char[] ca=new char[1];
    while(true){
      int dataLen=this.input.read(ca);
      if(dataLen < 0) break;
      char c=ca[0];
      
      gotChar=true;
      this.pos++;
      if(this.isTokenChar(c)){
        sb.append(c);
      }else{
        break;
      }
    }
    if(!gotChar) return null;
    
    token.clear();
    token.setTermText(sb.toString());
    token.setStartOffset(pos_start);
    token.setEndOffset(this.pos);
    return token;
  }
  
  public void reset(Reader input) throws IOException {
    super.reset(input);
    this.pos=0;
  }
}
