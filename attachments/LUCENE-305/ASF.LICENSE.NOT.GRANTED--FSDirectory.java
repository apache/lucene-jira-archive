package org.apache.lucene.store;

/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public class FSDirectory extends Directory {
  /** This cache of directories ensures that there is a unique Directory
   * instance per path, so that synchronization on the Directory can be used to
   * synchronize access between readers and writers.
   *
   * This should be a WeakHashMap, so that entries can be GC'd, but that would
   * require Java 1.2.  Instead we use refcounts...
   */
  private static final Hashtable DIRECTORIES = new Hashtable();

  private static final boolean DISABLE_LOCKS =
      Boolean.getBoolean("disableLuceneLocks") || Constants.JAVA_1_1;

  /**
   * Directory specified by <code>org.apache.lucene.lockdir</code>
   * or <code>java.io.tmpdir</code> system property
   */
  public static final String LOCK_DIR =
    System.getProperty("org.apache.lucene.lockdir",
      System.getProperty("java.io.tmpdir"));

  /** The default class which implements filesystem-based directories. */
  private static final Class IMPL;
  static {
    try {
      String name =
        System.getProperty("org.apache.lucene.FSDirectory.class",
                           FSDirectory.class.getName());
      IMPL = Class.forName(name);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
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
        try {
          dir = (FSDirectory)IMPL.newInstance();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        dir.init(file, create);
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

  private File directory = null;
  private int refCount;
  private File lockDir;

  protected FSDirectory() {};                     // permit subclassing

  private void init(File path, boolean create) throws IOException {
    directory = path;

    if (LOCK_DIR == null) {
      lockDir = directory;
    }
    else {
      lockDir = new File(LOCK_DIR);
    }
    if (create) {
      create();
    }

    if (!directory.isDirectory())
      throw new IOException(path + " not a directory");
  }

  private synchronized void create() throws IOException {
    if (!directory.exists())
      if (!directory.mkdirs())
        throw new IOException("Cannot create directory: " + directory);

    String[] files = directory.list();            // clear old files
    for (int i = 0; i < files.length; i++) {
      File file = new File(directory, files[i]);
      if (!file.delete())
        throw new IOException("Cannot delete " + files[i]);
    }

    String lockPrefix = getLockPrefix().toString(); // clear old locks
    files = lockDir.list();
    for (int i = 0; i < files.length; i++) {
      if (!files[i].startsWith(lockPrefix))
        continue;
      File lockFile = new File(lockDir, files[i]);
      if (!lockFile.delete())
        throw new IOException("Cannot delete " + files[i]);
    }
  }

  /** Returns an array of strings, one for each file in the directory. */
  public String[] list() {
    return directory.list();
  }

  /** Returns true iff a file with the given name exists. */
  public boolean fileExists(String name) {
    File file = new File(directory, name);
    return file.exists();
  }

  /** Returns the time the named file was last modified. */
  public long fileModified(String name) {
    File file = new File(directory, name);
    return file.lastModified();
  }

  /** Returns the time the named file was last modified. */
  public static long fileModified(File directory, String name) {
    File file = new File(directory, name);
    return file.lastModified();
  }

  /** Set the modified time of an existing file to now. */
  public void touchFile(String name) {
    File file = new File(directory, name);
    file.setLastModified(System.currentTimeMillis());
  }

  /** Returns the length in bytes of a file in the directory. */
  public long fileLength(String name) {
    File file = new File(directory, name);
    return file.length();
  }

  /** Removes an existing file in the directory. */
  public void deleteFile(String name) throws IOException {
    File file = new File(directory, name);
    if (!file.delete())
      throw new IOException("Cannot delete " + name);
  }

  /** Renames an existing file in the directory. */
  public synchronized void renameFile(String from, String to)
      throws IOException {
    File old = new File(directory, from);
    File nu = new File(directory, to);

    /* This is not atomic.  If the program crashes between the call to
       delete() and the call to renameTo() then we're screwed, but I've
       been unable to figure out how else to do this... */

    if (nu.exists())
      if (!nu.delete())
        throw new IOException("Cannot delete " + to);

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
        throw new IOException("Cannot rename " + from + " to " + to);
      }
      finally {
        if (in != null) {
          try {
            in.close();
          } catch (IOException e) {
            throw new RuntimeException("Cannot close input stream: " + e.getMessage());
          }
        }
        if (out != null) {
          try {
            out.close();
          } catch (IOException e) {
            throw new RuntimeException("Cannot close output stream: " + e.getMessage());
          }
        }
      }
    }
  }

  /** Creates a new, empty file in the directory with the given name.
      Returns a stream writing this file. */
  public IndexOutput createOutput(String name) throws IOException {
    return new FSIndexOutput(new File(directory, name));
  }

  /** Returns a stream reading an existing file. */
  public IndexInput openInput(String name) throws IOException {
    return new FSIndexInput(new File(directory, name));
  }

  /**
   * So we can do some byte-to-hexchar conversion below
   */
  private static final char[] HEX_DIGITS =
  {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

  /** Constructs a {@link Lock} with the specified name.  Locks are implemented
   * with {@link File#createNewFile() }.
   *
   * <p>In JDK 1.1 or if system property <I>disableLuceneLocks</I> is the
   * string "true", locks are disabled.  Assigning this property any other
   * string will <B>not</B> prevent creation of lock files.  This is useful for
   * using Lucene on read-only medium, such as CD-ROM.
   *
   * @param name the name of the lock file
   * @return an instance of <code>Lock</code> holding the lock
   */
  public Lock makeLock(String name) {
    StringBuffer buf = getLockPrefix();
    buf.append("-");
    buf.append(name);

    /* This is the override TODO: finish documentation */
    if( LockFactory.isOverridden() ) {
        /* Should better handle exceptions, but method
           doesn't throw any, so we have to eat... */
        try {
            return LockFactory.getLock( buf.toString() );
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    } else {
        // create a lock file
        final File lockFile = new File(lockDir, buf.toString());
        /* Default lock mechanism (filesystem lock) */
        return new Lock() {
          public boolean obtain() throws IOException {
            if (DISABLE_LOCKS)
              return true;

            if (!lockDir.exists()) {
              if (!lockDir.mkdirs()) {
                throw new IOException("Cannot create lock directory: " + lockDir);
              }
            }

            return lockFile.createNewFile();
          }
          public void release() {
            if (DISABLE_LOCKS)
              return;
            lockFile.delete();
          }
          public boolean isLocked() {
            if (DISABLE_LOCKS)
              return false;
            return lockFile.exists();
          }

          public String toString() {
            return "Lock@" + lockFile;
          }
        };
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
  public synchronized void close() {
    if (--refCount <= 0) {
      synchronized (DIRECTORIES) {
        DIRECTORIES.remove(directory);
      }
    }
  }

  public File getFile() {
    return directory;
  }

  /** For debug output. */
  public String toString() {
    return this.getClass().getName() + "@" + directory;
  }
}


class FSIndexInput extends BufferedIndexInput {

  private class Descriptor extends RandomAccessFile {
    public long position;
    public Descriptor(File file, String mode) throws IOException {
      super(file, mode);
    }
  }

  private Descriptor file = null;
  boolean isClone;
  private long length;

  public FSIndexInput(File path) throws IOException {
    file = new Descriptor(path, "r");
    length = file.length();
  }

  /** IndexInput methods */
  protected void readInternal(byte[] b, int offset, int len)
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

  public void close() throws IOException {
    if (!isClone)
      file.close();
  }

  protected void seekInternal(long position) {
  }

  public long length() {
    return length;
  }

  protected void finalize() throws IOException {
    close();            // close the file
  }

  public Object clone() {
    FSIndexInput clone = (FSIndexInput)super.clone();
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


class FSIndexOutput extends BufferedIndexOutput {
  RandomAccessFile file = null;

  public FSIndexOutput(File path) throws IOException {
    file = new RandomAccessFile(path, "rw");
  }

  /** output methods: */
  public void flushBuffer(byte[] b, int size) throws IOException {
    file.write(b, 0, size);
  }
  public void close() throws IOException {
    super.close();
    file.close();
  }

  /** Random-access methods */
  public void seek(long pos) throws IOException {
    super.seek(pos);
    file.seek(pos);
  }
  public long length() throws IOException {
    return file.length();
  }

  protected void finalize() throws IOException {
    file.close();          // close the file
  }

}