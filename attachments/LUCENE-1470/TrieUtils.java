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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.search.ExtendedFieldCache;

/**
 * This is a helper class to construct the trie-based index entries for numerical values. TODO!
 */
public final class TrieUtils {

  /** The "helper" field containing the lower precision terms is the original fieldname with this appended. */
  public static final String LOWER_PRECISION_FIELD_NAME_SUFFIX="#trie";

  /** Numbers are stored at lower precision by shifting off lower bits.  The shift count is
   * stored as SHIFT_START+shift in the first character */
  public static final char SHIFT_START_LONG = (char)0x20;
  public static final char SHIFT_START_INT  = (char)0x60;

  /**
   * A parser instance for filling a {@link ExtendedFieldCache}, that parses prefix encoded fields as longs.
   */
  public static final ExtendedFieldCache.LongParser FIELD_CACHE_LONG_PARSER=new ExtendedFieldCache.LongParser(){
    public final long parseLong(final String val) {
      return prefixCodedToLong(val);
    }
  };
  
  /**
   * A parser instance for filling a {@link FieldCache}, that parses prefix encoded fields as ints.
   */
  public static final FieldCache.IntParser FIELD_CACHE_INT_PARSER=new FieldCache.IntParser(){
    public final int parseInt(final String val) {
      return prefixCodedToInt(val);
    }
  };

  /** Returns prefix coded bits after reducing the precision by "shift" bits.
   */
  public static String longToPrefixCoded(final long val, final int shift) {
    int nBits = 64-shift;
    int nChars = (nBits-1)/7 + 1;
    final char[] arr = new char[nChars+1];
    arr[0] = (char)(SHIFT_START_LONG + shift);
    long sortableBits = val ^ 0x8000000000000000L;
    sortableBits >>>= shift;
    while (nChars>=1) {
      // Store 7 bits per character for good efficiency when UTF-8 encoding.
      // The whole number is right-justified so that lucene can prefix-encode
      // the terms more efficiently.
      arr[nChars--] = (char)(sortableBits & 0x7f);
      sortableBits >>>= 7;
    }
    return new String(arr);
  }

  /** Returns prefix coded bits after reducing the precision by "shift" bits.
   */
  public static String intToPrefixCoded(final int val, final int shift) {
    int nBits = 32-shift;
    int nChars = (nBits-1)/7 + 1;
    final char[] arr = new char[nChars+1];
    arr[0] = (char)(SHIFT_START_INT + shift);
    int sortableBits = val ^ 0x80000000;
    sortableBits >>>= shift;
    while (nChars>=1) {
      // Store 7 bits per character for good efficiency when UTF-8 encoding.
      // The whole number is right-justified so that lucene can prefix-encode
      // the terms more efficiently.
      arr[nChars--] = (char)(sortableBits & 0x7f);
      sortableBits >>>= 7;
    }
    return new String(arr);
  }

  /** Returns a long from prefixCoded characters.
   * Rightmost bits will be zero for lower precision codes.
   */
  public static long prefixCodedToLong(final String prefixCoded) {
    final int len = prefixCoded.length();
    final int shift = prefixCoded.charAt(0)-SHIFT_START_LONG;
    if (shift>63 || shift<0) {
      throw new NumberFormatException("Invalid shift value in prefixCoded string (is encoded value really a LONG?)");
    }
    long sortableBits = 0L;
    for (int i=1; i<len; i++) {
      sortableBits <<= 7;
      final char ch = prefixCoded.charAt(i);
      if (ch>0x7f) {
        throw new NumberFormatException(
          "Invalid prefixCoded numerical value representation (char "+
          Integer.toHexString((int)ch)+" at position "+i+" is invalid)"
        );
      }
      sortableBits |= (long)(ch & 0x7f);
    }
    return (sortableBits << shift) ^ 0x8000000000000000L;
  }

  /** Returns an int from prefixCoded characters.
   * Rightmost bits will be zero for lower precision codes.
   */
  public static int prefixCodedToInt(final String prefixCoded) {
    final int len = prefixCoded.length();
    final int shift = prefixCoded.charAt(0)-SHIFT_START_INT;
    if (shift>31 || shift<0) {
      throw new NumberFormatException("Invalid shift value in prefixCoded string (is encoded value really an INT?)");
    }
    int sortableBits = 0;
    for (int i=1; i<len; i++) {
      sortableBits <<= 7;
      final char ch = prefixCoded.charAt(i);
      if (ch>0x7f) {
        throw new NumberFormatException(
          "Invalid prefixCoded numerical value representation (char "+
          Integer.toHexString((int)ch)+" at position "+i+" is invalid)"
        );
      }
      sortableBits |= (int)(ch & 0x7f);
    }
    return (sortableBits << shift) ^ 0x80000000;
  }

  /** Returns a sequence of coded numbers suitable for TrieRangeFilter.
   * Each successive string in the list has had it's precision reduced by "precisionStep".
   * For sorting, index the first full-precision value into a
   * separate field and the remaining values into another field.
   */
  public static String[] trieCodeLong(long val, int precisionStep) {
    String[] arr = new String[(64-1)/precisionStep+1];
    int idx = 0;
    for (int shift=0; shift<64; shift+=precisionStep) {
      arr[idx++] = longToPrefixCoded(val, shift);
    }
    return arr;
  }

  /** Returns a sequence of coded numbers suitable for TrieRangeFilter.
   * Each successive string in the list has had it's precision reduced by "precisionStep".
   * For sorting, index the first full-precision value into a
   * separate field and the remaining values into another field.
   */
  public static String[] trieCodeInt(int val, int precisionStep) {
    String[] arr = new String[(32-1)/precisionStep+1];
    int idx = 0;
    for (int shift=0; shift<32; shift+=precisionStep) {
      arr[idx++] = intToPrefixCoded(val, shift);
    }
    return arr;
  }

  /** Converts a <code>double</code> value to a sortable signed <code>long</code>.
   * @see #sortableLongToDouble
   */
  public static long doubleToSortableLong(double val) {
    long f = Double.doubleToLongBits(val);
    if (f<0) f ^= 0x7fffffffffffffffL;
    return f;
  }

  /** Converts a sortable <code>long</code> back to a <code>double</code>.
   * @see #doubleToSortableLong
   */
  public static double sortableLongToDouble(long val) {
    if (val<0) val ^= 0x7fffffffffffffffL;
    return Double.longBitsToDouble(val);
  }

  /** Converts a <code>float</code> value to a sortable signed <code>int</code>.
   * @see #sortableIntToFloat
   */
  public static int floatToSortableInt(float val) {
    int f = Float.floatToIntBits(val);
    if (f<0) f ^= 0x7fffffff;
    return f;
  }

  /** Converts a sortable <code>int</code> back to a <code>float</code>.
   * @see #floatToSortableInt
   */
  public static float sortableIntToFloat(int val) {
    if (val<0) val ^= 0x7fffffff;
    return Float.intBitsToFloat(val);
  }

  /** Indexes a series of trie coded values into a lucene {@link Document} using the given field names.
   * If the array of field names is shorter than the trieCoded one, all trieCoded values with higher index get the last field name.
   **/
  public static void addIndexedFields(Document doc, String[] fields, String[] trieCoded) {
    for (int i=0; i<trieCoded.length; i++) {
      final int fnum = Math.min(fields.length-1, i);
      final Field f = new Field(fields[fnum], trieCoded[i], Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS);
      f.setOmitTf(true);
      doc.add(f);
    }
  }

  /** Indexes the full precision value only in the main field (for sorting), and indexes all other
   * lower precision values in the lowerPrecision field (or <code>field+LOWER_PRECISION_FIELD_NAME_SUFFIX</code> if null).
   *
   * Example:  <code>addIndexedFields(doc,"myDouble", "myDoubleTrie", trieCode(doubleToSortableLong(1.414d), 4));</code>
   * Example:  <code>addIndexedFields(doc,"myLong", "myLongTrie", trieCode(123456L, 4));</code>
   **/
  public static void addIndexedFields(Document doc, String field, String lowerPrecisionField, String[] trieCoded) {
    addIndexedFields(doc, new String[]{field, lowerPrecisionField==null ? (field+LOWER_PRECISION_FIELD_NAME_SUFFIX) : lowerPrecisionField}, trieCoded);
  }

  /** Indexes the full precision value only in the main field (for sorting), and indexes all other
   * lower precision values in <code>field+LOWER_PRECISION_FIELD_NAME_SUFFIX</code>.
   *
   * Example:  <code>addIndexedFields(doc,"mydouble",  trieCodeLong(doubleToSortableLong(1.414d), 4));</code>
   * Example:  <code>addIndexedFields(doc,"mylong",  trieCodeLong(123456L, 4));</code>
   **/
  public static void addIndexedFields(Document doc, String field, String[] trieCoded) {
    addIndexedFields(doc, new String[]{field, field+LOWER_PRECISION_FIELD_NAME_SUFFIX}, trieCoded);
  }

  /** A factory method, that generates a {@link SortField} instance for sorting prefix encoded long values. */
  public static SortField getLongSortField(final String field) {
    return new SortField(field, FIELD_CACHE_LONG_PARSER);
  }
  
  /** A factory method, that generates a {@link SortField} instance for sorting prefix encoded long values. */
  public static SortField getLongSortField(final String field, boolean reverse) {
    return new SortField(field, FIELD_CACHE_LONG_PARSER, reverse);
  }
  
  /** A factory method, that generates a {@link SortField} instance for sorting prefix encoded int values. */
  public static SortField getIntSortField(final String field) {
    return new SortField(field, FIELD_CACHE_INT_PARSER);
  }
  
  /** A factory method, that generates a {@link SortField} instance for sorting prefix encoded int values. */
  public static SortField getIntSortField(final String field, boolean reverse) {
    return new SortField(field, FIELD_CACHE_INT_PARSER, reverse);
  }


  public static Object buildRange(RangeBuilder builder, long ll, long uu, int precisionStep) {
    // figure out where to start shift at
    int shift = ((64-1)/precisionStep) * precisionStep;
    return buildRange(builder, ll, uu, shift, precisionStep);
  }

  // shift needs to start at the high end
  static Object buildRange(RangeBuilder builder, long ll, long uu, int shift, int step) {
    assert(shift >= 0);
    if (ll > uu) return null;
    if (ll == uu) return q(ll,uu,shift);
    if (shift <= 0) {
      return q(ll,uu,0);
    }

    // Example: 124,467  middle=2xx,3xx recurse on 124,1ff and 400,467

    long inc = 1L << shift;
    long mask = -1L >>> (64-shift);
    long lrounded = ll & ~mask;     // round down
    long lmiddle = lrounded;
    long urounded= (uu | mask);    // round up
    long umiddle = urounded;
    boolean needLower=false,needUpper=false;

    if (lrounded != ll) {
      needLower=true;
      lmiddle+=inc;
    }

    if (urounded != uu) {
      needUpper=true;
      umiddle-=inc;
    }

    Object q=null;

    // match at this level of precision on the middle of the range
    if (umiddle - lmiddle >= 0) {
      q = builder.range(lmiddle,umiddle,shift);
    } else {
      // no middle at this level of precision, simply recurse to a higher precision level
      return buildRange(builder, ll, uu, shift-step, step);
    }

    if (needLower) {
      // more precision needed on lower range
      Object lq = buildRange(builder, ll, lmiddle-1, shift-step, step);
      q = builder.or(q, lq);
    }

    if (needUpper) {
      // more precision needed on upper range
      Object uq = buildRange(builder, umiddle+1, uu, shift-step, step);
      q = builder.or(q, uq);
    }

    return q;
  }



  public static abstract class RangeBuilder {
    public abstract Object range(long lower, long upper, int shift);
    public abstract Object or(Object r1, Object r2);
  }

  public static class StringRangeBuilder extends RangeBuilder {
    static String t(long l, int shift) {
      return "s"+shift+':'+Long.toHexString(l>>>shift);
    }

    public Object range(long lower, long upper, int shift) {
      if (lower==upper) {
        return t(lower,shift);
      }
      return t(lower,shift) + '-' + t(upper,shift);
    }

    public Object or(Object r1, Object r2) {
      if (r1==null) return r2;
      if (r2==null) return r1;
      return (String)r1 + ' ' + (String)r2;
    }
  }

  
}
