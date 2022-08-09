/*
 * Created on Dec 2, 2004
 * Author: Andrzej Bialecki &lt;ab@getopt.org&gt;
 *
 */
package org.getopt.lb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.tar.TarEntry;
import org.apache.commons.compress.tar.TarInputStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
 */
public class LuceneBenchmark {
  public static Boolean[] bools = new Boolean[] { Boolean.FALSE, Boolean.TRUE };

  public static String[] queries = new String[] {
          "file:src",
          "body:article",
          "body:article subject:re",
          "file:comp body:article* subject:re"
  };
  
  /** Number of iterations for each operation. */
  public static int RUN_COUNT = 5;

  /** Artificially scale up the number of source documents by this factor. */
  public static int SCALE_UP = 5;

  public static int LOG_STEP = 1000;

  private static final String SETUP_DONE = ".setup_done";

  private URL[] sources = null;

  private File workDir = null;

  private File indexDir = null;

  private String srcUrl = null;

  private File sourceDir = null;

  private File jumboDir = null;

  private File setup_done = null;

  /**
   * Default constructor - initialize the a list of URLs with source corpora.
   *
   */
  public LuceneBenchmark() {
    try {
      sources = new URL[] { new URL("http://www-2.cs.cmu.edu/afs/cs.cmu.edu/project/theo-20/www/data/news20.tar.gz"),
          new URL("http://people.csail.mit.edu/u/j/jrennie/public_html/20Newsgroups/20news-18828.tar.gz"),
          new URL("http://kdd.ics.uci.edu/databases/20newsgroups/mini_newsgroups.tar.gz") };
    } catch (Exception e) {}
    ;
  }
  
  public void setWorkDir(File wd) throws Exception {
    if (setup_done != null) throw new Exception("Too late - setup already done.");
    workDir = wd;
  }

  private byte[] buf = new byte[4096];

  /**
   * Download a source file from URL.
   * @return local file, or null if failed
   * @throws Exception
   */
  protected File getSourceFile() throws Exception {
    for (int i = 0; i < sources.length; i++) {
      File out = new File(workDir, new File(sources[i].getPath()).getName());
      try {
        URLConnection con = sources[i].openConnection();
        int length = con.getContentLength();
        System.err.println(" - downloading " + length / 1024 + " kB: ");
        InputStream is = con.getInputStream();
        if (is == null) continue;
        saveStream(is, out, true);
        System.err.println("* Download OK.");
        return out;
      } catch (Exception e) {
        e.printStackTrace();
        out.delete();
        continue;
      }
    }
    return null;
  }

  /**
   * Save a stream to a file.
   * @param is input stream
   * @param out output file
   * @param closeInput if true, close the input stream when done.
   * @throws Exception
   */
  private void saveStream(InputStream is, File out, boolean closeInput) throws Exception {
    byte[] buf = new byte[4096];
    FileOutputStream fos = new FileOutputStream(out);
    int len = 0;
    long total = 0L;
    long time = System.currentTimeMillis();
    long delta = time;
    while ((len = is.read(buf)) > 0) {
      fos.write(buf, 0, len);
      total += len;
      time = System.currentTimeMillis();
      if (time - delta > 5000) {
        System.err.println(" - copied " + total / 1024 + " kB...");
        delta = time;
      }
    }
    fos.flush();
    fos.close();
    if (closeInput) is.close();
  }

  /**
   * Delete files and directories, even if non-empty.
   * @param dir file or directory
   * @return true on success, false if no or part of files have been deleted
   * @throws IOException
   */
  private static boolean fullyDelete(File dir) throws IOException {
    if (dir == null || !dir.exists()) return false;
    File contents[] = dir.listFiles();
    if (contents != null) {
      for (int i = 0; i < contents.length; i++) {
        if (contents[i].isFile()) {
          if (!contents[i].delete()) {
            return false;
          }
        } else {
          if (!fullyDelete(contents[i])) {
            return false;
          }
        }
      }
    }
    return dir.delete();
  }

  /**
   * Make sure the sources are downloaded and unpacked, remove old indexes.
   * Prepare a set of large documents.
   * 
   * @throws Exception
   */
  public void setup() throws Exception {
    if (workDir == null) {
      workDir = File.createTempFile(".lucene_benchmark", "");
      workDir.delete();
    }
    System.err.println(" - setup in " + workDir.getCanonicalPath());
    workDir.mkdirs();
    if (!workDir.exists()) throw new Exception("Unable to create workDir " + workDir);
    // reuse old setup
    setup_done = new File(workDir, ".setup_done");
    indexDir = new File(workDir, "index");
    sourceDir = new File(workDir, "src");
    jumboDir = new File(new File(workDir, "jumbo"), "jumbo");
    reset();
    if (setup_done.exists()) return;
    File src = null;
    // check if one of the sources is downloaded
    for (int i = 0; i < sources.length; i++) {
      File f = new File(workDir, new File(sources[i].getPath()).getName());
      if (f.exists()) {
        src = f;
        break;
      }
    }
    if (src == null) src = getSourceFile();
    fullyDelete(sourceDir);
    sourceDir.mkdirs();
    System.err.println("* Unpacking reference collection: " + src.getName());
    TarInputStream tis = new TarInputStream(new GZIPInputStream(new FileInputStream(src)));
    TarEntry te = null;
    int dircnt = 0, fcnt = 0;
    while ((te = tis.getNextEntry()) != null) {
      File out = new File(sourceDir, te.getName());
      if (te.isDirectory()) {
        out.mkdirs();
        System.err.println(" - " + te.getName());
        dircnt++;
        continue;
      }
      tis.copyEntryContents(new FileOutputStream(out));
      fcnt++;
    }
    System.err.println(" - " + fcnt + " source files in " + dircnt + " directories.");
    System.err.println("* Creating jumbo files...");
    fcnt = 0;
    dircnt = 0;
    // concatenate
    fullyDelete(jumboDir);
    jumboDir.mkdirs();
    File[] groups = sourceDir.listFiles()[0].listFiles();
    for (int i = 0; i < groups.length; i++) {
      File outdir = new File(jumboDir, groups[i].getName());
      outdir.mkdirs();
      dircnt++;
      System.err.println(" - creating jumbo files in " + outdir.getName());
      File[] files = groups[i].listFiles();
      for (int k = 11; k < 101; k++) {
        Vector streams = new Vector();
        for (int m = 0; m < k; m++) {
          FileInputStream fis = new FileInputStream(files[m]);
          streams.add(fis);
        }
        SequenceInputStream sis = new SequenceInputStream(streams.elements());
        saveStream(sis, new File(outdir, "" + k), true);
        fcnt++;
      }
    }
    System.err.println(" - " + fcnt + " jumbo files in " + dircnt + " directories.");
    // create the "done" file
    new FileOutputStream(setup_done).close();
  }

  /**
   * Remove existing index.
   * @throws Exception
   */
  public void reset() throws Exception {
    if (indexDir.exists()) fullyDelete(indexDir);
    indexDir.mkdirs();
  }

  /**
   * Remove index and unpacked source files. You have to run setup() after
   * you run this method.
   * @throws Exception
   */
  public void clean() throws Exception {
    reset();
    fullyDelete(jumboDir);
    fullyDelete(sourceDir);
    setup_done.delete();
  }

  /**
   * Assume the input is an NNTP message, where the header is separated from the
   * body with blank line. <br>
   * Extract basic metadata from the header: "From:", "Subject:", "Date:". If
   * more fields are needed, the first couple of lines from the body will be
   * converted into additional fields. <br>
   * NOTE: this method doesn't even pretend to be an RFC-compliant parser, so
   * don't expect any MIME or transport decoding or similar.
   * 
   * @param in input file
   * @param addFields if greater than 0, add more fields named "line0", "line1",
   *        "line2", etc, with the content made from the body text.
   * @return Lucene document
   */
  protected Document makeDocument(File in, int addFields, String[] tags, boolean stored, boolean tokenized, boolean tfv)
          throws Exception {
    Document doc = new Document();
    // tag this document
    if (tags != null) {
      for (int i = 0; i < tags.length; i++) {
        doc.add(new Field("tag" + i, tags[i], stored, true, tokenized, tfv));
      }
    }
    doc.add(new Field("file", in.getCanonicalPath(), stored, true, tokenized, tfv));
    Vector header = new Vector();
    Vector body = new Vector();
    BufferedReader br = new BufferedReader(new FileReader(in));
    boolean inHeader = true;
    String line = null;
    while ((line = br.readLine()) != null) {
      if (inHeader) {
        if (line.trim().equals("")) {
          inHeader = false;
          continue;
        }
        header.add(line);
      } else body.add(line);
    }
    br.close();
    for (int i = 0; i < header.size(); i++) {
      line = (String) header.get(i);
      if (line.startsWith("From: ")) {
        doc.add(new Field("from", line.substring(6), stored, true, tokenized, tfv));
      } else if (line.startsWith("Subject: ")) {
        doc.add(new Field("subject", line.substring(9), stored, true, tokenized, tfv));
      } else if (line.startsWith("Date: ")) {
        // parse date
        String val = null;
        try {
          val = DateField.timeToString(Date.parse(line.substring(6)));
        } catch (Exception e) {
          //System.out.println(" - " + in + ": bad date " + line);
          val = DateField.timeToString(System.currentTimeMillis());
        }
        doc.add(new Field("date", val, stored, true, false, false));
      }
    }
    // if additional fields are needed, add them here
    for (int i = 0; i < Math.min(addFields, body.size()); i++) {
      doc.add(new Field("line" + i, (String) body.get(i), stored, true, tokenized, tfv));
    }
    // add body
    StringBuffer bb = new StringBuffer();
    for (int i = 0; i < body.size(); i++) {
      if (i > 0) bb.append('\n');
      bb.append((String) body.get(i));
    }
    doc.add(new Field("body", bb.toString(), stored, true, tokenized, tfv));
    return doc;
  }

  /**
   * Make index, and collect time data.
   * @param trd run data to populate
   * @param srcDir directory with source files
   * @param iw index writer, already open
   * @param addFields fields to add to each document (see makeDocument method)
   * @param stored store values of fields
   * @param tokenized tokenize fields
   * @param tfv store term vectors
   * @throws Exception
   */
  public void makeIndex(TestRunData trd, File srcDir, IndexWriter iw, int addFields, boolean stored, boolean tokenized,
          boolean tfv) throws Exception {
    File[] groups = srcDir.listFiles()[0].listFiles();
    Document doc = null;
    long cnt = 0L;
    TimeData td = new TimeData();
    td.name = "addDocument";
    for (int s = 0; s < SCALE_UP; s++) {
      String[] tags = new String[] { srcDir.getName() + "/" + s };
      for (int i = 0; i < groups.length; i++) {
        File[] files = groups[i].listFiles();
        for (int k = 0; k < files.length; k++) {
          doc = makeDocument(files[k], addFields, tags, stored, tokenized, tfv);
          td.start();
          iw.addDocument(doc);
          td.stop();
          cnt++;
          if (cnt % LOG_STEP == 0) {
            System.err.println(" - processed " + cnt + ", run id=" + trd.id);
            trd.addData(td);
            td.reset();
          }
        }
      }
    }
    trd.addData(td);
  }

  /**
   * Run benchmark using supplied parameters.
   * @param params benchmark parameters
   * @throws Exception
   */
  public void runBenchmark(TestData params) throws Exception {
    for (int i = 0; i < RUN_COUNT; i++) {
      TestRunData trd = new TestRunData();
      trd.startRun();
      trd.id = "" + i;
      IndexWriter iw = new IndexWriter(params.dir, params.analyzer, true);
      iw.mergeFactor = params.mergeFactor;
      iw.minMergeDocs = params.minMergeDocs;
      iw.setUseCompoundFile(params.compound);
      makeIndex(trd, params.source, iw, 5, true, true, false);
      if (params.optimize) {
        TimeData td = new TimeData("optimize");
        trd.addData(td);
        td.start();
        iw.optimize();
        td.stop();
        trd.addData(td);
      }
      iw.close();
      if (params.queries != null) {
        IndexReader ir = null;
        IndexSearcher searcher = null;
        for (int k = 0; k < params.queries.length; k++) {
          QueryData qd = params.queries[k];
          if (ir != null && qd.reopen) {
            searcher.close();
            ir.close();
            ir = null;
            searcher = null;
          }
          if (ir == null) {
            ir = IndexReader.open(params.dir);
            searcher = new IndexSearcher(ir);
          }
          Document doc = null;
          if (qd.warmup) {
            TimeData td = new TimeData(qd.id + "-warm");
            for (int m = 0; m < ir.maxDoc(); m++) {
              td.start();
              if (ir.isDeleted(m)) {
                td.stop();
                continue;
              }
              doc = ir.document(m);
              td.stop();
            }
            trd.addData(td);
          }
          TimeData td = new TimeData(qd.id + "-srch");
          td.start();
          Hits h = searcher.search(qd.q);
          td.stop();
          trd.addData(td);
          td = new TimeData(qd.id + "-trav");
          if (h != null && h.length() > 0) {
            for (int m = 0; m < h.length(); m++) {
              td.start();
              int id = h.id(m);
              if (qd.retrieve) {
                doc = ir.document(id);
              }
              td.stop();
            }
          }
          trd.addData(td);
        }
        try {
          if (searcher != null) searcher.close();
        } catch (Exception e) {};
        try {
          if (ir != null) ir.close();
        } catch (Exception e) {};
      }
      trd.endRun();
      params.runData.add(trd);
    }
  }

  /**
   * Optional argument points to the output directory for the test.
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {
    LuceneBenchmark bench = new LuceneBenchmark();
    if (args.length > 0) bench.setWorkDir(new File(args[0]));
    bench.setup();
    Analyzer a = new StandardAnalyzer();
    Query[] qs = createQueries(queries, a);
    // Here you can limit the set of query benchmarks
    QueryData[] qds = QueryData.getAll(qs);
    // Here you can narrow down the set of test parameters
    TestData[] params = TestData.getAll(new File[] { bench.sourceDir, bench.jumboDir }, new Analyzer[] { a });
    for (int i = 0; i < params.length; i++) {
      try {
        bench.reset();
        params[i].dir = FSDirectory.getDirectory(bench.indexDir, true);
        params[i].queries = qds;
        System.out.println(params[i]);
        bench.runBenchmark(params[i]);
        // Here you can collect and output the runData for further processing.
        System.out.println(params[i].showRunData(params[i].id));
        //bench.runSearchBenchmark(queries, dir);
        params[i].dir.close();
        System.runFinalization();
        System.gc();
      } catch (Exception e) {
        e.printStackTrace();
        System.out.println("EXCEPTION: " + e.getMessage());
        //break;
      }
    }
  }

  /**
   * Parse the strings containing Lucene queries.
   * @param qs array of strings containing query expressions
   * @param a analyzer to use when parsing queries
   * @return array of Lucene queries
   */
  public static Query[] createQueries(String[] qs, Analyzer a) {
    QueryParser qp = new QueryParser("body", a);
    Vector queries = new Vector();
    for (int i = 0; i < qs.length; i++) {
      try {
        Query q = qp.parse(qs[i]);
        queries.add(q);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return (Query[])queries.toArray(new Query[0]);
  }
}

/**
 * This class holds parameters for a query benchmark.
 * 
 * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
 */
class QueryData {
  /** Benchmark id */
  public String id;
  /** Lucene query */
  public Query q;
  /** If true, re-open index reader before benchmark. */
  public boolean reopen;
  /** If true, warm-up the index reader before searching by sequentially
   * retrieving all documents from index.
   */
  public boolean warmup;
  /**
   * If true, actually retrieve documents returned in Hits.
   */
  public boolean retrieve;
  
  /**
   * Prepare a list of benchmark data, using all possible combinations of
   * benchmark parameters.
   * @param queries source Lucene queries
   * @return
   */
  public static QueryData[] getAll(Query[] queries) {
    Vector vqd = new Vector();
    for (int i = 0; i < queries.length; i++) {
      for (int r = 1; r >= 0; r--) {
        for (int w = 1; w >= 0; w--) {
          for (int t = 0; t < 2; t++) {
            QueryData qd = new QueryData();
            qd.id="qd-" + i + r + w + t;
            qd.reopen = LuceneBenchmark.bools[r].booleanValue();
            qd.warmup = LuceneBenchmark.bools[w].booleanValue();
            qd.retrieve = LuceneBenchmark.bools[t].booleanValue();
            qd.q = queries[i];
            vqd.add(qd);
          }
        }
      }
    }
    return (QueryData[])vqd.toArray(new QueryData[0]);
  }
  
  /** Short legend for interpreting toString() output. */
  public static String getLabels() {
    return "# Query data: R-reopen, W-warmup, T-retrieve, N-no";
  }
  
  public String toString() {
    return id + " " + (reopen ? "R" : "NR") + " " + (warmup ? "W" : "NW") +
      " " + (retrieve ? "T" : "NT") + " [" + q.toString() + "]";
  }
}

/**
 * This class holds a data point measuring speed of processing.
 * 
 * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
 */
class TimeData {
  /** Name of the data point - usually one of a data series with the same name */
  public String name;
  /** Number of records processed. */
  public long count = 0;
  /** Elapsed time in milliseconds. */
  public long elapsed = 0L;
  
  private long delta = 0L;
  /** Free memory at the end of measurement interval. */
  public long freeMem = 0L;
  /** Total memory at the end of measurement interval. */
  public long totalMem = 0L;

  public TimeData() {};

  public TimeData(String name) {
    this.name = name;
  }

  /** Start counting elapsed time. */
  public void start() {
    delta = System.currentTimeMillis();
  }

  /** Stop counting elapsed time. */
  public void stop() {
    count++;
    elapsed += (System.currentTimeMillis() - delta);
  }

  /** Record memory usage. */
  public void recordMemUsage() {
    freeMem = Runtime.getRuntime().freeMemory();
    totalMem = Runtime.getRuntime().totalMemory();
  }

  /** Reset counters. */
  public void reset() {
    count = 0;
    elapsed = 0L;
    delta = elapsed;
  }

  protected Object clone() {
    TimeData td = new TimeData(name);
    td.name = name;
    td.elapsed = elapsed;
    td.count = count;
    td.delta = delta;
    td.freeMem = freeMem;
    td.totalMem = totalMem;
    return td;
  }
  
  /** Get rate of processing, defined as number of processed records per second. */
  public double getRate() {
    double rps = (double) count * 1000.0 / (double) elapsed;
    return rps;
  }

  /** Get a short legend for toString() output. */
  public static String getLabels() {
    return "# count\telapsed\trec/s\tfreeMem\ttotalMem";
  }
  
  public String toString() { return toString(true); }
  /**
   * Return a tab-seprated string containing this data.
   * @param withMem if true, append also memory information
   * @return
   */
  public String toString(boolean withMem) {
    StringBuffer sb = new StringBuffer();
    sb.append(count + "\t" + elapsed + "\t" + getRate());
    if (withMem) sb.append("\t" + freeMem + "\t" + totalMem);
    return sb.toString();
  }
}

/**
 * This class holds series of TimeData related to a single test run. TimeData
 * values may contribute to different measurements, so this class provides also
 * some useful methods to separate them.
 * 
 * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
 */
class TestRunData {
  public String id;

  /** Start and end time of this test run. */
  public long start = 0L, end = 0L;

  private LinkedHashMap data = new LinkedHashMap();

  public TestRunData() {}

  public TestRunData(String id) {
    this.id = id;
  }

  /** Mark the starting time of this test run. */
  public void startRun() {
    start = System.currentTimeMillis();
  }

  /** Mark the ending time of this test run. */
  public void endRun() {
    end = System.currentTimeMillis();
  }

  /** Add a data point. */
  public void addData(TimeData td) {
    td.recordMemUsage();
    Vector v = (Vector) data.get(td.name);
    if (v == null) {
      v = new Vector();
      data.put(td.name, v);
    }
    v.add(td.clone());
  }

  /** Get a list of all available types of data points. */
  public Collection getLabels() {
    return data.keySet();
  }

  /** Get total values from all data points of a given type. */
  public TimeData getTotals(String label) {
    Vector v = (Vector) data.get(label);
    if (v == null) return null;
    TimeData res = new TimeData("TOTAL " + label);
    for (int i = 0; i < v.size(); i++) {
      TimeData td = (TimeData) v.get(i);
      res.count += td.count;
      res.elapsed += td.elapsed;
    }
    return res;
  }

  /** Get total values from all data points of all types.
   * @return a list of TimeData values for all types.
   */
  public Vector getTotals() {
    Collection labels = getLabels();
    Vector v = new Vector();
    Iterator it = labels.iterator();
    while (it.hasNext()) {
      TimeData td = getTotals((String) it.next());
      v.add(td);
    }
    return v;
  }

  /** Get memory usage stats. for a given data type. */
  public MemUsage getMemUsage(String label) {
    Vector v = (Vector) data.get(label);
    if (v == null) return null;
    MemUsage res = new MemUsage();
    res.minFree = Long.MAX_VALUE;
    res.minTotal = Long.MAX_VALUE;
    long avgFree = 0L, avgTotal = 0L;
    for (int i = 0; i < v.size(); i++) {
      TimeData td = (TimeData) v.get(i);
      if (res.maxFree < td.freeMem) res.maxFree = td.freeMem;
      if (res.maxTotal < td.totalMem) res.maxTotal = td.totalMem;
      if (res.minFree > td.freeMem) res.minFree = td.freeMem;
      if (res.minTotal > td.totalMem) res.minTotal = td.totalMem;
      avgFree += td.freeMem;
      avgTotal += td.totalMem;
    }
    res.avgFree = avgFree / v.size();
    res.avgTotal = avgTotal / v.size();
    return res;
  }

  /** Return a string representation. */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    Collection labels = getLabels();
    Iterator it = labels.iterator();
    while (it.hasNext()) {
      String label = (String) it.next();
      sb.append(id + "-" + label + " " + getTotals(label).toString(false) + " ");
      sb.append(getMemUsage(label).toScaledString(1024 * 1024, "MB") + "\n");
    }
    return sb.toString();
  }
}

/**
 * This class holds a set of memory usage values.
 * 
 * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
 */
class MemUsage {
  public long maxFree, minFree, avgFree;

  public long maxTotal, minTotal, avgTotal;

  public String toString() {
    return toScaledString(1, "B");
  }

  /** Scale down the values by divisor, append the unit string. */
  public String toScaledString(int div, String unit) {
    StringBuffer sb = new StringBuffer();
    sb.append("free=" + (minFree / div));
    sb.append("/" + (avgFree / div));
    sb.append("/" + (maxFree / div) + " " + unit);
    sb.append(", total=" + (minTotal / div));
    sb.append("/" + (avgTotal / div));
    sb.append("/" + (maxTotal / div) + " " + unit);
    return sb.toString();
  }
}

/**
 * This class holds together all parameters related to a test. Single test is
 * performed several times, and all results are averaged.
 * 
 * @author Andrzej Bialecki &lt;ab@getopt.org&gt;
 */
class TestData {
  public static int[] MINMERGE_COUNTS = new int[] { 10, 20, 50, 100, 200, 500 };
  public static int[] MERGEFACTOR_COUNTS = new int[] { 10, 20, 50, 100, 200, 500 };

  /** ID of this test data. */
  public String id;
  /** Heap size. */
  public long heap;
  /** List of results for each test run with these parameters. */
  public Vector runData = new Vector();
  public int minMergeDocs, mergeFactor;
  /** Directory containing source files. */
  public File source;
  /** Lucene Directory implementation for creating an index. */
  public Directory dir;
  /** Analyzer to use when adding documents. */
  public Analyzer analyzer;
  /** If true, use compound file format. */
  public boolean compound;
  /** If true, optimize index when finished adding documents. */
  public boolean optimize;
  /** Data for search benchmarks. */
  public QueryData[] queries;

  public TestData() {
    heap = Runtime.getRuntime().maxMemory();
  }

  private static class DCounter {
    double total;
    int count, recordCount;
  }
  
  private static class LCounter {
    long total;
    int count;
  }
  
  /** Get a textual summary of the benchmark results, average from all test runs. */
  public String showRunData(String prefix) {
    if (runData.size() == 0) return "# [NO RUN DATA]";
    StringBuffer sb = new StringBuffer();
    sb.append("# testData id\toperation\trunCnt\trecCnt\trec/s\tavgFreeMem\tavgTotalMem\n");
    LinkedHashMap mapMem = new LinkedHashMap();
    LinkedHashMap mapSpeed = new LinkedHashMap();
    for (int i = 0; i < runData.size(); i++) {
      TestRunData trd = (TestRunData)runData.get(i);
      Collection labels = trd.getLabels();
      Iterator it = labels.iterator();
      while (it.hasNext()) {
        String label = (String)it.next();
        MemUsage mem = trd.getMemUsage(label);
        if (mem != null) {
          LCounter[] tm = (LCounter[])mapMem.get(label);
          if (tm == null) {
            tm = new LCounter[2];
            tm[0] = new LCounter();
            tm[1] = new LCounter();
            mapMem.put(label, tm);
          }
          tm[0].total += mem.avgFree;
          tm[0].count++;
          tm[1].total += mem.avgTotal;
          tm[1].count++;
        }
        TimeData td = trd.getTotals(label);
        if (td != null) {
          DCounter dc = (DCounter)mapSpeed.get(label);
          if (dc == null) {
            dc = new DCounter();
            mapSpeed.put(label, dc);
          }
          dc.count++;
          dc.total += td.getRate();
          dc.recordCount += td.count;
        }
      }
    }
    LinkedHashMap res = new LinkedHashMap();
    Iterator it = mapSpeed.keySet().iterator();
    while (it.hasNext()) {
      String label = (String)it.next();
      DCounter dc = (DCounter)mapSpeed.get(label);
      res.put(label, dc.count + "\t" +
              (dc.recordCount / dc.count) + "\t" + (float)(dc.total / (double)dc.count));
    }
    it = mapMem.keySet().iterator();
    while (it.hasNext()) {
      String label = (String)it.next();
      LCounter[] lc = (LCounter[])mapMem.get(label);
      String speed = (String)res.get(label);
      if (speed == null) speed = lc[0].count + "\t0.0";
      res.put(label, speed + "\t" + (lc[0].total / lc[0].count) + 
              "\t" + (lc[1].total / lc[1].count));
    }
    it = res.keySet().iterator();
    while (it.hasNext()) {
      String label = (String)it.next();
      sb.append(prefix + "\t" + label + "\t" + res.get(label) + "\n");
    }
    return sb.toString();
  }
  
  /**
   * Prepare a list of benchmark data, using all possible combinations of
   * benchmark parameters.
   * @param sources list of directories containing different source document
   * collections
   * @param list of analyzers to use.
   */
  public static TestData[] getAll(File[] sources, Analyzer[] analyzers) {
    Vector res = new Vector();
    TestData ref = new TestData();
    for (int q = 0; q < analyzers.length; q++) {
      for (int m = 0; m < sources.length; m++) {
        for (int i = 0; i < MINMERGE_COUNTS.length; i++) {
          for (int k = 0; k < MERGEFACTOR_COUNTS.length; k++) {
            for (int n = 0; n < LuceneBenchmark.bools.length; n++) {
              for (int p = 0; p < LuceneBenchmark.bools.length; p++) {
                ref.id = "td-" + q + m + i + k + n + p;
                ref.source = sources[m];
                ref.analyzer = analyzers[q];
                ref.minMergeDocs = MINMERGE_COUNTS[i];
                ref.mergeFactor = MERGEFACTOR_COUNTS[k];
                ref.compound = LuceneBenchmark.bools[n].booleanValue();
                ref.optimize = LuceneBenchmark.bools[p].booleanValue();
                try {
                  res.add(ref.clone());
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            }
          }
        }
      }
    }
    return (TestData[]) res.toArray(new TestData[0]);
  }

  protected Object clone() {
    TestData cl = new TestData();
    cl.id = id;
    cl.compound = compound;
    cl.heap = heap;
    cl.mergeFactor = mergeFactor;
    cl.minMergeDocs = minMergeDocs;
    cl.optimize = optimize;
    cl.source = source;
    cl.dir = dir;
    cl.analyzer = analyzer;
    // don't clone runData
    return cl;
  }

  public String toString() {
    StringBuffer res = new StringBuffer();
    res.append("#-- ID: " + id + ", " + new Date().toGMTString() + ", heap=" + heap + " --\n");
    res.append("# source=" + source + ", dir=" + dir + "\n");
    res.append("# minMergeDocs=" + minMergeDocs + ", mergeFactor=" + mergeFactor);
    res.append(", compound=" + compound + ", optimize=" + optimize + "\n");
    if (queries != null) {
      res.append(QueryData.getLabels() + "\n");
      for (int i = 0; i < queries.length; i++) {
        res.append("# " + queries[i].toString() + "\n");
      }
    }
    return res.toString();
  }
}