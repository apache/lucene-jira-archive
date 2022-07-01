package org.apache.lucene.analysis;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.io.IOException;
import java.nio.CharBuffer;

public class NormalizerTokenFilter extends TokenFilter {
	protected Form form;
	public NormalizerTokenFilter(TokenStream input){
		super(input);
		this.form=Form.NFKC;
	}
	public void setForm(Form form){
		this.form=form;
	}
	public Token next(Token result) throws IOException {
		Token nx=input.next(result);
		if(nx==null) return null;
		String nxstr=Normalizer.normalize(CharBuffer.wrap(nx.termBuffer(), 0, nx.termLength()), this.form);
		nx.setTermText(nxstr);
		return nx;
	}
}