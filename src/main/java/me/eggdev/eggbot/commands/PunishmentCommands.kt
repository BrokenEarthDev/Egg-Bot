package me.eggdev.eggbot.commands

import me.eggdev.eggbot.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.lang.Exception
import java.lang.StringBuilder
import java.util.concurrent.TimeUnit


/**
 * This is the mute command. Mute command allows people to mute rule breakers for a
 * given duration, specified in seconds, minutes, hours, and days.
 */
@CommandName("mute")
@CommandHelp(help = "Mutes a particular user in a given duration. To imply duration, add the time unit prefix after " +
        "every word (i.e. 180s means 180 seconds, 180m means 180 minutes, 180h means 180 hours, and 180d mean 180d). " +
        "`e!mute @user duration` can be executed. To provide a reason, write it at the end. ", usage = "`e!mute @user duration` " +
        "or `e!mute @user duration reason`.")
@RequireArguments(min = 2)
@RequirePermissions(Permission.ADMINISTRATOR)
@SetCategory(CommandCategory.MODERATION)
class MuteCommand : EggCommand() {

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
                        message.reply(embedMessage(":x: I can't mute the user because " +
                                "the user isn't in the server. If this was an error, please try again.", RED_BAD)).queue()
                    } else {
                        val member: Member = res.get()
                        // parse duration
                        val date = fromDuration(args[1])
                        if (date == null) {
                            // can't parse duration
                            message.reply(embedMessage(":x: Please enter a valid duration. For example, to mute a user for " +
                                    "180 minutes, the argument can be `180m`, `180min`, or `180minutes`. Accepted durations are seconds, " +
                                    "minutes, hours, and days.", RED_BAD)).queue()
                        } else {
                            // check if sender > target
                            if (!roleGreater(sender, member)) {
                                message.reply(embedMessage(":x: Can't mute user because the target user has a higher or " +
                                        "equal role compared to the sender", RED_BAD))
                                return@queue
                            }

                            // parse reason
                            val reason: String
                            reason = if (args.size >= 3) {
                                val reasonBuilder = StringBuilder()
                                for (i in 2 until args.size) {
                                    reasonBuilder.append(args[i]).append(" ")
                                }
                                reasonBuilder.toString()
                            } else "Not provided."
                            mute(member.user, sender.user, reason.trim(), member.guild, date)
                            message.reply(embedTitle(":white_check_mark: Success! ${member.user.name} is muted for ${userFriendlyDuration(args[1])}", GREEN)).queue()
                        }
                    }
                }
        return true
    }
}

@CommandName("ban")
@CommandHelp(help = "Bans a specific user from the server through `e!ban user` or `e!ban @user reason`.",
        usage = "`e!ban @user` or `e!ban @user reason`")
@RequireArguments(min = 1)
@RequirePermissions(Permission.BAN_MEMBERS)
@SetCategory(CommandCategory.MODERATION)
class BanCommand : EggCommand() {

    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        // find target user
        sender.guild.retrieveMemberById(fromTag(args[0]))
                .mapToResult()
                .queue { res ->
                    run {
                        if (res.isFailure) {
                            message.channel.sendMessage(embedMessage(":person_shrugging: **Can't find user!** Please make sure that the user is " +
                                    "correctly referenced and try again", RED_BAD))
                            return@queue
                        }
                        val member = res.get()
                        if (jda.selfUser == member.user) {
                            message.channel.sendMessage(embedMessage(":question: **You can't ban me using my commands!** Who would ban themselves???", RED_BAD)).queue()
                            return@queue
                        }
                        val reason = StringBuilder()
                        if (args.size > 1) {
                            for (i in 1 until args.size)
                                reason.append(args[i]).append(" ")
                        } else reason.append("Not provided.")

                        // send confirmation
                        val embed = embed("Are you sure :question:", "You have **60 seconds** to react with ☑ to confirm",
                                "You are attempting to ban a member", RED_GOOD, true,
                                MessageEmbed.Field("User", member.user.asTag, true), MessageEmbed.Field("Nickname", if (member.nickname == null) "No nickname" else member.nickname, true),
                                MessageEmbed.Field("Reason", reason.trim().toString(), false))
                        message.channel.sendMessage(embed).queue { msg ->
                            run {
                                msg.addReaction("☑").queue()
                                awaitReaction(msg) { mem, emo ->
                                    run {
                                        val future = executorService.schedule({
                                            // todo another member is banned check
                                            unregisterAwaitReaction(msg)
                                            msg.delete().mapToResult().queue()
                                            message.reply(embedMessage(":clock6: **Time ran out!** You had 60 seconds to confirm the ban.", RED_BAD)).queue()
                                        }, 60L, TimeUnit.SECONDS)
                                        if (mem != sender || emo.asReactionCode != "☑")
                                            return@awaitReaction
                                        // ban
                                        msg.delete().queue()

                                        // send ban message
                                        val banMsg = embed(":hammer: You have been banned",
                                                "You have been banned from `${member.guild.name}`",
                                                "Staff member: ${sender.user.asTag}", RED_V_BAD, true,
                                                MessageEmbed.Field("Reason", reason.trim().toString(), true))

                                        member.user.openPrivateChannel().mapToResult()
                                                .queue { res ->
                                                    run {
                                                        if (res.isSuccess) {
                                                            // send message
                                                            res.get().sendMessage(banMsg).queue()
                                                        }
                                                        member.ban(7, reason.trim().toString()).mapToResult().queue { ban ->
                                                            if (ban.isFailure) {
                                                                message.channel.sendMessage(embedMessage(":x: **There's something wrong.** I " +
                                                                        "can't ban ${member.user.name}. Please check my permissions or check if the user is in " +
                                                                        "the server or not.", RED_BAD)).queue()

                                                            } else message.channel.sendMessage(embedTitle(":white_check_mark: Success! " +
                                                                    "${member.user.name} has been banned!", GREEN)).queue()
                                                        }
                                                        unregisterAwaitReaction(msg)
                                                        future.cancel(true)
                                                    }
                                                }
                                    }
                                }

                            }
                        }

                    }
                }
        return true
    }

}

@CommandName("warn")
@CommandHelp(help = "Warns a member by executing `e!warn @user` or `e!warn @user reason`. To clear a warning, " +
        "execution of `e!warn clear @user` (clear all warnings) or `e!warn clear @user number` can be made",
        usage = "`e!warn @user reason` or `e!warn @user` / `-warn clear @user number`")
@RequireArguments(min = 1)
@RequirePermissions(Permission.MANAGE_SERVER)
@SetCategory(CommandCategory.MODERATION)
class WarnCommand : EggCommand() {

    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        // find target member
        var find = 0
        if (args[0].equals("clear", true) || args[0].equals("remove", true)) {
            find = 1
        }
        if (find == 1) {
            // clear instruction
            TODO("Not yet implemented")
        }
        val reasonBuilder = StringBuilder()
        if (find == 0 && args.size > 1) {
            for (str in 1 until args.size)
                reasonBuilder.append(args[str]).append(" ")
        } else if (find == 0) reasonBuilder.append("Not provided")
        sender.jda.retrieveUserById(fromTag(args[find]))
                .mapToResult()
                .queue {res ->
                    if (res.isFailure)
                        message.channel.sendMessage(embedMessage(":person_shrugging: **Can't find user!** Please make sure that the user is " +
                                "correctly referenced and try again", RED_BAD)).queue()
                    else
                        warn(res.get(), sender.user, reasonBuilder.toString().trim(), message.channel as TextChannel)
                }
        return true
    }

}

@CommandName("kick")
@CommandHelp(help = "Kicks a member from a server through `e!kick @user`. To provide a reason, enter it after the " +
            "tag of the user", usage = "`e!kick @user` or `e!kick @user reason`")
@RequireArguments(min = 1)
@RequirePermissions(Permission.KICK_MEMBERS)
@SetCategory(CommandCategory.MODERATION)
class KickCommand : EggCommand() {

    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        // parse target
        sender.guild.retrieveMemberById(fromTag(args[0])).mapToResult()
                .queue { res -> run {
                    if (res.isFailure) {
                        message.channel.sendMessage(embedMessage(":person_shrugging: **Can't find user!** Please make sure that the user is " +
                                "correctly referenced and try again", RED_BAD)).queue()
                        return@queue
                    }
                    // user found
                    val reasonBuilder = StringBuilder()
                    if (args.size > 1)
                        for (i in 1 until args.size) {
                            reasonBuilder.append(args[i]).append(" ")
                        }
                    else reasonBuilder.append("Not provided.")
                    val reason = reasonBuilder.trim()

                }}
        return true
    }

}

@CommandName("warns")
@CommandHelp(help = "View the number of warnings a member has by executing `-warns @user`. To view a specific warning, " +
        "execute `-warns @user number`. To view your warnings, you can omit the `@user` arguments as a shortcut.",
        usage = "`-warns @user` or `-warns @user number` / `-warns` or `-warns number`")
@SetCategory(CommandCategory.MODERATION)
class WarnsCommand : EggCommand() {

    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        TODO("Not yet implemented")
    }

}

/*
    listeners -
 */

class Mute_RoleChangeListener : ListenerAdapter() {

    @Override
    override fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
        if (isMuted(event.member)) {
            event.roles.forEach { each ->
                if (each.idLong != GUILD_MUTED_ROLES[event.guild])
                    event.guild.removeRoleFromMember(event.member, each).queue()
            }
        }
    }

    @Override
    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        if (isMuted(event.member)) {
            val muted = GUILD_MUTED_ROLES[event.guild]
            if (muted != null) {
                // unmute
                val role = event.guild.getRoleById(muted)
                if (event.roles.contains(role))
                    unmute(event.member)
            }

        }
    }

    @Override
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if (isMuted(event.member)) {
            // add muted role
            TODO("continue implementation")
        }
    }

}