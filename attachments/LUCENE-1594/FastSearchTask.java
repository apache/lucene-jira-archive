package org.apache.lucene.benchmark.byTask.tasks;
import org.apache.lucene.util.*;
import org.apache.lucene.store.*;
import org.apache.lucene.search.*;
import org.apache.lucene.index.*;
import org.apache.lucene.benchmark.byTask.*;
import org.apache.lucene.benchmark.byTask.feeds.*;
import java.io.IOException;
import java.util.Arrays;
public class FastSearchTask extends ReadTask {
  public FastSearchTask(PerfRunData runData) {
    super(runData);
  }
  public boolean withRetrieve() {
    return false;
  }
  public boolean withSearch() {
    return true;
  }
  public boolean withTraverse() {
    return false;
  }
  public boolean withWarm() {
    return false;
  }
  public QueryMaker getQueryMaker() {
    return getRunData().getQueryMaker(this);
  }
  public TopDocs search(final IndexSearcher searcher, final Term t, final Sort sort, final Filter filter) throws IOException {
    int hitCount = 0;
    final int[] queueDocs = new int[11];
    final float[] queueScores = new float[11];
    // prefill w/ sentinel
    Arrays.fill(queueScores, Float.NEGATIVE_INFINITY);
    Arrays.fill(queueDocs, Integer.MAX_VALUE);
    float bottomScore = Float.NEGATIVE_INFINITY;
    final float[] normDecoder = Similarity.getNormDecoder();
    final Similarity sim = searcher.getSimilarity();
    final IndexReader[] subReaders = searcher.getSortedSubReaders();
    final int[] docBases = searcher.getDocBases();
    final Term aT = t;
    final float[] aScoreCache = new float[32];
    final float aWeightValue = new TermQuery(aT).weight(searcher).getValue();
    //System.out.println("weight=" + aWeightValue);
    for(int i=0;i<32;i++) {
      aScoreCache[i] = sim.tf(i) * aWeightValue;
    }
    
    for(int i=0;i<subReaders.length;i++) {
      final IndexReader r = subReaders[i];
      final int docBase = docBases[i];
      final byte[] aNorms = r.norms(aT.field());
      final TermDocs aTD = r.termDocs(aT);
      final IndexInput aFreqStream = ((SegmentTermDocs) aTD).getFreqStream();
      final int aLimit = ((SegmentTermDocs ) aTD).getTermFreq();
      int count = 0;
      int doc = 0;
      while (true) {
        if (++count > aLimit) {
          break;
        }
        final int x = aFreqStream.readVInt();
        doc += x>>>1;
        final int aFreq;
        if ((x & 1) != 0) {
          aFreq = 1;
        } else {
          aFreq = aFreqStream.readVInt();
        }
        final float score = (aFreq < 32 ? aScoreCache[aFreq] : sim.tf(aFreq)*aWeightValue) * normDecoder[aNorms[doc] & 0xFF];
        //System.out.println("doc=" + doc + " score=" + score);
        hitCount++;
        //System.out.println("  competes bottom=" + bottomScore);
        if (score <= bottomScore) {
          continue;
        }
        
        final int fullDoc = doc + docBase;
        queueDocs[1] = fullDoc;
        queueScores[1] = score;
        
        // Downheap
        int i0 = 1;
        int j0 = i0 << 1;
        int k0 = j0+1;
        if (k0 <= 10) {
          final boolean lt = queueScores[k0] < queueScores[j0] || (queueScores[k0] == queueScores[j0] && queueDocs[k0] > queueDocs[j0]);
          if (lt) {
            j0 = k0;
          }
        }
        while(j0 <= 10) {
          if (queueScores[j0] > score || (queueScores[j0] == score && queueDocs[j0] < fullDoc)) {
            break;
          }
          queueDocs[i0] = queueDocs[j0];
          queueScores[i0] = queueScores[j0];
          i0 = j0;
          j0 = i0 << 1;
          k0 = j0+1;
          if (k0 <= 10) {
            final boolean lt = queueScores[k0] < queueScores[j0] || (queueScores[k0] == queueScores[j0] && queueDocs[k0] > queueDocs[j0]);
            if (lt) {
              j0 = k0;
            }
          }
        }
        queueDocs[i0] = fullDoc;
        queueScores[i0] = score;
        bottomScore = queueScores[1];
      }
    }
    
    final float maxScore = queueScores[1];
    // Build results -- sort pqueue entries
    final SorterTemplate sorter = new SorterTemplate() {
      protected int compare(int i, int j) {
        final boolean lt = queueScores[i] < queueScores[j] || (queueScores[i] == queueScores[j] && queueDocs[i] > queueDocs[j]);
        if (lt) {
          return 1;
        } else {
          // pq entries are never equal
          return -1;
        }
      }
      protected void swap(int i, int j) {
        final int itmp = queueDocs[i];
        queueDocs[i] = queueDocs[j];
        queueDocs[j] = itmp;
        final float ftmp = queueScores[i];
        queueScores[i] = queueScores[j];
        queueScores[j] = ftmp;
      }
    };
    // Extract results
    final int numHits = hitCount > 10 ? 10 : hitCount;
    sorter.quickSort(1, numHits);
    final ScoreDoc[] hits = new ScoreDoc[numHits];
    for(int i=0;i<10;i++) {
      hits[i] = new ScoreDoc(queueDocs[1+i], queueScores[1+i]);
    }
    final TopDocs results = new TopDocs(hitCount, hits, maxScore);
    return results;
  }
}