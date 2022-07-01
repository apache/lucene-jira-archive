package com.carrotsearch.lingo4g.internal.dev.misc;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

public class StoreStore {
  public static void main(String[] args) throws Exception {
    Access access = new Access(); 
    IntSupplier supplier = () -> 0xdeadbeef;

    int [] computed = new int [1] ;
    
    Thread worker = new Thread() {
      @Override
      public void run() {
        int s = 0;
        while (true) {
          s += access.getInt(supplier);
          if (s == 0) {
            computed[0]++;
          }
        }
      }
    };
    worker.start();

    Thread.sleep(3000);
    System.out.println("Invalidating: " + computed[0]);
    access.invalidate();
    System.out.println("Invalidated: " + computed[0]);
    while (worker.isAlive()) {
      worker.join(1000);
      System.out.println("Worked active: " + computed[0]);
    }
    System.out.println("Worked dead: " + computed[0]);
  }

  
  public static class Access {
    private static final AtomicInteger STORE_BARRIER = new AtomicInteger();

    private boolean invalidated = false;

    private void ensureValid() {
      if (invalidated) {
        // this triggers an AlreadyClosedException in ByteBufferIndexInput:
        throw new NullPointerException();
      }
    }

    public int getInt(IntSupplier supplier) {
      ensureValid();
      return supplier.getAsInt();
    }
    
    public void invalidate() {
      invalidated = true;
      // this should trigger a happens-before - so flushes all caches
      STORE_BARRIER.lazySet(0);
      Thread.yield();
    }
  }
}
