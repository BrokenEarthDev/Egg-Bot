package me.eggdev.eggbot

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message

class MessagesCaches {

    // caches messages up to 1000 / guild before resetting them to the last
    // 500 messages
    private val map = HashMap<Long, ArrayList<Message>>()

    fun register(message: Message, guild: Guild) {
        val msgs = map.getOrDefault(guild.idLong, ArrayList())
        if (msgs.size > 1000) {
            for (i in 0 until 500 + 1) {
                // remove the message
                msgs.removeAt(i)
            }
        }
        msgs.add(message)
        map[guild.idLong] = msgs
    }

    /**
     * Gathers the original message. If [message] is edited, then an old
     * string will be returned if it is contained in the cache.
     *
     * @param message The edited message
     * @param replace Whether to replace it in the cache
     * @return The old message's string if it exists
     */
    fun gatherOriginal(message: Message, replace: Boolean) : String? {
        val msgs = map[message.guild.idLong]
        if (msgs != null && msgs.isNotEmpty()) {
            msgs.forEach { m -> run {
                if (m.idLong == message.idLong) {
                    val gathered = m.contentRaw
                    if (replace) {
                        msgs.remove(m)
                        msgs.add(message)
                    }
                    return gathered
                }
            }
            }
        }
        return null
    }

}