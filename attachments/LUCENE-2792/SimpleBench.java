package org.apache.lucene;

import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.automaton.State;

public class SimpleBench {
  public static void main(String args[]) {
    // warmup
    for (int i = 0; i < 3; i++) {
      method1(false);
      method2(false);
      method3(false);
    }
    
    for (int i = 0; i < 3; i++) {
      method1(true);
      method2(true);
      method3(true);
    }
  }
  
  static void method1(boolean print) {
    long ms = System.currentTimeMillis();
    for (int i = 0; i < 10000000; i++) {
      State st[] = new State[0];
      final State[] st2 = new State[ArrayUtil.oversize(st.length+1, RamUsageEstimator.NUM_BYTES_OBJECT_REF)];
      System.arraycopy(st, 0, st2, 0, st.length);
      st = st2;
    }
    if (print) System.out.println("method1: " + (System.currentTimeMillis() - ms));
  }
  
  static void method2(boolean print) {
    long ms = System.currentTimeMillis();
    for (int i = 0; i < 10000000; i++) {
      State st[] = new State[0];
      final State st2[] = ArrayUtil.grow(st, (Class<State>) st.getClass().getComponentType());
      st = st2;
    }
    if (print) System.out.println("method2: " + (System.currentTimeMillis() - ms));
  }
  
  static void method3(boolean print) {
    long ms = System.currentTimeMillis();
    for (int i = 0; i < 10000000; i++) {
      State st[] = new State[0];
      final State st2[] = ArrayUtil.grow(st, State.class);
      st = st2;
    }
    if (print) System.out.println("method3: " + (System.currentTimeMillis() - ms));
  }
}
