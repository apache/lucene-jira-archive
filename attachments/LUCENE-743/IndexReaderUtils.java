package org.apache.lucene.index;

import java.io.IOException;

import org.apache.lucene.store.*;

public class IndexReaderUtils {
    static {
        // must use String class name, otherwise instantiation order will not allow the override to work
        System.setProperty("org.apache.lucene.SegmentReader.class","org.apache.lucene.index.MySegmentReader");
    }
    
    /**
     * reopens the IndexReader, possibly reusing the segments for greater efficiency. The original IndexReader instance
     * is closed, and the reference is no longer valid
     * 
     * @return the new IndexReader
     */
    public static synchronized IndexReader reopen(IndexReader ir) throws IOException {
        final Directory directory = ir.directory();
        
        if(!(ir instanceof MyMultiReader)) {
            SegmentInfos infos = new SegmentInfos();
            infos.read(directory);
            IndexReader[] readers = new IndexReader[infos.size()];
            for(int i=0;i<infos.size();i++){
                readers[i] = MySegmentReader.get((SegmentInfo) infos.get(i));
            }
//            System.err.println("reopen, fresh reader with "+infos.size()+" segments");
            return new MyMultiReader(directory,infos,readers);
        }
        
        MyMultiReader mr = (MyMultiReader) ir;
        
        final IndexReader[] oldreaders = mr.getReaders();
        final boolean[] stayopen = new boolean[oldreaders.length];
        
        synchronized (directory) {            // in- & inter-process sync
            return (IndexReader)new Lock.With(
                directory.makeLock(IndexWriter.COMMIT_LOCK_NAME),
                IndexWriter.COMMIT_LOCK_TIMEOUT) {
                public Object doBody() throws IOException {
                  SegmentInfos infos = new SegmentInfos();
                  infos.read(directory);
                  if (infos.size() == 1) {        // index is optimized
                      SegmentInfo si = (SegmentInfo) infos.get(0);
//                      System.err.println("single segment "+si.name+" during reopen");
                    return MySegmentReader.get(si);
                  } else {
//                    System.err.println("reopen, has "+infos.size()+" segments");
                    IndexReader[] readers = new IndexReader[infos.size()];
                    for (int i = 0; i < infos.size(); i++) {
                        SegmentInfo newsi = (SegmentInfo) infos.get(i);
                        for(int j=0;j<oldreaders.length;j++) {
                            MySegmentReader sr = (MySegmentReader) oldreaders[j];
                            SegmentInfo si = sr.si;
                            if(si!=null && si.name.equals(newsi.name)) {
                                readers[i]=sr;
                                // no need to call reopen, since only a single reader is used, the
                                // segments' deletions must be up to date
                                // ((MySegmentReader)sr).reopen();
                                stayopen[j]=true;
//                                System.err.println("keeping "+si.name+" on reopen");
                            }
                        }
                        
                        if(readers[i]==null) {
//                            System.err.println("new open on "+newsi.name);
                            readers[i] = MySegmentReader.get(newsi);
                        }
                    }
                    
                    for(int i=0;i<stayopen.length;i++)
                        if(!stayopen[i])
                            oldreaders[i].close();
                        
                    return new MyMultiReader(directory,infos,readers);
                  }
                }
              }.run();
          }
    }

    public static synchronized IndexReader open(String path) throws IOException {
        Directory d = FSDirectory.getDirectory(path,false);
        return open(d,true);
    }
    
    private static IndexReader open(final Directory directory, final boolean closeDirectory) throws IOException {
        synchronized (directory) {            // in- & inter-process sync
          return (IndexReader)new Lock.With(
              directory.makeLock(IndexWriter.COMMIT_LOCK_NAME),
              IndexWriter.COMMIT_LOCK_TIMEOUT) {
              public Object doBody() throws IOException {
                SegmentInfos infos = new SegmentInfos();
                infos.read(directory);
                if (infos.size() == 1) {          // index is optimized
                  return MySegmentReader.get(infos.info(0));
                } else {
                  IndexReader[] readers = new IndexReader[infos.size()];
                  for (int i = 0; i < infos.size(); i++) {
                      SegmentInfo si = infos.info(i);
                    readers[i] = MySegmentReader.get(si);
                  }
                  return new MyMultiReader(directory,infos,readers);
                }
              }
            }.run();
        }
    }
}
