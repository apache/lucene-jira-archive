import org.apache.lucene.util.*;
import org.apache.lucene.store.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.analysis.core.*;
import org.apache.lucene.analysis.en.*;
import org.apache.lucene.queryParser.*;
import java.util.*;
import java.io.*;

public class CachedFilterIndexReader extends FilterIndexReader {
  private final Bits skipDocs;

  public CachedFilterIndexReader(IndexReader in, Filter f) throws IOException {
    super(in);
    assert in instanceof SegmentReader;

    final DocIdSetIterator keepDocs = f.getDocIdSet(in).iterator();
    final Bits delDocs = in.getDeletedDocs();
    final int maxDoc = in.maxDoc();

    // now pre-compile single skipDocs that folds in
    // the incoming filter (negated) and deletions
    final OpenBitSet skipDocs = new OpenBitSet(maxDoc);

    // skip all docs by default
    skipDocs.set(0, maxDoc);

    // naive impl -- skip the doc if it's deleted or if
    // the filter doesn't accept it
    while(true) {
      final int docID = keepDocs.nextDoc();
      if (docID == DocIdSetIterator.NO_MORE_DOCS) {
        break;
      }
      if (delDocs == null || !delDocs.get(docID)) {
        skipDocs.clear(docID);
      }
    }

    this.skipDocs = skipDocs;
  }

  @Override
    public Fields fields() throws IOException {

    return new FilterFields(in.fields()) {

      @Override
        public FieldsEnum iterator() throws IOException {
        return new FilterFieldsEnum(in.iterator()) {

          @Override
            public TermsEnum terms() throws IOException {
            return new FilterTermsEnum(in.terms()) {

              @Override
                public DocsEnum docs(Bits ignored, DocsEnum reuse) throws IOException {
                return in.docs(skipDocs, reuse);
              }

              @Override
                public DocsAndPositionsEnum docsAndPositions(Bits ignored, DocsAndPositionsEnum reuse) throws IOException {
                return in.docsAndPositions(skipDocs, reuse);
              }
            };
          }
        };
      }

      @Override
        public Terms terms(String field) throws IOException {

        return new FilterTerms(in.terms(field)) {

          @Override
            public TermsEnum iterator() throws IOException {
            return new FilterTermsEnum(in.iterator()) {

              @Override
                public DocsEnum docs(Bits ignored, DocsEnum reuse) throws IOException {
                return in.docs(skipDocs, reuse);
              }

              @Override
                public DocsAndPositionsEnum docsAndPositions(Bits ignored, DocsAndPositionsEnum reuse) throws IOException {
                return in.docsAndPositions(skipDocs, reuse);
              }
            };
          }

          @Override
            public DocsEnum docs(Bits ignored, BytesRef text, DocsEnum reuse) throws IOException {
            final TermsEnum termsEnum = getThreadTermsEnum();
            if (termsEnum.seek(text) == TermsEnum.SeekStatus.FOUND) {
              return termsEnum.docs(skipDocs, reuse);
            } else {
              return null;
            }
          }

          @Override
            public DocsAndPositionsEnum docsAndPositions(Bits ignored, BytesRef text, DocsAndPositionsEnum reuse) throws IOException {
            final TermsEnum termsEnum = getThreadTermsEnum();
            if (termsEnum.seek(text) == TermsEnum.SeekStatus.FOUND) {
              return termsEnum.docsAndPositions(skipDocs, reuse);
            } else {
              return null;
            }
          }
        };
      }
    };
  }

  public static IndexReader create(IndexReader r, Filter f) throws IOException {
    final IndexReader[] subReaders = r.getSequentialSubReaders();
    final IndexReader[] newSubReaders = new IndexReader[subReaders.length];

    System.out.println("SUBS=" + subReaders);
    for(int i=0;i<subReaders.length;i++) {
      newSubReaders[i] = new CachedFilterIndexReader(subReaders[i], f);
    }
    return new MultiReader(newSubReaders, false);
  }
}
