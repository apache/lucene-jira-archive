package org.apache.lucene.index;
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.util.WeakHashMap;
import java.util.Map.Entry;


/**
 * Utility class which efficiently monitors several threads performing time-limited activities
 * Client threads call start(maxTimeMilliseconds) and stop() to denote start and end of time-limited 
 * activity and calls to checkForTimeout() will throw a {@link ActivityTimedOutException} if that thread 
 * has exceeded the maxTimeMilliseconds defined in the start call.
 * 
 * Any class can call the static checkForTimeout method making it easy for objects shared by threads to 
 * simply check if the current thread is over-running in its activity.
 * 
 * The checkForTimeout call is intended to be very lightweight (no synchronisation) so can be called in tight loops.
 *
 */
public class ActivityTimeMonitor
{
	
	//List of active threads that have called start(maxTimeMilliseconds)
	static private WeakHashMap<Thread, Long> timeLimitedThreads=new WeakHashMap<Thread, Long>();
	//The earliest timeout we can anticipate
	static private volatile long firstAnticipatedTimeout=Long.MAX_VALUE;
	static public volatile boolean anActivityHasTimedOut;
	//Set of threads that are known to have overrun
	static private WeakHashMap<Thread, Long> timedOutThreads=new WeakHashMap<Thread, Long>();
	
	//an internal thread that monitors timeout activity
	private static TimeoutThread timeoutThread;
	
	/**
	 * Called by client thread that is starting some time-limited activity.
	 * The stop method MUST ALWAYS be called after calling this method - use of try-finally 
	 * is highly recommended.
	 * 
	 * 
	 * @param maxTimeMilliseconds dictates the maximum length of time that this thread is permitted to execute 
	 * a task.
	 */
	public static final void start(long maxTimeMilliseconds)
	{
		long scheduledTimeout=System.currentTimeMillis()+maxTimeMilliseconds;
		Thread currentThread=Thread.currentThread();
		synchronized (timeLimitedThreads)
		{
			//store the scheduled point in time when the current thread should fail
			timeLimitedThreads.put(currentThread, new Long(scheduledTimeout));

			//if we are not in the middle of handling a timeout
			if(!anActivityHasTimedOut) 
			{
				//check to see if this is now the first thread expected to time out...
				if(scheduledTimeout<firstAnticipatedTimeout)
				{				
					firstAnticipatedTimeout=scheduledTimeout;
	
					//Reset TimeOutThread with new earlier, more agressive deadline to on which to wait
					timeLimitedThreads.notify();
				}
			}
		}
	}
	/**
	 * MUST ALWAYS be called by clients after calling start
	 */
	public static final void stop()
	{
		Thread currentThread=Thread.currentThread();
		synchronized (timeLimitedThreads)
		{			

			Long thisTimeOut = timeLimitedThreads.get(currentThread);
			timeLimitedThreads.remove(currentThread);
			//Choosing not to throw an exception if thread has timed out - we don't punish overruns at last stage
			//but I guess that could be an option if you were feeling malicious
			if(timedOutThreads.remove(currentThread)!=null)
			{
				if(timedOutThreads.size()==0)
				{
					//All errors reported - reset volatile variable
					anActivityHasTimedOut=false;
				}
			}
			
			if(thisTimeOut!=null) //This thread may have timed out and been removed from timeLimitedThreads
			{
				if(thisTimeOut.longValue()<=firstAnticipatedTimeout)
				{
					//this was the first thread expected to timeout - resetFirstAnticipatedFailure 				
					resetFirstAnticipatedFailure();
				}
			}
		}
	}
	
	private static void resetFirstAnticipatedFailure()
	{
		synchronized (timeLimitedThreads)
		{
			// find out which is the next candidate for failure
			
			// TODO Not the fastest conceivable data structure to achieve this. 
			// TreeMap was suggested as alternative for timeLimitedThreads ( http://tinyurl.com/rd8xro ) because 
			// it is sorted but TreeMaps sorted by key not value. Life gets awkward in trying sort by value - see ....
			//     http://tinyurl.com/pjb6oe .. which makes the point that key.equals must be consistent with
			//  compareTo logic so not possible to use a single structure for fast hashcode lookup on a Thread key 
			// AND maintain a sorted list based on timeout value.
			// However not likely to be thousands of concurrent threads at any one time so maybe this is
			// not a problem - we only iterate over all of timeLimitedThreads in 2 scenarios
			// 1) Stop of a thread activity that is the current earliest expected timeout (relatively common)
			// 2) In the event of a timeout (hopefully very rare).
			// Although 1) is a relatively common event it is certainly not in the VERY frequently called checkForTimeout(). 
			
			
			long newFirstNextTimeout=Long.MAX_VALUE;
			for (Entry<Thread, Long> timeout : timeLimitedThreads.entrySet())
			{
				long timeoutTime=timeout.getValue().longValue();
				if(timeoutTime<newFirstNextTimeout)
				{
					newFirstNextTimeout=timeoutTime;
				}
			}
			firstAnticipatedTimeout=newFirstNextTimeout;
			
			//Reset TimeoutThread with lowest timeout from remaining active threads
			timeLimitedThreads.notify();			
		}
	}	
	
	
	/**
	 * Checks to see if this thread has exceeded it's pre-determined timeout. Throws {@link ActivityTimedOutException}
	 * RuntimeException in the event of any timeout.
	 */
	public static final void checkForTimeout()
	{
		if(ActivityTimeMonitor.anActivityHasTimedOut)
		{
			checkTimeoutIsThisThread();
		}
	}
	

	private static final void checkTimeoutIsThisThread()
	{
		Thread currentThread=Thread.currentThread();
		synchronized(timeLimitedThreads)
		{
			
			Long thisTimeOut = timedOutThreads.remove(currentThread);
			if(thisTimeOut!=null )
			{
				if(timedOutThreads.size()==0)
				{
					//All errors reported - reset the volatile variable that will be signalling error state
					anActivityHasTimedOut=false;
				}
				long now=System.currentTimeMillis();
				long overrun=now-thisTimeOut.longValue();
			
				throw new ActivityTimedOutException("Thread "+currentThread+" has timed out, overrun ="
						+overrun+ " ms",overrun);

			}
		}
	}

	

	static
	{
		timeoutThread=new TimeoutThread();
		timeoutThread.setDaemon(true);
		timeoutThread.start();
	}
	
	
	private static final class TimeoutThread extends Thread
	{

		@Override
		public void run()
		{
			while(true)
			{
				try
				{		
					synchronized (timeLimitedThreads)
					{
						long now=System.currentTimeMillis();
						long waitTime=firstAnticipatedTimeout-now;
						if(waitTime<=0)
						{
							//Something may have timed out - check all threads, adding
							// all currently timed out threads to timedOutThreads
							long newFirstAnticipatedTimeout=Long.MAX_VALUE;
							for (Entry<Thread, Long> timeout : timeLimitedThreads.entrySet())
							{
								long timeoutTime=timeout.getValue().longValue();
								if(timeoutTime<=now)
								{
									//thread has timed out
									Thread badThread=timeout.getKey();
									//remove from list of active threads
									timeLimitedThreads.remove(badThread);
									//add to list of bad threads
									timedOutThreads.put(badThread, timeoutTime);
									anActivityHasTimedOut=true;
								}
								else
								{
									//assess time of next potential failure
									newFirstAnticipatedTimeout=Math.min(newFirstAnticipatedTimeout,timeoutTime);
								}
							}	
							firstAnticipatedTimeout=newFirstAnticipatedTimeout;
							timeLimitedThreads.wait();
						}
						else
						{
							//Sleep until the next anticipated time of failure (or, hopefully more likely) until 
							//notify-ed of a new next anticipated time of failure 
							timeLimitedThreads.wait(waitTime);
						}
					}
					
				} catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
	}

}
