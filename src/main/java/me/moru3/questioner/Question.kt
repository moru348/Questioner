package me.moru3.questioner

import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.ChatColor
import org.bukkit.Sound
import org.bukkit.entity.Player

class Question(private val id: String, private val content: String, private val type: QuestionType) {

    fun save(config: Config): Question {
        config.config().set("${id}.content", content)
        config.config().set("${id}.type", type.toString())
        config.saveConfig()
        return this
    }

    fun send(player: Player, language: Config? = null) {
        val result = TextComponent(language?.getMessage("questionnaire_header")?:"§2§l+*********************アンケート*********************+")
        result.addExtra((language?.getMessage("content")?:"\n§9§l➣ §f%content%\n\n").replace("%content%", content))
        if(type==QuestionType.DECISION) {
            val yes = TextComponent(language?.getMessage("yes")?:"§a[はい]").apply {
                clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/question answer $id 1")
                hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("${(language?.getMessage("vote")?:"%select% §fに投票する").replace("%select%", language?.getMessage("yes")?:"§a[はい]")}\n${language?.getMessage("clicktovote")?:"§7クリックすると投票できます。"}").create())
            }
            val no = TextComponent(language?.getMessage("no")?:"§c[いいえ]").apply {
                clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/question answer $id 0")
                hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("${(language?.getMessage("vote")?:"%select% §fに投票する").replace("%select%", language?.getMessage("no")?:"§c[いいえ]")}\n${language?.getMessage("clicktovote")?:"§7クリックすると投票できます。"}").create())
            }
            result.apply {
                addExtra("      ")
                addExtra(yes)
                addExtra("      ")
                addExtra(no)
            }
        } else {
            val five = TextComponent(language?.getMessage("five")?:"§a§l[5]").apply {
                clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/question answer $id 5")
                hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("${(language?.getMessage("vote")?:"%select% §fに投票する").replace("%select%", language?.getMessage("five")?:"§a§l[5]")}\n${language?.getMessage("clicktovote")?:"§7クリックすると投票できます。"}").create())
            }
            val four = TextComponent(language?.getMessage("four")?:"§a§l[4]").apply {
                hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("${(language?.getMessage("vote")?:"%select% §fに投票する").replace("%select%", language?.getMessage("four")?:"§a§l[4]")}\n${language?.getMessage("clicktovote")?:"§7クリックすると投票できます。"}").create())
                clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/question answer $id 4")
            }

            val three = TextComponent(language?.getMessage("three")?:"§a§l[3]").apply {
                hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("${(language?.getMessage("vote")?:"%select% §fに投票する").replace("%select%", language?.getMessage("three")?:"§e§l[3]")}\n${language?.getMessage("clicktovote")?:"§7クリックすると投票できます。"}").create())
                clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/question answer $id 3")
            }
            val two = TextComponent(language?.getMessage("two")?:"§a§l[2]").apply {
                hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("${(language?.getMessage("vote")?:"%select% §fに投票する").replace("%select%", language?.getMessage("two")?:"§e§l[2]")}\n${language?.getMessage("clicktovote")?:"§7クリックすると投票できます。"}").create())
                clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/question answer $id 2")
            }
            val one = TextComponent(language?.getMessage("one")?:"§a§l[1]").apply {
                hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder("${(language?.getMessage("vote")?:"%select% §fに投票する").replace("%select%", language?.getMessage("one")?:"§c§l[1]")}\n${language?.getMessage("clicktovote")?:"§7クリックすると投票できます。"}").create())
                clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/question answer $id 1")
            }
            result.apply {
                addExtra(" ")
                addExtra(five)
                addExtra(" ")
                addExtra(four)
                addExtra(" ")
                addExtra(three)
                addExtra(" ")
                addExtra(two)
                addExtra(" ")
                addExtra(one)
                addExtra(one)
                addExtra("\n")
                addExtra(language?.getMessage("questionnaire_footer")?:"§2§l+*****************************************************+")
            }
        }
        player.spigot().sendMessage(ChatMessageType.CHAT, result)
    }

    fun addAnswer(answer: Int, player: Player, answerConfig: Config, language: Config? = null) {
        if(answerConfig.config().getStringList("${id}.players").contains(player.uniqueId.toString())) {
            player.sendMessage(language?.getMessage("alreadyAnswer")?:"§cすでにこのアンケートは回答済みです。ご協力ありがとうございました！")
            return
        }
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 1F)
        if(type==QuestionType.DECISION) {
            if(answer>1||answer<0) { return }
            if(answer==0) {
                answerConfig.config().set("${id}.no", answerConfig.config().getInt("${id}.no") + 1)
            } else {
                answerConfig.config().set("${id}.yes", answerConfig.config().getInt("${id}.yes") + 1)
            }
            val answers = answerConfig.config().getStringList("${id}.players")
            answers.add(player.uniqueId.toString())
            answerConfig.config().set("${id}.players", answers)
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', language?.config()?.getString("answer_complete")?:"&aアンケートに回答しました。ご協力ありがとうございます！&f回答&8: &c%answer%").replace("%answer%", if(answer==1) "はい" else "いいえ"))
        } else {
            if(answer>5||answer<1) { return }
            answerConfig.config().set("${id}.${answer}", answerConfig.config().getInt("${id}.${answer}") + 1)
            val answers = answerConfig.config().getStringList("${id}.players")
            answers.add(player.uniqueId.toString())
            answerConfig.config().set("${id}.players", answers)
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', language?.config()?.getString("answer_complete")?:"&aアンケートに回答しました。ご協力ありがとうございます！&f回答&8: &c%answer%").replace("%answer%", answer.toString()))
        }
        answerConfig.saveConfig()
    }
    fun getResult(answerConfig: Config): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        if(type==QuestionType.DECISION) {
            result["yes"] = answerConfig.config().getInt("${id}.yes")
            result["no"] = answerConfig.config().getInt("${id}.no")
        } else {
            result["1"] = answerConfig.config().getInt("${id}.1")
            result["2"] = answerConfig.config().getInt("${id}.2")
            result["3"] = answerConfig.config().getInt("${id}.3")
            result["4"] = answerConfig.config().getInt("${id}.4")
            result["5"] = answerConfig.config().getInt("${id}.5")
        }
        return result
    }
}