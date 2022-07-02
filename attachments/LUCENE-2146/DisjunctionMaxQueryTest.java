

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;

public class DisjunctionMaxQueryTest extends TestCase {

		public void testHashCodeAndEquals(){
			ArrayList list1 = new ArrayList();
			ArrayList list2 = new ArrayList();
			String[] terms ={"term1","term2"};
			String[] terms2 ={"term1","term2"};
			list1.add(terms);
			list2.add(terms2);

			DisjunctionMaxQuery query1 = new DisjunctionMaxQuery(list1,0.0f);
			DisjunctionMaxQuery query2 = new DisjunctionMaxQuery(list2,0.0f);
			
			assertEquals(query1.hashCode(),query2.hashCode());
			assertEquals(query1,query2);
			

		}
	}
