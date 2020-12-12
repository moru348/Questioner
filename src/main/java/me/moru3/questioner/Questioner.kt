package me.moru3.questioner

import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.regex.Pattern
import kotlin.math.round

class Questioner : JavaPlugin() {
    private var questionConfig = Config(this, "questions.yml")
    private var languagesConfig = Config(this, "languages.yml")
    private var answersConfig = Config(this, "answers.yml")
    private var id_regex = Pattern.compile("^[a-zA-Z0-9-]+\$").toRegex()
    private var questions = mutableMapOf<String, Question>()

    override fun onEnable() {
        questionConfig.saveDefaultConfig()
        answersConfig.saveDefaultConfig()
        languagesConfig.saveDefaultConfig()
        questionConfig.config().getKeys(false).forEach{s -> getQuestion(s)?.run{ questions[s] = this } }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if(sender !is Player) {sender.sendMessage("§cこのプラグインのコマンドはすべてプレイヤーからのみ実行できます。");return true}
        if(command.name!="question") return true
        if(args.isEmpty()) {
            sender.sendMessage("§cパラメータが指定されていません。")
            return true
        }
        when(args[0]) {
            "create" -> {
                if(!sender.hasPermission("questioner.create")) {sender.sendMessage(languagesConfig.getMessage("nopermission")?:"&cこのコマンドを実行する権限がありません。");return true}
                if(args.size!=4) {
                    sender.sendMessage(languagesConfig.getMessage("how_to_use_create")?:"使用方法: /question create (id) (質問内容) (タイプ: number, decision)")
                    return true
                }
                val id = args[1].run { if(!this.matches(id_regex)) { sender.sendMessage("IDは半角英数字、ハイフンのみ使用できます。"); return true; }; this }
                if(questionConfig.config().getKeys(false).contains(id)) {sender.sendMessage(languagesConfig.getMessage("alreadyExists")?:"§cすでにこのIDのアンケートが存在しています。");return true}
                val content = args[2]
                val type = args[3].questionType()?: kotlin.run{sender.sendMessage("タイプは number もしくは　decision のみ指定できます。");return true}
                questions[id] = Question(id, content, type).save(questionConfig)
                sender.sendMessage(languagesConfig.getMessage("created_question")?:"§aアンケートを作成しました！")
                return true
            }
            "reload" -> {
                if(!sender.hasPermission("questioner.reload")) {sender.sendMessage(languagesConfig.getMessage("nopermission")?:"&cこのコマンドを実行する権限がありません。");return true}
                languagesConfig.reloadConfig()
                answersConfig.reloadConfig()
                questionConfig.reloadConfig()
                sender.sendMessage(languagesConfig.getMessage("reload_config")?:"&aconfigのreloadが完了しました。")
                return true
            }
            "answer" -> {
                if(!sender.hasPermission("questioner.answer")) {sender.sendMessage(languagesConfig.getMessage("nopermission")?:"&cこのコマンドを実行する権限がありません。");return true}
                if(args.size!=3) {
                    sender.sendMessage(languagesConfig.getMessage("how_to_use_answer")?:"使用方法: /question answer (id) (回答)")
                    return true
                }
                if(args[2].toIntOrNull()==null) return true
                if(!questions.keys.contains(args[1])) return true
                questions[args[1]]?.addAnswer(args[2].toIntOrNull()!!, sender, answersConfig, languagesConfig)?: kotlin.run { sender.sendMessage(languagesConfig.getMessage("unknown_error")?:"§cエラーにより投票に失敗しました。詳細はコンソールをご確認ください。") }
                return true
            }
            "sendall" -> {
                if(!sender.hasPermission("questioner.create")) {sender.sendMessage(languagesConfig.getMessage("nopermission")?:"&cこのコマンドを実行する権限がありません。");return true}
                if(args.size!=2) {
                    sender.sendMessage(languagesConfig.getMessage("how_to_use_sendall")?:"使用方法: /question sendall [ID]")
                    return true
                }
                if(!questions.keys.contains(args[1])) {sender.sendMessage(languagesConfig.getMessage("not_found_questionnaire")?:"§c該当するアンケートが見つかりませんでした。");return true}
                sender.sendMessage((languagesConfig.getMessage("sendall")?:"§a全員に %question% を送信しました。").replace("%question%", args[1]))
                Bukkit.getOnlinePlayers().forEach { player -> run { questions[args[1]]?.send(player, languagesConfig)} }
                return true
            }
            "info" -> {
                if(!sender.hasPermission("questioner.info")) {sender.sendMessage(languagesConfig.getMessage("nopermission")?:"&cこのコマンドを実行する権限がありません。");return true}
                if(args.size!=2) {
                    sender.sendMessage(languagesConfig.getMessage("how_to_use_info")?:"使用方法: /question info [ID]")
                    return true
                }
                if(!questions.keys.contains(args[1])) {sender.sendMessage(languagesConfig.getMessage("not_found_questionnaire")?:"§c該当するアンケートが見つかりませんでした。");return true}
                val result = TextComponent(languagesConfig.getMessage("questionnaire_info")?:"§2§l+*******************アンケート詳細*******************+")
                if(questionConfig.config().getString("${args[1]}.type")=="number") {
                    // NUM
                    val res = questions[args[1]]?.getResult(answersConfig)
                    val one = res?.get("1")?:-1
                    val two = res?.get("2")?:-1
                    val three = res?.get("3")?:-1
                    val four = res?.get("4")?:-1
                    val five = res?.get("5")?:-1
                    var allcount = 0.0
                    res?.forEach { i -> allcount += i.value.toDouble() }
                    val p = fun(x: Int) = round(if(x!=0) (x / allcount) * 100 else 0.0)
                    result.apply {
                        addExtra("\n1: ${p(one)}% (${one}票)")
                        addExtra("\n3: ${p(two)}% (${three}票)")
                        addExtra("\n4: ${p(three)}% (${four}票)")
                        addExtra("\n4: ${p(four)}% (${four}票)")
                        addExtra("\n5: ${p(five)}% (${five}票)")
                    }

                } else {
                    // DEC
                    val res = questions[args[1]]?.getResult(answersConfig)
                    val yes = res?.get("yes")?:-1
                    val no = res?.get("no")?:-1
                    var allcount = 0.0
                    res?.forEach { i -> allcount += i.value.toDouble() }
                    val p = fun(x: Int) = round(if(x!=0) (x / allcount) * 100 else 0.0)
                    result.apply {
                        addExtra("\nはい: ${round(if(yes!=0) (yes / allcount) * 100 else 0.0)}% (${yes}票)")
                        addExtra("\nいいえ: ${round(if(no!=0) (no / allcount) * 100 else 0.0)}% (${no}票)")
                    }
                }
                sender.spigot().sendMessage(ChatMessageType.CHAT, result)
                return true
            }
            "delete" -> {
                if(!sender.hasPermission("questioner.delete")) {sender.sendMessage(languagesConfig.getMessage("nopermission")?:"&cこのコマンドを実行する権限がありません。");return true}
                if(args.size!=2) {
                    sender.sendMessage(languagesConfig.getMessage("how_to_use_delete")?:"使用方法: /question delete [ID]")
                    return true
                }
                if(!questions.keys.contains(args[1])) {sender.sendMessage(languagesConfig.getMessage("not_found_questionnaire")?:"§c該当するアンケートが見つかりませんでした。");return true}
                questionConfig.config().set(args[1], null)
                answersConfig.config().set(args[1], null)
                questionConfig.saveConfig()
                answersConfig.saveConfig()
                sender.sendMessage(languagesConfig.getMessage("delete_questionnaire")?:"§aアンケートを削除しました。")
                return true
            }
            else -> {
                sender.sendMessage(languagesConfig.getMessage("unknown_command")?:"§c不明なコマンドです。")
                return true
            }
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String> {
        val argOne = mutableListOf<String>()
        if(sender.hasPermission("questioner.create")) { argOne.add("create") }
        if(sender.hasPermission("questioner.sendall")) { argOne.add("sendall") }
        if(sender.hasPermission("questioner.info")) { argOne.add("info") }
        if(sender.hasPermission("questioner.delete")) { argOne.add("delete") }
        if(sender.hasPermission("questioner.reload")) { argOne.add("reload") }
        if(args.isEmpty()) {
            return argOne
        } else if(args.size==1) {
            val tab = mutableListOf<String>()
            for(i in argOne) {
                if(i.startsWith(args[0])) {
                    tab.add(i)
                }
            }
            return tab
        } else if(args.size==2) {
            when {
                args[0]=="create" -> {
                    return mutableListOf()
                }
                args[0]=="sendall" -> {
                    if(!sender.hasPermission("questioner.sendall")) {
                        return mutableListOf()
                    }
                    val tab = mutableListOf<String>()
                    for(i in questionConfig.config().getKeys(false).toMutableList()) {
                        if(i.startsWith(args[1])) {
                            tab.add(i)
                        }
                    }
                    return tab
                }
                args[0]=="info" -> {
                    if(!sender.hasPermission("questioner.info")) {
                        return mutableListOf()
                    }
                    val tab = mutableListOf<String>()
                    for(i in questionConfig.config().getKeys(false).toMutableList()) {
                        if(i.startsWith(args[1])) {
                            tab.add(i)
                        }
                    }
                    return tab
                }
                args[0]=="delete" -> {
                    if(!sender.hasPermission("questioner.delete")) {
                        return mutableListOf()
                    }
                    val tab = mutableListOf<String>()
                    for(i in questionConfig.config().getKeys(false).toMutableList()) {
                        if(i.startsWith(args[1])) {
                            tab.add(i)
                        }
                    }
                    return tab
                }
            }
        } else if (args.size==4) {
            if(args[0]=="create") {
                if(!sender.hasPermission("questioner.create")) {
                    return mutableListOf()
                }
                val tab = mutableListOf<String>()
                for(i in QuestionType.values()) {
                    if(i.toString().startsWith(args[3])) {
                        tab.add(i.toString())
                    }
                }
                return tab
            }
        }
        return mutableListOf()
    }

    private fun getQuestion(name: String): Question? {
        return Question(name, questionConfig.config().getString("${name}.content")?:return null, questionConfig.config().getString("${name}.type")!!.questionType()?:return null)
    }

    private fun String.questionType(): QuestionType? {
        return when {
            this.toLowerCase()== QuestionType.NUMBER.toString() -> {
                QuestionType.NUMBER
            }
            this.toLowerCase()== QuestionType.DECISION.toString() -> {
                QuestionType.DECISION
            }
            else -> {
                null
            }
        }
    }
}