package us.harward.commons.cache;

public class TTLCache<K, V> implements Cache<K, V> {

    private final Cache<K, TTLCache.TTLEntry<V>> cache;
    private final long                           defaultTTLMillis;

    public TTLCache(final long defaultTTLMillis, final Cache<K, TTLCache.TTLEntry<V>> cache) {
        this.cache = cache;
        this.defaultTTLMillis = defaultTTLMillis;
    }

    @Override
    public void put(final K key, final V value) {
        final TTLEntry<V> e = new TTLEntry<V>(value, System.currentTimeMillis() + defaultTTLMillis);
        cache.put(key, e);
    }

    public void put(final K key, final V value, final long ttlMillis) {
        final TTLEntry<V> e = new TTLEntry<V>(value, System.currentTimeMillis() + ttlMillis);
        cache.put(key, e);
    }

    @Override
    public V get(final K key) {
        final TTLEntry<V> e = cache.get(key);
        return e != null && e.isValid() ? e.getValue() : null;
    }

    @Override
    public V remove(K key) {
        final TTLEntry<V> e = cache.remove(key);
        return e != null && e.isValid() ? e.getValue() : null;
    }

    public static class TTLEntry<V> {

        private final V    value;
        private final long expireMillis;

        public TTLEntry(final V value, final long expireMillis) {
            this.value = value;
            this.expireMillis = expireMillis;
        }

        public V getValue() {
            return value;
        }

        public long getExpireMillis() {
            return expireMillis;
        }

        public boolean isValid() {
            return expireMillis >= System.currentTimeMillis();
        }
    }

}
