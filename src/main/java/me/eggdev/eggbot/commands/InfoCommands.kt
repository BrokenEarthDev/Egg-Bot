package me.eggdev.eggbot.commands

import me.eggdev.eggbot.*
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import java.util.*
import kotlin.math.min

@CommandName("leaderboard")
@CommandHelp(help = "Displays both the level leaderboard and the egg leaderboard with a maximum of `10` entries",
            usage = "`e!leaderboard`")
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
            message.channel.sendMessage(embedMessage(":cricket: **Cricket noise.** I guess that this " +
                    "server is not that active. Ask your members to engage with the chat so I could collect data!", RED_BAD)).queue()
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

        message.channel.sendMessage(embed("Leaderboard", aBuilder.toString() + bBuilder.toString(),
                "Requested by ${sender.user.asTag}", UFO_GREEN, true)).queue()
        return true
    }

}

@CommandName("ping")
@CommandHelp(help = "Measures the ping between the user's computer to the bot's computer. This doesn't measure " +
            "the ping between the user's computer to the server's computer. Additionally, this command also measures " +
            "the gateway ping.", usage = "`e!ping`")
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
        message.channel.sendMessage(embed(":ping_pong: Pong!", "This took **${diff}** milliseconds\nThe gateway ping is **${jda.gatewayPing}** milliseconds",
                "Requested by ${sender.user.asTag}", UFO_GREEN, true)).queue()
        return true
    }

}