import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.FixedBitSet;
import org.apache.lucene.util.PForDeltaDocIdSet;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.WAH8DocIdSet;
import org.apache.lucene.util.packed.EliasFanoDocIdSet;

public class DocIdSetBenchmark {

  private static class ScoresRegister {

    private final Map<Object, Map<Float, Map<Class<? extends DocIdSet>, Long>>> scores;

    ScoresRegister() {
      scores = new LinkedHashMap<>();
    }

    public void registerScore(Object key, Float loadFactor, DocIdSet set, long score) {
      Map<Float, Map<Class<? extends DocIdSet>, Long>> m1 = scores.get(key);
      if (m1 == null) {
        m1 = new LinkedHashMap<>();
        scores.put(key, m1);
      }

      Map<Class<? extends DocIdSet>, Long> m2 = m1.get(loadFactor);
      if (m2 == null) {
        m2 = new LinkedHashMap<>();
        m1.put(loadFactor, m2);
      }

      Long s = m2.get(set.getClass());
      if (s != null) {
        score = Math.max(s, score);
      }
      m2.put(set.getClass(), score);
    }

    public void printChartsTables() {
      for (Map.Entry<Object, Map<Float, Map<Class<? extends DocIdSet>, Long>>> entry : scores.entrySet()) {
        System.out.println("#### " + entry.getKey());
        Map<Float, Map<Class<? extends DocIdSet>, Long>> m2 = entry.getValue();
        System.out.print("['log10(loadFactor)', '" + FixedBitSet.class.getSimpleName() + "'");
        for (Class<?> cls : m2.values().iterator().next().keySet()) {
          if (cls != FixedBitSet.class) {
            System.out.print(", '" + cls.getSimpleName() + "'");
          }
        }
        System.out.println("],");
        for (Map.Entry<Float, Map<Class<? extends DocIdSet>, Long>> entry2 : m2.entrySet()) {
          final float loadFactor = entry2.getKey();
          final Map<Class<? extends DocIdSet>, Long> scores = entry2.getValue();
          final double fbsScore = scores.get(FixedBitSet.class);
          System.out.print("[" + Math.log10(loadFactor) + ",0");
          for (Map.Entry<Class<? extends DocIdSet>, Long> score : scores.entrySet()) {
            if (score.getKey() != FixedBitSet.class) {
              System.out.print("," + Math.log(score.getValue() / fbsScore) / Math.log(2));
            }
          }
          System.out.println("],");
        }
      }
    }

  }

  private static int DUMMY; // to prevent from JVM optimizations
  private static final long SECOND = 1000L * 1000L * 1000L; // in ns
  private static final Random RANDOM = new Random();
  private static DocIdSetFactory[] FACTORIES = new DocIdSetFactory[] {
    new DocIdSetFactory() {
      @Override
      public DocIdSet copyOf(FixedBitSet set) throws IOException {
        return new WAH8DocIdSet.Builder().add(set.iterator()).build();
      }
    },
    new DocIdSetFactory() {
      @Override
      public DocIdSet copyOf(FixedBitSet set) throws IOException {
        return new PForDeltaDocIdSet.Builder().add(set.iterator()).build();
      }
    },
    new DocIdSetFactory() {
      @Override
      public DocIdSet copyOf(FixedBitSet set) throws IOException {
        final EliasFanoDocIdSet copy = new EliasFanoDocIdSet(set.cardinality(), set.prevSetBit(set.length() - 1));
        copy.encodeFromDisi(set.iterator());
        return copy;
      }
    }
  };
  private static final int MAX_DOC = 1 << 24;
  private static float[] LOAD_FACTORS = new float[] {0.00001f, 0.0001f, 0.001f, 0.01f, 0.1f, 0.5f, 0.9f, 1f};

  private static abstract class DocIdSetFactory {
    public abstract DocIdSet copyOf(FixedBitSet set) throws IOException;
  }

  protected static FixedBitSet randomSet(int numBits, int numBitsSet) {
    assert numBitsSet <= numBits;
    final FixedBitSet set = new FixedBitSet(numBits);
    if (numBitsSet == numBits) {
      set.set(0, numBits);
    } else {
      for (int i = 0; i < numBitsSet; ++i) {
        while (true) {
          final int o = RANDOM.nextInt(numBits);
          if (!set.get(o)) {
            set.set(o);
            break;
          }
        }
      }
    }
    return set;
  }

  protected static FixedBitSet randomSet(int numBits, float percentSet) {
    return randomSet(numBits, (int) (percentSet * numBits));
  }

  private static int exhaustIterator(DocIdSet set) throws IOException {
    int dummy = 0;
    final DocIdSetIterator it = set.iterator();
    for (int doc = it.nextDoc(); doc != DocIdSetIterator.NO_MORE_DOCS; doc = it.nextDoc()) {
      dummy += doc;
    }
    return dummy;
  }

  private static int exhaustIterator(DocIdSet set, int increment) throws IOException {
    int dummy = 0;
    final DocIdSetIterator it = set.iterator();
    for (int doc = -1; doc != DocIdSetIterator.NO_MORE_DOCS; doc = it.advance(doc + increment)) {
      dummy += doc;
    }
    return dummy;
  }

  private static Collection<DocIdSet> sets(int numBits, float load) throws IOException {
    final FixedBitSet fixedSet = randomSet(numBits, load);
    final List<DocIdSet> sets = new ArrayList<DocIdSet>();
    sets.add(fixedSet);
    for (DocIdSetFactory factory : FACTORIES) {
      sets.add(factory.copyOf(fixedSet));
    }
    return sets;
  }

  public static long score(DocIdSet set) throws IOException {
    final long start = System.nanoTime();
    int dummy = 0;
    long score = 0;
    while (System.nanoTime() - start < SECOND) {
      dummy += exhaustIterator(set);
      ++score;
    }
    DUMMY += dummy;
    return score;
  }

  public static long score(DocIdSet set, int inc) throws IOException {
    final long start = System.nanoTime();
    int dummy = 0;
    long score = 0;
    while (System.nanoTime() - start < SECOND) {
      dummy += exhaustIterator(set, inc);
      ++score;
    }
    DUMMY += dummy;
    return score;
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    ScoresRegister reg = new ScoresRegister();
    for (float loadFactor : LOAD_FACTORS) {
      for (DocIdSet set : sets(MAX_DOC, loadFactor)) {
        final long memSize = RamUsageEstimator.sizeOf(set);
        reg.registerScore("memory", loadFactor, set, memSize);
        System.out.println(set.getClass().getSimpleName() + "\t" + loadFactor + "\t" + memSize);
      }
    }
    System.out.println();
    System.out.println("JVM warm-up");
    long start = System.nanoTime();
    while (System.nanoTime() - start < 30 * SECOND) { 
      final int numBits = 1 << 18;
      for (float loadFactor : LOAD_FACTORS) {
        for (DocIdSet set : sets(numBits, loadFactor)) {
          score(set);
          score(set, 1);
          score(set, 701);
        }
      }
    }

    // Start the test
    System.out.println("LoadFactor\tBenchmark\tImplementation\tScore");
    for (int i = 0; i < 5; ++i) {
      for (float load : LOAD_FACTORS) {
        final Collection<DocIdSet> sets = sets(MAX_DOC, load);
        // Free memory so that GC doesn't kick in while the benchmark is running
        System.gc();
        Thread.sleep(5 * 1000);
        for (DocIdSet set : sets) {
          final long score = score(set);
          reg.registerScore("nextDoc", load, set, score);
          System.out.println(load + "\tnextDoc()\t" + set.getClass().getSimpleName() + "\t" + score(set));
        }
        for (int inc : new int[] {1, 31, 313, 3571, 33533, 319993}) { // primes
          final String key = "advance(" + inc + ")";
          for (DocIdSet set : sets) {
            final long score = score(set, inc);
            reg.registerScore(key, load, set, score);
            System.out.println(load + "\t" + key + "\t" + set.getClass().getSimpleName() + "\t" + score);
          }
        }
      }
    }
    System.out.println("DONE " + DUMMY);
    System.out.println("Tables for Google charts:");
    reg.printChartsTables();
  }
}
