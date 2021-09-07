package me.eggdev.eggbot.memory

import me.eggdev.eggbot.Inventory
import me.eggdev.eggbot.Pet
import org.fusionyaml.library.`object`.YamlElement
import org.fusionyaml.library.`object`.YamlObject
import java.io.File
import java.lang.reflect.Type
import kotlin.collections.ArrayList

/*

Contains all database connector implementations.

 */

class NullConnector<T>: StorageConnector<T>(File(""), "", Object::class.java) {
    /**
     * This method serializes an object of the type passed in to a [YamlElement]
     *
     * @param obj  The object
     * @param type The type of the object passed in
     * @return A [YamlElement], serialized from the object
     */
    override fun serialize(obj: T, type: Type?): YamlElement {
        TODO("Not yet implemented")
    }

    /**
     * Deserializes a [YamlElement]
     *
     * @param element The element
     * @param type    The type of object to deserialize into
     * @return An object of type [T], deserialized from
     * [YamlElement]
     */
    override fun deserialize(element: YamlElement?, type: Type?): T {
        TODO("Not yet implemented")
    }

}

class InventoryConnector: StorageConnector<Inventory>(
    File("inventories.yml"),
    "inventories",
    CachedList::class.java
) {

    override fun serialize(obj: Inventory, type: Type?): YamlElement {
        val obj = YamlObject()
            .set(obj.owner.toString(), YAML.serialize(obj.pets, ArrayList::class.java))
        return obj
    }


    override fun deserialize(element: YamlElement?, type: Type?): Inventory {
        val obj = element!!.asYamlObject
        val list = ArrayList(obj.keySet())

        val id = list[0].toLongOrNull() ?: return Inventory(0L, ArrayList())
        val petsList = obj.get(list[0])
        val petsArrayList = YAML.deserialize(petsList, ArrayList::class.java)
                    as ArrayList<Pet>
        return Inventory(id, petsArrayList)
    }

}

class PetConnector: StorageConnector<Pet>(
    File("inventories.yml"),
    "pets",
    Pet::class.java
) {
    /**
     * This method serializes an object of the type passed in to a [YamlElement]
     *
     * @param obj  The object
     * @param type The type of the object passed in
     * @return A [YamlElement], serialized from the object
     */
    override fun serialize(obj: Pet, type: Type?): YamlElement {
        return YamlObject()
            .set(obj.petName, YamlObject()
                .set("emoji", obj.emoji)
                .set("rarity", obj.rarity)
            )
    }

    /**
     * Deserializes a [YamlElement]
     *
     * @param element The element
     * @param type    The type of object to deserialize into
     * @return An object of type [T], deserialized from
     * [YamlElement]
     */
    override fun deserialize(element: YamlElement?, type: Type?): Pet {
        val obj = element!!.asYamlObject
        val list = ArrayList(obj.keySet())

        val name = list[0] // only compare through NAME
        Pet.values().forEach { pet ->
            run {
                if (pet.name.equals(name, true))
                    return pet
            }
        }
        return Pet.DOG // unable to identify pet from name therefore
                       // return any (fallback) pet.
                       // todo add an "unknown pet"
    }

}