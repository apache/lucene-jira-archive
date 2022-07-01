import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class TestSPINames {

    private static final String DATADIR = "data";

    @Test
    public void testTokenizerNames() throws IOException {
        var lines = Files.readAllLines(Paths.get(DATADIR,"tokenizers.txt"), StandardCharsets.UTF_8);
        for (String line : lines) {
            String[] ary = line.split("\t");
            String spiName = ary[0];
            String className = ary[1];
            assertEquals(className, TokenizerFactory.lookupClass(spiName).getName());
        }
    }

    @Test
    public void testCharfilterNames() throws IOException {
        var lines = Files.readAllLines(Paths.get(DATADIR, "charfilters.txt"), StandardCharsets.UTF_8);
        for (String line : lines) {
            String[] ary = line.split("\t");
            String spiName = ary[0];
            String className = ary[1];
            assertEquals(className, CharFilterFactory.lookupClass(spiName).getName());
        }
    }

    @Test
    public void testTokenfilterNames() throws IOException {
        var lines = Files.readAllLines(Paths.get(DATADIR, "tokenfilters.txt"), StandardCharsets.UTF_8);
        for (String line : lines) {
            String[] ary = line.split("\t");
            String spiName = ary[0];
            String className = ary[1];
            assertEquals(className, TokenFilterFactory.lookupClass(spiName).getName());

        }
    }
}
