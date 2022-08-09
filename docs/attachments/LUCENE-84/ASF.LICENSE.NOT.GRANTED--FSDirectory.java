package org.apache.lucene.store;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001, 2003 The Apache Software Foundation.  All
 * rights reserved.
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
 * 4. The names "Apache" and "Apache Software Foundation" and
 *    "Apache Lucene" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Lucene", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
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
 */

import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Hashtable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.lucene.util.Constants;

/**
 * Straightforward implementation of {@link Directory} as a directory of files.
 * <p>If the system property 'disableLuceneLocks' has the String value of
 * "true", lock creation will be disabled.
 *
 * @see Directory
 * @author Doug Cutting
 */
public final class FSDirectory extends Directory {
  /** This cache of directories ensures that there is a unique Directory
   * instance per path, so that synchronization on the Directory can be used to
   * synchronize access between readers and writers.
   *
   * This should be a WeakHashMap, so that entries can be GC'd, but that would
   * require Java 1.2.  Instead we use refcounts...
   */
  private static final Hashtable DIRECTORIES = new Hashtable();

  private static boolean _disableLocks = false;

  private boolean locksDisabledByProp()
  {
      return Boolean.getBoolean("disableLuceneLocks") || Constants.JAVA_1_1;
  }

  private static MessageDigest DIGESTER;
 
  static {
    try {
      DIGESTER = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e.toString());
    }
  }

  /** A buffer optionally used in renameTo method */
  private byte[] buffer = null;

  /** Returns the directory instance for the named location.
   *
   * <p>Directories are cached, so that, for a given canonical path, the same
   * FSDirectory instance will always be returned.  This permits
   * synchronization on directories.
   *
   * @param path the path to the directory.
   * @param create if true, create, or erase any existing contents.
   * @return the FSDirectory for the named file.  */
  public static FSDirectory getDirectory(String path, boolean create)
      throws IOException {
    return getDirectory(new File(path), create);
  }

  /** Returns the directory instance for the named location, with option of
   *  disabled locking.
   *
   * <p>Directories are cached, so that, for a given canonical path, the same
   * FSDirectory instance will always be returned.  This permits
   * synchronization on directories.
   *
   * @param path the path to the directory.
   * @param create if true, create, or erase any existing contents.
   * @param useLocks if false, don't use locks during index reads. Useful in
   *                 read-only filesystems or from applets.
   * @return the FSDirectory for the named file.  */
  public static FSDirectory getDirectory(String path, boolean create, boolean useLocks)
      throws IOException {
    _disableLocks = !useLocks;
    return getDirectory(new File(path), create);
  }

  /** Returns the directory instance for the named location.
   *
   * <p>Directories are cached, so that, for a given canonical path, the same
   * FSDirectory instance will always be returned.  This permits
   * synchronization on directories.
   *
   * @param file the path to the directory.
   * @param create if true, create, or erase any existing contents.
   * @return the FSDirectory for the named file.  */
  public static FSDirectory getDirectory(File file, boolean create)
    throws IOException {
    file = new File(file.getCanonicalPath());
    FSDirectory dir;
    synchronized (DIRECTORIES) {
      dir = (FSDirectory)DIRECTORIES.get(file);
      if (dir == null) {
        dir = new FSDirectory(file, create);
        DIRECTORIES.put(file, dir);
      } else if (create) {
        dir.create();
      }
    }
    synchronized (dir) {
      dir.refCount++;
    }
    return dir;
  }

  /** Returns the directory instance for the named location, with option of
   *  disabled locking.
   *
   * <p>Directories are cached, so that, for a given canonical path, the same
   * FSDirectory instance will always be returned.  This permits
   * synchronization on directories.
   *
   * @param file the path to the directory.
   * @param create if true, create, or erase any existing contents.
   * @param useLocks if false, don't use locks during index reads. Useful in
   *                 read-only filesystems or from applets.
   * @return the FSDirectory for the named file.  */
  public static FSDirectory getDirectory(File file, boolean create, boolean useLocks)
    throws IOException {
    _disableLocks = !useLocks;
    return getDirectory(file, create);
  }

  private File directory = null;
  private int refCount;

  private FSDirectory(File path, boolean create) throws IOException {
    directory = path;

    if (create)
      create();

    if (!directory.isDirectory())
      throw new IOException(path + " not a directory");
  }

  private synchronized void create() throws IOException {
    if (!directory.exists())
      if (!directory.mkdir())
        throw new IOException("Cannot create directory: " + directory);

    String[] files = directory.list();            // clear old files
    for (int i = 0; i < files.length; i++) {
      File file = new File(directory, files[i]);
      if (!file.delete())
        throw new IOException("couldn't delete " + files[i]);
    }
    
    String lockPrefix = getLockPrefix().toString(); // clear old locks
    File tmpdir = new File(System.getProperty("java.io.tmpdir"));
    files = tmpdir.list();
    for (int i = 0; i < files.length; i++) {      
      if (!files[i].startsWith(lockPrefix))
        continue;
      File file = new File(tmpdir, files[i]);
      if (!file.delete())
        throw new IOException("couldn't delete " + files[i]);
    }
  }

  /** Returns an array of strings, one for each file in the directory. */
  public final String[] list() throws IOException {
    return directory.list();
  }

  /** Returns true iff a file with the given name exists. */
  public final boolean fileExists(String name) throws IOException {
    File file = new File(directory, name);
    return file.exists();
  }

  /** Returns the time the named file was last modified. */
  public final long fileModified(String name) throws IOException {
    File file = new File(directory, name);
    return file.lastModified();
  }

  /** Returns the time the named file was last modified. */
  public static final long fileModified(File directory, String name)
       throws IOException {
    File file = new File(directory, name);
    return file.lastModified();
  }

  /** Set the modified time of an existing file to now. */
  public void touchFile(String name) throws IOException {
    File file = new File(directory, name);
    file.setLastModified(System.currentTimeMillis());
  }

  /** Returns the length in bytes of a file in the directory. */
  public final long fileLength(String name) throws IOException {
    File file = new File(directory, name);
    return file.length();
  }

  /** Removes an existing file in the directory. */
  public final void deleteFile(String name) throws IOException {
    File file = new File(directory, name);
    if (!file.delete())
      throw new IOException("couldn't delete " + name);
  }

  /** Renames an existing file in the directory. */
  public final synchronized void renameFile(String from, String to)
      throws IOException {
    File old = new File(directory, from);
    File nu = new File(directory, to);

    /* This is not atomic.  If the program crashes between the call to
       delete() and the call to renameTo() then we're screwed, but I've
       been unable to figure out how else to do this... */

    if (nu.exists())
      if (!nu.delete())
        throw new IOException("couldn't delete " + to);

    // Rename the old file to the new one. Unfortunately, the renameTo()
    // method does not work reliably under some JVMs.  Therefore, if the
    // rename fails, we manually rename by copying the old file to the new one
    if (!old.renameTo(nu)) {
      java.io.InputStream in = null;
      java.io.OutputStream out = null;
      try {
        in = new FileInputStream(old);
        out = new FileOutputStream(nu);
        // see if the buffer needs to be initialized. Initialization is
        // only done on-demand since many VM's will never run into the renameTo
        // bug and hence shouldn't waste 1K of mem for no reason.
        if (buffer == null) {
          buffer = new byte[1024];
        }
        int len;
        while ((len = in.read(buffer)) >= 0) {
          out.write(buffer, 0, len);
        }

        // delete the old file.
        old.delete();
      }
      catch (IOException ioe) {
        throw new IOException("couldn't rename " + from + " to " + to);
      }
      finally {
        if (in != null) {
          try {
            in.close();
          } catch (IOException e) {
            throw new RuntimeException("could not close input stream: " + e.getMessage());
          }
        }
        if (out != null) {
          try {
            out.close();
          } catch (IOException e) {
            throw new RuntimeException("could not close output stream: " + e.getMessage());
          }
        }
      }
    }
  }

  /** Creates a new, empty file in the directory with the given name.
      Returns a stream writing this file. */
  public final OutputStream createFile(String name) throws IOException {
    return new FSOutputStream(new File(directory, name));
  }

  /** Returns a stream reading an existing file. */
  public final InputStream openFile(String name) throws IOException {
    return new FSInputStream(new File(directory, name));
  }

  /**
   * So we can do some byte-to-hexchar conversion below
   */
  private static final char[] HEX_DIGITS =
  {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
  
  /**
   * A Lock that doesn't really create a lock.  Used by {@link #makeLock}.
   */
  public class StubLock extends Lock {
    public boolean obtain() throws IOException {
      return true;
    }
    public void release() {
      return;
    }
    public boolean isLocked() {
      return false;
    }
    public String toString() {
      return "Lock@locksDisabled";
    }
  }

  /**
   * A Lock that really creates a lock.  Used by {@link #makeLock}.
   */
  public class RealLock extends Lock {

    private File lockFile = null;
    
    public RealLock(String name) {
      StringBuffer buf = getLockPrefix();
      buf.append("-");
      buf.append(name);
    
      // make the lock file in tmp, where anyone can create files.
      lockFile = new File(System.getProperty("java.io.tmpdir"),
                                     buf.toString());
    }
                                   
    public boolean obtain() throws IOException {
      return lockFile.createNewFile();
    }
    public void release() {
      lockFile.delete();
    }
    public boolean isLocked() {
      return lockFile.exists();
    }

    public String toString() {
      return "Lock@" + lockFile;
    }
  }

  /** Constructs a {@link Lock} with the specified name.  Locks are implemented
   * with {@link File#createNewFile() }.
   *
   * <p>In JDK 1.1 or if system property <I>disableLuceneLocks</I> is the
   * string "true", locks are disabled.  Assigning this property any other
   * string will <B>not</B> prevent creation of lock files.
   * 
   * Disabling locks is useful when using Lucene on read-only medium, such
   * as CD-ROM, or from an applet that does not have permission to write lock
   * files to the filesystem or set properties.
   *
   * @param name the name of the lock file
   * @return an instance of <code>Lock</code> holding the lock
   */
  public final Lock makeLock(String name) {

    if (_disableLocks || locksDisabledByProp()) {
      // if locks disabled, return a stubbed Lock object that does nothing
      return new StubLock();
    }
    else {
      // otherwise, return a real lock
      return new RealLock(name);
    }
  }

  private StringBuffer getLockPrefix() {
    String dirName;                               // name to be hashed
    try {
      dirName = directory.getCanonicalPath();
    } catch (IOException e) {
      throw new RuntimeException(e.toString());
    }
    
    byte digest[];
    synchronized (DIGESTER) {
      digest = DIGESTER.digest(dirName.getBytes());
    }
    StringBuffer buf = new StringBuffer();
    buf.append("lucene-");
    for (int i = 0; i < digest.length; i++) {
      int b = digest[i];
      buf.append(HEX_DIGITS[(b >> 4) & 0xf]);
      buf.append(HEX_DIGITS[b & 0xf]);
    }

    return buf;
  }

  /** Closes the store to future operations. */
  public final synchronized void close() throws IOException {
    if (--refCount <= 0) {
      synchronized (DIRECTORIES) {
        DIRECTORIES.remove(directory);
      }
    }
  }

  /** For debug output. */
  public String toString() {
    return "FSDirectory@" + directory;
  }
}


final class FSInputStream extends InputStream {
  private class Descriptor extends RandomAccessFile {
    /* DEBUG */
    //private String name;
    /* DEBUG */
    public long position;
    public Descriptor(File file, String mode) throws IOException {
      super(file, mode);
      /* DEBUG */
      //name = file.toString();
      //debug_printInfo("OPEN");
      /* DEBUG */
    }
    
    /* DEBUG */
    //public void close() throws IOException {
    //  debug_printInfo("CLOSE");
    //    super.close();
    //}
    //
    //private void debug_printInfo(String op) {
    //  try { throw new Exception(op + " <" + name + ">"); 
    //  } catch (Exception e) {
    //    java.io.StringWriter sw = new java.io.StringWriter();
    //    java.io.PrintWriter pw = new java.io.PrintWriter(sw);
    //    e.printStackTrace(pw);
    //    System.out.println(sw.getBuffer().toString());
    //  }
    //}
    /* DEBUG */
  }

  Descriptor file = null;
  boolean isClone;

  public FSInputStream(File path) throws IOException {
    file = new Descriptor(path, "r");
    length = file.length();
  }

  /** InputStream methods */
  protected final void readInternal(byte[] b, int offset, int len)
       throws IOException {
    synchronized (file) {
      long position = getFilePointer();
      if (position != file.position) {
        file.seek(position);
        file.position = position;
      }
      int total = 0;
      do {
        int i = file.read(b, offset+total, len-total);
        if (i == -1)
          throw new IOException("read past EOF");
        file.position += i;
        total += i;
      } while (total < len);
    }
  }

  public final void close() throws IOException {
    if (!isClone)
      file.close();
  }

  /** Random-access methods */
  protected final void seekInternal(long position) throws IOException {
  }

  protected final void finalize() throws IOException {
    close();            // close the file
  }

  public Object clone() {
    FSInputStream clone = (FSInputStream)super.clone();
    clone.isClone = true;
    return clone;
  }
  
  /** Method used for testing. Returns true if the underlying
   *  file descriptor is valid.
   */
  boolean isFDValid() throws IOException {
    return file.getFD().valid();
  }
}


final class FSOutputStream extends OutputStream {
  RandomAccessFile file = null;

  public FSOutputStream(File path) throws IOException {
    file = new RandomAccessFile(path, "rw");
  }

  /** output methods: */
  public final void flushBuffer(byte[] b, int size) throws IOException {
    file.write(b, 0, size);
  }
  public final void close() throws IOException {
    super.close();
    file.close();
  }

  /** Random-access methods */
  public final void seek(long pos) throws IOException {
    super.seek(pos);
    file.seek(pos);
  }
  public final long length() throws IOException {
    return file.length();
  }

  protected final void finalize() throws IOException {
    file.close();          // close the file
  }

}
