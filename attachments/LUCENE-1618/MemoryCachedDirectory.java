package ru.hh.searcher.lucene.directory;

import java.io.FileNotFoundException;
import java.io.IOException;
import static java.lang.System.arraycopy;
import static java.util.Collections.synchronizedMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;

/**
 * Warning! This Directory cannot handle individual file size > Integer.MAX_VALUE (2Gb) (not that you often try to fit
 * such beasts in memory)
 */

public final class MemoryCachedDirectory extends Directory {
  private final Directory delegate;
  private final Map<String, MemoryFile> files = synchronizedMap(new HashMap<String, MemoryFile>());

  public MemoryCachedDirectory(Directory delegate) throws IOException {
    this.delegate = delegate;

    for (String file : delegate.list()) {
      mirrorFile(file);
    }
  }

  public void deleteFile(String name) throws IOException {
    delegate.deleteFile(name);
    files.remove(name);
  }

  public boolean fileExists(String name) throws IOException {
    return files.containsKey(name);
  }

  public long fileLength(String name) throws IOException {
    MemoryFile file = files.get(name);

    if (file == null) {
      throw new FileNotFoundException(name);
    }

    return file.getLength();
  }

  public long fileModified(String name) throws IOException {
    MemoryFile file = files.get(name);

    if (file == null) {
      throw new FileNotFoundException(name);
    }

    return file.getLastModified();
  }

  public String[] list() throws IOException {
    Set<String> names = files.keySet();
    return names.toArray(new String[files.size()]);
  }

  @Deprecated
  public void renameFile(String from, String to) throws IOException {
    throw new UnsupportedOperationException("deprecated");
  }

  public void touchFile(String name) throws IOException {
    delegate.touchFile(name);
    files.get(name).setLastModified(delegate.fileModified(name));
  }

  public Lock makeLock(String name) {
    return delegate.makeLock(name);
  }

  public void clearLock(String name) throws IOException {
    delegate.clearLock(name);
  }

  public void setLockFactory(LockFactory lockFactory) {
    delegate.setLockFactory(lockFactory);
  }

  public LockFactory getLockFactory() {
    return delegate.getLockFactory();
  }

  public String getLockID() {
    return delegate.getLockID();
  }

  public void sync(String name) throws IOException {
    delegate.sync(name);
  }

  public void close() throws IOException {
    delegate.close();
  }

  public IndexInput openInput(String name) throws IOException {
    MemoryFile file = files.get(name);
    if (file == null) {
      throw new FileNotFoundException(name);
    }

    return file.getIndexInput();
  }

  public IndexOutput createOutput(String name) throws IOException {
    IndexOutput indexOutput = delegate.createOutput(name);
    return new MemoryIndexOutput(indexOutput, name);
  }

  private void mirrorFile(String name) throws IOException {
    files.put(name, new MemoryFile(name));
  }

  private final class MemoryFile {
    private byte[] buffer;
    private long lastModified;
    private long length;

    MemoryFile(String name) throws IOException {
      lastModified = delegate.fileModified(name);
      length = delegate.fileLength(name);

      if (length > Integer.MAX_VALUE) {
        throw new IOException("File size is " + length + ", which is more than Integer.MAX_VALUE and unsupported by MemoryCachedDirectory");
      }

      IndexInput source = delegate.openInput(name);
      buffer = new byte[(int) length];
      source.readBytes(buffer, 0, (int) length, false);
      source.close();
    }

    IndexInput getIndexInput() throws IOException {
      return new MemoryIndexInput(buffer, length);
    }

    long getLength() {
      return length;
    }

    long getLastModified() {
      return lastModified;
    }

    void setLastModified(long lastModified) {
      this.lastModified = lastModified;
    }
  }

  private final class MemoryIndexOutput extends IndexOutput {
    private final IndexOutput delegate;
    private final String name;

    MemoryIndexOutput(IndexOutput delegate, String name) {
      this.delegate = delegate;
      this.name = name;
    }

    public void writeByte(byte b) throws IOException {
      delegate.writeByte(b);
    }

    public void writeBytes(byte[] b, int length) throws IOException {
      delegate.writeBytes(b, length);
    }

    public void writeBytes(byte[] b, int offset, int length) throws IOException {
      delegate.writeBytes(b, offset, length);
    }

    public void writeInt(int i) throws IOException {
      delegate.writeInt(i);
    }

    public void writeVInt(int i) throws IOException {
      delegate.writeVInt(i);
    }

    public void writeLong(long i) throws IOException {
      delegate.writeLong(i);
    }

    public void writeVLong(long i) throws IOException {
      delegate.writeVLong(i);
    }

    public void writeString(String s) throws IOException {
      delegate.writeString(s);
    }

    @SuppressWarnings({"deprecation"})
    public void writeChars(String s, int start, int length) throws IOException {
      delegate.writeChars(s, start, length);
    }

    @SuppressWarnings({"deprecation"})
    public void writeChars(char[] s, int start, int length) throws IOException {
      delegate.writeChars(s, start, length);
    }

    public void copyBytes(IndexInput input, long numBytes) throws IOException {
      delegate.copyBytes(input, numBytes);
    }

    public void flush() throws IOException {
      delegate.flush();
    }

    public void close() throws IOException {
      delegate.close();

      mirrorFile(name);
    }

    public long getFilePointer() {
      return delegate.getFilePointer();
    }

    public void seek(long pos) throws IOException {
      delegate.seek(pos);
    }

    public long length() throws IOException {
      return delegate.length();
    }

    public void setLength(long length) throws IOException {
      delegate.setLength(length);
    }
  }

  private final static class MemoryIndexInput extends IndexInput {
    private final byte[] buffer;
    private final long length;

    private int position;

    public MemoryIndexInput(byte buffer[], long length) throws IOException {
      this.buffer = buffer;
      this.length = length;
      position = 0;
    }

    public void close() {
    }

    public long length() {
      return length;
    }

    public byte readByte() throws IOException {
      if (position >= length) { // eq to (position + 1 > length)
        throw new IOException("Read past EOF");
      }

      return buffer[position++];
    }

    public void readBytes(byte[] b, int offset, int len) throws IOException {
      if (position + len > length) {
        throw new IOException("Read past EOF");
      }

      arraycopy(buffer, position, b, offset, len);

      position += len;
    }

    public long getFilePointer() {
      return position;
    }

    public void seek(long pos) throws IOException {
      if (pos < 0 || pos >= length) {
        throw new IOException("Seek past S/EOF");
      }

      position = (int) pos;
    }
  }
}
