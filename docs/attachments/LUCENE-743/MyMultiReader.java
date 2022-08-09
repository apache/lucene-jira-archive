package org.apache.lucene.index;

import java.io.IOException;

import org.apache.lucene.store.Directory;

/**
 * overridden to allow retrieval of contained IndexReader's to enable IndexReaderUtils.reopen()
 */
public class MyMultiReader extends MultiReader {

    private IndexReader[] readers;
    
    public MyMultiReader(Directory directory,SegmentInfos infos,IndexReader[] subReaders) throws IOException {
        super(directory,infos,true,subReaders);
        readers = subReaders;
    }
    
    public IndexReader[] getReaders() {
        return readers;
    }
    
    public void doCommit() throws IOException {
        super.doCommit();
    }
}
