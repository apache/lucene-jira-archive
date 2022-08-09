package org.apache.lucene.util;

import java.util.*;

/**
 * version of ThreadLocal that does not leak memory (ThreadLocal is fixed in JDK 1.5) 
 */
public class FixedThreadLocal {
    private final Map values = new WeakHashMap();
    
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
}
