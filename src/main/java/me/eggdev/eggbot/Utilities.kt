package me.eggdev.eggbot

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.awt.Color
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import java.util.function.BiConsumer
import kotlin.collections.HashMap

/**
 * Creates an embed message without having to create an EmbedBuilder instance.
 * This doesn't include icon, thumbnail, and author.
 *
 * @param title The embed title
 * @param description The embed description
 * @param footer The embed footer
 * @param color The embed color
 * @param fields Fields in the embeds
 */
fun embed(
        title: String?, description: String?, footer: String?,
        color: Color, timestamp: Boolean, vararg fields: MessageEmbed.Field,
) : MessageEmbed {
    val builder = EmbedBuilder()
    for (field in fields)
        builder.addField(field)
    if (timestamp) builder.setTimestamp(Instant.now())
    return builder.setTitle(title).setDescription(description).setFooter(footer).setColor(color).build()
}

/**
 * Creates an embed message
 *
 * @param message The message
 * @param color The color
 */
fun embedMessage(message: String, color: Color) : MessageEmbed {
    return EmbedBuilder().setDescription(message).setColor(color).build()
}

/**
 * Creates an embed title
 *
 * @param message The message
 * @param color The color
 */
fun embedTitle(message: String, color: Color) : MessageEmbed {
    return EmbedBuilder().setTitle(message).setColor(color).build()
}

/**
 * Converts tag to an id string. The third to (text.length - 1)th character
 * should always contain the full string
 *
 * @param text The text
 */
fun fromTag(text: String) : String {
    return text.substring(3, text.length - 1)
}

private var durationsA = listOf("seconds", "minutes", "hours", "days")
private var durationsB = listOf("sec", "min", "hr", "day")
private var durationsC = listOf("s", "m", "h", "d")
private var durations = listOf(durationsA, durationsB, durationsC)
private var units = listOf(Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR, Calendar.DAY_OF_YEAR)

/**
 * Converts a duration to a date. Durations are numbers ending with a unit which
 * implies the time unit for the duration. Examples include 180m, which translates to
 * 180 minutes. The same result will be yielded if the duration string is 180minutes or
 * 180 min.
 * Possible duration identifiers are - seconds, minutes, hours, days, sec, min, hr, day, s, m, h, d.
 * Only seconds, minutes, hours, and days are allowed for time unites. The current time will be
 * added to the duration to yield a new date.
 *
 * @param text The duration string, containing number and duration identifier without any black space
 * @return The date added to the duration implied, or null if such duration can't be parsed
 */
fun fromDuration(text: String) : Date? {
    for (list in durations) {
        for (i in list.indices) {
            if (text.endsWith(list[i]) && !(list[i].length == 1 && list[i][list[i].length - 1].isDigit())) {
                val calendar = Calendar.getInstance()
                // attempt parse int
                val numStr = text.substring(0, text.length - list[i].length)
                val num = numStr.toIntOrNull()
                if (num == null) return null
                else calendar.add(units[i], num)
                return calendar.time
            }
        }
    }
    return null
}

/**
 * Converts duration to a more user-friendly duration text. For example, 180m becomes 180 minutes.
 * This is useful for sending it to users.
 *
 * @param text The duration
 * @return Returns a user friendly duration or null if it can't be parsed
 */
fun userFriendlyDuration(text: String) : String? {
    for (list in durations) {
        for (i in list.indices) {
            if (text.endsWith(list[i]) && !(list[i].length == 1 && list[i][list[i].length - 1].isDigit())) {
                // parse number
                val numStr = text.substring(0, text.length - list[i].length)
                val num = numStr.toIntOrNull()
                return if (num == null) null
                else "".plus(num).plus(" ${durationsA[i]}")
            }
        }
    }
    return null
}

/**
 * Checks whether the highest role of the member is greater than the
 * target member.
 *
 * @param member The member where their position is compared to the other
 * @param target The target member
 * @return True if the member highest role is greater than the target
 * member
 */
fun roleGreater(member: Member, target: Member) : Boolean {
    val posA = if (member.roles.size == 0) -1 else member.roles[0].position
    val posB = if (target.roles.size == 0) -1 else target.roles[0].position
    return posA > posB
}

/**
 * Simple rgb function returning a color.
 *
 * @param a Red
 * @param b Blue
 * @param c Green
 * @return The color represented by the rgb
 */
fun rgb(a: Int, b: Int, c: Int) : Color {
    return Color(a, b, c)
}

// COLORS ---

val PINK = rgb(239, 87, 119)
val RED_GOOD = rgb(245, 59, 87)
val RED_BAD = rgb(255, 94, 87)
val RED_V_BAD = rgb(194, 54, 22)
val GREEN = rgb(123, 237, 159)
val UFO_GREEN = rgb(46, 213, 115)
val FRENCH_SKY_BLUE = rgb(112, 161, 255)
val CLEAR_CHILL = rgb(30, 144, 255)

val FLIRTATIOUS = rgb(254, 211, 48)

// -----------

/**
 * Formats the date into a readable string
 *
 * @param date The date object
 * @param subDay Whether to display seconds, minutes, and hour
 * @return The formatted date
 */
fun formatDate(date: Date, subDay: Boolean) : String {
    val tz = TimeZone.getTimeZone("GMT")
    val format = SimpleDateFormat("dd/MM/yy")
    format.timeZone = tz
    var formatted = format.format(date)
    if (subDay) {
        val formatB = SimpleDateFormat("HH:mm:ss")
        formatB.timeZone = tz
        formatted += " at " + formatB.format(date)
    }

    return formatted
}

// map of await reactions
val awaited = HashMap<Message, BiConsumer<Member?, MessageReaction.ReactionEmote>>()
var awaitReactionRegistered = false // whether the await reaction listener is registered or not

/**
 * A listener for reactions
 */
private class ReactionListener : ListenerAdapter() {
    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        awaited.forEach { (msg, cons) ->
            run {
                if (msg.id == event.messageId) {
                    cons.accept(event.member, event.reactionEmote)
                    return@forEach
                }
            }
        }
    }
}

/**
 * Registers the await reaction
 *
 * @param message The message
 * @param then Biconsumer that fires after awaiting reaction
 */
fun awaitReaction(message: Message, then: BiConsumer<Member?, MessageReaction.ReactionEmote>) {
    awaited[message] = then
    if (!awaitReactionRegistered) {
        awaitReactionRegistered = true
        jda.addEventListener(ReactionListener())
    }
}

/**
 * Unregisters the await reaction
 *
 * @param message The message
 */
fun unregisterAwaitReaction(message: Message) {
    awaited.remove(message)
}

/**
 * Clears the await reactions
 */
fun clearAwaitReactions() {
    awaited.clear()
}

/**
 * Converts an array containing <out [T]> to an array of [T].
 *
 * @param outArray The array containing <out [T]>
 * @return An array of [T]
 */
inline fun <reified T> fromOutArray(outArray: Array<out T>) : Array<T> {
    return Array(outArray.size) { index -> return@Array outArray[index] }
}

/**
 * Parses a boolean from a user-inputted string argument. Yes and true return true.
 * No and false return false. If a boolean couldn't be parsed, null will be returned
 *
 * @param string The string that contains a boolean
 * @return The parsed boolean
 */
fun parseBoolean(string: String) : Boolean? {
    if (string.equals("true", true) || string.equals("yes", true))
        return true
    else if (string.equals("false", true) || string.equals("no", true))
        return false
    return null
}

/**
 * Simple function to combine strings from an array (with separator " ") and
 * trims it.
 *
 * @param array The array where the strings will be combined
 * @param from The beginning index of the array where the strings will be
 *             combined. array.size() > from >= 0
 * @return The combined strings
 */
fun combineStrings(array: List<String>, from: Int) : String {
    return array.subList(from, array.size)
            .joinToString(" ", "", "", -1, "...")
            { str -> return@joinToString str }.trimEnd()
}

/**
 * Checks whether any string in [list] is contained in the [list]
 *
 * @param list The list
 * @param string The string
 * @return The first found string from the [list], or null if none is found
 */
fun containsAny(list: List<String>, string: String) : String? {
    list.forEach { str -> if (string.contains(str)) return str }
    return null
}

/**
 * Creates a list of the elements in the [string]. The list components will
 * be indented by two lines and will be followed by a dash and a space.
 * Each component will have it's own line.
 *
 * @param string The list components
 * @return The returned user-friendly list
 */
fun createBulletedListOf(vararg string: String) : String {
    val builder = StringBuilder()
    for (str in string)
        builder.append("  - $str").append("\n")
    return builder.toString().trimEnd()
}

fun limitString(string: String, maxChar: Int, truncateStr: String) : String {
    return if (string.length > maxChar) {
        string.dropLast(truncateStr.length) + truncateStr
    } else string
}

/**
 * Allows for creation of a range
 */
class Range<T : Number> {
    /**
     * @return Min value
     */
    // From & to values
    lateinit var from: T
        private set

    /**
     * @return Max value
     */
    lateinit var to: T
        private set

    /**
     * Checks whether the number passed in satisfies the range
     *
     * @param num The number
     * @return Whether it satisfies the range
     */
    fun satisfyRange(num: T): Boolean {
        return if (from is Long || from is Int || from is Short || from is Byte) num!!.toLong() <= to!!.toLong() && num.toLong() >= from!!.toLong() else (from is Double || from is Float) && num!!.toDouble() <= to!!.toDouble() && num.toDouble() >= from!!.toDouble()
    }

    companion object {
        /**
         * Creates a range between two values
         *
         * @param from The minimum value
         * @param to The maximum value
         * @param <N> The type, subclass of [Number]
         * @return a new range
        </N> */
        fun <N : Number> between(from: N, to: N): Range<N> {
            val range: Range<N> = Range()
            range.from = from
            range.to = to
            return range
        }
    }
}

// CUSTOM EMOJIS

private val emojiGuild = jda.getGuildById(865256117705375754L) // Server where custom emojis
                                                                   // are stored

fun emotes(name: String, ignoreCase: Boolean) : List<Emote> {
    return emojiGuild!!.getEmotesByName(name, ignoreCase)
}