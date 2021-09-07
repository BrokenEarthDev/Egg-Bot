package me.eggdev.eggbot

import me.eggdev.eggbot.memory.MemoryCache
import me.eggdev.eggbot.memory.NullConnector
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.requests.RestAction
import java.awt.Color
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

val GUILD_MUTED_ROLES: HashMap<Guild, Long> = HashMap()
val GUILD_MUTED_ROLES_V2 = MemoryCache<HashMap<Guild, Long>>(HashMap(), 0, 10000,
    0, NullConnector())

/**
 * The punishment type (possible punishments for a user)
 */
enum class PunishmentType {

    /**
     * A warning is a message sent to a user warning them
     * that further actions may be taken
     */
    WARN,

    /**
     * Disallows user from speaking for a specific amount of time
     */
    MUTE,

    /**
     * Bans the user from joining
     */
    BAN,

    /**
     * Kick the user
     */
    KICK
}

/**
 * An immutable class containing information about a specific punishment
 */
class PunishmentProfile(val user: User, val punisher: User, val guild: Guild, val type: PunishmentType,
                        val reason: String, val date: Date, val end: Date?, var ongoing: Boolean)


/**
 * Issues a warning to a user
 *
 * @param user The target user
 * @param punisher The user who issued the warning
 * @param reason The reason
 * @param issued The channel where the command was run
 */
fun warn(user: User, punisher: User, reason: String, issued: TextChannel) : PunishmentProfile {
    val profile = PunishmentProfile(user, punisher, issued.guild, PunishmentType.WARN,
            reason, Date(), null, true)
    val userProfile = getProfile(user)
    val punishments = userProfile.punishmentProfiles
    punishments.add(profile)
    // Embed warning
    val embed = embed(":question: Warning " + user.asTag, "You have been warned in `${issued.guild.name}` for violating a rule " +
            "or rules", "Staff member: " + punisher.asTag, PINK, true, MessageEmbed.Field("Reason", reason, true),
            MessageEmbed.Field("Number of warnings", userProfile.getPunishmentProfiles(PunishmentType.WARN, issued.guild).size.toString(), true))

    issued.sendMessage(embed)
            .flatMap {msg -> return@flatMap user.openPrivateChannel()}
            .flatMap {pc -> return@flatMap pc.sendMessage(embed)}
            .mapToResult().queue()
    return profile
}

/**
 * Mutes the user and creates a warn profile
 */
fun mute(user: User, punisher: User, reason: String, issued: Guild, end: Date) : PunishmentProfile {
    val profile = PunishmentProfile(user, punisher, issued, PunishmentType.MUTE, reason, Date(), end, true)
    val userProfile: UserProfile = getProfile(user)
    userProfile.punishmentProfiles.add(profile)
    val muted: Long? = GUILD_MUTED_ROLES[issued]
    val retrieved: Role? = if (muted == null) null else issued.getRoleById(muted)
    if (retrieved == null) {
        // create
        createMutedRole(issued).queue {r -> run {
                GUILD_MUTED_ROLES[issued] = r.idLong
                issued.retrieveMember(user).queue { mem ->
                    run {
                        issued.addRoleToMember(mem, r).queue {role ->
                            executorService.schedule({ unmute(mem)}, end.time - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                            issued.textChannels.forEach { i -> run {
                                i.manager.putPermissionOverride(r, null, arrayListOf(Permission.MESSAGE_HISTORY,
                                    Permission.MESSAGE_READ, Permission.MESSAGE_WRITE)).mapToResult().queue()
                            } }
                        }
                    }
                }
            }
        }
    } else issued.retrieveMember(user).queue {mem -> issued.addRoleToMember(mem, retrieved).queue()}
    val embed = embed(":sob: You have been muted", "You have been muted in `${issued.name}` for " +
            "violating a rule(s)", "Staff member: " + punisher.asTag, RED_V_BAD, true,
            MessageEmbed.Field("Reason", reason, true),
            MessageEmbed.Field("Unmute Date (GMT)", formatDate(end, true), true))

    user.openPrivateChannel()
            .flatMap {pm -> return@flatMap pm.sendMessage(embed)}
            .mapToResult()
            .queue()
    return profile
}

/**
 * Unmutes the member by setting the ongoing boolean to false in every
 * mute profile. Additionally, it checks whether the member has a
 * registered muted role. If so, it will be removed.
 *
 * @param member The target member
 */
fun unmute(member: Member) {
    // unmutes the user
    val muteProfiles = getProfile(member.user).getPunishmentProfiles(PunishmentType.MUTE, member.guild)
    muteProfiles.forEach {profile -> if (profile.ongoing) profile.ongoing = false}
    // remove muted role
    val muteRole = GUILD_MUTED_ROLES[member.guild]
    if (muteRole != null) {
        val role = member.guild.getRoleById(muteRole)
        if (role != null)
            member.guild.removeRoleFromMember(member, role).queue()
        // todo add prior roles
    }
}

/**
 * Bans the member from the guild.
 *
 * @param member The member
 * @param punisher The one who banned the member
 * @param reason The reason of the ban
 */
fun ban(member: Member, punisher: Member, reason: String) {
    // bans the user
    val pro = PunishmentProfile(member.user, punisher.user, punisher.guild, PunishmentType.WARN,
                                reason, Date(), null, false) // A "one time" punishment
    getProfile(member.user).punishmentProfiles.add(pro)

    val banMsg = embed(":hammer: You have been banned",
            "You have been banned from `${member.guild.name}`",
            "Staff member: ${punisher.user.asTag}", RED_V_BAD, true,
            MessageEmbed.Field("Reason", reason.trim(), true))

    member.user.openPrivateChannel().mapToResult()
            .queue {result -> run {
                if (result.isSuccess)
                    result.get().sendMessage(banMsg).queue()
                member.guild.ban(member, 7, reason).queue()
            }}

}

fun kick(user: User, punisher: User, reason: String, issued: Guild) {
    val profile = PunishmentProfile(user, punisher, issued, PunishmentType.KICK, reason, Date(), null,
            false) // kick is a "one-time" punishment. Once executed, the punishment technically
                           // ends
    val embed = embed(":boot: You have been kicked", "You have been kicked in " +
            "${issued.name} for violating one or more rules", "Staff member: ${punisher.asTag}",
            RED_BAD, true, MessageEmbed.Field("Reason", reason, true))
    user.openPrivateChannel().mapToResult().queue { p -> run {
        if (p.isSuccess) {
            p.get().sendMessage(embed).queue()
        }
        issued.kick(user.id).reason(reason).mapToResult().queue { r->
            if (r.isSuccess)
                getProfile(user).punishmentProfiles.add(profile)
        }
    }}
}

/**
 * Checks whether the member is muted or not
 *
 * @param member The member
 * @return Whether the member is muted in their guild or not
 */
fun isMuted(member: Member) : Boolean {
    val profiles = getProfile(member.user).getPunishmentProfiles(PunishmentType.MUTE, member.guild)
    profiles.forEach { p -> if (p.ongoing) return true}
    return false
}

/**
 * Creates a muted role
 */
private fun createMutedRole(target: Guild) : RestAction<Role> {
    return target.createRole()
            .setColor(Color.BLACK).setName("Muted")
            .setPermissions(Permission.UNKNOWN)
            .setMentionable(false)
}