package org.apache.lucene.test;

import java.io.IOException;

import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexInput;

public class CorruptionCheckerForPreLucene3
{
	/**
	 * Added safety check on last batch of additions for < 3.0 based Lucene implementations
	 * See https://issues.apache.org/jira/browse/LUCENE-3255
	 * @param dir
	 * @return true if OK false if a potentially corrupted segments file was removed
	 * @throws IOException
	 */
	public static boolean checkLastCommitOk(FSDirectory dir) throws IOException
	{
		String latestSeg = SegmentInfos.getCurrentSegmentFileName(dir);
		IndexInput input = dir.openInput(latestSeg);
		int format=input.readInt();
		input.close();
		if(format!=SegmentInfos.FORMAT_DIAGNOSTICS)
		{
			dir.deleteFile(latestSeg);
			return false;
		}
		return true;
	}
}
