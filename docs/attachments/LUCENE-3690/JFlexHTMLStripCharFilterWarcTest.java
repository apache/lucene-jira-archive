import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.ibm.icu.text.CharsetDetector;
import edu.cmu.lemurproject.WarcRecord;
import org.apache.lucene.analysis.CharReader;
import org.apache.lucene.analysis.charfilter.JFlexHTMLStripCharFilter;

public class JFlexHTMLStripCharFilterWarcTest {

    private static final Pattern metaCharsetPattern 
	= Pattern.compile("<\\s*meta\\s+http-equiv\\s*=\\s*['\"]?Content-type.*charset\\s*=\\s*([^>\\s\"]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern httpHeaderCharsetPattern 
	= Pattern.compile("Content-Type:.*charset\\s*=\\s*([^>\\s\"]+)", Pattern.CASE_INSENSITIVE);
    private static final Charset UTF8Charset = Charset.forName("UTF-8");

    public static void main(String... args) throws IOException {
	if (args.length != 1) {
	    System.err.println("Usage: JFlexHTMLStripCharFilterWarcTest <warc-gz-file>");
	    System.exit(1);
	}
	String inputWarcFile = args[0];
	GZIPInputStream gzInputStream = new GZIPInputStream(new FileInputStream(inputWarcFile));
	DataInputStream inStream=new DataInputStream(gzInputStream);
	WarcRecord warcRecord;
	while (null != (warcRecord = WarcRecord.readNextWarcRecord(inStream))) {
	    if (warcRecord.getHeaderRecordType().equals("response")) {
		byte[] content = warcRecord.getContent();
		int afterBlankLinePos = 0;
		byte prevByte = 0;
		byte byteBeforePrevByte = 0;
		while ( ! (content[afterBlankLinePos++] == '\n'
			   && (prevByte == '\n'
			       || (prevByte == '\r' && byteBeforePrevByte == '\n')))) {
		    byteBeforePrevByte = prevByte;
		    prevByte = content[afterBlankLinePos - 1];
		}
		Reader inReader = new InputStreamReader(new ByteArrayInputStream(content), UTF8Charset);
		StringBuilder builder = new StringBuilder();
		int ch;
		while (-1 != (ch = inReader.read())) {
		    builder.append((char)ch);
		}
		inReader.close();
		Charset declaredCharset = null;
		Matcher matcher = metaCharsetPattern.matcher(builder);
		if (matcher.find()) {
		    try {
			declaredCharset = Charset.forName(matcher.group(1));
		    } catch (Exception e) {
			// Ignore bad charset names
		    }
		}
		if (null == declaredCharset) {
		    matcher = httpHeaderCharsetPattern.matcher(builder);
		    if (matcher.find()) {
			try {
			    declaredCharset = Charset.forName(matcher.group(1));
			} catch (Exception e) {
			    // Ignore bad charset names
			}
		    }
		}
		InputStream contentAfterHeader = new ByteArrayInputStream
		    (content, afterBlankLinePos, content.length - afterBlankLinePos);
		CharsetDetector detector = new CharsetDetector();
		String declaredCharsetName = (null == declaredCharset ? null : declaredCharset.name());
		try {
		    inReader = detector.getReader(contentAfterHeader, declaredCharsetName);
		} catch (Exception e) { /* do nothing */ }
		if (null == inReader) {
		    Charset charset = null == declaredCharset ? UTF8Charset : declaredCharset;
		    inReader = new InputStreamReader(contentAfterHeader, charset);
		}
		Reader charFilterReader = new JFlexHTMLStripCharFilter(CharReader.get(inReader));
		int inputBufferLength = 16284;
		char[] inputBuffer = new char[inputBufferLength];
		try {
		    while (charFilterReader.read(inputBuffer) > 0);
		} catch (Throwable t) {
		    String trecID = warcRecord.getHeaderMetadataItem("WARC-TREC-ID");
		    System.err.format("====== %s ======\n", trecID);
		    t.printStackTrace(System.err);
		}
	    }
	}
	inStream.close();
    }
}

