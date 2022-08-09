import java.io.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.charfilter.*;
import org.apache.lucene.analysis.cjk.*;
import org.apache.lucene.analysis.core.*;
import org.apache.lucene.analysis.ja.*;
import org.apache.lucene.analysis.ja.JapaneseTokenizer.Mode;
import org.apache.lucene.util.*;

// javac -cp build/core/classes/java:build/analysis/common/classes/java:build/analysis/kuromoji/classes/java PerfTestMappingCharFilter.java; java -cp .:build/core/classes/java:build/analysis/common/classes/java PerfTestMappingCharFilter

public class PerfTestMappingCharFilter {

  public static void main(String[] args) throws Exception {
    File file = new File("maps.fst");

    final NormalizeCharMap map;

    System.out.println("Build map...");
    final NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
    builder.add("\uff66\uff9e", "\u30fa");
    builder.add("\uff73\uff9e", "\u30f4");
    builder.add("\uff76\uff9e", "\u30ac");
    builder.add("\uff77\uff9e", "\u30ae");
    builder.add("\uff78\uff9e", "\u30b0");
    builder.add("\uff79\uff9e", "\u30b2");
    builder.add("\uff7a\uff9e", "\u30b4");
    builder.add("\uff7b\uff9e", "\u30b6");
    builder.add("\uff7c\uff9e", "\u30b8");
    builder.add("\uff7d\uff9e", "\u30ba");
    builder.add("\uff7e\uff9e", "\u30bc");
    builder.add("\uff7f\uff9e", "\u30be");
    builder.add("\uff80\uff9e", "\u30c0");
    builder.add("\uff81\uff9e", "\u30c2");
    builder.add("\uff82\uff9e", "\u30c5");
    builder.add("\uff83\uff9e", "\u30c7");
    builder.add("\uff84\uff9e", "\u30c9");
    builder.add("\uff8a\uff9e", "\u30d0");
    builder.add("\uff8a\uff9f", "\u30d1");
    builder.add("\uff8b\uff9e", "\u30d3");
    builder.add("\uff8b\uff9f", "\u30d4");
    builder.add("\uff8c\uff9e", "\u30d6");
    builder.add("\uff8c\uff9f", "\u30d7");
    builder.add("\uff8d\uff9e", "\u30d9");
    builder.add("\uff8d\uff9f", "\u30da");
    builder.add("\uff8e\uff9e", "\u30dc");
    builder.add("\uff8e\uff9f", "\u30dd");
    builder.add("\uff9c\uff9e", "\u30f7");
    builder.add("\uff01", "\u0021");
    //builder.add("\uff02", "\u0022");
    builder.add("\uff03", "\u0023");
    builder.add("\uff04", "\u0024");
    builder.add("\uff05", "\u0025");
    builder.add("\uff06", "\u0026");
    builder.add("\uff07", "\u0027");
    builder.add("\uff08", "\u0028");
    builder.add("\uff09", "\u0029");
    builder.add("\uff0a", "\u002a");
    builder.add("\uff0b", "\u002b");
    builder.add("\uff0c", "\u002c");
    builder.add("\uff0d", "\u002d");
    builder.add("\uff0e", "\u002e");
    builder.add("\uff0f", "\u002f");
    builder.add("\uff10", "\u0030");
    builder.add("\uff11", "\u0031");
    builder.add("\uff12", "\u0032");
    builder.add("\uff13", "\u0033");
    builder.add("\uff14", "\u0034");
    builder.add("\uff15", "\u0035");
    builder.add("\uff16", "\u0036");
    builder.add("\uff17", "\u0037");
    builder.add("\uff18", "\u0038");
    builder.add("\uff19", "\u0039");
    builder.add("\uff1a", "\u003a");
    builder.add("\uff1b", "\u003b");
    builder.add("\uff1c", "\u003c");
    builder.add("\uff1d", "\u003d");
    builder.add("\uff1e", "\u003e");
    builder.add("\uff1f", "\u003f");
    builder.add("\uff20", "\u0040");
    builder.add("\uff21", "\u0041");
    builder.add("\uff22", "\u0042");
    builder.add("\uff23", "\u0043");
    builder.add("\uff24", "\u0044");
    builder.add("\uff25", "\u0045");
    builder.add("\uff26", "\u0046");
    builder.add("\uff27", "\u0047");
    builder.add("\uff28", "\u0048");
    builder.add("\uff29", "\u0049");
    builder.add("\uff2a", "\u004a");
    builder.add("\uff2b", "\u004b");
    builder.add("\uff2c", "\u004c");
    builder.add("\uff2d", "\u004d");
    builder.add("\uff2e", "\u004e");
    builder.add("\uff2f", "\u004f");
    builder.add("\uff30", "\u0050");
    builder.add("\uff31", "\u0051");
    builder.add("\uff32", "\u0052");
    builder.add("\uff33", "\u0053");
    builder.add("\uff34", "\u0054");
    builder.add("\uff35", "\u0055");
    builder.add("\uff36", "\u0056");
    builder.add("\uff37", "\u0057");
    builder.add("\uff38", "\u0058");
    builder.add("\uff39", "\u0059");
    builder.add("\uff3a", "\u005a");
    builder.add("\uff3b", "\u005b");
    //builder.add("\uff3c", "\u005c");
    //builder.add("\uff3d", "\u005d");
    builder.add("\uff3e", "\u005e");
    builder.add("\uff3f", "\u005f");
    builder.add("\uff40", "\u0060");
    builder.add("\uff41", "\u0061");
    builder.add("\uff42", "\u0062");
    builder.add("\uff43", "\u0063");
    builder.add("\uff44", "\u0064");
    builder.add("\uff45", "\u0065");
    builder.add("\uff46", "\u0066");
    builder.add("\uff47", "\u0067");
    builder.add("\uff48", "\u0068");
    builder.add("\uff49", "\u0069");
    builder.add("\uff4a", "\u006a");
    builder.add("\uff4b", "\u006b");
    builder.add("\uff4c", "\u006c");
    builder.add("\uff4d", "\u006d");
    builder.add("\uff4e", "\u006e");
    builder.add("\uff4f", "\u006f");
    builder.add("\uff50", "\u0070");
    builder.add("\uff51", "\u0071");
    builder.add("\uff52", "\u0072");
    builder.add("\uff53", "\u0073");
    builder.add("\uff54", "\u0074");
    builder.add("\uff55", "\u0075");
    builder.add("\uff56", "\u0076");
    builder.add("\uff57", "\u0077");
    builder.add("\uff58", "\u0078");
    builder.add("\uff59", "\u0079");
    builder.add("\uff5a", "\u007a");
    builder.add("\uff5b", "\u007b");
    builder.add("\uff5c", "\u007c");
    builder.add("\uff5d", "\u007d");
    builder.add("\uff5e", "\u007e");
    builder.add("\uff65", "\u30fb");
    builder.add("\uff66", "\u30f2");
    builder.add("\uff67", "\u30a1");
    builder.add("\uff68", "\u30a3");
    builder.add("\uff69", "\u30a5");
    builder.add("\uff6a", "\u30a7");
    builder.add("\uff6b", "\u30a9");
    builder.add("\uff6c", "\u30e3");
    builder.add("\uff6d", "\u30e5");
    builder.add("\uff6e", "\u30e7");
    builder.add("\uff6f", "\u30c3");
    builder.add("\uff70", "\u30fc");
    builder.add("\uff71", "\u30a2");
    builder.add("\uff72", "\u30a4");
    builder.add("\uff73", "\u30a6");
    builder.add("\uff74", "\u30a8");
    builder.add("\uff75", "\u30aa");
    builder.add("\uff76", "\u30ab");
    builder.add("\uff77", "\u30ad");
    builder.add("\uff78", "\u30af");
    builder.add("\uff79", "\u30b1");
    builder.add("\uff7a", "\u30b3");
    builder.add("\uff7b", "\u30b5");
    builder.add("\uff7c", "\u30b7");
    builder.add("\uff7d", "\u30b9");
    builder.add("\uff7e", "\u30bb");
    builder.add("\uff7f", "\u30bd");
    builder.add("\uff80", "\u30bf");
    builder.add("\uff81", "\u30c1");
    builder.add("\uff82", "\u30c4");
    builder.add("\uff83", "\u30c6");
    builder.add("\uff84", "\u30c8");
    builder.add("\uff85", "\u30ca");
    builder.add("\uff86", "\u30cb");
    builder.add("\uff87", "\u30cc");
    builder.add("\uff88", "\u30cd");
    builder.add("\uff89", "\u30ce");
    builder.add("\uff8a", "\u30cf");
    builder.add("\uff8b", "\u30d2");
    builder.add("\uff8c", "\u30d5");
    builder.add("\uff8d", "\u30d8");
    builder.add("\uff8e", "\u30db");
    builder.add("\uff8f", "\u30de");
    builder.add("\uff90", "\u30df");
    builder.add("\uff91", "\u30e0");
    builder.add("\uff92", "\u30e1");
    builder.add("\uff93", "\u30e2");
    builder.add("\uff94", "\u30e4");
    builder.add("\uff95", "\u30e6");
    builder.add("\uff96", "\u30e8");
    builder.add("\uff97", "\u30e9");
    builder.add("\uff98", "\u30ea");
    builder.add("\uff99", "\u30eb");
    builder.add("\uff9a", "\u30ec");
    builder.add("\uff9b", "\u30ed");
    builder.add("\uff9c", "\u30ef");
    builder.add("\uff9d", "\u30f3");
    builder.add("\uff9e", "\u3099");
    builder.add("\uff9f", "\u309a");

    map = builder.build();

    final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("/x/lucene/data/jawiki/jawiki-20120220-pages-articles.xml"), "UTF-8"), 1<<16);

    final long t0 = System.currentTimeMillis();
    long totBytes = 0;
    long nextPrint = 10*1024*1024;

    final char[] buffer = new char[4096];
    final CharFilter filt = new MappingCharFilter(map, reader);
    while(true) {
      final int count = filt.read(buffer, 0, buffer.length);
      if (count == -1) {
        break;
      }
      totBytes += count;
      if (totBytes >= nextPrint) {
        System.out.println(String.format("%.1f MB %d msec", totBytes/1024./1024., System.currentTimeMillis()-t0));
        nextPrint += 10*1024*1024;
      }
    }
    System.out.println(String.format("%.1f MB %d msec", totBytes/1024./1024., System.currentTimeMillis()-t0));

    /*
    final Analyzer a = new Analyzer() {
        @Override
        public TokenStreamComponents createComponents(String field, Reader reader) {
          Tokenizer tokenizer = new JapaneseTokenizer(reader, null, true, JapaneseTokenizer.DEFAULT_MODE);
          TokenStream stream = new JapaneseBaseFormFilter(tokenizer);
          stream = new JapanesePartOfSpeechStopFilter(true, stream, JapaneseAnalyzer.getDefaultStopTags());
          stream = new CJKWidthFilter(stream);
          stream = new StopFilter(Version.LUCENE_36, stream, JapaneseAnalyzer.getDefaultStopSet());
          stream = new JapaneseKatakanaStemFilter(stream);
          stream = new LowerCaseFilter(Version.LUCENE_36, stream);
          return new TokenStreamComponents(tokenizer, stream);
        }

        @Override
        protected Reader initReader(Reader reader) {
          return new MappingCharFilter(map, reader);
        }
      };

    long tokenCount= 0;
    while(true) {
      final String line = reader.readLine();
      if (line == null) {
        break;
      }

      final TokenStream ts = a.tokenStream("field", new StringReader(line));
      ts.reset();
      while(ts.incrementToken()) {
        tokenCount++;
      }
      ts.end();
      ts.close();
      totBytes += line.length();
      if (totBytes >= nextPrint) {
        System.out.println(String.format("%.1f MB %d tokens %d msec", totBytes/1024./1024., tokenCount, System.currentTimeMillis()-t0));
        nextPrint += 10*1024*1024;
      }
    }
    System.out.println(String.format("%.1f MB %d tokens %d msec", totBytes/1024./1024., tokenCount, System.currentTimeMillis()-t0));
    */
    reader.close();
  }
}
