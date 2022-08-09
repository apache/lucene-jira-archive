package org.newtecnia.index;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

/**
 * 
 */
public class App {

	private static Directory indexDir;
	static File baseDir;
	static IndexReader reader;
	static IndexSearcher searcher;

	public static void main(String[] args) throws IOException,
			InterruptedException {

		setup();

		System.out.println("Sleeping 10 seconds");
		Thread.sleep(10000L);
		System.out.println("Start");

		int users = 500;
		ExecutorService service = Executors.newCachedThreadPool();

		for (int i = 0; i < users; i++) {
			service.submit(new Runnable() {
				public void run() {
					try {

						final Random rand = new Random();
						long max = 0, min = Long.MAX_VALUE, avg = 0;

						int loopCounter = 10000;
						final CountDownLatch latch = new CountDownLatch(
								loopCounter);
						
						int number = rand.nextInt(1000);
						
						Query q = new QueryParser(Version.LUCENE_34, "",
								new WhitespaceAnalyzer(Version.LUCENE_34))
								.parse("name:name_" + number);

						for (int i = 0; i < loopCounter; i++) {
							

							final long start = System.currentTimeMillis();
							searcher.search(q, 10);

							long t = System.currentTimeMillis() - start;
							max = Math.max(t, max);
							min = Math.min(min, t);
							avg += t;

						}

						System.out.println(max + "," + min + ","
								+ (avg / loopCounter));

					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			});
		}

		service.awaitTermination(10, TimeUnit.MINUTES);
	}

	public static void setup() throws IOException {

		baseDir = new File("target/test/AppTest/setup/index");
		if (baseDir.exists())
			baseDir.delete();

		baseDir.mkdirs();

		indexDir = new RAMDirectory(new NIOFSDirectory(baseDir));

		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_34,
				new WhitespaceAnalyzer(Version.LUCENE_34));

		IndexWriter writer = new IndexWriter(indexDir, config);
		for (int i = 0; i < 1000; i++) {
			Document doc = new Document();
			doc.add(new Field("name", "name_" + i, Store.YES, Index.ANALYZED));
			doc.add(new Field("age", "age_" + i, Store.YES, Index.ANALYZED));
			doc.add(new Field("car", "car_" + i, Store.YES, Index.ANALYZED));
			doc.add(new Field("loc", "loc_" + i, Store.YES, Index.ANALYZED));
			doc.add(new Field("ip", "ip_" + i, Store.YES, Index.ANALYZED));
			doc.add(new Field("add", "add_" + i, Store.YES, Index.ANALYZED));

			writer.addDocument(doc);
		}

		writer.close(true);

		reader = IndexReader.open(indexDir, true);
		searcher = new IndexSearcher(indexDir, true);

	}

}
