package org.apache.lucene.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * wrapper for NIO FileChannel in order to circumvent problems with multiple threads reading the
 * same FileChannel, and to provide local cache. The current Windows implementation of FileChannel
 * has some synchronization even when performing positioned reads. See JDK bug #6265734.
 * 
 * The NioFile contains internal caching to reduce the number of physical disk reads.
 */
public class NioFile {
    static final int NCHANNELS = 4;
    static final int BLOCKSIZE = Integer.getInteger("org.apache.lucene.BlockSize",4096).intValue();
    
    static final boolean SINGLECHANNEL = !org.apache.lucene.util.Constants.WINDOWS;
    
    static public int cachehits = 0; 
    static public int cachemisses = 0; 
    
    static float cachePercent = .10f;
    static {
        int percent = Integer.getInteger("org.apache.lucene.CachePercent",30).intValue();
        if(percent<0 || percent >100)
            percent = 10;
        cachePercent = percent / 100.0f;
    }
    
    static long cacheSize = Long.getLong("org.apache.lucene.CacheSize",64*1024*1024L).longValue();
    
    File path;
    
    LinkedList queue = new LinkedList();
    int count=0;
    
    String mode;
    boolean open = true;
    
    FileChannel channel;
    
    static public MemoryLRUCache cache;

    public NioFile(File path,String mode) throws IOException {
        this.path = path;
        this.mode = mode;
        
        if(cache==null)
            cache = new MemoryLRUCache((long) Math.max(cacheSize,Runtime.getRuntime().maxMemory()*cachePercent));
        
        RandomAccessFile raf = new RandomAccessFile(path,mode);
        channel = raf.getChannel();
    }

    private FileChannel getChannel() throws IOException {
        if(SINGLECHANNEL)
            return channel;
        
        synchronized(queue) {
            while(queue.size()==0) {
                if(count<NCHANNELS) {
                    count++;
                    FileChannel channel = new RandomAccessFile(path,mode).getChannel();
                    return channel;
                }
                try {
                    queue.wait();
                } catch (InterruptedException e) {
                    throw new IOException("interrupted");
                }
            }
            FileChannel channel = (FileChannel) queue.removeFirst();
            return channel;
        }
    }

    private void release(FileChannel channel) {
        if(SINGLECHANNEL)
            return;
        
        synchronized(queue) {
            queue.add(channel);
            queue.notify();
        }
    }

    public void close() throws IOException {
        channel.close();
        
        if(SINGLECHANNEL)
            return;
        
        synchronized(queue) {
            for(Iterator i = queue.iterator();i.hasNext();){
                FileChannel fc = (FileChannel) i.next();
                fc.close();
                i.remove();
            }
        }
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public void read(byte[] b, int offset, int len, long position) throws IOException {
        do {
            long blockno = (position/BLOCKSIZE);
            BlockKey bk = new BlockKey(this,blockno);
            byte[] block = cache.get(bk);
            
            if(block==null) {
                cachemisses++;
                block = new byte[BLOCKSIZE];
                
                FileChannel channel = getChannel();
                try {
                    channel.read(ByteBuffer.wrap(block),blockno*BLOCKSIZE);
                } finally {
                    release(channel);
                }
                cache.put(bk,block);
            } else
                cachehits++;
            
            int blockoffset = (int) (position % BLOCKSIZE);
            int i = Math.min(len,BLOCKSIZE-blockoffset);
            
            System.arraycopy(block,blockoffset,b,offset,i);
            
            offset += i;
            len -= i;
            position += i;
            
        } while (len >0);
    }
    
    static class BlockKey {
        NioFile file;
        long blockno;

        public BlockKey(NioFile file, long blockno) {
            this.file = file;
            this.blockno = blockno;
        }
        public int hashCode() {
            return (int) (file.hashCode() ^ blockno);
        }
        public boolean equals(Object o){
            BlockKey bk0 = (BlockKey) o;
            return file==bk0.file && blockno==bk0.blockno;
        }
    }
}
