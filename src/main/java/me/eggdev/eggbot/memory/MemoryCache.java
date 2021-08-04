package me.eggdev.eggbot.memory;

import me.eggdev.eggbot.EggBotKt;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Represents a memory cache, which can have its own lifetime,
 * write regularly to the database, and read regularly from the
 * database.
 * <p>
 * It is of paramount importance to ensure that the memory cache
 * be used for caches which are large in size and requires
 * synchronization due to the fact that multiple schedules are
 * made.
 *
 * @param <T> The type
 */
public class MemoryCache<T> {

    // The instance of the cache
    private T object;

    // Whether the cache is alive or not
    private boolean alive = true;

    // The scheduled future for read and write operations
    private ScheduledFuture<?> write, read;

    // The connector to the storage, where memory is dumped
    // into storage
    private final StorageConnector<T, ? super Object> connector;

    private final int lifespanMillis, writeMillis, readMillis;

    /**
     * Initializes a memory cache from a given set of specified properties
     * including its lifespan in millis, the refresh time for writing,
     * and the refresh time for reading.
     * <p>
     * It is important to know that setting the refresh time for writing
     * and reading can cause some conflicts.
     *
     * @param initialObj     The initial object
     * @param lifespanMillis The lifespan of this object in milliseconds
     * @param writeMillis    The rate at which the object gets written into
     *                       the database
     * @param readMillis     The rate at which the object is read (updated)
     * @param connector      The storage connector for the object
     */
    public MemoryCache(T initialObj, int lifespanMillis, int writeMillis,
                       int readMillis, StorageConnector<T, ? super Object> connector) {
        object = initialObj;
        this.connector = connector;
        this.lifespanMillis = lifespanMillis;
        this.writeMillis = writeMillis;
        this.readMillis = readMillis;

        if (lifespanMillis > 0) {
            // create lifespan
            EggBotKt.getExecutorService()
                    .schedule(() -> {
                        alive = false;
                        object = null;

                        if (write != null)
                            write.cancel(false);
                    }, lifespanMillis, TimeUnit.MILLISECONDS);
        }

        if (writeMillis > 0)
            writeCache();
        if (readMillis > 0)
            readCache();
    }

    /**
     * Sets a simple cache with no write/read refresh.
     *
     * @param initialObj The initial value of the object
     * @param connector  The connector to the storage
     */
    public MemoryCache(T initialObj, StorageConnector<T, ? super Object> connector) {
        this(initialObj, -1, -1, -1, connector);
    }

    /**
     * Retrieves this object from the cache
     *
     * @param forceWrite Whether the object should be written just before the
     *                   retrieval.
     * @param forceRead  Whether the object should be read and updated. The retrieved
     *                   object will be the updated instance.
     * @return The object retrieved from the cache.
     */
    public T get(boolean forceWrite, boolean forceRead) {
        if (!alive)
            return null;
        if (forceWrite)
            connector.write(object);
        if (forceRead)
            object = connector.read();
        return object;
    }

    /**
     * @return The object retrieved from the cache
     */
    public T get() {
        return get(false, false);
    }

    // Writes the cache
    private void writeCache() {
        if (!alive)
            return;
        if (write != null)
            write.cancel(false);
        // create update
        write = EggBotKt.getExecutorService()
                .scheduleAtFixedRate(() -> {
                    // write to target database
                    connector.write(object);
                }, writeMillis, writeMillis, TimeUnit.MILLISECONDS);
    }

    // Reads the cache
    private void readCache() {
        if (!alive)
            return;
        if (read != null)
            read.cancel(false);

        read = EggBotKt.getExecutorService()
                .scheduleAtFixedRate(() -> {
                    // read from target database
                    object = connector.read();
                    readCache();
                }, readMillis, readMillis, TimeUnit.MILLISECONDS);
    }

}
