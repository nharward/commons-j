package us.harward.commons.cache.heap;

import java.util.concurrent.ConcurrentHashMap;

import us.harward.commons.cache.Cache;

/**
 * Enforces maximum number of entries
 * 
 * @author alex
 * @param <K>
 * @param <V>
 */
public class HeapCache<K, V> implements Cache<K, V> {

    private final int                            maxSize;
    private final ConcurrentHashMap<K, Entry<V>> map = new ConcurrentHashMap<K, Entry<V>>();

    public HeapCache(int maxSize) {
        super();
        this.maxSize = maxSize;
    }

    @Override
    public void put(K key, V value) {
        final Entry<V> e = new Entry<V>(value);
        map.put(key, e);
        if (map.size() > maxSize) {
            purge();
        }
    }

    private void purge() {
        // TODO Auto-generated method stub
    }

    @Override
    public V get(K key) {
        final Entry<V> e = map.get(key);
        if (e == null) {
            return null;
        } else {
            e.recordHit();
            return e.getValue();
        }
    }

    @Override
    public V remove(K key) {
        Entry<V> e = map.remove(key);
        return e == null ? null : e.getValue();
    }

    private static class Entry<V> {

        private final V    value;
        private final long createMillis;
        private long       accessMillis;
        private long       hitCount;

        protected Entry(final V value) {
            this(value, System.currentTimeMillis(), 0, 0);
        }

        protected Entry(V value, long createMillis, long accessMillis, long hitCount) {
            this.value = value;
            this.createMillis = createMillis;
            this.accessMillis = accessMillis;
            this.hitCount = hitCount;
        }

        public void recordHit() {
            synchronized (this) {
                accessMillis = System.currentTimeMillis();
                hitCount++;
            }
        }

        public V getValue() {
            return value;
        }

        public long getCreateMillis() {
            return createMillis;
        }

        public long getAccessMillis() {
            return accessMillis;
        }

        public long getHitCount() {
            return hitCount;
        }

    }

}
