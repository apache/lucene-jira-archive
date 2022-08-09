import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SPINamesGenerator {

    private static Map<String, String> charFilterNames() {
        Set<String> names = CharFilterFactory.availableCharFilters();
        Map<String, String> res = new HashMap<>();
        for (String name : names) {
            var clazz = CharFilterFactory.lookupClass(name);
            if (Objects.equals(CharFilterFactory.class, clazz)) {
                continue;
            }
            res.put(clazz.getSimpleName(), convert(clazz.getSimpleName(), "CharFilterFactory"));
        }
        return res;
    }

    private static Map<String, String> tokenizerNames() {
        Set<String> names = TokenizerFactory.availableTokenizers();
        Map<String, String> res = new HashMap<>();
        for (String name : names) {
            var clazz = TokenizerFactory.lookupClass(name);
            if (Objects.equals(TokenizerFactory.class, clazz)) {
                continue;
            }
            res.put(clazz.getSimpleName(), convert(clazz.getSimpleName(), "TokenizerFactory"));
        }
        return res;
    }

    private static Map<String, String> tokenfilterNames() {
        Set<String> names = TokenFilterFactory.availableTokenFilters();
        Map<String, String> res = new HashMap<>();
        for (String name : names) {
            var clazz = TokenFilterFactory.lookupClass(name);
            if (Objects.equals(TokenFilterFactory.class, clazz)) {
                continue;
            }
            if (clazz.getSimpleName().endsWith("TokenFilterFactory")) {
                res.put(clazz.getSimpleName(), convert(clazz.getSimpleName(), "TokenFilterFactory"));
            } else {
                res.put(clazz.getSimpleName(), convert(clazz.getSimpleName(), "FilterFactory"));
            }
        }
        return res;
    }

    private static String convert(String className, String suffix) {
        String s = className.replaceAll(suffix + "$", "");
        char[] ary = new char[s.length()];
        ary[0] = s.substring(0, 1).toLowerCase().charAt(0);
        for (int i = 1; i < ary.length; i++) {
            char c = s.charAt(i);
            if (!isUpperCase(c) || !isUpperCase(s.charAt(i-1)) || (i < ary.length-1 && !isUpperCase(s.charAt(i+1)))) {
                ary[i] = c;
            } else {
                ary[i] = s.substring(i, i+1).toLowerCase().charAt(0);
            }
        }
        return String.copyValueOf(ary);
    }

    private static boolean isUpperCase(char c) {
        return c >= 'A' && c <= 'Z';
    }

    private static void save(String file, Map<String, String> spiNameMap) {
        try(BufferedWriter w = Files.newBufferedWriter(Paths.get(file))) {
            for (Map.Entry<String, String> e : spiNameMap.entrySet()) {
                w.write(e.getKey() + "\n");
                w.write("public static final String NAME = \"" + e.getValue() + "\";\n\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        save("charfilters.txt", charFilterNames());
        save("tokenizers.txt", tokenizerNames());
        save("tokenfilters.txt", tokenfilterNames());
    }
}
