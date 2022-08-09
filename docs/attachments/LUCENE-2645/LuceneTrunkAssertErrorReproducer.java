// javac -cp lucene-core-4.0-SNAPSHOT.jar LuceneTrunkAssertErrorReproducer.java
// java -ea -cp .:lucene-core-4.0-SNAPSHOT.jar LuceneTrunkAssertErrorReproducer

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class LuceneTrunkAssertErrorReproducer {

	/**
	 * Attempt to reproduce an assertion error that happens
	 * only with the trunk version around April 2011.
	 * @param args
	 */
	public static void main(String[] args)
	throws Exception
	{
        Directory directory = new RAMDirectory();
        IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_40, 
                new BugReproAnalyzer());
        IndexWriter iwriter = new IndexWriter(directory, iwc);
        Document doc = new Document();
        doc.add(new Field("eng", "Six drunken" /*This shouldn't matter. */, 
        		Field.Store.YES, Field.Index.ANALYZED));
        iwriter.addDocument(doc);
       // iwriter.optimize();
        iwriter.commit();
        iwriter.close();
	}
}
final class BugReproAnalyzer extends Analyzer{
	@Override
	public TokenStream tokenStream(String arg0, Reader arg1) {
		return new BugReproAnalyzerTokenizer();
	}
}
final class BugReproAnalyzerTokenizer extends TokenStream{
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
	int tokenCount = 4;
	int nextTokenIndex = 0;
	String terms[]=new String[]{"six", "six", "drunken", "drunken"};
	int starts[]=new int[]{0, 0, 4, 4};
	int ends[]=new int[]{3, 3, 11, 11};
	int incs[]=new int[]{1, 0, 1, 0};
	@Override
	public boolean incrementToken() throws IOException {
		if(nextTokenIndex < tokenCount){
			termAtt.setEmpty().append(terms[nextTokenIndex]);
			offsetAtt.setOffset(starts[nextTokenIndex], ends[nextTokenIndex]);
			posIncAtt.setPositionIncrement(incs[nextTokenIndex]);
			nextTokenIndex++;
			return true;			
		}else
			return false;
	}
}
