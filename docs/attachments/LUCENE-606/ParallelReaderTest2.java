package org.apache.lucene.index;

import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.store.RAMDirectory;

public class ParallelReaderTest2 extends TestCase {

    public void testAllFields() throws IOException {
        Document doc;

        RAMDirectory rd1 = new RAMDirectory();
        IndexWriter iw1 = new IndexWriter(rd1, new SimpleAnalyzer(), true);

        doc = new Document();
        doc.add(new Field("field1", "the quick brown fox jumps", Store.YES,
            Index.TOKENIZED));
        iw1.addDocument(doc);

        iw1.close();
        RAMDirectory rd2 = new RAMDirectory();
        IndexWriter iw2 = new IndexWriter(rd2, new SimpleAnalyzer(), true);

        doc = new Document();
        doc.add(new Field("field1", "the fox jumps over the lazy dog",
            Store.YES, Index.TOKENIZED));
        doc.add(new Field("field2", "the fox jumps over the lazy dog",
            Store.YES, Index.TOKENIZED));
        iw2.addDocument(doc);

        iw2.close();

        IndexReader ir1 = IndexReader.open(rd1);
        IndexReader ir2 = IndexReader.open(rd2);
        ;

        {
            ParallelReader pr = new ParallelReader();
            pr.add(ir1);
            pr.add(ir2);

            assertEquals(pr.document(0).getFields("field1"), new Field[] {
                ir1.document(0).getFields("field1")[0],
                ir2.document(0).getFields("field1")[0]
            });
            assertEquals(pr.document(0).getFields("field2"), ir2.document(0).getFields("field2"));
        }
        {
            ParallelReader pr = new ParallelReader(true);
            pr.add(ir1);
            pr.add(ir2);

            assertEquals(pr.document(0).getFields("field1"), new Field[] {
                ir1.document(0).getFields("field1")[0],
                ir2.document(0).getFields("field1")[0]
            });
            assertEquals(pr.document(0).getFields("field2"), ir2.document(0).getFields("field2"));
        }
        {
            ParallelReader pr = new ParallelReader(false);
            pr.add(ir1);
            pr.add(ir2);

            assertEquals(pr.document(0).getFields("field1"), ir1.document(0).getFields("field1"));
            assertEquals(pr.document(0).getFields("field2"), ir2.document(0).getFields("field2"));
        }
        {
            ParallelReader pr = new ParallelReader();
            pr.add(ir2);
            pr.add(ir1);

            assertEquals(pr.document(0).getFields("field1"), new Field[] {
                ir2.document(0).getFields("field1")[0],
                ir1.document(0).getFields("field1")[0]
            });
            assertEquals(pr.document(0).getFields("field2"), ir2.document(0).getFields("field2"));
        }
        {
            ParallelReader pr = new ParallelReader(true);
            pr.add(ir2);
            pr.add(ir1);

            assertEquals(pr.document(0).getFields("field1"), new Field[] {
                ir2.document(0).getFields("field1")[0],
                ir1.document(0).getFields("field1")[0]
            });
            assertEquals(pr.document(0).getFields("field2"), ir2.document(0).getFields("field2"));

            RAMDirectory rd = new RAMDirectory();
            IndexWriter iw = new IndexWriter(rd, new SimpleAnalyzer(),true);
            iw.addIndexes(new IndexReader[]{pr});
            iw.close();
            IndexReader ir = IndexReader.open(rd);
            assertEquals(ir.document(0).getFields("field1"), pr.document(0).getFields("field1"));
            assertEquals(ir.document(0).getFields("field2"), ir2.document(0).getFields("field2"));
        }
        {
            ParallelReader pr = new ParallelReader(false);
            pr.add(ir2);
            pr.add(ir1);

            assertEquals(pr.document(0).getFields("field1"), ir2.document(0).getFields("field1"));
            assertEquals(pr.document(0).getFields("field2"), ir2.document(0).getFields("field2"));
            
            RAMDirectory rd = new RAMDirectory();
            IndexWriter iw = new IndexWriter(rd, new SimpleAnalyzer(),true);
            iw.addIndexes(new IndexReader[]{pr});
            iw.close();
            IndexReader ir = IndexReader.open(rd);
            assertEquals(ir.document(0).getFields("field1"), ir2.document(0).getFields("field1"));
            assertEquals(ir.document(0).getFields("field2"), ir2.document(0).getFields("field2"));
        }
        
    }

    private void assertEquals(Field[] a, Field[] b) {
        boolean equal = true;
        if(a.length != b.length) {
            equal = false;
        } else {
            for(int i = 0; i < a.length; i++) {
                if(!a[i].toString().equals(b[i].toString())) {
                    equal = false;
                    break;
                }
            }
        }
        if(!equal) {
            fail("expected: " + Arrays.asList(a) + " but was: "
                + Arrays.asList(b));
        }
    }
}
