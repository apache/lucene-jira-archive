import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

public class DocValuesTest {
	
	public static void main(String[] args) {
				
		if (args.length==0) {
			System.out.println("Expected index dir as argument");
			return;
		}
		
		File indexDir = new File(args[0]);
		if (!indexDir.exists() || !indexDir.isDirectory()) {
			System.err.println("Index dir "+indexDir+" not found");
			return;
		}
		if (indexDir.list().length>2) {
			System.err.println("Index dir "+indexDir+" is not empty");
			return;
		}
		
		IndexWriter iw = null;
		IndexReader ir = null;
		try {
						
			
			iw = new IndexWriter(FSDirectory.open(indexDir.toPath()), getIWConfig());
			
			int docCount = 2;
			for (int i=0; i<docCount; i++) {								
				
				if (i==docCount/2) {
						//when all docs that have docvalues are indexed,
						//commit to force creation of a new segment
						//containing the docs that have no docvalues					
					iw.commit();
				}
				
				Document doc = new Document();
				
				if (i<docCount/2) {
					doc.add(new IntPoint("numfield", i));
					doc.add(new SortedNumericDocValuesField("numfield", i));
				} else {
					doc.add(new IntPoint("numfield", (-1)*i));
				}				
				
				iw.addDocument(doc);
			}
			
			iw.flush();
			iw.close();
			iw = null;						
			
			ir = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));
			
			printSegments(ir);
			
			IndexSearcher is = new IndexSearcher(ir);
			Query query = new BooleanQuery.Builder()
					.add(new BooleanClause(IntPoint.newRangeQuery("numfield", 0, Integer.MAX_VALUE), Occur.MUST))
					.build();		
			
			SortField sortField = new SortedNumericSortField("numfield", Type.INT);
			sortField.setMissingValue(Integer.MAX_VALUE);
			Sort sort = new Sort(sortField);
			
			TopDocs result = is.search(query, 100, sort);
			System.out.println("Query returned "+result.totalHits);
			
		}  catch (Exception e) {
			e.printStackTrace();
			
		} finally {
			if (iw!=null) {
				try { iw.close(); } catch (IOException e) {}
			}
			if (ir!=null) {
				try { ir.close(); } catch (IOException e) {}
			}
		}
	}


	private static IndexWriterConfig getIWConfig() {
		IndexWriterConfig indexWrtCfg = new IndexWriterConfig();
		indexWrtCfg.setUseCompoundFile(true);
		
       	//Set segment merge policy
		TieredMergePolicy mergePolicy = (TieredMergePolicy)indexWrtCfg.getMergePolicy();
		mergePolicy.setMaxMergedSegmentMB( 500 );
		
		return indexWrtCfg;
	}
	
	private static void printSegments(IndexReader reader) throws IOException {
		
		List<LeafReaderContext> leaves = reader.leaves();		
		
		for (int readerIndex=0;  readerIndex<leaves.size(); readerIndex++) {		
			
			LeafReaderContext ctx = leaves.get(readerIndex);
			LeafReader lr = ctx.reader();
			
			if (lr instanceof SegmentReader) {
				System.out.println("Checking segment: "+((SegmentReader)lr).getSegmentName());
			} else {
				System.out.println("Checking unknown segment: "+lr);
			}
			
			SortedNumericDocValues docvalues = lr.getSortedNumericDocValues("numfield");
			if (docvalues==null) {
				System.out.println("No DocValues for field 'numfield' present in segment!");
			}
		}
		
	}
}
