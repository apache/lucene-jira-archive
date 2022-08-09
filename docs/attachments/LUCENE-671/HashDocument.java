
/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Searcher;

import java.util.*;             // for javadoc

/** Documents are the unit of indexing and search.
 *
 * A Document is a set of fields.  Each field has a name and a textual value.
 * A field may be {@link Fieldable#isStored() stored} with the document, in which
 * case it is returned with search hits on the document.  Thus each document
 * should typically contain one or more stored fields which uniquely identify
 * it.
 *
 * <p>Note that fields which are <i>not</i> {@link Fieldable#isStored() stored} are
 * <i>not</i> available in documents retrieved from the index, e.g. with {@link
 * Hits#doc(int)}, {@link Searcher#doc(int)} or {@link
 * IndexReader#document(int)}.
 */
public class HashDocument extends Document {
	//List fields = new Vector();
	public Hashtable fields = new Hashtable();
	private float boost = 1.0f;
	/** Constructs a new document with no fields. */
	public HashDocument() {}
	
	
	public int size(){
		return 0;
	}
	
	/**
	 * <p>Adds a field to a document.  Several fields may be added with
	 * the same name.  In this case, if the fields are indexed, their text is
	 * treated as though appended for the purposes of search.</p>
	 * <p> Note that add like the removeField(s) methods only makes sense 
	 * prior to adding a document to an index. These methods cannot
	 * be used to change the content of an existing index! In order to achieve this,
	 * a document has to be deleted from an index and a new changed version of that
	 * document has to be added.</p>
	 */
	public void add(Fieldable field) {
		LinkedList l = null;
		if((l = (LinkedList)fields.get(field.name())) != null){
			l.addLast(field);
		} else {
			l = new LinkedList();
			l.addLast(field);
			fields.put(field.name(), l);  
			if(field.getInstanceID() > maxInstance){
				maxInstance = field.getInstanceID();
			}
		}
	}
	
	/**
	 * <p>Removes field with the specified name from the document.
	 * If multiple fields exist with this name, this method removes the first field that has been added.
	 * If there is no field with the specified name, the document remains unchanged.</p>
	 * <p> Note that the removeField(s) methods like the add method only make sense 
	 * prior to adding a document to an index. These methods cannot
	 * be used to change the content of an existing index! In order to achieve this,
	 * a document has to be deleted from an index and a new changed version of that
	 * document has to be added.</p>
	 */
	public void removeField(String name) {
		LinkedList ll = (LinkedList)fields.get(name);
		if(ll == null || ll.size() == 0){
			return;
		}
		((LinkedList)fields.get(name)).remove(0);
		if(((LinkedList)fields.get(name)).size() == 0){
			fields.remove(name);
		}
	}
	
	/**
	 * <p>Removes all fields with the given name from the document.
	 * If there is no field with the specified name, the document remains unchanged.</p>
	 * <p> Note that the removeField(s) methods like the add method only make sense 
	 * prior to adding a document to an index. These methods cannot
	 * be used to change the content of an existing index! In order to achieve this,
	 * a document has to be deleted from an index and a new changed version of that
	 * document has to be added.</p>
	 */
	public void removeFields(String name) {
		fields.remove(name);
	}
	
	/** Returns a field with the given name if any exist in this document, or
	 * null.  If multiple fields exists with this name, this method returns the
	 * first value added.
	 * Do not use this method with lazy loaded fields.
	 */
	public Field getField(String name) {
		LinkedList l = (LinkedList)fields.get(name);
		if(l == null){
			return null;
		} else {
			return (Field)l.getFirst();
		}
	}
	
	
	/** Returns a field with the given name if any exist in this document, or
	 * null.  If multiple fields exists with this name, this method returns the
	 * first value added.
	 */
	public Fieldable getFieldable(String name) {
		LinkedList l = (LinkedList)fields.get(name);
		if(l == null){
			return null;
		} else {
			return (Fieldable)l.getFirst();
		}
	}
	
	/** Returns the string value of the field with the given name if any exist in
	 * this document, or null.  If multiple fields exist with this name, this
	 * method returns the first value added. If only binary fields with this name
	 * exist, returns null.
	 */
	public String get(String name) {
		LinkedList l = (LinkedList)fields.get(name);
		if(l == null){
			return null;
		} else {
			for(int i = 0; i < l.size(); i++){
				if(!((Fieldable)l.get(i)).isBinary()){
					return ((Fieldable)l.get(i)).stringValue();
				}
			}
			return null;
		}
	}
	
	/** Returns an Enumeration of all the fields in a document.
	 * @deprecated use {@link #getFields()} instead
	 */
	public Enumeration fields() {
		return new Glare(fields);
	}
	
	
	
	/** Returns a List of all the fields in a document.
	 * <p>Note that fields which are <i>not</i> {@link Fieldable#isStored() stored} are
	 * <i>not</i> available in documents retrieved from the index, e.g. with {@link
	 * Hits#doc(int)}, {@link Searcher#doc(int)} or {@link IndexReader#document(int)}.
	 */
	public List getFields() {
		LinkedList ret = new LinkedList();
		Enumeration e = fields();
		while(e.hasMoreElements()){
			ret.add(e.nextElement());
		}
		return ret;
	}
	
	/**
	 * Returns an array of {@link Field}s with the given name.
	 * This method can return <code>null</code>.
	 * Do not use with lazy loaded fields.
	 *
	 * @param name the name of the field
	 * @return a <code>Field[]</code> array
	 */
	public Field[] getFields(String name) {
		LinkedList l = (LinkedList)fields.get(name);
		if (l == null){
			return null;
		}
		else{
			Field[] f = new Field[l.size()];
			l.toArray(f);
			return f;
		}
	}
	
	public Field getField(String name, int number) {
		LinkedList l = (LinkedList)fields.get(name);
		if(l == null){
			return null;
		} else {
			for(int i = 0; i < l.size(); i++){
				if(((Field)l.get(i)).getInstanceID() == number){
					return (Field)l.get(i);
				}
			}
			return null;
		}
	}
	
	public int getMaxInstance(){
		return maxInstance;
	}
	
	/**
	 * Returns an array of {@link Fieldable}s with the given name.
	 * This method can return <code>null</code>.
	 *
	 * @param name the name of the field
	 * @return a <code>Fieldable[]</code> array or <code>null</code>
	 */
	public Fieldable[] getFieldables(String name) {
		LinkedList l = (LinkedList)fields.get(name);
		if (l == null){
			return null;
		}
		else{
			Fieldable[] f = new Fieldable[l.size()];
			l.toArray(f);
			return f;
		}
	}
	
	
	/**
	 * Returns an array of values of the field specified as the method parameter.
	 * This method can return <code>null</code>.
	 *
	 * @param name the name of the field
	 * @return a <code>String[]</code> of field values or <code>null</code>
	 */
	public String[] getValues(String name) {
		LinkedList l = (LinkedList)fields.get(name);
		if (l == null){
			return null;
		}
		else{
			String[] ret = new String[l.size()];
			for (int i = 0; i < l.size(); i++){
				if(!((Fieldable)l.get(i)).isBinary()){
					ret[i] = ((Fieldable)l.get(i)).stringValue();
				}
			}
			return ret;
		}
		
	}
	
	/**
	 * Returns an array of byte arrays for of the fields that have the name specified
	 * as the method parameter. This method will return <code>null</code> if no
	 * binary fields with the specified name are available.
	 *
	 * @param name the name of the field
	 * @return a  <code>byte[][]</code> of binary field values or <code>null</code>
	 */
	public byte[][] getBinaryValues(String name) {
		LinkedList l = (LinkedList)fields.get(name);
		if (l == null){
			return null;
		}
		else{
			byte[][] ret = new byte[l.size()][];
			for (int i = 0; i < l.size(); i++){
				if(((Fieldable)l.get(i)).isBinary()){
					ret[i] = ((Fieldable)l.get(i)).binaryValue();
				}
			}
			return ret;
		}
	}
	
	
	/**
	 * Returns an array of bytes for the first (or only) field that has the name
	 * specified as the method parameter. This method will return <code>null</code>
	 * if no binary fields with the specified name are available.
	 * There may be non-binary fields with the same name.
	 *
	 * @param name the name of the field.
	 * @return a <code>byte[]</code> containing the binary field value or <code>null</code>
	 */
	public byte[] getBinaryValue(String name) {
		LinkedList l = (LinkedList)fields.get(name);
		if(l == null){
			return null;
		} else {
			for(int i = 0; i < l.size(); i++){
				if(((Fieldable)l.get(i)).isBinary()){
					return ((Fieldable)l.get(i)).binaryValue();
				}
			}
			return null;
		}
	}
	
	/** Prints the fields of a document for human consumption. */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Document<");
		Enumeration e = fields.elements();
		while(e.hasMoreElements()){
			LinkedList l = (LinkedList)e.nextElement();
			for(int i = 0; i < l.size(); i++){
				buffer.append(l.get(i).toString());
			}
		}
		buffer.append(">");
		return buffer.toString();
	}
	
	private class Glare implements Enumeration {
		
		private Hashtable iter;
		Enumeration garbage;
		private LinkedList current = null;
		
		int index = 0;
		boolean moreElements = true;
		public Glare(Hashtable h){
			iter = h;
			garbage = iter.elements();
		}
		
		public boolean hasMoreElements() {
			if(garbage.hasMoreElements()){
				return true;
			} else if (current == null){
				return false; // This would be really odd
			} else if (index > current.size() - 1) {
				return false;
			} else {
				return true;
			}
		}
		
		public Object nextElement() {
			if(current == null && !garbage.hasMoreElements()){
				throw new NoSuchElementException();
			}
			if(current == null && garbage.hasMoreElements()){
				current = (LinkedList)garbage.nextElement();
				index++;
				return current.get(0);
			} 
			if (index < current.size()) {
				return current.get(index++);
			} else {
				if(garbage.hasMoreElements()){
					current = (LinkedList)garbage.nextElement();
					index = 1;
					return current.get(0);
				} else {
					throw new NoSuchElementException();
				}
			}
		}
	}
}
