import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class FSyncPerfTest {

  static int NUM_FILES = 200;
  static int NUM_FILE_CHARS = 200;
  static boolean CPU_WORK = true;
  
  static File WORK_DIR = new File("D:\\tmp\\sync_test");
  
  static String TEXT = "This is the text that we repeatedly write to the files. " +
      "It doesn't matter what this text contains, as we simply repeat it," +
      "until the file contains as many characters as we want/need"; 

  public static void main(String[] args) throws IOException {
    //if (WORK_DIR.exists()) {
    //  System.err.println("work dir exists! remove it before running this test: "+WORK_DIR.getAbsolutePath());
    //  System.exit(1);
    //}
    WORK_DIR.mkdirs();
    int nfiles [] = { 10000 , 1000,   100 }; 
    int nchars [] = {   100 , 1000, 10000 }; 
    for (int k=0; k<nfiles.length; k++) {  // try few configurations
      NUM_FILES = nfiles[k];
      NUM_FILE_CHARS = nchars[k];
      System.out.println("===");
      System.out.println("NUM_FILES="+NUM_FILES+"  NUM_FILE_CHARS="+NUM_FILE_CHARS+"  CPU_WORK="+CPU_WORK);
      for (int i=0; i<2; i++) { // try twice each configuration
        System.out.println("===");
        testNoSync();
        testEndSync();
        testBackgroundSync();
        testImmediateSync();
      }
    }
  }

  private static void testEndSync() throws IOException {
    String name = "SyncAtEnd";
    FSyncThread t = new FSyncThread();
    File dir = new File(WORK_DIR,name);
    dir.mkdirs();
    long t1 = System.currentTimeMillis();
    for (int i=0; i<NUM_FILES; i++) {
      CharWriter w = writeFile(dir,i);
      t.add(w);
    }
    t.done = true;
    t.start(); // only at the end: all files kept open! (unreasonable, just for test)
    try {
      t.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    printTime(name, t1);
  }
  
  private static void testBackgroundSync() throws IOException {
    String name = "BackgroundSync";
    FSyncThread t = new FSyncThread();
    t.start();
    File dir = new File(WORK_DIR,name);
    dir.mkdirs();
    long t1 = System.currentTimeMillis();
    for (int i=0; i<NUM_FILES; i++) {
      CharWriter w = writeFile(dir,i);
      t.add(w);
    }
    t.done = true;
    try {
      t.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    printTime(name, t1);
  }

  private static void testImmediateSync() throws IOException {
    String name = "ImmediateSync";
    File dir = new File(WORK_DIR,name);
    dir.mkdirs();
    long t1 = System.currentTimeMillis();
    for (int i=0; i<NUM_FILES; i++) {
      CharWriter w = writeFile(dir,i);
      w.syncAndClose();
    }
    printTime(name, t1);
  }

  private static void testNoSync() throws IOException {
    String name = "NoSync";
    File dir = new File(WORK_DIR,name);
    dir.mkdirs();
    long t1 = System.currentTimeMillis();
    for (int i=0; i<NUM_FILES; i++) {
      CharWriter w = writeFile(dir,i);
      w.close();
    }
    printTime(name, t1);
  }

  private static void printTime(String name, long t1) {
    long t2 = System.currentTimeMillis();
    String s = "          "+(t2-t1);
    int k = s.length() - 10;
    System.out.println(s.substring(k)+"  millis for "+name);
  }


  public static long m = 0; // for immitating CPU work between chars.
  
  private static CharWriter writeFile(File dir, int fnum) throws IOException {
    File f = new File(dir,"f"+fnum+".txt");
    CharWriter w = new CharWriter(f);
    int n = 0;
    int i = fnum % TEXT.length(); // so not all files are exactly the same.
    //System.out.println("writeFile "+f.getAbsolutePath()+" starts from char "+i+" = "+TEXT.charAt(i));
    while (n<NUM_FILE_CHARS) {
      while (n<NUM_FILE_CHARS && i<TEXT.length()) {
        m=10;
        w.append(TEXT.charAt(i++));
        n++;
        if (CPU_WORK) {
          for (int k=0; k<20; k++) {
            for (int j=2; j<10; j+=2) {
              m += m/j;
              m -= m%(j+1);
            } 
          }
        }
      }
      i = 0;
    }
    return w;
  }

  static class FSyncThread extends Thread {
    boolean done = false;
    boolean hasWork = false;
    ArrayList a;
    
    public FSyncThread () {
      this.a = new ArrayList();
    }
    
    public void add(CharWriter w) {
      synchronized(a) {
        a.add(w);
        hasWork = true;
        a.notifyAll();
      }
    }
    
    public void run() {
      int n = 0;
      while (!done || hasWork) {
        if (!hasWork) {
          synchronized(a) {
            try {
              a.wait();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        } else {
          CharWriter w;
          synchronized(a) {
            w = (CharWriter) a.remove(0);
            hasWork = a.size()>0;
          }
          w.syncAndClose();
          n++;
        }
      }
      if (n<NUM_FILES) {
        throw new RuntimeException("Synced "+n+" files, < "+NUM_FILES);
      }
    }
  }
  
  static class CharWriter {
    FileOutputStream fos;
    BufferedWriter w;
    CharWriter (File f) throws IOException {
      fos = new FileOutputStream(f);
      w = new BufferedWriter(new OutputStreamWriter(fos),NUM_FILE_CHARS); //ideal...
    }
    public void syncAndClose() {
      try {
        w.flush();
        fos.getFD().sync();
        w.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    void append(char c) throws IOException {
      w.append(c);
    }
    public void close() {
      try {
        w.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
