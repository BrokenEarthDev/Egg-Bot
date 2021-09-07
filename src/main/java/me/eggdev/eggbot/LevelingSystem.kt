package me.eggdev.eggbot

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.awt.Color
import java.time.Instant
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.ArrayList
import kotlin.math.round
import kotlin.math.roundToInt

/*
  Better leveling system than the one in traverse bot. The exp gains depends on
  the length of the messages. It has to be passed through a spam check first.

  The currency system is eggs. Here are the awards per level up

    (1) DIRT DIVISION - +20 eggs/level
    (2) WOOD DIVISION - +50 eggs/level
    (3) STONE DIVISION - +100 eggs/level
    (4) IRON DIVISION - +200 eggs/level
    (5) GOLD DIVISION - +500 eggs/level
    (6) DIAMOND DIVISION - +5000 eggs/level

  The levels for each division -

    (1) DIRT DIVISION - levels 1 to 9
    (2) WOOD DIVISION - levels 10 to 29
    (3) STONE DIVISION - levels 30 to 49
    (4) IRON DIVISION - levels 50 to 74
    (5) GOLD DIVISION - levels 75 to 99
    (6) DIAMOND DIVISION - levels 100+

 */

private val maxLevels = maxLevels() // Maximum level per division
private val totalLevelUnitPerDivision = totalLevelUnitPerDivision() // The level unit required to move to
                                                                    // another division

private val levelUnitPerLevelInDivision = levelUnitPerLevelInDivision() // The level unit required to move
                                                                        // to another level in a division
private var levelingSysListener: LevelingSystemListener? = null

val levelingSystem = LevelingSystem(ArrayList())

fun initLevelingSystem() {
    levelingSysListener = LevelingSystemListener(jda, arrayOf(levelingSystem))
}

/**
 * Represents the division that comes with a group of levels
 *
 * @param division The division name (user friendly format)
 * @param messageLength The level unit (see below)
 * @param range The range of the levels
 * @param rewardEggs The amount of eggs given per level up in a division
 * @param color The 'color' of the division
 *
 * @see LevelUnitManager
 * @see LevelingSystem
 */
enum class Division(val division: String, val levelUnit: Int, val range: Range<Int>,
                    val rewardEggs : Int, val icon: String, val color: Color) {

    DIRT("Dirt Division", 400, Range.between(0, 9), 20, ":poop:", rgb(155,118,83)),
    WOOD("Wood Division", 1000, Range.between(10, 29), 75, ":wood:", rgb(193,154,107)),
    STONE("Stone Division", 2400, Range.between(30, 49), 225, "<:stone_block:866136510265163807>", rgb(189, 195, 199)),
    IRON("Iron Division", 6000, Range.between(50, 74), 700, "<:iron_rank:866137225246539816>", rgb(236, 240, 241)),
    GOLD("Gold Division", 18000, Range.between(75, 99), 2200, "<:gold:866137531422998558>", rgb(241, 196, 15)),
    DIAMOND("Diamond Division", 60000, Range.between(100, 199), 7000, ":large_blue_diamond:", rgb(52, 152, 219)),
    LEGEND("Legend Division", 200000, Range.between(200, 299), 40000, "", rgb(255, 234, 167)),
    GOD("God Division", 500000, Range.between(300, Integer.MAX_VALUE), 100000, "", rgb(45, 52, 54))
    // max = 3500
}


class LevelingSystemListener(private val jda: JDA, private val systems: Array<LevelingSystem>) : ListenerAdapter() {
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        // todo add spam checks
        if (!event.member!!.user.isBot)
            systems.forEach { lvl -> lvl.acceptMessage(event.message, true) }
    }

    init {
        jda.addEventListener(this)
    }
}

class LevelingSystem(val levelCaches: ArrayList<LevelCache>) {


    /**
     * Accepts the message into the system. The user's level cache will be
     * modified to update the data because the user has sent a message.
     *
     * @param message The message to accept
     * @param award Whether to send a congratulations message when the member
     *              progresses
     */
    fun acceptMessage(message: Message, award: Boolean) {
        val cache = levelCache(message.member!!.user)
        val new = cache.levelUnit + message.contentRaw.length
        val lvl = predictLevel(new)
        val div = predictDivision(lvl)
        println("Old = ${cache.levelUnit}, ${cache.level} || New = ${new}, $lvl")
        if (lvl > cache.level && award) {
            // new level

            var newEggs = 0
            for (i in cache.level until lvl) {
                newEggs += div.rewardEggs
            }
            newEggs = ((ThreadLocalRandom.current().nextInt(5, 16) / 10.0) * newEggs.toDouble()).toInt()
            currencySystem!!.addEggs(message.author, newEggs)
            if (div != cache.division) {
                // new division has been reached

                val embed = EmbedBuilder()
                        .setTitle("${div.icon} You have leveled up to ${div.division.toLowerCase()} ${div.icon} ")
                        .setColor(div.color)
                        .setDescription("${div.rewardEggs} will be given on level up!")
                        .setTimestamp(Instant.now())
                        .addField("Percentile",
                                "N/A", true)
                        .addField("Level", lvl.toString(), true)
                        .addField("Total eggs", currencySystem!!.getEggs(message.author).toString(), true) // todo
                        .build()
                message.channel.sendMessage(embed).queue()
            } else {
                // same division
                message.channel.sendMessage("**Congratulations <@${message.member!!.id}>!** You have leveled up to level $lvl!\n" +
                        "+$newEggs eggs (level up)")
                        .queue()
            }
        }

        // modify cache
        cache.levelUnit = new
        cache.level = lvl
        cache.division = div
    }

    /**
     * Predicts the level based on exp
     *
     * @param levelUnit The level unit
     * @return The predicted level
     */
    fun predictLevel(levelUnit: Int): Int {
        var sum = 0
        var diff = 0
        var index = 0
        for (i in totalLevelUnitPerDivision.indices) {
            if (totalLevelUnitPerDivision[i] != Int.MAX_VALUE) sum += totalLevelUnitPerDivision[i]
            if (levelUnit <= sum || totalLevelUnitPerDivision[i] == Int.MAX_VALUE) {

                // Add the maximum points to the exp
                // the sum from the value.
                // max + exp gives the exp above the maximum values
                // the sum is for subtracting the previous values of max retrieved
                diff = if (totalLevelUnitPerDivision[i] != Int.MAX_VALUE) totalLevelUnitPerDivision[i] + levelUnit - sum
                       else levelUnit - sum
                index = i
                break
            }
        }
        val base = if (index == 0) 0 else maxLevels[index - 1] // base (aka min) level for the division
        val inc: Int = diff / levelUnitPerLevelInDivision[index] // The levels to increment depending on the rewarding of the messages
        return base + inc
    }

    /**
     * Predicts the division based on level
     *
     * @param level The level
     * @return The predicted division
     */
    fun predictDivision(level: Int) : Division {
        for (div in Division.values()) {
            if (div.range.satisfyRange(level))
                return div
        }
        return Division.WOOD
    }

    // finds or creates the level cache
    fun levelCache(user: User) : LevelCache {
        levelCaches.forEach { cache -> if (cache.user == user) return cache}
        // else create a new cache
        val cache = LevelCache(user, Division.DIRT, 0, 0)
        levelCaches.add(cache)
        return cache
    }

    /**
     * Retrieves the user by their level unit.
     *
     * @param unit The level unit
     * @return The users retrieved
     */
    fun getUsersByLevelUnit(unit: Int) : List<User> {
        val users = ArrayList<User>()
        levelCaches.forEach { cache -> if (cache.levelUnit == unit) users.add(cache.user)}
        return users
    }

    /**
     * Returns a sorted [List] of [User]s, where the
     * highest level unit is at the top and descends as
     * index increases.
     *
     * @return The sorted [List] of [User]s
     */
    fun sort() : List<User> {
        val caches = ArrayList<User>()
        val ints = ArrayList<Int>()

        levelCaches.forEach { cache -> ints.add(cache.levelUnit) }
        ints.sort()
        ints.reverse()

        ints.forEach { int -> caches.addAll(getUsersByLevelUnit(int)) }
        return caches
    }

    /**
     * This has a similar functionality to [sort]; however, only users in the
     * [guild] are included in the list.
     *
     * @param guild The guild
     * @return The sorted [List] of [User]s
     */
    fun sort(guild: Guild) : List<User> {
        val users = ArrayList<User>()
        val ints = ArrayList<Int>()

        levelCaches.forEach { cache -> if (guild.isMember(cache.user)) ints.add(cache.levelUnit) }
        ints.sort()
        ints.reverse()

        ints.forEach { int -> users.addAll(getUsersByLevelUnit(int)) }
        return users
    }

}

/**
 * Level cache for a member. This is to be used internally and it is not recommended
 * to alter with the values here unless the levels are adjusted.
 *
 * @param user The user
 * @param level The member's level
 * @param levelUnit The level unit
 */
class LevelCache(val user: User, var division: Division, var level: Int, var levelUnit: Int) {

    /**
     * Compares this level cache to the other based solely on
     * the level unit
     *
     * @param levelCache The level cache to compare to
     * @return 1 if this level unit is greater than the other's, 0 if equal, and 1 if
     * less than
     */
    fun compare(levelCache: LevelCache) : Int {
        return when {
            levelUnit > levelCache.levelUnit -> 1
            levelUnit == levelCache.levelUnit -> 0
            else -> -1
        }
    }

}

// calculate max levels
private fun maxLevels() : ArrayList<Int> {
    val array = ArrayList<Int>()
    Division.values().forEach {div ->
        array.add(div.range.to)
    }
    return array
}

// level unit per division
private fun totalLevelUnitPerDivision() : ArrayList<Int> {
    val array = ArrayList<Int>()
    Division.values().forEach { div ->
        run {
            array.add((div.range.to - div.range.from + 1) * div.levelUnit)
        }
    }
    return array
}

// level unit required to level in every division
private fun levelUnitPerLevelInDivision() : ArrayList<Int> {
    val array = ArrayList<Int>()
    Division.values().forEach { div -> array.add(div.levelUnit) }
    return array
}