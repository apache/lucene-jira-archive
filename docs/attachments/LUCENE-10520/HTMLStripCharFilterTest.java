import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class HTMLStripCharFilterTest {

	private static final String HTML_TEXT = "<!DOCTYPE html><html lang=\"en\"><head><title>Test</title></head><body><p class=\"foo>bar\" id=\"baz\">Some text.</p></body></html>";

	@Test
	public void test()
			  throws IOException
	{
		Reader reader = new StringReader(HTML_TEXT);
		HTMLStripCharFilter filter = new HTMLStripCharFilter(reader);
		StringWriter result = new StringWriter();
		filter.transferTo(result);
		assertEquals("Test\n\n\n\nSome text.", result.toString().trim());
	}

}