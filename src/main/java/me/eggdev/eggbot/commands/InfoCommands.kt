package me.eggdev.eggbot.commands

import me.eggdev.eggbot.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Emote
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import java.awt.Color
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.min

@CommandName("leaderboard")
@CommandHelp(
    help = "Displays both the level leaderboard and the egg leaderboard with a maximum of `10` entries",
    usage = "`e!leaderboard`"
)
@SetCategory(CommandCategory.ENTERTAINMENT)
class LeaderboardCommand : EggCommand() {

    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        val units = levelingSystem.sort(sender.guild)
        val eggs = currencySystem!!.sort(sender.guild)

        // Emptiness check
        if (units.isEmpty() && eggs.isEmpty()) {
            message.channel.sendMessage(
                embedMessage(
                    ":cricket: **Cricket noise.** I guess that this " +
                            "server is not that active. Ask your members to engage with the chat so I could collect data!",
                    RED_BAD
                )
            ).queue()
            return true
        }

        // Top level builder
        val aBuilder = StringBuilder()
        if (units.isNotEmpty()) {
            aBuilder.append("**Displaying top levels**\n")
            var num = 0
            for (i in 0 until min(20, units.size)) {
                if (num > 500)
                    break
                val new = " **#${i + 1}.** ${units[i].asTag} (level **${levelingSystem.levelCache(units[i]).level}**)"
                aBuilder.append(new).append("\n")
                num += new.length
            }
        }

        // Top egg builder
        val bBuilder = StringBuilder()
        if (eggs.isNotEmpty()) {
            bBuilder.append("\n**Displaying top :egg: hoarders**\n")
            var numB = 0
            for (i in 0 until min(20, eggs.size)) {
                if (numB > 500)
                    break
                val new = " **#${i + 1}.** ${eggs[i].asTag} (**${currencySystem!!.getEggs(eggs[i])} :egg:**)"
                bBuilder.append(new)
                numB += new.length
            }
        }

        message.channel.sendMessage(
            embed(
                "Leaderboard", aBuilder.toString() + bBuilder.toString(),
                "Requested by ${sender.user.asTag}", UFO_GREEN, true
            )
        ).queue()
        return true
    }

}

@CommandName("ping")
@CommandHelp(
    help = "Measures the ping between the user's computer to the bot's computer. This doesn't measure " +
            "the ping between the user's computer to the server's computer. Additionally, this command also measures " +
            "the gateway ping.", usage = "`e!ping`"
)
@SetCategory(CommandCategory.UTILITIES)
class PingCommand : EggCommand() {

    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        val diff = Date().time - message.timeCreated.toInstant().toEpochMilli()
        message.channel.sendMessage(
            embed(
                ":ping_pong: Pong!",
                "This took **${diff}** milliseconds\nThe gateway ping is **${jda.gatewayPing}** milliseconds",
                "Requested by ${sender.user.asTag}",
                UFO_GREEN,
                true
            )
        ).queue()
        return true
    }

}

@CommandName("help")
@CommandHelp(
    help = "Gives help about the bot's function and more information about commands",
    usage = "`e!help` or `e!help argument`"
)
@SetCategory(CommandCategory.UTILITIES)
open class HelpCommand : EggCommand() {

    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        if (args.isNotEmpty()) {
            val combined = combineStrings(args, 0)
            val command = getCommand(combined)
            if (command != null) {
                val permsString = ArrayList<String>()
                command.getPermissions()
                    .forEach { perm -> permsString.add(perm.toString().toLowerCase().replace("_", " ")) }
                val permissions =
                    "• **Permissions:** " + (if (permsString.isEmpty()) "None required" else "\n" + createBulletedListOf(
                        *permsString.toTypedArray()
                    ))
                val arguments = "• **Number of arguments:** " +
                        if (command.range.from == 0 && command.range.to == Int.MAX_VALUE)
                            "Any"
                        else if (command.range.from == 0 && command.range.to > 0)
                            "Up to ${command.range.to}"
                        else if (command.range.from == 0 && command.range.to == 0)
                            "No argument is required"
                        else if (command.range.from != 0 && command.range.to == Int.MAX_VALUE)
                            "${command.range.from} and upwards"
                        else "From ${command.range.from} to ${command.range.to}"

                val builder = EmbedBuilder()
                    .setTitle("Command information for ${command.name}")
                    .setDescription("• **Help:** " + command.help)
                    .appendDescription("\n")
                    .appendDescription("• **Usage:** " + command.usage)
                    .appendDescription("\n")
                    .appendDescription(arguments)
                    .appendDescription("\n")
                    .appendDescription("• **Category:** " + command.category.name.toLowerCase())
                    .appendDescription("\n")
                    .appendDescription(permissions)
                    .setColor(FRENCH_SKY_BLUE)

                message.channel.sendMessage(builder.build()).queue()
                return true
            }
        }
        val embed = EmbedBuilder()
            .setTitle("Help :question:")
            .setColor(UFO_GREEN)
            .addField(
                "What is this bot for?", "This is a multi-purpose, elegant, and the perfect bot that " +
                        "suits all your needs. From memes to muting someone to laying eggs, this bot will be your " +
                        "favorite one. [Join our server](https://discord.gg/KykFgvwcDN)", false
            )
            .addField(":video_game: Entertainment", "`e!entertainment`", true)
            .addField(":police_officer: Moderation", "`e!moderation`", true)
            .addField(":cd: Utilities", "`e!utilities`", true)
            .addField("Invite our bot", "Invite links to be developed", true)
            .addField("Suggest a feature", "[In our server](https://discord.gg/KykFgvwcDN)", true)
            .addField("Like our bot?", "Please vote for our bot here", true)
            .setFooter("Developed with ❤ by our developers")
            .build()
        message.channel.sendMessage(embed).queue()
        return true
    }

    protected fun category(category: CommandCategory, description: String, color: Color): MessageEmbed {
        val commands = category.getCommands()
        val builder = EmbedBuilder()
        when (category) {
            CommandCategory.ENTERTAINMENT -> builder.setTitle(":video_game: Entertainment Commands")
            CommandCategory.MODERATION -> builder.setTitle(":police_officer: Moderation Commands")
            else -> builder.setTitle(":cd: Utility Commands")
        }

        commands.forEach { cmds ->
            run {
                builder.addField(cmds.name, "`e!help ${cmds.name}`", true)
            }
        }

        builder.setDescription(description)
        builder.setColor(color)
        return builder.build()
    }

}

@CommandName("entertainment")
@CommandHelp(help = "Sends information about entertainment commands", usage = "`e!entertainment`")
class EntertainmentHelpCommand : HelpCommand() {
    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        val e = super.category(
            CommandCategory.ENTERTAINMENT, "The following are entertainment commands," +
                    " which can be used by users in this server", FLIRTATIOUS
        )
        message.channel.sendMessage(e).queue()
        return true
    }

}

@CommandName("moderation")
@CommandHelp(help = "Sends information about moderation commands", usage = "`e!moderation`")
class ModerationHelpCommand : HelpCommand() {
    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        val m = category(
            CommandCategory.MODERATION, "The following are moderation commands, which " +
                    "can be used by staff members", FLIRTATIOUS
        )
        message.channel.sendMessage(m).queue()
        return true
    }
}

@CommandName("utilities")
@CommandHelp(help = "Sends information about utility commands", usage = "`e!utilities`")
class UtilitiesHelpCommand : HelpCommand() {
    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        val u = category(
            CommandCategory.UTILITIES, "The following are utility commands, which " +
                    "can be very useful to any member", FLIRTATIOUS
        )
        message.channel.sendMessage(u).queue()
        return true
    }
}

@CommandName("poll")
@CommandHelp(help = "Sends a poll for user to vote by executing `e!poll question`", usage = "`e!poll question`")
@RequireArguments(min = 1)
@RequirePermissions(Permission.MANAGE_CHANNEL)
@SetCategory(CommandCategory.UTILITIES)
class PollCommand : EggCommand() {
    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        val question = combineStrings(args, 0)
        if (question.endsWith("?")) question.dropLast(1)
        val poll = EmbedBuilder()
            .setTitle("Poll: $question :grey_question: ")
            .setColor(FLIRTATIOUS)
            .setFooter("Poll created by ${sender.user.asTag}")
            .setTimestamp(Instant.now())
            .build()
        message.channel.sendMessage(poll).queue { send ->
            run {
                send.addReaction("☑").queue()

                send.addReaction(emotes("maybe", false)[0]).queue()
                send.addReaction(emotes("cross_mark", false)[0]).queue()
            }
        }
        return true
    }

}

@CommandName("eggs")
@CommandHelp(help = "Cheks the number of :egg: s you or another use has", "`e!eggs` or `e!eggs @target`")
@RequireArguments(max = 1)
@SetCategory(CommandCategory.UTILITIES)

class EggsCommand : EggCommand() {

    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        sender.guild.retrieveMemberById(fromTag(args[0]))
            .mapToResult()
            .queue { res ->
                if (res.isFailure) {
                    message.reply(embedMessage("❌ You can't check the number of eggs from this user has because " + "the user isn't in the server. If this was an error, please try again.", RED_BAD)).queue()
                } else if (args.size == 1) {
                    val member: Member = res.get()
                    val eggs = currencySystem!!.getEggs(member.user)
                    message.reply(embedMessage("This user has: " + eggs + ":egg: 's", UFO_GREEN)).queue()
                } else {
                    eggs_sender = currencySystem!!.getEggs(sender.user)
                    message.reply(embedMessage("You have: " + eggs_sender + ":egg: 's", UFO_GREEN)).queue()
                }
            }
        return true
    }
}