package me.eggdev.eggbot

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

val blacklistingManagers = HashMap<Long, GuildBlacklistingManager>()

fun initSecuritySystem() {
    jda.addEventListener(JoinListener())
}

/**
 * Contains information about blacklisted users and names.
 * This is important in checking if a user is blacklisted based on
 * a function [isBlacklisted]. Each guild should have a guild blacklisting
 * manager
 *
 * @param guild The guild's id
 * @param users The user ids of blacklisted users
 * @param match Banned names that should match the user's name in order
 *              for the user to get banned. Cases will be ignored and
 *              homoglyphs will also be checked
 * @param contains Banned names that should be included in the user's name
 *                 in order for the user to get banned. Cases will be ignored
 *                 and homoglyphs will also be checked. The length of each
 *                 string should be GREATER THAN 4
 */
class GuildBlacklistingManager(val guild: Long,
                               val users: HashSet<Long>,
                               val match: HashSet<String>,
                               val contains: HashSet<String>) {

    /**
     * Checks whether the member, based on the information in the sets
     * [users], [match], and [contains], is blacklisted
     *
     * @param user The user to check
     * @return Whether the user should be blacklisted or not
     */
    fun isBlacklisted(user: User) : Boolean {
        if (users.contains(user.idLong))
            return true

        // check hemoglyphs
        match.forEach { str -> run {
            val search = homoglyphs.search(user.name.toLowerCase(), str.toLowerCase())
            search.forEach { result -> run {
                if (result.index == 0 && result.match.length == str.length) {
                    // equals
                    return true
                }
            } }
        } }

        // check contains
        contains.forEach { str -> run {
            val res = homoglyphs.search(user.name.toLowerCase(), str.toLowerCase())
            if (res.size >= 1)
                return true // contains
        } }

        return false
    }

    fun isMatch(user: User, match: String) : Boolean {
        val res = homoglyphs.search(user.name.toLowerCase(), match.toLowerCase())
        return res.isNotEmpty() && res[0].index == 0
    }

}

private class JoinListener : ListenerAdapter() {

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        // check if blacklisted
        val get = blacklistingManagers[event.guild.idLong]
        if (get != null && get.isBlacklisted(event.member.user)) {
            // user blacklisted, ban
            ban(event.member, event.guild.selfMember, "You are blacklisted")
        }
    }
}