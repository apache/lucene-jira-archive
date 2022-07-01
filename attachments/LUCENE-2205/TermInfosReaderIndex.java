package org.apache.lucene.index;

import java.io.IOException;

public abstract class TermInfosReaderIndex {

	public abstract void build(SegmentTermEnum indexEnum, int indexDivisor, int fileLength) throws IOException;

	public abstract int length();

	public abstract int getIndexOffset(Term term);

	public abstract void seekEnum(SegmentTermEnum enumerator, int indexOffset, int totalIndexInterval) throws IOException;

	public abstract int compareTo(Term term, int enumOffset);

}
