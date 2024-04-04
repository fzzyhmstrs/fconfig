package me.fzzyhmstrs.fzzy_config.screen

import com.google.common.collect.ArrayListMultimap
import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.annotations.ClientModifiable
import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.annotations.WithPerms
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.impl.Walkable
import me.fzzyhmstrs.fzzy_config.screen.ConfigScreenManager.ConfigScreenBuilder
import me.fzzyhmstrs.fzzy_config.screen.entry.ConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.entry.ConfigForwardableEntry
import me.fzzyhmstrs.fzzy_config.screen.entry.ConfigUpdatableEntry
import me.fzzyhmstrs.fzzy_config.screen.widget.ConfigListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.NoPermsButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.ScreenOpenButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextlessConfigActionWidget
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.descLit
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlComment
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.*
import java.util.function.Function
import kotlin.math.min

@Environment(EnvType.CLIENT)
class ConfigScreenManager(private val scope: String, private val configs: List<Pair<Config,Boolean>>) {

    private val configMap: Map<String,Set<Config>>
    private var screens: Map<String, ConfigScreenBuilder> = mapOf()
    private val forwardedUpdates: MutableList<ForwardedUpdate> = mutableListOf()

    init{
        val map: MutableMap<String,Set<Config>> = mutableMapOf()
        map[scope] = configs.map { it.first }.toSet()
        configs.forEach { map[it.first.getId().toTranslationKey()] = setOf(it.first) }
        configMap = map

        prepareScreens()
    }

    private var manager: ConfigUpdateManager = ConfigUpdateManager(configs,configMap,forwardedUpdates, ConfigApiImplClient.getPlayerPermissionLevel())

    internal fun receiveForwardedUpdate(update: String, player: UUID, scope: String) {
        var entry: Entry<*,*>? = null
        for ((config,_) in configs){
            ConfigApiImpl.walk(config,config.getId().toTranslationKey(),true){_, new, thing, _, _ ->
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
            manager = ConfigUpdateManager(configs,configMap,forwardedUpdates,ConfigApiImplClient.getPlayerPermissionLevel())
            configs.forEach { manager.pushStates(it.first) }
        }
        openScopedScreen(scope)
    }

    private fun openScopedScreen(scope: String){
        val screen = screens[scope]?.build() ?: return
        MinecraftClient.getInstance().setScreen(screen)
    }

    private fun prepareScreens(){
        val permLevel = ConfigApiImplClient.getPlayerPermissionLevel()
        if (configs.size == 1){
            prepareSingleConfigScreen(permLevel)
        } else {
            prepareMultiConfigScreens(permLevel)
        }
    }

    //move translation and description to descLit and transLit, with fallback of fieldName and TomlComment, if any.

    private fun prepareSingleConfigScreen(playerPermLevel: Int) {
        val functionMap: ArrayListMultimap<String, Function<ConfigListWidget, ConfigEntry>> = ArrayListMultimap.create()
        val nameMap: MutableMap<String,Text> = mutableMapOf()
        val config = configs[0]
        walkConfig(config.first, functionMap, nameMap, if(config.second) 4 else playerPermLevel)
        val scopes = functionMap.keySet().toList()
        val scopeButtonFunctions = buildScopeButtons(nameMap)
        val builders: MutableMap<String, ConfigScreenBuilder> = mutableMapOf()
        for((scope, entryBuilders) in functionMap.asMap()){
            val name = nameMap[scope] ?: continue
            builders[scope] = buildBuilder(name, scope, scopes, scopeButtonFunctions, entryBuilders.toList())
        }
        this.screens = builders
    }

    private fun prepareMultiConfigScreens(playerPermLevel: Int) {
        val functionMap: ArrayListMultimap<String, Function<ConfigListWidget, ConfigEntry>> = ArrayListMultimap.create()
        val nameMap: MutableMap<String,Text> = mutableMapOf()
        for (config in configs) {
            walkConfig(config.first, functionMap, nameMap, if(config.second) 4 else playerPermLevel)
        }
        val scopes = functionMap.keySet().toList()
        val scopeButtonFunctions = buildScopeButtons(nameMap)
        val builders: MutableMap<String, ConfigScreenBuilder> = mutableMapOf()
        for((scope, entryBuilders) in functionMap.asMap()){
            val name = nameMap[scope] ?: continue
            builders[scope] = buildBuilder(name, scope, scopes, scopeButtonFunctions, entryBuilders.toList())
        }
        this.screens = builders
    }

    @Suppress("DEPRECATION")
    private fun walkConfig(config: Config, functionMap: ArrayListMultimap<String, Function<ConfigListWidget, ConfigEntry>>, nameMap: MutableMap<String, Text>, playerPermLevel: Int){
        val defaultPermLevel = config.defaultPermLevel()
        //putting the config buttons themselves, in the base scope. ex: "my_mod"
        functionMap.put(scope, screenOpenEntryBuilder(config.translation(), config.description(), config.getId().toTranslationKey()))
        nameMap[config.getId().toTranslationKey()] = config.transLit(config::class.java.simpleName.split(FcText.regex).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } })
        //walking the config, base scope passed to walk is ex: "my_mod.my_config"
        ConfigApiImpl.walk(config,config.getId().toTranslationKey(),true) {old, new, thing, prop, annotations ->
            if(thing is Walkable) {
                val fieldName = new.substringAfterLast('.')
                val name = thing.transLit(fieldName.split(FcText.regex).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } })
                nameMap[new] = name
                functionMap.put(old, screenOpenEntryBuilder(name, thing.descLit(getComments(annotations)), new))
            } else if (thing is Updatable && thing is Entry<*,*>) {
                val fieldName = new.substringAfterLast('.')
                val name = thing.transLit(fieldName.split(FcText.regex).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } })
                nameMap[new] = name
                thing.setUpdateManager(manager)
                if(hasNeededPermLevel(playerPermLevel,defaultPermLevel,annotations))
                    if (ConfigApiImpl.isNonSync(annotations))
                        functionMap.put(old, forwardableEntryBuilder(name, thing.descLit(getComments(annotations)), thing))
                    else
                        functionMap.put(old, updatableEntryBuilder(name, thing.descLit(getComments(annotations)), thing))
                else
                    functionMap.put(old, noPermsEntryBuilder(name, thing.descLit(getComments(annotations))))
            } else if (thing != null) {
                val basicValidation = manager.basicValidationStrategy(thing,prop.returnType)?.instanceEntry()
                if (basicValidation != null) {
                    val fieldName = new.substringAfterLast('.')
                    val name = thing.transLit(fieldName.split(FcText.regex).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } })
                    nameMap[new] = name
                    basicValidation.setEntryKey(new)
                    basicValidation.pushState()
                    basicValidation.setUpdateManager(manager)
                    if(hasNeededPermLevel(playerPermLevel,defaultPermLevel,annotations))
                        if (ConfigApiImpl.isNonSync(annotations))
                            functionMap.put(old, forwardableEntryBuilder(name, thing.descLit(getComments(annotations)), basicValidation))
                        else
                            functionMap.put(old, updatableEntryBuilder(name, thing.descLit(getComments(annotations)), basicValidation))
                    else
                        functionMap.put(old, noPermsEntryBuilder(name, thing.descLit(getComments(annotations))))
                }
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
        val parentScopes = scopes.filter { scope.contains(it) }.sortedBy { it.length }
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
            ConfigUpdatableEntry(
                name,
                desc,
                parent,
                entry.widgetEntry(),
                TextlessConfigActionWidget("widget/action/revert".fcId(), "widget/action/revert_inactive".fcId(),"widget/action/revert_highlighted".fcId(), FcText.translatable("fc.button.revert.active"),FcText.translatable("fc.button.revert.inactive"),{ entry.peekState() } ) { entry.revert() },
                TextlessConfigActionWidget("widget/action/restore".fcId(), "widget/action/restore_inactive".fcId(),"widget/action/restore_highlighted".fcId(), FcText.translatable("fc.button.restore.active"),FcText.translatable("fc.button.restore.inactive"),{ !entry.isDefault() } ) { entry.restore() })
        }
    }

    private fun <T> forwardableEntryBuilder(name: Text, desc: Text, entry: T): Function<ConfigListWidget, ConfigEntry> where T: Updatable, T: Entry<*,*> {
        return Function { parent ->
            ConfigForwardableEntry(
                name,
                desc,
                parent,
                entry.widgetEntry(),
                TextlessConfigActionWidget("widget/action/revert".fcId(), "widget/action/revert_inactive".fcId(),"widget/action/revert_highlighted".fcId(), FcText.translatable("fc.button.revert.active"),FcText.translatable("fc.button.revert.inactive"),{ entry.peekState() } ) { entry.revert() },
                TextlessConfigActionWidget("widget/action/restore".fcId(), "widget/action/restore_inactive".fcId(),"widget/action/restore_highlighted".fcId(), FcText.translatable("fc.button.restore.active"),FcText.translatable("fc.button.restore.inactive"),{ !entry.isDefault() } ) { entry.restore() },
                TextlessConfigActionWidget("widget/action/forward".fcId(),"widget/action/forward_inactive".fcId(),"widget/action/forward_highlighted".fcId(), FcText.translatable("fc.button.forward.active"),FcText.translatable("fc.button.forward.inactive"),{ MinecraftClient.getInstance()?.networkHandler?.playerList?.let { it.size > 1 } ?: false } ) { popupEntryForwardingWidget(entry) })
        }
    }

    private fun noPermsEntryBuilder(name: Text, desc: Text): Function<ConfigListWidget, ConfigEntry> {
        return Function { parent -> ConfigEntry(name, desc, parent,NoPermsButtonWidget()) }
    }

    private fun screenOpenEntryBuilder(name: Text, desc: Text, scope: String): Function<ConfigListWidget,ConfigEntry> {
        return Function { parent -> ConfigEntry(name, desc, parent, ScreenOpenButtonWidget(name) { openScopedScreen(scope) } ) }
    }

    /////////////////////////////

    private fun <T> openRightClickPopup(x: Supplier<Int>, y: Supplier<Int>, entry: T, withForwarding: Boolean) where T: Updatable, T: Entry<*,*> {
        val popup = PopupWidget.Builder("fc.config.right_click".translate(),2,2)
            .addDivider()
            .addElement("revert", , Position.BELOW, POSITION.ALIGN_RIGHT)
            .addElement("restore", , Position.BELOW, POSITION.ALIGN_RIGHT)
            .positionX(PopupWidget.Builder.at(x))
            .positionY(PopupWidget.Builder.at(y))
            .background("widget/popup/background_right_click".fcId())
        if(withForwarding)
            popup.addElement("forward", , Position.BELOW, POSITION.ALIGN_RIGHT)
        PopupWidget.setPopup(popup.build())
    }
    private fun <T> openRestoreConfirmPopup(b: ActiveButtonWidget, entry: T) where T: Updatable, T: Entry<*,*> {
        val client = MinecraftClient.getInstance()
        val confirmText = "fc.button.restore.confirm".translate()
        val confirmTextWidth = max(50,client.textRenderer.getWidth(confirmText) + 8)
        val cancelText = "fc.button.restore.cancel".translate()
        val cancelTextWidth = max(50,client.textRenderer.getWidth(cancelText) + 8)
        val buttonWidth = max(confirmTextWidth,cancelTextWidth)

        val popup = PopupWidget.Builder("fc.button.restore".translate())
            .addDivider()
            .addElement("confirm_text", MultilineTextWidget("fc.config.restore.confirm.desc".translate(), MinecraftClient.getInstance().textRenderer).setCentered(true).setMaxWidth(buttonWidth + 4 + buttonWidth), Position.BELOW, Position.ALIGN_CENTER)
            .addElement("confirm_button", ButtonWidget.builder(confirmText) { entry.restore(); PopupWidget.pop() }.size(buttonWidth,20).build(), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("cancel_button", ButtonWidget.builder(cancelText) { PopupWidget.pop() }.size(buttonWidth,20).build(),"confirm_text", Position.BELOW, Position.ALIGN_RIGHT)
            .positionX(PopupWidget.Builder.popupContext { w -> b.x + b.width/2 - w/2 })
            .positionY(PopupWidget.Builder.popupContext { h -> b.y - h + 28 })
            .width(buttonWidth + 4 + buttonWidth + 16)
            .build()
        PopupWidget.setPopup(popup)
    }
    
    private fun <T> openEntryForwardingPopup(entry: T) where T: Updatable, T: Entry<*,*> {
        val popup = PopupWidget.Builder("fc.button.forward".translate())
            .addElement()
    }

    ///////////////////////////////////////

    internal fun interface ConfigScreenBuilder {
        fun build(): ConfigScreen
    }

    internal class ForwardedUpdate(val update: String, val player: UUID, val entry: Entry<*,*>)

}
