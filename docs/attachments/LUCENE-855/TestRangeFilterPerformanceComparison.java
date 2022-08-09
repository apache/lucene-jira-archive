package org.apache.lucene.misc;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.*;

import junit.framework.TestCase;

import org.apache.lucene.misc.ChainedFilter;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.RangeFilter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

/**
 */
public class TestRangeFilterPerformanceComparison extends TestCase {

    public static final long INTERVAL = 5 * 365 * 24 * 60 * 60 * 1000L;
    static Date endInterval = new Date();
    static Date startInterval = new Date(endInterval.getTime() - (INTERVAL));
    
    public void testPerformance() throws IOException {
        RAMDirectory ramDir = new RAMDirectory();
        Random r = new Random(1);
        
//        Directory ramDir = FSDirectory.getDirectory(new File("/home/andy/tmp/dateindex"));
        
        final Map testsResults = new HashMap();

        IndexWriter writer = new IndexWriter(ramDir, new KeywordAnalyzer(), true);
        
        System.out.println("Start interval: " + startInterval.toString());
        System.out.println("End interval: " + endInterval.toString());
                
        System.out.println("Creating RAMDirectory index...");
        for (int i=0; i<100000; i++) {
            long newInterval = Math.round(r.nextDouble() * INTERVAL);
            
            Date curDate = new Date(startInterval.getTime() + newInterval);
            String dateStr = DateTools.dateToString(curDate, Resolution.MINUTE);
            
            Document document = new Document();
            
            document.add(new Field("id", String.valueOf(i), Store.YES, Index.NO));
            document.add(new Field("date", dateStr, Store.YES, Index.UN_TOKENIZED));
            
            writer.addDocument(document);
        }
        
        writer.optimize();
        writer.close();
        
        IndexReader reader = IndexReader.open(ramDir);
        Searcher searcher = new IndexSearcher(reader);
        
        String type;
        
        System.out.println("Reader opened with " + reader.maxDoc() + " documents.  Creating RangeFilters...");

        
        type = "RangeFilter";
        new Benchmark(searcher, reader, type , "MatchAllDocsQuery", testsResults) {
            public Filter getFilter(String field, String start, String end) {
                return new RangeFilter("date", start, end, true, true);
            }            
            public Query getQuery(Filter filter, Term term) {
                return new MatchAllDocsQuery();
            }
        }.go();
        
        
        new Benchmark(searcher, reader, type , "ConstantScoreQuery", testsResults) {
            public Filter getFilter(String field, String start, String end) {
                return new RangeFilter("date", start, end, true, true);
            }            
            public Query getQuery(Filter filter, Term term) {
                return new ConstantScoreQuery(filter);
            }
        }.go();
        
        new Benchmark(searcher, reader, type, "TermQuery", testsResults) {
            public Filter getFilter(String field, String start, String end) {
                return new RangeFilter("date", start, end, true, true);
            }            
            public Query getQuery(Filter filter, Term term) {
                return new TermQuery(term);
            }
        }.go();
        
        
        /************************************************************************
         *  MemoryCachedRangeFilter
         ************************************************************************/
        new MemoryCachedRangeFilter("date", 0, 1, true, true).warmup(reader);
        type  = "MemoryCachedRangeFilter";

        new Benchmark(searcher, reader , type , "MatchAllDocsQuery", testsResults) {
            public Filter getFilter(String field, String start, String end) {
                return new MemoryCachedRangeFilter("date", Long.parseLong(start), Long.parseLong(end), true, true);
            }            
            public Query getQuery(Filter filter, Term term) {
                return new MatchAllDocsQuery();
            }
        }.go();

        new Benchmark(searcher, reader, type, "ConstantScoreQuery" ,testsResults) {
            public Filter getFilter(String field, String start, String end) {
                return new MemoryCachedRangeFilter("date", Long.parseLong(start), Long.parseLong(end), true, true);
            }            
            public Query getQuery(Filter filter, Term term) {
                return new ConstantScoreQuery(filter);
            }
        }.go();

        new Benchmark(searcher, reader, type, "TermQuery", testsResults) {
            public Filter getFilter(String field, String start, String end) {
                return new MemoryCachedRangeFilter("date", Long.parseLong(start), Long.parseLong(end), true, true);
            }            
            public Query getQuery(Filter filter, Term term) {
                return new TermQuery(term);
            }
        }.go();
        
        
        /************************************************************************
         *  ChainedFilter (MemoryCachedRangeFilter)
         ************************************************************************/
        new MemoryCachedRangeFilter("date", 0, 1, true, true).warmup(reader);

        type = "Chained MemoryCachedRangeFilter";
        new Benchmark(searcher, reader, type , "MatchAllDocsQuery", testsResults) {
            public Filter getFilter(String field, String start, String end) {
                Filter[] filters = { new MemoryCachedRangeFilter("date", Long.parseLong(start), Long.parseLong(end), true, true)};
                return new ChainedFilter(filters);
            }            
            public Query getQuery(Filter filter, Term term) {
                return new MatchAllDocsQuery();
            }
        }.go();

        new Benchmark(searcher, reader, type, "ConstantScoreQuery", testsResults) {
            public Filter getFilter(String field, String start, String end) {
                Filter filters[] =  {new MemoryCachedRangeFilter("date", Long.parseLong(start), Long.parseLong(end), true, true)};
                return new ChainedFilter(filters);
            }            
            public Query getQuery(Filter filter, Term term) {
                return new ConstantScoreQuery(filter);
            }
        }.go();

        new Benchmark(searcher, reader,type, "TermQuery",testsResults) {
            public Filter getFilter(String field, String start, String end) {
                Filter filters[] =  {new MemoryCachedRangeFilter("date", Long.parseLong(start), Long.parseLong(end), true, true)};
                return new ChainedFilter(filters);
            }            
            public Query getQuery(Filter filter, Term term) {
                return new TermQuery(term);
            }
        }.go();
        

        //By Running the query once we will prime the Field Cache and get it ready 
        FieldCacheRangeFilter warmfilter = new FieldCacheRangeFilter("date", 0L, 1L, true, true);
        warmfilter.bits(reader);

        /************************************************************************
         *  FieldCacheRangeFilter
         ************************************************************************/
        type ="FieldCacheRangeFilter";
        new Benchmark(searcher, reader, type, "MatchAllDocsQuery", testsResults) {
            public Filter getFilter(String field, String start, String end) {
                return new FieldCacheRangeFilter("date", Long.parseLong(start), Long.parseLong(end), true, true);
            }
            public Query getQuery(Filter filter, Term term) {
                return new MatchAllDocsQuery();
            }
        }.go();

        new Benchmark(searcher, reader, type, "ConstantScoreQuery", testsResults) {
            public Filter getFilter(String field, String start, String end) {
                return new FieldCacheRangeFilter("date", Long.parseLong(start), Long.parseLong(end), true, true);
            }
            public Query getQuery(Filter filter, Term term) {
                return new ConstantScoreQuery(filter);
            }
        }.go();

        new Benchmark(searcher, reader, type, "TermQuery", testsResults) {
            public Filter getFilter(String field, String start, String end) {
                return new FieldCacheRangeFilter("date", Long.parseLong(start), Long.parseLong(end), true, true);
            }
            public Query getQuery(Filter filter, Term term) {
                return new TermQuery(term);
            }
        }.go();
        
        /************************************************************************
         *  Chained FieldCacheRangeFilter
         ************************************************************************/
        type = "Chained FieldCacheRangeFilter";
        new Benchmark(searcher, reader, type,"MatchAllDocsQuery", testsResults) {
            public Filter getFilter(String field, String start, String end) {
                Filter[] filters = {new FieldCacheRangeFilter("date", Long.parseLong(start), Long.parseLong(end), true, true)};
                return new ChainedFilter(filters);
            }
            public Query getQuery(Filter filter, Term term) {
                return new MatchAllDocsQuery();
            }
        }.go();
        
        new Benchmark(searcher, reader,type,"ConstantScoreQuery", testsResults) {
            public Filter getFilter(String field, String start, String end) {
                Filter filters[] = {new FieldCacheRangeFilter("date", Long.parseLong(start), Long.parseLong(end), true, true)};
                return new ChainedFilter(filters);
            }
            public Query getQuery(Filter filter, Term term) {
                return new ConstantScoreQuery(filter);
            }
        }.go();

        new Benchmark(searcher, reader, type, "TermQuery", testsResults) {
            public Filter getFilter(String field, String start, String end) {
                Filter  filters[] = { new FieldCacheRangeFilter("date", Long.parseLong(start), Long.parseLong(end), true, true)};
                return new ChainedFilter(filters);
            }
            public Query getQuery(Filter filter, Term term) {
                return new TermQuery(term);
            }
        }.go();
        
        
        
        //Will print all the results
        Iterator it = testsResults.entrySet().iterator();
        it = testsResults.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Map values = (Map)entry.getValue();
            Iterator it2 =values.entrySet().iterator();
            System.out.println("\n" + entry.getKey() + "\n");
            while (it2.hasNext()) {
                Map.Entry entry2 = (Map.Entry) it2.next();
                System.out.println(entry2.getKey() );
                System.out.println(entry2.getValue());
            }
        }
        
    }

    
    public abstract static class Benchmark {
        Searcher searcher;
        IndexReader reader;
        String test; 
        String type;
        Map testResults;
        Random r = new Random(1);

        public Benchmark(Searcher searcher, IndexReader reader, String type, String test, Map testResults) throws IOException {
            this.searcher = searcher;
            this.reader = reader;
            this.type   = type;
            this.test   = test;
            this.testResults = testResults;
        }
        
        public abstract Filter getFilter(String field, String start, String end);
        public abstract Query getQuery(Filter filter, Term term);
        
        public void go() throws IOException {
            long s = System.currentTimeMillis();
            long ss = 0;
            long bitsTime = 0;
            long searchTime = 0;
            for (int i=0; i<100; i++) {
                // Generate random date interval
                Date date1 = new Date(startInterval.getTime() + Math.round(r.nextDouble() * INTERVAL));
                Date date2 = new Date(startInterval.getTime() + Math.round(r.nextDouble() * INTERVAL));
                
                String start, end;
                if (date1.after(date2)) {
                    start = DateTools.dateToString(date2, Resolution.MINUTE); 
                    end = DateTools.dateToString(date1, Resolution.MINUTE); 
                } else {
                    start = DateTools.dateToString(date1, Resolution.MINUTE); 
                    end = DateTools.dateToString(date2, Resolution.MINUTE); 
                }
                
                ss = System.currentTimeMillis();
                Filter filter = getFilter("date", start, end);
                filter.bits(reader);
                bitsTime += System.currentTimeMillis() - ss;
                
                Query query = getQuery(filter, new Term("id", String.valueOf(i)));

                ss = System.currentTimeMillis();
                Hits hits = searcher.search(query, filter);
                searchTime += System.currentTimeMillis() - ss;
//                System.out.println(hits.length());
            }
            
            Map tests = (Map) testResults.get(test);
            if (tests == null) {
                tests = new HashMap();
                testResults.put(test, tests);
            }
            
            long e = System.currentTimeMillis() - s;
            
            tests.put(type,  "  * Total: " + e + "ms\n" +
                             "  * Bits: " + bitsTime + "ms\n" +
                             "  * Search: " + searchTime + "ms");
            
        }
    }
    
}
