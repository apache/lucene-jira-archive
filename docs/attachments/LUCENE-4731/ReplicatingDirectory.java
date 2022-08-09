import java.io.Closeable;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.index.SegmentInfoPerCommit;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.OutputStreamDataOutput;
import org.apache.lucene.util.NamedThreadFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Directory that replicates the index files to HDFS after a commit. 
 * Delegates all calls to an underlying Directory that is passed in through
 * the constructor.
 *
 * Uses a scheduler thread to serialize remote operations. We delete remote
 * files when they are deleted through this Directory, and we copy any new
 * files over when a commit happens (when segmenets.gen is sync'd). A shared
 * thread pool is use to concurrently copy/delete files during a replication.
 * Individual deletes (from Directory#deleteFile) are run by the scheduler
 * thread.
 */
public class ReplicatingDirectory extends Directory implements Closeable {

  private static Logger log = LoggerFactory.getLogger(ReplicatingDirectoryFactory.class);

  private static final ExecutorService executor =
      Executors.newCachedThreadPool(new NamedThreadFactory("lucene.replication.worker") {
        @Override
        public Thread newThread(Runnable r) {
          Thread t = super.newThread(r);
          t.setPriority(Thread.MIN_PRIORITY);
          return t;
        }
      });

  /* 
   * Encapuslate the state of an index replication
   *
   * Uses a shared thread pool to asynchronously copy and delete remote files.
   * Only one of these may run at a time (though multiple may be submitted).
   * Once all files have been copied/deleted, a new segments.gen is generated
   * on the remote FileSystem
   */
  private static final class DirectoryReplicator implements Runnable {
    private final Directory dir;
    private final FileSystem hdfs;
    private final Path base;
    private final int version;
    private final long gen;
    private final Collection<String> names;

    public DirectoryReplicator(Directory dir, FileSystem hdfs, Path base, int version, long gen, String[] names) {
      this.dir = dir;
      this.hdfs = hdfs;
      this.base = base;
      this.version = version;
      this.gen = gen;
      this.names = Collections.unmodifiableCollection(Arrays.asList(names));
    }

    /**
     * Calculates a list of files to be copied and files to be deleted. Submits
     * a thread for each remote operation, and generates a new segments.gen file
     * when all the other operations have completed.
     *
     * If any of the operations fail, just abort the whole thing.
     */
    @Override
    public void run() {
      try {
        log.info("Replicating index files to remote FileSystem");
        
        // Get list of files to copy and files to delete
        Set<String> filesToCopy = new HashSet<String>(names);
        Set<String> filesToDelete = new HashSet<String>();
        try {
          FileStatus[] remoteFiles = hdfs.listStatus(base);
          filesToCopy.remove(IndexWriter.WRITE_LOCK_NAME);
          for(FileStatus fstat : remoteFiles) {
            String remoteName = fstat.getPath().getName();
            if(filesToCopy.contains(remoteName)) {
              filesToCopy.remove(remoteName);
            } else {
              filesToDelete.add(remoteName);  
            }
          }
        } catch (IOException e) {
          log.warn("Could not list remote files, aborting replication.", e);
          return;
        }
        log.info("Copying {} files", filesToCopy.size());
        log.info("Deleting {} files", filesToDelete.size());

        final CountDownLatch doneLatch = 
            new CountDownLatch(filesToCopy.size() + filesToDelete.size());
        final ConcurrentMap<String, Future> futures = 
            new ConcurrentHashMap<String, Future>(filesToCopy.size() + filesToDelete.size());
   
        // Copy the files to remote FS. 
        for(final String fileName: filesToCopy) {
          Future future = executor.submit(new Runnable() {
            @Override
            public void run() {
              try {
                // TODO, better way to do this?
                if(dir.fileExists(fileName)) {
                  IndexInput input = dir.openInput(fileName, IOContext.READONCE);
                  OutputStream output = hdfs.create(new Path(base, fileName));
                  byte[] buffer = new byte[4096];
                  if(input.length() <= buffer.length) {
                    input.readBytes(buffer, 0, (int)input.length());
                    output.write(buffer, 0, (int)input.length());
                  } else {
                    for(;;) {
                      input.readBytes(buffer, 0, buffer.length);
                      long left = input.length() - input.getFilePointer();
                      if(left >= buffer.length) {
                        output.write(buffer, 0, buffer.length);
                      } else {
                        output.write(buffer, 0, (int)left);
                        break;
                      }
                    }
                  }
                  input.close();
                  output.close();
                  log.info("Copied {}", fileName);
                } else {
                  // Must have been deleted since we started
                }
              } catch (FileNotFoundException e) {
                // Must have been deleted since we started
              } catch (IOException e) {
                log.error("Failed to copy " + fileName, e);
                throw new RuntimeException(e);
              } finally {
                doneLatch.countDown();
              }
            }
          });
          futures.put(fileName, future);
        }

        // Delete remote files
        for(final String fileName: filesToDelete) {
          Future future = executor.submit(new Runnable() {
            @Override
            public void run() {
              try {
                hdfs.delete(new Path(base, fileName), false);
                log.info("Deleted {}", fileName);
              } catch (IOException e) {
                log.error("Failed to delete " + fileName, e);
                throw new RuntimeException(e);
              } finally {
                doneLatch.countDown();
              }
            }
          });
          futures.put(fileName, future);
        }

        // Atomically create segments.gen once other files are done copying
        doneLatch.await();
        log.info("Finished replicating index files, checking success.");
        for(Map.Entry<String, Future> entry: futures.entrySet()) {
          try {
            entry.getValue().get(50, TimeUnit.MILLISECONDS);
          } catch (Exception e) {
            log.warn("Failed to replicate {}, aborting replication.", entry.getKey());
            return;
          }
        }
        log.info("All files successfully replicated. Finalizing replication");
        // Write out a new segments.gen directly rather than copying
        // TODO is there a class that does this?
        OutputStreamDataOutput out = new OutputStreamDataOutput(
          hdfs.create(new Path(base, IndexFileNames.SEGMENTS_GEN + ".tmp"), true)
        );
        out.writeInt(version);
        out.writeLong(gen);
        out.writeLong(gen);
        out.close();
        hdfs.rename(new Path(base, IndexFileNames.SEGMENTS_GEN + ".tmp"), 
                    new Path(base, IndexFileNames.SEGMENTS_GEN)
        );
        log.info("Copied {}", IndexFileNames.SEGMENTS_GEN);
      } catch (Exception e) {
        log.error("Failed to replicate index to remote FileSystem.", e);
        throw new RuntimeException(e);
      }
    }
  }

  private final FSDirectory other;
  private final FileSystem fs;
  private final Path path;
  private final ExecutorService scheduler =
      Executors.newFixedThreadPool(1, new NamedThreadFactory("lucene.replication.scheduler"));

  public ReplicatingDirectory(FSDirectory other, Configuration conf, Path path) throws IOException {
    this.other = other;
    this.fs = path.getFileSystem(conf);
    this.path = fs.makeQualified(path);
    log.info("Initializing {}", this);
  }

  @Override
  public String[] listAll() throws IOException {
    return other.listAll();
  }

  @Override
  public boolean fileExists(String name) throws IOException {
    return other.fileExists(name);
  }

  @Override
  public void deleteFile(final String name) throws IOException {
    other.deleteFile(name);
    scheduler.submit(new Runnable() {
      @Override
      public void run() {
        try {
          fs.delete(new Path(path, name), false);
          log.info("Deleted {}", name);
        } catch (IOException e) {
          log.error("Failed to delete " + name, e);
        }
      }
    });
  }

  @Override
  public long fileLength(String name) throws IOException {
    return other.fileLength(name);
  }

  @Override
  public IndexOutput createOutput(String name, IOContext context) throws IOException {
    return other.createOutput(name, context);
  }

  @Override
  public void sync(Collection<String> names) throws IOException {
    other.sync(names);
    // If we are writing segments.gen, we know there has been
    // a commit, so we initiate a replication
    if(names.contains(IndexFileNames.SEGMENTS_GEN)) {
      SegmentInfos segInfos = new SegmentInfos();
      segInfos.read(this);
      try {
        IndexInput genInput = openInput(IndexFileNames.SEGMENTS_GEN, IOContext.READONCE);
        int version = genInput.readInt();
        long gen0 = genInput.readLong();
        long gen1 = genInput.readLong();
        genInput.close();
        scheduler.submit(new DirectoryReplicator(this, fs, path, version, gen0, listAll()));
      } catch (IOException e) {
        log.warn("Could not read {}, skipping replication", IndexFileNames.SEGMENTS_GEN);
      }
    }
  }

  @Override
  public IndexInput openInput(String name, IOContext context) throws IOException {
    return other.openInput(name, context);
  }

  @Override
  public Lock makeLock(String name) {
    return other.makeLock(name);
  }

  @Override
  public void clearLock(String name) throws IOException {
    other.clearLock(name);
  }

  @Override
  public void close() throws IOException {
    scheduler.shutdown();
    executor.shutdown();
    other.close();
  }

  @Override
  public void setLockFactory(LockFactory lockFactory) throws IOException {
    other.setLockFactory(lockFactory);
  }

  @Override
  public LockFactory getLockFactory() {
    return other.getLockFactory();
  }

  @Override
  public String getLockID() {
    return other.getLockID();
  }

  @Override
  public String toString() {
    return "ReplicatingDirectory(" + other.toString() + ", " + path.toString() + ")";
  }

  @Override
  public void copy(Directory to, String src, String dest, IOContext context) throws IOException {
    other.copy(to, src, dest, context);
  }

  @Override
  public Directory.IndexInputSlicer createSlicer(final String name, final IOContext context) throws IOException {
    return other.createSlicer(name, context);
  }
}
