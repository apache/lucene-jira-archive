
import java.io.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;

public class Test {
    private static boolean down = false;

    public static void main(String[] args) throws Exception {
        System.setProperty("org.apache.lucene.FSDirectory.class", FaultyFSDirectory.class.getName());
        File dir = new File("textindex");
        IndexReader reader = IndexReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);

        Hits hits = searcher.search(new MatchAllDocsQuery());
        down = true;
        for (int i = 0; i < hits.length(); i++) {
            try {
                hits.doc(i);
            } catch (IOException e) {
                System.out.println("Bang occurred");
            }
        }

        searcher.close();
        reader.close();
    }

    public static class FaultyFSDirectory extends FSDirectory {
        public IndexInput openInput(String name) throws IOException {
            return new FaultyIndexInput(super.openInput(name));
        }
        public IndexInput openInput(String name, int bufferSize) throws IOException {
            return new FaultyIndexInput(super.openInput(name, bufferSize));
        }
    }

    private static class FaultyIndexInput extends BufferedIndexInput {
        IndexInput delegate;
        private FaultyIndexInput(IndexInput delegate) {
            this.delegate = delegate;
        }
        private void simOutage() throws IOException {
            if (down && Math.random() < 0.5) {
                throw new IOException("Simulated network outage");
            }
        }
        public void readInternal(byte[] b, int offset, int length) throws IOException {
            simOutage();
            delegate.readBytes(b, offset, length);
        }
        public void seekInternal(long pos) throws IOException {
            simOutage();
            delegate.seek(pos);
        }
        public long length() {
            return delegate.length();
        }
        public void close() throws IOException {
            delegate.close();
        }
    }
}
