package org.apache.lucene.codec;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.codecs.DocValuesFormat;
import org.apache.lucene.codecs.bool.BooleanDocValuesFormat;
import org.apache.lucene.codecs.lucene80.Lucene80Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.DocValuesBooleanQuery;
import org.apache.lucene.search.DocValuesNumbersQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.FixedBitSet;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.LinuxPerfAsmProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

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
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 3, time = 2)
@Fork(1)
@State(Scope.Benchmark)
public class SynteticDocValuesBench80 {

    private static final String TEMPORARY_PATH = "/tmp/bench";

    private DirectoryReader reader;
    private IndexSearcher searcher;
    private IndexWriter writer;

    private final Random random = new Random();

    @Param({ "10", "35", "50", "60", "90" })
    private int density = 90;

    //@Param({ "1", "2", "3", "4", "5", "10" })
    private int numSegments = 1;

    @Param({ "dense", "sparse" })
    private String compression;

    private FixedBitSet bitSet;

    private final int maxDoc = 2_000_000;
    private String store, boolean_store;

    public static void main(String[] args) throws Exception {

        Options options = new OptionsBuilder().include(SynteticDocValuesBench80.class.getName())
                                              .forks(1)
                                              .verbosity(VerboseMode.NORMAL)
                                              .resultFormat(ResultFormatType.JSON) // https://jmh.morethan.io in order to visualize this report
                                              //.addProfiler(LinuxPerfAsmProfiler.class)
                                              .build();
        new Runner(options).run();
    }

    @Setup
    public void init() throws IOException {
        store = "store_" + density;
        boolean_store = "boolean_store_" + density;

        createIndex();

        FSDirectory directory = FSDirectory.open(new File(TEMPORARY_PATH).toPath());
        reader = DirectoryReader.open(directory);
        searcher = new IndexSearcher(reader);

        /*
        System.out.println("\n******************************************************************");
        System.out.printf("baseline %s %d%n", store, baseline());
        System.out.printf("evalNumbersQuery %s %d%n", store, docValuesNumbersQuery());
        System.out.printf("evalBooleanQuery %s %d%n", store, docValuesBooleanQuery());
        System.out.printf("evalDvIterator  %s %d%n", store, docValuesRaw());
        System.out.printf("evalBooleanDvIterator  %s %d%n", store, booleanDocValuesRaw());
        
        //System.out.printf("docValuesBooleanQuery_Lucene80  %s %d%n", store, docValuesBooleanQuery_Lucene80());
        //System.out.printf("docValuesBooleanQuery_Lucene80  %s %d%n", store, docValuesBooleanQuery_Lucene80());
        System.out.println("******************************************************************\n");
        */
    }

    /**
     * Creates a small density index with different column variations.
     */
    private void createIndex() throws IOException {
        FSDirectory directory = FSDirectory.open(new File(TEMPORARY_PATH).toPath());
        IndexWriterConfig conf = new IndexWriterConfig(null).setOpenMode(IndexWriterConfig.OpenMode.CREATE)
                                                            .setCodec(new BooleanCodec())
                                                            .setUseCompoundFile(false)
                                                            .setMaxBufferedDocs(500000)
                                                            .setRAMBufferSizeMB(1024);

        writer = new IndexWriter(directory, conf);

		boolean checkSparseDv = "sparse".equals(compression);

        bitSet = new FixedBitSet(maxDoc);
        float probability = density / 100f;
        for (int i = 0; i < maxDoc; i++) {
            Document doc = new Document();
            doc.add(new StringField("id", String.valueOf(i), Field.Store.NO));

            if (random.nextFloat() < probability) {
                bitSet.set(i);

                doc.add(new NumericDocValuesField(store, 1));
                doc.add(new NumericDocValuesField(boolean_store, 1));
            } else if (!checkSparseDv || random.nextBoolean()) { //to check different compressions (Lucene80DocValuesProducer$SparseNumericDocValues vs Lucene80DocValuesProducer$DenseNumericDocValues)
                doc.add(new NumericDocValuesField(store, 0));
                doc.add(new NumericDocValuesField(boolean_store, 0));
            }

            writer.addDocument(doc);
        }

        writer.forceMerge(numSegments);
        writer.commit();
    }

    /**
     * In almost all cases Solr uses FixedBitSet in filter cache to keep store availability filters.
     * This data structure is dense and fast due to optimizations at CPU level(it checks the whole long in 1 popcnt
     * instruction)
     * So we can use this as a baseline, i.e. as the fastest implementation.
     */
    @Benchmark
    public int baseline() {
        int result = 0;
        //actually this is how filter works in Solr. It iterates through all values and delegates search to downstream collectors
        for (int ord = bitSet.nextSetBit(0); ord != DocIdSetIterator.NO_MORE_DOCS; ord = ord + 1 >= bitSet.length() ? DocIdSetIterator.NO_MORE_DOCS
                : bitSet.nextSetBit(ord + 1)) {
            result++;
        }
        return result;
    }

    @Benchmark
    public int docValuesNumbersQuery() throws IOException {
        return evalQuery(new DocValuesNumbersQuery(store, 1L));
    }

    @Benchmark
    public int docValuesBooleanQuery() throws IOException {
        return evalQuery(new DocValuesBooleanQuery(boolean_store));
    }

    //@Benchmark
    public int docValuesNumbersQuery_Boolean() throws IOException {
        return evalQuery(new DocValuesNumbersQuery(boolean_store, 1L));
    }

    //@Benchmark
    public int docValuesBooleanQuery_Lucene80() throws IOException {
        return evalQuery(new DocValuesBooleanQuery(store));
    }

    @Benchmark //usually shows almost the same results as docValuesBooleanQuery()
    public int booleanDocValuesRaw() throws IOException {
        return evalDocValues(boolean_store);
    }

    @Benchmark
    public int docValuesRaw() throws IOException {
        return evalDocValues(store);
    }

    //@Benchmark
    public long testInPlaceUpdates_Lucene80() throws IOException {
        return testInPlaceUpdates(store);
    }

    //@Benchmark
    public long testInPlaceUpdates_Boolean() throws IOException {
        return testInPlaceUpdates(boolean_store);
    }

	private long testInPlaceUpdates(String field) throws IOException {
		long result = 0;
		for (int i = 0; i < maxDoc; i++) {
			result += writer.updateNumericDocValue(new Term("id", String.valueOf(random.nextInt(maxDoc))), field, random.nextInt(2));
		}

		// writer.forceMerge(numSegments);
		writer.commit();

		return result;
	}

    private int evalQuery(Query query) throws IOException {
        query = query.rewrite(reader);
        Weight weight = query.createWeight(searcher, ScoreMode.COMPLETE_NO_SCORES, 1);
        int result = 0;
        for (LeafReaderContext leafReaderContext : reader.leaves()) {
            Scorer scorer = weight.scorer(leafReaderContext);
            DocIdSetIterator iterator = scorer.iterator();
            while (iterator.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                result++;
            }
        }
        return result;
    }

    private int evalDocValues(String columnName) throws IOException {
        int result = 0;
        for (LeafReaderContext leafReaderContext : reader.leaves()) {
            NumericDocValues numericDocValues = DocValues.getNumeric(leafReaderContext.reader(), columnName);
            while (numericDocValues.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                if (numericDocValues.longValue() == 1)
                    result++;
            }
        }
        return result;
    }

    /**
     * Simple example codec with ability to create {@link BooleanDocValuesFormat} as default implementation always
     * returns "Lucene80" format for any field.
     */
    public static class BooleanCodec extends Lucene80Codec {

        private final DocValuesFormat booleanDVFormat = DocValuesFormat.forName("Boolean");

        @Override
        public DocValuesFormat getDocValuesFormatForField(String field) {
            if (field.startsWith("boolean_")) {
                return booleanDVFormat;
            } else {
                return super.getDocValuesFormatForField(field);
            }
        }
    }
}
