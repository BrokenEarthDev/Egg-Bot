package me.eggdev.eggbot.memory;


/**
 * Represents data of primitive type
 *
 * @deprecated There is no need for a class holding
 * primitive objects. Although the probability of it
 * being fully removed is still unknown.
 */
@Deprecated(forRemoval = true)
public class Primitive<T> {

    // The value
    public T value;

    public Primitive(T value) {
        if (value instanceof Integer || value instanceof Long || value instanceof Double ||
            value instanceof Byte || value instanceof Short || value instanceof Boolean  ||
            value instanceof Character || value == null || value instanceof String) {
            this.value = value;
        } else throw new IllegalArgumentException(value + " is not primitive (null, string, or any java primitive type)");
    }

}
