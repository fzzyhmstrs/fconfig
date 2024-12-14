/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.internal

import com.mojang.brigadier.suggestion.SuggestionsBuilder
import me.fzzyhmstrs.fzzy_config.annotations.*
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigAction
import me.fzzyhmstrs.fzzy_config.config.ConfigSection
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryFlag
import me.fzzyhmstrs.fzzy_config.entry.EntryParent
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.impl.ConfigSet
import me.fzzyhmstrs.fzzy_config.networking.NetworkEventsClient
import me.fzzyhmstrs.fzzy_config.screen.entry.BaseConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.entry.ConfigConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.entry.SectionConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.entry.SettingConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.internal.ConfigScreenManager.ConfigScreenBuilder
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.Position
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.ConfigListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.NoPermsButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.ScreenOpenButtonWidget
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.Walkable
import me.fzzyhmstrs.fzzy_config.util.platform.impl.PlatformUtils
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.MultilineTextWidget
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.command.CommandSource
import net.minecraft.text.Text
import java.util.*
import java.util.function.Function
import kotlin.math.max
import kotlin.math.min

//client
internal class ConfigScreenManager(private val scope: String, private val configs: List<ConfigSet>) {

    private val configMap: Map<String, Set<Config>>
    private val forwardedUpdates: MutableList<ForwardedUpdate> = mutableListOf()
    private val manager: ConfigUpdateManager

    private var screens: Map<String, ConfigScreenBuilder> = mapOf()
    private var copyBuffer: Any? = null
    private var cachedPermissionLevel = 0
    private var cachedPerms:  Map<String, Map<String, Boolean>> = mapOf()
    private var cachedOutOfGame: Boolean = false

    init {
        val map: MutableMap<String, Set<Config>> = mutableMapOf()
        if (configs.size > 1)
            map[scope] = configs.map { it.active }.toSet()
        configs.forEach { map[it.active.getId().toTranslationKey()] = setOf(it.active) }
        configMap = map

        manager = ConfigUpdateManager(configs, forwardedUpdates, ConfigApiImplClient.getPlayerPermissionLevel())
        cachedPermissionLevel = ConfigApiImplClient.getPlayerPermissionLevel()
        cachedPerms = ConfigApiImplClient.getPerms()
        prepareScreens()
    }

    //////////////////////////////////////////////

    internal fun receiveForwardedUpdate(update: String, player: UUID, scope: String, summary: String) {
        var entry: Entry<*, *>? = null
        for ((config, _, _) in configs) {
            ConfigApiImpl.walk(config, config.getId().toTranslationKey(), 1) { _, _, new, thing, _, _, _, callback ->
                if (new == scope) {
                    if(thing is Entry<*, *>) {
                        entry = thing
                        callback.cancel()
                    } else {
                        val basicThing = manager.getUpdatableEntry(new)
                        if (basicThing != null && basicThing is Entry<*, *>) {
                            entry = basicThing
                            callback.cancel()
                        }
                    }
                }
            }
            if (entry != null)
                break
        }
        if (entry == null)
            return
        try {
            forwardedUpdates.add(ForwardedUpdate(scope, update, player, entry!!, summary))
        } catch (e: Throwable) {
            //empty catch block to avoid stupid crashes
        }
    }

    internal fun provideScreen(scope: String = this.scope): Screen? {
        if(cachedPermissionLevel != ConfigApiImplClient.getPlayerPermissionLevel()
            || cachedPerms != ConfigApiImplClient.getPerms()
            || cachedOutOfGame != outOfGame()) {
            cachedPermissionLevel = ConfigApiImplClient.getPlayerPermissionLevel()
            cachedPerms = ConfigApiImplClient.getPerms()
            cachedOutOfGame = outOfGame()
            manager.flush()
            prepareScreens()
        }
        if (MinecraftClient.getInstance().currentScreen !is ConfigScreen) {
            manager.flush()
            manager.pushUpdatableStates()
        }
        return provideScopedScreen(scope)
    }

    private fun provideScopedScreen(scope: String): Screen? {
        val realScope = if (scope == this.scope && configMap.size == 1)
            configMap.keys.toList()[0]
        else
            scope
        return screens[realScope]?.build() ?: return null
    }

    internal fun openScreen(scope: String = this.scope) {
        if(cachedPermissionLevel != ConfigApiImplClient.getPlayerPermissionLevel()
            || cachedPerms != ConfigApiImplClient.getPerms()
            || cachedOutOfGame != outOfGame()) {
            cachedPermissionLevel = ConfigApiImplClient.getPlayerPermissionLevel()
            cachedPerms = ConfigApiImplClient.getPerms()
            cachedOutOfGame = outOfGame()
            manager.flush()
            prepareScreens()
        }
        if (MinecraftClient.getInstance().currentScreen !is ConfigScreen) {
            manager.flush()
            manager.pushUpdatableStates()
        }
        openScopedScreen(scope)
    }

    private fun openScopedScreen(scope: String) {
        val realScope = if(scope == this.scope && configMap.size == 1)
            configMap.keys.toList()[0]
        else
            scope
        val screen = screens[realScope]?.build() ?: return
        MinecraftClient.getInstance().setScreen(screen)
    }

    private fun outOfGame(): Boolean {
        val client = MinecraftClient.getInstance()
        return (client.world == null || client.networkHandler == null || !client.isInSingleplayer)
    }

    private fun pushToBuffer(input: Any?) {
        copyBuffer = input
    }

    ////////////////////////////////////////////////

    private fun prepareScreens() {
        val permLevel = ConfigApiImplClient.getPlayerPermissionLevel()
        if (configs.size == 1) {
            prepareSingleConfigScreen(permLevel)
        } else {
            prepareMultiConfigScreens(permLevel)
        }
    }

    private fun prepareSingleConfigScreen(playerPermLevel: Int) {
        val functionMap: MutableMap<String, SortedMap<Int, Pair<String, Function<ConfigListWidget, BaseConfigEntry>>>> = mutableMapOf()
        val nameMap: MutableMap<String, Text> = mutableMapOf()
        val restartSet: MutableMap<String, MutableSet<Action>> = mutableMapOf()
        val config = configs[0]
        walkConfig(config, functionMap, nameMap, restartSet, if(config.clientOnly) 4 else playerPermLevel)
        //walkBasicValues(config.base, functionMap, nameMap, if(config.clientOnly) 4 else playerPermLevel)
        val scopes = functionMap.keys.toList()
        val scopeButtonFunctions = buildScopeButtons(nameMap)
        val builders: MutableMap<String, ConfigScreenBuilder> = mutableMapOf()
        for((scope, entryBuilders) in functionMap) {
            val name = nameMap[scope] ?: continue
            builders[scope] = buildBuilder(name, scope, scopes, scopeButtonFunctions, restartSet, entryBuilders.values.toList())
        }
        this.screens = builders
    }

    private fun prepareMultiConfigScreens(playerPermLevel: Int) {
        val functionMap: MutableMap<String, SortedMap<Int, Pair<String, Function<ConfigListWidget, BaseConfigEntry>>>> = mutableMapOf()
        val nameMap: MutableMap<String, Text> = mutableMapOf()
        val actionMap: MutableMap<String, MutableSet<Action>> = mutableMapOf()
        nameMap[scope] = PlatformUtils.configName(this.scope, "Config Root").lit()
        for ((i, config) in configs.withIndex()) {
            functionMap.computeIfAbsent(scope) { sortedMapOf() }[i] = Pair(config.active.getId().toTranslationKey(), configOpenEntryBuilder(ConfigApiImplClient.getTranslation(config.active, "", config.active::class.annotations, listOf(), config.active::class.java.simpleName), ConfigApiImplClient.getDescription(config.active, "", config.active::class.annotations, listOf()), config.active.getId().toTranslationKey()))
            walkConfig(config, functionMap, nameMap, actionMap, if(config.clientOnly) 4 else playerPermLevel)
            //walkBasicValues(config.base, functionMap, nameMap, if(config.clientOnly) 4 else playerPermLevel)
        }
        val scopes = functionMap.keys.toList()
        val scopeButtonFunctions = buildScopeButtons(nameMap)
        val builders: MutableMap<String, ConfigScreenBuilder> = mutableMapOf()
        for((scope, entryBuilders) in functionMap) {
            val name = nameMap[scope] ?: continue
            builders[scope] = buildBuilder(name, scope, scopes, scopeButtonFunctions, actionMap, entryBuilders.values.toList())
        }
        this.screens = builders
    }

    private fun walkConfig(set: ConfigSet, functionMap: MutableMap<String, SortedMap<Int, Pair<String, Function<ConfigListWidget, BaseConfigEntry>>>>, nameMap: MutableMap<String, Text>, actionMap: MutableMap<String, MutableSet<Action>>, playerPermLevel: Int) {
        val config: Config = set.active
        val baseConfig: Config = set.base
        //putting the config buttons themselves, in the base scope. ex: "my_mod"
        nameMap[config.getId().toTranslationKey()] = ConfigApiImplClient.getTranslation(config, "", config::class.annotations, listOf(), config::class.java.simpleName) //config.transLit(config::class.java.simpleName.split(FcText.regex).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } })
        //walking the config, base scope passed to walk is ex: "my_mod.my_config"
        var index = 0
        val prefix = config.getId().toTranslationKey()
        ConfigApiImpl.walk(config, prefix, 1) { _, old, new, thing, _, annotations, globalAnnotations, callback ->
            val fieldName = new.substringAfterLast('.')
            val action = ConfigApiImpl.requiredAction(annotations, globalAnnotations)
            val totalActions = action?.let { mutableSetOf(it) } ?: mutableSetOf()
            if (thing is EntryParent) {
                val anyActions = thing.actions()
                if (anyActions.isNotEmpty()) {
                    totalActions.addAll(anyActions)
                    actionMap.computeIfAbsent(new) { mutableSetOf() }.addAll(anyActions)
                }
                if (thing.continueWalk()) {
                    callback.cont()
                }
            }
            if (action != null) actionMap[new] = mutableSetOf(action)
            val flags = if(thing is EntryFlag) {
                EntryFlag.Flag.entries.filter { thing.hasFlag(it) }
            } else {
                EntryFlag.Flag.NONE
            }
            if (thing is ConfigSection) {
                val name = ConfigApiImplClient.getTranslation(thing, fieldName, annotations, globalAnnotations)
                nameMap[new] = name
                functionMap.computeIfAbsent(old) { sortedMapOf()}[index] = Pair(new, sectionOpenEntryBuilder(name, ConfigApiImplClient.getDescription(thing, fieldName, annotations, globalAnnotations), totalActions, new))
                index++
            } else if (thing is Updatable && thing is Entry<*, *>) {
                val name = ConfigApiImplClient.getTranslation(thing, fieldName, annotations, globalAnnotations)
                nameMap[new] = name
                val perms = hasNeededPermLevel(playerPermLevel, config, prefix, new, annotations, set.clientOnly, flags)
                if(perms.success) {
                    thing.setUpdateManager(manager)
                    manager.setUpdatableEntry(thing)
                    if (ConfigApiImpl.isNonSync(annotations) || set.clientOnly)
                        functionMap.computeIfAbsent(old) { sortedMapOf()}[index] = Pair(new, forwardableEntryBuilder(name, ConfigApiImplClient.getDescription(thing, fieldName, annotations, globalAnnotations), totalActions, thing))
                    else
                        functionMap.computeIfAbsent(old) { sortedMapOf()}[index] = Pair(new, updatableEntryBuilder(name, ConfigApiImplClient.getDescription(thing, fieldName, annotations, globalAnnotations), totalActions, thing))
                } else if (perms == PermResult.OUT_OF_GAME) {
                    functionMap.computeIfAbsent(old) { sortedMapOf()}[index] = Pair(new, outOfGameEntryBuilder(name, FcText.empty(), totalActions))
                } else {
                    functionMap.computeIfAbsent(old) { sortedMapOf()}[index] = Pair(new, noPermsEntryBuilder(name, FcText.empty(), totalActions))
                }
                index++
            } else if (thing is ConfigAction) {
                val name = ConfigApiImplClient.getTranslation(thing, fieldName, annotations, globalAnnotations)
                nameMap[new] = name
                val perms = hasNeededPermLevel(playerPermLevel, config, prefix, new, annotations, set.clientOnly, flags)
                if (perms.success) {
                    functionMap.computeIfAbsent(old) { sortedMapOf() }[index] = Pair(new, configActionEntryBuilder(name, ConfigApiImplClient.getDescription(thing, fieldName, annotations, globalAnnotations), totalActions, thing))
                } else if (perms == PermResult.OUT_OF_GAME) {
                    functionMap.computeIfAbsent(old) { sortedMapOf() }[index] = Pair(new, outOfGameEntryBuilder(name, FcText.empty(), totalActions))
                } else {
                    functionMap.computeIfAbsent(old) { sortedMapOf() }[index] = Pair(new, noPermsEntryBuilder(name, FcText.empty(), totalActions))
                }

                index++
            } else if (thing != null) {
                var basicValidation: ValidatedField<*>? = null
                val target = new.removePrefix("$prefix.")
                ConfigApiImpl.drill(baseConfig, target, '.', 1) { _, _, _, thing2, drillProp, drillAnnotations, _, _ ->
                    basicValidation = manager.basicValidationStrategy(thing2, drillProp.returnType, drillAnnotations)?.instanceEntry()
                }
                val basicValidation2 = basicValidation
                if (basicValidation2 != null) {
                    basicValidation2.trySet(thing)
                    @Suppress("DEPRECATION")
                    basicValidation2.setEntryKey(new)
                    val name = ConfigApiImplClient.getTranslation(basicValidation2, fieldName, annotations, globalAnnotations)
                    nameMap[new] = name
                    val perms = hasNeededPermLevel(playerPermLevel, config, prefix, new, annotations, set.clientOnly, flags)
                    if(perms.success) {
                        @Suppress("DEPRECATION")
                        basicValidation2.setUpdateManager(manager)
                        manager.setUpdatableEntry(basicValidation2)
                        if (ConfigApiImpl.isNonSync(annotations) || set.clientOnly)
                            functionMap.computeIfAbsent(old) { sortedMapOf() } [index] = Pair(new, forwardableEntryBuilder(name, if(thing is Translatable) ConfigApiImplClient.getDescription(thing, fieldName, annotations, globalAnnotations) else ConfigApiImplClient.getDescription(basicValidation2, fieldName, annotations, globalAnnotations), totalActions, basicValidation2))
                        else
                            functionMap.computeIfAbsent(old) { sortedMapOf()}[index] = Pair(new, updatableEntryBuilder(name, if(thing is Translatable) ConfigApiImplClient.getDescription(thing, fieldName, annotations, globalAnnotations) else ConfigApiImplClient.getDescription(basicValidation2, fieldName, annotations, globalAnnotations), totalActions, basicValidation2))
                    } else if (perms == PermResult.OUT_OF_GAME) {
                        functionMap.computeIfAbsent(old) { sortedMapOf()}[index] = Pair(new, outOfGameEntryBuilder(name, FcText.empty(), action?.let { setOf(it) } ?: setOf()))
                    } else {
                        functionMap.computeIfAbsent(old) { sortedMapOf()}[index] = Pair(new, noPermsEntryBuilder(name, FcText.empty(), action?.let { setOf(it) } ?: setOf()))
                    }
                }
                index++
            }
        }
    }

    private fun buildScopeButtons(nameMap: Map<String, Text>): Map<String, Function<ConfigScreen, ClickableWidget>> {
        val textRenderer = MinecraftClient.getInstance().textRenderer
        return nameMap.mapValues { (name, translation) ->
                Function { _ ->
                    CustomButtonWidget.builder(translation) { openScopedScreen(name) }
                        .dimensions(0, 0, min(100, textRenderer.getWidth(translation) + 8), 20)
                        .narrationSupplier{ _ -> FcText.translatable("fc.button.navigate", translation) }
                        .build()
                }
            }
    }

    private fun buildBuilder(name: Text, scope: String, scopes: List<String>, scopeButtonFunctions: Map<String, Function<ConfigScreen, ClickableWidget>>, actionMap: MutableMap<String, MutableSet<Action>>, entryBuilders: List<Pair<String, Function<ConfigListWidget, BaseConfigEntry>>>): ConfigScreenBuilder {
        val scopeSplit = scope.split(".")
        val parentScopes = scopes.filter { scopeSplit.containsAll(it.split(".")) && it != scope }.sortedBy { it.length }
        val functionList: MutableList<Function<ConfigScreen, ClickableWidget>> = mutableListOf()
        for(fScope in parentScopes) {
            functionList.add(scopeButtonFunctions[fScope] ?: continue)
        }
        val configListFunction = Function { screen: ConfigScreen ->
            val listWidget = ConfigListWidget(MinecraftClient.getInstance(), screen)
            for ((entryScope, entryBuilder) in entryBuilders) {
                val set: MutableSet<Action> = actionMap.filter { it.key.startsWith(entryScope) }.map { it.value }.stream().collect( { mutableSetOf() }, { s, actions -> s.addAll(actions) }, MutableSet<Action>::addAll)
                listWidget.add(entryBuilder.apply(listWidget).restartTriggering(set))
            }
            listWidget
        }
        return ConfigScreenBuilder {
            val screen = ConfigScreen(name, scope, manager, configListFunction, functionList)
            screen.setParent(MinecraftClient.getInstance().currentScreen)
            screen
        }
    }

    private fun hasNeededPermLevel(playerPermLevel: Int, config: Config, configId: String, id: String, annotations: List<Annotation>, clientOnly: Boolean, flags: List<EntryFlag.Flag>): PermResult {
        val client = MinecraftClient.getInstance()
        val needsWorld = flags.contains(EntryFlag.Flag.REQUIRES_WORLD)
        if(client.isInSingleplayer || (clientOnly && !needsWorld)) return PermResult.SUCCESS //single player or client config, they can do what they want!!
        if (needsWorld) return PermResult.OUT_OF_GAME //setting specifically needs
        // 1. NonSync wins over everything, even whole config annotations
        if (ConfigApiImpl.isNonSync(annotations)) return PermResult.SUCCESS

        val configAnnotations = config::class.annotations
        // 2. whole-config ClientModifiable
        for (annotation in configAnnotations) {
            if (annotation is ClientModifiable)
                return PermResult.SUCCESS
        }
        // 3. per-setting ClientModifiable
        for (annotation in annotations) {
            if (annotation is ClientModifiable)
                return PermResult.SUCCESS
        }

        //not in a game, can't send packets so can't know your permissions for real
        if (client.world == null || client.networkHandler == null) return PermResult.OUT_OF_GAME

        for (annotation in annotations) {
            //4. per-setting WithCustomPerms
            if (annotation is WithCustomPerms) {
                if(cachedPerms[configId]?.get(id) == true) {
                    return PermResult.SUCCESS
                }
                return if (annotation.fallback >= 0) {
                    if (playerPermLevel >= annotation.fallback) {
                        PermResult.SUCCESS
                    } else {
                        PermResult.FAILURE
                    }
                } else {
                    PermResult.FAILURE
                }
            }
            //5. per-setting WithPerms
            if (annotation is WithPerms) {
                return if (playerPermLevel >= annotation.opLevel) {
                    PermResult.SUCCESS
                } else {
                    PermResult.FAILURE
                }
            }
        }
        for (annotation in configAnnotations) {
            //6. whole-config WithCustomPerms
            if (annotation is WithCustomPerms) {
                if(cachedPerms[configId]?.get(id) == true) {
                    return PermResult.SUCCESS
                }
                return if (annotation.fallback >= 0) {
                    if (playerPermLevel >= annotation.fallback) {
                        PermResult.SUCCESS
                    } else {
                        PermResult.FAILURE
                    }
                } else {
                    PermResult.FAILURE
                }
            }
            //7. whole-config WithCustomPerms
            if (annotation is WithPerms) {
                return if (playerPermLevel >= annotation.opLevel) {
                    PermResult.SUCCESS
                } else {
                    PermResult.FAILURE
                }
            }
        }
        //8. fallback to default vanilla permission level
        return if (playerPermLevel >= config.defaultPermLevel()) {
            PermResult.SUCCESS
        } else {
            PermResult.FAILURE
        }
    }

    //////////////////////////////////////

    private fun <T> updatableEntryBuilder(name: Text, desc: Text, actions: Set<Action>, entry: T): Function<ConfigListWidget, BaseConfigEntry> where T: Updatable, T: Entry<*, *> {
        return Function { parent ->
            SettingConfigEntry(
                name,
                desc,
                actions,
                parent,
                entry.widgetEntry(),
                { pushToBuffer(entry.get()) },
                { if (entry.isValidEntry(copyBuffer)) entry.trySet(copyBuffer) }
            ) { mX, mY, _ -> openRightClickPopup(mX, mY, entry, false) }
        }
    }

    private fun <T> forwardableEntryBuilder(name: Text, desc: Text, actions: Set<Action>, entry: T): Function<ConfigListWidget, BaseConfigEntry> where T: Updatable, T: Entry<*, *> {
        return Function { parent ->
            SettingConfigEntry(
                name,
                desc,
                actions,
                parent,
                entry.widgetEntry(),
                { pushToBuffer(entry.get()) },
                { if (entry.isValidEntry(copyBuffer)) entry.trySet(copyBuffer) }
            ) { mX, mY, _ -> openRightClickPopup(mX, mY, entry, true) }
        }
    }

    private fun noPermsEntryBuilder(name: Text, desc: Text, actions: Set<Action>): Function<ConfigListWidget, BaseConfigEntry> {
        return Function { parent -> BaseConfigEntry(name, desc, actions, parent, NoPermsButtonWidget()) }
    }

    private fun outOfGameEntryBuilder(name: Text, desc: Text, actions: Set<Action>): Function<ConfigListWidget, BaseConfigEntry> {
        return Function { parent -> BaseConfigEntry(name, desc, actions, parent, NoPermsButtonWidget("fc.button.outOfGame".translate(), "fc.button.outOfGame.desc".translate())) }
    }

    private fun sectionOpenEntryBuilder(name: Text, desc: Text, actions: Set<Action>, scope: String): Function<ConfigListWidget, BaseConfigEntry> {
        return Function { parent -> SectionConfigEntry(name, desc, actions, parent, ScreenOpenButtonWidget(name) { openScopedScreen(scope) }) }
    }

    private fun configOpenEntryBuilder(name: Text, desc: Text, scope: String): Function<ConfigListWidget, BaseConfigEntry> {
        return Function { parent -> ConfigConfigEntry(name, desc, setOf(), parent, ScreenOpenButtonWidget(name) { openScopedScreen(scope) }) }
    }

    private fun configActionEntryBuilder(name: Text, desc: Text, actions: Set<Action>, entry: ConfigAction): Function<ConfigListWidget, BaseConfigEntry> {
        return Function { parent -> BaseConfigEntry(name, desc, actions, parent, entry.widgetEntry()) }
    }

    /////////////////////////////

    private fun <T> openRightClickPopup(x: Int, y: Int, entry: T, withForwarding: Boolean) where T: Updatable, T: Entry<*, *> {
        val client = MinecraftClient.getInstance()
        val copyText = "fc.button.copy".translate()
        val pasteText = "fc.button.paste".translate()
        val revertText = "fc.button.revert".translate()
        val restoreText = "fc.button.restore".translate()
        val forwardText = "fc.button.forward".translate()
        val popup = PopupWidget.Builder("fc.config.right_click".translate(), 2, 2)
            .addDivider()
            .positionX(PopupWidget.Builder.at{ x })
            .positionY(PopupWidget.Builder.at{ y })
            .background("widget/popup/background_right_click".fcId())
            .noBlur()
        popup.addElement("copy", ActiveButtonWidget(copyText, client.textRenderer.getWidth(copyText) + 8, 14, { true }, { pushToBuffer(entry.get()); PopupWidget.pop() }, "widget/popup/button_right_click_highlighted".fcId()), Position.BELOW, Position.ALIGN_LEFT)
        popup.addElement("paste", ActiveButtonWidget(pasteText, client.textRenderer.getWidth(pasteText) + 8, 14, { entry.isValidEntry(copyBuffer) }, { entry.trySet(copyBuffer); PopupWidget.pop() }, "widget/popup/button_right_click_highlighted".fcId()), Position.BELOW, Position.ALIGN_LEFT)
        popup.addElement("revert", ActiveButtonWidget(revertText, client.textRenderer.getWidth(revertText) + 8, 14, { entry.peekState() }, { entry.revert(); PopupWidget.pop() }, "widget/popup/button_right_click_highlighted".fcId()), Position.BELOW, Position.ALIGN_LEFT)
        popup.addElement("restore", ActiveButtonWidget(restoreText, client.textRenderer.getWidth(restoreText) + 8, 14, { !entry.isDefault() }, { b -> openRestoreConfirmPopup(b, entry) }, "widget/popup/button_right_click_highlighted".fcId()), Position.BELOW, Position.ALIGN_LEFT)
        if(withForwarding)
            popup.addElement("forward", ActiveButtonWidget(forwardText, client.textRenderer.getWidth(forwardText) + 8, 14, { true }, { openEntryForwardingPopup(entry) }, "widget/popup/button_right_click_highlighted".fcId()), Position.BELOW, Position.ALIGN_LEFT)
        PopupWidget.push(popup.build())
    }
    private fun <T> openRestoreConfirmPopup(b: ActiveButtonWidget, entry: T) where T: Updatable, T: Entry<*, *> {
        val client = MinecraftClient.getInstance()
        val confirmText = "fc.button.restore.confirm".translate()
        val confirmTextWidth = max(50, client.textRenderer.getWidth(confirmText) + 8)
        val cancelText = "fc.button.cancel".translate()
        val cancelTextWidth = max(50, client.textRenderer.getWidth(cancelText) + 8)
        val buttonWidth = max(confirmTextWidth, cancelTextWidth)

        val popup = PopupWidget.Builder("fc.button.restore".translate())
            .addDivider()
            .addElement("confirm_text", MultilineTextWidget("fc.config.restore.confirm.desc".translate(), MinecraftClient.getInstance().textRenderer).setCentered(true).setMaxWidth(buttonWidth + 4 + buttonWidth), Position.BELOW, Position.ALIGN_CENTER)
            .addElement("confirm_button", CustomButtonWidget.builder(confirmText) { entry.restore(); PopupWidget.pop(); PopupWidget.pop() }.size(buttonWidth, 20).build(), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("cancel_button", CustomButtonWidget.builder(cancelText) { PopupWidget.pop() }.size(buttonWidth, 20).build(), "confirm_text", Position.BELOW, Position.ALIGN_RIGHT)
            .positionX(PopupWidget.Builder.popupContext { w -> b.x + b.width/2 - w/2 })
            .positionY(PopupWidget.Builder.popupContext { h -> b.y - h + 28 })
            .width(buttonWidth + 4 + buttonWidth + 16)
            .build()
        PopupWidget.push(popup)
    }

    private fun <T> openEntryForwardingPopup(entry: T) where T: Updatable, T: Entry<*, *> {
        val client = MinecraftClient.getInstance()
        val playerEntries = client.player?.networkHandler?.playerList?.associateBy { it.profile.name } ?: return
        val validator = EntryValidator.Builder<String>().both({s -> playerEntries.containsKey(s)}).buildValidator()
        var player = ""
        val forwardText = "fc.button.forward.confirm".translate()
        val forwardTextWidth = max(50, client.textRenderer.getWidth(forwardText) + 8)
        val cancelText = "fc.button.cancel".translate()
        val cancelTextWidth = max(50, client.textRenderer.getWidth(cancelText) + 8)
        val buttonWidth = max(forwardTextWidth, cancelTextWidth)
        val popup = PopupWidget.Builder("fc.button.forward".translate())
            .addDivider()
            .addElement("desc",
                MultilineTextWidget("fc.button.forward.active".translate(), MinecraftClient.getInstance().textRenderer).setCentered(true).setMaxWidth(buttonWidth + 4 + buttonWidth), Position.BELOW, Position.ALIGN_CENTER)
            .addElement("player_finder", SuggestionBackedTextFieldWidget(110, 20, {player}, ChoiceValidator.any(), validator, {s -> player = s}, { s, cursor, choiceValidator -> CommandSource.suggestMatching(playerEntries.keys.filter { choiceValidator.validateEntry(it, EntryValidator.ValidationType.STRONG).isValid() }, s.substring(0, cursor).let{ SuggestionsBuilder(it, it.lowercase(Locale.ROOT), 0) }) }), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("forward_button", ActiveButtonWidget(forwardText, buttonWidth, 20, {playerEntries.containsKey(player)}, { forwardUpdate(entry, playerEntries[player]); PopupWidget.pop(); PopupWidget.pop() }), Position.BELOW, Position.ALIGN_LEFT)
            .addElement("cancel_button", CustomButtonWidget.builder(cancelText) { PopupWidget.pop() }.size(buttonWidth, 20).build(), "forward_button", Position.ALIGN_RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
            .build()
        PopupWidget.push(popup)
    }

    private fun<T> forwardUpdate(entry: T, playerListEntry: PlayerListEntry?)where T: Updatable, T: Entry<*, *> {
        if (playerListEntry == null) return
        val update = ConfigApiImpl.serializeEntry(entry, mutableListOf())
        val id = playerListEntry.profile.id
        val key = entry.getEntryKey()
        val summary = entry.get().toString()
        NetworkEventsClient.forwardSetting(update, id, key, summary)
    }

    ///////////////////////////////////////

    private enum class PermResult(val success: Boolean) {
        SUCCESS(true),
        OUT_OF_GAME(false),
        FAILURE(false)
    }

    internal fun interface ConfigScreenBuilder {
        fun build(): ConfigScreen
    }

    internal class ForwardedUpdate(val scope: String, val update: String, val player: UUID, val entry: Entry<*, *>, val summary: String)

}