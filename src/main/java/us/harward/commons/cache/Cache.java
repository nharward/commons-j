package us.harward.commons.cache;

public interface Cache<K, V> {

    void put(final K key, final V value);

    V get(final K key);

    V remove(final K key);
}
