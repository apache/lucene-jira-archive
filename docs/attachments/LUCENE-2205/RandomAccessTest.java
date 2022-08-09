import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.FSDirectory;

public class RandomAccessTest {

	private static final String SMALL = "small";
	private static final String DEFAULT = "default";
	private static final String BREAK = "===========================================================";

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		String termFile = "./terms.dat";
		String indexFile = "./index";
		
		System.out.println("Loading terms to use in test...");
		//these are pre shuffled terms
		ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(termFile));
		List<String> terms = (List<String>) inputStream.readObject();
		inputStream.close();
		
		int pass = 10;
		Map<String,Long> times = new HashMap<String, Long>();
		times.put(DEFAULT, 0l);
		times.put(SMALL, 0l);
		
		boolean quiet = true;
		System.out.println("Warming and compiling everything...");
		run(DEFAULT,terms,indexFile,quiet,0);
		run(SMALL,terms,indexFile,quiet,0);
		long baseJvmOverheadPlusTerms = printMemoryStats(quiet);
		System.out.println("Go Time...");
		quiet = false;
		for (int i = 0; i < pass; i++) {
			times.put(DEFAULT, times.get(DEFAULT) + run(DEFAULT,terms,indexFile,quiet,baseJvmOverheadPlusTerms));
			baseJvmOverheadPlusTerms = printMemoryStats(quiet);
			times.put(SMALL, times.get(SMALL) + run(SMALL,terms,indexFile,quiet,baseJvmOverheadPlusTerms));
			printMemoryStats(quiet);
		}
		System.out.println(BREAK);
		for (String key : times.keySet()) {
			System.out.println("type [" + key +
					"] total time [" + times.get(key) / pass +
					"]");
		}
		
	}

	static long run(String type, List<String> terms, String indexFile, boolean quiet, long baseJvmOverheadPlusTerms) throws Exception {
		System.setProperty("org.apache.lucene.index.TermInfosReader", type);
		long s = System.currentTimeMillis();
		IndexReader reader = IndexReader.open(FSDirectory.open(new File(indexFile)));
		long e = System.currentTimeMillis();
		if (!quiet) {
			System.out.println(BREAK);
			System.out.println("Open time [" + (e-s) + "] [" + type + "]");
			System.out.println(BREAK);
			System.out.println("Starting the test...");
		}
		long total = 0;
		int pass = 10;
		for (int i = 0; i < pass; i++) {
			long runTest = runTest(i,terms,reader,quiet);
			long heapSizePlusLoadedIndex = printMemoryStats(quiet);
			if (!quiet) {
				System.out.println("Pass [" + i + "], Time [" + runTest + "] ms, Approx heap space used for index ["
						+ (heapSizePlusLoadedIndex - baseJvmOverheadPlusTerms) + "]");
			}
			total += runTest;
			Thread.sleep(10);
		}
		if (!quiet) {
			System.out.println(BREAK);
			System.out.println("Avg Time [" + (total/pass) + "] ms, for type [" + type + "]");
		}
		reader.close();
		return (total/pass);
	}
	
	private static long printMemoryStats(boolean quiet) {
		long s = System.currentTimeMillis();
		System.gc();
		System.gc();
		long e = System.currentTimeMillis();
		MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		long used = memoryMXBean.getHeapMemoryUsage().getUsed();
		if (!quiet) {
			System.out.println("Time to call 2 System.gc()'s [" + (e-s) + "], current heap used [" + used + "]");
		}
		return used;
	}

	static long runTest(int pass, List<String> terms, IndexReader reader, boolean quiet) throws Exception {
		long count = 0;
		int termSize = terms.size();
		int k = 0;
		long s = System.currentTimeMillis();
		Term term = new Term("body");
		for (int i = 0; i < 2500; i++) {
			Term t = term.createTerm(terms.get(k++));
			k++;
			TermEnum termEnum = reader.terms(t);
			count += termEnum.docFreq();
			termEnum.close();
			if (k >= termSize) {
				k = 0;
			}
		}
		long e = System.currentTimeMillis();
		if (!quiet) {
			System.out.println("\t\tTest Count [" + count + "]");
		}
		return (e-s);
	}

}
