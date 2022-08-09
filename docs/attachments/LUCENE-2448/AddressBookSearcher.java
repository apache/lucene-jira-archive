import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import java.io.IOException;


/**
 * <code>AddressBookSearcher</code> class provides a simple
 * example of searching with Lucene.  It looks for an entry whose
 * 'name' field contains keyword 'Zane'.  The index being searched
 * is called "address-book", located in a temporary directory.
 */
public class AddressBookSearcher
{
    public static void main(String[] args) throws IOException
    {
        String indexDir =
            System.getProperty("java.io.tmpdir", "tmp") +
            System.getProperty("file.separator") + "address-book";
        IndexSearcher searcher = new IndexSearcher(indexDir);
        Query query = new TermQuery(new Term("name", "Zane"));
        Hits hits = searcher.search(query);
        System.out.println("NUMBER OF MATCHING CONTACTS: " + hits.length());
        for (int i = 0; i < hits.length(); i++)
        {
            System.out.println("NAME: " + hits.doc(i).get("name"));
        }
    }
}
