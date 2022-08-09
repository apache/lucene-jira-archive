package org.apache.lucene.store;

import junit.framework.TestCase;

/**
 * @author kimchy
 */
public class RAMOutputTest extends TestCase {

    public void testRAMAndBufferedOutputExactSize() throws Exception {
        RAMOutputStream os = new RAMOutputStream();
        byte[] data = new byte[BufferedIndexOutput.BUFFER_SIZE + 1];
        os.writeBytes(data, data.length);
        assertEquals(os.getFilePointer(), data.length);
    }
}
