/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package dsmiley;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.FilterDirectoryReader;
import org.apache.lucene.index.FilterLeafReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TieredMergePolicy;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.IOUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MyBenchmark {

  private static final int numFields = 150;
  private static final int numSegments = 30;
  private static final String[] fields;

  static {
    fields = new String[numFields];
    for (int i = 0; i < numFields; i++) {
      fields[i] = "fld" + i;
    }
  }

  @Benchmark
  @BenchmarkMode(Mode.AverageTime)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  public void bench(Blackhole bh, BenchmarkState state) throws IOException {
    for (int i = 0; i < fields.length; i++) {
      String field = i == 2 ? "nonexistentField" : fields[i];
      bh.consume(concurrentHashMap(state, field));//change me
    }
  }

  CollectionStatistics multiFieldsImpl(BenchmarkState state, String field) throws IOException {
    final int docCount;
    final long sumTotalTermFreq;
    final long sumDocFreq;

    assert field != null;

    Terms terms = MultiFields.getTerms(state.reader, field);
    if (terms == null) {
      docCount = 0;
      sumTotalTermFreq = 0;
      sumDocFreq = 0;
    } else {
      docCount = terms.getDocCount();
      sumTotalTermFreq = terms.getSumTotalTermFreq();
      sumDocFreq = terms.getSumDocFreq();
    }

    return new CollectionStatistics(field, state.reader.maxDoc(), docCount, sumTotalTermFreq, sumDocFreq);
  }

  CollectionStatistics sharedMultiFieldsImpl(BenchmarkState state, String field) throws IOException {
    final int docCount;
    final long sumTotalTermFreq;
    final long sumDocFreq;

    assert field != null;

    Terms terms = state.multiFields.terms(field);
    if (terms == null) {
      docCount = 0;
      sumTotalTermFreq = 0;
      sumDocFreq = 0;
    } else {
      docCount = terms.getDocCount();
      sumTotalTermFreq = terms.getSumTotalTermFreq();
      sumDocFreq = terms.getSumDocFreq();
    }

    return new CollectionStatistics(field, state.reader.maxDoc(), docCount, sumTotalTermFreq, sumDocFreq);
  }

  CollectionStatistics rawImpl(BenchmarkState state, String field) throws IOException {
    long docCount = 0;
    long sumTotalTermFreq = 0;
    long sumDocFreq = 0;
    for (LeafReaderContext leaf : state.reader.leaves()) {
      final Terms terms = leaf.reader().terms(field);
      if (terms == null) {
        continue;
      }
      docCount =+ terms.getDocCount();
      sumTotalTermFreq += terms.getSumTotalTermFreq();
      sumDocFreq += terms.getSumDocFreq();
    }
    return new CollectionStatistics(field, state.reader.maxDoc(), docCount, sumTotalTermFreq, sumDocFreq);
  }

  CollectionStatistics concurrentHashMap(BenchmarkState state, String field) throws IOException {
    return state.concurrentHashMap.computeIfAbsent(field, f -> {
      long docCount = 0;
      long sumTotalTermFreq = 0;
      long sumDocFreq = 0;
      try {
        for (LeafReaderContext leaf : state.reader.leaves()) {
          final Terms terms = leaf.reader().terms(field);
          if (terms == null) {
            continue;
          }
          docCount =+ terms.getDocCount();
          sumTotalTermFreq += terms.getSumTotalTermFreq();
          sumDocFreq += terms.getSumDocFreq();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return new CollectionStatistics(field, state.reader.maxDoc(), docCount, sumTotalTermFreq, sumDocFreq);
    });
  }

  @State(Scope.Benchmark)
  public static class BenchmarkState {
    RAMDirectory ramDirectory;
    IndexReader reader;

    Fields multiFields;
    ConcurrentHashMap<String, CollectionStatistics> concurrentHashMap = new ConcurrentHashMap<>();

    @Setup
    public void setup() throws IOException {
      ramDirectory = new RAMDirectory();
      IndexWriterConfig conf = new IndexWriterConfig();
      conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
      conf.setMergePolicy(new TieredMergePolicy().setSegmentsPerTier(numSegments + 1).setMaxMergeAtOnce(numSegments + 1));
      //conf.setMergeScheduler(NoMergeScheduler.INSTANCE); TODO report hang bug in waitForMerges()
      // one document per segment; details don't matter for this benchmark so long as we have
      //   expected segment count and number of fields
      try (IndexWriter iw = new IndexWriter(ramDirectory, conf)) {
        for (int segIdx = 0; segIdx < numSegments; segIdx++) {
          iw.addDocument(
                  Arrays.stream(fields)
                          .map(f -> new StringField(f, "someterm", Field.Store.NO))
                          .collect(Collectors.toList())
          );
          iw.commit();
        }
      }
      reader = new TermsHashMapDirectoryWrapper(DirectoryReader.open(ramDirectory));
      if (reader.leaves().size() != numSegments) {
        throw new AssertionError("wrong segment count");
      }
      multiFields = MultiFields.getFields(reader);
    }

    @TearDown
    public void teardown() throws IOException {
      IOUtils.close(reader, ramDirectory);
    }
  }

  public static class TermsHashMapDirectoryWrapper extends FilterDirectoryReader {

    public TermsHashMapDirectoryWrapper(DirectoryReader in) throws IOException {
      super(in, new MySubReaderWrapper());
    }

    @Override
    protected DirectoryReader doWrapDirectoryReader(DirectoryReader in) throws IOException {
      return new TermsHashMapDirectoryWrapper(in);
    }

    @Override
    public CacheHelper getReaderCacheHelper() {
      return in.getReaderCacheHelper();
    }

    private static class MySubReaderWrapper extends SubReaderWrapper {
      @Override
      public LeafReader wrap(LeafReader reader) {
        try {
          return new MyLeafReader(reader);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }

    private static class MyLeafReader extends FilterLeafReader {
      private HashMap<String, Terms> fieldToTerms = new HashMap<>();
      MyLeafReader(LeafReader reader) throws IOException {
        super(reader);
        for (FieldInfo fieldInfo : reader.getFieldInfos()) {
          if (fieldInfo.getIndexOptions() != IndexOptions.NONE) {
            fieldToTerms.put(fieldInfo.name, reader.terms(fieldInfo.name));
          }
        }
      }

      @Override
      public Terms terms(String field) throws IOException {
        return fieldToTerms.get(field);
      }

      // this impl does not change deletes or data so we can delegate the
      // CacheHelpers
      @Override
      public CacheHelper getReaderCacheHelper() {
        return in.getReaderCacheHelper();
      }

      @Override
      public CacheHelper getCoreCacheHelper() {
        return in.getCoreCacheHelper();
      }
    }
  }
}
