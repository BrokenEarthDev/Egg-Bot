package me.eggdev.eggbot

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

val pets = arrayOf("EggDog`:dog:`", "EggCat`:cat:`", "EggChicken`:chicken:`")
var eventIsOn = false
var crackchance = 0.0
var pet_inventory: List<String> = arrayListOf()
val petrandom = ""

fun petappear() {
    val timetaken = ThreadLocalRandom.current().nextLong(2000, 10000)
    eventIsOn = true
    executorService.schedule({ run {

        val petrandom = pets[ThreadLocalRandom.current().nextInt(0, 3)]
        embedMessage("A wild " + petrandom + " has appeared! ", UFO_GREEN)
        if (petrandom == "EggDog`:dog:`") {
            crackchance = 0.1
        } else if (petrandom == "EggCat`:cat:`") {
            crackchance = 0.05
        } else {
            crackchance = 0.02
        }
        embedMessage("Use e!capture <number of eggs> to try and capture the pet!", UFO_GREEN)

    }
    }, timetaken, TimeUnit.MILLISECONDS)
}

private class PetMsgListener : ListenerAdapter() {
    override fun onGuildMessageReceived(e: GuildMessageReceivedEvent) {
        val random = ThreadLocalRandom.current().nextInt(0, 10)
        if (random == 0 || random == 1) {
            petappear()
        }
    }
}