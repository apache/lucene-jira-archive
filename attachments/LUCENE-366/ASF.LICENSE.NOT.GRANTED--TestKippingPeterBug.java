
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.DateField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilteredQuery;

import java.io.IOException;
import java.util.Random;
import java.util.BitSet;

import junit.framework.TestCase;

public class TestKippingPeterBug extends TestCase {

    public static final boolean F = false;
    public static final boolean T = true;

    public static String[] data = new String [] {
        "a b   m n",
        "a x   q r",
        "a b   s t",
        "a x   e f",
        "a"
    };

    
    RAMDirectory index = new RAMDirectory();
    IndexReader r;
    IndexSearcher s;
    Query ALL = new TermQuery(new Term("data","a"));

    
    public TestKippingPeterBug(String name) {
	super(name);
    }
    public TestKippingPeterBug() {
        super();
    }

    public void setUp() throws Exception {

        /* build an index */
        IndexWriter writer = new IndexWriter(index,
                                             new SimpleAnalyzer(), T);
        
        for (int i = 0; i < data.length; i++) {
            Document doc = new Document();
            doc.add(Field.Keyword("id",String.valueOf(i)));
            doc.add(Field.Text("data",data[i]));
            writer.addDocument(doc);
        }
        
        writer.optimize();
        writer.close();

        r = IndexReader.open(index);
        s = new IndexSearcher(r);
        
    }

    public void testA() throws Exception {
        Hits result;

        result = s.search(ALL);
        assertEquals("not all docs found", data.length, result.length());

    }

    public void testFilters() throws Exception {

        Filter f = new FirstFilter();
        Filter l = new LastFilter();
        
        Hits result;
        
        assertEquals("only one should be set in FirstFilter",
                     1, f.bits(r).cardinality());
        assertEquals("only one should be set in LastFilter",
                     1, l.bits(r).cardinality());

        result = s.search(ALL, f);
        assertEquals("should get one doc from FirstFilter(ALL)",
                     1, result.length());
        assertEquals("wrong doc from FirstFilter(ALL)",
                     0, result.id(0));
                    
        result = s.search(ALL, l);
        assertEquals("should get one doc from LastFilter(ALL)",
                     1, result.length());
        assertEquals("wrong doc from LastFilter(ALL)",
                     data.length-1, result.id(0));
                    
    }

    public void testBooleanFilteredQuery() throws Exception {

        Filter f = new FirstFilter();
        Filter l = new LastFilter();

        Query ff = new FilteredQuery(ALL, f);
        Query ll = new FilteredQuery(ALL, l);
        
        Hits result;

        result = s.search(ff);
        assertEquals("should get one doc from FirstFilter(ALL)",
                     1, result.length());
        assertEquals("wrong doc from FirstFilter(ALL)",
                     0, result.id(0));
                    
        result = s.search(ll);
        assertEquals("should get one doc from LastFilter(ALL)",
                     1, result.length());
        assertEquals("wrong doc from LastFilter(ALL)",
                     data.length-1, result.id(0));


        BooleanQuery or = new BooleanQuery();
        or.add(ff, false, false);
        or.add(ll, false, false);
        
        result = s.search(or);
        assertEquals("should get two docs from *or*",
                     2, result.length());
                    
        BooleanQuery and = new BooleanQuery();
        and.add(ff, true, false);
        and.add(ll, true, false);
        
        result = s.search(and);
        assertEquals("should get no docs from *and*",
                     0, result.length());
                    
                    
    }

    
    
}

class FirstFilter extends Filter {
    /** only lets the first doc in the index pass the filter */
    public BitSet bits(IndexReader r) {
        BitSet b = new BitSet(r.maxDoc());
        b.set(0);
        return b;
    }
}
class LastFilter extends Filter {
    /** only lets the last doc in the index pass the filter */
    public BitSet bits(IndexReader r) {
        BitSet b = new BitSet(r.maxDoc());
        b.set(r.maxDoc()-1);
        return b;
    }
}

    
