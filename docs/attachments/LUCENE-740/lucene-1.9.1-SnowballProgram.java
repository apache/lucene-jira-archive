--- lucene-1.9.1/SnowballProgram.java	2006-12-11 18:58:43.000000000 +0100
+++ lucene-1.9.1-patched/SnowballProgram.java	2006-12-11 18:58:52.000000000 +0100
@@ -27,7 +27,15 @@
      */
     public String getCurrent()
     {
-	return current.toString();
+        String result = current.toString();
+        // Make a new StringBuffer.  If we reuse the old one, and a user of
+        // the library keeps a reference to the buffer returned (for example,
+        // by converting it to a String in a way which doesn't force a copy),
+        // the buffer size will not decrease, and we will risk wasting a large
+        // amount of memory.
+        // Thanks to Wolfram Esser for spotting this problem.
+        current = new StringBuffer();
+        return result;
     }
 
     // current string
@@ -317,7 +325,7 @@
     protected int replace_s(int c_bra, int c_ket, String s)
     {
 	int adjustment = s.length() - (c_ket - c_bra);
-	current.replace(bra, ket, s);
+	current.replace(c_bra, c_ket, s);
 	limit += adjustment;
 	if (cursor >= c_ket) cursor += adjustment;
 	else if (cursor > c_bra) cursor = c_bra;
