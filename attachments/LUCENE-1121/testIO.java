import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;
import java.text.NumberFormat;

public class testIO {
  public static void main(String[] args) throws IOException {

    // Make a big file
    final int tenMBChunks = Integer.parseInt(args[0]);
    final long size = 10*1024*1024*tenMBChunks;
    long times[] = new long[2];

    for(int iter=0;iter<2;iter++) {

      System.out.print("  create " + (tenMBChunks*10) + " MB file...");
      long t0 = System.currentTimeMillis();
      RandomAccessFile out = new RandomAccessFile("big.bin", "rw");
      byte[] buffer = new byte[1024*1024];
      for(int i=0;i<tenMBChunks*10;i++)
        out.write(buffer, 0, 1024*1024);
      out.close();
      long t1 = System.currentTimeMillis();
      System.out.println(" " + (t1-t0) + " msec");

      new File("bigcopy.bin").delete();

      RandomAccessFile fileIn = new RandomAccessFile("big.bin", "r");
      RandomAccessFile fileOut = new RandomAccessFile("bigcopy.bin", "rw");

      if (0 == iter) {
        System.out.print("  transferTo...");
        t0 = System.currentTimeMillis();
        final long length = fileIn.length();
        final long tenMB = 10*1024*1024;
        long upto = 0;
        for(int j=0;j<tenMBChunks;j++) {
          final long count = fileIn.getChannel().transferTo(upto, tenMB, fileOut.getChannel());
          if (count != tenMB)
            throw new RuntimeException("failed to transfer full chunk: " + count);
          upto += tenMB;
        }
      } else {

        System.out.print("  buffer...");

        // buffer
        t0 = System.currentTimeMillis();
        buffer = new byte[65536];
        while(true) {
          int num = fileIn.read(buffer, 0, 65536);
          if (num == -1)
            break;
          fileOut.write(buffer, 0, num);
        }
      }
      t1 = System.currentTimeMillis();
      if (fileOut.length() != size)
        throw new RuntimeException("final size is wrong: " + fileOut.length());
      fileOut.close();
      fileIn.close();
      times[iter] = t1-t0;
      System.out.println(" " + (t1-t0) + " msec");
      new File("bigcopy.bin").delete();
      new File("big.bin").delete();
    }
    
    double pctDiff = (100.0*(times[0]-times[1]))/times[1];
    NumberFormat nf = NumberFormat.getInstance();

    if (times[0] < times[1])
      System.out.println("  FASTER " + nf.format(-pctDiff) + "%");
    else
      System.out.println("  SLOWER " + nf.format(pctDiff) + "%");
  }
}