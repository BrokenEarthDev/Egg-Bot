package me.eggdev.eggbot.commands

import me.eggdev.eggbot.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * The registered commands
 */
val commands: List<EggCommand> = listOf(WarnCommand(), MuteCommand(), BanCommand(), PurgeCommand(), MemeCommand(),
        LayCommand(), CrackCommand(), Magic8BallCommand(), CensorCommand(), LeaderboardCommand(), PingCommand())
private var dispatcher: CommandDispatcher? = null

fun initCommandsSystem() {
    dispatcher = CommandDispatcher(jda)
}

/**
 * Represents a command executor
 */
interface CommandExecutor {

    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    fun executeCommand(sender: Member, message: Message, args: List<String>) : Boolean

}

class CommandDispatcher(val jda: JDA) {

    // The map of guild to string containing
    // prefixes for every guild
    private val guildPrefix: Map<Guild, String> = HashMap()

    class Listener(private val disp: CommandDispatcher) : ListenerAdapter() {
        override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
            val corresp = disp.guildPrefix.getOrDefault(event.guild, "e!")
            val raw = event.message.contentRaw
            print("content raw ==> $raw || $corresp")
            if (raw.startsWith(corresp)) {
                // possible command
                println("raw starts with corresp")
                var args: List<String> = raw.split(Regex("\\s+"))
                commands.forEach { cmd -> run {
                        println("COMMAND NAME = " + cmd.name + " | NAME = " + args[0].substring(corresp.length))
                        if (cmd.name.equals(args[0].substring(corresp.length), true)) {
                            println("Command has been processed")
                            if (!cmd.processCommand(event.message, args.subList(1, args.size)))
                                println("SD")
                        }
                    }
                }
            }
        }
    }

    init {
        jda.addEventListener(Listener(this))
    }
}

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class CommandHelp(val help: String, val usage: String)

/**
 * A class instanceof [EggCommand] should be annotated with
 * this to specify the name
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class CommandName(
        /**
         * @return The command name (without prefix)
         */
        val value: String,
)

/**
 * Defines cool down for a command
 */
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class Cooldown(
        /**
         * @return The cooldown in millis (default is 15 seconds)
         */
        val defaultCooldown: Long = 15 * 1000.toLong(),
        /**
         * @return The special cooldown (default is 5 seconds)
         */
        val specialCooldown: Long = 5 * 1000.toLong(),
)

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class RequireArguments(val min: Int = 0, val max: Int = Int.MAX_VALUE)


@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class RequirePermissions(vararg val value: Permission = [])


/**
 * This is the egg command class, the base class for every commands that
 * are to be registered. It requires an override of the function in CommandExecutor, which
 * is called whenever the command is executed.
 * <p>
 * Every sub-class of require the CommandName annotation to define the command name. Failure
 * to do so upon registration will cause an exception to be thrown. Additionally, it is
 * best to have the the CommandHelp annotation so that a usage message will be given whenever
 * an execution isn't successful.
 *
 * @see CommandName The command name, required for every command classes
 * @see CommandHelp Helps to define the help message and the usage message
 * @see RequireArguments Requires a specific argument before executing the command
 * @see RequirePermissions Requires a permission before executing the command
 */
@CommandName("null")
@CommandHelp(help = "null", usage = "null")
@RequireArguments
@RequirePermissions
abstract class EggCommand : CommandExecutor {

    /**
     * name of the command
     */
    val name: String

    /**
     * help of the command, can be null
     */
    val help: String?

    /**
     * command usage, can be null
     */
    val usage: String?

    /**
     * range of the arguments
     */
    val range: Range<Int>

    /**
     * The cooldown in milliseconds
     */
    val cooldown: Long

    /**
     * The special cooldown in milliseconds
     */
    val cooldownSpecial: Long
    private val permissions: Array<Permission>

    private val executed = HashMap<User, Date>()

    /**
     * Processes the command sent
     *
     * @param message The message
     * @param args The arguments of the command
     * @return Whether the execute command method is
     * executed.
     */
    fun processCommand(message: Message, args: List<String>) : Boolean {
        if (message.member == null) return false
        val member = message.member as Member
        if (executed.containsKey(member.user)) {
            // cooldown
            val date = Date()
            // todo check if user in server
            if (getMembersInGuild().contains(message.member!!.idLong) && date.time - executed[member.user]!!.time <= cooldownSpecial) {
                message.channel.sendMessage(embedMessage(":zap: You only have to wait for ${cooldownSpecial / 1000} seconds. " +
                        "Some commands have different cooldowns. Thanks for being in our server!", RED_BAD)).queue()
                return false
            } else if (!getMembersInGuild().contains(message.member!!.idLong) && date.time - executed[member.user]!!.time <= cooldown) {
                message.channel.sendMessage(embedMessage(":timer: Sorry, you have to wait `${cooldown / 1000}` seconds before " +
                        "re-executing. If you are in our server, the cooldown is `${cooldownSpecial / 1000}` seconds. " +
                        "Some commands have different cooldowns.", RED_BAD)).queue()
                return false
            } else executed.remove(member.user)

        }
        if (!range.satisfyRange(args.size)) {
            if (usage == null)
                message.channel.sendMessage(embedMessage(":x: **Invalid usage!**", RED_BAD)).queue()
            else message.channel.sendMessage(embedMessage(":x: **Invalid usage!** Use $usage", RED_BAD)).queue()
            return false
        }
        for (perm in permissions) {
            if (!member.hasPermission(perm)) {
                message.channel.sendMessage(embedMessage(":x: **Invalid permissions!** You do not have permission to execute this command!",
                        RED_BAD))
                        .queue()
                return false
            }
        }
        executeCommand(member, message, args)
        executed[member.user] = Date()
        println("Command has been executed")
        return true
    }

    init {
        val cmd: CommandName = this.javaClass.getAnnotation(CommandName::class.java)!!
        val help: CommandHelp? = this.javaClass.getAnnotation(CommandHelp::class.java)
        val args: RequireArguments? = this.javaClass.getAnnotation(RequireArguments::class.java)
        val perms: RequirePermissions? = this.javaClass.getAnnotation(RequirePermissions::class.java)
        val cooldown: Cooldown? = this.javaClass.getAnnotation(Cooldown::class.java)
        name = cmd.value
        if (help != null) {
            this.help = help.help
            usage = help.usage
        } else {
            this.help = null
            usage = null
        }

        range = if (args != null) {
            Range.between(args.min, args.max)
        } else Range.between(0, Int.MAX_VALUE)
        this.cooldown = cooldown?.defaultCooldown ?: 15000
        this.cooldownSpecial = cooldown?.specialCooldown?: 5000
        permissions = if (perms == null) emptyArray() else fromOutArray(perms.value)
    }

    /**
     * @return A copy of the permissions
     */
    fun getPermissions() : Array<Permission> {
        return permissions.copyOf()
    }

}
