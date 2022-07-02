import org.apache.lucene.search.HitCollector;

class HitCollectorTimeoutDecorator extends HitCollector
{
	private static class TimerThread extends Thread
	{
		private final int resolution;
		private volatile long time; // hmm...1.5?

		public TimerThread()
		{
			this( 1 );
		}

		public TimerThread( final int resolution )
		{
			this.resolution = resolution;
			this.setDaemon( true );
		}

		@Override
		public void run()
		{
			for( ;; )
			{
				time++;

				try
				{
					Thread.sleep( resolution );
				}
				catch( final InterruptedException e )
				{
					Thread.currentThread().interrupt();
				}
			}
		}

		public long getTime()
		{
			return time;
		}
	}

	private final static TimerThread TIMER_THREAD = new TimerThread();
	static
	{
		TIMER_THREAD.start();
	}

	private final long t0;
	private final int timeout;
	private final HitCollector hc;

	public HitCollectorTimeoutDecorator( final HitCollector hc, final int timeout )
	{
		this.hc = hc;
		this.timeout = timeout;
		this.t0 = TIMER_THREAD.getTime();
	}

	public static HitCollectorTimeoutDecorator decorate( final HitCollector hc, final int timeout )
	{
		return new HitCollectorTimeoutDecorator( hc, timeout );
	}

	@Override
	public void collect( final int doc, final float score )
	{
		final long t = TIMER_THREAD.getTime() - t0;
		if( t > timeout ) throw new RuntimeException( "query timed out after " + t + "ms" );
		hc.collect( doc, score );
	}
}