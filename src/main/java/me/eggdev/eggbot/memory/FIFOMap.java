package me.eggdev.eggbot.memory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This FIFO (First In, First Out) map works by removing the first
 * element that were added after the insertion of a new one if
 * the new size exceeds the maximum capacity.
 *
 * @param <K> The key
 * @param <V> The value
 */
public class FIFOMap<K, V> extends LinkedHashMap<K, V> {

    private int capacity;

    /**
     * Constructs an empty insertion-ordered {@code LinkedHashMap} instance
     * with the specified initial capacity and a default load factor (0.75).
     *
     * @param capacity the initial capacity
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    public FIFOMap(int capacity) {
        super(capacity);
        this.capacity = capacity;
    }

    /**
     * Constructs an empty insertion-ordered {@code LinkedHashMap} instance
     * with the default initial capacity (16) and load factor (0.75).
     */
    public FIFOMap() {
        super();
    }

    /**
     * Constructs an empty {@code LinkedHashMap} instance with the
     * specified initial capacity, load factor and ordering mode.
     *
     * @param capacity the initial capacity
     * @param loadFactor      the load factor
     * @param accessOrder     the ordering mode - {@code true} for
     *                        access-order, {@code false} for insertion-order
     * @throws IllegalArgumentException if the initial capacity is negative
     *                                  or the load factor is nonpositive
     */
    public FIFOMap(int capacity, float loadFactor, boolean accessOrder) {
        super(capacity, loadFactor, accessOrder);
        this.capacity = capacity;
    }

    /**
     * Returns {@code true} if this map should remove its eldest entry.
     * This method is invoked by {@code put} and {@code putAll} after
     * inserting a new entry into the map.  It provides the implementor
     * with the opportunity to remove the eldest entry each time a new one
     * is added.  This is useful if the map represents a cache: it allows
     * the map to reduce memory consumption by deleting stale entries.
     *
     * <p>Sample use: this override will allow the map to grow up to 100
     * entries and then delete the eldest entry each time a new entry is
     * added, maintaining a steady state of 100 entries.
     * <pre>
     *     private static final int MAX_ENTRIES = 100;
     *
     *     protected boolean removeEldestEntry(Map.Entry eldest) {
     *        return size() &gt; MAX_ENTRIES;
     *     }
     * </pre>
     *
     * <p>This method typically does not modify the map in any way,
     * instead allowing the map to modify itself as directed by its
     * return value.  It <i>is</i> permitted for this method to modify
     * the map directly, but if it does so, it <i>must</i> return
     * {@code false} (indicating that the map should not attempt any
     * further modification).  The effects of returning {@code true}
     * after modifying the map from within this method are unspecified.
     *
     * <p>This implementation merely returns {@code false} (so that this
     * map acts like a normal map - the eldest element is never removed).
     *
     * @param eldest The least recently inserted entry in the map, or if
     *               this is an access-ordered map, the least recently accessed
     *               entry.  This is the entry that will be removed it this
     *               method returns {@code true}.  If the map was empty prior
     *               to the {@code put} or {@code putAll} invocation resulting
     *               in this invocation, this will be the entry that was just
     *               inserted; in other words, if the map contains a single
     *               entry, the eldest entry is also the newest.
     * @return {@code true} if the eldest entry should be removed
     * from the map; {@code false} if it should be retained.
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }

    public int getCapacity() {
        return capacity;
    }

}
