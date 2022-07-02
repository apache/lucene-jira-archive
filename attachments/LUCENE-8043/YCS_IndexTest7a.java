/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;


public class YCS_IndexTest7a {
  private static class XORShift64Random {
    private long x;
    /** Creates a xorshift random generator using the provided seed */
    public XORShift64Random(long seed) {
      x = seed == 0 ? 0xdeadbeef : seed;
    }

    /** Get the next random long value */
    public long randomLong() {
      x ^= (x << 21);
      x ^= (x >>> 35);
      x ^= (x << 4);
      return x;
    }

    /** Get the next random int, between 0 (inclusive) and n (exclusive) */
    public int nextInt(int n) {
      int res = (int) (randomLong() % n);
      return (res < 0) ? -res : res;
    }
  }


  static long maxdoc = 10;
  static long ndocs = maxdoc * 2;
  static long maxid = maxdoc * 3;
  static long update = Math.min(1000000, maxdoc);  // update the last 1M adds
  static long numTooManyDocsEx = ndocs/10;  // how many docs to try and pound in after it starts throwing exceptions
  static int reopenFreq = (int)(ndocs/5);  // try a reader reopen every n docs or so...
  static int commitFreq = (int)(ndocs/10);  // commit sometimes
  static int dbqFreq = (int)(ndocs/10);
  static int every=5000000;
  static Directory dir;
  static IndexWriterConfig iwc;
  static IndexWriter iw;
  static Exception unexpected;

  static int nThreads = 32;
  static int verbose = 1;

  static Analyzer analyzer = new Analyzer() {
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
      return null;
    }
  };

  public static String info(Directory dir) {
    if (dir instanceof RAMDirectory) {
      return " sizeInBytes:" + ((RAMDirectory)dir).ramBytesUsed();
    } else {
      return " " + dir.toString();
    }
  }

  public static DirectoryReader reopen(DirectoryReader r) throws IOException {
    DirectoryReader newReader = null;
    try {
      newReader = r == null ? DirectoryReader.open(iw) : DirectoryReader.openIfChanged(r, iw, true);
      if (newReader == null) {
        // reopen javadoc says it doesn't return null, but it actually does sometimes.
        return r;
      }
    } catch (Exception e) {
      unexpected = e;
      e.printStackTrace(System.out);
      return r;
    }
    // check maxdoc
    /***
    if (newReader.maxDoc() > maxdoc) {
      System.out.println("ERROR: Exceeded writer maxdoc. reader maxdoc=" + newReader.maxDoc());  // print immediately so it won't get swallowed
      throw new RuntimeException("Exceeded writer maxdoc. reader maxdoc=" + newReader.maxDoc());
    }
     ***/
    if (r != null) r.close();
    return newReader;
  }

  public static void doIndex(final long ndocs, final long perThread) throws IOException {
    final AtomicLong docnum = new AtomicLong();
    final boolean doUpdate = true;
    final AtomicLong tooManyDocs = new AtomicLong();

    Thread[] threads = new Thread[nThreads];
    for (int t=0; t<threads.length; t++) {
      final int threadnum = t;
      threads[t] = new Thread() {
        Document doc = new Document();
        StringField field = new StringField("id", "0", Field.Store.NO); // xor-shift will never produce a value of 0
        { doc.add(field); }
        XORShift64Random random = new XORShift64Random(threadnum+1);
        DirectoryReader reader;
        long perThreadCountdown = perThread;


        @Override
        public void run() {
          try {
            for(;;) {
              if (--perThreadCountdown < 0) break;
              long thisDoc = docnum.getAndIncrement();
              if (thisDoc >= ndocs) {
                return;
              }

              // System.out.println("time in sec:" + (System.currentTimeMillis()-start)/1000 + " Docs indexed:" + currdoc + info(dir));

              try {
                // if we're not updating, don't even change the field for extra speed
                if (doUpdate) {
                  // System.out.println("First Update: time in sec:" + (System.currentTimeMillis()-start)/1000 + " Docs indexed:" + currdoc + " ramBytesUsed:" + info(dir));

                  long id = random.randomLong();  // full long bits range.... this means that xorshift should not repeat any ids if maxid==-1
                  if (maxid != -1) id = id % maxid;
                  String v = Long.toString(id); // System.out.println("#################### INDEXING " + v);
                  field.setStringValue(Long.toString(id));
                  Term t = new Term("id", v);
                  if (random.nextInt(dbqFreq) == 0) {
                    iw.deleteDocuments(new TermQuery(t));
                  }
                  iw.updateDocument(t, doc);
                } else {
                  iw.addDocument(doc);
                }
              } catch (IllegalStateException e) { // Lucene 4
                // too many docs... this exception is OK (a user error, not a lucene error)
                // let this happen a few times.
                if (tooManyDocs.incrementAndGet() > numTooManyDocsEx) break;
              } catch (IllegalArgumentException e) { // Lucene 7 (or 4 with patch)
                // too many docs... this exception is OK (a user error, not a lucene error)
                // let this happen a few times.
                if (tooManyDocs.incrementAndGet() > numTooManyDocsEx) break;
              }

              if (random.nextInt(reopenFreq) == 0) {
                reader = reopen(reader);
              }

              if (random.nextInt(commitFreq) == 0) {
                iw.commit();
              }

            }

          } catch (Exception e) {
            unexpected = e;
            e.printStackTrace(System.out);
          } finally {
            try {
              // nocommit reader = reopen(reader);
              if (reader != null) reader.close();
            } catch (IOException e) {
              unexpected = e;
              e.printStackTrace(System.out);
            }
          }
        }

      };

    }

    for (Thread t : threads) {
      t.start();
    }

    for (Thread t : threads) {
      try {
        t.join();
      } catch (InterruptedException e) {
        e.printStackTrace(System.out);
      }
    }


    /***

    System.out.println("tooManyDocsFails=" + tooManyDocs.get());

    // We aren't using the IndexWriter for this open, so if this fails, we know the directory is in a bad state.
    // Background merges may bring it into a good state again, but there is no guarantee that that will happen (i.e. the JVM may get killed).
    try {
      DirectoryReader reader = DirectoryReader.open(dir);
      reader.close();
    } catch (Exception e) {
      unexpected = e;
      System.out.println("DIRECTORY IS IN BAD STATE!");
      e.printStackTrace(System.out);
    }

     ***/
  }


  public static void main(String[] args) throws Exception {
    int i = 0;
    if (args.length > i) {
      ndocs = Long.parseLong(args[i]);
      i++;
    }
    if (args.length > i) {
      maxid = Long.parseLong(args[i]);
      i++;
    }
    if (args.length > i) {
      update = Long.parseLong(args[i]);
      i++;
    }

    maxdoc = 1000;

    // IndexWriter.setMaxDocs((int) maxdoc);

    dir = new RAMDirectory();
    iwc = new IndexWriterConfig(analyzer);
    iwc.setRAMBufferSizeMB(0.025);

    // iwc.setInfoStream(System.out);

    // iwc.setMaxThreadStates(32);
    iw = new IndexWriter(dir, iwc);


    boolean firstUpdate = true;
    long start = System.currentTimeMillis();


    ndocs = maxdoc * 2;
    maxid = 2000000000; // maxdoc * 3;
    update = Math.min(1000000, maxdoc);  // update the last 1M adds
    numTooManyDocsEx = ndocs / 10;  // how many docs to try and pound in after it starts throwing exceptions
    reopenFreq = 5; // Integer.MAX_VALUE;  // try a reader reopen every n docs or so...
    commitFreq = 20; // Integer.MAX_VALUE;  // commit sometimes
    dbqFreq = 10; // Integer.MAX_VALUE;
    nThreads = 2;

    for (int indexiter = 0; indexiter < 100; indexiter++) {
      // doIndex(ndocs);
      System.out.println("########## STARTING INDEXING RUN " + indexiter + "  IW.pendingNumDocs=" + iw.pendingNumDocs.get());

      doIndex(2, 1);

      // System.out.println("########## ABOUT TO SLEEP: IW.pendingNumDocs=" + iw.pendingNumDocs.get());
      Thread.sleep(1000);  // this doesn't prevent (doIndex(5)) from failing, and actually makes it fail with a 5,0 instead of a 10,5 or 11,6 or whatever.  may be simpler to debug

      System.out.println("########## IW.pendingNumDocs=" + iw.pendingNumDocs.get());

      if (iw.hasPendingMerges()) {
        // System.out.println("ABOUT TO CALL waitForMerges");
        // iw.waitForMerges();
      }
      System.out.println("ABOUT TO CALL commit");
      iw.commit();

      DirectoryReader reader = DirectoryReader.open(dir);
      System.out.println("READER: reader.maxDoc=" + reader.maxDoc() + " IW.pendingNumDocs=" + iw.pendingNumDocs.get());
      if (reader.maxDoc() != iw.pendingNumDocs.get()) {
        System.out.println("ERROR!!!!!!!!!!!!!!!!!!: reader.maxDoc=" + reader.maxDoc() + " IW.pendingNumDocs=" + iw.pendingNumDocs.get());
        Thread.sleep(5000); // THIS SOMETIMES HELPS (or the commit/close does)... this does *not* help if I change to doIndex(4).... I tend to get reader=10, IW=6
        iw.commit();  // this commit after the sleep often brings back down maxDoc to pendingNumDocs
        // iw.close();
        reader.close();
        reader = DirectoryReader.open(dir);
        System.out.println("After sleep,commit,close reader.maxDoc=" + reader.maxDoc() + " IW.pendingNumDocs=" + iw.pendingNumDocs.get());
        break;
      }
      reader.close(); // this changes failure modes
    }

    iw.close();
    dir.close();
  }

}
