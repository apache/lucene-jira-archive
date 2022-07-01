import org.apache.lucene.document.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.QueryParser;

class LuceneTest 
{
    static String[] docs = {
        "a", "b", "c", "d", 
        "a b", "a c", "a d", "b c", "b d", "c d", 
        "a b c", "a b d", "a c d", "b c d", 
        "a b c d"
    };

    static String[] queries = {
        "a OR b AND c",
        "(a OR b) AND c",
        "a OR (b AND c)",
        "a AND b",
        "a AND b OR c AND d",
        "(a AND b) OR (c AND d)",
        "a AND (b OR c) AND d",
        "((a AND b) OR c) AND d",
        "a AND (b OR (c AND d))",
        "a AND b AND c AND d",

        "a OR b AND NOT c",
        "(a OR b) AND NOT c",
        "a OR (b AND NOT c)",
        "a AND NOT d",
        "a AND NOT b OR c AND NOT d",
        "(a AND NOT b) OR (c AND NOT d)",
        "a AND NOT (b OR c) AND NOT d",
        "((a AND NOT b) OR c) AND NOT d",
        "a AND NOT (b OR (c AND NOT d))",
        "a AND NOT b AND NOT c AND NOT d",
	
	"a OR NOT b",
	"a OR NOT a",

	"a b",
	"a b c",
	"a b (c d e)",
	"+a +b",
	"a -b",
	"a +b -c",
	"+a b -c",
	"+a -b c",
	"a -b -c",
	"-a b c",

	"a OR b c AND d",
	"a OR b c",
	"a AND b c",
	"a OR b c OR d",
	"a OR b c d OR e",
	"a AND b c AND d",
	"a AND b c d AND e"
    };

    public static void main(String argv[]) throws Exception {
        Directory dir = new RAMDirectory();
        String[] stop = {};
        Analyzer analyzer = new StandardAnalyzer(stop);
        
        IndexWriter writer = new IndexWriter(dir, analyzer, true);

        for ( int i=0; i < docs.length; i++ ) {
            Document doc = new Document();
            doc.add(Field.Text("text", docs[i]));
            writer.addDocument(doc);
        }
        writer.close();

        Searcher searcher = new IndexSearcher(dir);
        for ( int i=0; i < queries.length; i++ ) {
	    QueryParser parser = new QueryParser("text", analyzer);
	    parser.setOperator(QueryParser.DEFAULT_OPERATOR_AND);

	    Query [] query = new Query[4];

            query[0] = QueryParser.parse(queries[i], "text", analyzer);
	    query[1] = QueryParser.parse(query[0].toString("text"), "text", analyzer);
	    query[2] = parser.parse(queries[i]);
	    query[3] = QueryParser.parse(query[2].toString("text"), "text", analyzer);

            System.out.println(i + ": " + queries[i] + " ==> " + query[0].toString("text") + " -> " + query[1].toString("text") + " / " + query[2].toString("text") + " -> " + query[3].toString("text"));
	    if ( argv.length > 0 && argv[0].equals("-q") ) {
		for ( int k=0; k<4; k++ ) {
		    Hits hits = searcher.search(query[k]);
		    System.out.println(k + " " + query[k].toString("text") + "\t" + hits.length() + " documents found");
		    for ( int j=0; j < hits.length(); j++ ) {
			Document doc = hits.doc(j);
			System.out.println("\t"+doc.get("text"));
		    }
		}
	    }
        }
    }
}
