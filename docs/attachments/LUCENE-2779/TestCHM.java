import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class TestCHM {

  public static void main(String[] args) throws Exception {
    final int iter = args.length > 0 ? Integer.parseInt(args[0]) : 1000000;

    final ConcurrentHashMap map = new ConcurrentHashMap(4);
    map.put(1,1);
    map.put(-1,-1);

    Thread thread = new Thread() {
      @Override
      public void run() {
        for (int i=2; i<iter; i++) {
          map.put(i,i);
          map.put(-i,-i);
          int prev = i-1;
          map.remove(prev);
          map.remove(-prev);
        }
      }
    };

    thread.start();

    int minLen = Integer.MAX_VALUE;
    int maxLen = Integer.MIN_VALUE;
    int[] tmp = new int[100];
    int errors = 0;

    for (int i=0; i<iter; i++) {
      Object[] vals = map.keySet().toArray();
      minLen = Math.min(minLen, vals.length);
      maxLen = Math.max(maxLen, vals.length);

      // There are a new pair of numbers always added before the old pair is
      // removed. If we see atomic snapshots in time, then we should always
      // see at least one pair.
      for (int j=0; j<vals.length; j++) {
        tmp[j] = Math.abs((Integer)vals[j]);
      }
      Arrays.sort(tmp, 0, vals.length);
      boolean found = false;
      for (int j=0; j<vals.length-1; j++) {
        if (tmp[j] == tmp[j+1]) {
          found = true;
          break;
        }
      }
      if (!found) {
        errors++;
        System.out.println("ERROR:" + Arrays.asList(vals));
      }
    }

    thread.join();

    System.out.println("iterations=" + iter + " errors=" + errors + " minLen=" + minLen + " maxLen=" + maxLen);
  }


}

