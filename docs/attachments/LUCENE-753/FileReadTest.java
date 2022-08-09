import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.Channel;
import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;
import java.lang.reflect.Constructor;
import java.util.Random;
import java.util.LinkedList;
import java.util.ArrayList;

/**
 * @author yonik
 * @version $Id$
 */
public class FileReadTest {

  static int poolsize=2;

  public static void main(String[] args) throws Exception {
    int argpos=0;
    final String filename = args[argpos++];
    final String implementation = args[argpos++];
    final boolean serial = args[argpos++].toLowerCase().equals("true");
    final int nThreads = Integer.parseInt(args[argpos++]);
    final int iterations = Integer.parseInt(args[argpos++]);
    final int bufsize=argpos<args.length ? Integer.parseInt(args[argpos++]) : 1024;
    MyFile.BUFFER_SIZE = bufsize;

    while (argpos < args.length) {
      String s = args[argpos++];
      if (s.equals("-poolsize")) {
        poolsize = Integer.parseInt(args[argpos++]);
      }
    }

    final RandomAccessFile f = new RandomAccessFile(filename,"r");
    final long filelen = f.length();
    final int nPages = (int)((filelen-1) / bufsize)+1;

    Class clazz = Class.forName(implementation);
    Constructor con = clazz.getConstructor(new Class[]{RandomAccessFile.class, String.class});
    final MyFile myfile = (MyFile)con.newInstance(new Object[]{f, filename});
    final int[] answer = new int[1];

    Thread[] threads = new Thread[nThreads];
    final long[] times = new long[nThreads];

    for (int i=0; i<threads.length; i++) {
      final int seed = i;
      threads[i] = new Thread() {
        Random r = new Random(seed);
        MyFile.MyReader reader = myfile.getReader(); // each thread gets one

        public void run() {
          long start = System.currentTimeMillis();
          int pg = r.nextInt(nPages);
          int val=0;
          for (int n=0; n<iterations; n++) {
            for (int j=0; j<nPages; j++) {
              int rn = r.nextInt(nPages);
              if (++pg>=nPages) pg-=nPages;
              long pos = (serial ? pg : rn) * bufsize;
              try {
                val += reader.read(pos);
              } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
              }
            }
          }
          synchronized (answer) { answer[0]+=val; }
          times[seed] = System.currentTimeMillis() - start;
        }
      };
    }

    for (int i=0;i<threads.length;i++) threads[i].start();
    for (int i=0;i<threads.length;i++) threads[i].join();

    long ms = 0;
    for (int i=0;i<nThreads;i++)
      ms += times[i];
    ms /= nThreads;
    int a;
    synchronized(answer) { a=answer[0]; }

    System.out.println("config:"
          + " impl=" + implementation
          + " serial="+serial + " nThreads="+nThreads +" iterations="+iterations
          + " bufsize="+bufsize + " poolsize=" + poolsize + " filelen=" + filelen
    );
    System.out.println("answer=" + a
                    + ", ms=" + ms
                    + ", MB/sec="+(filelen*iterations*nThreads)/(((double)ms)/1000)/1000000
    );

  }
}


abstract class MyFile {
  public final RandomAccessFile f;
  public static int BUFFER_SIZE=1024;
  public MyFile(RandomAccessFile f) {
    this.f = f;  
  }

  abstract MyReader getReader() throws Exception;

  abstract class MyReader {
    public abstract int read(long pos) throws IOException;
  }
}

class ClassicFile extends MyFile {
  long filepos=0;  // current position in the file

  public ClassicFile(RandomAccessFile f, String filename) {
    super(f);
  }

  MyReader getReader() {
    return new MyReader() {
      byte[] b = new byte[BUFFER_SIZE];
      public int read(long pos) throws IOException {
        int len;
        synchronized(f) {
          if (pos != filepos) f.seek(pos);
          len = f.read(b,0,BUFFER_SIZE);
          filepos = pos + len;
        }
        return b[0]+b[len>>1];
      }
    };
  }
}

class SeparateFile extends MyFile {
  String filename;
  public SeparateFile(RandomAccessFile f, String filename) throws Exception {
    super(f);
    this.filename = filename;
  }

  MyReader getReader() throws Exception {
    return new MyReader() {
      byte[] b = new byte[BUFFER_SIZE];
      RandomAccessFile f2 = new RandomAccessFile(filename, "r");
      long filepos=0;  // current position in the file      
      public int read(long pos) throws IOException {
        int len;
        if (pos != filepos) f2.seek(pos);
        len = f2.read(b,0,BUFFER_SIZE);
        filepos = pos + len;
        return b[0]+b[len>>1];
      }
    };
  }
}


class PooledPread extends MyFile {
  FileChannel[] all;
  int[] useCount;
  public PooledPread(RandomAccessFile f, String filename) throws Exception {
    super(f);
    all = new FileChannel[FileReadTest.poolsize];
    all[0]=f.getChannel();
    for (int i=1; i<all.length; i++) {
      all[i] = new RandomAccessFile(filename,"r").getChannel();
    }
    useCount = new int[all.length];
  }

  MyReader getReader() throws Exception {
    return new MyReader() {
      byte[] b = new byte[BUFFER_SIZE];
      ByteBuffer bb = ByteBuffer.wrap(b);

      public int read(long pos) throws IOException {
        bb.clear();

        FileChannel channel;
        int idx = 0;
        if (all.length > 1) {
          synchronized(PooledPread.this) {
            int minCount = useCount[0];
            for (int i=1; i<all.length; i++) {
              if (useCount[i] < minCount) {
                minCount = useCount[i];
                idx = i;
              }
            }
            useCount[idx]++;
          }
        }

        channel = all[idx];

        int len = channel.read(bb, pos);

        if (all.length > 1) {
          synchronized(PooledPread.this) {
            useCount[idx]--;
          }
        }

        return b[0]+b[len>>1];
      }
    };
  }
}


class ChannelFile extends MyFile {
  final FileChannel channel;
  long filepos=0;  // current position in the file
  
  public ChannelFile(RandomAccessFile f, String filename) {
    super(f);
    channel = f.getChannel();
  }

  MyReader getReader() {
    return new MyReader() {
      byte[] b = new byte[BUFFER_SIZE];
      ByteBuffer bb = ByteBuffer.wrap(b);

      public int read(long pos) throws IOException {
        bb.clear();
        int len;
        synchronized(channel) {
          if (filepos != pos) channel.position(pos);
          len = channel.read(bb);
          filepos = pos + len;
        }
        return b[0]+b[len>>1];
      }
    };
  }
}


class ChannelPread extends MyFile {
  final FileChannel channel;
  public ChannelPread(RandomAccessFile f, String filename) {
    super(f);
    channel = f.getChannel();
  }

  MyReader getReader() {
    return new MyReader() {
      byte[] b = new byte[BUFFER_SIZE];
      ByteBuffer bb = ByteBuffer.wrap(b);

      public int read(long pos) throws IOException {
        bb.clear();
        int len = channel.read(bb, pos);
        return b[0]+b[len>>1];
      }
    };
  }
}


class ChannelTransfer extends MyFile {
  final FileChannel channel;
  public ChannelTransfer(RandomAccessFile f, String filename) {
    super(f);
    channel = f.getChannel();
  }

  MyReader getReader() {
    return new MyReader() {
      final byte[] b = new byte[BUFFER_SIZE];
      int posInBuffer=0;
      final ByteBuffer bb = ByteBuffer.wrap(b);

      final WritableByteChannel sink = new WritableByteChannel() {
        public int write(ByteBuffer src) throws IOException {
          int remaining = src.remaining();
          src.get(b,posInBuffer,remaining);
          // may be called multiple times, so we need to keep track
          posInBuffer += remaining;
          return remaining;
        }
        public boolean isOpen() {return true;}
        public void close() throws IOException {}
      };

      public int read(long pos) throws IOException {
        posInBuffer=0;
        int len = (int)channel.transferTo(pos, BUFFER_SIZE, sink);
        return b[0]+b[len>>1];
      }
    };
  }
}
