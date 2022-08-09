import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BreakIt {
	public static final String LOCKFILE = "lock";
	public static final boolean DELETE = false;

	public static void main(String[] args) throws Exception {
		ExecutorService service = Executors.newCachedThreadPool();

		List<Future<?>> futures = new ArrayList<Future<?>>();

		final CyclicBarrier barrier1 = new CyclicBarrier(3);
		final CyclicBarrier barrier2 = new CyclicBarrier(3);
		final CyclicBarrier barrier3 = new CyclicBarrier(3);

		{
			File path = new File(LOCKFILE);
			path.delete();
		}

		futures.add(service.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				File path = new File(LOCKFILE);
				RandomAccessFile file = new RandomAccessFile(path, "rw");
				FileChannel channel = file.getChannel();
				FileLock lock = channel.lock();
				System.out.println("1 held");
				lock.release();
				System.out.println("1 released");
				channel.close();
				file.close();

				barrier1.await();

				if (DELETE)
					path.delete();
				System.out.println("Path deleted");

				barrier2.await();
				barrier3.await();
				return null;
			}
		}));

		futures.add(service.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				File path = new File(LOCKFILE);
				RandomAccessFile file = new RandomAccessFile(path, "rw");
				FileChannel channel = file.getChannel();
				barrier1.await();
				FileLock lock = channel.lock();
				System.out.println("2 held");
				barrier2.await();
				barrier3.await();
				lock.release();
				System.out.println("2 released");
				channel.close();
				file.close();
				if (DELETE)
					path.delete();
				return null;
			}
		}));

		futures.add(service.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				barrier1.await();
				barrier2.await();
				File path = new File(LOCKFILE);
				RandomAccessFile file = new RandomAccessFile(path, "rw");
				FileChannel channel = file.getChannel();
				FileLock lock = channel.lock();
				System.out.println("3 held");
				lock.release();
				System.out.println("3 released");
				channel.close();
				file.close();
				if (DELETE)
					path.delete();
				barrier3.await();
				return null;
			}
		}));

		for (Future<?> future : futures) {
			future.get();
		}
		service.shutdown();
		service.awaitTermination(1, TimeUnit.MINUTES);
	}
}