package org.apache.lucene.analysis;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

public class PassTokenizer extends Tokenizer {
	private boolean done;
	
	public PassTokenizer(Reader input){
		super(input);
		done=false;
	}
	
	public Token next(Token token) throws IOException {
		if(done) return null;
		done=true;
		
		StringBuffer sb=new StringBuffer();
		char[] buf=new char[255];
		while(true){
			int dataLen=input.read(buf);
			if(dataLen == -1) break;
			sb.append(buf,0,dataLen);
		}
		token.clear();
		String txt=sb.toString();
		token.setTermText(txt);
		token.setStartOffset(0);
		token.setEndOffset(txt.length());
		return token;
	}
	public void reset(Reader input) throws IOException {
		super.reset(input);
		done=false;
	}
}
