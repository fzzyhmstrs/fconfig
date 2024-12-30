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

import me.fzzyhmstrs.fzzy_config.config.*
import me.fzzyhmstrs.fzzy_config.entry.*
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.impl.ConfigSet
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.entry.*
import me.fzzyhmstrs.fzzy_config.screen.internal.ConfigScreenManager.ConfigScreenBuilder
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.Ref
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.platform.impl.PlatformUtils
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.min

//client
internal class ConfigScreenManager(private val scope: String, private val configs: List<ConfigSet>) {

    private val configMap: Map<String, Set<Config>>
    private val forwardedUpdates: MutableList<ForwardedUpdate> = mutableListOf()
    private val manager: ConfigUpdateManager

    private var screens: Map<String, ConfigScreenBuilder> = mapOf()
    private var copyBuffer: Ref<Any?> = Ref(null)
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

    private fun checkForRebuild() {
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
    }

    internal fun provideScreen(scope: String = this.scope): Screen? {
        checkForRebuild()
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
        //don't open the screen that's already open
        if (MinecraftClient.getInstance().currentScreen?.nullCast<ConfigScreen>()?.scope == scope) return
        checkForRebuild()
        openScopedScreen(scope)
    }

    private fun openScopedScreen(scope: String) {
        val realScope = if(scope == this.scope && configMap.size == 1)
            configMap.keys.toList()[0]
        else
            getValidScope(scope) ?: return
        val screen = screens[realScope]?.build() ?: return
        MinecraftClient.getInstance().setScreen(screen)
    }

    private fun getValidScope(scope: String): String? {
        val validScopes = screens.keys
        if(validScopes.contains(scope)) return scope
        var validScopeTry = scope.substringBeforeLast('.')
        if (validScopeTry == scope) return null
        while(!validScopes.contains(validScopeTry) && validScopeTry.contains('.')) {
            validScopeTry = validScopeTry.substringBeforeLast('.')
        }
        return if(validScopes.contains(validScopeTry)) validScopeTry else null
    }

    private fun outOfGame(): Boolean {
        val client = MinecraftClient.getInstance()
        return (client.world == null || client.networkHandler == null || !client.isInSingleplayer)
    }

    ////////////////////////////////////////////////

    private fun prepareScreens() {
        val permLevel = ConfigApiImplClient.getPlayerPermissionLevel()
        prepareConfigScreens(permLevel)
    }

    private fun prepareConfigScreens(playerPermLevel: Int) {
        val functionMap: MutableMap<String, MutableList<EntryCreator.Creator>> = mutableMapOf()
        val nameMap: MutableMap<String, Text> = mutableMapOf()
        val anchors: MutableList<Function<DynamicListWidget, out DynamicListWidget.Entry>> = mutableListOf()
        var anchorWidth = 0
        val anchorPredicate: Predicate<AnchorResult> =
            Predicate { result ->
                if (result.thing !is EntryAnchor) return@Predicate false
                nameMap[result.scope] = result.texts.name
                val layer = result.scope.split('.').filter { it != this.scope }.size
                val anchor = result.thing.anchorEntry(EntryAnchor.Anchor(layer, result.texts.name))
                val anchorTexts = Translatable.Result(anchor.name, result.texts.desc, result.texts.prefix)
                if (configs.size == 1 && anchor.type == EntryAnchor.AnchorType.INLINE) {
                    anchor.layer++
                } else if (anchor.type == EntryAnchor.AnchorType.INLINE) {
                    anchor.layer += 2
                } else if (anchor.type == EntryAnchor.AnchorType.SECTION) {
                    anchor.layer++
                }
                anchorWidth = max(anchorWidth, SidebarEntry.neededWidth(anchorTexts, anchor.layer))
                val anchorFunction: Function<DynamicListWidget, out DynamicListWidget.Entry> = Function { list ->
                    val anchorId = result.thing.anchorId(result.scope)
                    val action = anchor.type.action(result.scope, anchorId)
                    SidebarEntry(
                        list,
                        result.scope,
                        anchorTexts,
                        anchor.decoration ?: TextureDeco.DECO_QUESTION,
                        action,
                        anchor.layer)
                }
                anchors.add(anchorFunction)
                false
            }
        nameMap[scope] = PlatformUtils.configName(this.scope, "Config Root").lit()
        for (config in configs) {
            walkConfig(config, functionMap, anchorPredicate, playerPermLevel)
        }
        val scopes = functionMap.keys.toList()
        val scopeButtonFunctions = buildScopeButtons(nameMap)
        val builders: MutableMap<String, ConfigScreenBuilder> = mutableMapOf()
        for((s, entryBuilders) in functionMap) {
            val name = nameMap[s] ?: FcText.EMPTY
            builders[s] = buildBuilder(name, s, scopes, scopeButtonFunctions, entryBuilders, anchors, anchorWidth)
        }
        this.screens = builders
    }

    private fun walkConfig(
        set: ConfigSet, //the current set of configs to walk
        functionMap: MutableMap<String, MutableList<EntryCreator.Creator>>, //Creators go here, sorted by encounter order and scope
        anchorPredicate: Predicate<AnchorResult>, //Returns true if the anchor will supplant an inline entry. nameMap will now happen here
        playerPermLevel: Int)
    {
        val config: Config = set.active
        val baseConfig: Config = set.base
        val prefix = config.getId().toTranslationKey()
        val configTexts = ConfigApiImplClient.getText(config, "", config::class.annotations, emptyList(), config::class.java.simpleName)
        val groups: LinkedList<String> = LinkedList()

        fun List<EntryCreator.Creator>.applyToMap(parent: String, functionMap: MutableMap<String, MutableList<EntryCreator.Creator>>) {
            for (creator in this) {
                functionMap.computeIfAbsent(parent) { mutableListOf() }.add(creator)
            }
        }

        val contextMisc: EntryCreator.CreatorContextMisc = EntryCreator.CreatorContextMisc()
            .put(EntryCreators.OPEN_SCREEN, Consumer { s -> openScopedScreen(s) })
            .put(EntryCreators.COPY_BUFFER, copyBuffer)

        val skip = anchorPredicate.test(AnchorResult(prefix, config, configTexts))

        //apply top level Creators as needed
        if (!skip || configTexts.prefix != null) {
            val context = EntryCreator.CreatorContext(prefix, groups, set.clientOnly, configTexts, config::class.annotations, ConfigApiImpl.getActions(config, ConfigApiImpl.IGNORE_NON_SYNC), contextMisc)
            //config button, if the config spec allows for them
            if (!skip) {
                EntryCreators.createConfigEntry(context).applyToMap(this.scope, functionMap)
            }
            //Header entry injected into function map at top of config
            if (configTexts.prefix != null) {
                EntryCreators.createHeaderEntry(context, configTexts.prefix).applyToMap(prefix, functionMap)
            }
        }

        //walking the config, base scope passed to walk is ex: "my_mod.my_config"
        ConfigApiImpl.walk(config, prefix, 1) { _, old, new, thing, _, annotations, globalAnnotations, callback ->
            val flags = if(thing is EntryFlag) {
                EntryFlag.Flag.entries.filter { thing.hasFlag(it) }
            } else {
                EntryFlag.Flag.NONE
            }

            val entryCreator: EntryCreator?

            val prepareResult = if (thing is EntryCreator) {
                entryCreator = thing
                thing.prepare(new, groups, annotations, globalAnnotations)
                ConfigApiImplClient.prepare(thing, playerPermLevel, config, prefix, new, annotations, globalAnnotations, set.clientOnly, flags)
            } else if (thing != null) {
                var basicValidation: ValidatedField<*>? = null
                val target = new.removePrefix("$prefix.")
                ConfigApiImpl.drill(baseConfig, target, '.', 1) { _, _, _, thing2, drillProp, drillAnnotations, _, _ ->
                    basicValidation = manager.basicValidationStrategy(thing2, drillProp.returnType, drillAnnotations)?.instanceEntry()
                }
                val basicValidation2 = basicValidation
                if (basicValidation2 != null) {
                    basicValidation2.trySet(thing)
                    basicValidation2.setEntryKey(new)
                    entryCreator = basicValidation2
                    basicValidation2.prepare(new, groups, annotations, globalAnnotations)
                    ConfigApiImplClient.prepare(thing, playerPermLevel, config, prefix, new, annotations, globalAnnotations, set.clientOnly, flags)
                } else {
                    entryCreator = null
                    ConfigApiImplClient.PrepareResult.FAIL
                }
            } else {
                entryCreator = null
                ConfigApiImplClient.PrepareResult.FAIL
            }

            if (!prepareResult.fail) {
                if (prepareResult.cont) {
                    callback.cont()
                }

                if (entryCreator is Updatable) {
                    entryCreator.setUpdateManager(manager)
                    manager.setUpdatableEntry(entryCreator)
                }

                val skip2 = anchorPredicate.test(AnchorResult(new, thing, prepareResult.texts))

                if (!skip2) {
                    val context = EntryCreator.CreatorContext(new, LinkedList(groups), set.clientOnly, prepareResult.texts, annotations, prepareResult.actions, contextMisc)

                    when (prepareResult.perms) {
                        ConfigApiImplClient.PermResult.FAILURE -> {
                            EntryCreators.createNoPermsEntry(context, "noPerms").applyToMap(old, functionMap)
                        }
                        ConfigApiImplClient.PermResult.OUT_OF_GAME -> {
                            EntryCreators.createNoPermsEntry(context, "outOfGame").applyToMap(old, functionMap)
                        }
                        else -> {
                            entryCreator?.createEntry(context)?.applyToMap(old, functionMap)
                        }
                    }
                }

                ConfigGroup.pop(annotations, groups)
            }
        }
    }

    private class AnchorResult(val scope: String, val thing: Any?, val texts: Translatable.Result)

    private fun buildScopeButtons(nameMap: Map<String, Text>): Map<String, Supplier<ClickableWidget>> {
        val textRenderer = MinecraftClient.getInstance().textRenderer
        return nameMap.mapValues { (name, translation) ->
                Supplier {
                    CustomButtonWidget.builder(translation) { openScopedScreen(name) }
                        .dimensions(0, 0, min(100, textRenderer.getWidth(translation) + 8), 20)
                        .narrationSupplier{ _ -> FcText.translatable("fc.button.navigate", translation) }
                        .build()
                }
            }
    }

    private fun buildBuilder(name: Text,
                             scope: String,
                             scopes: List<String>,
                             scopeButtonFunctions: Map<String, Supplier<ClickableWidget>>,
                             entries: List<EntryCreator.Creator>,
                             anchors: List<Function<DynamicListWidget, out DynamicListWidget.Entry>>,
                             anchorWidth: Int): ConfigScreenBuilder {
        val scopeSplit = scope.split(".")
        val parentScopes = scopes.filter { scopeSplit.containsAll(it.split(".")) && it != scope }.sortedBy { it.length }
        val suppliers: MutableList<Supplier<ClickableWidget>> = mutableListOf()
        for(fScope in parentScopes) {
            suppliers.add(scopeButtonFunctions[fScope] ?: continue)
        }
        return ConfigScreenBuilder {
            val list = DynamicListWidget(MinecraftClient.getInstance(), entries.map { it.entry }, 0, 0, 290, 290, DynamicListWidget.ListSpec(verticalPadding = 4))
            ConfigScreen(name, scope, this.manager, list, suppliers, anchors, anchorWidth).setParent(MinecraftClient.getInstance().currentScreen)
        }
    }

    ///////////////////////////////////////

    internal fun interface ConfigScreenBuilder {
        fun build(): ConfigScreen
    }

    internal class ForwardedUpdate(val scope: String, val update: String, val player: UUID, val entry: Entry<*, *>, val summary: String)

}