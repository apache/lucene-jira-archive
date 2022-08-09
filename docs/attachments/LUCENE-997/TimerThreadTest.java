import java.util.*;

public class TimerThreadTest
{
  private static class TimerThread extends Thread
  {
    private final long resolution = 10;  // this is about the minimum reasonable time for a Object.wait(long) call.

    // NOTE: we can avoid explicit synchronization here for several reasons:
    // * updates to 32-bit-sized variables are atomic
    // * only single thread modifies this value
    // * use of volatile keyword ensures that it does not reside in
    //   a register, but in main memory (so that changes are visible to
    //   other threads).
    // * visibility of changes does not need to be instantanous, we can
    //   afford losing a tick or two.
    //
    // See section 17 of the Java Language Specification for details.
    private volatile long time = 0;

    /**
     * TimerThread provides a pseudo-clock service to all searching
     * threads, so that they can count elapsed time with less overhead
     * than repeatedly calling System.currentTimeMillis.  A single
     * thread should be created to be used for all searches.
     */
    private TimerThread()
    {
      super("HitCollectorTimeoutDecorator timer thread");
      this.setDaemon( true );
    }

    public void run()
    {
      boolean interrupted = false;
      try
      {
        while( true ) {
          time += resolution;
          try {
            Thread.sleep( resolution );
          }
          catch( final InterruptedException e )
          {
            interrupted = true;
          }
        }
      }
      finally {
        if( interrupted ) {
          Thread.currentThread().interrupt();
        }
      }
    }

    /**
     * Get the timer value in milliseconds.
     */
    public long getMilliseconds()
    {
      return time;
    }

    /**
     * Returns the number of milliseconds elapsed since startTicks.
     * This method handles an overflow condition.
     *
     * @param startTime The number of millliseconds registered by
     * getMilliseconds() at the begining of the interval being timed.
     */
    public long getElapsedMilliseconds( long startTime ) {
      /*
      long curMilliseconds = time;
      if (curMilliseconds < startTime) {
        curMilliseconds += Long.MAX_VALUE;
      }
      return (curMilliseconds - startMilliseconds);
      */
      return ( time - startTime );
    }
  }

  private static class FibonacciWorkThread extends Thread
  {
    private volatile long cur = 1;
    private volatile long last = 0;

    public void calculate()
    {
      long temp = cur + last;
      last = cur;
      cur = temp;
    }

    public void run()
    {
      while( true )
      {
        calculate();
      }
    }    

    public long getCur()
    {
      return cur;
    }
  }

  public static void main( String arg[] )
  {
    int numWorkerThreads = 0;
    if( arg.length > 0 ) {
      numWorkerThreads = Integer.parseInt(arg[0]);
    }
    System.out.println( "Running with " + numWorkerThreads + '\n' );

    System.out.println( "TimerThread\tSys Time\tFibLocal\tFibThread0...\tFibThreadN" );

    TimerThread timer = new TimerThread();
    timer.start();

    List<FibonacciWorkThread> workerThreadList
      = new ArrayList<FibonacciWorkThread> ( numWorkerThreads );
    for( int i = 0; i < numWorkerThreads; ++i ) {
      FibonacciWorkThread worker = new FibonacciWorkThread();
      worker.setDaemon( true );
      worker.start();
      workerThreadList.add(worker);
    }
    
    FibonacciWorkThread localWorker = new FibonacciWorkThread();

    long start = timer.getMilliseconds();
    long elapsed = timer.getElapsedMilliseconds( start );
    long sysStart = System.currentTimeMillis();
    StringBuilder buff = new StringBuilder();
    buff.append( elapsed ).append( '\t' )
      .append( System.currentTimeMillis() - sysStart ).append( '\t' )
      .append( localWorker.getCur() ).append( '\t' );
    for( FibonacciWorkThread fwt : workerThreadList ) {
      buff.append( fwt.getCur() ).append( '\t' );
    }
    System.out.println( buff );

    while( timer.getElapsedMilliseconds( start ) < 10000 ) {
      localWorker.calculate();
      if( timer.getElapsedMilliseconds( start ) > elapsed ) {
        elapsed = timer.getElapsedMilliseconds( start );
        buff.setLength(0);
        buff.append( elapsed ).append( '\t' )
          .append( System.currentTimeMillis() - sysStart ).append( '\t' )
          .append( localWorker.getCur() ).append( '\t' );
        for( FibonacciWorkThread fwt : workerThreadList ) {
          buff.append( fwt.getCur() ).append( '\t' );
        }
        System.out.println( buff );
        //        System.out.println( "" + elapsed + '\t' + ( System.currentTimeMillis() - sysStart ) + '\t'
        //                            + worker.getCur() + '\t' + localWorker.getCur() );
      }
    }
  }
}
