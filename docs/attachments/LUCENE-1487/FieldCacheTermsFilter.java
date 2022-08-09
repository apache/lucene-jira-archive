package org.apache.lucene.search;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.OpenBitSet;

public class FieldCacheTermsFilter extends Filter {
  private String field;
  private Iterable terms;

  public FieldCacheTermsFilter(String field, Iterable terms) {
    this.field = field;
    this.terms = terms;
  }

  public FieldCache getFieldCache() {
    return FieldCache.DEFAULT;
  }

  public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
    return new FieldCacheTermsFilterDocIdSet(getFieldCache().getStringIndex(
        reader, field));
  }

  protected class FieldCacheTermsFilterDocIdSet extends DocIdSet {
    private FieldCache.StringIndex fcsi;

    private OpenBitSet openBitSet;

    public FieldCacheTermsFilterDocIdSet(FieldCache.StringIndex fcsi) {
      this.fcsi = fcsi;
      initialize();
    }

    private void initialize() {
      openBitSet = new OpenBitSet(fcsi.lookup.length);
      for (Iterator it = terms.iterator(); it.hasNext();) {
        int termNumber = fcsi.binarySearchLookup((String) it.next());
        if (termNumber > 0) {
          openBitSet.fastSet(termNumber);
        }
      }
    }

    public DocIdSetIterator iterator() {
      return new FieldCacheTermsFilterDocIdSetIterator();
    }

    protected class FieldCacheTermsFilterDocIdSetIterator extends
        DocIdSetIterator {
      private int doc = -1;

      public int doc() {
        return doc;
      }

      public boolean next() {
        try {
          do {
            doc++;
          } while (!openBitSet.fastGet(fcsi.order[doc]));
          return true;
        } catch (ArrayIndexOutOfBoundsException e) {
          doc = Integer.MAX_VALUE;
          return false;
        }
      }

      public boolean skipTo(int target) {
        try {
          doc = target;
          while (!openBitSet.fastGet(fcsi.order[doc])) {
            doc++;
          }
          return true;
        } catch (ArrayIndexOutOfBoundsException e) {
          doc = Integer.MAX_VALUE;
          return false;
        }
      }

    }
  }
}
