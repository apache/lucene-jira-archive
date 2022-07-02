import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import junit.framework.TestCase;

public class DirectoryTest extends TestCase {

    public void testRAMDirectory() throws IOException {
        checkDirectory(new RAMDirectory());
    }

    public void testFSDirectory() throws IOException {
        checkDirectory(FSDirectory.getDirectory("test"));
    }

    private void checkDirectory(Directory dir) throws IOException {
        String name = "file";
        try {
            dir.createOutput(name).close();
            assertTrue(dir.fileExists(name));
            assertTrue(Arrays.asList(dir.list()).contains(name));
        } finally {
            dir.close();
        }
    }
}
