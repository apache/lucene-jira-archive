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
package org.apache.lucene.search;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import org.apache.lucene.index.IndexReader.CacheHelper;
import org.apache.lucene.index.IndexReader.CacheKey;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.util.Accountable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.Weigher;

/**
 *
 */
public final class CaffeineQueryCache implements QueryCache, Accountable {
  private final LoadingCache<CacheKey, Segment> segments;
  private final Weigher<Query, DocIdSet> weigher;
  private final LongAdder missCount;
  private final LongAdder hitCount;
  
  public CaffeineQueryCache() {
    this.segments = Caffeine.newBuilder()
        .weigher((CacheKey key, Segment segment) -> segment.segmentWeight)
        .maximumWeight(1_000_000)
        .build(Segment::new);
    this.weigher = (query, value) -> {
      long bytes = value.ramBytesUsed();
      return (bytes > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) bytes;
    };
    this.missCount = new LongAdder();
    this.hitCount = new LongAdder();
  }

  @Override
  public Weight doCache(Weight weight, QueryCachingPolicy policy) {
    while (weight instanceof CachingWeight) {
      weight = ((CachingWeight) weight).delegate;
    }
    return new CachingWeight(weight, policy);
  }

  @Override
  public long ramBytesUsed() {
    return segments.policy().eviction().get().getMaximum();
  }
  
  final class Segment {
    private final Cache<Query, DocIdSet> mappings;
    private final CacheKey segmentKey;
    
    private volatile int segmentWeight;
    
    public Segment(CacheKey segmentKey) {
      this.segmentKey = requireNonNull(segmentKey);
      this.mappings = Caffeine.newBuilder().build();
    }
    
    DocIdSet computeIfAbsent(Query query, Function<Query, DocIdSet> mappingFunction) {
      boolean[] updated = { false };
      DocIdSet value = mappings.get(query, q -> {
        DocIdSet ids = mappingFunction.apply(q);
        if (ids != null) {
          updated[0] = true;
        }
        return ids;
      });
      
      if (value == null) {
        missCount.increment();
      } else if (updated[0]) {
        notifySegmentCache(query, value);
        missCount.increment();
      } else {
        hitCount.increment();
      }
      return value;
    }

    private void notifySegmentCache(Query query, DocIdSet value) {
      boolean[] updated = { false };
      int weight = weigher.weigh(query, value);
      segments.asMap().computeIfPresent(segmentKey, (key, segment) -> {
        if (segment == this) {
          int newSegmentWeight = (segmentWeight + weight);
          if (newSegmentWeight > 0) {
            segmentWeight = newSegmentWeight;
            updated[0] = true;
          } else {
            mappings.asMap().remove(query, value);
          }
        }
        return segment;
      });
      
      if (!updated[0]) {
        // discard value
      }
    }
  }
  
  private final class CachingWeight extends ConstantScoreWeight {
    private final QueryCachingPolicy policy;
    private final Weight delegate;
    
    CachingWeight(Weight delegate, QueryCachingPolicy policy) {
      super(delegate.getQuery(), 1f);
      this.delegate = requireNonNull(delegate);
      this.policy = policy;
    }
    
    @Override
    public Matches matches(LeafReaderContext context, int doc) throws IOException {
      return delegate.matches(context, doc);
    }

    @Override
    public boolean isCacheable(LeafReaderContext ctx) {
      return delegate.isCacheable(ctx);
    }

    @Override
    public Scorer scorer(LeafReaderContext context) throws IOException {
      ScorerSupplier scorerSupplier = scorerSupplier(context);
      return (scorerSupplier == null) ? null : scorerSupplier.get(Long.MAX_VALUE);
    }
    
    @Override
    public ScorerSupplier scorerSupplier(LeafReaderContext context) throws IOException {
      CacheHelper helper = context.reader().getCoreCacheHelper();
      CacheKey key = helper.getKey();
      return null;
    }
  }
}
