package org.apache.lucene.search.trie;

/**
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
import java.util.Date;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.ToStringUtils;


/**
 * Implementation of a Lucene {@link Filter} that implements trie-based range filtering.
 * This filter depends on a specific structure of terms in the index that can only be created
 * by {@link TrieUtils} methods.
 * For more information, how the algorithm works, see the package description {@link org.apache.lucene.search.trie}.
 */
public final class TrieRangeFilter extends Filter {

  /**
   * Universal constructor (expert use only): Uses already trie-converted min/max values.
   * You can set <code>min</code> or <code>max</code> (but not both) to <code>null</code> to leave one bound open.
   * With <code>minInclusive</code> and <code>maxInclusive</code> can be choosen, if the corresponding
   * bound should be included or excluded from the range.
   */
  public TrieRangeFilter(final String[] fields, final int precisionStep,
    Long min, Long max, final boolean minInclusive, final boolean maxInclusive
  ) {
    if (min==null && max==null) throw new IllegalArgumentException("The min and max values cannot be both null.");
    this.fields=fields;
    this.precisionStep=precisionStep;
    this.min=min;
    this.max=max;
    this.minInclusive=minInclusive;
    this.maxInclusive=maxInclusive;
    this.minUnconverted=min;
    this.maxUnconverted=max;
  }

  /**
   * Generates a trie filter using the supplied field with range bounds in integer form (long).
   * You can set <code>min</code> or <code>max</code> (but not both) to <code>null</code> to leave one bound open.
   * With <code>minInclusive</code> and <code>maxInclusive</code> can be choosen, if the corresponding
   * bound should be included or excluded from the range.
   */
  public TrieRangeFilter(final String field, final String lowerPrecisionField, final int precisionStep,
    final Long min, final Long max, final boolean minInclusive, final boolean maxInclusive
  ) {
    this(
      new String[]{field, lowerPrecisionField==null ? (field+TrieUtils.LOWER_PRECISION_FIELD_NAME_SUFFIX) : lowerPrecisionField},
      precisionStep,min,max,minInclusive,maxInclusive
    );
  }

  /**
   * Generates a trie filter using the supplied field with range bounds in integer form (long).
   * You can set <code>min</code> or <code>max</code> (but not both) to <code>null</code> to leave one bound open.
   * With <code>minInclusive</code> and <code>maxInclusive</code> can be choosen, if the corresponding
   * bound should be included or excluded from the range.
   */
  public TrieRangeFilter(final String field, final int precisionStep,
    final Long min, final Long max, final boolean minInclusive, final boolean maxInclusive
  ) {
    this(
      new String[]{field, field+TrieUtils.LOWER_PRECISION_FIELD_NAME_SUFFIX},
      precisionStep,min,max,minInclusive,maxInclusive
    );
  }

  //@Override
  public String toString() {
    return toString(null);
  }

  public String toString(final String field) {
    /*final StringBuffer sb=new StringBuffer();
    if (!this.field.equals(field)) sb.append(this.field).append(':');
    return sb.append(minInclusive ? '[' : '{')
      .append((minUnconverted==null) ? "*" : minUnconverted.toString())
      .append(" TO ")
      .append((maxUnconverted==null) ? "*" : maxUnconverted.toString())
      .append(maxInclusive ? ']' : '}').toString();
    */ return "dummy";
  }

  /**
   * Two instances are equal if they have the same trie-encoded range bounds, same field, and same variant.
   * If one of the instances uses an exclusive lower bound, it is equal to a range with inclusive bound,
   * when the inclusive lower bound is equal to the incremented exclusive lower bound of the other one.
   * The same applys for the upper bound in other direction.
   */
  //@Override
  /*public final boolean equals(final Object o) {
    if (o instanceof TrieRangeFilter) {
      TrieRangeFilter q=(TrieRangeFilter)o;
      // trieVariants are singleton per type, so no equals needed.
      return (field==q.field && min.equals(q.min) && max.equals(q.max) && trieVariant==q.trieVariant);
    } else return false;
  }*/

  //@Override
  /*public final int hashCode() {
    // the hash code uses from the variant only the number of bits, as this is unique for the variant
    return field.hashCode()+(min.hashCode()^0x14fa55fb)+(max.hashCode()^0x733fa5fe)+(trieVariant.TRIE_BITS^0x64365465);
  }*/
  
  /** Marks documents in a specific range. Code borrowed from original RangeFilter and simplified (and returns number of terms) */
  private int setBits(
    final IndexReader reader, final TermDocs termDocs, final OpenBitSet bits, 
    String field, String lowerTerm, String upperTerm
  ) throws IOException {
    System.out.println(Long.toHexString(TrieUtils.prefixCodedToLong(lowerTerm)^0x8000000000000000L)+".."+Long.toHexString(TrieUtils.prefixCodedToLong(upperTerm)^0x8000000000000000L));
    int count=0;
    final int len=lowerTerm.length();
    final TermEnum enumerator = reader.terms(new Term(field, lowerTerm));
    try {
      do {
        final Term term = enumerator.term();
        if (term!=null && term.field()==field) {
          // break out when upperTerm reached or length of term is different
          final String t=term.text();
          if (len!=t.length() || t.compareTo(upperTerm)>0) break;
          // we have a good term, find the docs
          count++;
          termDocs.seek(enumerator);
          while (termDocs.next()) bits.set(termDocs.doc());
        } else break;
      } while (enumerator.next());
    } finally {
      enumerator.close();
    }
    return count;
  }

  /** Splits range recursively (and returns number of terms) */
  private int splitRange(
    final IndexReader reader, final TermDocs termDocs, final OpenBitSet bits,
    final long minBound, final boolean minBoundOpen, final long maxBound, final boolean maxBoundOpen,
    final int shift
  ) throws IOException {
    int count=0;
    final String field = fields[Math.min(fields.length-1, shift/precisionStep)].intern();
    
    // calculate new bounds for inner precision
    final long diff = 1L << (shift+precisionStep),
      mask = diff-1L,
      nextMinBound = (minBoundOpen ? Long.MIN_VALUE : (minBound + diff)) & ~mask,
      nextMaxBound = (maxBoundOpen ? Long.MAX_VALUE : (maxBound - diff)) & ~mask;

    if (shift+precisionStep>63 || nextMinBound>=nextMaxBound) {
      // we are in the lowest precision or the next precision is not available
      count+=setBits(
        reader, termDocs, bits, field,
        TrieUtils.longToPrefixCoded(minBound, shift),
        TrieUtils.longToPrefixCoded(maxBound, shift)
      );
    } else {
      if (!minBoundOpen) {
        count+=setBits(reader, termDocs, bits, field,
          TrieUtils.longToPrefixCoded(minBound, shift),
          TrieUtils.longToPrefixCoded((nextMinBound - diff) | mask, shift)
        );
      }
      if (!maxBoundOpen) {
        count+=setBits(reader, termDocs, bits, field,
          TrieUtils.longToPrefixCoded((nextMaxBound + diff) & ~mask, shift),
          TrieUtils.longToPrefixCoded(maxBound, shift)
        );
      }
      count+=splitRange(
        reader,termDocs,bits,
        nextMinBound,minBoundOpen,
        nextMaxBound,maxBoundOpen,
        shift+precisionStep
      );
    }
    return count;
  }

  /**
   * Returns a DocIdSet that provides the documents which should be permitted or prohibited in search results.
   */
  //@Override
  public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
    // calculate the upper and lower bounds respecting the inclusive and null values.
    long minBound=(this.min==null) ? Long.MIN_VALUE : (
      minInclusive ? this.min.longValue() : (this.min.longValue()+1L)
    );
    long maxBound=(this.max==null) ? Long.MAX_VALUE : (
      maxInclusive ? this.max.longValue() : (this.max.longValue()-1L)
    );
    
    if (minBound > maxBound) {
      // shortcut, no docs will match this
      lastNumberOfTerms=0;
      return DocIdSet.EMPTY_DOCIDSET;
    } else {
      final OpenBitSet bits = new OpenBitSet(reader.maxDoc());
      final TermDocs termDocs = reader.termDocs();
      try {
        lastNumberOfTerms=splitRange(
          reader,termDocs,bits,
          minBound, minBound==Long.MIN_VALUE, maxBound, maxBound==Long.MAX_VALUE,
          0 /* start with no shift */
        );
      } finally {
        termDocs.close();
      }
      return bits;
    }
  }
  
  /**
   * EXPERT: Return the number of terms visited during the last execution of {@link #getDocIdSet}.
   * This may be used for performance comparisons of different trie variants and their effectiveness.
   * This method is not thread safe, be sure to only call it when no query is running!
   * @throws IllegalStateException if {@link #getDocIdSet} was not yet executed.
   */
  public int getLastNumberOfTerms() {
    if (lastNumberOfTerms < 0) throw new IllegalStateException();
    return lastNumberOfTerms;
  }
  
  /** Returns this range filter as a query.
   * Using this method, it is possible to create a Query using <code>new TrieRangeFilter(....).asQuery()</code>.
   * This is a synonym for wrapping with a {@link ConstantScoreQuery},
   * but this query returns a better <code>toString()</code> variant.
   */
  public Query asQuery() {
    return new ConstantScoreQuery(this) {
    
      /** this instance return a nicer String variant than the original {@link ConstantScoreQuery} */
      //@Override
      public String toString(final String field) {
        // return a more convenient representation of this query than ConstantScoreQuery does:
        return ((TrieRangeFilter) filter).toString(field)+ToStringUtils.boost(getBoost());
      }

    };
  }

  // members
  private final String[] fields;
  private final int precisionStep;
  private final Long min,max;
  private final boolean minInclusive,maxInclusive;
  private Object minUnconverted,maxUnconverted;
  private int lastNumberOfTerms=-1;
}
