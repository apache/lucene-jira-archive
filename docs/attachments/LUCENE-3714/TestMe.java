import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.FST.Arc;
import org.apache.lucene.util.fst.PositiveIntOutputs;
import org.apache.lucene.util.fst.Util;

import com.google.common.collect.Lists;

public class TestMe {
  public static void main(String[] args) throws Exception {
    final PositiveIntOutputs outputs = PositiveIntOutputs.getSingleton(true);
    final Builder<Long> builder = new Builder<Long>(FST.INPUT_TYPE.BYTE1,
        outputs);
    final IntsRef scratch = new IntsRef();
    int v = 100;
    
    System.out.println("Generating badass fst (key|score).");
    for (int i = 'a', j = 1; i < 'g'; i++, j++) {
      for (int k = i; k < i + 3; k++) {
        addEntry(builder, scratch, "" + (char) i + "a" + (char) k,
            k == i ? j + 2 : outputs.get(v) - j);
      }
      addEntry(builder, scratch, "" + (char) i + "b", outputs.get(j));
    }

    final FST<Long> fst = builder.finish();
    Writer w = new OutputStreamWriter(new FileOutputStream("out.dot"));
    Util.toDot(fst, w, false, false);
    w.close();

    /*
     * A snapshot of fst traversal - the arc to follow and the cost if we take it.  
     */
    class TraversalState {
      String path;
      long accumulatedOutput;
      Arc<Long> arc;

      public TraversalState(String pathSoFar, long outputSoFar, Arc<Long> arc) {
        this.arc = arc;
        this.accumulatedOutput = outputSoFar + arc.output;
        this.path = pathSoFar + (char) arc.label;
      }
    }

    // Pick N entries with the smallest score.
    int n = 3;

    // Priority queue of the best "direction" to follow. The top will be an arc with
    // so-far minimum accumulated output value. This should be a bounded queue so that
    // we drop elements beyond 'n'... but I was a bit confused about lucene's priorityqueue
    // implementation -- doesn't seem to do what I think it should do.
    PriorityQueue<TraversalState> pq = new PriorityQueue<TraversalState>(1, new Comparator<TraversalState>() {
      public int compare(TraversalState o1, TraversalState o2) {
        if (o1.accumulatedOutput < o2.accumulatedOutput) {
          return -1;
        } else if (o1.accumulatedOutput > o2.accumulatedOutput) {
          return 1;
        } else {
          if (o1.arc.label < o2.arc.label) {
            return -1;
          } else if (o1.arc.label > o2.arc.label) {
            return 1;
          } else {
            return 0;
          }
        }
      };
    });

    /*
     * Initially, fill in the PQ with the start state. This will be the state which ends
     * the suggestion prefix, but I take the root node for simplicity. 
     */
    Arc<Long> root = fst.getFirstArc(new Arc<Long>());
    Arc<Long> arc = fst.readFirstTargetArc(root, new Arc<Long>());
    while (true) {
      pq.add(new TraversalState("", 0, new Arc<Long>().copyFrom(arc)));
      if (arc.isLast())
        break;
      fst.readNextArc(arc);
    }

    // The algorithm is simple: follow the path with minimum accumulated cost, then
    // reinsert its children into the queue (or output if it's a final arc).
    System.out.println("Best to worst suggestions from the root, score ASC, alpha DESC, in order.");
    List<TraversalState> result = Lists.newArrayList();
    while (pq.size() > 0) {
      TraversalState min = pq.remove();

      Arc<Long> followMe = min.arc;
      if (followMe.isFinal()) {
        result.add(min);
        System.out.println(
            String.format("key: %5s, score: %3d, pq size: %3d",
                min.path,
                min.accumulatedOutput,
                pq.size()));
      }

      if (fst.targetHasArcs(followMe)) {
        Arc<Long> tmp = fst.readFirstTargetArc(followMe, new Arc<Long>());
        while (true) {
          pq.add(new TraversalState(min.path, min.accumulatedOutput, new Arc<Long>().copyFrom(tmp)));
          if (tmp.isLast()) 
            break;
          fst.readNextArc(tmp);
        }
      }
    }

    // result has the result.
  }

  private static void addEntry(Builder<Long> builder, IntsRef scratch,
      String key, long value) throws IOException {
    builder.add(Util.toIntsRef(new BytesRef(key), scratch), value);
    System.out.println(key + "|" + value);
  }
}
