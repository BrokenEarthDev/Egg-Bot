import me.eggdev.eggbot.FLIRTATIOUS
import me.eggdev.eggbot.embedMessage
import me.eggdev.eggbot.executorService
import me.eggdev.eggbot.jda
import me.eggdev.eggbot.memory.FIFOList
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

fun initPetsSystem() {
    jda.addEventListener(MsgListener())
}

// Probability a pet will appear when a message is sent
// in a text channel
private val petProbability = 0.1

// List that stores guild ids with active
// events
val activeEvents = FIFOList<Long>(256) // up to 256 "events" active

val inventories = FIFOList<Inventory>(256) // up to 256 retrieved inventories
                                                   // todo- this may not work especially if
                                                   // todo there are more than 256 members
                                                   // todo until the cache system is capable
                                                   // todo of loading info

/**
 * Allows a pet to appear whenever this execution is called
 * except if an active event already exists in the guild.
 *
 * @param channel The text channel to show the message
 */
fun appearPet(channel: TextChannel)  {
    if (activeEvents.contains(channel.guild.idLong)) {
        // active event in guild
        return
    }

    channel.sendMessage(
        embedMessage("**A wild pet has appeared** Please use e!capture (number of :egg:) to capture a " +
                "pet! You have **5** minutes to capture pet!", FLIRTATIOUS
        )
    ).queue { send ->
        run {
            activeEvents.add(channel.guild.idLong)

            // todo I will implement a more efficient
            // todo timer soon
            executorService.schedule({ run {
                    activeEvents.remove(channel.guild.idLong)
                }
            }, 5, TimeUnit.MINUTES)

        }
    }
}

/**
 * Retrieves an inventory for a [User], or creates a new one if
 * it doesn't exist.
 *
 * @param user The user
 * @return The inventory for the user
 */
fun retrieveInventory(user: User) : Inventory {
    var inv : Inventory? = null
    inventories.forEach { i -> if (i.owner.equals(user)) {
            inv = i
            return@forEach
        }
    }
    if (inv == null) {
        // todo- again, the user can have an inventory but due to the wau
        // todo the system currently works, it may return an empty one
        // todo we need to implement a caching system for a list where it can be reloaded
        val inventory = Inventory(user.idLong, ArrayList())
        inventories.add(inventory)
        return inventory
    }
    return inv as Inventory
}

private class MsgListener : ListenerAdapter() {
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        val random = ThreadLocalRandom.current().nextInt(0, 100)
        if (random <= (petProbability * 100)) {
            // appear pet
            // lucky enough :)
            appearPet(event.channel)
        }
    }
}

/**
 * Represents an inventory for a [User], where it contains information
 * such as pets, etc.
 *
 * @param owner The user who owns the inventory
 * @param pets  The collection of pets
 */
data class Inventory(val owner: Long, val pets: ArrayList<Pet>)

/**
 * Contains information about every pet
 *
 * @param name The name of the pet
 * @param emoji The emoji that can be used to represent the pet
 * @param rarity The probability (from 0 to 1) that this pet
 *               can be captured.
 */
enum class Pet(name: String, emoji: String, rarity: Double){

    // todo: AwwabienTG - implement rarities

    DOG("Dog", ":dog:", -1.0),
    CAT("Cat", ":cat:", -1.0)

}