package org.apache.lucene.util.pfor;
import org.apache.lucene.store.*;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.IndexReader;

import java.io.IOException;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Random;
import java.text.NumberFormat;

public class TestPFor2 extends LuceneTestCase {

  private static final int BLOCK_SIZE = 128;

  public static void main(String[] args) throws Throwable {
    Directory dir = FSDirectory.getDirectory(args[0]);

    if (args.length != 2) {
      System.out.println("\nUsage: java org.apache.lucene.util.TestPFor2 <indexDirName> <vIntFileNameIn>\n");
      System.out.println("Eg: java org.apache.lucene.util.TestPFor2 /lucene/index _l.prx\n");
      System.exit(1);
    }

    String vIntFileNameIn = args[1];
    String vIntFileNameOut = vIntFileNameIn + ".vint";
    String pForFileNameOut = vIntFileNameIn + ".pfor";

    if (vIntFileNameIn.endsWith(".frq") && !vIntFileNameIn.endsWith(".frq.frq") && !dir.fileExists(vIntFileNameIn + ".frq"))
      splitFreq(dir, vIntFileNameIn);

    // Convert vInt encoding --> pfor
    if (!dir.fileExists(pForFileNameOut) || !dir.fileExists(vIntFileNameOut)) {
      System.out.println("\nencode " + vIntFileNameIn + " to " + pForFileNameOut + " and " + vIntFileNameOut + "...");
      convertVIntToPFor(dir, vIntFileNameIn, pForFileNameOut);
    }

    final int numRound = DEBUG ? 1:5;

    System.out.println("\ndecompress " + pForFileNameOut + " using pfor:");
    long bestPFor = 0;
    for(int round=0;round<numRound;round++) {
      long speed = readPFor(dir, pForFileNameOut);
      if (speed > bestPFor)
        bestPFor = speed;
    }

    System.out.println("\ndecompress " + vIntFileNameOut + " using readVInt:");
    long bestVInt = 0;
    for(int round=0;round<numRound;round++) {
      long speed = readVInts(dir, vIntFileNameOut);
      if (speed > bestVInt)
        bestVInt = speed;
    }

    NumberFormat nf = NumberFormat.getInstance();
    if (bestVInt > bestPFor)
      System.out.println("\nPFor is " + nf.format((bestVInt-bestPFor)*100.0/bestVInt) + "% slower");
    else
      System.out.println("\nPFor is " + nf.format((bestPFor-bestVInt)*100.0/bestVInt) + "% faster");

    dir.close();
  }

  public static void splitFreq(Directory dir, String fileNameIn) throws Throwable {
    System.out.println("Splitting out frq & doc from " + fileNameIn + "...");
    IndexReader r = IndexReader.open(dir);
    TermEnum te = r.terms();
    TermDocs td = r.termDocs();
    IndexOutput freqOut = dir.createOutput(fileNameIn + ".frq");
    IndexOutput docsOut = dir.createOutput(fileNameIn + ".doc");

    while(te.next()) {
      td.seek(te);
      int lastDoc = 0;
      while(td.next()) {
        freqOut.writeVInt(td.freq());
        int doc = td.doc();
        docsOut.writeVInt(doc-lastDoc);
        lastDoc = doc;
      }
    }
    freqOut.close();
    docsOut.close();
    System.out.println("  doc file is " + dir.fileLength(fileNameIn + ".doc") + " bytes");
    System.out.println("  frq file is " + dir.fileLength(fileNameIn + ".frq") + " bytes");
  }

  private static boolean DEBUG = false;

  private static long checksum;
  private static long counter;

  private static boolean checksum(int v) {
    checksum += v;
    if (DEBUG && counter++<1000)
      System.out.println("v=" + v + " cs=" + checksum);
    return true;
  }

  private static boolean checksum(int[] v) {
    for(int i=0;i<v.length;i++) {
      checksum += v[i];
      if (DEBUG && counter++<1000)
        System.out.println("v=" + v[i] + " cs=" + checksum);
    }
    return true;
  }

  private static boolean printChecksum() {
    System.out.println("SUM: " + checksum);
    return true;
  }

  /** Returns ints/sec speed */
  public static long readVInts(Directory dir, String vIntFileNameIn) throws Throwable {
    checksum = 0;
    counter = 0;
    IndexInput in = dir.openInput(vIntFileNameIn);
    final long t0 = System.currentTimeMillis();
    long count = 0;
    while(true) {
      try {
        int b = in.readVInt();
        assert checksum(b);
        count++;
      } catch (IOException ioe) {
        break;
      }
    }
    final long t1 = System.currentTimeMillis();
    in.close();
    System.out.println((t1-t0) + " msec to read " + count + " ints (" + (count/(t1-t0)) + " ints/msec)");
    assert printChecksum();

    return count/(t1-t0);
  }

  /** Returns ints/sec speed */
  public static long readPFor(Directory dir, String pForFileNameOut) throws Throwable {
    IndexInput in = dir.openInput(pForFileNameOut);

    PFor pforDecompress = new PFor();
    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    byte[] bufferByteArray = byteBuffer.array();
    IntBuffer intBuffer = byteBuffer.asIntBuffer(); // no offsets used here.
    pforDecompress.setBuffer(intBuffer);
    final int[] temp = new int[BLOCK_SIZE];

    // Read first one: make sure BLOCK_SIZE matches
    in.readBytes(bufferByteArray, 0, in.readInt());
    if (BLOCK_SIZE != pforDecompress.getIntSize())
      throw new RuntimeException("Please remove pfor file " + pForFileNameOut + " and re-run: BLOCK_SIZE changed");
    in.seek(0);

    checksum = 0;
    counter = 0;

    final long t0 = System.currentTimeMillis();
    long count = 0;
    while(true) {
      try {
        in.readBytes(bufferByteArray, 0, in.readInt());
        pforDecompress.decompress(temp, 0);
        assert checksum(temp);
        count++;
      } catch (IOException ioe) {
        break;
      }
    }
    final long t1 = System.currentTimeMillis();
    System.out.println((t1-t0) + " msec to decode " + (BLOCK_SIZE*count) + " ints (" + (BLOCK_SIZE*count/(t1-t0)) + " ints/msec)");
    in.close();
    assert printChecksum();

    return (BLOCK_SIZE*count)/(t1-t0);
  }

  public static void convertVIntToPFor(Directory dir, String vIntFileNameIn, String pForFileNameOut) throws Throwable {
    IndexInput in = dir.openInput(vIntFileNameIn);
    IndexOutput out = dir.createOutput(pForFileNameOut);
    String vIntFileNameOut = vIntFileNameIn + ".vint";
    IndexOutput vIntOut = dir.createOutput(vIntFileNameOut);

    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    byte[] bufferByteArray = byteBuffer.array();
    IntBuffer intBuffer = byteBuffer.asIntBuffer(); // no offsets used here.

    PFor pforCompress = new PFor();
    pforCompress.setBuffer(intBuffer);

    // Get ints
    int count = 0;
    int upto = 0;
    int[] temp = new int[BLOCK_SIZE];

    final Random r = new Random();
    final int[] counts = new int[32];

    while(true) {
      try {
        temp[upto++] = in.readVInt();
      } catch (IOException ioe) {
        break;
      }

      if (upto == BLOCK_SIZE) {

        // Seems to be necessary?:
        Arrays.fill(bufferByteArray, (byte) 0);

        final int numFrameBits = PFor.getNumFrameBits(temp, 0, BLOCK_SIZE);   
        counts[numFrameBits]++;
        pforCompress.compress(temp, 0, BLOCK_SIZE, numFrameBits);
        final int numByte = pforCompress.bufferByteSize();
        out.writeInt(numByte);
        out.writeBytes(bufferByteArray, 0, numByte);
        upto = 0;
        count++;
        for(int i=0;i<BLOCK_SIZE;i++)
          vIntOut.writeVInt(temp[i]);
      }
    }
    in.close();
    out.close();
    vIntOut.close();
    
    long origSize = dir.fileLength(vIntFileNameIn);
    long newSize = dir.fileLength(pForFileNameOut);
    String desc;
    double delta = newSize - origSize;
    NumberFormat nf = NumberFormat.getInstance();
    if (delta < 0)
      desc = nf.format(100.0*-delta/origSize) + "% smaller";
    else
      desc = nf.format(100.0*-delta/origSize) + "% bigger";

    System.out.println((BLOCK_SIZE*count) + " ints; " + newSize + " bytes compressed vs orig size " + origSize + " (" + desc + ")");
 
    for(int i=1;i<31;i++)
      System.out.println(i + " bits: " + counts[i] + " [" + nf.format(100.0*counts[i]/count) + " %]");
  }
}