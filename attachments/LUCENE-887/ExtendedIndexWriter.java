package org.apache.lucene.index;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IndexOutput;


/**
 * This class extends Lucene's IndexWriter and adds a shutdown() method: 
 * This method may always be called, even while a different
 * thread is using an addDocument() or updateDocument() method at the same time. 
 * A call of shutdown() causes the IndexWriter to flush the buffered docs and
 * close the IndexWriter. Cascading merges are not triggered.
 *  
 */
public class ExtendedIndexWriter extends IndexWriter {
    // counts how many threads are currently calling addDocument() or updateDocument() 
    private int inAddDocumentCount = 0;
    
    // addDocument() or updateDocument() call notify on this object
    // before returning. shutdown() waits on this object until
    // inAddDocumentCount == 0
    private Object waitForAddDocument = new Object();
    
    // whether shutdown() has been called
    private boolean shutdownCalled = false;
    
    /**
     * @see IndexWriter#IndexWriter(Directory, Analyzer)
     */
    public ExtendedIndexWriter(Directory d, Analyzer a) throws IOException {
        super(d, a);
    }
    
    /**
     * @see IndexWriter#IndexWriter(Directory, Analyzer, boolean)
     */
    public ExtendedIndexWriter(Directory d, Analyzer a, boolean create) throws IOException {
        super(d, a, create);
    }

    /**
     * @see IndexWriter#IndexWriter(File, Analyzer, boolean)
     */

    public ExtendedIndexWriter(File path, Analyzer a, boolean create) throws IOException {
        super(path, a, create);
    }

    /**
     * @see IndexWriter#IndexWriter(File, Analyzer)
     */
    public ExtendedIndexWriter(File path, Analyzer a) throws IOException {
        super(path, a);
    }

    /**
     * @see IndexWriter#IndexWriter(String, Analyzer, boolean)
     */
    public ExtendedIndexWriter(String path, Analyzer a, boolean create) throws IOException {
        super(path, a, create);
    }

    /**
     * @see IndexWriter#IndexWriter(String, Analyzer)
     */
    public ExtendedIndexWriter(String path, Analyzer a) throws IOException {
        super(path, a);
    }

    private void checkShutdownCalled() throws IOException {
        if (this.shutdownCalled) {
            throw new IOException("IndexWriter already closed.");
        }
    }
    
    // Increments inAddDocumentCount
    private void startAddDocument() throws IOException {
        synchronized(this.waitForAddDocument) {
            checkShutdownCalled();
            this.inAddDocumentCount++;
        }
    }
    
    // Decrements inAddDocumentCount
    private void endAddDocument() {
        synchronized(this.waitForAddDocument) {
            this.inAddDocumentCount--;
            this.waitForAddDocument.notify();
        }
    }
    
    // waits until no thread is in a call of addDocument() or updateDocument()
    private void waitForAddDocument() {
        synchronized(this.waitForAddDocument) {
            this.shutdownCalled = true;
            while(this.inAddDocumentCount > 0) {
                try {
                    this.waitForAddDocument.wait();
                } catch (InterruptedException e) {
                    // should never happen
                }
            }
        }
    }
    
    /**
     * @see IndexWriter#addDocument(Document)
     */
    public void addDocument(Document d) throws IOException {
        startAddDocument();
        try {
            super.addDocument(d);
        } catch (IndexWriterInterruptException e) {
            flushAfterInterrupt();
        } finally {
            endAddDocument();
        }
    }

    /**
     * @see IndexWriter#addDocument(Document, Analyzer)
     */
    public void addDocument(Document d, Analyzer a) throws IOException {
        startAddDocument();
        try {
            super.addDocument(d, a);
        } catch (IndexWriterInterruptException e) {
            flushAfterInterrupt();
        } finally {
            endAddDocument();
        }
    }
    
    /**
     * @see IndexWriter#updateDocument(Term, Document)
     */
    public void updateDocument(Term t, Document d) throws IOException {
        startAddDocument();
        try {
            super.updateDocument(t, d);
        } catch (IndexWriterInterruptException e) {
            flushAfterInterrupt();
        } finally {
            endAddDocument();
        }
    }

    /**
     * @see IndexWriter#updateDocument(Term, Document, Analyzer)
     */
    public void updateDocument(Term t, Document d, Analyzer a) throws IOException {
        startAddDocument();
        try {
            super.updateDocument(t, d, a);
        } catch (IndexWriterInterruptException e) {
            flushAfterInterrupt();
        } finally {
            endAddDocument();
        }
    }

    public synchronized void deleteDocuments(Term term) throws IOException {
        checkShutdownCalled();
        try {
            super.deleteDocuments(term);
        } catch (IndexWriterInterruptException e) {
            flushAfterInterrupt();
        } 
    }

    public synchronized void deleteDocuments(Term[] terms) throws IOException {
        checkShutdownCalled();
        try {
            super.deleteDocuments(terms);
        } catch (IndexWriterInterruptException e) {
            flushAfterInterrupt();
        } 
    }

    
    private void flushAfterInterrupt() throws IOException {
        ((ExtendedFSDirectory) getDirectory()).clearInterrupt();
        super.flushRamSegments(false);
    }
    
    public void shutdown() throws IOException {
        Directory d = getDirectory();
        try {
            if (d instanceof ExtendedFSDirectory) {
                ((ExtendedFSDirectory) d).interrupt();
            }
            
            waitForAddDocument();
            
            try {
                super.flushRamSegments(false);
            } catch (IndexWriterInterruptException e) {
                flushAfterInterrupt();
            }
            
            close();
        } finally {
            if (d instanceof ExtendedFSDirectory) {
                ((ExtendedFSDirectory) d).clearInterrupt();
            }
        }
    }
    
    public static class ExtendedFSDirectory extends FSDirectory {
        private boolean interrupted = false;
        
        public void interrupt() {
            this.interrupted = true;
        }
        
        public void clearInterrupt() {
            this.interrupted = false;
        }
        
        public IndexOutput createOutput(String name) throws IOException {
            File file = new File(getFile(), name);
            if (file.exists() && !file.delete())          // delete existing, if any
              throw new IOException("Cannot overwrite: " + file);

            return new FSIndexOutput(file) {
                public void flushBuffer(byte[] b, int offset, int size) throws IOException {
                    if (ExtendedFSDirectory.this.interrupted) {
                        throw new IndexWriterInterruptException();
                    }
                    
                    super.flushBuffer(b, offset, size);
                }

            };
        }
    }
    
    
    // This exception is used to signal an interrupt request    
    static final class IndexWriterInterruptException extends IOException {
        private static final long serialVersionUID = 1L;
    }

}
