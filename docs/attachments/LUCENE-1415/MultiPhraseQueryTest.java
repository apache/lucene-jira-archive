import junit.framework.TestCase;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.MultiPhraseQuery;


public class MultiPhraseQueryTest extends TestCase {

	public void testHashCodeAndEquals(){
		MultiPhraseQuery query1 = new MultiPhraseQuery();
		MultiPhraseQuery query2 = new MultiPhraseQuery();
		
		assertEquals(query1.hashCode(), query2.hashCode());
		assertEquals(query1,query2);
		
		Term term1= new Term("someField","someText");
		
		query1.add(term1);
		query2.add(term1);
		
		assertEquals(query1.hashCode(), query2.hashCode());
		assertEquals(query1,query2);
		
		Term term2= new Term("someField","someMoreText");
		
		query1.add(term2);
		
		assertFalse(query1.hashCode()==query2.hashCode());
		assertFalse(query1.equals(query2));
		
		query2.add(term2);
		
		assertEquals(query1.hashCode(), query2.hashCode());
		assertEquals(query1,query2);
	}
}
