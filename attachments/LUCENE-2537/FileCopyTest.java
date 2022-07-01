import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class FileCopyTest {

	public static long parseSize(String value) {
		int K = 1024;
		int M = K * K;
		int G = M * K;

		String subVal = value.substring(0, value.length() - 1);
		if (value.endsWith("K")) {
			return Integer.parseInt(subVal) * K;
		} else if (value.endsWith("M")) {
			return Integer.parseInt(subVal) * M;
		} else if (value.endsWith("G")) {
			return Long.parseLong(subVal) * G;
		}
		
		return Long.parseLong(value);
	}
	
	public static void createFile(File f, long size, int ioChunkSize) throws IOException {
		// Use a random # generator w/ same seed
		Random rand = new Random(1);
		byte[] buf = new byte[ioChunkSize];
		int numWritten = 0;
		FileOutputStream fos = new FileOutputStream(f);
		while (numWritten < size) {
			rand.nextBytes(buf);
			fos.write(buf, 0, (int) Math.min(buf.length, size - numWritten));
			numWritten += buf.length;
		}
		fos.close();
	}
	
	public static long copyFileChannel(File src, int ioChunkSize) throws IOException {
		FileInputStream fis = new FileInputStream(src);
		FileOutputStream fos = new FileOutputStream(src.getAbsolutePath() + "_channel");
		FileChannel in = fis.getChannel();
		FileChannel out = fos.getChannel();
		
		long numWritten = 0;
		long numToWrite = src.length();
		long time = System.nanoTime();
		while (numWritten < numToWrite) {
		  numWritten += out.transferFrom(in, numWritten, ioChunkSize);
		}
		time = System.nanoTime() - time;
		
		fis.close();
		fos.close();
		return time;
	}

	public static long copyIntermidBuffer(File src, int ioChunkSize) throws IOException {
		FileInputStream fis = new FileInputStream(src);
		FileOutputStream fos = new FileOutputStream(src.getAbsolutePath() + "_buffer");

		byte[] buf = new byte[ioChunkSize];
		long numWritten = 0;
		long numToWrite = src.length();
		long time = System.nanoTime();
		while (numWritten < numToWrite) {
			int numRead = fis.read(buf);
			fos.write(buf, 0, numRead);
			numWritten += numRead;
		}
		time = System.nanoTime() - time;
		fis.close();
		fos.close();
		return time;
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			System.out.println("Usage: java " + FileCopyTest.class.getName() + " <file-to-create> <size> <ioChunkSize>");
			System.out.println("\tsize/ioChunkSize either an absolute value, or ends with K/M/G for KB/MB/GB respectively");
			System.out.println("\tNOTE: if the file exists and its size is not the same as the specified size, the file will be recreated");
			return;
		}
		
		File f = new File(args[0]).getAbsoluteFile();
		long size = parseSize(args[1]);
		int ioChunkSize = (int) parseSize(args[2]);

		boolean create = !f.exists();
		// If the file exists, however of a different size, recreate it.
		if (f.exists() && f.length() != size) {
			create = true;
		}

		if (create) {
			System.out.println("creating " + f + "; size=" + args[1]);
			createFile(f, size, ioChunkSize);
		}

		int numIterations = 3;
		
		System.out.println("copying file using FileChannel API");
		long bestChannelTime = Long.MAX_VALUE;
		for (int i = 0; i < numIterations; i++) {
			long t = copyFileChannel(f, ioChunkSize);
			if (t < bestChannelTime) {
				bestChannelTime = t;
			}
		}
		System.out.println("Copy time: " + TimeUnit.NANOSECONDS.toMillis(bestChannelTime) + " ms");

		System.out.println("copying file using Intermediate byte[] buffer");
		long bestBufferTime = Long.MAX_VALUE;
		for (int i = 0; i < numIterations; i++) {
			long t = copyIntermidBuffer(f, ioChunkSize);
			if (t < bestBufferTime) {
				bestBufferTime = t;
			}
		}
		System.out.println("Copy time: " + TimeUnit.NANOSECONDS.toMillis(bestBufferTime) + " ms");
	}

}
