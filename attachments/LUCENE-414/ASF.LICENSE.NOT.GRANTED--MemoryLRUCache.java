package org.apache.lucene.util;

import java.util.*;
import java.util.Map.Entry;

/**
 * memory cache. maintains blockno to data block mapping, with a maximum size for the entire
 * cache.
 */
public class MemoryLRUCache {
    private LinkedHashMap cache;
    
    private long maxsize;
    private long cachesize;
    
    public MemoryLRUCache(long size) {
        maxsize = size;
        cache = new LinkedHashMap(10000, .75F, true) {
            public boolean removeEldestEntry(Map.Entry eldest) {
                if(cachesize>maxsize) {
                    cachesize -= ((byte[])eldest.getValue()).length;
                    return true;
                }
                return false;
            }
        };
    }

    public synchronized byte[] get(Object key) {
        byte[] data = (byte[]) cache.get(key);
        return data;
    }
        
    public synchronized Object remove(Object key) {
        byte[] data = (byte[]) cache.remove(key);
        if(data!=null)
            cachesize-=data.length;
        return data;
    }
        
    public synchronized void put(Object key,byte[] data) {
        cachesize+=data.length;
        byte[] olddata = (byte[]) cache.put(key,data);
        if(olddata!=null)
            cachesize-=olddata.length;
        if(cachesize>maxsize) {
            for(Iterator i = cache.entrySet().iterator();i.hasNext() && cachesize>maxsize;){
                Map.Entry e = (Entry) i.next();
                if(e!=null) {
                    i.remove();
                    cachesize -= ((byte[])e.getValue()).length;
                }
            }
        }
    }
        
    public synchronized void clear() {
        cache.clear();
        cachesize=0;
    }
    
    public synchronized int size() {
        return cache.size();
    }
        
    public long maxmem() {
        return maxsize;
    }

    public long memused() {
        return cachesize;
    }

    public synchronized boolean containsKey(Object key) {
        return cache.containsKey(key);
    }
}