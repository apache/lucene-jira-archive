package org.apache.lucene.util;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * version of ThreadLocal that does not leak memory (ThreadLocal is fixed in JDK 1.5) 
 */
public class SafeThreadLocal {
    private final static Set locals = new HashSet();
    private final Map values = new WeakHashMap();
    
    public SafeThreadLocal() {
	synchronized(SafeThreadLocal.class) {
	    locals.add(new WeakReference(this));
	}
    }
    
    protected Object initialValue() {
        return null;
    }
    
    public final synchronized Object get() {
        final Object key = Thread.currentThread();
        Object value;
        if(!values.containsKey(key)) {
            value = initialValue();
            values.put(key,value);
        } else {
            value = values.get(key);
        }
        return value;
    }
    
    public final synchronized void set(Object value) {
        values.put(Thread.currentThread(),value);
    }
    
    /**
     * clear any stale entries across all thread locals
     */
    public static synchronized void purge() {
	for(Iterator i = locals.iterator();i.hasNext();) {
	    SafeThreadLocal sfl = (SafeThreadLocal) ((WeakReference)i.next()).get();
	    if(sfl==null)
		i.remove();
	    else {
		synchronized(sfl) {
		    sfl.locals.size(); // causes stale entries to be purged
		}
	    }
	}
    }
}
