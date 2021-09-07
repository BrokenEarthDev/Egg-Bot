package me.eggdev.eggbot.memory

import me.eggdev.eggbot.executorService
import org.fusionyaml.library.`object`.YamlElement
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Represents a memory cache, which can have its own lifetime,
 * write regularly to the database, and read regularly from the
 * database.
 *
 *
 * It is of paramount importance to ensure that the memory cache
 * be used for caches which are large in size and requires
 * synchronization due to the fact that multiple schedules are
 * made.
 *
 * @param <T> The type
</T> */
open class MemoryCache<T>(
    initialObj: T, lifespanMillis: Int, writeMillis: Int,
    readMillis: Int, connector: StorageConnector<T>
) {
    // The instance of the cache
    private var `object`: T?

    // Whether the cache is alive or not
    private var alive = true

    // The scheduled future for read and write operations
    private var write: ScheduledFuture<*>? = null
    private var read: ScheduledFuture<*>? = null

    // The connector to the storage, where memory is dumped
    // into storage
    private val connector: StorageConnector<T>
    private val lifespanMillis: Int
    private val writeMillis: Int
    private val readMillis: Int

    /**
     * Kills this cache.
     *
     * @param interrupt Interrupts the process
     */
    fun killCache(interrupt: Boolean) {
        alive = false
        `object` = null
        if (write != null) write!!.cancel(interrupt)
        if (read != null) read!!.cancel(interrupt)
    }

    /**
     * Sets a simple cache with no write/read refresh.
     *
     * @param initialObj The initial value of the object
     * @param connector  The connector to the storage
     */
    constructor(initialObj: T, connector: StorageConnector<T>) : this(initialObj, -1, -1, -1, connector) {}
    /**
     * Retrieves this object from the cache. Force reading the instance will cause
     * the threat to be paused. Therefore, only force read the object when
     * necessary.
     *
     * @param forceWrite Whether the object should be written just before the
     * retrieval.
     * @param forceRead  Whether the object should be read and updated. The retrieved
     * object will be the updated instance.
     * @return The object retrieved from the cache.
     */
    /**
     * @return The object retrieved from the cache
     */
    @JvmOverloads
    operator fun get(forceWrite: Boolean = false, forceRead: Boolean = false): T? {
        if (!alive) return null
        if (forceWrite) connector.write(`object`)
        if (forceRead) `object` = connector.read().get()
        return `object`
    }

    // Writes the cache
    private fun writeCache() {
        if (!alive) {
            if (write != null) {
                write?.cancel(false)
            }
            return
        }
        // create update
        write = executorService
            .scheduleAtFixedRate({
                // write to target database
                connector.write(`object`)
            }, writeMillis.toLong(), writeMillis.toLong(), TimeUnit.MILLISECONDS)
    }

    // Reads the cache
    private fun readCache() {
        if (!alive) {
            if (!read?.isCancelled!!) {
                read?.cancel(false)
            }
            return
        }
        read = executorService
            .scheduleAtFixedRate({

                // read from target database
                // Thread should wait for at least half the next
                // interval to ensure that other processes are
                // performed first.
                `object` = connector.read().get(readMillis / 2L, TimeUnit.MILLISECONDS)
            }, readMillis.toLong(), readMillis.toLong(), TimeUnit.MILLISECONDS)
    }

    /**
     * Allows creating [MemoryCache] objects easier.
     * @param <T> The type of the memory cache
    </T> */
    class Builder<T> {
        private var lifespan = 0

        // lifespan of builder
        private var write = 0

        // refresh time of write operation
        private var read // refresh time of read operation
                = 0

        fun setLifespanMillis(lifespan: Int): Builder<T> {
            this.lifespan = lifespan
            return this
        }

        fun setWriteMillis(write: Int): Builder<T> {
            this.write = write
            return this
        }

        fun setReadMillis(read: Int): Builder<T> {
            this.read = read
            return this
        }

        /**
         * Creates a memory cache based on the data passed in
         * to the builder methods
         *
         * @param initial   The initial value of T
         * @param connector The storage connector for T
         * @return A new memory cache object
         */
        fun create(initial: T, connector: StorageConnector<T>): MemoryCache<T> {
            return MemoryCache(initial, lifespan, write, read, connector)
        }
    }

    /**
     * Initializes a memory cache from a given set of specified properties
     * including its lifespan in millis, the refresh time for writing,
     * and the refresh time for reading.
     *
     *
     * It is important to know that setting the refresh time for writing
     * and reading can cause some conflicts.
     *
     * @param initialObj     The initial object
     * @param lifespanMillis The lifespan of this object in milliseconds
     * @param writeMillis    The rate at which the object gets written into
     * the database
     * @param readMillis     The rate at which the object is read (updated)
     * @param connector      The storage connector for the object
     */
    init {
        `object` = initialObj
        this.connector = connector
        this.lifespanMillis = lifespanMillis
        this.writeMillis = writeMillis
        this.readMillis = readMillis
        if (lifespanMillis > 0) {
            // create lifespan
            executorService
                .schedule({ killCache(false) }, lifespanMillis.toLong(), TimeUnit.MILLISECONDS)
        }
        if (writeMillis > 0) writeCache()
        if (readMillis > 0) readCache()
    }
}

class CachedList<T> : MemoryCache<FIFOList<T>> {

    /**
     * Initializes a memory cache from a given set of specified properties
     * including its lifespan in millis, the refresh time for writing,
     * and the refresh time for reading.
     *
     *
     * It is important to know that setting the refresh time for writing
     * and reading can cause some conflicts.
     *
     * @param capacity       The capacity of the cache
     * @param lifespanMillis The lifespan of this object in milliseconds
     * @param writeMillis    The rate at which the object gets written into
     * the database
     * @param readMillis     The rate at which the object is read (updated)
     * @param connector      The storage connector for the object
     */
    constructor(
        capacity: Int, lifespanMillis: Int, writeMillis: Int, readMillis: Int,
        connector: StorageConnector<FIFOList<T>>
    ) : super(FIFOList<T>(capacity), lifespanMillis, writeMillis, readMillis, connector)

    /**
     * Sets a simple cache with no write/read refresh.
     *
     * @param capacity   The capacity of the cache
     * @param connector  The connector to the storage
     */
    constructor(capacity: Int, connector: StorageConnector<FIFOList<T>>) : super(
        FIFOList<T>(capacity),
        connector
    )

    fun loadCache(key: String) : T? {
        // attempts to load and push the newly loaded
        // element into the list
        return null
    }

}
