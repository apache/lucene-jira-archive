import java.util.List;
import java.util.Vector;

import org.apache.lucene.queryparser.surround.query.SpanNearClauseFactory;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.vectorhighlight.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.*;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.*;
import org.apache.lucene.index.*;

public class TestMultiPhraseQueryHighlighting {

    static Analyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_40);

	public static void main(String[] args) throws Throwable {
		// Create an index and add one simple doc
	    Directory directory = new RAMDirectory();
	    IndexWriterConfig iwconf = new IndexWriterConfig(Version.LUCENE_40, analyzer);
	    IndexWriter iwriter = new IndexWriter(directory, iwconf);
	    Document doc = new Document();
	    String text = "This is the text to be indexed.";
	    doc.add(new Field("fieldname", text, Field.Store.YES, Field.Index.ANALYZED, TermVector.WITH_POSITIONS_OFFSETS));
	    iwriter.addDocument(doc);
	    iwriter.close();
	    IndexSearcher isearcher = new IndexSearcher(directory, true);

	    // Simple queries that search for the phrase "is the text":
	    List<Query> queries = new Vector<Query>();
	    {
	    	// Does NOT work with FastVectorHighlighter /!\ 
		    MultiPhraseQuery q = new MultiPhraseQuery();
		    q.setSlop(3);
		    q.add(new Term("fieldname", "is"));
		    //q.add(new Term("fieldname", "the")); // You can use this instead of the next line if you wish, results are the same
		    q.add(new Term[]{ new Term("fieldname", "a"), new Term("fieldname", "the") });
		    q.add(new Term("fieldname", "text"));
	    	queries.add(q);
	    }
	    {
		    PhraseQuery q = new PhraseQuery();
		    q.setSlop(3);
		    q.add(new Term("fieldname", "is"));
		    q.add(new Term("fieldname", "the"));
		    q.add(new Term("fieldname", "text"));
	    	queries.add(q);
	    }
	    {
	    	// Does NOT work with FastVectorHighlighter either /!\ 
	    	SpanQuery clauses[] = new SpanQuery[]{ new SpanTermQuery(new Term("fieldname", "is")), new SpanTermQuery(new Term("fieldname", "the")), new SpanTermQuery(new Term("fieldname", "text")) };
	    	SpanNearQuery q = new SpanNearQuery(clauses, 3, true);
	    	queries.add(q);
	    }
	    
	    // Try both PhraseQuery and MultiPhraseQuery
	    for (Query query : queries) {
	    	System.out.println("Query: "+query.toString("fieldname")+" ("+query.getClass().getSimpleName()+")");
		    ScoreDoc[] hits = isearcher.search(query, null, 1000).scoreDocs;
		    System.out.println(""+hits.length+" result(s)");
		    // Try both highlighters
		    highlight(isearcher, query, hits);
		    vectorhighlight(isearcher, query, hits);
		    System.out.println();
	    }
	    
	    isearcher.close();
	    directory.close();
	}
	
	static void vectorhighlight(IndexSearcher isearcher, Query query, ScoreDoc[] hits) throws Throwable {
	    System.out.println("→ Fast Vector Highlighter");
	    FastVectorHighlighter highlighter = new FastVectorHighlighter(true, true);
	    // Iterate through the results:
		for (int i = 0; i < hits.length; i++) {
			Document hitDoc = isearcher.doc(hits[i].doc);
			String[] frag = highlighter.getBestFragments(highlighter.getFieldQuery(query), isearcher.getIndexReader(), hits[i].doc, "fieldname", 150, 3);
			if (frag.length == 0) System.out.println("  No fragment!");
			for (int j = 0; j < frag.length; j++) {
				System.out.println("  "+(frag[j].toString()));
			}
		}
	}
	
	static void highlight(IndexSearcher isearcher, Query query, ScoreDoc[] hits) throws Throwable {
	    System.out.println("→ Highlighter");
	    SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
	    Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
	    // Iterate through the results:
		for (int i = 0; i < hits.length; i++) {
			Document hitDoc = isearcher.doc(hits[i].doc);
			String text = hitDoc.get("fieldname");
			TokenStream tokenStream = TokenSources.getAnyTokenStream(isearcher.getIndexReader(), hits[i].doc, "fieldname", analyzer);
			TextFragment[] frag = highlighter.getBestTextFragments(tokenStream, text, false, 10);
			if (frag.length == 0) System.out.println("  No fragment!");
			for (int j = 0; j < frag.length; j++) {
				if ((frag[j] != null) && (frag[j].getScore() > 0)) {
					System.out.println("  "+(frag[j].toString()));
				}
			}
		}
	}

}
