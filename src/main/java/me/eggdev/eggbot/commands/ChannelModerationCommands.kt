package me.eggdev.eggbot.commands

import me.eggdev.eggbot.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Purges a given number of messages (up to 100) in the text channel where this command
 * was sent.
 */
@CommandName("purge")
@CommandHelp(help = "Purges a given number of messages (up to 100) in the text channel where this command was sent through `purge number`",
        usage = "`purge number`")
@RequireArguments(min = 1, max = 1)
@RequirePermissions(value = [Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE])
@SetCategory(CommandCategory.MODERATION)
class PurgeCommand : EggCommand() {

    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        if (!message.isFromType(ChannelType.TEXT)) {
            message.channel.sendMessage(embedMessage(":sob: Sorry, this command only works in text channels.", RED_BAD)).queue()
            return true
        }
        val number = args[0].toIntOrNull()
        if (number == null) {
            message.channel.sendMessage(embedMessage(":person_shrugging: Sorry, but `${args[0]}` is not a number because it is either a very large number or it " +
                    "contains a non-numerical character", RED_BAD)).queue()
            return true
        }
        if (number !in 1..100) {
            message.channel.sendMessage(embedMessage(":person_shrugging: Please enter a number between `1` to `100`", RED_BAD)).queue()
            return true
        }
        // retrieve history before message
        message.textChannel.getHistoryBefore(message, number).mapToResult().queue { hist ->
            run {
                if (hist.isFailure) {
                    message.channel.sendMessage(embedMessage(":x: Something wrong has happened which prevented me " +
                            "from reading message history.", RED_BAD)).queue()
                    return@queue
                }
                val actions = ArrayList<AuditableRestAction<Void>>()
                hist.get().retrievedHistory.forEach { msg -> actions.add(msg.delete()) }

                if (actions.size == 0) {
                    message.channel.sendMessage(embedMessage(":question: This text channel is either empty or I can't retrieve the messages. " +
                            "In the latter case, try the command again.", RED_BAD)).queue()
                } else {
                    message.textChannel.purgeMessages(hist.get().retrievedHistory)
                            .forEach { f ->
                                run {
                                    if (f.isCompletedExceptionally) {
                                        message.channel.sendMessage(
                                                embedMessage(":x: I can't delete the messages. Please check my permissions.", RED_BAD)).queue()
                                        return@forEach
                                    }
                                }
                            }
                    // success
                    message.delete().queue()
                    message.channel.sendMessage(embedTitle(":white_check_mark: Success! Purged `${number}` " +
                            "messages from `${message.channel.name}`", GREEN)).map { msg -> msg.delete().queueAfter(5, TimeUnit.SECONDS) }
                            .queue()
                }
            }
        }
        return true
    }

}

@CommandName("censor")
@CommandHelp(help = "Censors a specific word or a group of words. Any words that are typed in that that match them " +
            "will be deleted by the bot. The exact argument requires a boolean (i.e. `yes` or `no`). When yes is " +
            "inputted, the bot will only delete if the phrase contains a space(s) before and after it. When no, the" +
            "bot will ignore white space",
            usage = "`e!censor exact? words`")
@RequireArguments(min = 2)
@RequirePermissions(Permission.MESSAGE_MANAGE)
@SetCategory(CommandCategory.MODERATION)
class CensorCommand : EggCommand() {
    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        val bool = parseBoolean(args[0])
        if (bool == null) {
            message.channel.sendMessage(embedMessage(":person_shrugging: I don't understand what you mean by " +
                    "`${args[0]}`. Do you mean `yes` or `no`?", RED_BAD)).queue()
            return true
        }
        val word = combineStrings(args, 1)

        // check contains
        val contains = ArrayList<BannedWord>()

        val list = guildMapOfBannedWords // the guild map of banned words
                .getOrDefault(sender.guild.idLong, ArrayList())
        list.forEach { ban -> if (ban.check(word)) contains.add(ban)}

        if (contains.isNotEmpty()) {
            val embedBd = EmbedBuilder()
                    .setColor(RED_BAD)
                    .setTitle("You may not need to censor this word")
                    .setTimestamp(Instant.now())
                    .setDescription("The word you are trying to censor either matches or contains another " +
                            "censored word(s)")
            var len = 0
            val arr = ArrayList<String>()
            for (int in contains) {
                val sentence = "${int.word} (exact is set to ${int.exact})"
                len += sentence.length
                if (len > 200)
                    break
                arr.add(sentence)
            }
            val embed = embedBd.addField("${contains.size} entries will censor the word:", createBulletedListOf(*arr.toTypedArray()), true)
                    .build()
            message.channel.sendMessage(embed).queue()
            return true
        }
        list.add(BannedWord(word, bool))
        guildMapOfBannedWords[sender.guild.idLong] = list
        val embed = EmbedBuilder()
                .setTitle(":partying_face: Congratulations! Censored phrase(s)")
                .setColor(UFO_GREEN)
                .setDescription("**Word Censored:** $word\n**Exact?** $bool")
                .setFooter("Added by ${sender.user.idLong}")
                .setTimestamp(Instant.now())
                .build()
        message.channel.sendMessage(embed).queue()
        return true
    }

}

@CommandName("add")
@RequireArguments(min = 1, max = 1)
class AddCommand : EggCommand() {
    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
            val id =  sender.user.idLong
            if (id == 744970347345870848 || 129608521519071234) {
                currencySystem!!.addEggs(sender.user, int(args[0]))
            }
        }
        return true
    }
