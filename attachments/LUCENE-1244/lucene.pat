Index: /Users/hdiwan/Developer/lucene/src/test/org/apache/lucene/AnalysisTest.java
===================================================================
--- /Users/hdiwan/Developer/lucene/src/test/org/apache/lucene/AnalysisTest.java	(revision 640448)
+++ /Users/hdiwan/Developer/lucene/src/test/org/apache/lucene/AnalysisTest.java	(working copy)
@@ -31,11 +31,12 @@
 import java.util.Date;
 
 class AnalysisTest {
+  static File tmpFile;
   public static void main(String[] args) {
     try {
       test("This is a test", true);
-      // FIXME: OG: what's with this hard-coded file name??
-      test(new File("words.txt"), false);
+      tmpFile = File.createTempFile("words", ".txt");
+      test(tmpFile, false);
     } catch (Exception e) {
       System.out.println(" caught a " + e.getClass() +
 			 "\n with message: " + e.getMessage());
@@ -40,6 +41,7 @@
       System.out.println(" caught a " + e.getClass() +
 			 "\n with message: " + e.getMessage());
     }
+    tmpFile.deleteOnExit();
   }
 
   static void test(File file, boolean verbose)
