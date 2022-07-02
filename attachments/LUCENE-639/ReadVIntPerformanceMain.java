package apache.lucene.performance;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

// Use the following VM parameters: -Xmx512m
public class ReadVIntPerformanceMain
{
  // Use at most 192 MB for the data
  private static final int NUMBER_OF_INTEGERS_TO_GENERATE = 192 * 1000 * 1000 / 5;

  private static final int MAXIMUM_SIZE_OF_DATA = NUMBER_OF_INTEGERS_TO_GENERATE * 5;

  private static final int NUMBER_OF_LOOPS = 3;

  private static final int NUMBER_OF_VARIANT_LOOPS = 5;

  private static final int ONE_BYTE_EXCLUSIVE_UPPER_LIMIT = 128;

  private static final int TWO_BYTE_EXCLUSIVE_UPPER_LIMIT = 16384;

  private static final int THREE_BYTE_EXCLUSIVE_UPPER_LIMIT = 2097152;

  private static final int FOUR_BYTE_EXCLUSIVE_UPPER_LIMIT = 268435456;

  private static final String JVM_INFO = "" + System.getProperty("java.vm.vendor") + " "
        + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + " "
        + System.getProperty("java.vm.info");

  private byte[] data;

  private String dataId;

  private int range;

  private int offset;

  private InputStream inputStream;

  private ReadVIntPerformanceMain(byte[] d, String di, int mv, int o)
  {
    super();

    data = d;
    dataId = di;
    offset = o;
    range = mv - o;
  }

  private void run() throws Exception
  {
    runVariantLoops(0);

    for (int i = 0; i < NUMBER_OF_LOOPS; i += 1)
    {
      runVariantLoops(i + 1);
    }
  }

  private void runVariantLoops(int loop) throws IOException
  {
    long start = System.currentTimeMillis();
    for (int i = 0; i < NUMBER_OF_VARIANT_LOOPS; i += 1)
    {
      Random r = new Random(0);
      inputStream = new ByteArrayInputStream(data);
      runLuceneCurrent(r);
      inputStream.close();
    }
    long end = System.currentTimeMillis() - start;
    if (loop > 0)
    {
      System.out.println(JVM_INFO + '\t' + dataId + '\t' + loop + "\tlucenecurrent\t" + end);
    }

    start = System.currentTimeMillis();
    for (int i = 0; i < NUMBER_OF_VARIANT_LOOPS; i += 1)
    {
      Random r = new Random(0);
      inputStream = new ByteArrayInputStream(data);
      runUnrolled(r);
      inputStream.close();
    }
    end = System.currentTimeMillis() - start;
    if (loop > 0)
    {
      System.out.println(JVM_INFO + '\t' + dataId + '\t' + loop + "\tunrolled\t" + end);
    }

    start = System.currentTimeMillis();
    for (int i = 0; i < NUMBER_OF_VARIANT_LOOPS; i += 1)
    {
      Random r = new Random(0);
      inputStream = new ByteArrayInputStream(data);
      runXor(r);
      inputStream.close();
    }
    end = System.currentTimeMillis() - start;
    if (loop > 0)
    {
      System.out.println(JVM_INFO + '\t' + dataId + '\t' + loop + "\txor\t" + end);
    }

    start = System.currentTimeMillis();
    for (int i = 0; i < NUMBER_OF_VARIANT_LOOPS; i += 1)
    {
      Random r = new Random(0);
      inputStream = new ByteArrayInputStream(data);
      runCombineOnExit(r);
      inputStream.close();
    }
    end = System.currentTimeMillis() - start;
    if (loop > 0)
    {
      System.out.println(JVM_INFO + '\t' + dataId + '\t' + loop + "\tcombineonexit\t" + end);
    }
  }

  private void runLuceneCurrent(Random r) throws IOException
  {
    for (int i = 0; i < NUMBER_OF_INTEGERS_TO_GENERATE; i += 1)
    {
      int j = readVIntLuceneCurrent();
      int k = r.nextInt(range) + offset;
      if (j != k)
      {
        throw new IllegalStateException("Expected: " + k + ", returned: " + j);
      }
    }
  }

  private void runUnrolled(Random r) throws IOException
  {
    for (int i = 0; i < NUMBER_OF_INTEGERS_TO_GENERATE; i += 1)
    {
      int j = readVIntUnrolled();
      int k = r.nextInt(range) + offset;
      if (j != k)
      {
        throw new IllegalStateException("Expected: " + k + ", returned: " + j);
      }
    }
  }

  private void runXor(Random r) throws IOException
  {
    for (int i = 0; i < NUMBER_OF_INTEGERS_TO_GENERATE; i += 1)
    {
      int j = readVIntXOr();
      int k = r.nextInt(range) + offset;
      if (j != k)
      {
        throw new IllegalStateException("Expected: " + k + ", returned: " + j);
      }
    }
  }

  private void runCombineOnExit(Random r) throws IOException
  {
    for (int i = 0; i < NUMBER_OF_INTEGERS_TO_GENERATE; i += 1)
    {
      int j = readVIntCombineOnExit();
      int k = r.nextInt(range) + offset;
      if (j != k)
      {
        throw new IllegalStateException("Expected: " + k + ", returned: " + j);
      }
    }
  }

  public int readVIntLuceneCurrent() throws IOException
  {
    byte b = readByte();
    int i = b & 0x7F;
    for (int shift = 7; (b & 0x80) != 0; shift += 7)
    {
      b = readByte();
      i |= (b & 0x7F) << shift;
    }
    return i;
  }

  public int readVIntUnrolled() throws IOException
  {
    byte b = readByte();
    int i = b & 0x7F;
    if (b < 0)
    {
      b = readByte();
      i |= (b & 0x7F) << 7;
      if (b < 0)
      {
        b = readByte();
        i |= (b & 0x7F) << 14;
        if (b < 0)
        {
          b = readByte();
          i |= (b & 0x7F) << 21;
          if (b < 0)
          {
            b = readByte();
            i |= (b & 0x7F) << 28;
          }
        }
      }
    }
    return i;
  }

  public int readVIntXOr() throws IOException
  {
    int i = readByte();
    if (i < 0)
    {
      // Second byte present
      i ^= readByte() << 7;
      if (i >= 0)
      {
        // Third byte present
        i ^= readByte() << 14;
        if (i < 0)
        {
          // Fourth byte present
          i ^= readByte() << 21;
          if (i >= 0)
          {
            // Fifth byte present
            i ^= readByte() << 28;
            return i ^ 0x0FE03F80;
          }
          // No fifth byte
          return i ^ 0xFFE03F80;
        }
        // No fourth byte
        return i ^ 0x00003F80;
      }
      // No third byte
      return i ^ 0xFFFFFF80;
    }
    return i;
  }

  public int readVIntCombineOnExit() throws IOException
  {
    byte b = readByte();
    if ((b & 0x80) == 0)
      return b;
    b &= 0x7f;
    byte b2 = readByte();
    if ((b2 & 0x80) == 0)
      return (b2 << 7) | b;
    b2 &= 0x7f;
    byte b3 = readByte();
    if ((b3 & 0x80) == 0)
      return (b3 << 14) | (b2 << 7) | b;
    b3 &= 0x7f;
    byte b4 = readByte();
    if ((b4 & 0x80) == 0)
      return (b4 << 21) | (b3 << 14) | (b2 << 7) | b;
    b4 &= 0x7f;
    byte b5 = readByte();
    return (b5 << 28) | (b4 << 21) | (b3 << 14) | (b2 << 7) | b;
  }

  private byte readByte() throws IOException
  {
    return (byte)inputStream.read();
  }

  public static void main(String[] args) throws Exception
  {
    byte[] data = generateRandomSizeData();
    new ReadVIntPerformanceMain(data, "randomsize", Integer.MAX_VALUE, 0).run();
    // Allow the garbage collector to release the current data while new data is
    // being generated
    data = null;

    data = generateOneByteData();
    new ReadVIntPerformanceMain(data, "onebyte", ONE_BYTE_EXCLUSIVE_UPPER_LIMIT, 0).run();
    data = null;

    data = generateTwoByteData();
    new ReadVIntPerformanceMain(data, "twobyte", TWO_BYTE_EXCLUSIVE_UPPER_LIMIT, ONE_BYTE_EXCLUSIVE_UPPER_LIMIT).run();
    data = null;

    data = generateThreeByteData();
    new ReadVIntPerformanceMain(data, "threebyte", THREE_BYTE_EXCLUSIVE_UPPER_LIMIT, TWO_BYTE_EXCLUSIVE_UPPER_LIMIT)
          .run();
    data = null;

    data = generateFourByteData();
    new ReadVIntPerformanceMain(data, "fourbyte", FOUR_BYTE_EXCLUSIVE_UPPER_LIMIT, THREE_BYTE_EXCLUSIVE_UPPER_LIMIT)
          .run();
    data = null;

    data = generateFiveByteData();
    new ReadVIntPerformanceMain(data, "fivebyte", Integer.MAX_VALUE, FOUR_BYTE_EXCLUSIVE_UPPER_LIMIT).run();
  }

  private static byte[] generateRandomSizeData() throws IOException
  {
    Random r = new Random(0);
    ByteArrayOutputStream bos = new ByteArrayOutputStream(MAXIMUM_SIZE_OF_DATA);
    for (int i = 0; i < NUMBER_OF_INTEGERS_TO_GENERATE; i += 1)
    {
      int j = r.nextInt(Integer.MAX_VALUE);
      writeVInt(j, bos);
    }
    bos.close();

    return bos.toByteArray();
  }

  private static byte[] generateOneByteData() throws IOException
  {
    Random r = new Random(0);
    ByteArrayOutputStream bos = new ByteArrayOutputStream(MAXIMUM_SIZE_OF_DATA);
    for (int i = 0; i < NUMBER_OF_INTEGERS_TO_GENERATE; i += 1)
    {
      int j = r.nextInt(ONE_BYTE_EXCLUSIVE_UPPER_LIMIT);
      writeVInt(j, bos);
    }
    bos.close();

    byte[] result = bos.toByteArray();
    if (result.length != NUMBER_OF_INTEGERS_TO_GENERATE)
    {
      throw new IllegalStateException("Array size does not match expected size");
    }
    return result;
  }

  private static byte[] generateTwoByteData() throws IOException
  {
    Random r = new Random(0);
    ByteArrayOutputStream bos = new ByteArrayOutputStream(MAXIMUM_SIZE_OF_DATA);
    for (int i = 0; i < NUMBER_OF_INTEGERS_TO_GENERATE; i += 1)
    {
      int j = r.nextInt(TWO_BYTE_EXCLUSIVE_UPPER_LIMIT - ONE_BYTE_EXCLUSIVE_UPPER_LIMIT);
      writeVInt(ONE_BYTE_EXCLUSIVE_UPPER_LIMIT + j, bos);
    }
    bos.close();

    byte[] result = bos.toByteArray();
    if (result.length != NUMBER_OF_INTEGERS_TO_GENERATE * 2)
    {
      throw new IllegalStateException("Array size does not match expected size");
    }
    return result;
  }

  private static byte[] generateThreeByteData() throws IOException
  {
    Random r = new Random(0);
    ByteArrayOutputStream bos = new ByteArrayOutputStream(MAXIMUM_SIZE_OF_DATA);
    for (int i = 0; i < NUMBER_OF_INTEGERS_TO_GENERATE; i += 1)
    {
      int j = r.nextInt(THREE_BYTE_EXCLUSIVE_UPPER_LIMIT - TWO_BYTE_EXCLUSIVE_UPPER_LIMIT);
      writeVInt(TWO_BYTE_EXCLUSIVE_UPPER_LIMIT + j, bos);
    }
    bos.close();

    byte[] result = bos.toByteArray();
    if (result.length != NUMBER_OF_INTEGERS_TO_GENERATE * 3)
    {
      throw new IllegalStateException("Array size does not match expected size");
    }
    return result;
  }

  private static byte[] generateFourByteData() throws IOException
  {
    Random r = new Random(0);
    ByteArrayOutputStream bos = new ByteArrayOutputStream(MAXIMUM_SIZE_OF_DATA);
    for (int i = 0; i < NUMBER_OF_INTEGERS_TO_GENERATE; i += 1)
    {
      int j = r.nextInt(FOUR_BYTE_EXCLUSIVE_UPPER_LIMIT - THREE_BYTE_EXCLUSIVE_UPPER_LIMIT);
      writeVInt(THREE_BYTE_EXCLUSIVE_UPPER_LIMIT + j, bos);
    }
    bos.close();

    byte[] result = bos.toByteArray();
    if (result.length != NUMBER_OF_INTEGERS_TO_GENERATE * 4)
    {
      throw new IllegalStateException("Array size does not match expected size");
    }
    return result;
  }

  private static byte[] generateFiveByteData() throws IOException
  {
    Random r = new Random(0);
    ByteArrayOutputStream bos = new ByteArrayOutputStream(MAXIMUM_SIZE_OF_DATA);
    for (int i = 0; i < NUMBER_OF_INTEGERS_TO_GENERATE; i += 1)
    {
      int j = r.nextInt(Integer.MAX_VALUE - FOUR_BYTE_EXCLUSIVE_UPPER_LIMIT);
      writeVInt(FOUR_BYTE_EXCLUSIVE_UPPER_LIMIT + j, bos);
    }
    bos.close();

    byte[] result = bos.toByteArray();
    if (result.length != NUMBER_OF_INTEGERS_TO_GENERATE * 5)
    {
      throw new IllegalStateException("Array size does not match expected size");
    }
    return result;
  }

  private static void writeVInt(int i, OutputStream os) throws IOException
  {
    while ((i & ~0x7F) != 0)
    {
      os.write((i & 0x7f) | 0x80);
      i >>>= 7;
    }
    os.write(i);
  }
}
