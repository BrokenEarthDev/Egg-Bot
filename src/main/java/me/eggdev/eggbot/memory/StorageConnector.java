package me.eggdev.eggbot.memory;

/**
 * A storage connector "connects" objects in memory to storage
 * by converting it to a primitive type. Afterwards, the
 * primitive type will be moved to the database where it is
 * stored in a write operation. During a read operation,
 * the primitive gets extracted and gets converted into the
 * type of the object.
 *
 * @param <I> The type of the object to be used in memory
 * @param <O> The type of the object - must be a primitive -
 *            to be used in databases
 */
public abstract class StorageConnector<I, O> {

    // The database name
    private final String databaseName;

    // The database path
    private final String path;

    public StorageConnector(String databaseName, String path) {
        this.databaseName = databaseName;
        this.path = path;
    }

    public void write(I in) {
        O out = toOut(in).value;

        // todo implement database algorithm

    }

    public I read() {
        return null;
    }

    /**
     * Converts the memory object to an object
     * of a primitive type, which is used in databases
     *
     * @param in The object used in memory
     * @return The primitive object that corresponds to the
     *          one used in memory
     *
     * @see #fromOut(Primitive)
     */
    protected abstract Primitive<O> toOut(I in);

    /**
     * Converts the storage data type to the one used
     * in memory.
     *
     * @param out The storage data (primitive)
     * @return The memory object that corresponds to the
     * one used in memory
     */
    protected abstract I fromOut(Primitive<O> out);
}
