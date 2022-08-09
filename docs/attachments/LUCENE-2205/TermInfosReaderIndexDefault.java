package org.apache.lucene.index;

import java.io.IOException;

public class TermInfosReaderIndexDefault extends TermInfosReaderIndex {

	private Term[] indexTerms;
	private TermInfo[] indexInfos;
	private long[] indexPointers;

	@Override
	public void build(SegmentTermEnum indexEnum, int indexDivisor, int fileLength) throws IOException {
//		System.out.println("building default");
		int indexSize = 1 + ((int) indexEnum.size - 1) / indexDivisor; // otherwise
																		// read
																		// index
		indexTerms = new Term[indexSize];
		indexInfos = new TermInfo[indexSize];
		indexPointers = new long[indexSize];

		for (int i = 0; indexEnum.next(); i++) {
			indexTerms[i] = indexEnum.term();
			indexInfos[i] = indexEnum.termInfo();
			indexPointers[i] = indexEnum.indexPointer;

			for (int j = 1; j < indexDivisor; j++)
				if (!indexEnum.next())
					break;
		}
	}

	@Override
	public int compareTo(Term term, int enumOffset) {
		return term.compareTo(indexTerms[enumOffset]);
	}

	@Override
	public int getIndexOffset(Term term) {
		int lo = 0; // binary search indexTerms[]
		int hi = indexTerms.length - 1;

		while (hi >= lo) {
			int mid = (lo + hi) >>> 1;
			int delta = term.compareTo(indexTerms[mid]);
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
		return indexTerms.length;
	}

	@Override
	public void seekEnum(SegmentTermEnum enumerator, int indexOffset, int totalIndexInterval) throws IOException {
		enumerator.seek(indexPointers[indexOffset], (indexOffset * totalIndexInterval) - 1, indexTerms[indexOffset],
				indexInfos[indexOffset]);

	}

}
