import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ListAnalysisComponents {

    private static Map<String, String> tokenizerNames() {
        Set<String> names = TokenizerFactory.availableTokenizers();
        var res = new HashMap<String, String>();
        for (String name : names) {
            res.put(name, TokenizerFactory.lookupClass(name).getName());
        }
        return res;
    }

    private static Map<String, String> charfilterNames() {
        Set<String> names = CharFilterFactory.availableCharFilters();
        var res = new HashMap<String, String>();
        for (String name : names) {
            res.put(name, CharFilterFactory.lookupClass(name).getName());
        }
        return res;
    }

    private static Map<String, String> tokenfilterNames() {
        Set<String> names = TokenFilterFactory.availableTokenFilters();
        var res = new HashMap<String, String>();
        for (String name : names) {
            res.put(name, TokenFilterFactory.lookupClass(name).getName());
        }
        return res;
    }

    private static void save(Path path, Map<String, String> spiNameMap) {
        try(BufferedWriter w = Files.newBufferedWriter(path)) {
            for (Map.Entry<String, String> e : spiNameMap.entrySet()) {
                w.write(e.getKey() + "\t" + e.getValue() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String dataDir = "data";
        save(Paths.get(dataDir, "tokenizers.txt"), tokenizerNames());
        save(Paths.get(dataDir, "charfilters.txt"), charfilterNames());
        save(Paths.get(dataDir, "tokenfilters.txt"), tokenfilterNames());
    }
}
