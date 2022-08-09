package org.apache.lucene.index;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.document.Document;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

/**
 * <p>
 * This index reader delegates most tasks to the normal {@link IndexReader}
 * implementation. It adds support for refreshing a stale index
 * (one that has been modified since it was opened) without
 * requiring the consumer of the reader to be aware of all the details.
 * It also supplies a mechanism to close the underlying reader
 * when it has gone idle and re-open it when it is used again.
 * </p><p>
 * There are four configurable properties:
 * <dl>
 * <dt>{@link #minimumWaitToClose} (static member)</dt>
 * <dd>The minimum allowed value of the waitToClose property.</dd>
 * <dt>{@link #waitToClose}</dt>
 * <dd>The time in milliseconds to wait before closing an index that has timed out or been reopened for a refresh.
 * This should allow enough time for any current consumers to wrap up their use of the reader delegate. This
 * IdleTimeoutRefreshingIndexReader is still available for reads and will have a new delegate open to service
 * requests.</dd>
 * <dt>{@link #timeout}</dt>
 * <dd>The time in milliseconds to wait before closing an index due to inactivity.</dd>
 * <dt>{@link #refreshInterval}</dt>
 * <dd>The time in milliseconds to wait before examining an index to determine if it needs to be refreshed.</dd>
 * </dl>
 * </p><p>
 * The minimumWaitToClose property can be configured using {@link #setMinimumWaitToClose(long)}.
 * Each of the other properties can be configured on a per instance basis using the property setters
 * {@link #setWaitToClose(long)}, {@link #setTimeout(long)} and {@link #setRefreshInterval(long)}.
 * </p><p>
 * Defaults can be set on the command line by specifying long values as system properties using the keys
 * {@link #PROP_WAIT_TO_CLOSE}, {@link #PROP_TIMEOUT} and {@link #PROP_REFRESH_INTERVAL}.
 * If those defaults are not set or don't parse as longs according to {@link Long#getLong(String)},
 * the default is to never timeout and never refresh (behavior analogous to a normal IndexReader with a 
 * performance penalty due to the delegation model).
 * </p><p>
 * The defaults can also be manipulated at runtime using the static setters
 * {@link #setDefaultWaitToClose(long)}, {@link #setDefaultTimeout(long)} and {@link #setDefaultRefreshInterval(long)}.
 * Runtime changes to the defaults will not affect already instantiated instances, use the 
 * instance level setters instead.
 * </p><p>
 * The waitToClose property has a minimum setting determined by <code>MINIMUM_WAIT_TO_CLOSE</code>.
 * It is important that the closing reader have enough time to complete the servicing
 * of any existing requests before being closed. This strategy was chosen in lieu of completely
 * synchronizing all requests to the index reader. If the reader is closed to soon the consumers
 * will get an IOException since they will be attempting to read from closed files.
 * </p><p>
 * <font style="color:red;font-weight:bold">There were three methods of IndexReader that did not throw IOException.
 * These were wrapped to throw RuntimeException. See {@link #isDeleted(int)}, {@link #maxDoc()} and {@link #numDocs()}.
 * I'm not sure this was a good strategy, but I wasn't sure what else to choose.</font>
 * </p><p>
 * <font style="color:red;font-weight:bold">I'm not sure at the time of this writing
 * whether this implementation works with a RAMDirectory implementation.</font>
 * </p> 
 * 
 * @author <A HREF="mailto:eisakson@nc.rr.com">Eric Isakson</A>
 * @version $Id$
 */
public class IdleTimeoutRefreshingIndexReader extends IndexReader {

//	-----------------------------------------------
//	 Property names
//	-----------------------------------------------

	/** The name of the <code>minimumWaitToClose</code> property. */
	public static final String PROP_MINIMUM_WAIT_TO_CLOSE = "org.apache.lucene.index.IdleTimeoutRefreshingIndexReader.minimumWaitToClose";
	
	/** The name of the <code>waitToClose</code> property. */
	public static final String PROP_WAIT_TO_CLOSE = "org.apache.lucene.index.IdleTimeoutRefreshingIndexReader.waitToClose";
	
	/** The name of the <code>timeout</code> property. */
	public static final String PROP_TIMEOUT = "org.apache.lucene.index.IdleTimeoutRefreshingIndexReader.timeout";
	
	/** The name of the <code>refreshInterval</code> property. */
	public static final String PROP_REFRESH_INTERVAL = "org.apache.lucene.index.IdleTimeoutRefreshingIndexReader.refreshInterval";
	
//	-----------------------------------------------
//	 Defaults
//	-----------------------------------------------

	/**
	 * The default minimum value of the <code>waitToClose</code> property
	 * used if no default was specified and the system property is not set.
	 */
	public static final long DEFAULT_MINIMUM_WAIT_TO_CLOSE = 10000; // 10 seconds
	
	/** 
	 * The default value of the <code>waitToClose</code> property.
	 * used if no default was specified and the system property is not set.
	 */
	public static final long DEFAULT_WAIT_TO_CLOSE = DEFAULT_MINIMUM_WAIT_TO_CLOSE;

	/** 
	 * The default value of the <code>timeout</code> property.
	 * used if no default was specified and the system property is not set.
	 */
	public static final long DEFAULT_TIMEOUT = 0;
	
	/** 
	 * The default value of the <code>refreshInterval</code> property.
	 * used if no default was specified and the system property is not set.
	 */
	public static final long DEFAULT_REFRESH_INTERVAL = 0;
	
//	-----------------------------------------------
//	 Class variables
//	-----------------------------------------------

	// Using Long objects so we can check if these have been set.
	// Only need to check the system properties if they are not set.
	
	/** The minimum value of the <code>waitToClose</code> property. */
	private static Long minimumWaitToClose;
	
	/** The default value of the <code>waitToClose</code> property. */
	private static Long defaultWaitToClose;
	
	/** The default value of the <code>timeout</code> property. */
	private static Long defaultTimeout;
	
	/** The default value of the <code>refreshInterval</code> property. */
	private static Long defaultRefreshInterval;
	
//	-----------------------------------------------
//	 Instance variables
//	-----------------------------------------------

	/** The index reader to delegate to. */
	private IndexReader reader;
	
	/** The time the index was last accessed. */
	private long lastAccessed;
	
	/**
	 * The time the index was last modified when it was opened.
	 * Used to determine if the index needs to be refreshed.
	 */
	private long modifiedTimeOnOpen;
	
	/**
	 * The time to wait before closing the reader delegate
	 * that has timed out or been reopened for a refresh.
	 */
	private long waitToClose;
	
	/**
	 * The time the reader may be idle before being closed.
	 */
	private long timeout;
	
	/**
	 * The period on which the index should be examined for
	 * changes and refreshed.
	 */
	private long refreshInterval;
	
	/**
	 * A monitoring thread that will close the index reader
	 * delegate after it has been idle too long.
	 */
	private TimeoutMonitor timeoutMonitor;

	/**
	 * A monitoring thread that will refresh the index reader
	 * delegate when it has been modified.
	 */
	private RefreshMonitor refreshMonitor;
	
//	-----------------------------------------------
//	 Constructor and static construction methods
//	-----------------------------------------------

	/**
	 * Constructs an IdleTimeoutRefreshingIndexReader.
	 * 
	 * @param directory The directory to pass to the {@link IndexReader} implementation.
	 * @param timeout The idle timeout in milliseconds.
	 * @param refreshInterval The refresh interval in milliseconds.
	 * @param waitToClose The time in milliseconds to wait before closing a timed out or refreshed index.
	 */
	protected IdleTimeoutRefreshingIndexReader(
		Directory directory,
		long timeout,
		long refreshInterval,
		long waitToClose) {
		super(directory);
		setTimeout(timeout);
		setRefreshInterval(refreshInterval);
		setWaitToClose(waitToClose);
	}
	
	/**
	 * Constructs and returns an IdleTimeoutRefreshingIndexReader.
	 * 
	 * @param directory The directory to pass to the {@link IndexReader} implementation.
	 * @param timeout The idle timeout in milliseconds.
	 * @param refreshInterval The refresh interval in milliseconds.
	 * @param waitToClose The time in milliseconds to wait before closing a timed out or refreshed index.
	 * @return The configured idle timeout, refreshing reader.
	 */
	public static IdleTimeoutRefreshingIndexReader open(Directory directory, long timeout, long refreshInterval, long waitToClose) {
		return new IdleTimeoutRefreshingIndexReader(directory,timeout,refreshInterval,waitToClose);
	}
	
	/**
	 * Overrides {@link IndexReader#open(Directory)} to create an
	 * IdleTimeoutRefreshingIndexReader. Uses the default timings.
	 * 
	 * @param directory The directory to pass to the {@link IndexReader} implementation.
	 * @return The configured idle timeout, refreshing reader.
	 */
	public static IndexReader open(Directory directory) {
		return open(directory,getDefaultTimeout(),getDefaultRefreshInterval(),getDefaultWaitToClose());
	}
	
	/**
	 * Constructs and returns an IdleTimeoutRefreshingIndexReader.
	 * 
	 * @param file The {@link File} that points to the index to read.
	 * @param timeout The idle timeout in milliseconds.
	 * @param refreshInterval The refresh interval in milliseconds.
	 * @param waitToClose The time in milliseconds to wait before closing a timed out or refreshed index.
	 * @return The configured idle timeout, refreshing reader.
	 */
	public static IdleTimeoutRefreshingIndexReader open(File file, long timeout, long refreshInterval, long waitToClose) throws IOException {
		return new IdleTimeoutRefreshingIndexReader(FSDirectory.getDirectory(file,false),timeout,refreshInterval,waitToClose);
	}
	
	/**
	 * Overrides {@link IndexReader#open(File)} to create an
	 * IdleTimeoutRefreshingIndexReader. Uses the default timings.
	 * 
	 * @param file The {@link File} that points to the index to read.
	 * @return The configured idle timeout, refreshing reader.
	 */
	public static IndexReader open(File file) throws IOException {
		return open(file,getDefaultTimeout(),getDefaultRefreshInterval(),getDefaultWaitToClose());
	}
	
	/**
	 * Constructs and returns an IdleTimeoutRefreshingIndexReader.
	 * 
	 * @param filepath The path that points to the index to read.
	 * @param timeout The idle timeout in milliseconds.
	 * @param refreshInterval The refresh interval in milliseconds.
	 * @param waitToClose The time in milliseconds to wait before closing a timed out or refreshed index.
	 * @return The configured idle timeout, refreshing reader.
	 */
	public static IdleTimeoutRefreshingIndexReader open(String filepath, long timeout, long refreshInterval, long waitToClose) throws IOException {
		return new IdleTimeoutRefreshingIndexReader(FSDirectory.getDirectory(filepath,false),timeout,refreshInterval,waitToClose);
	}
	
	/**
	 * Overrides {@link IndexReader#open(String)} to create an
	 * IdleTimeoutRefreshingIndexReader. Uses the default timings.
	 * 
	 * @param filepath The path that points to the index to read.
	 * @return The configured idle timeout, refreshing reader.
	 */
	public static IndexReader open(String filepath) throws IOException {
		return open(filepath,getDefaultTimeout(),getDefaultRefreshInterval(),getDefaultWaitToClose());
	}

//	-----------------------------------------------
//	 Synchronization and monitor/reader lifecycle
//	-----------------------------------------------

	/**
	 * Implements a thread that accepts an IndexReader and
	 * waits a certain interval before closing it. 
	 */
	private class ReaderCloser extends Thread {
		private IndexReader readerToClose;
		public ReaderCloser(IndexReader readerToClose) {
			setName("IdleTimeoutRefreshingIndexReader.ReaderCloser");
			this.readerToClose = readerToClose;
		}
		public void run() {
			try {
				Thread.sleep(waitToClose);
			} catch (InterruptedException ie) {
				// no-op
			}
			try {
				readerToClose.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
	
	/** <p>
	 * This thread checks a condition on an interval and
	 * whenever the condition is true at the end of the 
	 * interval executes its action.
	 * </p><p>
	 * The thread terminates when die is called or its member
	 * variable die is true and its interval has elapsed.
	 * </p><p>
	 * The thread synchronizes its condition/action block on
	 * directory to ensure the monitors and main thread
	 * do not cause an inconsistent state.
	 * </p><p>
	 * This thread is a daemon thread which allows the JVM to exit
	 * while this thread is running.
	 * </p>
	 */
	private abstract class MonitorThread extends Thread {
		/** The interval between checks on the condition. */
		private long interval;
		/** Flag to indicate the thread should terminate. */
		private boolean die;
		public MonitorThread(long interval) {
			this.interval = interval;
			setDaemon(true);
		}
		public void run() {
			while (!die) {
				try {
					Thread.sleep(interval);
				}
				catch (InterruptedException ie) {
					if (die) break;
				}
				if (!interrupted()) {
					synchronized (directory) { // in- & inter-process sync
						if (condition() && !die) {
							action();
						}
					}
					if (die) break;
				}
			}
		}
		
		/** <p>
		 * The condition under which this action should run.
		 * </p><p>
		 * If die is called, the action will be skipped and
		 * the thread will terminate.
		 * </p>
		 * @return <code>true</code> if the action should be executed.
		 */
		public abstract boolean condition();
		
		/**
		 * The action that should be performed.
		 */
		public abstract void action();
		public long getInterval() {
			return interval;
		}
		public void setInterval(long l) {
			interval = l;
			if (isAlive()) {
				interrupt();
			} 
		}
		public void die() {
			die = true;
			if (isAlive()) {
				interrupt();
			}
		}
	}
	
	/**
	 * This monitor thread will close an index that has
	 * been idle longer than the timeout. It assumes there
	 * is only one of these in the instance of IdleTimeoutRefreshingIndexReader
	 * and that it is assigned to timeoutMonitor.
	 */
	private class TimeoutMonitor extends MonitorThread {
		public TimeoutMonitor() {
			super(timeout);
			setName("IdleTimeoutRefreshingIndexReader.TimeoutMonitor");
		}
		public boolean condition() {
			return isIdle();
		}
		public void action() {
			doClose();
		}
    }

	/**
	 * This monitor thread will close an index that has
	 * been modified since it was opened and open a new
	 * reader to delegate work to.  It assumes there
	 * is only one of these in the instance of IdleTimeoutRefreshingIndexReader
	 * and that it is assigned to refreshMonitor.
	 */
	private class RefreshMonitor extends MonitorThread {
		public RefreshMonitor() {
			super(refreshInterval);
			setName("IdleTimeoutRefreshingIndexReader.RefreshMonitor");
		}
		public boolean condition() {
			return isStale();
		}
		public void action() {
			refresh();
		}
	}

    /**
     * Checks if the reader has been idle longer than the idle
     * timeout.
     * @return <code>true</code> if the reader is idle.
     */	
	public boolean isIdle() {
		return System.currentTimeMillis() - lastAccessed > timeout;
	}

	/**
	 * Checks if the index has changed since it was opened.
	 * 
	 * @return <code>true</code> if the index has changed.
	 * <code>false</code> if the index has not changed or an IOException
	 * prevented verification of the index's last modified time.
	 */	
	public boolean isStale() {
		try {
			return modifiedTimeOnOpen != IndexReader.lastModified(directory);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return false; // assume it isn't stale, a refresh will probably just get an IOException
	}
	
	/**
	 * Refresh the reader delegate.
	 */
	private void refresh() {
		try {
			// close the old reader
			closeReader();
			// open the new reader
			openReader();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	/**
	 * Starts the refresh monitor.
	 */
	private void startRefreshMonitor() {
		if (refreshInterval > 0) {
			refreshMonitor = new RefreshMonitor();
			refreshMonitor.start();
		}
	}
	
	/**
	 * Stops the refresh monitor.
	 */
	private void stopRefreshMonitor() {
		if (refreshMonitor != null) {
			refreshMonitor.die();
			refreshMonitor = null;
		} 
	}
	
	/**
	 * Starts the idle timeout monitor.
	 */
	private void startTimeoutMonitor() {
		if (timeout > 0) {
			timeoutMonitor = new TimeoutMonitor();
			timeoutMonitor.start();
		}
	}
	
	/**
	 * Stops the idle timeout monitor.
	 */
	private void stopTimeoutMonitor() {
		if (timeoutMonitor != null) {
			timeoutMonitor.die();
			timeoutMonitor = null;
		} 
	}
	
	/**
	 * Opens the reader delegate and notes its last modification time
	 * for use with the refresh monitor.
	 * 
	 * @throws IOException
	 */
	private void openReader() throws IOException {
		reader = IndexReader.open(directory);
		modifiedTimeOnOpen = IndexReader.lastModified(directory);
	}
	
	/**
	 * Closes the reader delegate with a ReaderCloser
	 * to give consumers time to finish up before the
	 * underlying files are closed.
	 */
	private void closeReader() {
		if (reader != null) {
			new ReaderCloser(reader).start();
			reader = null;
		}
	}
	
	/**
	 * Gets the reader delegate and notes its last access time.
	 * If the reader delegate is not open, open it and start
	 * the monitors.
	 *  
	 * @return The {@link IndexReader} to delegate work to.
	 * @throws IOException If the reader could not be opened.
	 */
	private IndexReader getReader() throws IOException {
		synchronized (directory) { // in- & inter-process sync
			lastAccessed = System.currentTimeMillis();
			if (reader == null) {
				openReader();
				startRefreshMonitor();
				startTimeoutMonitor();
			}
			return reader;
		}
	}

	/**
	 * Closes up any monitoring threads and the reader delegate if they exist.
	 * Doesn't throw IOException because it is caught and handled in the
	 * ReaderCloser thread if it occurrs.
	 * 
	 * @see IndexReader#doClose()
	 */
	protected void doClose() {
		synchronized (directory) { // in- & inter-process sync
			// cleanup the monitor threads
			stopRefreshMonitor();
			stopTimeoutMonitor();
			// close the reader delegate
			closeReader();
		}
	}

//	-----------------------------------------------
//	 Delegate to work to the underlying reader.
//	-----------------------------------------------	

	/**
	 * @see IndexReader#docFreq(Term)
	 */
	public int docFreq(Term arg0) throws IOException {
		return getReader().docFreq(arg0);
	}

	/**
	 * @see IndexReader#document(int)
	 */
	public Document document(int arg0) throws IOException {
		return getReader().document(arg0);
	}

	/**
	 * @see IndexReader#getFieldNames()
	 */
	public Collection getFieldNames() throws IOException {
		return getReader().getFieldNames();
	}

	/**
	 * @see IndexReader#getFieldNames(boolean)
	 */
	public Collection getFieldNames(boolean arg0) throws IOException {
		return getReader().getFieldNames(arg0);
	}

	/**
	 * @throws RuntimeException if the method is accessed while
	 * the delegate reader is not open.
	 * @see IndexReader#isDeleted(int)
	 */
	public boolean isDeleted(int arg0) {
		try {
			return getReader().isDeleted(arg0);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe.getMessage());
		}
	}

	/**
	 * @throws RuntimeException if the method is accessed while
	 * the delegate reader is not open.
	 * @see IndexReader#maxDoc()
	 */
	public int maxDoc() {
		try {
			return getReader().maxDoc();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe.getMessage());
		}
	}

	/**
	 * @see IndexReader#norms(String)
	 */
	public byte[] norms(String arg0) throws IOException {
		return getReader().norms(arg0);
	}

	/**
	 * @throws RuntimeException if the method is accessed while
	 * the delegate reader is not open.
	 * @see IndexReader#numDocs()
	 */
	public int numDocs() {
		try {
			return getReader().numDocs();
		}
		catch (IOException ioe) {
			throw new RuntimeException(ioe.getMessage());
		}
	}

	/**
	 * @see IndexReader#termDocs()
	 */
	public TermDocs termDocs() throws IOException {
		return getReader().termDocs();
	}

	/**
	 * @see IndexReader#termDocs(Term)
	 */
	public TermDocs termDocs(Term arg0) throws IOException {
		return getReader().termDocs(arg0);
	}

	/**
	 * @see IndexReader#termPositions()
	 */
	public TermPositions termPositions() throws IOException {
		return getReader().termPositions();
	}

	/**
	 * @see IndexReader#termPositions(Term)
	 */
	public TermPositions termPositions(Term arg0) throws IOException {
		return getReader().termPositions(arg0);
	}

	/**
	 * @see IndexReader#terms()
	 */
	public TermEnum terms() throws IOException {
		return getReader().terms();
	}

	/**
	 * @see IndexReader#terms(Term)
	 */
	public TermEnum terms(Term arg0) throws IOException {
		return getReader().terms(arg0);
	}

	/**
	 * @see IndexReader#doDelete(int)
	 */
	protected void doDelete(int arg0) throws IOException {
		getReader().doDelete(arg0);
	}

//	-----------------------------------------------
//	 Setters and getters
//	-----------------------------------------------	

	/**
	 * Gets the wait time used to give consumers of this
	 * reader time to finish up before the underlying files
	 * are closed.
	 * 
	 * @return The time to wait in milliseconds.
	 */
	public long getWaitToClose() {
		return waitToClose;
	}

	/** <p>
	 * Sets the wait time used to give consumers of this
	 * reader time to finish up before the underlying files
	 * are closed.
	 * </p><p>
	 * If the time is less than {@link #getMinimumWaitToClose()},
	 * {@link #getMinimumWaitToClose()} will be used instead.
	 * </p>
	 * @param waitToClose The time to wait in milliseconds.
	 */
	public void setWaitToClose(long waitToClose) {
		if (waitToClose >= getMinimumWaitToClose()) {
			this.waitToClose = waitToClose;
		}
		else {
			this.waitToClose = getMinimumWaitToClose();
		}
	}

	/**
	 * Gets the refresh interval used to check for changes to
	 * the index being read. This is the time between checks.
	 * 
	 * @return The time to wait in milliseconds.
	 */
	public long getRefreshInterval() {
		return refreshInterval;
	}

	/** <p>
	 * Sets the refresh interval used to check for changes to
	 * the index being read. This is the time between checks.
	 * </p><p>
	 * Setting this value to 0 will prevent monitoring for refreshes.
	 * </p><p>
	 * This number should be reasonably high to prevent constant checking
	 * of file modification times on the disk.
	 * </p>
	 * @param refreshInterval The time to wait in milliseconds.
	 */
	public void setRefreshInterval(long refreshInterval) {
		this.refreshInterval = refreshInterval;
	}

	/**
	 * Gets the idle timeout used to close inactive readers.
	 * 
	 * @return The time to wait in milliseconds.
	 */
	public long getTimeout() {
		return timeout;
	}

	/** <p>
	 * Sets the idle timeout used to close inactive readers.
	 * </p><p>
	 * Setting this value to 0 will prevent monitoring for idle readers.
	 * </p><p>
	 * This number should be reasonably high to prevent constant
	 * opening and closing of files on the disk.
	 * </p>
	 * @param timeout The time to wait in milliseconds.
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/** <p>
	 * Gets the minimum value for the <code>waitToClose</code> property.
	 * </p><p>
	 * If this property has not been set, the system property with the key
	 * {@link #PROP_MINIMUM_WAIT_TO_CLOSE} will be checked.
	 * </p><p>
	 * If the system property is not set or cannot be parsed, {@link #DEFAULT_MINIMUM_WAIT_TO_CLOSE}
	 * will be used.
	 * </p>
	 * @see Long#getLong(String,String)
	 * @return The minimum time in milliseconds.
	 */
	public static long getMinimumWaitToClose() {
		if (minimumWaitToClose == null) {
			minimumWaitToClose = Long.getLong(PROP_MINIMUM_WAIT_TO_CLOSE,DEFAULT_MINIMUM_WAIT_TO_CLOSE);
		}
		return minimumWaitToClose.longValue();
	}

	/**
	 * Sets the minimum value for the <code>waitToClose</code> property.
	 * 
	 * @param minWaitToClose The minimum time in milliseconds.
	 */
	public static void setMinimumWaitToClose(long minWaitToClose) {
		minimumWaitToClose = new Long(minWaitToClose);
	}

	/** <p>
	 * Gets the default value for the <code>waitToClose</code> property.
	 * </p><p>
	 * If this property has not been set, the system property with the key
	 * {@link #PROP_WAIT_TO_CLOSE} will be checked.
	 * </p><p>
	 * If the system property is not set or cannot be parsed, {@link #DEFAULT_WAIT_TO_CLOSE}
	 * will be used.
	 * </p>
	 * @see Long#getLong(String,String)
	 * @return The default time in milliseconds.
	 */
	public static long getDefaultWaitToClose() {
		if (defaultWaitToClose == null) {
			defaultWaitToClose = Long.getLong(PROP_WAIT_TO_CLOSE,DEFAULT_WAIT_TO_CLOSE);
		}
		return defaultWaitToClose.longValue();
	}

	/**
	 * Sets the default value for the <code>waitToClose</code> property.
	 * 
	 * @param waitToClose The default time in milliseconds.
	 */
	public static void setDefaultWaitToClose(long waitToClose) {
		defaultWaitToClose = new Long(waitToClose);
	}

	/** <p>
	 * Gets the default value for the <code>refreshInterval</code> property.
	 * </p><p>
	 * If this property has not been set, the system property with the key
	 * {@link #PROP_REFRESH_INTERVAL} will be checked.
	 * </p><p>
	 * If the system property is not set or cannot be parsed, {@link #DEFAULT_REFRESH_INTERVAL}
	 * will be used.
	 * </p>
	 * @see Long#getLong(String,String)
	 * @return The default time in milliseconds.
	 */
	public static long getDefaultRefreshInterval() {
		if (defaultRefreshInterval == null) {
			defaultRefreshInterval = Long.getLong(PROP_REFRESH_INTERVAL,DEFAULT_REFRESH_INTERVAL);
		}
		return defaultRefreshInterval.longValue();
	}

	/**
	 * Sets the default value for the <code>refreshInterval</code> property.
	 * 
	 * @param refreshInterval The default time in milliseconds.
	 */
	public static void setDefaultRefreshInterval(long refreshInterval) {
		defaultRefreshInterval = new Long(refreshInterval);
	}

	/** <p>
	 * Gets the default value for the <code>timeout</code> property.
	 * </p><p>
	 * If this property has not been set, the system property with the key
	 * {@link #PROP_TIMEOUT} will be checked.
	 * </p><p>
	 * If the system property is not set or cannot be parsed, {@link #DEFAULT_TIMEOUT}
	 * will be used.
	 * </p>
	 * @see Long#getLong(String,String)
	 * @return The default time in milliseconds.
	 */
	public static long getDefaultTimeout() {
		if (defaultTimeout == null) {
			defaultTimeout = Long.getLong(PROP_TIMEOUT,DEFAULT_TIMEOUT);
		}
		return defaultTimeout.longValue();
	}

	/**
	 * Sets the default value for the <code>timeout</code> property.
	 * 
	 * @param timeout The default time in milliseconds.
	 */
	public static void setDefaultTimeout(long timeout) {
		defaultTimeout = new Long(timeout);
	}

	/**
	 * Gets the last accessed time for this reader.
	 * 
	 * @see System#currentTimeMillis();
	 * @return The time this index was last accessed by this reader.
	 */
	public long getLastAccessed() {
		return lastAccessed;
	}
	
	/**
	 * Gets the time this index reported as its last modified time when it was opened.
	 * 
	 * @see IndexReader#lastModified(Directory);
	 * @return The time this index was modified at the time it was opened.
	 */
	public long getModifiedTimeOnOpen() {
		return modifiedTimeOnOpen;
	}

}
