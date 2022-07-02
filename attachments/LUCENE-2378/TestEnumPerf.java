import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import org.apache.lucene.util.*;

import java.util.Random;

public class TestEnumPerf {
  public static RAMDirectory dir;
  public static IndexWriter writer;
  public static IndexReader reader;
  public static Random r = new Random(0);

  public static void main(String[] args) throws Exception {
    int a = 0;
    int ndocs = Integer.parseInt(args[a++]);
    int maxBufferedDocs = Integer.parseInt(args[a++]);
    int nUniqueTerms = Integer.parseInt(args[a++]);
    int iter = Integer.parseInt(args[a++]);

    dir = new RAMDirectory();

    Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_31);

    writer = new IndexWriter(dir, analyzer, true, IndexWriter.MaxFieldLength.UNLIMITED);
    writer.setMergePolicy(new LogDocMergePolicy());
    writer.setMaxBufferedDocs(maxBufferedDocs);  // control number of segments

    Document doc = new Document();
    Field f = new Field("a","",Field.Store.NO, Field.Index.NOT_ANALYZED);
    doc.add(f);

    long time = System.currentTimeMillis();
    for (int i=0; i<ndocs; i++) {
      int val = r.nextInt(nUniqueTerms);
      f.setValue(Integer.toString(val));
      writer.addDocument(doc);
    }
 writer.optimize();  // TODO: REMOVE ME
    writer.close();
    long endTime = System.currentTimeMillis();

    System.out.println("Indexing completed.");
    System.out.println("\ttime=" + (endTime-time));
    System.out.println("\tsize=" + dir.sizeInBytes());

    time = System.currentTimeMillis();
    reader = IndexReader.open(dir);
    endTime = System.currentTimeMillis();

    System.out.println("Index reader opened.");
    System.out.println("\topen time=" + (endTime-time));
    System.out.println("\tnSegments=" + reader.getSequentialSubReaders().length);


    time = System.currentTimeMillis();
    int ret = 0;
    for (int i=0; i<iter; i++) {
      ret += enumerate(true);
    }
    endTime = System.currentTimeMillis();
    System.out.println("Iteration done.  result=" + ret + " time=" + (endTime-time));

  }


  // lucene 4
  public static int enumerate(boolean iterateDocs) throws Exception {
    int ret = 0;
    Fields fields = MultiFields.getFields(reader);
    Terms terms = fields.terms("a");
    TermsEnum tenum = terms.iterator();
    Bits deleted = MultiFields.getDeletedDocs(reader);
    BytesRef term;
    DocsEnum docsEnum = null;

    while ((term = tenum.next()) != null) {
      ret += term.length;

      if (iterateDocs) {
        docsEnum = tenum.docs(deleted, docsEnum);
        DocsEnum.BulkReadResult bulk = docsEnum.getBulkResult();
        for (;;) {
          int nDocs = docsEnum.read();
          if (nDocs == 0) break;
          int[] docArr = bulk.docs.ints;  // this might be movable outside the loop, but perhaps not worth the risk.
          int end = bulk.docs.offset + nDocs;
          for (int i=bulk.docs.offset; i<end; i++) {
            ret += docArr[i];
          }
        }

      }

    }
    return ret;
  }

  /***
  // lucene 4 no bulk read
  public static int enumerate(boolean iterateDocs) throws Exception {
    int ret = 0;
    Fields fields = MultiFields.getFields(reader);
    Terms terms = fields.terms("a");
    TermsEnum tenum = terms.iterator();
    Bits deleted = MultiFields.getDeletedDocs(reader);
    BytesRef term;
    DocsEnum docsEnum = null;

    while ((term = tenum.next()) != null) {
      ret += term.length;

      if (iterateDocs) {
        docsEnum = tenum.docs(deleted, docsEnum);
        for (;;) {
          int doc = docsEnum.nextDoc();
          if (doc == DocsEnum.NO_MORE_DOCS) break;
          ret += doc;
        }
      }

    }
    return ret;
  }
   ***/

  /***
  // lucene 3x
  public static int enumerate(boolean iterateDocs) throws Exception {
    int ret = 0;
    TermEnum tenum = reader.terms(new Term("a",""));
    TermDocs tdocs = reader.termDocs();
    int[] docs = new int[1000];
    int[] freq = new int[1000];


    do {
      Term t = tenum.term();
      ret += t.text().length();

      if (iterateDocs) {
        tdocs.seek(tenum);
        for(;;) {
          int num = tdocs.read(docs, freq);
          if (num==0) break;
          for (int i=0; i<num; i++) {
            ret += docs[i];
          }
        }
      }
      

    } while (tenum.next());
    return ret;
  }
  ***/

}
