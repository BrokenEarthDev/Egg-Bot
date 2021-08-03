package me.eggdev.eggbot.commands

import com.github.blad3mak3r.memes4j.Memes4J
import me.eggdev.eggbot.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import java.lang.StringBuilder
import java.time.Instant
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.roundToInt

// The gif images to show when a user executes e!lay
val eggLayLinks = arrayListOf(  "https://media.giphy.com/media/gTt2q509ZA4xZqxxKU/giphy.gif",
                                "https://media.giphy.com/media/YPswkidIAtWmmqgW39/giphy.gif",
                                "https://media.giphy.com/media/wfyX3EHlwnPlY3snZd/giphy.gif",
                                "https://media.giphy.com/media/4PUp2US6ExgLMwW4BX/giphy.gif",
                                "https://media.giphy.com/media/4PUp2US6ExgLMwW4BX/giphy.gif"    )

// The gif links to show when a user executes e!crack
val eggCrackLinks = arrayListOf( "https://media.giphy.com/media/3o7TKIH8zvG9UkW0dG/giphy.gif",
                                 "https://media.giphy.com/media/cZUo8CgKF5lfQTOYQP/giphy.gif",
                                 "https://media.giphy.com/media/jtoNgCggzD31xL81lP/giphy.gif",
                                 "https://media.giphy.com/media/H22cY1sTScIFm0yM1u/giphy.gif",
                                 "https://media.giphy.com/media/biwEneuqbSpM5HDiM2/giphy.gif",
                                 "https://media.giphy.com/media/axM1GXWGnmfy8/giphy.gif"        )

// Possible magic 8 ball answers
// yes - 0, 4, 8, 12, 16
// yes - 1, 5, 9, 13, 17
// uncertain - 2, 6, 10, 14, 18
// no - 3, 7, 11, 15, 19
val _8BallAns = arrayListOf( "It is certain", "As I see it, yes", "Reply hazy, try again", "Don't count on it",
                             "It is decidedly so", "Most likely", "Ask again later", "My reply is no",
                             "Without a doubt", "Outlook good", "Better not tell you now", "My sources say no",
                             "Yes definitely", "Yes", "Cannot predict now", "Outlook not so good",
                             "You may rely on it", "Signs point to yes", "Concentrate and ask again", "Very doubtful")

// The probability a chicken will be born after cracking an
// egg
val chickenProbability = 0.05

/**
 * Sends memes :)
 */
@CommandName("meme")
@CommandHelp(help = "Sends random memes fetched from reddit by using `e!meme`", usage = "`e!meme`")
@SetCategory(CommandCategory.ENTERTAINMENT)
class MemeCommand : EggCommand() {
    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        val req = Memes4J.getRandomMeme()
        req.queue({meme ->
            run {
                val embed = EmbedBuilder()
                        .setAuthor(meme.title, meme.link)//meme.title)
                        .setDescription(":thumbsup: - ${meme.ups} **|** :thumbsdown: - ${meme.downs}" +
                                        "\n" +
                                        ":speech_left: - ${meme.comments}")
                        .setColor(FRENCH_SKY_BLUE)
                        .setImage(meme.image)
                        .setFooter("Fetched from r/${meme.subreddit}")
                        .build()
                message.channel.sendMessage(embed).queue()
            }
        }, { error ->
            run {
                message.channel.sendMessage(embedMessage(":x: **Can't fetch meme!** An error has occurred. Please " +
                        "try the command again after a few minutes.", RED_BAD)).queue()
            }
        })
        return true
    }

}

@CommandName("lay")
@CommandHelp(help = "Lays eggs :). A certain amount of eggs will be gained. You can only execute this command " +
        "thrice per day", usage = "`e!lay`")
@SetCategory(CommandCategory.ENTERTAINMENT)
class LayCommand : EggCommand() {

    val execs = HashMap<User, ArrayList<Date>>()
    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        val now = Date()
        val date = execs.getOrDefault(sender.user, ArrayList())

        // latest
        if (date.size == 3 && date[date.size - 1].time - now.time < 1000 * 3600 * 24) {
            message.channel.sendMessage(embedMessage(":clock: You can only lay 3 times per day. I don't know why, " +
                    "but who would force their chicken?", RED_BAD)).queue()
            return true
        } else if (date.size == 3) date.clear()

        date.add(now)
        execs[sender.user] = date

        val random = ThreadLocalRandom.current().nextInt(0, 101)

        currencySystem!!.addEggs(sender.user, random)
        val embed = EmbedBuilder()
                .setTitle("You have gained $random :egg: ")
                .setDescription("After laying on your nest, you have $random new eggs")
                .setImage(eggLayLinks[ThreadLocalRandom.current().nextInt(0, eggLayLinks.size)])
                .setColor(UFO_GREEN)
                .setFooter("You can only execute this three times per day")
                .build()
        message.channel.sendMessage(embed).queue()
        return true
    }

}

@CommandName("crack")
@CommandHelp(help = "To crack one egg, `e!crack` or `e!crack number`. For more than 1, you can enter the " +
        "number in the first argument. Cracking an egg gives you a chance to give birth to a baby chicken. " +
        "This will allow you to produce more eggs", usage = "`e!crack` or `e!crack number`")
@RequireArguments(max = 1)
@SetCategory(CommandCategory.ENTERTAINMENT)
class CrackCommand : EggCommand() {

    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        val number = if (args.isEmpty()) 1 else args[0].toIntOrNull()
        if (number == null) {
            // can't parse integer
            message.channel.sendMessage(embedMessage(":person_shrugging: I can't understand by " +
                                        "what you mean by `${args[0]}`, dummy. Please make sure only numbers are " +
                                        "entered and that it isn't too large.", RED_BAD)).queue()
            return true
        }
        val eggs = currencySystem!!.getEggs(sender.user)
        if (number > eggs) {
            message.channel.sendMessage(embedMessage("<a:upset_egg:866771089479565342> I'm pretty sure that " +
                    "the amount of eggs you have is less than the amount of eggs you wanted to crack.", RED_BAD)).queue()
            return true
        }
        if (number > 200) {
            message.channel.sendMessage(embedMessage("<a:upset_egg:866771089479565342> Be cautious on your egg " +
                    "supply. You can't crack more than 200 eggs at one time!", RED_BAD)).queue()
            return true
        }

        // finally crack them
        currencySystem!!.removeEggs(sender.user, number) // rem eggs
        val embed = EmbedBuilder()
                .setTitle(":crack: Cracking $number :egg: ...")
                .setDescription("There is a ${chickenProbability * 100}% chance to get a chicken")
                .setImage(eggCrackLinks[ThreadLocalRandom.current().nextInt(0, eggCrackLinks.size)])
                .setColor(CLEAR_CHILL)
                .build()

        message.channel.sendMessage(embed).queue{ msg ->
            run {
                // eval probability
                val random = ThreadLocalRandom.current().nextDouble()
                if (random <= chickenProbability) {
                    // chicken :D
                    val m = embed(":partying_face: ${sender.user.name} has hatched a :chicken:",
                                                "... at the expense of $number eggs",
                                                null, UFO_GREEN, false)
                    msg.channel.sendMessage("<@!${sender.idLong}>").embed(m).queueAfter(5, TimeUnit.SECONDS)
                    // todo add chicken collection
                } else {
                    // no chicken :(
                    val m = embedTitle("${sender.user.name} just... wasted $number eggs",
                                        RED_GOOD)
                    msg.channel.sendMessage("<@!${sender.idLong}>").embed(m).queueAfter(5, TimeUnit.SECONDS)
                }
            }
        }
        return true
    }

}

@CommandName("8ball")
@CommandHelp(help = "Uses :magic: to answer your yes-no question",
            usage = "`e!8ball question`")
@RequireArguments(min = 1)
@SetCategory(CommandCategory.ENTERTAINMENT)
class Magic8BallCommand : EggCommand() {
    /**
     * Executes the command
     *
     * @param sender The member who executed the command
     * @param message The command message
     * @param args The arguments of the command, excluding the command itself
     * @return Whether the execution is considered to be 'successful'
     */
    override fun executeCommand(sender: Member, message: Message, args: List<String>): Boolean {
        val questionBuilder = StringBuilder()
        for (i in args.indices)
            questionBuilder.append(args[i]).append(" ")
        var question = questionBuilder.trim().toString()
        if (question.length > 200) {
            // long argument
            message.channel.sendMessage(embedMessage("**Long argument.** Egg folks- it's a bit obvious that this " +
                    "is too long as a question. 200 is the max character length", RED_BAD)).queue()
            return true
        }
        if (!question.endsWith("?")) question += "?"
        val resp = ThreadLocalRandom.current().nextInt(0, _8BallAns.size)
        val responseString = _8BallAns[resp]
        val embed = EmbedBuilder()
                .setTitle(":8ball: $question")
                .setTimestamp(Instant.now())
                .setFooter("Requested by ${sender.user.asTag}")
        if (resp % 4 == 0 || (resp - 1) % 4 == 0) {
            // yes
            embed.setDescription("<:yes:866840091066630175> $responseString")
                    .setColor(UFO_GREEN)
        } else if ((resp - 2) % 4 == 0) {
            // uncertain
            embed.setDescription(":person_shrugging: $responseString")
                    .setColor(FLIRTATIOUS)
        } else if ((resp - 3) % 4 == 0) {
            // no
            embed.setDescription("<:no:866841279249645568> $responseString")
                    .setColor(RED_GOOD)
        } else {
            // treat uncertain
            embed.setDescription("<:no:866841279249645568> $responseString")
                    .setColor(RED_GOOD)
        }
        message.channel.sendMessage(embed.build()).queue()
        return true
    }

}

@CommandName("steal")
@CommandHelp(help = "Steals a certain number of Eggs from the targeted user", "e!steal @target")
@RequireArguments(min = 2)
@SetCategory(CommandCategory.ENTERTAINMENT)

class StealCommand : EggCommand() {

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
                    message.reply(embedMessage("❌ You can't steal Eggs from this user because " +
                            "the user isn't in the server. If this was an error, please try again.", RED_BAD)).queue()
                } else {
                    val member: Member = res.get()
                    var eggs = currencySystem!!.getEggs(member.user)
                    var amount_stolen = ThreadLocalRandom.current().nextInt(-1, 11)
                    if (amount_stolen == -1) {+
                        currencySystem!!.removeEggs(sender.user, (0.1 * eggs).roundToInt())
                        message.reply(embedMessage("You got caught and had to pay " +
                                (0.1 * eggs) + " to get bailed out.", RED_BAD)).queue()
                    else if (amount_stolen == 0) {
                        message.reply(embedMessage("You couldn't steal anything haha", RED_BAD)).queue()
                    } else {
                        var stolen = amount_stolen * eggs
                        currencySystem!!.removeEggs(member.user, stolen)
                        currencySystem!!.addEggs(sender.user, stolen)
                        message.reply(embedMessage("You stole: " +
                                stolen + " Eggs from " + member.user + ".", UFO_GREEN)).queue()
                    }
                }
            }
        return true
    }
}
