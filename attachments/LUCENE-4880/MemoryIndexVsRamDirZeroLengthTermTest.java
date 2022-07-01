package tst;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.icu.ICUFoldingFilter;

import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttributeImpl;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.search.IndexSearcher;

import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;


public class MemoryIndexVsRamDirZeroLengthTermTest {
	private final String CONTENT_FIELD = "content";
	private final String KEY_FIELD = "key";
	private final Version VERSION = Version.LUCENE_42;

	public IndexSearcher getSingleDocSearcherFromRAMDirectory(String key, String content, Analyzer analyzer) throws IOException {
		Directory dir = new RAMDirectory();
		IndexWriterConfig c = new IndexWriterConfig(VERSION, analyzer);
		IndexWriter w = new IndexWriter(dir, c);
		Document d = new Document();
		d.add(new StringField(KEY_FIELD, key, Field.Store.YES));
		d.add(new TextField(CONTENT_FIELD, content, Field.Store.YES));
		w.addDocument(d);
	
		w.close();
		IndexReader reader = DirectoryReader.open(dir);
		return new IndexSearcher(reader);
	}

	public IndexSearcher getSingleDocSearcherFromMemoryIndex(String key, String content, Analyzer analyzer) throws IOException {
		MemoryIndex index = new MemoryIndex();
		index.addField(KEY_FIELD,  key, analyzer);
		index.addField(CONTENT_FIELD,  content, analyzer);
		return index.createSearcher();
	}

	
	@Test
	public void testCompareMemoryIndexWithRamDirectory() {
	
		Analyzer analyzer = new ICUAnalyzer(VERSION, CharArraySet.EMPTY_SET);
		String s = "duck duck goose \u0640 swan";
		OffsetAttribute ramDirOffset = new OffsetAttributeImpl();
		OffsetAttribute memoryIndexOffset = new OffsetAttributeImpl();

		try{
			
			String key = "1";
			IndexSearcher ramSearcher = getSingleDocSearcherFromRAMDirectory(key, s, analyzer);
			IndexReader ramReader = ramSearcher.getIndexReader();
			Term t = new Term(CONTENT_FIELD, "swan");
			
			
			SpanQuery sq1 = new SpanTermQuery(t);
			sq1 = (SpanQuery)sq1.rewrite(ramReader);
			
			Map<Term, TermContext> m = new HashMap<Term, TermContext>();
			for (AtomicReaderContext r : ramReader.leaves()){
				Spans spans = sq1.getSpans(r,  null, m);
				while (spans.next()){
					System.out.println("RAMDir: " + spans.start() + " : " + spans.end());
					ramDirOffset.setOffset(spans.start(), spans.end());
				}
			}
			m.clear();

			IndexSearcher memorySearcher = getSingleDocSearcherFromMemoryIndex(key, s, analyzer);
			IndexReader memoryReader = memorySearcher.getIndexReader();

			
			SpanQuery sq2 = new SpanTermQuery(t);
			sq2 = (SpanQuery)sq2.rewrite(memoryReader);

			for (AtomicReaderContext r : memoryReader.leaves()){
				Spans spans = sq2.getSpans(r,  null, m);
				while (spans.next()){
					memoryIndexOffset.setOffset(spans.start(), spans.end());
					System.out.println("MEMORY: " + spans.start() + " : " + spans.end());
				}				
			}
			
		} catch (IOException e){
			e.printStackTrace();
		}
		assertEquals(ramDirOffset, memoryIndexOffset);
	}
	
	@Test
	public void testShowOffsets() {
		String content = "duck duck goose \u0640 swan";
		boolean atLeastOneTLengthZero = false;

		try{
			Analyzer analyzer = new ICUAnalyzer(VERSION, CharArraySet.EMPTY_SET);
			
			TokenStream stream = analyzer.tokenStream(CONTENT_FIELD, new StringReader(content));
			OffsetAttribute offsetAtt = stream.getAttribute(org.apache.lucene.analysis.tokenattributes.OffsetAttribute.class);
			CharTermAttribute termAtt = stream.getAttribute(CharTermAttribute.class);

			stream.reset();
			while (stream.incrementToken()){
				String t = termAtt.toString();
				if (t.length() == 0){
					atLeastOneTLengthZero = true;
					System.out.println(offsetAtt.startOffset() + " : " + offsetAtt.endOffset() + " : >" + t+"<");
					String bit = content.substring(offsetAtt.startOffset(), offsetAtt.endOffset());
					System.out.println("HEX: " + toHex(bit, ", "));
					//System.out.println(content.substring(offsetAtt.startOffset(), offsetAtt.endOffset()));
				}
			}
			
		} catch (IOException e){
			e.printStackTrace();
		}
		assertEquals(true, atLeastOneTLengthZero);
	}
	
	public static String toHex(String s, String delimiter){
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length()-1; i++){
			int point = s.codePointAt(i);
			sb.append(Integer.toHexString(point)+delimiter);
		}
		if (s.length() > 0){
			sb.append(Integer.toHexString(s.codePointAt(s.length()-1)));
		}
		return sb.toString();
	}
	
	private class ICUAnalyzer extends Analyzer{

		private CharArraySet stopWords = null;
		private final Version version;
		public ICUAnalyzer(Version version){
			this.version = version;
		}
		public ICUAnalyzer(Version version, CharArraySet stopWords){
			this(version);
			this.stopWords = stopWords;
		}

		@Override
		protected TokenStreamComponents createComponents(String field, Reader reader) {
			Tokenizer stream = new StandardTokenizer(version, reader);
			TokenFilter icu = new ICUFoldingFilter(stream);
			if (stopWords != null && stopWords.size() > 0){
				TokenFilter stop = new StopFilter(version, icu, stopWords);
				return new TokenStreamComponents(stream, stop);
			}
			return new TokenStreamComponents(stream, icu);
			
		}
	}

}
