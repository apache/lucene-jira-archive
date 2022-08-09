package org.apache.lucene.index;

import java.io.IOException;

import org.apache.lucene.util.BitVector;

public class MySegmentReader extends SegmentReader {
    SegmentInfo si;

    public MySegmentReader() {
    }
    
    public void reopen() throws IOException {
        if (hasDeletions(si))
            deletedDocs = new BitVector(directory(), si.name + ".del");
    }
    
    public static SegmentReader get(SegmentInfo si) throws IOException {
        MySegmentReader reader = (MySegmentReader) SegmentReader.get(si);
        reader.si = si;
        return reader;
    }
    
    public String toString(){
    	return "MySegmentReader("+si.name+")";
    }
}
