import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseTokenizer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Payload;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.payloads.PayloadSpanUtil;
import org.apache.lucene.search.spans.PayloadSpans;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;

public class Test {

	@SuppressWarnings("unchecked")
	public static void main(String args[]) throws Exception {
		IndexWriter writer = new IndexWriter(args[0],
				new TestPayloadAnalyzer(), true,
				IndexWriter.MaxFieldLength.LIMITED);
		Document doc = new Document();
		doc.add(new Field("content", new StringReader(
				"a a b c d e a f g h i j a b k k")));
		writer.addDocument(doc);

		writer.close();

		IndexSearcher is = new IndexSearcher(args[0]);

		SpanTermQuery stq1 = new SpanTermQuery(new Term("content", "a"));
		SpanTermQuery stq2 = new SpanTermQuery(new Term("content", "k"));
		SpanQuery[] sqs = { stq1, stq2 };
		SpanNearQuery snq = new SpanNearQuery(sqs, 30, false);
		
		System.out.println("\ngetPayloadSpans test");
		PayloadSpans pspans = snq.getPayloadSpans(is.getIndexReader());
		while (pspans.next()) {
			System.out.println(pspans.doc() + " - " + pspans.start() + " - "
					+ pspans.end());
			Collection<byte[]> payloads = pspans.getPayload();
			for (Iterator<byte[]> it = payloads.iterator(); it.hasNext();) {
				System.out.println(new String(it.next()));
			}
		}

		System.out.println("\ngetSpans test");
		Spans spans = snq.getSpans(is.getIndexReader());
		while (spans.next()) {
			System.out.println(spans.doc() + " - " + spans.start() + " - "
					+ spans.end());
		}
		
		System.out.println("\nPayloadSpanUtil test");

		PayloadSpanUtil psu = new PayloadSpanUtil(is.getIndexReader());
		Collection<byte[]> pls = psu.getPayloadsForQuery(snq);
		for (Iterator<byte[]> it = pls.iterator(); it.hasNext();) {
			System.out.println(new String(it.next()));
		}

	}
}

class TestPayloadAnalyzer extends Analyzer {

	public TokenStream tokenStream(String fieldName, Reader reader) {
		TokenStream result = new LowerCaseTokenizer(reader);
		result = new PayloadFilter(result, fieldName);
		return result;
	}
}

class PayloadFilter extends TokenFilter {
	String fieldName;

	int pos;

	int i;

	public PayloadFilter(TokenStream input, String fieldName) {
		super(input);
		this.fieldName = fieldName;
		pos = 0;
		i = 0;
	}

	public Token next() throws IOException {

		Token result = new Token();
		result = input.next(result);
		if (result != null) {
			System.out.println(result.term() + " indexed at postion " +  pos);
			result.setPayload(new Payload(("pos: " + pos).getBytes()));
			if (i % 2 == 1) {
				result.setPositionIncrement(1);
			} else {
				result.setPositionIncrement(0);
			}
			pos += result.getPositionIncrement();
			i++;
		}
		return result;
	}
}

