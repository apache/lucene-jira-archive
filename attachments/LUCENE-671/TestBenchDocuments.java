import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;


import junit.framework.TestCase;

public class TestBenchDocuments extends TestCase {

	public void testBasicAdditionRemovalSpeed(){

		Document oldDoc = new Document();
		HashDocument newDoc = new HashDocument();
		Date bench = new Date();
		bench = new Date();
		//Insert the same field name a hundred times
		for(int i = 0; i < 100; i++){
			oldDoc.add(new Field("noname", new Integer(i).toString(), Store.YES, Index.NO));
		}
		System.out.println("Total time to add 100 fields of same name (old): " + (new Date().getTime() - bench.getTime()) + "ms");
		bench = new Date();
		for(int i = 0; i < 100; i++){
			newDoc.add(new Field("noname", new Integer(i).toString(), Store.YES, Index.NO));
		}
		System.out.println("Total time to add 100 fields of same name (new): " + (new Date().getTime() - bench.getTime()) + "ms");
		
		bench = new Date();
		for(int i = 0; i < 100; i++){
			oldDoc.getFields("noname");
		}
		System.out.println("Total time to get 100 fields of same name (old): " + (new Date().getTime() - bench.getTime()) + "ms");
		bench = new Date();
		for(int i = 0; i < 100; i++){
			newDoc.getFields("noname");
		}
		System.out.println("Total time to get 100 fields of same name (new): " + (new Date().getTime() - bench.getTime()) + "ms");	
		
		bench = new Date();
		for(int i = 0; i < 100; i++){
			oldDoc.removeFields("noname");
		}
		System.out.println("Total time to remove 100 fields of same name (old): " + (new Date().getTime() - bench.getTime()) + "ms");
		bench = new Date();
		for(int i = 0; i < 100; i++){
			newDoc.removeFields("noname");
		}
		System.out.println("Total time to remove 100 fields of same name (new): " + (new Date().getTime() - bench.getTime()) + "ms");



		//Insert a bunch of differently named fields
		for(int i = 0; i < 100; i++){
			oldDoc.add(new Field(new Integer(i).toString(), "noname", Store.YES, Index.NO));
		}
		System.out.println("Total time to add 100 fields of different names (old): " + (new Date().getTime() - bench.getTime()) + "ms");
		bench = new Date();
		for(int i = 0; i < 100; i++){
			newDoc.add(new Field(new Integer(i).toString(), "noname", Store.YES, Index.NO));
		}
		System.out.println("Total time to add 100 fields of different names (new): " + (new Date().getTime() - bench.getTime()) + "ms");
		for(int i = 0; i < 100; i++){
			oldDoc.removeField(new Integer(i).toString());
		}
		System.out.println("Total time to remove 100 fields of different names (old): " + (new Date().getTime() - bench.getTime()) + "ms");
		bench = new Date();
		for(int i = 0; i < 100; i++){
			newDoc.removeField(new Integer(i).toString());
		}
		System.out.println("Total time to remove 100 fields of different names (new): " + (new Date().getTime() - bench.getTime()) + "ms");




	}

	public void testGetSpeed(){
		System.out.println("***** Testing OLD Document get speed *****");
		getSpeed(new Document());
		System.out.println("***** Testing NEW Document get speed *****");
		getSpeed(new HashDocument());
	}

	public void getSpeed(Document doc){
		//Document oldDoc = new Document();
		HashDocument newDoc = new HashDocument();
		Date bench = new Date();	
		//Insert the same field name a hundred times
		for(int i = 0; i < 100; i++){
			doc.add(new Field(new Integer(i).toString(), "noname", Store.YES, Index.NO));
		}
		bench = new Date();
		for(int i = 0; i < 100; i++ ){
			doc.getField(new Integer(49).toString());
			doc.getField(new Integer(50).toString());
			doc.getField(new Integer(51).toString());
			doc.getField(new Integer(49).toString());
			doc.getField(new Integer(50).toString());
			doc.getField(new Integer(51).toString());
		}
		System.out.println("Total time for get from middle: " + (new Date().getTime() - bench.getTime()) + "ms");
		bench = new Date();
		for(int i = 0; i < 100; i++ ){
			doc.getField(new Integer(0).toString());
			doc.getField(new Integer(1).toString());
			doc.getField(new Integer(2).toString());
			doc.getField(new Integer(0).toString());
			doc.getField(new Integer(1).toString());
			doc.getField(new Integer(2).toString());
		}
		System.out.println("Total time for get from front: " + (new Date().getTime() - bench.getTime()) + "ms");
		bench = new Date();
		for(int i = 0; i < 100; i++ ){
			doc.getField(new Integer(97).toString());
			doc.getField(new Integer(98).toString());
			doc.getField(new Integer(99).toString());
			doc.getField(new Integer(97).toString());
			doc.getField(new Integer(98).toString());
			doc.getField(new Integer(99).toString());
		}
		System.out.println("Total time for get from back: " + (new Date().getTime() - bench.getTime()) + "ms");
	}

	public void testIterationSpeed(){
		Date bench = new Date();
		Document oldDoc = new Document();
		HashDocument newDoc = new HashDocument();	
		for(int i = 0; i < 100; i++){
			oldDoc.add(new Field(new Integer(i).toString(), "noname", Store.YES, Index.NO));
		}
		for(int i = 0; i < 100; i++){
			newDoc.add(new Field(new Integer(i).toString(), "noname", Store.YES, Index.NO));
		}
		List l = null;
		//OLD
		bench = new Date();
		for(int i = 0; i < 100; i++){
			l = oldDoc.getFields();
			Iterator h = l.iterator();
			while(h.hasNext()){
				h.next();
			}
		}
		System.out.println("Total time for iteration (old): " + (new Date().getTime() - bench.getTime()) + "ms");
		//NEW
		bench = new Date();
		for(int i = 0; i < 100; i++){
			l = oldDoc.getFields();
			Iterator b = l.iterator();
			while(b.hasNext()){
				b.next();
			}
		}
		System.out.println("Total time for iteration (new): " + (new Date().getTime() - bench.getTime()) + "ms");		
	}
}
