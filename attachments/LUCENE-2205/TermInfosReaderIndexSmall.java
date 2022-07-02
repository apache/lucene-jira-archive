package org.apache.lucene.index;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class replaces the existing term array, terminfos array, and indexpointer long array
 * to be much more compact while still maintaining a high level of performance.
 * @author Aaron McCurry amccurry@nearinfinity.com
 */
public class TermInfosReaderIndexSmall extends TermInfosReaderIndex {

	private int[] indexToTerms;
	private byte[] data;
	private Term[] fields;
	
	/**
	 * Loads the segment information at segment load time.
	 */
	@Override
	public void build(SegmentTermEnum indexEnum, int indexDivisor, int tiiFileLength) throws IOException {
		int indexSize = 1 + ((int) indexEnum.size - 1) / indexDivisor;
		indexToTerms = new int[indexSize];
		// this is only an inital size, it will be GCed once the build is complete
		int initialSize = (int) (tiiFileLength * 1.5);
		ByteArrayOutputStream baos = new ByteArrayOutputStream(initialSize);
		CustomDataOutputStream outputStream = new CustomDataOutputStream(baos);
		String currentField = null;
		List<String> fieldStrs = new ArrayList<String>();
		int fieldCounter = -1;
		for (int i = 0; indexEnum.next(); i++) {
			Term term = indexEnum.term();
			if (currentField != term.field) {
				currentField = term.field;
				fieldStrs.add(currentField);
				fieldCounter++;
			}
			TermInfo termInfo = indexEnum.termInfo();
			indexToTerms[i] = baos.size();
			outputStream.writeVInt(fieldCounter);
			outputStream.writeString(term.text());
			outputStream.writeVInt(termInfo.docFreq);
			outputStream.writeVInt(termInfo.skipOffset);
			outputStream.writeVLong(termInfo.freqPointer);
			outputStream.writeVLong(termInfo.proxPointer);
			outputStream.writeVLong(indexEnum.indexPointer);
			for (int j = 1; j < indexDivisor; j++)
				if (!indexEnum.next())
					break;
		}
		outputStream.close();
		fields = new Term[fieldStrs.size()];
		for (int i = 0; i < fields.length; i++) {
			fields[i] = new Term(fieldStrs.get(i));
		}
		this.data = baos.toByteArray();
		
//		System.out.println("Internal Memory Size [" + data.length + 
//				"], Tii file size [" + tiiFileLength + "], Initial Size [" + initialSize + "]");
	}
	
	@Override
	public void seekEnum(SegmentTermEnum enumerator, int indexOffset, int totalIndexInterval) throws IOException {
		int index = indexToTerms[indexOffset];
		int[] intResults = new int[2];
		long[] longResults = new long[2];
		
		//read the field index and get the field from the index array 
		Term field = getFieldTerm(index,intResults);
		index += intResults[1]; //increment the length of the vint
		
		//read the text length
		readVInt(data, index, intResults);
		index += intResults[1]; //increment the length of the vint
		
		//create the term from the field term for speed
		Term term = field.createTerm(getString(data, index, intResults, longResults));
		index += intResults[1]; //increment the length of the string IN BYTES
		
		//create term info object
		TermInfo termInfo = new TermInfo();
		
		//read the doc freq
		readVInt(data, index, intResults);
		termInfo.docFreq = intResults[0];
		index += intResults[1]; //increment the length of the vint
		
		//read the skip offset
		readVInt(data, index, intResults);
		termInfo.skipOffset = intResults[0];
		index += intResults[1]; //increment the length of the vint
		
		//read the freq pointer
		readVLong(data, index, longResults);
		termInfo.freqPointer = longResults[0];
		index += longResults[1]; //increment the length of the vlong
		
		//read the prox pointer
		readVLong(data, index, longResults);
		termInfo.proxPointer = longResults[0];
		index += longResults[1]; //increment the length of the vlong
		
		//read the long pointer
		readVLong(data, index, longResults);
		long pointer = longResults[0];
		
		//perform the seek
		enumerator.seek(pointer,
	              (indexOffset * totalIndexInterval) - 1,
	              term, termInfo);
	}

	/**
	 * Binary search for the given term.
	 */
	@Override
	public int getIndexOffset(Term term) {
		int lo = 0; // binary search indexTerms[]
		int hi = indexToTerms.length - 1;
		int[] buffer = new int[2];
//		getBytes(term);
		while (hi >= lo) {
			int mid = (lo + hi) >>> 1;
			int delta = compare(term, mid, buffer);
			if (delta < 0)
				hi = mid - 1;
			else if (delta > 0)
				lo = mid + 1;
			else 
				return mid;
		}
		return hi;
	}
	
	@Override
	public int length() {
		return indexToTerms.length;
	}
	
	@Override
	public int compareTo(Term term, int termIndex) {
//		getBytes(term);
		int[] buffer = new int[2]; //creating a buffer for return multiple values during vint reads
		return compare(term, termIndex, buffer);
	}
	
	private int compare(Term term, int termIndex, int[] results) {
		// if term field does not equal mid's field index, then compare fields
		// else if they are equal, compare term's string values...
		int c = compareField(term, termIndex, results);
		return c == 0 ? compareText(term, termIndex,
				results[1] /* needed to move the data index position because of the field vint length*/
		        , results) : c;
	}
	
	private int compareText(Term term, int termIndex, int fieldLengthOffset, int[] buffer) {
		//this method may need work when it comes to uni-code values
		//but handling all of them as int[] arrays seems to work
		
		byte[] s2b = data;
		
		//fetch the position of the term information and add the length of the field pointer vint
		int indexOfStr2 = indexToTerms[termIndex] + fieldLengthOffset;
		readVInt(data, indexOfStr2, buffer);
		
		char[] s1b = term.text.toCharArray();
		
		int len1 = s1b.length;
		int len2 = buffer[0];
		int n = Math.min(len1, len2);
		int i = 0;
		int j = indexOfStr2 + buffer[1];  //index of the binary of the string plus the vint offset
		
		//lexicographical compare
		while (n-- != 0) {
			char c1 = s1b[i++];
			readVInt(s2b, j, buffer); //needed to use ints here (vint for better compaction) because of uni-code
			char c2 = (char) buffer[0];
			j += buffer[1];
			if (c1 != c2) {
				return c1- c2;
			}
		}
		return len1 - len2;
	}

	private int compareField(Term term, int termIndex, int[] buffer) {
		return term.field.compareTo(getFieldTerm(indexToTerms[termIndex], buffer).field);
	}

	/**
	 * Fetches the field pointer from the data array given the data index value.
	 * @param dataIndex the data index. 
	 * @param results used for calling readvint method.
	 * @return the Term field.
	 */
	private Term getFieldTerm(int dataIndex, int[] results) {
		readVInt(data, dataIndex, results);
		return fields[results[0]];
	}
	
	/**
	 * Reads an integer from the data array at offset and populates the results array.  
	 * Position 0 is the value read, and position 1 is the amount of bytes read.
	 * @param data the data byte array.
	 * @param offset the offset into the array.
	 * @param results the value and number of bytes read from the array.
	 */
	private static void readVInt(byte[] data, int offset, int[] results) {
		int originalOffset = offset;
		byte b = data[offset++];
		int i = b & 0x7F;
		for (int shift = 7; (b & 0x80) != 0; shift += 7) {
			b = data[offset++];
			i |= (b & 0x7F) << shift;
		}
		results[0] = i;
		results[1] = offset - originalOffset;
	}
	
	/**
	 * Reads an long from the data array at offset and populates the results array.  
	 * Position 0 is the value read, and position 1 is the amount of bytes read.
	 * @param data the data byte array.
	 * @param offset the offset into the array.
	 * @param results the value and number of bytes read from the array.
	 */
	private static void readVLong(byte[] data, int offset, long[] results) {
		int originalOffset = offset;
		byte b = data[offset++];
	    long i = b & 0x7F;
	    for (int shift = 7; (b & 0x80) != 0; shift += 7) {
	      b = data[offset++];
	      i |= (b & 0x7FL) << shift;
	    }
		results[0] = i;
		results[1] = offset - originalOffset;
	}

	/**
	 * Gets bytes form the text of the term and sets the text data reference for reuse in other segments.
	 * @param term the term.
	 */
//	private static void getBytes(Term term) {
//		if (term.textData != null) {
//			return;
//		}
//		String text = term.text;
//		int[] data = new int[text.length()];
//		for (int i = 0; i < data.length; i++) {
//			data[i] = text.charAt(i);
//		}
//		term.textData = data;
//	}

	/**
	 * Gets a string object from the data array at the offset with length provided.
	 * @param data the data byte array.
	 * @param offset the offset to start reading the string.
	 * @param length the string.
	 * @return the string generated.
	 */
	private static String getString(byte[] data, int offset, int[] results, long[] buffer) {
		int length = results[0];
		results[1] = 0;
    	char[] chars = new char[length];
	   	for (int i = 0; i < length; i++) {
	   		readVLong(data, offset, buffer);
	   		chars[i] = (char) buffer[0];
	   		offset += buffer[1];
	   		results[1] += buffer[1];
	    }
		return new String(chars);
	}

	private static class CustomDataOutputStream extends DataOutputStream {
		CustomDataOutputStream(OutputStream out) {
			super(out);
		}
		void writeString(String s) throws IOException {
			int length = s.length();
			writeVInt(length);
			for (int i = 0; i < length; i++) {
				writeVLong(s.charAt(i));
			}
		}
		void writeVInt(int i) throws IOException {
			while ((i & ~0x7F) != 0) {
				writeByte((byte) ((i & 0x7f) | 0x80));
				i >>>= 7;
			}
			writeByte((byte) i);
		}
		void writeVLong(long i) throws IOException {
			while ((i & ~0x7F) != 0) {
				writeByte((byte) ((i & 0x7f) | 0x80));
				i >>>= 7;
			}
			writeByte((byte) i);
		}
	}
}