package me.eggdev.eggbot

import me.eggdev.eggbot.commands.*
import me.eggdev.eggbot.memory.FIFOList
import me.eggdev.eggbot.memory.MemoryCache
import net.codebox.homoglyph.Homoglyph
import net.codebox.homoglyph.HomoglyphBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import kotlin.collections.ArrayList
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

val jda: JDA = JDABuilder.create(System.getenv("EGG_TOKEN"), EnumSet.allOf(GatewayIntent::class.java))
        .setMemberCachePolicy(MemberCachePolicy.ALL)
        .enableCache(EnumSet.allOf(CacheFlag::class.java))
        .build()

val homoglyphs = HomoglyphBuilder.build()

val profiles = MemoryCache.Builder<FIFOList<UserProfile>>()
    .setWriteMillis(1800000) // 30 mins
    .create(FIFOList(500), null)

// TODO extract user profiles
val executorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

private var eggGuild: Guild? = null
private var membersInGuild: ArrayList<Long> = ArrayList()

// entry point of app
fun main() {
    jda.awaitReady()
    initLevelingSystem()
    initCommandsSystem()
    initCurrencySystem()
    initChannelModSystem()
    initSecuritySystem()

    eggGuild = jda.getGuildById(866458262064857089L)
    eggGuild!!.members.forEach {mem -> membersInGuild.add(mem.idLong)}
    jda.addEventListener(EggCommunityListener())
}

fun getEggGuild() : Guild {
    return  eggGuild!!
}

fun getMembersInGuild() : ArrayList<Long> {
    return membersInGuild
}

private class EggCommunityListener : ListenerAdapter() {
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if (event.guild == eggGuild && !membersInGuild.contains(event.member.idLong))
            membersInGuild.add(event.member.idLong)
    }

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        if (event.guild == eggGuild)
            membersInGuild.remove(event.user.idLong)
    }
}

/**
 * Retrieves the profile for a user. If no such profile exists,
 * a new profile for the user wil be created
 *
 * @param user The user
 */
fun getProfile(user: User) : UserProfile {
    var profile: UserProfile? = null
    profiles.get().forEach { p -> if (user == p.user) profile = p}
    if (profile == null) {
        // add to list of user profiles
        val profileNew = UserProfile(user)
        profiles.get().add(profileNew)
        return profileNew
    }
    return profile as UserProfile
}

/**
 * Contains information about the user, level and exp, and punishment profiles.
 */
class UserProfile(val user: User) {

    /**
     * The user level
     */
    var level: Int = 0

    /**
     * The user exp
     */
    var exp: Int = 0

    /**
     * The punishment profiles
     */
    val punishmentProfiles: ArrayList<PunishmentProfile> = ArrayList()

    /**
     * Gets punishment profiles of a specific type
     *
     * @param type The type of the punishment profiles
     */
    fun getProfiles(type: PunishmentType) : List<PunishmentProfile> {
        val list = ArrayList<PunishmentProfile>()
        punishmentProfiles.forEach { pp -> if (pp.type == type) list.add(pp) }
        return list
    }

    /**
     * Gets punishment profiles issued in a specific guild
     *
     * @param guild The guild
     * @return The collected punishment profiles
     */
    fun getPunishmentProfiles(guild: Guild) : List<PunishmentProfile> {
        val list = ArrayList<PunishmentProfile>()
        punishmentProfiles.forEach { pp -> if (pp.guild == guild) list.add(pp)}
        return list
    }

    fun getPunishmentProfiles(type: PunishmentType, guild: Guild) : List<PunishmentProfile> {
        val list = ArrayList<PunishmentProfile>()
        punishmentProfiles.forEach { pp -> if (pp.guild == guild && type == pp.type) list.add(pp)}
        return list
    }

}

