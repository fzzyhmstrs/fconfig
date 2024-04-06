package me.fzzyhmstrs.fzzy_config.screen

import com.google.common.collect.ArrayListMultimap
import me.fzzyhmstrs.fzzy_config.annotations.ClientModifiable
import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.annotations.WithPerms
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.impl.ConfigSet
import me.fzzyhmstrs.fzzy_config.impl.Walkable
import me.fzzyhmstrs.fzzy_config.screen.ConfigScreenManager.ConfigScreenBuilder
import me.fzzyhmstrs.fzzy_config.screen.entry.ConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.Position
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.descLit
import me.fzzyhmstrs.fzzy_config.util.FcText.description
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.FcText.translation
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.MultilineTextWidget
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlComment
import java.util.*
import java.util.function.Function
import kotlin.math.max
import kotlin.math.min

@Environment(EnvType.CLIENT)
internal class ConfigScreenManager(private val scope: String, private val configs: List<ConfigSet>) {

    private val configMap: Map<String,Set<Config>>
    private var screens: Map<String, ConfigScreenBuilder> = mapOf()
    private val forwardedUpdates: MutableList<ForwardedUpdate> = mutableListOf()
    private var copyBuffer: Any? = null
    private val manager: ConfigUpdateManager

    init{
        val map: MutableMap<String,Set<Config>> = mutableMapOf()
        if (configs.size > 1)
            map[scope] = configs.map { it.active }.toSet()
        configs.forEach { map[it.active.getId().toTranslationKey()] = setOf(it.active) }
        configMap = map

        manager = ConfigUpdateManager(configs, forwardedUpdates, ConfigApiImplClient.getPlayerPermissionLevel())

        prepareScreens()
    }

    //////////////////////////////////////////////

    internal fun receiveForwardedUpdate(update: String, player: UUID, scope: String) {
        var entry: Entry<*,*>? = null
        for ((config,_,_) in configs){
            ConfigApiImpl.walk(config,config.getId().toTranslationKey(),true){ _,_, new, thing, _, _ ->
                if (new == scope){
                    if(thing is Entry<*,*>){
                        entry = thing
                        return@walk
                    }
                }
            }
            if (entry != null)
                break
        }
        if (entry == null)
            return
        try {
            forwardedUpdates.add(ForwardedUpdate(update, player, entry!!))
        } catch (e: Exception){
            //empty catch block to avoid stupid crashes
        }
    }

    internal fun openScreen(scope: String = this.scope) {
        if (MinecraftClient.getInstance().currentScreen !is ConfigScreen) {
            manager.flush()
            manager.pushUpdatableStates()
        }
        openScopedScreen(scope)
    }

    private fun openScopedScreen(scope: String){
        val realScope = if(scope == this.scope && configMap.size == 1)
            configMap.keys.toList()[0]
        else
            scope
        val screen = screens[realScope]?.build() ?: return
        MinecraftClient.getInstance().setScreen(screen)
    }

    private fun pushToBuffer(input: Any?){
        copyBuffer = input
    }

    ////////////////////////////////////////////////

    private fun prepareScreens(){
        val permLevel = ConfigApiImplClient.getPlayerPermissionLevel()
        if (configs.size == 1){
            prepareSingleConfigScreen(permLevel)
        } else {
            prepareMultiConfigScreens(permLevel)
        }
    }

    private fun prepareSingleConfigScreen(playerPermLevel: Int) {
        val functionMap: MutableMap<String, SortedMap<Int, Function<ConfigListWidget, ConfigEntry>>> = mutableMapOf()
        val nameMap: MutableMap<String,Text> = mutableMapOf()
        val config = configs[0]
        walkConfig(config.active, config.base, functionMap, nameMap, if(config.clientOnly) 4 else playerPermLevel)
        //walkBasicValues(config.base, functionMap, nameMap, if(config.clientOnly) 4 else playerPermLevel)
        val scopes = functionMap.keys.toList()
        val scopeButtonFunctions = buildScopeButtons(nameMap)
        val builders: MutableMap<String, ConfigScreenBuilder> = mutableMapOf()
        for((scope, entryBuilders) in functionMap){
            val name = nameMap[scope] ?: continue
            builders[scope] = buildBuilder(name, scope, scopes, scopeButtonFunctions, entryBuilders.values.toList())
        }
        this.screens = builders
    }

    private fun prepareMultiConfigScreens(playerPermLevel: Int) {
        val functionMap: MutableMap<String, SortedMap<Int, Function<ConfigListWidget, ConfigEntry>>> = mutableMapOf()
        val nameMap: MutableMap<String,Text> = mutableMapOf()
        for ((i,config) in configs.withIndex()) {
            functionMap.computeIfAbsent(scope) { sortedMapOf()}[i] = screenOpenEntryBuilder(config.active.translation(), config.active.description(), config.active.getId().toTranslationKey())
            walkConfig(config.active, config.base, functionMap, nameMap, if(config.clientOnly) 4 else playerPermLevel)
            //walkBasicValues(config.base, functionMap, nameMap, if(config.clientOnly) 4 else playerPermLevel)
        }
        val scopes = functionMap.keys.toList()
        val scopeButtonFunctions = buildScopeButtons(nameMap)
        val builders: MutableMap<String, ConfigScreenBuilder> = mutableMapOf()
        for((scope, entryBuilders) in functionMap){
            val name = nameMap[scope] ?: continue
            builders[scope] = buildBuilder(name, scope, scopes, scopeButtonFunctions, entryBuilders.values.toList())
        }
        this.screens = builders
    }

    private fun walkConfig(config: Config, baseConfig: Config, functionMap: MutableMap<String, SortedMap<Int, Function<ConfigListWidget, ConfigEntry>>>, nameMap: MutableMap<String, Text>, playerPermLevel: Int){
        val defaultPermLevel = config.defaultPermLevel()
        //putting the config buttons themselves, in the base scope. ex: "my_mod"
        nameMap[config.getId().toTranslationKey()] = config.transLit(config::class.java.simpleName.split(FcText.regex).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } })
        //walking the config, base scope passed to walk is ex: "my_mod.my_config"
        var index = 0
        val prefix = config.getId().toTranslationKey()
        ConfigApiImpl.walk(config,prefix,true) { _,old,new,thing,_,annotations ->
            println(new)
            if(thing is Walkable) {
                val fieldName = new.substringAfterLast('.')
                val name = thing.transLit(fieldName.split(FcText.regex).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } })
                nameMap[new] = name
                functionMap.computeIfAbsent(old) { sortedMapOf()}[index] = screenOpenEntryBuilder(name, thing.descLit(getComments(annotations)), new)
                index++
            } else if (thing is Updatable && thing is Entry<*,*>) {
                val fieldName = new.substringAfterLast('.')
                val name = thing.transLit(fieldName.split(FcText.regex).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } })
                nameMap[new] = name
                if(hasNeededPermLevel(playerPermLevel,defaultPermLevel,annotations)) {
                    thing.setUpdateManager(manager)
                    manager.setUpdatableEntry(thing)
                    if (ConfigApiImpl.isNonSync(annotations))
                        functionMap.computeIfAbsent(old) { sortedMapOf()}[index] = forwardableEntryBuilder(name, thing.descLit(getComments(annotations)), thing)
                    else
                        functionMap.computeIfAbsent(old) { sortedMapOf()}[index] = updatableEntryBuilder(name, thing.descLit(getComments(annotations)), thing)
                } else
                    functionMap.computeIfAbsent(old) { sortedMapOf()}[index] = noPermsEntryBuilder(name, thing.descLit(getComments(annotations)))
                index++
            } else if (thing != null) {
                var basicValidation: ValidatedField<*>? = null
                val target = new.removePrefix("$prefix.")
                ConfigApiImpl.drill(baseConfig,target,'.',true) { _,_,_,thing2,prop,_ ->
                    basicValidation = manager.basicValidationStrategy(thing2,prop.returnType)?.instanceEntry()
                }
                val basicValidation2 = basicValidation
                if (basicValidation2 != null) {
                    basicValidation2.trySet(thing)
                    basicValidation2.setEntryKey(new)
                    val name = basicValidation2.translation()
                    nameMap[new] = name
                    if(hasNeededPermLevel(playerPermLevel,defaultPermLevel,annotations)) {
                        basicValidation2.setUpdateManager(manager)
                        manager.setUpdatableEntry(basicValidation2)
                        if (ConfigApiImpl.isNonSync(annotations))
                            functionMap.computeIfAbsent(old) { sortedMapOf()}[index] = forwardableEntryBuilder(name, thing.descLit(getComments(annotations)), basicValidation2)
                        else
                            functionMap.computeIfAbsent(old) { sortedMapOf()}[index] = updatableEntryBuilder(name, thing.descLit(getComments(annotations)), basicValidation2)
                    } else
                        functionMap.computeIfAbsent(old) { sortedMapOf()}[index] = noPermsEntryBuilder(name, thing.descLit(getComments(annotations)))
                }
                index++
            }
        }
    }

    private fun getComments(annotations: List<Annotation>): String{
        var comment = ""
        for (annotation in annotations){
            if (annotation is TomlComment){
                if (comment.isNotEmpty())
                    comment += ". "
                comment += annotation.text
            } else if(annotation is Comment){
                if (comment.isNotEmpty())
                    comment += ". "
                comment += annotation.value
            }
        }
        if (comment.isNotEmpty())
            comment += "."
        return comment
    }

    private fun buildScopeButtons(nameMap: Map<String,Text>): Map<String, Function<ConfigScreen,ClickableWidget>>{
        val textRenderer = MinecraftClient.getInstance().textRenderer
        return nameMap.mapValues { (name, translation) ->
                Function { _ ->
                    ButtonWidget.builder(translation) { openScopedScreen(name) }
                        .dimensions(0, 0, min(100,textRenderer.getWidth(translation) + 8),20)
                        .narrationSupplier{ _ -> FcText.translatable("fc.button.navigate",translation) }
                        .build()
                }
            }
    }

    private fun buildBuilder(name:Text, scope: String, scopes: List<String>, scopeButtonFunctions: Map<String, Function<ConfigScreen,ClickableWidget>>, entryBuilders: List<Function<ConfigListWidget,ConfigEntry>>): ConfigScreenBuilder{
        val parentScopes = scopes.filter { scope.contains(it) && it != scope }.sortedBy { it.length }
        val functionList: MutableList<Function<ConfigScreen, ClickableWidget>> = mutableListOf()
        for(fScope in parentScopes) {
            functionList.add(scopeButtonFunctions[fScope] ?: continue)
        }
        val configListFunction = Function { screen: ConfigScreen ->
            val listWidget = ConfigListWidget(MinecraftClient.getInstance(), screen)
            for (entryBuilder in entryBuilders){
                listWidget.add(entryBuilder.apply(listWidget))
            }
            listWidget
        }
        return ConfigScreenBuilder {
            val screen = ConfigScreen(name, scope,manager, configListFunction, functionList)
            screen.parent = MinecraftClient.getInstance().currentScreen
            screen
        }
    }

    private fun hasNeededPermLevel(playerPermLevel: Int, defaultPerm: Int, annotations: List<Annotation>): Boolean {
        if (ConfigApiImpl.isNonSync(annotations)) return true
        for (annotation in annotations){
            if (annotation is WithPerms)
                return playerPermLevel >= annotation.opLevel
        }
        for (annotation in annotations){
            if (annotation is ClientModifiable)
                return true
        }
        return playerPermLevel >= defaultPerm
    }

    //////////////////////////////////////

    private fun <T> updatableEntryBuilder(name: Text, desc: Text, entry: T): Function<ConfigListWidget, ConfigEntry> where T: Updatable, T: Entry<*,*> {
        return Function { parent ->
            ConfigEntry(
                name,
                desc,
                parent,
                entry.widgetEntry()
            ) { mX, mY, _ -> openRightClickPopup(mX, mY, entry, false) }
        }
    }

    private fun <T> forwardableEntryBuilder(name: Text, desc: Text, entry: T): Function<ConfigListWidget, ConfigEntry> where T: Updatable, T: Entry<*,*> {
        return Function { parent ->
            ConfigEntry(
                name,
                desc,
                parent,
                entry.widgetEntry()
            ) { mX, mY, _ -> openRightClickPopup(mX, mY, entry, true) }
        }
    }

    private fun noPermsEntryBuilder(name: Text, desc: Text): Function<ConfigListWidget, ConfigEntry> {
        return Function { parent -> ConfigEntry(name, desc, parent,NoPermsButtonWidget()) {_,_,_ ->} }
    }

    private fun screenOpenEntryBuilder(name: Text, desc: Text, scope: String): Function<ConfigListWidget,ConfigEntry> {
        return Function { parent -> ConfigEntry(name, desc, parent, ScreenOpenButtonWidget(name) { openScopedScreen(scope) }) {_,_,_ ->} }
    }

    /////////////////////////////

    private fun <T> openRightClickPopup(x: Int, y: Int, entry: T, withForwarding: Boolean) where T: Updatable, T: Entry<*,*> {
        val client = MinecraftClient.getInstance()
        val copyText = "fc.button.copy".translate()
        val pasteText = "fc.button.paste".translate()
        val revertText = "fc.button.revert".translate()
        val restoreText = "fc.button.restore".translate()
        val popup = PopupWidget.Builder("fc.config.right_click".translate(),2,2)
            .addDivider()
            .positionX(PopupWidget.Builder.at{ x })
            .positionY(PopupWidget.Builder.at{ y })
            .background("widget/popup/background_right_click".fcId())
            .noBlur()
        if(entry.canCopyEntry()) {
            popup.addElement("copy", ActiveButtonWidget(copyText, client.textRenderer.getWidth(copyText) + 8, 14, { true }, { pushToBuffer(entry.get()); PopupWidget.pop() },"widget/popup/button_right_click_highlighted".fcId()), Position.BELOW, Position.ALIGN_LEFT)
            popup.addElement("paste", ActiveButtonWidget(pasteText, client.textRenderer.getWidth(pasteText) + 8, 14, { entry.isValidEntry(copyBuffer) }, { entry.trySet(copyBuffer); PopupWidget.pop() },"widget/popup/button_right_click_highlighted".fcId()), Position.BELOW, Position.ALIGN_LEFT)
        }
        popup.addElement("revert", ActiveButtonWidget(revertText, client.textRenderer.getWidth(revertText) + 8, 14, { entry.peekState() }, { entry.revert(); PopupWidget.pop() },"widget/popup/button_right_click_highlighted".fcId()), Position.BELOW, Position.ALIGN_LEFT)
        popup.addElement("restore", ActiveButtonWidget(restoreText, client.textRenderer.getWidth(restoreText) + 8, 14, { !entry.isDefault() }, { b -> openRestoreConfirmPopup(b, entry) },"widget/popup/button_right_click_highlighted".fcId()), Position.BELOW, Position.ALIGN_LEFT)
        if(withForwarding)
            popup.addElement("forward", ActiveButtonWidget(restoreText, client.textRenderer.getWidth(restoreText), 14, { true }, { openEntryForwardingPopup(entry) },"widget/popup/button_right_click_highlighted".fcId()), Position.BELOW, Position.ALIGN_LEFT)
        PopupWidget.setPopup(popup.build())
    }
    private fun <T> openRestoreConfirmPopup(b: ActiveButtonWidget, entry: T) where T: Updatable, T: Entry<*,*> {
        val client = MinecraftClient.getInstance()
        val confirmText = "fc.button.restore.confirm".translate()
        val confirmTextWidth = max(50,client.textRenderer.getWidth(confirmText) + 8)
        val cancelText = "fc.button.cancel".translate()
        val cancelTextWidth = max(50,client.textRenderer.getWidth(cancelText) + 8)
        val buttonWidth = max(confirmTextWidth,cancelTextWidth)

        val popup = PopupWidget.Builder("fc.button.restore".translate())
            .addDivider()
            .addElement("confirm_text", MultilineTextWidget("fc.config.restore.confirm.desc".translate(), MinecraftClient.getInstance().textRenderer).setCentered(true).setMaxWidth(buttonWidth + 4 + buttonWidth), Position.BELOW, Position.ALIGN_CENTER)
            .addElement("confirm_button", ButtonWidget.builder(confirmText) { entry.restore(); PopupWidget.pop(); PopupWidget.pop() }.size(buttonWidth,20).build(), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("cancel_button", ButtonWidget.builder(cancelText) { PopupWidget.pop() }.size(buttonWidth,20).build(),"confirm_text", Position.BELOW, Position.ALIGN_RIGHT)
            .positionX(PopupWidget.Builder.popupContext { w -> b.x + b.width/2 - w/2 })
            .positionY(PopupWidget.Builder.popupContext { h -> b.y - h + 28 })
            .width(buttonWidth + 4 + buttonWidth + 16)
            .build()
        PopupWidget.setPopup(popup)
    }

    private fun <T> openEntryForwardingPopup(entry: T) where T: Updatable, T: Entry<*,*> {
        val client = MinecraftClient.getInstance()
        val playerEntries = client.player?.networkHandler?.playerList?.associateBy { it.profile.name } ?: return
        val validator = EntryValidator.Builder<String>().both({s -> playerEntries.containsKey(s)}).buildValidator()
        var player = ""
        val forwardText = "fc.button.forward.confirm".translate()
        val forwardTextWidth = max(50,client.textRenderer.getWidth(forwardText) + 8)
        val cancelText = "fc.button.cancel".translate()
        val cancelTextWidth = max(50,client.textRenderer.getWidth(cancelText) + 8)
        val buttonWidth = max(forwardTextWidth,cancelTextWidth)
        val popup = PopupWidget.Builder("fc.button.forward".translate())
            .addDivider()
            .addElement("desc",
                MultilineTextWidget("fc.button.forward.active".translate(), MinecraftClient.getInstance().textRenderer).setCentered(true).setMaxWidth(buttonWidth + 4 + buttonWidth), Position.BELOW, Position.ALIGN_CENTER)
            .addElement("player_finder", ValidationBackedTextFieldWidget(110,20, {player}, ChoiceValidator.any(), validator, {s -> player = s}), Position.BELOW, Position.ALIGN_LEFT)
            //.addElement("forward_button", ActiveButtonWidget(forwardText,buttonWidth,20,{players.contains(player)},{}), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("cancel_button", ButtonWidget.builder(cancelText) { PopupWidget.pop() }.size(buttonWidth,20).build(),"confirm_text", Position.BELOW, Position.ALIGN_RIGHT)
            .width(buttonWidth + 4 + buttonWidth + 16)
            .build()
        PopupWidget.setPopup(popup)
    }

    ///////////////////////////////////////

    internal fun interface ConfigScreenBuilder {
        fun build(): ConfigScreen
    }

    internal class ForwardedUpdate(val update: String, val player: UUID, val entry: Entry<*,*>)

}