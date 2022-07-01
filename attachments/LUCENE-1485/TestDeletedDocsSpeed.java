
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.apache.lucene.util.BitVector;
import org.apache.lucene.util.OpenBitSet;

public class TestDeletedDocsSpeed {
  private int length = -1;
  private static final int batch = 256;
  private int max = -1;
  
  public TestDeletedDocsSpeed(int length, int max) {
    this.length = length;
    this.max = max;
  }
  
  public static void main(String[] args) throws Exception {
    TestDeletedDocsSpeed tdds = new TestDeletedDocsSpeed(1024*2, 10*1024*1024);
    for (int x=0; x < 25; x++) {
      tdds.testDeletedDocsGet();
    }
  }
  
  public void testDeletedDocsGet() throws IOException {
    double booster  = ((max*1.0)/(1000f*length));
    System.out.println("");
    //System.out.println("Running OBSDocIdSet Iterate Sanity test");
    //System.out.println("----------------------------");
    OpenBitSet obs = new OpenBitSet(max);
    BitVector bv = new BitVector(max);
    Random random = new Random();

    // Minimum 5 bits
    int randomizer = 0;
    int count = 0;
    for (int i = 1; i < (length); i++)
    {
      ArrayList<Integer> list = new ArrayList<Integer>();

      for (int k = 0; k < batch; k++) {
        list.add(randomizer + (int) (random.nextDouble() * 1000));
      }
      Collections.sort(list);
      randomizer += 1000*booster;

      for (int k = 0; k < batch; k++) {
        obs.set(list.get(k));
        bv.set(list.get(k));
        count++;
      }
    }
    System.out.println("bit set size: "+max);
    System.out.println("set bits count: "+count);
    timeOpenBitSetGet(obs);
    timeBitVectorGet(bv);
  }
  
  private void timeOpenBitSetGet(OpenBitSet openBitSet) {
    long now = System.currentTimeMillis();
    for (int x=0; x < max; x++) {
      openBitSet.fastGet(x);
    }
    long totalTime = System.currentTimeMillis() - now;
    System.out.println("openbitset: "+totalTime);
  }
  
  private void timeBitVectorGet(BitVector bitVector) {
    long now = System.currentTimeMillis();
    for (int x=0; x < max; x++) {
      bitVector.get(x);
    }
    long totalTime = System.currentTimeMillis() - now;
    System.out.println("bitvector: "+totalTime);
  }
}
