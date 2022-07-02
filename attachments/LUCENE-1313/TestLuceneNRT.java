package test;

import java.io.File;
import java.util.Random;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.BooleanFilter;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.FilterClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermsFilter;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.FSDirectory;

public class TestLuceneNRT
{
    // A bit of padding to bloat the index a bit
    static final String PADDING = "PADDINGPADDINGPADDINGPADDINGPADDINGPADDING";

    public static void main(String[] args) throws Exception
    {
        Writer w = new Writer();
        w.runTest();
    }

    static class Writer
    {
        IndexWriter indexWriter;

        boolean running = true;

        Writer() throws Exception
        {
            File testDir = new File("./testIndex");
            testDir.mkdir();
            for (File f : testDir.listFiles())
            {
                if (!f.delete())
                {
                    throw new RuntimeException("test dir not cleared");
                }

            }
            FSDirectory dir = FSDirectory.open(testDir);
            indexWriter = new IndexWriter(dir, new WhitespaceAnalyzer(), true,
                    MaxFieldLength.UNLIMITED);
            indexWriter.setRAMBufferSizeMB(1500);
        }

        long count = 0;

        public void runTest() throws Exception
        {
            IndexReader indexReader = null;

            int NUM_FIELDS = 10;

            Random random = new Random(System.currentTimeMillis());

            try
            {
                long startTime = System.currentTimeMillis();
                double sumSearch = 0;
                double sumIndexing = 0;
                double sumReopenReader = 0;

                while (running && count < Long.MAX_VALUE)
                {
                    long flipStart = System.currentTimeMillis();
                    indexReader = indexWriter.getReader();
                    IndexSearcher searcher = new IndexSearcher(indexReader);
                    sumReopenReader += System.currentTimeMillis() - flipStart;

                    if (indexReader.numDocs() > 0)
                    {
                        // do a random boolean query search that will return 1 result
                        int value = random.nextInt(indexReader.numDocs());

                        BooleanFilter filter = new BooleanFilter();
                        ConstantScoreQuery query = new ConstantScoreQuery(filter);
                        for (int i = 0; i < NUM_FIELDS; i++)
                        {
                            TermsFilter allDocs = new TermsFilter();
                            allDocs.addTerm(new Term("FIELD" + i, String.valueOf(value + PADDING)));
                            filter.add(new FilterClause(allDocs, Occur.MUST));
                        }

                        long searchStart = System.currentTimeMillis();
                        searcher.search(query, new Collector() {
                            public void setScorer(Scorer scorer)
                            {
                            }

                            public boolean acceptsDocsOutOfOrder()
                            {
                                return true;
                            }

                            public void collect(int doc)
                            {
                            }

                            public void setNextReader(IndexReader reader, int docBase)
                            {
                            }
                        });
                        sumSearch += (System.currentTimeMillis() - searchStart);
                    }
                    Document doc = new Document();
                    for (int i = 0; i < NUM_FIELDS; i++)
                    {
                        doc.add(new Field("FIELD" + i, String.valueOf(count + PADDING),
                                Store.NO, Index.ANALYZED));
                    }

                    indexReader.close();
                    searcher.close();

                    long indexStart = System.currentTimeMillis();
                    indexWriter.addDocument(doc);
                    sumIndexing += System.currentTimeMillis() - indexStart;

                    count++;
                    System.out.println("added " + count + " docs");

                    System.out.println("Rate: " + ((double) count)
                            / (((double) (System.currentTimeMillis() - startTime)) / 1000d)
                            + " docs per sec");
                    System.out.println("avg search: " + (sumSearch / count) + " ms; avg indexing: "
                            + (sumIndexing / count) + " ms; avg reader reopen: "
                            + (sumReopenReader / count) + " ms");

                    System.out.println("-----------------------");
                }
            }
            finally
            {
                if (indexReader != null)
                {
                    indexReader.close();
                }
                indexWriter.close();
            }
        }
    }
}
