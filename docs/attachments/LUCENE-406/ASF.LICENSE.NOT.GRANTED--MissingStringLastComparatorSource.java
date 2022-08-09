package org.apache.lucene.search;

import org.apache.lucene.search.*;
import org.apache.lucene.index.IndexReader;

import java.io.IOException;


/**
 * A {@link SortComparatorSource} for strings that orders null values after non-null values.
 * Based on FieldSortedHitQueue.comparatorString
 * <p>
 *
 * @author Chris Hostetter
 * @author yonik
 * @version $Id: MissingStringLastComparatorSource.java,v 1.1 2005/06/02 04:43:06 yonik Exp $
 *
 */

// move to apache package and make public if it is accepted as a patch
class MissingStringLastComparatorSource implements SortComparatorSource {

  public static final String bigString="\uffff\uffff\uffff\uffff\uffff\uffff\uffff\uffffNULL_VAL";

  private final String missingValueProxy;

  public MissingStringLastComparatorSource() {
    this(bigString);
  }

  /** Creates a {@link SortComparatorSource} that uses <tt>missingValueProxy</tt> as the value to return from ScoreDocComparator.sortValue()
   * which is only used my multisearchers to determine how to collate results from their searchers.
   *
   * @param missingValueProxy   The value returned when sortValue() is called for a document missing the sort field.
   * This value is *not* normally used for sorting, but used to create
   */
  public MissingStringLastComparatorSource(String missingValueProxy) {
    this.missingValueProxy=missingValueProxy;
  }

  public ScoreDocComparator newComparator(final IndexReader reader,
                                          final String fieldname)
          throws IOException {

    final String field = fieldname.intern();
    final FieldCache.StringIndex index =
            FieldCache.DEFAULT.getStringIndex (reader, field);

    // :HACK:
    // final String lastString =
    // (index.lookup[index.lookup.length-1]+"X").intern();
    //
    // Note: basing lastStringValue on the StringIndex won't work
    // with a multisearcher.


    return new ScoreDocComparator () {

      public final int compare (final ScoreDoc i, final ScoreDoc j) {
        final int fi = index.order[i.doc];
        final int fj = index.order[j.doc];

        // 0 is the magic position of null

        /**** alternate logic
         if (fi < fj && fi != 0) return -1;
         if (fj < fi && fj != 0) return 1;
         if (fi==fj) return 0;
         return fi==0 ? 1 : -1;
         ****/

        if (fi==fj) return 0;
        if (fi==0) return 1;
        if (fj==0) return -1;
        return fi < fj ? -1 : 1;

      }

      public Comparable sortValue (final ScoreDoc i) {
        int f = index.order[i.doc];
        return (0 == f) ? missingValueProxy : index.lookup[f];
      }

      public int sortType() {
        return SortField.CUSTOM;
      }
    };

  }
}

