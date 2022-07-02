

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;


@SuppressWarnings("deprecation")
public class TestBinaryDocIndex {
   static Directory dir = new RAMDirectory();
    
	private final static IndexWriterConfig getConfig(Version ver) {
		Analyzer testeg = new CJKAnalyzer(ver);
		TieredMergePolicy mergePolicy = new TieredMergePolicy();
		mergePolicy.setFloorSegmentMB(16);
		mergePolicy.setMaxMergedSegmentMB(256);
		IndexWriterConfig conf = new IndexWriterConfig(ver, testeg);
		conf.setMergePolicy(mergePolicy);
		conf.setOpenMode(OpenMode.CREATE_OR_APPEND);
		return conf;
	}

	public static void index() throws IOException, Exception {
		IndexWriter indexWriter1 = new IndexWriter(dir, getConfig(Version.LUCENE_46));
		Document doc1 = new Document();
		TextField field = new TextField("contents", "", Field.Store.YES);
		field.setStringValue("123");
		doc1.add(field);
		doc1.add(new NumericDocValuesField("$bindoc", 20140728234955L));
		indexWriter1.addDocument(doc1);
		System.out.println("numDocs" + indexWriter1.numDocs());
		indexWriter1.close();
		
		/*=============*/
		IndexWriter indexWriter2 = new IndexWriter(dir, getConfig(Version.LUCENE_4_9));
		
		Document doc2 = new Document();
		TextField field2 = new TextField("contents", "", Field.Store.YES);
        field.setStringValue("123");
        doc2.add(field2);
		doc2.add(new NumericDocValuesField("$bindoc", 20140728234956L));
		indexWriter2.addDocument(doc2);
        
		IndexReader reader = DirectoryReader.open(indexWriter2, true);

        Analyzer testeg = new CJKAnalyzer(Version.LUCENE_4_9);
        IndexSearcher indexSearch = new IndexSearcher(reader);
        QueryParser queryParser = new QueryParser(Version.LUCENE_4_9, "contents", testeg);
        // queryParser.setPhraseSlop(1);
        Query query = queryParser.parse("\"123\"");
        System.out.println(query.toString());
        TopDocs hits = indexSearch.search(query, 10);
        System.out.println("找到了" + hits.totalHits + "个");
        for (int i = 0; i < hits.scoreDocs.length; i++) {
            ScoreDoc sdoc = hits.scoreDocs[i];
            Document doc3 = indexSearch.doc(sdoc.doc);
            
            for (AtomicReaderContext r : reader.leaves()) {
                NumericDocValues nd = r.reader().getNumericDocValues("$bindoc");
                if (nd != null) {
                    System.out.println(nd.get(100));
                }
            }
            
            System.out.println(doc3.get("contents"));
        }
		
	}

	
	public static void main(String[] args) throws IOException, Exception {
		index();
	
	}
	
	
}
