/*
 * Created on Dec 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.apache.lucene.store;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

/**
 * @author jwang
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class FSLock extends Lock {

	private File _lockFile;
	private File _lockDir;
	private FileLock _lock;
	private RandomAccessFile rLockFile=null;
	
	FSLock(File lockDir,File lockFile){
		_lockDir=lockDir;
		_lockFile=lockFile;
	}
	/* (non-Javadoc)
	 * @see org.apache.lucene.store.Lock#obtain()
	 */
	public boolean obtain() throws IOException {
		if (!_lockDir.exists()){
			if (!_lockDir.mkdirs()) {
	            throw new IOException("Cannot create lock directory: " + _lockDir);
	          }
		}
		
		if (!_lockFile.exists()){
			if (!_lockFile.createNewFile())
				throw new IOException("Cannot create lock file: " + _lockFile);
		}
		
		if (isLocked()){
			return false;
		}
		
		rLockFile=new RandomAccessFile(_lockFile,"rw");
		_lock=rLockFile.getChannel().lock();
		
		return true;
	}

	/* (non-Javadoc)
	 * @see org.apache.lucene.store.Lock#release()
	 */
	public synchronized void release() {
		if (_lock!=null){
			try{
				_lock.release();
				_lock=null;
			}
			catch(IOException ioe){
				ioe.printStackTrace();
			}
			finally{
				try{
				rLockFile.close();
				
				}
				catch(IOException ioe){
					ioe.printStackTrace();
				}
				finally{
					rLockFile=null;
					_lock=null;
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.lucene.store.Lock#isLocked()
	 */
	public synchronized boolean isLocked() {
		return _lock!=null;
	}

}