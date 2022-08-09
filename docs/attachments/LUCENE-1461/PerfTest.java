import org.apache.lucene.store.*;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.*;
import java.io.File;
import java.util.Random;

public class PerfTest {
  public static void main(String[] args) throws Exception {

    File indexDir = new File(args[0]);
    Directory dir = new SimpleFSDirectory(indexDir, new NativeFSLockFactory(indexDir));
    
    int precisionStep=8;
    Random r = new Random();
		
    if (!indexDir.exists() || indexDir.list().length == 0) {
      int numDocs = 5000000;
      IndexWriter writer = new IndexWriter(dir, null, MaxFieldLength.UNLIMITED);
      writer.setMergeFactor(50);
      writer.setMaxBufferedDocs(numDocs/100);
      System.out.println("populating index");
      
      // slow
      /*
      long time = System.currentTimeMillis();
      for (int i = 0; i < numDocs; i++) {
        Document doc = new Document();
        doc.add(new Field("c", "test", Store.NO, Index.NOT_ANALYZED));
        Field triefield = new Field("i", stream);
        triefield.setOmitTermFreqAndPositions(true);
        triefield.setOmitNorms(true);
        doc.add(triefield);
        writer.addDocument(doc);
      }
      */
      // fast
      Document doc = new Document();
      doc.add(new Field("c", "test", Store.NO, Index.NOT_ANALYZED));
      NumericField f=new NumericField("i", precisionStep);
      doc.add(f);
      long time = System.currentTimeMillis();
      for (int i = 0; i < numDocs; i++) {
        f.setIntValue(r.nextInt());
        writer.addDocument(doc);
      }
      
      System.out.println("time=" + (System.currentTimeMillis() - time) + " ms");
      System.out.println("optimize & close");
      writer.optimize();
      writer.close();
    }

    System.out.println("searching");
    IndexSearcher searcher = new IndexSearcher(dir);
    System.out.println("loading field cache");
    final long tf = System.nanoTime();
    FieldCache.DEFAULT.getInts(searcher.getIndexReader(),"i");
    System.out.println("time: "+ ((System.nanoTime()-tf)/1000000.0) + " ms");
    System.out.println("Warming searcher...");
    searcher.search(new TermQuery(new Term("c", "test")), null, 10/*, sort*/); // warming
    searcher.search(new ConstantScoreQuery(FieldCacheRangeFilter.newIntRange("i",Integer.MIN_VALUE,Integer.MAX_VALUE,true,true)), null, 10/*, sort*/);
    
    long time = 0;
    int numQueries = 200;
    long bestTime1 = Long.MAX_VALUE, worstTime1 = 0L, time1=0L;
    long bestTime2 = Long.MAX_VALUE, worstTime2 = 0L, time2=0L;
    int sum1 = 0, sum2 = 0;
    long terms=0L;
    for (int i = 0; i < numQueries; i++) {
      int i1=r.nextInt(), i2=r.nextInt();
      if (i1>i2) {
        int j=i1;
        i1=i2; i2=j;
      }
      
      long t;
      MultiTermQuery q1=NumericRangeQuery.newIntRange("i",precisionStep,i1,i2,true,true);
      q1.setConstantScoreRewrite(true);
      t = System.nanoTime();
      sum1 += searcher.search(q1, null, 10).totalHits;
      t = System.nanoTime()-t;
      terms += q1.getTotalNumberOfTerms();
      if (t < bestTime1)  bestTime1 = t;
      if (t > worstTime1)  worstTime1 = t;
      time1+=t;
      
      // for comparison:
      //q=new RangeQuery("i",TrieUtils.intToPrefixCoded(i1),TrieUtils.intToPrefixCoded(i2),true,true);
      //q.setConstantScoreRewrite(true);
      Query q2 = new ConstantScoreQuery(FieldCacheRangeFilter.newIntRange("i",i1,i2,true,true));
      t = System.nanoTime();
      sum2 += searcher.search(q2, null, 10).totalHits;
      t = System.nanoTime()-t;
      if (t < bestTime2)  bestTime2 = t;
      if (t > worstTime2)  worstTime2 = t;
      time2+=t;
    }
    System.out.println("avg number of terms: "+(((double)terms)/numQueries));
    System.out.println("TRIE:       best time=" + (bestTime1/1000000.0) + " ms; worst time=" + (worstTime1/1000000.0) + " ms; avg=" + (((double)time1)/numQueries/1000000.0) + " ms; sum=" + sum1);
    System.out.println("FIELDCACHE: best time=" + (bestTime2/1000000.0) + " ms; worst time=" + (worstTime2/1000000.0) + " ms; avg=" + (((double)time2)/numQueries/1000000.0) + " ms; sum=" + sum2);
    searcher.close();
  }
}