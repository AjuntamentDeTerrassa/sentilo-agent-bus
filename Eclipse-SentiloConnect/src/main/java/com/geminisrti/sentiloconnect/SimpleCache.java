/**
 * Class Sensor
 * This object is intended to be part of the sentilo Rest Structure.
 * 
 * @author: Miguel Angel Pescador <miguelangelps@prometeoinnova.com>
 * @package com.geminisrti.sentiloconnect
 * @copyright 20/04/17 Geminis RTI.
 */
package com.geminisrti.sentiloconnect;
 
import java.util.ArrayList;
import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.map.LRUMap;
 
/**
 * Class SimpleCache.
 * 
 * @author mapescador
 * @param <K> Insex type
 * @param <T> Value type
 */ 
public class SimpleCache<K, T> {
 
    private long timeToLive;
    private LRUMap simpleCacheMap;
    /**
     * Class Simple CacheObject
     */
    protected class SimpleCacheObject {
        public long lastAccessed = System.currentTimeMillis();
        public T value;
        /**
         * Constructor of SimpleCacheObject
         * @param value Value to store
         */
        protected SimpleCacheObject(T value) {
            this.value = value;
        }
    }
    /**
     * Simple Cache Constructor
     * @param simpleTimeToLive      Time to live in seconds
     * @param simpleTimerInterval   Refresh interval in secods
     * @param maxItems              Max items in cache
     */
    public SimpleCache(long simpleTimeToLive, final long simpleTimerInterval, int maxItems) {
        this.timeToLive = simpleTimeToLive * 1000;
 
        simpleCacheMap = new LRUMap(maxItems);
 
        if (timeToLive > 0 && simpleTimerInterval > 0) {
 
            Thread t = new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(simpleTimerInterval * 1000);
                        } catch (InterruptedException ex) {
                        }
                        cleanup();
                    }
                }
            });
 
            t.setDaemon(true);
            t.start();
        }
    }
    /**
     * Return the iterator for this cache.
     * 
     * @return MapIterator
     * @see MapIterator
     */
    public MapIterator iterator(){
        return simpleCacheMap.mapIterator();
    }
    /**
     * Insert a pair Key-Value in Cache.
     * 
     * @param key Key of the element to put
     * @param value Value of the element to put
     */
    public void put(K key, T value) {
        synchronized (simpleCacheMap) {
            simpleCacheMap.put(key, new SimpleCacheObject(value));
        }
    }
    /**
     * Get a value from key.
     * 
     * @param key Key of the element to get
     * @return T value
     */
    @SuppressWarnings("unchecked")
    public T get(K key) {
        synchronized (simpleCacheMap) {
            SimpleCacheObject c = (SimpleCacheObject) simpleCacheMap.get(key);
 
            if (c == null)
                return null;
            else {
                //Destructive get.
                simpleCacheMap.remove(key);
                //c.lastAccessed = System.currentTimeMillis();
                return c.value;
            }
        }
    }
    /**
     * Remove element by key
     * @param key Key of the element to remove
     */
    public void remove(K key) {
        synchronized (simpleCacheMap) {
            simpleCacheMap.remove(key);
        }
    }
    /**
     * Cache size
     * @return Int size of cache.
     */
    public int size() {
        synchronized (simpleCacheMap) {
            return simpleCacheMap.size();
        }
    }
    /**
     * Cleanup of the cache
     */
    @SuppressWarnings("unchecked")
    public void cleanup() {
 
        long now = System.currentTimeMillis();
        ArrayList<K> deleteKey = null;
 
        synchronized (simpleCacheMap) {
            MapIterator itr = simpleCacheMap.mapIterator();
 
            deleteKey = new ArrayList<K>((simpleCacheMap.size() / 2) + 1);
            K key = null;
            SimpleCacheObject c = null;
 
            while (itr.hasNext()) {
                key = (K) itr.next();
                c = (SimpleCacheObject) itr.getValue();
 
                if (c != null && (now > (timeToLive + c.lastAccessed))) {
                    deleteKey.add(key);
                }
            }
        }
 
        for (K key : deleteKey) {
            synchronized (simpleCacheMap) {
                simpleCacheMap.remove(key);
            }
 
            Thread.yield();
        }
    }
}