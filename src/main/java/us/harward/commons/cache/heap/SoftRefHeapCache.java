package us.harward.commons.cache.heap;

import java.lang.ref.SoftReference;

import us.harward.commons.cache.Cache;

public class SoftRefHeapCache<K, V> implements Cache<K, V> {

    private final HeapCache<K, SoftReference<V>> cache;

    public SoftRefHeapCache(final int maxSize) {
        this.cache = new HeapCache<K, SoftReference<V>>(maxSize);
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, new SoftReference<V>(value));
    }

    @Override
    public V get(K key) {
        final SoftReference<V> ref = cache.get(key);
        return ref == null ? null : ref.get();
    }

    @Override
    public V remove(K key) {
        final SoftReference<V> ref = cache.remove(key);
        return ref == null ? null : ref.get();
    }

}
