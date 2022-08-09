import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.jupiter.api.Test;

class FindSqlHighlightTest {

   @Test
   void reproduceHighlightProblem() throws IOException, ParseException, InvalidTokenOffsetsException {
      String text = "doesn't"; 
      String field = "text";
      //NOK: se~, se~2 and any higher number
      //OK: sel~, s~, se~1
      String uQuery = "se~";
      int maxStartOffset = -1;
      Analyzer analyzer = new SimpleAnalyzer();

      Path indexLocation = Path.of("temp", "reproduceHighlightProblem").toAbsolutePath();
      if (indexLocation.toFile().exists()) {
         FileUtils.deleteDirectory(indexLocation.toFile());
      }
      Directory indexDir = FSDirectory.open(indexLocation);

      //Create index
      IndexWriterConfig dimsIndexWriterConfig = new IndexWriterConfig(analyzer);
      dimsIndexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
      IndexWriter idxWriter = new IndexWriter(indexDir, dimsIndexWriterConfig);
      //add doc
      Document doc = new Document();
      doc.add(new TextField(field, text, Field.Store.NO));
      idxWriter.addDocument(doc);
      //commit
      idxWriter.commit();
      idxWriter.close();

      //search & highlight
      Query query = new QueryParser(field, analyzer).parse(uQuery);
      Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter(), new QueryScorer(query));
      TokenStream tokenStream = TokenSources.getTokenStream(field, null, text, analyzer, maxStartOffset);
      String highlighted = highlighter.getBestFragment(tokenStream, text);
      System.out.println(highlighted);
   }
}