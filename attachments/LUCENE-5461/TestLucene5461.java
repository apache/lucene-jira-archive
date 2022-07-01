package org.apache.lucene.search;

import static org.junit.Assert.*;

import java.util.Random;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.KeepOnlyLastCommitDeletionPolicy;
import org.apache.lucene.index.SnapshotDeletionPolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TrackingIndexWriter;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NRTCachingDirectory;
import org.apache.lucene.util.Version;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public class TestLucene5461 {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testCRTReopen() throws Exception {
        //test behaving badly

        //should be high enough
        int maxStaleSecs = 10;

        //build crap data just to store it.
        String s = "        abcdefghijklmnopqrstuvwxyz     ";
        char[] chars = s.toCharArray();
        Random r = new Random();
        StringBuilder builder = new StringBuilder(2048);
        for (int i = 0; i < 2048; i++) {
            builder.append(chars[r.nextInt(chars.length)]);
        }
        String content = builder.toString();

        final SnapshotDeletionPolicy sdp =
            new SnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy());
        Directory dir =
            new NRTCachingDirectory(new MMapDirectory(folder.getRoot()), 5,
                128);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_46,
            new StandardAnalyzer(Version.LUCENE_46));
        config.setIndexDeletionPolicy(sdp);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        final IndexWriter iw = new IndexWriter(dir, config);
        SearcherManager sm =
            new SearcherManager(iw, true, new SearcherFactory());
        final TrackingIndexWriter tiw = new TrackingIndexWriter(iw);
        ControlledRealTimeReopenThread controlledRealTimeReopenThread =
            new ControlledRealTimeReopenThread(tiw, sm, maxStaleSecs, 0);

        controlledRealTimeReopenThread.setDaemon(true);
        controlledRealTimeReopenThread.start();

        for (int i = 0; i < 10000; i++) {
            if (i > 0 && i % 1000 == 0) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            iw.commit();
                            IndexCommit ic = sdp.snapshot();
                            for (String names : ic.getFileNames()) {
                                //distribute, and backup
                                System.out.println(names);
                            }
                        } catch (Exception e) {

                        }
                    }
                }).start();
            }
            Document d = new Document();
            d.add(new TextField("count", i + "", Field.Store.NO));
            d.add(new TextField("content", content, Field.Store.YES));
            long start = System.currentTimeMillis();
            long l = tiw.addDocument(d);
            controlledRealTimeReopenThread.waitForGeneration(l);
            long wait = System.currentTimeMillis() - start;
            assertTrue("waited too long for generation " + wait,
                wait < (maxStaleSecs *1000));
            IndexSearcher searcher = sm.acquire();
            TopDocs td =
                searcher.search(new TermQuery(new Term("count", i + "")), 10);
            sm.release(searcher);
            assertEquals(1, td.totalHits);

        }
    }
}
