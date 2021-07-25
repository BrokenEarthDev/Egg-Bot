package me.eggdev.eggbot

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

val guildMapOfBannedWords = HashMap<Long, ArrayList<BannedWord>>()

fun initChannelModSystem() {
    jda.addEventListener(BannedWordsCheck())
}

/**
 * Represents a banned word from chat
 *
 * @param word The word that is banned
 * @param ignoreWhiteSpace Whether whitespace will be ignored
 *                         or not
 */
class BannedWord(val word: String, val ignoreWhiteSpace: Boolean) {

    /**
     * Checks whether the word is contained in the [string]. If [ignoreWhiteSpace]
     * is true, all spaces will be removed and replaced with ""
     *
     * @param string The string
     * @return Whether [word] is contained in [string]
     */
    fun check(string: String) : Boolean {
        val new = if (ignoreWhiteSpace) word.replace(" ", "")
                        else word
        val content = if (ignoreWhiteSpace) string.replace(" ", "")
                            else string
        return content.contains(new)
    }
}

class BannedWordsCheck : ListenerAdapter() {

    private fun checkDelete(message: Message) {
        if (message.author.isBot) return
        val banned = guildMapOfBannedWords.getOrDefault(message.guild.idLong, ArrayList())
        banned.forEach { ban -> if (ban.check(message.contentRaw)) {
                    message.delete().queue()
                    return
        }}
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        checkDelete(event.message)
    }

    override fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
        checkDelete(event.message)
    }
}