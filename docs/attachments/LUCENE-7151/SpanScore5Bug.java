
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;


public class SpanScore5Bug
{
   public static void main( String[] args ) throws Exception
   {
      //--------------------------------------------------------------
      // first, index some documents
      Directory dir = new RAMDirectory();
      IndexWriter writer = new IndexWriter(dir,new IndexWriterConfig(new StandardAnalyzer()));
      String[] docs = new String[] {
         // these two documents should score the same
        "Lorem ipsum dolor sit amet, state government consectetuer adipiscing elit central government. Fusce posuere, magna sed pulvinar ultricies: central, state, local government offices magna eros quis urna.",
        "Lorem ipsum dolor sit amet, central government consectetuer adipiscing elit state government. Fusce posuere, magna sed pulvinar ultricies: central, state, local government offices magna eros quis urna.",
        // these three will not score but will help increase the score of the above two into a 'visible' range
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
        "Aliquam rhoncus dui ac fringilla sodales. Vivamus accumsan semper consectetur. Etiam cursus ac neque eu molestie. Cras vehicula, augue eget feugiat gravida, nisi enim lacinia urna, in vestibulum ipsum mi at turpis. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Sed maximus lorem a magna aliquet porttitor. Donec pretium, sem quis fringilla suscipit, ex metus mattis mauris, et vulputate odio est tristique erat.",
        "Mauris mi orci, iaculis molestie felis eu, dignissim vehicula ante. Nam et nibh rutrum, condimentum enim quis, euismod neque. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Sed condimentum volutpat vestibulum. Pellentesque et arcu vitae risus facilisis accumsan nec nec velit. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Etiam et consequat urna. Phasellus vel nulla nec purus ultrices vehicula."
      };
      for( int i=0; i < docs.length; ++i )
      {
         Document document = new Document();
         document.add(new StringField("name","doc"+i,Field.Store.YES));
         document.add(new TextField("contents",docs[i],Field.Store.NO));
         writer.addDocument(document);
      }
      writer.close();

      //--------------------------------------------------------------
      // Now, build the query:
      // spanNear([spanNear([contents:central, contents:government], 2, true), spanNear([contents:state, contents:government], 2, true)], 2, false)
      SpanTermQuery term1 = new SpanTermQuery(new Term("contents","central"));
      SpanTermQuery term2 = new SpanTermQuery(new Term("contents","state"));
      SpanTermQuery term3 = new SpanTermQuery(new Term("contents","government"));
      SpanNearQuery inner1 = new SpanNearQuery( new SpanQuery[] {term1,term3}, 2, true );
      SpanNearQuery inner2 = new SpanNearQuery( new SpanQuery[] {term2,term3}, 2, true );
      SpanNearQuery query = new SpanNearQuery( new SpanQuery[] {inner1,inner2}, 2, false );
      System.out.println(query);

      // Search and show the scoring
      IndexReader reader = DirectoryReader.open(dir);
      IndexSearcher searcher = new IndexSearcher(reader);
		searcher.setSimilarity(new LMDirichletSimilarity());
      TopDocs tds = searcher.search(query,10);
      for( int i=0; i < tds.scoreDocs.length; ++i )
      {
         int docId = tds.scoreDocs[i].doc;
         Document doc = reader.document(docId);
         System.out.println("----------------------------");
         System.out.println(doc.getField("name").stringValue());
         Explanation exp = searcher.explain(query,docId);
         System.out.println(exp);
      }
      reader.close();
   }
}
