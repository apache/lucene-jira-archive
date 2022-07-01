Index: .
===================================================================
--- .	(revision 1362078)
+++ .	(working copy)

Property changes on: .
___________________________________________________________________
Modified: svn:mergeinfo
   Merged /lucene/dev/trunk:r1353101
Index: lucene
===================================================================
--- lucene	(revision 1362078)
+++ lucene	(working copy)

Property changes on: lucene
___________________________________________________________________
Modified: svn:mergeinfo
   Merged /lucene/dev/trunk/lucene:r1353101
Index: lucene/CHANGES.txt
===================================================================
--- lucene/CHANGES.txt	(revision 1362078)
+++ lucene/CHANGES.txt	(working copy)
@@ -30,6 +30,13 @@
   public, otherwise it's impossible to implement Scorers outside
 	the Lucene package.  (Uwe Schindler)
 
+Optimizations
+
+* LUCENE-4163: Improve concurrency of MMapIndexInput.clone() by using
+  the new WeakIdentityMap on top of a ConcurrentHashMap to manage
+  the cloned instances. WeakIdentityMap was extended to support
+  iterating over its keys.  (Uwe Schindler)
+
 Tests:
 
 * LUCENE-3873: add MockGraphTokenFilter, testing analyzers with
Index: lucene/core/src
===================================================================
--- lucene/core/src	(revision 1362078)
+++ lucene/core/src	(working copy)

Property changes on: lucene/core/src
___________________________________________________________________
Modified: svn:mergeinfo
   Merged /lucene/dev/trunk/lucene/core/src:r1353101
Index: lucene/core/src/java/org/apache/lucene/store/MMapDirectory.java
===================================================================
--- lucene/core/src/java/org/apache/lucene/store/MMapDirectory.java	(revision 1362078)
+++ lucene/core/src/java/org/apache/lucene/store/MMapDirectory.java	(working copy)
@@ -27,16 +27,15 @@
 import java.nio.channels.FileChannel;
 import java.nio.channels.FileChannel.MapMode;
 
-import java.util.Set;
-import java.util.WeakHashMap;
+import java.util.Iterator;
 
 import java.security.AccessController;
 import java.security.PrivilegedExceptionAction;
 import java.security.PrivilegedActionException;
 import java.lang.reflect.Method;
 
-import org.apache.lucene.util.MapBackedSet;
 import org.apache.lucene.util.Constants;
+import org.apache.lucene.util.WeakIdentityMap;
 
 /** File-based {@link Directory} implementation that uses
  *  mmap for reading, and {@link
@@ -239,7 +238,7 @@
     private ByteBuffer curBuf; // redundant for speed: buffers[curBufIndex]
   
     private boolean isClone = false;
-    private final Set<MMapIndexInput> clones = new MapBackedSet<MMapIndexInput>(new WeakHashMap<MMapIndexInput,Boolean>());
+    private final WeakIdentityMap<MMapIndexInput,Boolean> clones = WeakIdentityMap.newConcurrentHashMap();
 
     MMapIndexInput(String resourceDescription, RandomAccessFile raf, int chunkSizePower) throws IOException {
       super(resourceDescription);
@@ -409,9 +408,7 @@
       }
       
       // register the new clone in our clone list to clean it up on closing:
-      synchronized(this.clones) {
-        this.clones.add(clone);
-      }
+      this.clones.put(clone, Boolean.TRUE);
       
       return clone;
     }
@@ -427,34 +424,24 @@
       try {
         if (isClone || buffers == null) return;
         
+        // make local copy, then un-set early
+        final ByteBuffer[] bufs = buffers;
+        unsetBuffers();
+        
         // for extra safety unset also all clones' buffers:
-        synchronized(this.clones) {
-          for (final MMapIndexInput clone : this.clones) {
-            assert clone.isClone;
-            clone.unsetBuffers();
-          }
-          this.clones.clear();
+        for (Iterator<MMapIndexInput> it = this.clones.keyIterator(); it.hasNext();) {
+          final MMapIndexInput clone = it.next();
+          assert clone.isClone;
+          clone.unsetBuffers();
         }
+        this.clones.clear();
         
-        curBuf = null; curBufIndex = 0; // nuke curr pointer early
-        for (int bufNr = 0; bufNr < buffers.length; bufNr++) {
-          cleanMapping(buffers[bufNr]);
+        for (final ByteBuffer b : bufs) {
+          cleanMapping(b);
         }
       } finally {
         unsetBuffers();
       }
     }
-
-    // make sure we have identity on equals/hashCode for WeakHashMap
-    @Override
-    public int hashCode() {
-      return System.identityHashCode(this);
-    }
-
-    // make sure we have identity on equals/hashCode for WeakHashMap
-    @Override
-    public boolean equals(Object obj) {
-      return obj == this;
-    }
   }
 }
Index: lucene/core/src/java/org/apache/lucene/util/WeakIdentityMap.java
===================================================================
--- lucene/core/src/java/org/apache/lucene/util/WeakIdentityMap.java	(revision 1362078)
+++ lucene/core/src/java/org/apache/lucene/util/WeakIdentityMap.java	(working copy)
@@ -21,7 +21,9 @@
 import java.lang.ref.ReferenceQueue;
 import java.lang.ref.WeakReference;
 import java.util.HashMap;
+import java.util.Iterator;
 import java.util.Map;
+import java.util.NoSuchElementException;
 import java.util.concurrent.ConcurrentHashMap;
 
 /**
@@ -38,9 +40,10 @@
  * 
  * <p>This implementation was forked from <a href="http://cxf.apache.org/">Apache CXF</a>
  * but modified to <b>not</b> implement the {@link java.util.Map} interface and
- * without any set/iterator views on it, as those are error-prone
- * and inefficient, if not implemented carefully. Lucene's implementation also
- * supports {@code null} keys, but those are never weak!
+ * without any set views on it, as those are error-prone and inefficient,
+ * if not implemented carefully. The map only contains {@link Iterator} implementations
+ * on the values and not-GCed keys. Lucene's implementation also supports {@code null}
+ * keys, but those are never weak!
  *
  * @lucene.internal
  */
@@ -97,6 +100,68 @@
     reap();
     return backingStore.size();
   }
+  
+  /** Returns an iterator over all weak keys of this map.
+   * Keys already garbage collected will not be returned.
+   * This Iterator does not support removals. */
+  public Iterator<K> keyIterator() {
+    reap();
+    final Iterator<IdentityWeakReference> iterator = backingStore.keySet().iterator();
+    return new Iterator<K>() {
+      // holds strong reference to next element in backing iterator:
+      private Object next = null;
+      // the backing iterator was already consumed:
+      private boolean nextIsSet = false;
+    
+      public boolean hasNext() {
+        return nextIsSet ? true : setNext();
+      }
+      
+      @SuppressWarnings("unchecked")
+      public K next() {
+        if (nextIsSet || setNext()) {
+          try {
+            assert nextIsSet;
+            return (K) next;
+          } finally {
+             // release strong reference and invalidate current value:
+            nextIsSet = false;
+            next = null;
+          }
+        }
+        throw new NoSuchElementException();
+      }
+      
+      public void remove() {
+        throw new UnsupportedOperationException();
+      }
+      
+      private boolean setNext() {
+        assert !nextIsSet;
+        while (iterator.hasNext()) {
+          next = iterator.next().get();
+          if (next == null) {
+            // already garbage collected!
+            continue;
+          }
+          // unfold "null" special value
+          if (next == NULL) {
+            next = null;
+          }
+          return nextIsSet = true;
+        }
+        return false;
+      }
+    };
+  }
+  
+  /** Returns an iterator over all values of this map.
+   * This iterator may return values whose key is already
+   * garbage collected while iterator is consumed. */
+  public Iterator<V> valueIterator() {
+    reap();
+    return backingStore.values().iterator();
+  }
 
   private void reap() {
     Reference<?> zombie;
@@ -104,6 +169,9 @@
       backingStore.remove(zombie);
     }
   }
+  
+  // we keep a hard reference to our NULL key, so map supports null keys that never get GCed:
+  static final Object NULL = new Object();
 
   private static final class IdentityWeakReference extends WeakReference<Object> {
     private final int hash;
@@ -129,9 +197,6 @@
       }
       return false;
     }
-  
-    // we keep a hard reference to our NULL key, so map supports null keys that never get GCed:
-    private static final Object NULL = new Object();
   }
 }
 
Index: lucene/core/src/test/org/apache/lucene/util/TestWeakIdentityMap.java
===================================================================
--- lucene/core/src/test/org/apache/lucene/util/TestWeakIdentityMap.java	(revision 1362078)
+++ lucene/core/src/test/org/apache/lucene/util/TestWeakIdentityMap.java	(working copy)
@@ -17,7 +17,9 @@
 
 package org.apache.lucene.util;
 
+import java.util.Iterator;
 import java.util.Map;
+import java.util.NoSuchElementException;
 import java.util.Random;
 import java.util.concurrent.atomic.AtomicReferenceArray;
 import java.util.concurrent.Executors;
@@ -42,9 +44,18 @@
     assertNotSame(key2, key3);
     assertEquals(key2, key3);
 
+    // try null key & check its iterator also return null:
+    map.put(null, "null");
+    {
+      Iterator<String> it = map.keyIterator();
+      assertTrue(it.hasNext());
+      assertNull(it.next());
+      assertFalse(it.hasNext());
+      assertFalse(it.hasNext());
+    }
+    // 2 more keys:
     map.put(key1, "bar1");
     map.put(key2, "bar2");
-    map.put(null, "null");
     
     assertEquals(3, map.size());
 
@@ -84,6 +95,25 @@
     map.put(key3, "bar3");
     assertEquals(3, map.size());
     
+    int c = 0, keysAssigned = 0;
+    for (Iterator<String> it = map.keyIterator(); it.hasNext();) {
+      assertTrue(it.hasNext()); // try again, should return same result!
+      final String k = it.next();
+      assertTrue(k == key1 || k == key2 | k == key3);
+      keysAssigned += (k == key1) ? 1 : ((k == key2) ? 2 : 4);
+      c++;
+    }
+    assertEquals(3, c);
+    assertEquals("all keys must have been seen", 1+2+4, keysAssigned);
+    
+    c = 0;
+    for (Iterator<String> it = map.valueIterator(); it.hasNext();) {
+      final String v = it.next();
+      assertTrue(v.startsWith("bar"));
+      c++;
+    }
+    assertEquals(3, c);
+    
     // clear strong refs
     key1 = key2 = key3 = null;
     
@@ -93,7 +123,13 @@
       System.runFinalization();
       System.gc();
       Thread.sleep(100L);
-      assertTrue(size >= map.size());
+      c = 0;
+      for (Iterator<String> it = map.keyIterator(); it.hasNext();) {
+        assertNotNull(it.next());
+        c++;
+      }
+      assertTrue(size >= c);
+      assertTrue(c >= map.size());
       size = map.size();
     } catch (InterruptedException ie) {}
 
@@ -101,6 +137,14 @@
     assertEquals(0, map.size());
     assertTrue(map.isEmpty());
     
+    Iterator<String> it = map.keyIterator();
+    assertFalse(it.hasNext());
+    try {
+      it.next();
+      fail("Should throw NoSuchElementException");
+    } catch (NoSuchElementException nse) {
+    }
+    
     key1 = new String("foo");
     key2 = new String("foo");
     map.put(key1, "bar1");
@@ -133,7 +177,7 @@
             final int count = atLeast(rnd, 10000);
             for (int i = 0; i < count; i++) {
               final int j = rnd.nextInt(keyCount);
-              switch (rnd.nextInt(4)) {
+              switch (rnd.nextInt(5)) {
                 case 0:
                   map.put(keys.get(j), Integer.valueOf(j));
                   break;
@@ -150,6 +194,12 @@
                   // renew key, the old one will be GCed at some time:
                   keys.set(j, new Object());
                   break;
+                case 4:
+                  // check iterator still working
+                  for (Iterator<Object> it = map.keyIterator(); it.hasNext();) {
+                    assertNotNull(it.next());
+                  }
+                  break;
                 default:
                   fail("Should not get here.");
               }
@@ -173,7 +223,13 @@
       System.runFinalization();
       System.gc();
       Thread.sleep(100L);
-      assertTrue(size >= map.size());
+      int c = 0;
+      for (Iterator<Object> it = map.keyIterator(); it.hasNext();) {
+        assertNotNull(it.next());
+        c++;
+      }
+      assertTrue(size >= c);
+      assertTrue(c >= map.size());
       size = map.size();
     } catch (InterruptedException ie) {}
   }
