import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.Version;

public class DisjunctionMaxQueryTest {
    private static void addDoc(IndexWriter writer, String field, String text)
	    throws IOException {
	Document doc = new Document();
	doc.add(new TextField(field, text, Store.NO));
	writer.addDocument(doc);
    }

    private static BooleanQuery createBooleanQuery(Occur occur,
	    Query... subqueries) {
	BooleanQuery result = new BooleanQuery();
	for (Query query : subqueries)
	    result.add(query, occur);
	return result;
    }

    private static DisjunctionMaxQuery createDisjunctionMaxQuery(
	    Query... subqueries) {
	return new DisjunctionMaxQuery(Arrays.asList(subqueries), 0.0f);
    }

    public static void main(String[] args) throws IOException {
	// Create an index with 4 documents.
	// Doc 0: "A" x 5, "B" x 4
	// Doc 1: "A" x 4, "B" x 3
	// Doc 2: "A" x 4
	// Doc 3: "B" x 3

	RAMDirectory dir = new RAMDirectory();
	WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_45);
	IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(
		Version.LUCENE_45, analyzer));

	addDoc(writer, "text", "A A A A A B B B B");
	addDoc(writer, "text", "A A A A B B B");
	addDoc(writer, "text", "A A A A");
	addDoc(writer, "text", "B B B");

	writer.close();

	AtomicReader reader = SlowCompositeReaderWrapper.wrap(DirectoryReader
		.open(dir));
	IndexSearcher searcher = new IndexSearcher(reader);

	// "X" does not occur in any documents, so whether term text:"X" is
	// included in a query should not affect the frequencies reported at
	// all. I am including "X" in disjunction clauses to prevent
	// BooleanQuery and DisjunctionMaxQuery from being rewritten into
	// TermQuery.

	Term termA = new Term("text", "A");
	Term termB = new Term("text", "B");
	Term termX = new Term("text", "X");

	TermQuery termAQuery = new TermQuery(termA);
	TermQuery termBQuery = new TermQuery(termB);
	TermQuery termXQuery = new TermQuery(termX);

	BooleanQuery booleanAQuery = createBooleanQuery(Occur.MUST, termAQuery);

	BooleanQuery booleanAAndBQuery = createBooleanQuery(Occur.MUST,
		termAQuery, termBQuery);
	BooleanQuery booleanAAndAQuery = createBooleanQuery(Occur.MUST,
		termAQuery, termAQuery);

	BooleanQuery booleanAOrBQuery = createBooleanQuery(Occur.SHOULD,
		termAQuery, termBQuery);
	BooleanQuery booleanAOrAQuery = createBooleanQuery(Occur.SHOULD,
		termAQuery, termAQuery);
	BooleanQuery booleanAOrXQuery = createBooleanQuery(Occur.SHOULD,
		termAQuery, termXQuery);

	DisjunctionMaxQuery disMaxAQuery = createDisjunctionMaxQuery(termAQuery);

	DisjunctionMaxQuery disMaxABQuery = createDisjunctionMaxQuery(
		termAQuery, termBQuery);
	DisjunctionMaxQuery disMaxAAQuery = createDisjunctionMaxQuery(
		termAQuery, termAQuery);
	DisjunctionMaxQuery disMaxAXQuery = createDisjunctionMaxQuery(
		termAQuery, termXQuery);

	printScorer(reader, searcher, termAQuery);
	printScorer(reader, searcher, termBQuery);
	printScorer(reader, searcher, termXQuery);

	printScorer(reader, searcher, booleanAQuery);

	printScorer(reader, searcher, booleanAAndBQuery);
	printScorer(reader, searcher, booleanAAndAQuery);

	printScorer(reader, searcher, booleanAOrBQuery);
	printScorer(reader, searcher, booleanAOrAQuery);
	printScorer(reader, searcher, booleanAOrXQuery);

	printScorer(reader, searcher, disMaxAQuery);

	printScorer(reader, searcher, disMaxABQuery);
	printScorer(reader, searcher, disMaxAAQuery);
	printScorer(reader, searcher, disMaxAXQuery);

	reader.close();
    }

    private static void printScorer(AtomicReader reader,
	    IndexSearcher searcher, Query query) throws IOException {
	System.out.println("query class = " + query.getClass().getSimpleName()
		+ ", query = " + query);
	Weight weight = searcher.createNormalizedWeight(query);
	if (weight != null) {
	    Scorer scorer = weight.scorer(reader.getContext(), true, false,
		    new Bits.MatchAllBits(reader.maxDoc()));
	    if (scorer != null) {
		System.out.println("* scorer class = "
			+ scorer.getClass().getSimpleName());
		while (scorer.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
		    System.out.println("* doc = " + scorer.docID()
			    + ", freq = " + scorer.freq());
		}
	    } else {
		System.out.println("* no scorer");
	    }
	} else {
	    System.out.println("* no weight");
	}
    }
}
