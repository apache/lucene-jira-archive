import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.cn.ChineseTokenizer;

/**
 * @author rayt
 */
public class ChineseTokenizerTest extends TestCase
{
    public void testOtherLetterOffset() throws IOException
    {
        String s = "a天b";
        ChineseTokenizer tokenizer = new ChineseTokenizer(new StringReader(s));
        Token token = null;
        
        int correctStartOffset = 0;
        int correctEndOffset = 1;
        while ((token = tokenizer.next()) != null)
        {
            assertEquals(token.startOffset(), correctStartOffset);
            assertEquals(token.endOffset(), correctEndOffset);
            correctStartOffset++;
            correctEndOffset++;
        }
    }
}