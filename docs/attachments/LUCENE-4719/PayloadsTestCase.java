import java.io.IOException;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;


public class PayloadsTestCase extends TestCase {
	
	Directory    _id;
	AtomicReader _ir;
	Analyzer     _a;
	
	TokenStream newTokenStream() {
		return new TokenStream() {
			CharTermAttribute ta = addAttribute(CharTermAttribute.class);
			PayloadAttribute  pa = addAttribute(PayloadAttribute.class);
			
			int i=0, n=10;
			
			@Override
			public boolean incrementToken() throws IOException {
				if (i < n) {
					ta.setEmpty().append("value");
					pa.setPayload(new BytesRef(encodeInt(i)));
					i++;
					return true;
				}
				return false;
			}
		};
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void setUp() throws Exception {
		_id = new RAMDirectory();
		_a  = new StandardAnalyzer(Version.LUCENE_CURRENT);
		
		Document d = new Document();
		d.add(new Field("field", newTokenStream(), newTokenStreamFieldType()));
		
		IndexWriter iw = new IndexWriter(_id, new IndexWriterConfig(Version.LUCENE_CURRENT, _a));
		iw.addDocument(d);
		iw.commit();
		iw.close();
		
		_ir = SlowCompositeReaderWrapper.wrap(DirectoryReader.open(_id));
	}
	
	@Override
	protected void tearDown() throws Exception {
		_ir.close();
		_id.close();
	}
	
	public void testPayloadsOnTermVector() throws IOException {
		System.out.println("==== testPayloadsOnTermVector ====");
		Terms ts = _ir.getTermVector(0, "field");
		if (ts != null) {
			TermsEnum te = ts.iterator(null);
			if (te.seekExact(new BytesRef("value"), true)) {
				DocsAndPositionsEnum tp = te.docsAndPositions(null, null);
				if (tp != null) {
					if (tp.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
						for (int i=0; i < tp.freq(); i++) {
							tp.nextPosition();
							System.out.println(te.term().utf8ToString() + ": " + decodeInt(tp.getPayload().bytes, 0));
						}
					}
				}
			}
		}
	}
	
	public void testPayloadsOnTermPositions() throws IOException {
		System.out.println("==== testPayloadsOnTermPositions ====");
		Terms ts = _ir.terms("field");
		if (ts != null) {
			TermsEnum te = ts.iterator(null);
			if (te.seekExact(new BytesRef("value"), true)) {
				DocsAndPositionsEnum tp = te.docsAndPositions(null, null);
				if (tp != null) {
					if (tp.advance(0) == 0) {
						for (int i=0; i < tp.freq(); i++) {
							tp.nextPosition();
							System.out.println(te.term().utf8ToString() + ": " + decodeInt(tp.getPayload().bytes, 0));
						}
					}
				}
			}
		}		
	}
	
	static FieldType newTokenStreamFieldType() {
  	FieldType type = new FieldType();
  	type.setStored(false);
  	type.setIndexed(true);
  	type.setTokenized(true);
  	type.setStoreTermVectors(true);
  	type.setStoreTermVectorPositions(true);
  	type.setStoreTermVectorPayloads(true);
  	type.setStoreTermVectorOffsets(false);	
  	type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
  	type.setOmitNorms(false);
  	type.freeze();
  	return type;
  }
	
	static final int decodeInt(byte[] b, int offset) {
		return
		(b[offset    ] & 0xFF) << 24 |
		(b[offset + 1] & 0xFF) << 16 |
		(b[offset + 2] & 0xFF) <<  8 |
		 b[offset + 3] & 0xFF;
	}
	
	static final byte[] encodeInt(int v) {
		return encodeInt(v, new byte[4], 0);
	}

	static final byte[] encodeInt(int v, byte[] b, int off) {
    b[off    ] = (byte)(v >> 24);
    b[off + 1] = (byte)(v >> 16);
    b[off + 2] = (byte)(v >>  8);
    b[off + 3] = (byte) v;
    return b;
  }
}
