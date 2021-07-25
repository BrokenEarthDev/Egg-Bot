package me.eggdev.eggbot

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import java.lang.Integer.max

var currencySystem: CurrencySystem? = null
    private set

fun initCurrencySystem() {
    currencySystem = CurrencySystem(HashMap())
}

/**
 * The currency system, which can be used to manipulate
 * eggs for a given user.
 *
 * @param userEggs The user eggs
 */
class CurrencySystem(val userEggs: HashMap<User, Int>) {

    init {
        userEggs.forEach { (k, v) -> userEggs[k] = max(0, v)}
    }

    /**
     * Sets the number of eggs to a given amount
     *
     * @param user The user
     * @param amount The amount (>= 0)
     * @return The set amount
     */
    fun setEggs(user: User, amount: Int) : Int{
        userEggs[user] = max(0, amount)
        return amount
    }

    /**
     * Adds the number of eggs to a given amount
     *
     * @param user The user
     * @param amount The number of eggs to add (>= 0)
     * @return The total amount
     */
    fun addEggs(user: User, amount: Int) : Int {
        return setEggs(user, getEggs(user) + max(0, amount))
    }

    /**
     * Removes the amount from the number of eggs
     *
     * @param user The user
     * @param amount The number of eggs to remove (>= 0)
     * @return The total amount
     */
    fun removeEggs(user: User, amount: Int) : Int {
        val new = getEggs(user) - max(amount, 0)
        return setEggs(user, max(new, 0))
    }

    /**
     * @param user The user
     * @return The number of eggs the user has
     */
    fun getEggs(user: User) : Int {
        if (!userEggs.containsKey(user)) {
            userEggs[user] = 0
            return 0
        }
        return userEggs.getOrDefault(user, 0)
    }

    /**
     * @param eggs The amount of eggs
     * @return The [List] of [User] who all have the same amount of eggs
     */
    fun getUsers(eggs: Int) : List<User> {
        val list = ArrayList<User>()
        userEggs.forEach { t, u -> if (u == eggs) list.add(t) }
        return list
    }

    /**
     * Sorts the [List] of [User]s depending on the amount of eggs
     * they have. The users will be sorted in descending order
     *
     * @return The [List] of [User]s
     */
    fun sort() : List<User> {
        val sorted = ArrayList<User>()
        val eggs = ArrayList<Int>()

        userEggs.forEach { (t, u) -> eggs.add(u) }

        eggs.sort()
        eggs.reverse()

        eggs.forEach { egg -> sorted.addAll(getUsers(egg)) }
        return sorted
    }

    /**
     * Similar functionality to [sort] however only users in the
     * guild are included in the [List]
     *
     * @param guild The guild
     * @return The [List] of [User]s
     */
    fun sort(guild: Guild) : List<User> {
        val sorted = ArrayList<User>()
        val eggs = ArrayList<Int>()

        userEggs.forEach { t, u -> if (guild.isMember(t)) eggs.add(u) }

        eggs.sort()
        eggs.reverse()

        eggs.forEach { egg -> sorted.addAll(getUsers(egg)) }
        return sorted
    }

}