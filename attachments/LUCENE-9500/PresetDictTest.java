import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class PresetDictTest {

  private static final int DICT_OFFSET = 491520;
  private static final int DICT_LENGTH = 8192;
  private static final int OFFSET = 499712;
  private static final int LENGTH = 49152;
  private static final byte[] DATA;
  static {
    try (InputStream fis = PresetDictTest.class.getResourceAsStream("/test_data.txt");
        InputStreamReader rd = new InputStreamReader(fis, "UTF-8");
        BufferedReader brd = new BufferedReader(rd)) {
      DATA = readArray(brd.readLine().trim());
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  private static byte[] readArray(String s) {
    String[] bytes = s.split(" ");
    byte[] b = new byte[bytes.length];
    for (int i = 0; i < b.length; ++i) {
      b[i] = Byte.valueOf(bytes[i]);
    }
    return b;
  }

  public static void main(String[] args) {
    Deflater deflater = new Deflater(6, true);
    byte[] data = Arrays.copyOf(DATA, DATA.length);
    byte[] compressed = new byte[16];
    try {
      deflater.reset();
      deflater.setDictionary(data, DICT_OFFSET, DICT_LENGTH);
      // Doing this instead makes the test pass (!)
      //deflater.setDictionary(Arrays.copyOfRange(data, DICT_OFFSET, DICT_OFFSET + DICT_LENGTH));
      deflater.setInput(data, OFFSET, LENGTH);
      deflater.finish();
      int compressedLength = 0;
      for (;;) {
        final int count = deflater.deflate(compressed, compressedLength, compressed.length - compressedLength, Deflater.FULL_FLUSH);
        compressedLength += count;
        if (deflater.finished()) {
          break;
        } else {
          compressed = Arrays.copyOf(compressed, compressed.length * 2);
        }
      }
      compressed = Arrays.copyOf(compressed, compressedLength);
    } finally {
      deflater.end();
    }
    System.out.println("Compressed to " + compressed.length + " bytes");

    Inflater inflater = new Inflater(true);
    byte[] restored = new byte[LENGTH];
    try {
      inflater.reset();
      inflater.setDictionary(data, DICT_OFFSET, DICT_LENGTH);
      inflater.setInput(compressed);
      final int restoredLength = inflater.inflate(restored);
      if (restoredLength != LENGTH) {
        throw new Error();
      }
    } catch (DataFormatException e) {
      throw new Error(e);
    } finally {
      inflater.end();
    }

    if (Arrays.equals(DATA, data) == false) {
      throw new Error("input array was modified");
    }

    byte[] rebased = Arrays.copyOfRange(data, OFFSET, OFFSET+LENGTH);
    if (Arrays.equals(rebased, restored) == false) {
      System.out.println("First mismatch byte " + Arrays.mismatch(data, OFFSET, OFFSET+LENGTH, restored, 0, restored.length));
      System.out.println("First original bytes " + Arrays.toString(Arrays.copyOf(rebased, 30)));
      System.out.println("First restored bytes " + Arrays.toString(Arrays.copyOf(restored, 30)));
      throw new Error();
    }
  }

}
