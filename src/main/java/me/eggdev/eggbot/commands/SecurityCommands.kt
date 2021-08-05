package me.eggdev.eggbot.commands

import me.eggdev.eggbot.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import java.util.concurrent.ThreadLocalRandom

@SetCategory(CommandCategory.MODERATION)
class SecurityCommand : EggCommand() {
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

@CommandName("blacklist")
@CommandHelp(help = "Blacklists a specific user, a user with a name, and a user with a name containing a " +
            "specific phrase. To blacklist a specific user, executing `e!blacklist @user` is appropriate. " +
            "To blacklist a user with a name, execute `e!blacklist equals name`. To blacklist a user with a phrase," +
            "`e!blacklist contains name` is appropriate. The bot will also look for hemoglyphs",
            usage = "`e!blacklist @user`, `e!blacklist equals/contains name`")
@RequireArguments(min = 1)
@RequirePermissions(Permission.BAN_MEMBERS, Permission.ADMINISTRATOR)
@SetCategory(CommandCategory.MODERATION)
class BlacklistCommand : EggCommand() {
    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        var manager = blacklistingManagers[sender.guild.idLong]
        if (manager == null) {
            manager = GuildBlacklistingManager(sender.guild.idLong, HashSet(), HashSet(), HashSet())
            blacklistingManagers[sender.guild.idLong] = manager
        }
        if (args[0].startsWith("<@!") && args[0].endsWith(">")) {
            // bans a specific user
            sender.guild.retrieveMemberById(fromTag(args[0])).mapToResult()
                    .queue { res -> run {
                        // add list to blacklist
                        if (res.isFailure) {
                            message.channel.sendMessage(embedMessage(":person_shrugging: **Can't find user!** Please make sure that the user is " +
                                    "correctly referenced and try again", RED_BAD)).queue()
                            return@queue
                        }
                        manager.users.add(res.get().idLong)
                    }}
        } else if (args[0].equals("contains", true) || args[0].equals("contain", true)) {
            // ban contains
            var combined = combineStrings(args, 1)
            // search all members with that
        } else if (args[0].equals("equals", true) || args[0].equals("equal", true)) {

        } else {
            // invalid usage
        }
        return true
    }

}