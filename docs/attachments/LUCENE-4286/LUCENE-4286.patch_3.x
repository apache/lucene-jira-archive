Index: lucene/CHANGES.txt
===================================================================
--- lucene/CHANGES.txt	(revision 1407027)
+++ lucene/CHANGES.txt	(working copy)
@@ -37,6 +37,10 @@
 * LUCENE-4411: when sampling is enabled for a FacetRequest, its depth
   parameter is reset to the default (1), even if set otherwise.
   (Gilad Barkai via Shai Erera)
+
+New Features
+
+* LUCENE-4286: CJKBigramFilter has flag to also index unigrams as well as bigrams
   
 Documentation
 
Index: lucene/contrib/analyzers/common/src/java/org/apache/lucene/analysis/cjk/CJKBigramFilter.java
===================================================================
--- lucene/contrib/analyzers/common/src/java/org/apache/lucene/analysis/cjk/CJKBigramFilter.java	(revision 1407027)
+++ lucene/contrib/analyzers/common/src/java/org/apache/lucene/analysis/cjk/CJKBigramFilter.java	(working copy)
@@ -1,6 +1,6 @@
 package org.apache.lucene.analysis.cjk;
 
-/**
+/*
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
@@ -24,6 +24,8 @@
 import org.apache.lucene.analysis.standard.StandardTokenizer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
+import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
+import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
 import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
 import org.apache.lucene.util.ArrayUtil;
 
@@ -35,6 +37,12 @@
  * {@link #CJKBigramFilter(TokenStream, int)} to explicitly control which
  * of the CJK scripts are turned into bigrams.
  * <p>
+ * By default, when a CJK character has no adjacent characters to form
+ * a bigram, it is output in unigram form. If you want to always output
+ * both unigrams and bigrams, set the <code>outputUnigrams</code>
+ * flag in {@link CJKBigramFilter#CJKBigramFilter(TokenStream, int, boolean)}.
+ * This can be used for a combined unigram+bigram approach.
+ * <p>
  * In all cases, all non-CJK input is passed thru unmodified.
  */
 public final class CJKBigramFilter extends TokenFilter {
@@ -67,10 +75,16 @@
   private final Object doHiragana;
   private final Object doKatakana;
   private final Object doHangul;
+  
+  // true if we should output unigram tokens always
+  private final boolean outputUnigrams;
+  private boolean ngramState; // false = output unigram, true = output bigram
     
   private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
   private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
   private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
+  private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
+  private final PositionLengthAttribute posLengthAtt = addAttribute(PositionLengthAttribute.class);
   
   // buffers containing codepoint and offsets in parallel
   int buffer[] = new int[8];
@@ -88,23 +102,36 @@
   
   /** 
    * Calls {@link CJKBigramFilter#CJKBigramFilter(TokenStream, int)
-   *       CJKBigramFilter(HAN | HIRAGANA | KATAKANA | HANGUL)}
+   *       CJKBigramFilter(in, HAN | HIRAGANA | KATAKANA | HANGUL)}
    */
   public CJKBigramFilter(TokenStream in) {
     this(in, HAN | HIRAGANA | KATAKANA | HANGUL);
   }
   
   /** 
-   * Create a new CJKBigramFilter, specifying which writing systems should be bigrammed.
+   * Calls {@link CJKBigramFilter#CJKBigramFilter(TokenStream, int, boolean)
+   *       CJKBigramFilter(in, flags, false)}
+   */
+  public CJKBigramFilter(TokenStream in, int flags) {
+    this(in, flags, false);
+  }
+  
+  /**
+   * Create a new CJKBigramFilter, specifying which writing systems should be bigrammed,
+   * and whether or not unigrams should also be output.
    * @param flags OR'ed set from {@link CJKBigramFilter#HAN}, {@link CJKBigramFilter#HIRAGANA}, 
    *        {@link CJKBigramFilter#KATAKANA}, {@link CJKBigramFilter#HANGUL}
+   * @param outputUnigrams true if unigrams for the selected writing systems should also be output.
+   *        when this is false, this is only done when there are no adjacent characters to form
+   *        a bigram.
    */
-  public CJKBigramFilter(TokenStream in, int flags) {
+  public CJKBigramFilter(TokenStream in, int flags, boolean outputUnigrams) {
     super(in);
     doHan =      (flags & HAN) == 0      ? NO : HAN_TYPE;
     doHiragana = (flags & HIRAGANA) == 0 ? NO : HIRAGANA_TYPE;
     doKatakana = (flags & KATAKANA) == 0 ? NO : KATAKANA_TYPE;
     doHangul =   (flags & HANGUL) == 0   ? NO : HANGUL_TYPE;
+    this.outputUnigrams = outputUnigrams;
   }
   
   /*
@@ -120,7 +147,24 @@
         // case 1: we have multiple remaining codepoints buffered,
         // so we can emit a bigram here.
         
-        flushBigram();
+        if (outputUnigrams) {
+
+          // when also outputting unigrams, we output the unigram first,
+          // then rewind back to revisit the bigram.
+          // so an input of ABC is A + (rewind)AB + B + (rewind)BC + C
+          // the logic in hasBufferedUnigram ensures we output the C, 
+          // even though it did actually have adjacent CJK characters.
+
+          if (ngramState) {
+            flushBigram();
+          } else {
+            flushUnigram();
+            index--;
+          }
+          ngramState = !ngramState;
+        } else {
+          flushBigram();
+        }
         return true;
       } else if (doNext()) {
         
@@ -205,7 +249,7 @@
   /**
    * refills buffers with new data from the current token.
    */
-  private void refill() throws IOException {
+  private void refill() {
     // compact buffers to keep them smallish if they become large
     // just a safety check, but technically we only need the last codepoint
     if (bufferLen > 64) {
@@ -260,6 +304,11 @@
     termAtt.setLength(len2);
     offsetAtt.setOffset(startOffset[index], endOffset[index+1]);
     typeAtt.setType(DOUBLE_TYPE);
+    // when outputting unigrams, all bigrams are synonyms that span two unigrams
+    if (outputUnigrams) {
+      posIncAtt.setPositionIncrement(0);
+      posLengthAtt.setPositionLength(2);
+    }
     index++;
   }
   
@@ -292,7 +341,13 @@
    * inputs.
    */
   private boolean hasBufferedUnigram() {
-    return bufferLen == 1 && index == 0;
+    if (outputUnigrams) {
+      // when outputting unigrams always
+      return bufferLen - index == 1;
+    } else {
+      // otherwise its only when we have a lone CJK character
+      return bufferLen == 1 && index == 0;
+    }
   }
 
   @Override
@@ -303,5 +358,6 @@
     lastEndOffset = 0;
     loneState = null;
     exhausted = false;
+    ngramState = false;
   }
 }
Index: lucene/contrib/analyzers/common/src/test/org/apache/lucene/analysis/cjk/TestCJKBigramFilter.java
===================================================================
--- lucene/contrib/analyzers/common/src/test/org/apache/lucene/analysis/cjk/TestCJKBigramFilter.java	(revision 0)
+++ lucene/contrib/analyzers/common/src/test/org/apache/lucene/analysis/cjk/TestCJKBigramFilter.java	(revision 0)
@@ -0,0 +1,165 @@
+package org.apache.lucene.analysis.cjk;
+
+/*
+ * Licensed to the Apache Software Foundation (ASF) under one or more
+ * contributor license agreements.  See the NOTICE file distributed with
+ * this work for additional information regarding copyright ownership.
+ * The ASF licenses this file to You under the Apache License, Version 2.0
+ * (the "License"); you may not use this file except in compliance with
+ * the License.  You may obtain a copy of the License at
+ *
+ *     http://www.apache.org/licenses/LICENSE-2.0
+ *
+ * Unless required by applicable law or agreed to in writing, software
+ * distributed under the License is distributed on an "AS IS" BASIS,
+ * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
+ * See the License for the specific language governing permissions and
+ * limitations under the License.
+ */
+
+import java.io.Reader;
+import java.util.Random;
+
+import org.apache.lucene.analysis.Analyzer;
+import org.apache.lucene.analysis.BaseTokenStreamTestCase;
+import org.apache.lucene.analysis.Tokenizer;
+import org.apache.lucene.analysis.standard.StandardTokenizer;
+import org.apache.lucene.analysis.TokenStream;
+
+public class TestCJKBigramFilter extends BaseTokenStreamTestCase {
+  // need example of setting up analyzer with a tokenizer and a filter i.e.
+  // a chain in 3.6 api instead of the 4.0 components
+  
+  Analyzer analyzer = new Analyzer() {
+    
+    public TokenStream tokenStream(String fieldName, Reader reader) {
+      return new CJKBigramFilter(new StandardTokenizer(TEST_VERSION_CURRENT,
+          reader));
+    }
+  };
+  
+  Analyzer unibiAnalyzer = new Analyzer() {
+    
+    public TokenStream tokenStream(String fieldName, Reader reader) {
+      return new CJKBigramFilter(new StandardTokenizer(TEST_VERSION_CURRENT,
+          reader), 0xff, true);
+    }
+  };
+  
+  public void testHuge() throws Exception {
+    assertAnalyzesTo(analyzer, "多くの学生が試験に落ちた" + "多くの学生が試験に落ちた" + "多くの学生が試験に落ちた"
+        + "多くの学生が試験に落ちた" + "多くの学生が試験に落ちた" + "多くの学生が試験に落ちた" + "多くの学生が試験に落ちた"
+        + "多くの学生が試験に落ちた" + "多くの学生が試験に落ちた" + "多くの学生が試験に落ちた" + "多くの学生が試験に落ちた",
+        new String[] {"多く", "くの", "の学", "学生", "生が", "が試", "試験", "験に", "に落",
+            "落ち", "ちた", "た多", "多く", "くの", "の学", "学生", "生が", "が試", "試験", "験に",
+            "に落", "落ち", "ちた", "た多", "多く", "くの", "の学", "学生", "生が", "が試", "試験",
+            "験に", "に落", "落ち", "ちた", "た多", "多く", "くの", "の学", "学生", "生が", "が試",
+            "試験", "験に", "に落", "落ち", "ちた", "た多", "多く", "くの", "の学", "学生", "生が",
+            "が試", "試験", "験に", "に落", "落ち", "ちた", "た多", "多く", "くの", "の学", "学生",
+            "生が", "が試", "試験", "験に", "に落", "落ち", "ちた", "た多", "多く", "くの", "の学",
+            "学生", "生が", "が試", "試験", "験に", "に落", "落ち", "ちた", "た多", "多く", "くの",
+            "の学", "学生", "生が", "が試", "試験", "験に", "に落", "落ち", "ちた", "た多", "多く",
+            "くの", "の学", "学生", "生が", "が試", "試験", "験に", "に落", "落ち", "ちた", "た多",
+            "多く", "くの", "の学", "学生", "生が", "が試", "試験", "験に", "に落", "落ち", "ちた",
+            "た多", "多く", "くの", "の学", "学生", "生が", "が試", "試験", "験に", "に落", "落ち",
+            "ちた"});
+  }
+  
+  public void testHanOnly() throws Exception {
+    
+    Analyzer a = new Analyzer() {
+      
+      public TokenStream tokenStream(String fieldName, Reader reader) {
+        return new CJKBigramFilter(new StandardTokenizer(TEST_VERSION_CURRENT,
+            reader), CJKBigramFilter.HAN);
+      }
+    };
+    
+    assertAnalyzesTo(a, "多くの学生が試験に落ちた。", new String[] {"多", "く", "の", "学生",
+        "が", "試験", "に", "落", "ち", "た"}, new int[] {0, 1, 2, 3, 5, 6, 8, 9, 10,
+        11}, new int[] {1, 2, 3, 5, 6, 8, 9, 10, 11, 12}, new String[] {
+        "<SINGLE>", "<HIRAGANA>", "<HIRAGANA>", "<DOUBLE>", "<HIRAGANA>",
+        "<DOUBLE>", "<HIRAGANA>", "<SINGLE>", "<HIRAGANA>", "<HIRAGANA>",
+        "<SINGLE>"}, new int[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, new int[] {1, 1,
+        1, 1, 1, 1, 1, 1, 1, 1});
+  }
+  
+  public void testAllScripts() throws Exception {
+    
+    Analyzer a = new Analyzer() {
+      
+      public TokenStream tokenStream(String fieldName, Reader reader) {
+        return new CJKBigramFilter(new StandardTokenizer(TEST_VERSION_CURRENT,
+            reader), 0xff, false);
+      }
+    };
+    
+    assertAnalyzesTo(a, "多くの学生が試験に落ちた。", new String[] {"多く", "くの", "の学", "学生",
+        "生が", "が試", "試験", "験に", "に落", "落ち", "ちた"});
+  }
+  
+  public void testUnigramsAndBigramsAllScripts() throws Exception {
+    assertAnalyzesTo(unibiAnalyzer, "多くの学生が試験に落ちた。", new String[] {"多", "多く",
+        "く", "くの", "の", "の学", "学", "学生", "生", "生が", "が", "が試", "試", "試験", "験",
+        "験に", "に", "に落", "落", "落ち", "ち", "ちた", "た"}, new int[] {0, 0, 1, 1, 2,
+        2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11},
+        new int[] {1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10,
+            11, 11, 12, 12}, new String[] {"<SINGLE>", "<DOUBLE>", "<SINGLE>",
+            "<DOUBLE>", "<SINGLE>", "<DOUBLE>", "<SINGLE>", "<DOUBLE>",
+            "<SINGLE>", "<DOUBLE>", "<SINGLE>", "<DOUBLE>", "<SINGLE>",
+            "<DOUBLE>", "<SINGLE>", "<DOUBLE>", "<SINGLE>", "<DOUBLE>",
+            "<SINGLE>", "<DOUBLE>", "<SINGLE>", "<DOUBLE>", "<SINGLE>"},
+        new int[] {1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0,
+            1, 0, 1}, new int[] {1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1,
+            2, 1, 2, 1, 2, 1, 2, 1});
+  }
+  
+  public void testUnigramsAndBigramsHanOnly() throws Exception {
+    
+    Analyzer a = new Analyzer() {
+      
+      public TokenStream tokenStream(String fieldName, Reader reader) {
+        return new CJKBigramFilter(new StandardTokenizer(TEST_VERSION_CURRENT,
+            reader), CJKBigramFilter.HAN, true);
+      }
+    };
+    
+    assertAnalyzesTo(a, "多くの学生が試験に落ちた。", new String[] {"多", "く", "の", "学",
+        "学生", "生", "が", "試", "試験", "験", "に", "落", "ち", "た"}, new int[] {0, 1,
+        2, 3, 3, 4, 5, 6, 6, 7, 8, 9, 10, 11}, new int[] {1, 2, 3, 4, 5, 5, 6,
+        7, 8, 8, 9, 10, 11, 12}, new String[] {"<SINGLE>", "<HIRAGANA>",
+        "<HIRAGANA>", "<SINGLE>", "<DOUBLE>", "<SINGLE>", "<HIRAGANA>",
+        "<SINGLE>", "<DOUBLE>", "<SINGLE>", "<HIRAGANA>", "<SINGLE>",
+        "<HIRAGANA>", "<HIRAGANA>", "<SINGLE>"}, new int[] {1, 1, 1, 1, 0, 1,
+        1, 1, 0, 1, 1, 1, 1, 1}, new int[] {1, 1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 1,
+        1, 1});
+  }
+  
+  public void testUnigramsAndBigramsHuge() throws Exception {
+    assertAnalyzesTo(unibiAnalyzer, "多くの学生が試験に落ちた" + "多くの学生が試験に落ちた"
+        + "多くの学生が試験に落ちた" + "多くの学生が試験に落ちた" + "多くの学生が試験に落ちた" + "多くの学生が試験に落ちた"
+        + "多くの学生が試験に落ちた" + "多くの学生が試験に落ちた" + "多くの学生が試験に落ちた" + "多くの学生が試験に落ちた"
+        + "多くの学生が試験に落ちた", new String[] {"多", "多く", "く", "くの", "の", "の学", "学",
+        "学生", "生", "生が", "が", "が試", "試", "試験", "験", "験に", "に", "に落", "落", "落ち",
+        "ち", "ちた", "た", "た多", "多", "多く", "く", "くの", "の", "の学", "学", "学生", "生",
+        "生が", "が", "が試", "試", "試験", "験", "験に", "に", "に落", "落", "落ち", "ち", "ちた",
+        "た", "た多", "多", "多く", "く", "くの", "の", "の学", "学", "学生", "生", "生が", "が",
+        "が試", "試", "試験", "験", "験に", "に", "に落", "落", "落ち", "ち", "ちた", "た", "た多",
+        "多", "多く", "く", "くの", "の", "の学", "学", "学生", "生", "生が", "が", "が試", "試",
+        "試験", "験", "験に", "に", "に落", "落", "落ち", "ち", "ちた", "た", "た多", "多", "多く",
+        "く", "くの", "の", "の学", "学", "学生", "生", "生が", "が", "が試", "試", "試験", "験",
+        "験に", "に", "に落", "落", "落ち", "ち", "ちた", "た", "た多", "多", "多く", "く", "くの",
+        "の", "の学", "学", "学生", "生", "生が", "が", "が試", "試", "試験", "験", "験に", "に",
+        "に落", "落", "落ち", "ち", "ちた", "た", "た多", "多", "多く", "く", "くの", "の", "の学",
+        "学", "学生", "生", "生が", "が", "が試", "試", "試験", "験", "験に", "に", "に落", "落",
+        "落ち", "ち", "ちた", "た", "た多", "多", "多く", "く", "くの", "の", "の学", "学", "学生",
+        "生", "生が", "が", "が試", "試", "試験", "験", "験に", "に", "に落", "落", "落ち", "ち",
+        "ちた", "た", "た多", "多", "多く", "く", "くの", "の", "の学", "学", "学生", "生", "生が",
+        "が", "が試", "試", "試験", "験", "験に", "に", "に落", "落", "落ち", "ち", "ちた", "た",
+        "た多", "多", "多く", "く", "くの", "の", "の学", "学", "学生", "生", "生が", "が", "が試",
+        "試", "試験", "験", "験に", "に", "に落", "落", "落ち", "ち", "ちた", "た", "た多", "多",
+        "多く", "く", "くの", "の", "の学", "学", "学生", "生", "生が", "が", "が試", "試", "試験",
+        "験", "験に", "に", "に落", "落", "落ち", "ち", "ちた", "た"});
+  }
+  
+}
Index: solr/CHANGES.txt
===================================================================
--- solr/CHANGES.txt	(revision 1407027)
+++ solr/CHANGES.txt	(working copy)
@@ -30,6 +30,11 @@
 * SOLR-3589: Edismax parser does not honor mm parameter if analyzer splits a token.
   (Tom Burton-West, Robert Muir)
 
+
+New Features
+
+* LUCENE-4286: CJKBigramFilterFactory added to allow using Lucene CJKBigramFilter 
+
 ==================  3.6.1  ==================
 More information about this release, including any errata related to the 
 release notes, upgrade instructions, or other changes may be found online at:
Index: solr/core/src/test/org/apache/solr/analysis/TestCJKBigramFilterFactory.java
===================================================================
--- solr/core/src/test/org/apache/solr/analysis/TestCJKBigramFilterFactory.java	(revision 1407027)
+++ solr/core/src/test/org/apache/solr/analysis/TestCJKBigramFilterFactory.java	(working copy)
@@ -49,4 +49,16 @@
     assertTokenStreamContents(stream,
         new String[] { "多", "く", "の",  "学生", "が",  "試験", "に",  "落", "ち", "た" });
   }
+  
+  public void testHanOnlyUnigrams() throws Exception {
+    Reader reader = new StringReader("多くの学生が試験に落ちた。");
+    CJKBigramFilterFactory factory = new CJKBigramFilterFactory();
+    Map<String,String> args = new HashMap<String,String>();
+    args.put("hiragana", "false");
+    args.put("outputUnigrams", "true");
+    factory.init(args);
+    TokenStream stream = factory.create(new StandardTokenizer(TEST_VERSION_CURRENT, reader));
+    assertTokenStreamContents(stream,
+        new String[] { "多", "く", "の",  "学", "学生", "生", "が",  "試", "試験", "験", "に",  "落", "ち", "た" });
+  }
 }
Index: solr/core/src/java/org/apache/solr/analysis/CJKBigramFilterFactory.java
===================================================================
--- solr/core/src/java/org/apache/solr/analysis/CJKBigramFilterFactory.java	(revision 1407027)
+++ solr/core/src/java/org/apache/solr/analysis/CJKBigramFilterFactory.java	(working copy)
@@ -32,12 +32,13 @@
  *     &lt;filter class="solr.LowerCaseFilterFactory"/&gt;
  *     &lt;filter class="solr.CJKBigramFilterFactory" 
  *       han="true" hiragana="true" 
- *       katakana="true" hangul="true" /&gt;
+ *       katakana="true" hangul="true" outputUnigrams="false" /&gt;
  *   &lt;/analyzer&gt;
  * &lt;/fieldType&gt;</pre>
  */
 public class CJKBigramFilterFactory extends BaseTokenFilterFactory {
   int flags;
+  boolean outputUnigrams;
 
   public void init(Map<String,String> args) {
     super.init(args);
@@ -54,9 +55,10 @@
     if (getBoolean("hangul", true)) {
       flags |= CJKBigramFilter.HANGUL;
     }
+    outputUnigrams = getBoolean("outputUnigrams", false);
   }
   
   public TokenStream create(TokenStream input) {
-    return new CJKBigramFilter(input, flags);
+    return new CJKBigramFilter(input, flags, outputUnigrams);
   }
 }
