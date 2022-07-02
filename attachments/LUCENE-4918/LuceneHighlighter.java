import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;

public class LuceneHighlighter {
    public static void main(String[] args) throws IOException, InvalidTokenOffsetsException, ParseException {
        String directoryPath = args[0];
        Directory directory = FSDirectory.open(new File(directoryPath));
        Analyzer analyzer = new WhitespaceAnalyzer(Version.LUCENE_42);

        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_42, analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        FieldType fieldType = new FieldType(TextField.TYPE_STORED);
        fieldType.setStoreTermVectors(true);
        fieldType.setStoreTermVectorOffsets(true);
        fieldType.setStoreTermVectorPositions(true);
        fieldType.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        Field field = new Field("tv", "million documents", fieldType);
        Document document = new Document();
        document.add(field);
        indexWriter.addDocument(document);
        indexWriter.close();

        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(indexReader);

        QueryParser parser = new QueryParser(Version.LUCENE_42, "tv", analyzer);
        Query query = parser.parse("million");

        TopDocs hits = searcher.search(query, 10);

        SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
        QueryScorer scorer = new QueryScorer(query, indexReader, "tv");
        Highlighter highlighter = new Highlighter(htmlFormatter, scorer);
        for (int i = 0; i < hits.scoreDocs.length; i++) {
            int id = hits.scoreDocs[i].doc;
            Document doc = searcher.doc(id); // throws the exception if there is more than one hit
            String text = doc.get("tv");
            TokenStream tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), hits.scoreDocs[i].doc, "tv", analyzer);
            String[] fragments = highlighter.getBestFragments(tokenStream, text, 10);
            for (String frag : fragments) {
                System.out.println(frag);
            }
        }

        System.out.println("number of documents: " + indexReader.numDocs()); // throws the exception
        indexReader.close();
    }
}
