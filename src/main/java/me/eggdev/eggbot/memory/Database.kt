package me.eggdev.eggbot.memory

import org.fusionyaml.library.FusionYAML
import org.fusionyaml.library.`object`.YamlElement
import org.fusionyaml.library.configurations.FileConfiguration
import org.fusionyaml.library.serialization.TypeAdapter
import java.io.File
import java.lang.reflect.Type
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * This is the yaml object, which contains all settings
 * to help customize the YAML
 */
val YAML = FusionYAML.Builder()
    .timezone(TimeZone.getTimeZone("GMT"))
    .enumNameMentioned(false)
    .build()


data class Arguments(val args: Array<Any>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Arguments
        if (!args.contentEquals(other.args)) return false
        return true
    }

    override fun hashCode(): Int {
        return args.contentHashCode()
    }
}

/**
 * A storage connector "connects" objects in memory to storage
 * by converting it to a primitive type. Afterwards, the
 * primitive type will be moved to the database where it is
 * stored in a write operation. During a read operation,
 * the primitive gets extracted and gets converted into the
 * type of the object.
 * <p>
 * Please do note that read/write operations are expensive
 *
 * @param <I> The type of the object to be used in memory
 * @param <O> The type of the object
</O></I> */
abstract class StorageConnector<I>(
    // The YAML file
    val file: File,
    // The parent path. The dot '.' character is reserved
    // for splitting parent to child (etc)
    val parentPath: String,
    // The type of the object
    val type: Class<*>
) : TypeAdapter<I>() {

    /**
     * Writes [I] into the file configuration
     */
    fun write(`in`: I?) : Future<*> {

        // start a yaml configuration
        return IOThread.submit{
            val config = FileConfiguration(file, YAML)
            if (`in` == null) {
                // delete
                config.removePath(parentPath, '.')
            } else {
                config.set(parentPath, '.', serialize(`in`, `in`!!::class.java))
            }
            // Writes to a file
            config.save()
        }
    }

    fun read(): Future<I?> {
        return IOThread.submit<I?> {
            // read
            val config = FileConfiguration(file, YAML)
            val get = config.getElement(parentPath, '.') ?: return@submit null
            return@submit deserialize(get, type)
        }
    }

    /**
     * This method serializes an object of the type passed in to a [YamlElement]
     *
     * @param obj  The object
     * @param type The type of the object passed in
     * @return A [YamlElement], serialized from the object
     */
    abstract override fun serialize(obj: I, type: Type?): YamlElement

    /**
     * Deserializes a [YamlElement]
     *
     * @param element The element
     * @param type    The type of object to deserialize into
     * @return An object of type [T], deserialized from
     * [YamlElement]
     */
    abstract override fun deserialize(element: YamlElement?, type: Type?): I
}

// The thread that will be used during read/write
// operations
val IOThread = Executors.newSingleThreadExecutor()
