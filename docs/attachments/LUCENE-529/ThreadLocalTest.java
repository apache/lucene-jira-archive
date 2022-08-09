import java.lang.ref.WeakReference;
import java.util.ArrayList;

import junit.framework.TestCase;


public class ThreadLocalTest extends TestCase
{
    // These settings fail for 64m
    
    public static final int SIZE = 1000000;
    
    public static final int INITIAL_THREAD_LOCAL_MAP_SIZE =  128;  // * 2/3 - 1 -3 to allow for table threshold and three found at the start
    
    public static final int REPEAT = 1000;
    
    public ThreadLocalTest()
    {
        super();
    }
    
    public void testToSetInitialThreadLocalSize()
    {
        Thread thread = Thread.currentThread();
        System.out.println("Memory: "+Runtime.getRuntime().maxMemory());
        
        ArrayList<ThreadLocal> threadLocals = new ArrayList<ThreadLocal>();
        for(int i = 0; i < INITIAL_THREAD_LOCAL_MAP_SIZE; i++)
        {
            ThreadLocal<String> local = new ThreadLocal<String>();
            local.set(" ");
            threadLocals.add(local);
        }
        
        // An clear the values out
        for(ThreadLocal<String> tl : threadLocals)
        {
            tl.set(null);
        }
        threadLocals = null;
        System.gc();
       
    }
    
    public void testICanMakeTheObjectsAndTheyAreGCed()
    {
        for(int i = 0; i < REPEAT; i++)
        {
            Integer[] integers = new Integer[SIZE];
        }
    }
    
    public void testICanMakeTheWeakRefsAndTheyAreGCed()
    {
        for(int i = 0; i < REPEAT; i++)
        {
            WeakReferenceThreadLocalHolder weak = new WeakReferenceThreadLocalHolder(SIZE);
        }
    }
    
    public void testICanMakeTheSimpleRefsAndTheyBlowUp()
    {
        for(int i = 0; i < REPEAT; i++)
        {
            SimpleThreadLocalHolder simple = new SimpleThreadLocalHolder(SIZE);
        }
        
        // Break point so we can look at the thread locals ....
        
        @SuppressWarnings("unused") 
        Thread thread = Thread.currentThread();
    }
    
    /**
     * A thread local with a value that goes stale and is not GCed
     */
    static class SimpleThreadLocalHolder
    {
        ThreadLocal<Integer[]> threadLocal = new ThreadLocal<Integer[]>();

        SimpleThreadLocalHolder(int size)
        {
            super();
            threadLocal.set(new Integer[size]);
        }

        public void finalize()
        {
            if (threadLocal.get() != null)
            {
                System.out.println("finalize() found thread local!");
            }
        }
    }

    /**
     * A thread local using weak ref that will go stale but it will be GCed when not used.
     * So the overhead is a weak reference to null.
     */
    static class WeakReferenceThreadLocalHolder
    {
        ThreadLocal<WeakReference<Integer[]>> threadLocal = new ThreadLocal<WeakReference<Integer[]>>();

        WeakReferenceThreadLocalHolder(int size)
        {
            super();
            threadLocal.set(new WeakReference(new Integer[size]));
        }

        public void finalize()
        {
            if (threadLocal.get() != null)
            {
                System.out.println("finalize() found thread local!");
            }
        }
    }


}
