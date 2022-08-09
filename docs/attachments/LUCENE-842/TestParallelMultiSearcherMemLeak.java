import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.ParallelMultiSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import junit.framework.TestCase;

public class TestParallelMultiSearcherMemLeak extends TestCase {

  static final long MEM_D = 10000; // max mem delta allowed (in bytes)
  static final int N_INDEXES = 1;
  static final int N_DOCS = 1000;
  static final int N_FIELDS = 10;
  static final int N_QUERIES = 10000;
  static boolean PARALLEL = true;
  private Directory dirs[];
  
  
  /**
   * @param name Test name
   */
  public TestParallelMultiSearcherMemLeak(String name) {
    super(name);
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#setUp()
   */
  protected synchronized void setUp() throws Exception {
    super.setUp();
    if (dirs != null) {
      return; // init only once.
    }
    // populate indexes
    dirs = new Directory[N_INDEXES];
    for (int i = 0; i < dirs.length; i++) {
      dirs[i] = new RAMDirectory();
      IndexWriter iw = new IndexWriter(dirs[i],analyzer(),true);
      for (int j = 0; j < N_DOCS; j++) {
        Document d = new Document();
        for (int k = 0; k < N_FIELDS; k++) {
          d.add(new Field("field"+k,"value"+k+" of this field",Store.YES, Field.Index.TOKENIZED));
        }
        iw.addDocument(d);
      }
      iw.close();
    }
  }


  private Analyzer analyzer() {
    return new StandardAnalyzer();
  }

  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }


  public void testMemLeak() throws Exception {
    long total1 = -1;
    long free1 = -1;
    Runtime rt = Runtime.getRuntime();
    Filter filter = null;
    Sort sort = null;
    int cnt = 0;
    while (cnt<N_QUERIES) {
      for (int k=0; k<N_FIELDS && cnt<N_QUERIES; k++) {
        Searcher searchers[] = new Searcher[N_INDEXES];
        for (int i=0; i<searchers.length; i++) { // reopen searchers every query, quite expensive isn't it
          searchers[i] = new IndexSearcher(dirs[i]);
        }
        doSearch("field"+k+":"+"value"+k,searchers,filter,sort,PARALLEL);
        cnt++;
        if (cnt%100==0) {
          rt.gc();
          long total2 = rt.totalMemory();
          long free2 = rt.freeMemory();
          if (total1<0) {
            total1 = total2; //save stats of first run 
            free1 = free2;
          }
          long dtot = total2-total1;
          long dfree = free1 - free2;
          System.out.println("after "+cnt+" queries, totmem: "+total2+" (d="+dtot+")  free: "+free2+" (d="+dfree+")  free: "+free2);
          assertTrue(cnt+": tot mem "+total2+" grew from initial "+total1+" by more than "+MEM_D+" bytes!",dtot<MEM_D);
          assertTrue(cnt+": free mem "+free2+" shrinked from initial "+free1+" by more than "+MEM_D+" bytes!",dfree<MEM_D);
        }
      }
    }
  }
  
  // create multiSearcher (possibly parallel), search, close multiSearcher.
  private void doSearch(String qtxt, Searcher searchers[], Filter filter, Sort sort, boolean doParallel) throws Exception {
    MultiSearcher multiSearcher = null;
    
    // aggregate the searches across multiple indexes
    if (doParallel) {
      multiSearcher = new ParallelMultiSearcher(searchers);
    } else {
      multiSearcher = new MultiSearcher(searchers);
    }
    
    final QueryParser parser = new QueryParser("content", new StandardAnalyzer());
    final Query query = parser.parse(qtxt);
    final Hits hits = multiSearcher.search(query, filter, sort);
    
    // process hits...
    
    // close all
    //for (int i = 0; i < searchers.length; i++) {
    //  searchers[i].close();
    //}
    multiSearcher.close();
  }

}
