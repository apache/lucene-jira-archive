import org.apache.lucene.util.FixedThreadLocal;

public class ThreadLocalTest extends Thread {

  public static void main(String[] args) {
    ThreadLocalTest t = new ThreadLocalTest();
    t.start();
  }

  private static int counter = 0;

  public void run() {
    System.out.println("Starting");
    try {
      while (true) {
        makeNewThreadLocal();
      }
    } catch (OutOfMemoryError oome) {

      System.out.print("Managed to allocate and release: '"+counter);
      System.out.println("' ThreadLocals before running out of memory");

      throw oome;
    }
  }

  private void makeNewThreadLocal() {
    ThreadLocal t = new ThreadLocal();
//    FixedThreadLocal t = new FixedThreadLocal();
    t.set(new byte[1024*1024]);
    ++counter;
  }

}