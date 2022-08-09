package org.apache.lucene.codec;

import org.apache.lucene.codecs.bool.Boolean70Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.FixedBitSet;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.LinuxPerfAsmProfiler;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * This benchmark compares latency of different lucene's filter query implementations.
 * Of course there are a lot of trade-offs and pitfalls when you are trying to compare abstract solution
 * in one small benchmark.
 * This benchmark is intended to show the difference for one particular and important e-commerce use case:
 * inventory availability. In this case we have quite huge set of dense columns/fields, with the only flow:
 * filter and sort by availability.
 * <p>
 * In general we have only two approaches to apply filter query: use filter cache or use doc values.
 * Filter cache is fast enough and it keeps filters in quite compact bitset structure. For DocValues
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 2, time = 2)
@Fork(value = 1)
@State(Scope.Benchmark)
public class SynteticDocValuesBench70 {

    private static final String TEMPORARY_PATH = "/tmp/bench";
    private static final String STORE_AS_IS = "store_";
    private static final String BOOLEAN_STORE = "boolean_store_";

    private LeafReaderContext leafReaderContext;
    private DirectoryReader reader;
    private IndexSearcher searcher;

    @Param({"10", "35", "50", "60", "90"})
    private int density;

    private FixedBitSet bitSet;

    private int maxDoc = 2000000;
    private String store;

    public static void main(String[] args) throws Exception {

        Options options = new OptionsBuilder()
                .include(SynteticDocValuesBench70.class.getName())
                .forks(2)
                .verbosity(VerboseMode.NORMAL)
                .resultFormat(ResultFormatType.JSON) // https://jmh.morethan.io in order to visualize this report
                .build();
        new Runner(options).run();
    }

    @Setup
    public void init() throws IOException {
        createIndex();
        FSDirectory directory = FSDirectory.open(new File(TEMPORARY_PATH).toPath());
        reader = DirectoryReader.open(directory);
        searcher = new IndexSearcher(reader);
        List<LeafReaderContext> leaves = reader.leaves();
        leafReaderContext = leaves.get(0);
        maxDoc = reader.numDocs();
        store = getStore(STORE_AS_IS);

        System.out.printf("evalNumbersIterator %s %d%n", store, evalIterator(getResultSetNumbers(store)));
        System.out.printf("evalBooleanIterator %s %d%n", store, evalIterator(getResultSetBoolean("boolean_" + store)));
        System.out.printf("evalDocValIterator  %s %d%n", store, evalDocValues(store));
    }

    /**
     * Creates a small density index with different column variations.
     */
    private void createIndex() throws IOException {
        Random random = new Random();

        FSDirectory directory = FSDirectory.open(new File(TEMPORARY_PATH).toPath());
        IndexWriterConfig conf = new IndexWriterConfig(null);
        conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        conf.setUseCompoundFile(false);
        conf.setMaxBufferedDocs(500000);
        conf.setRAMBufferSizeMB(1024);
        conf.setCodec(new Boolean70Codec());
        IndexWriter writer = new IndexWriter(directory, conf);

        bitSet = new FixedBitSet(maxDoc);
        float probability = density / 100f;
        for (int i = 0; i < maxDoc; i++) {
            if (random.nextFloat() < probability) {
                bitSet.set(i);
            }
        }

        for (int i = 0; i < maxDoc; i++) {
            Document doc = new Document();
            doc.add(new StringField("id", "" + i, Field.Store.NO));
            int value = bitSet.get(i) ? 1 : 0;
            if (value == 0 && random.nextBoolean()) {
                continue; //to check different compression
            }
            doc.add(new NumericDocValuesField(STORE_AS_IS + density, value));
            doc.add(new NumericDocValuesField(BOOLEAN_STORE + density, value));
            writer.addDocument(doc);
        }
//        writer.forceMerge(1);
        writer.commit();
    }

    private String getStore(String storePrefix) {
        return storePrefix + density;
    }

    private int evalIterator(DocIdSetIterator iterator) throws IOException {
        int result = 0;
        while (iterator.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
            result++;
        }
        return result;
    }

    private int evalDocValues(String columnName) throws IOException {
        int result = 0;
        NumericDocValues numericDocValues = DocValues.getNumeric(leafReaderContext.reader(), columnName);
        while (numericDocValues.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
            if (numericDocValues.longValue() == 1)
                result += 1;
        }
        return result;
    }

    /**
     * In almost all cases Solr uses FixedBitSet in filter cache to keep store availability filters.
     * This data structure is dense and fast due to optimizations at CPU level(it checks the whole long in 1 popcnt
     * instruction)
     * So we can use this as a baseline, i.e. as the fastest implementation.
     */
    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public int baseline() {
        int result = 0;
        //actually this is how filter works in Solr. It iterates through all values and delegates search to downstream collectors
        for (int ord = bitSet.nextSetBit(0); ord != DocIdSetIterator.NO_MORE_DOCS; ord = ord + 1 >= bitSet.length() ? DocIdSetIterator.NO_MORE_DOCS : bitSet.nextSetBit(ord + 1)) {
            result++;
        }
        return result;
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public int docValuesNumbersQuery() throws IOException {
        return evalIterator(getResultSetNumbers(store));
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public int docValuesBooleanQuery() throws IOException {
        return evalIterator(getResultSetBoolean("boolean_" + store));
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public int docValuesRaw() throws IOException {
        return evalDocValues(store);
    }

    private DocIdSetIterator getResultSetNumbers(String storeName) throws IOException {
        Query query = new DocValuesNumbersQuery(storeName, 1L);
        query = query.rewrite(reader);
        Weight weight = query.createWeight(searcher, false, 1);
        Scorer scorer = weight.scorer(leafReaderContext);
        return scorer.iterator();
    }

    private DocIdSetIterator getResultSetBoolean(String storeName) throws IOException {
        Query query = new DocValuesBooleanQuery(storeName);
        query = query.rewrite(reader);
        Weight weight = query.createWeight(searcher, false, 1);
        Scorer scorer = weight.scorer(leafReaderContext);
        return scorer.iterator();
    }
}