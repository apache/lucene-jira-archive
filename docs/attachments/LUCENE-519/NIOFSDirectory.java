package org.apache.lucene.store;

import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;

/**
 * Alternative implementation of FSDirectory that uses NIO to
 * avoid sychronization on the file pointer.
 *
 * <p>To use this, invoke Java with the System property
 * org.apache.lucene.FSDirectory.class set to
 * org.apache.lucene.store.NIOFSDirectory.  This will cause {@link
 * FSDirectory#getDirectory(File,boolean)} to return instances of this class.
 *
 * @author Bogdan Ghidireac
 */
public class NIOFSDirectory extends FSDirectory {

    public IndexInput openInput(String name) throws IOException {
        return new NIOFSIndexInput(new File(getFile(), name));
    }
}

/**
 * The NIOFSIndexInput uses {@link FileChannel#transferTo(long, long, WritableByteChannel)}
 * that does not modify the channel's position. 
 */
class NIOFSIndexInput extends BufferedIndexInput implements Cloneable {

    private FileChannel channel;
    private long position;
    private long length;
    boolean isClone;

    public NIOFSIndexInput(File path) throws IOException {
        RandomAccessFile file = new RandomAccessFile(path, "r");

        length = file.length();
        channel = file.getChannel();
    }

    public void readInternal(final byte[] dest, final int destOffset, final int len) throws IOException {
        channel.transferTo(position, len, new WritableByteChannel() {
            private int bytesLeft = len;
            private int relativeOffset = destOffset;

            public int write(ByteBuffer srcBuffer) {
                int bytesInBuffer = srcBuffer.limit();
                int bytesToCopy = bytesInBuffer >= bytesLeft ? bytesLeft : bytesInBuffer;

                srcBuffer.get(dest, relativeOffset, bytesToCopy);

                relativeOffset += bytesToCopy;
                bytesLeft -= bytesToCopy;

                return bytesToCopy;
            }

            public boolean isOpen() {
                return true;
            }

            public void close() {
            }
        });

        position += len;
    }

    public void close() throws IOException {
        if (!isClone)
            channel.close();
    }

    public void seekInternal(long pos) {
        position = pos;
    }

    public long length() {
        return length;
    }

    public Object clone() {
        NIOFSIndexInput clone = (NIOFSIndexInput) super.clone();
        clone.isClone = true;
        
        return clone;
    }
}