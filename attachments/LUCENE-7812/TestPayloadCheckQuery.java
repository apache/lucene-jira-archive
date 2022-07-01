package org.apache.lucene.queries.payloads;

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
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.queries.payloads.SpanPayloadCheckQuery;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.index.PostingsEnum;
import java.lang.reflect.Constructor;
import org.apache.lucene.store.MockDirectoryWrapper;
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.lucene.store.RawDirectoryWrapper;
import org.apache.lucene.util.CloseableDirectory;
import org.apache.lucene.util.Rethrow;
import java.util.Random;
import org.apache.lucene.store.BaseDirectoryWrapper;
import org.apache.lucene.store.FSDirectory;
import java.nio.file.Path;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.util.CommandLineUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.apache.lucene.store.MMapDirectory;
import java.util.Collections;
import java.util.List;
import org.apache.lucene.util.Constants;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.SimplePayloadFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.CheckHits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanPositionRangeQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.English;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.TestUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/** basic test of payload-spans */
public class TestPayloadCheckQuery extends LuceneTestCase {
  private static final Log log = LogFactory.getLog(TestPayloadCheckQuery.class);
  private static IndexSearcher searcher;
  private static IndexReader reader;
  private static Directory directory;

  @BeforeClass
  public static void beforeClass() throws Exception {
    Analyzer simplePayloadAnalyzer = new Analyzer() {
        @Override
        public TokenStreamComponents createComponents(String fieldName) {
          Tokenizer tokenizer = new MockTokenizer(MockTokenizer.SIMPLE, true);
          return new TokenStreamComponents(tokenizer, new SimplePayloadFilter(tokenizer));
        }
    };

    directory = newDirectory();
    LogMergePolicy logMergePolicy=newLogMergePolicy();
    RandomIndexWriter writer=null;
    Random random=null;
    if(simplePayloadAnalyzer!=null)
    {
		if(directory!=null)
		{
			if(logMergePolicy!=null)
			{
				random=random();
				if(random!=null)
				{
					writer = new RandomIndexWriter(random, directory,
		    	    	newIndexWriterConfig(simplePayloadAnalyzer)
	        	    	.setMaxBufferedDocs(TestUtil.nextInt(random, 100, 1000)).setMergePolicy(logMergePolicy));
				}
			}
		}
	}
    //writer.infoStream = System.out;
    for (int i = 0; i < 2000; i++) {
      Document doc = new Document();
      doc.add(newTextField("field", English.intToEnglish(i), Field.Store.YES));
      writer.addDocument(doc);
    }
    reader = writer.getReader();
    searcher = newSearcher(reader);
    writer.close();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    reader.close();
    directory.close();
    searcher = null;
    reader = null;
    directory = null;
  }

  private void checkHits(Query query, int[] results) throws IOException {
    CheckHits.checkHits(random(), query, "field", searcher, results);
  }
/**
   * Returns true if something should happen rarely,
   * <p>
   * The actual number returned will be influenced by whether {@link #TEST_NIGHTLY}
   * is active and {@link #RANDOM_MULTIPLIER}.
   */
  public static boolean rarely(java.util.Random random)
  {
    int p = TEST_NIGHTLY ? 10 : 1;
    p += (p * Math.log(com.carrotsearch.randomizedtesting.RandomizedTest.systemPropertyAsInt("tests.multiplier", 1)));
    int min = 100 - Math.min(p, 50); // never more than 50
    return random.nextInt(100) >= min;
  }
  public static Directory newDirectoryImpl(java.util.Random random, String clazzName) {
    return newDirectoryImpl(random, clazzName, org.apache.lucene.store.FSLockFactory.getDefault());
  }
/** Returns true, if MMapDirectory supports unmapping on this platform (required for Windows), or if we are not on Windows. */
  public static boolean hasWorkingMMapOnWindows() {
    return !Constants.WINDOWS || MMapDirectory.UNMAP_SUPPORTED;
  }
 /** Filesystem-based {@link Directory} implementations. */
  public static List<String> FS_DIRECTORIES = java.util.Arrays.asList(
    "SimpleFSDirectory",
    "NIOFSDirectory",
    // SimpleFSDirectory as replacement for MMapDirectory if unmapping is not supported on Windows (to make randomization stable):
    hasWorkingMMapOnWindows() ? "MMapDirectory" : "SimpleFSDirectory"
  );
/** All {@link Directory} implementations. */
  private static List<String> CORE_DIRECTORIES=new java.util.ArrayList<String>();
  static {
    CORE_DIRECTORIES = new ArrayList<>(FS_DIRECTORIES);
    CORE_DIRECTORIES.add("RAMDirectory");
  }
 public static Directory newFSDirectoryImpl(Class<? extends FSDirectory> clazz, Path path, LockFactory lf) throws IOException {
    FSDirectory d = null;
    try {
      d = CommandLineUtil.newFSDirectory(clazz, path, lf);
    } catch (ReflectiveOperationException e) {
      Rethrow.rethrow(e);
    }
    return d;
  }
  public static Directory newDirectoryImpl(java.util.Random random, String clazzName, org.apache.lucene.store.LockFactory lf) {
    if (clazzName.equals("random"))
    {
      if (rarely(random))
      {
        clazzName = com.carrotsearch.randomizedtesting.generators.RandomPicks.randomFrom(random, CORE_DIRECTORIES);
      }
      else
      {
        clazzName = "RAMDirectory";
      }
    }
    try
    {
      final Class<? extends org.apache.lucene.store.Directory> clazz = org.apache.lucene.util.CommandLineUtil.loadDirectoryClass(clazzName);
      // If it is a FSDirectory type, try its ctor(Path)
      if (org.apache.lucene.store.FSDirectory.class.isAssignableFrom(clazz)) {
        final java.nio.file.Path dir = createTempDir("index-" + clazzName);
        return newFSDirectoryImpl(clazz.asSubclass(org.apache.lucene.store.FSDirectory.class), dir, lf);
      }

      // See if it has a Path/LockFactory ctor even though it's not an
      // FSDir subclass:
      try {
        Constructor<? extends org.apache.lucene.store.Directory> pathCtor = clazz.getConstructor(java.nio.file.Path.class, org.apache.lucene.store.LockFactory.class);
        final java.nio.file.Path dir = createTempDir("index");
        return pathCtor.newInstance(dir, lf);
      } catch (NoSuchMethodException nsme) {
        // Ignore
      }

      // the remaining dirs are no longer filesystem based, so we must check that the passedLockFactory is not file based:
      if (!(lf instanceof org.apache.lucene.store.FSLockFactory)) {
        // try ctor with only LockFactory (e.g. RAMDirectory)
        try {
          return clazz.getConstructor(org.apache.lucene.store.LockFactory.class).newInstance(lf);
        } catch (NoSuchMethodException nsme) {
          // Ignore
        }
      }

      // try empty ctor
      return clazz.newInstance();
    } catch (Exception e) {
      org.apache.lucene.util.Rethrow.rethrow(e);
      throw null; // dummy to prevent compiler failure
    }
  } //newDirectoryImpl(Clazz...)
 private static BaseDirectoryWrapper wrapDirectory(Random random, Directory directory, boolean bare) {
    if (rarely(random) && !bare) {
      directory = new NRTCachingDirectory(directory, random.nextDouble(), random.nextDouble());
    }

    if (bare) {
      BaseDirectoryWrapper base = new RawDirectoryWrapper(directory);
      closeAfterSuite(new org.apache.lucene.util.CloseableDirectory(base, suiteFailureMarker));
      return base;
    } else {
      MockDirectoryWrapper mock = new MockDirectoryWrapper(random, directory);

      mock.setThrottling(TEST_THROTTLING);
      closeAfterSuite(new org.apache.lucene.util.CloseableDirectory(mock, suiteFailureMarker));
      return mock;
    }
  } //end wrapDirectory
/**
   * Returns a new Directory instance, using the specified random.
   * See {@link #newDirectory()} for more information.
   */
  public static BaseDirectoryWrapper newDirectory(Random r) {
    return wrapDirectory(r, newDirectoryImpl(r, TEST_DIRECTORY), rarely(r));
  }
  // assigning the values
  public void setUp(){
        query = null;
   }
  public org.apache.lucene.queries.payloads.SpanPayloadCheckQuery query=null;
  @org.junit.Test
  public void testSpanPayloadCheck() throws Exception
  {
	log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 231 before term1=new org.apache.lucene.index.Term('field','withPayload')");
	org.apache.lucene.index.Term term1=new org.apache.lucene.index.Term("field", "withPayload");
	log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 233 term1="+term1);
    int position=5;
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 235 position="+position);
    BytesRef pay = new BytesRef("pos: " + position);
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 237 pay="+pay);
    org.apache.lucene.search.spans.SpanQuery spanQuery1 = new SpanTermQuery(term1);
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 239 spanQuery1="+spanQuery1);
    java.util.List<org.apache.lucene.util.BytesRef> payloadToMatch=new java.util.ArrayList<org.apache.lucene.util.BytesRef>();
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 241 payloadToMatch="+payloadToMatch);
    payloadToMatch.add(pay);
    //now lets test the collectLeaf for query
    //lets call Base Class SpanPayloadCheckQuery
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 245 before payloadToMatch="+payloadToMatch);
	query=new org.apache.lucene.queries.payloads.SpanPayloadCheckQuery(
		(org.apache.lucene.search.spans.SpanQuery)query,
		(java.util.List<org.apache.lucene.util.BytesRef>)payloadToMatch);
	log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 249 query="+query);
	org.apache.lucene.store.Directory ram = newDirectory(com.carrotsearch.randomizedtesting.RandomizedContext.current().getRandom());
	log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 251 ram="+ram);
	SegmentReader reader = getOnlySegmentReader(org.apache.lucene.index.DirectoryReader.open(ram));
	log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 253 reader="+reader);
    org.apache.lucene.index.LeafReader sr = org.apache.lucene.index.SlowCompositeReaderWrapper.wrap(reader);
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 255 sr="+sr);
	org.apache.lucene.index.PostingsEnum postings = sr.postings(term1, PostingsEnum.PAYLOADS);
	log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 257 before query.getPayloadChecker().collectLeaf((org.apache.lucene.index.PostingsEnum)postings, (int)position,(org.apache.lucene.index.Term)term1) where postings="+postings);
	log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 258 before query.getPayloadChecker().collectLeaf((org.apache.lucene.index.PostingsEnum)postings, (int)position,(org.apache.lucene.index.Term)term1) where position="+position);
	log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 259 before query.getPayloadChecker().collectLeaf((org.apache.lucene.index.PostingsEnum)postings, (int)position,(org.apache.lucene.index.Term)term1) where term1="+term1);
    try
    { //public void collectLeaf(org.apache.lucene.index.PostingsEnum postings, int position, org.apache.lucene.index.Term term) throws java.io.IOException {
		query.getPayloadChecker().collectLeaf((org.apache.lucene.index.PostingsEnum)postings, (int)position,(org.apache.lucene.index.Term)term1);
	}
	catch(java.io.IOException ioe) { log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 264 query.getPayloadChecker().collectLeaf((org.apache.lucene.index.PostingsEnum)postings, (int)position,(org.apache.lucene.index.Term)term1) LINE 106 throws IOException ="+ioe.getMessage()); }

    checkHits(query, new int[]
      {1125, 1135, 1145, 1155, 1165, 1175, 1185, 1195, 1225, 1235, 1245, 1255, 1265, 1275, 1285, 1295, 1325, 1335, 1345, 1355, 1365, 1375, 1385, 1395, 1425, 1435, 1445, 1455, 1465, 1475, 1485, 1495, 1525, 1535, 1545, 1555, 1565, 1575, 1585, 1595, 1625, 1635, 1645, 1655, 1665, 1675, 1685, 1695, 1725, 1735, 1745, 1755, 1765, 1775, 1785, 1795, 1825, 1835, 1845, 1855, 1865, 1875, 1885, 1895, 1925, 1935, 1945, 1955, 1965, 1975, 1985, 1995});
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 268 before assertTrue(searcher.explain(query, 1125).getValue() > 0.0f) where query="+query+" searcher.explain(query, 1125).getValue()="+searcher.explain(query, 1125).getValue());
    assertTrue(searcher.explain(query, 1125).getValue() > 0.0f);
    Term term2=new Term("field", "hundred");
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 271 term2="+term2);
    SpanTermQuery spanQuery2 = new SpanTermQuery(term2);
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 273 spanQuery2="+spanQuery2);
    SpanNearQuery snq;
    SpanQuery[] clauses;
    List<BytesRef> list;
    BytesRef pay2;
    clauses = new SpanQuery[2];
    clauses[0] = spanQuery1;
    clauses[1] = spanQuery2;
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 281 before   snq = new SpanNearQuery(clauses, 0, true) clauses="+clauses);
    snq = new SpanNearQuery(clauses, 0, true);
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 283 before pay = new BytesRef('pos: '+ 0)");
    pay = new BytesRef("pos: " + 0);
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 285 pay="+pay);
    pay2 = new BytesRef("pos: " + 1);
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 287 pay2="+pay2);
    list = new ArrayList<>();
    list.add(pay);
    list.add(pay2);
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 291 before query = new SpanPayloadCheckQuery(snq, list) where list="+list);
    query = new SpanPayloadCheckQuery(snq, list);
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 293 before checkHits(query, new int[] query="+query);
    checkHits(query, new int[]
      {500, 501, 502, 503, 504, 505, 506, 507, 508, 509, 510, 511, 512, 513, 514, 515, 516, 517, 518, 519, 520, 521, 522, 523, 524, 525, 526, 527, 528, 529, 530, 531, 532, 533, 534, 535, 536, 537, 538, 539, 540, 541, 542, 543, 544, 545, 546, 547, 548, 549, 550, 551, 552, 553, 554, 555, 556, 557, 558, 559, 560, 561, 562, 563, 564, 565, 566, 567, 568, 569, 570, 571, 572, 573, 574, 575, 576, 577, 578, 579, 580, 581, 582, 583, 584, 585, 586, 587, 588, 589, 590, 591, 592, 593, 594, 595, 596, 597, 598, 599});
    clauses = new SpanQuery[3];
    clauses[0] = spanQuery1;
    clauses[1] = spanQuery2;
    Term term3=new Term("field", "five");
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 300 term3="+term3);
    clauses[2] = new SpanTermQuery(term3);
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 302 clauses="+clauses);
    snq = new SpanNearQuery(clauses, 0, true);
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 304 snq="+snq);
    pay = new BytesRef("pos: " + 0);
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 306 pay="+pay);
    pay2 = new BytesRef("pos: " + 1);
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 308 pay2="+pay2);
    BytesRef pay3 = new BytesRef("pos: " + 2);
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 310 pay3="+pay3);
    list = new ArrayList<>();
    list.add(pay);
    list.add(pay2);
    list.add(pay3);
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 315 before query = new SpanPayloadCheckQuery(snq, list) snq="+snq);
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 316 before query = new SpanPayloadCheckQuery(snq, list) list="+list);
    query = new SpanPayloadCheckQuery(snq, list);
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheck LINE 318 before  checkHits(query, new int[] query="+query);
    checkHits(query, new int[]
      {505});
    log.debug("TestPayloadCheckQuery::testSpanPayloadCheckb LINE 321 testcase has finished");
  }

  public void testUnorderedPayloadChecks() throws Exception {

    SpanTermQuery term5 = new SpanTermQuery(new Term("field", "five"));
    SpanTermQuery term100 = new SpanTermQuery(new Term("field", "hundred"));
    SpanTermQuery term4 = new SpanTermQuery(new Term("field", "four"));
    SpanNearQuery nearQuery = new SpanNearQuery(new SpanQuery[]{term5, term100, term4}, 0, false);

    List<BytesRef> payloads = new ArrayList<>();
    payloads.add(new BytesRef("pos: " + 2));
    payloads.add(new BytesRef("pos: " + 1));
    payloads.add(new BytesRef("pos: " + 0));

    SpanPayloadCheckQuery payloadQuery = new SpanPayloadCheckQuery(nearQuery, payloads);
    checkHits(payloadQuery, new int[]{ 405 });

    payloads.clear();
    payloads.add(new BytesRef("pos: " + 0));
    payloads.add(new BytesRef("pos: " + 1));
    payloads.add(new BytesRef("pos: " + 2));

    payloadQuery = new SpanPayloadCheckQuery(nearQuery, payloads);
    checkHits(payloadQuery, new int[]{ 504 });

  }

  public void testComplexSpanChecks() throws Exception {
    SpanTermQuery one = new SpanTermQuery(new Term("field", "one"));
    SpanTermQuery thous = new SpanTermQuery(new Term("field", "thousand"));
    //should be one position in between
    SpanTermQuery hundred = new SpanTermQuery(new Term("field", "hundred"));
    SpanTermQuery three = new SpanTermQuery(new Term("field", "three"));

    SpanNearQuery oneThous = new SpanNearQuery(new SpanQuery[]{one, thous}, 0, true);
    SpanNearQuery hundredThree = new SpanNearQuery(new SpanQuery[]{hundred, three}, 0, true);
    SpanNearQuery oneThousHunThree = new SpanNearQuery(new SpanQuery[]{oneThous, hundredThree}, 1, true);
    SpanQuery query;
    //this one's too small
    query = new SpanPositionRangeQuery(oneThousHunThree, 1, 2);
    checkHits(query, new int[]{});
    //this one's just right
    query = new SpanPositionRangeQuery(oneThousHunThree, 0, 6);
    checkHits(query, new int[]{1103, 1203,1303,1403,1503,1603,1703,1803,1903});

    List<BytesRef> payloads = new ArrayList<>();
    BytesRef pay = new BytesRef(("pos: " + 0).getBytes(StandardCharsets.UTF_8));
    BytesRef pay2 = new BytesRef(("pos: " + 1).getBytes(StandardCharsets.UTF_8));
    BytesRef pay3 = new BytesRef(("pos: " + 3).getBytes(StandardCharsets.UTF_8));
    BytesRef pay4 = new BytesRef(("pos: " + 4).getBytes(StandardCharsets.UTF_8));
    payloads.add(pay);
    payloads.add(pay2);
    payloads.add(pay3);
    payloads.add(pay4);
    query = new SpanPayloadCheckQuery(oneThousHunThree, payloads);
    checkHits(query, new int[]{1103, 1203,1303,1403,1503,1603,1703,1803,1903});

  }
}
