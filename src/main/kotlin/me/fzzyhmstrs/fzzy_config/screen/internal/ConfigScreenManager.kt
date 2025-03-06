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

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup
import me.fzzyhmstrs.fzzy_config.entry.*
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.impl.ConfigSet
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.entry.EntryCreators
import me.fzzyhmstrs.fzzy_config.screen.entry.SidebarEntry
import me.fzzyhmstrs.fzzy_config.screen.internal.ConfigScreenManager.ConfigScreenBuilder
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureDeco
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.updates.BasicValidationProvider
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.updates.UpdateManager
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
import java.lang.ref.SoftReference
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.min

//client
internal class ConfigScreenManager(private val scope: String, private val configs: Map<String, ConfigSet>): BasicValidationProvider {

    private val sidebar: Sidebar = Sidebar()
    private val screenCaches: MutableMap<String, ConfigScreenCache> = mutableMapOf()

    private var copyBuffer: Ref<Any?> = Ref(null)
    private var cachedPerms:  Map<String, Map<String, Boolean>> = mapOf()
    private val cachedPermKey: AtomicInteger = AtomicInteger(0)
    private var cachedScope: String = ""

    init {
        cachedPerms = ConfigApiImplClient.getPerms()
    }

    //////////////////////////////////////////////

    internal fun receiveForwardedUpdate(update: String, player: UUID, scope: String, summary: String) {
        val realScope = getValidScope(scope)
        if (realScope == null) {
            FC.LOGGER.error("Received forwarded update from unknown config")
            FC.LOGGER.error("    > Player: $player")
            FC.LOGGER.error("    > Scope: $scope")
            FC.LOGGER.error("    > Summary: $summary")
            return
        }
        val config = configs[realScope]
            ?: throw IllegalStateException("Unexpected error: Config $realScope not found in manager")
        val cache = screenCaches.computeIfAbsent(realScope) { _ ->
            val playerPerms = ConfigApiImplClient.getPlayerPermissionLevel()
            val customPermsKey = cachedPermKey.get()
            val outOfWorld = outOfGame()
            prepareConfigScreenCache(config, playerPerms, customPermsKey, outOfWorld)
        }
        cache.receiveForwardedUpdate(config.active, update, player, scope, summary)
    }

    internal fun provideScreen(scope: String = this.scope): Screen? {
        cachedScope = scope
        if (cachedPerms != ConfigApiImplClient.getPermsRef()) {
            cachedPerms = ConfigApiImplClient.getPerms()
            cachedPermKey.incrementAndGet()
        }
        //checkForRebuild()
        return provideScopedScreen(scope)
    }

    private fun provideScopedScreen(scope: String): Screen? {
        val realScope = if (scope == this.scope && configs.size == 1)
            configs.keys.toList()[0]
        else if (scope == this.scope)
            return provideRootScreen()
        else
            getValidScope(scope) ?: return null
        val configSet = configs[realScope] ?: return null
        val playerPerms = ConfigApiImplClient.getPlayerPermissionLevel()
        val customPermsKey = cachedPermKey.get()
        val outOfWorld = outOfGame()
        var cache = screenCaches.computeIfAbsent(realScope) { _ ->
            prepareConfigScreenCache(configSet, playerPerms, customPermsKey, outOfWorld)
        }
        if (cache.isInvalid(playerPerms, customPermsKey, outOfWorld)) {
            cache = prepareConfigScreenCache(configSet, playerPerms, customPermsKey, outOfWorld, cache)
            screenCaches[realScope] = cache
        }
        cache.manager.pushUpdatableStates(cachedScope)
        val realScope2 = if (scope == this.scope && configs.size == 1)
            realScope
        else
            scope
        return cache.provideScreen(realScope2)
    }

    private fun provideRootScreen(): Screen? {
        val playerPerms = ConfigApiImplClient.getPlayerPermissionLevel()
        val customPermsKey = cachedPermKey.get()
        val outOfWorld = outOfGame()
        var cache = screenCaches.computeIfAbsent(this.scope) { _ ->
            prepareRootScreenCache(playerPerms, customPermsKey, outOfWorld)
        }
        if (cache.isInvalid(playerPerms, customPermsKey, outOfWorld)) {
            cache = prepareRootScreenCache(playerPerms, customPermsKey, outOfWorld, cache)
            screenCaches[this.scope] = cache
        }
        cache.manager.pushUpdatableStates(cachedScope)
        return cache.provideScreen(this.scope)
    }

    internal fun openScreen(scope: String = this.scope) {
        cachedScope = scope
        if (cachedPerms != ConfigApiImplClient.getPermsRef()) {
            cachedPerms = ConfigApiImplClient.getPerms()
            cachedPermKey.incrementAndGet()
        }
        //don't open the screen that's already open
        if (MinecraftClient.getInstance().currentScreen?.nullCast<ConfigScreen>()?.scope == scope) return
        openScopedScreen(scope)
    }

    private fun openScopedScreen(scope: String) {
        val screen = provideScopedScreen(scope) ?: return
        MinecraftClient.getInstance().setScreen(screen)
    }

    private tailrec fun getValidScope(scope: String): String? {
        if(configs.keys.contains(scope)) return scope
        val validScopeTry = scope.substringBeforeLast('.')
        if (validScopeTry == scope) return null
        return getValidScope(validScopeTry)
    }

    private fun outOfGame(): Boolean {
        val client = MinecraftClient.getInstance()
        return (client.world == null || client.networkHandler == null || !client.isInSingleplayer)
    }

    ////////////////////////////////////////////////

    private fun prepareConfigScreenCache(configSet: ConfigSet, playerPerms: Int, customPermsKey: Int, outOfGame: Boolean, previous: ConfigScreenCache? = null): ConfigScreenCache {
        val functionMap: MutableMap<String, MutableList<EntryCreator.Creator>> = mutableMapOf()
        val nameMap: MutableMap<String, Text> = mutableMapOf()
        val anchorPredicate: Predicate<AnchorResult> =
            Predicate { result ->
                if (result.thing !is EntryAnchor) return@Predicate false
                nameMap[result.scope] = result.texts.name
                false
            }
        val forwardedUpdates: MutableList<ForwardedUpdate> = previous?.forwardedUpdates ?: mutableListOf()
        val manager = ConfigSingleUpdateManager(configSet, forwardedUpdates, playerPerms)

        if (configs.size > 1) {
            val cache = screenCaches[this.scope]
            cache?.manager?.nullCast<ConfigRootUpdateManager>()?.addChild(manager)
        }

        walkConfig(configSet, functionMap, anchorPredicate, playerPerms, manager)

        if(configs.size > 1)
            nameMap[this.scope] = PlatformUtils.configName(this.scope, "Config Root").lit()
        val scopes = functionMap.keys.toList() + if(configs.size > 1) listOf(this.scope) else listOf()
        val scopeButtonFunctions = buildScopeButtons(nameMap)
        val builders: MutableMap<String, ConfigScreenBuilder> = mutableMapOf()
        for((s, entryBuilders) in functionMap) {
            val name = nameMap[s] ?: FcText.EMPTY
            builders[s] = buildBuilder(name, s, scopes, scopeButtonFunctions, entryBuilders, manager)
        }
        return ConfigScreenCache(forwardedUpdates, manager, builders, playerPerms, customPermsKey, outOfGame)
    }

    private fun prepareRootScreenCache(playerPerms: Int, customPermsKey: Int, outOfGame: Boolean, previous: ConfigScreenCache? = null): ConfigScreenCache {
        val name = PlatformUtils.configName(this.scope, "Config Root").lit()
        val forwardedUpdates: MutableList<ForwardedUpdate> = previous?.forwardedUpdates ?: mutableListOf()
        val manager = ConfigRootUpdateManager()
        val functions: MutableList<EntryCreator.Creator> = mutableListOf()
        val contextMisc: EntryCreator.CreatorContextMisc = EntryCreator.CreatorContextMisc()
            .put(EntryCreators.OPEN_SCREEN, Consumer { s -> openScopedScreen(s) })
            .put(EntryCreators.COPY_BUFFER, copyBuffer)

        fun List<EntryCreator.Creator>.applyToList(functions: MutableList<EntryCreator.Creator>) {
            for (creator in this) {
                functions.add(creator)
            }
        }

        for ((s, set) in configs) {
            screenCaches[s]?.let { manager.addChild(it.manager) }
            val config: Config = set.active
            val prefix = config.getId().toTranslationKey()
            val configTexts = ConfigApiImplClient.getText(config, "", config::class.annotations, emptyList(), config::class.java.simpleName)

            val context = EntryCreator.CreatorContext(prefix, ConfigGroup.emptyGroups, set.clientOnly, configTexts, config::class.annotations, ConfigApiImpl.getActions(config, ConfigApiImpl.IGNORE_NON_SYNC), contextMisc)
            //config button, if the config spec allows for them
            EntryCreators.createConfigEntry(context).applyToList(functions)
            //Header entry injected into function map at top of config
        }

        val builder = buildBuilder(name, this.scope, listOf(), mapOf(), functions, manager)

        return ConfigScreenCache(forwardedUpdates, manager, mapOf(this.scope to builder), playerPerms, customPermsKey, outOfGame)
    }

    private fun prepareSidebarData(): SidebarData {

        val anchors: MutableList<BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry>> = mutableListOf()
        var anchorWidth = 0

        val anchorConsumer: Consumer<AnchorResult> =
            Consumer { result ->
                if (result.thing !is EntryAnchor) return@Consumer
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
                val anchorFunction: BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry> = BiFunction { list, _ ->
                    val anchorId = result.thing.anchorId(result.scope)
                    val action = anchor.type.action(result.scope, anchorId)
                    SidebarEntry(
                        list,
                        result.scope,
                        anchorTexts,
                        Decorated.DecoratedOffset(anchor.decoration ?: TextureDeco.DECO_QUESTION, anchor.offsetX, anchor.offsetY),
                        action,
                        anchor.layer)
                }
                anchors.add(anchorFunction)
            }

        for (configSet in configs.values) {
            val config = configSet.active
            val prefix = config.getId().toTranslationKey()
            val configTexts = ConfigApiImplClient.getText(config, "", config::class.annotations, emptyList(), config::class.java.simpleName)
            anchorConsumer.accept(AnchorResult(prefix, config, configTexts))

            ConfigApiImpl.walk(config, prefix, 1) { _, _, new, thing, thingProp, annotations, globalAnnotations, callback ->
                if (thing != null) {
                    if (thing is EntryParent) {
                        if (thing.continueWalk()) callback.cont()
                    } else if (thing !is EntryCreator && this.basicValidationStrategy(thing, thingProp.returnType, new, annotations).nullCast<EntryParent>()?.continueWalk() == true) {
                        callback.cont()
                    }
                    val fieldName = new.substringAfterLast('.')
                    val texts = ConfigApiImplClient.getText(thing, fieldName, annotations, globalAnnotations)
                    anchorConsumer.accept(AnchorResult(new, thing, texts))
                }
            }
        }

        return SidebarData(anchors, anchorWidth + 1)
    }

    private val contextMisc: EntryCreator.CreatorContextMisc by lazy {
        EntryCreator.CreatorContextMisc()
            .put(EntryCreators.OPEN_SCREEN, Consumer { s -> openScopedScreen(s) })
            .put(EntryCreators.COPY_BUFFER, copyBuffer)
    }



    private fun walkConfig(
        set: ConfigSet, //the current set of configs to walk
        functionMap: MutableMap<String, MutableList<EntryCreator.Creator>>, //Creators go here, sorted by encounter order and scope
        anchorPredicate: Predicate<AnchorResult>, //Returns true if the anchor will supplant an inline entry. nameMap will now happen here
        playerPermLevel: Int,
        manager: ConfigBaseUpdateManager)
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

        anchorPredicate.test(AnchorResult(prefix, config, configTexts))

        if (configTexts.prefix != null) {
            val context = EntryCreator.CreatorContext(prefix, groups, set.clientOnly, configTexts, config::class.annotations, ConfigApiImpl.getActions(config, ConfigApiImpl.IGNORE_NON_SYNC), contextMisc)
            EntryCreators.createHeaderEntry(context).applyToMap(prefix, functionMap)
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
                    basicValidation = this.basicValidationStrategy(thing2, drillProp.returnType, new, drillAnnotations)?.instanceEntry()
                }
                val basicValidation2 = basicValidation
                if (basicValidation2 != null) {
                    basicValidation2.trySet(thing)
                    basicValidation2.setEntryKey(new)
                    entryCreator = basicValidation2
                    basicValidation2.prepare(new, groups, annotations, globalAnnotations)
                    ConfigApiImplClient.prepare(basicValidation2, playerPermLevel, config, prefix, new, annotations, globalAnnotations, set.clientOnly, flags)
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
                    //pass in a singleton list if there is an empty group state to save memory
                    val context = EntryCreator.CreatorContext(new, if (groups.isEmpty()) ConfigGroup.emptyGroups else LinkedList(groups), set.clientOnly, prepareResult.texts, annotations, prepareResult.actions, contextMisc)

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
                        .narrationSupplier { _, _ -> FcText.translatable("fc.button.navigate", translation) }
                        .build()
                }
            }
    }

    private fun buildBuilder(name: Text,
                             scope: String,
                             scopes: List<String>,
                             scopeButtonFunctions: Map<String, Supplier<ClickableWidget>>,
                             entries: List<EntryCreator.Creator>,
                             manager: UpdateManager): ConfigScreenBuilder {
        return ConfigScreenBuilder {
            val scopeSplit = scope.split(".")
            val parentScopes = scopes.filter { scopeSplit.containsAll(it.split(".")) && it != scope }.sortedBy { it.length }
            val suppliers: List<Supplier<ClickableWidget>> = parentScopes.mapNotNull { scopeButtonFunctions[it] }
            val list = DynamicListWidget(MinecraftClient.getInstance(), entries.map { it.entry }, 0, 0, 290, 290, DynamicListWidget.ListSpec(verticalPadding = 4))
            ConfigScreen(name, scope, manager, list, suppliers, this.sidebar) {
                for ((_, cache) in screenCaches) {
                    if (cache.manager.hasChanges()) cache.manager.apply(true)
                    cache.manager.invalidatePush()
                }
            }.setParent(MinecraftClient.getInstance().currentScreen)
        }
    }

    ///////////////////////////////////////

    private fun interface ConfigScreenBuilder {
        fun build(): ConfigScreen
    }

    private class ConfigScreenCache(val forwardedUpdates: MutableList<ForwardedUpdate>, val manager: ConfigBaseUpdateManager, screenBuilders: Map<String, ConfigScreenBuilder>, private val permLevel: Int, private val customPermsKey: Int, private val outOfWorld: Boolean) {

        private val screens: SoftReference<Map<String, ConfigScreenBuilder>> = SoftReference(screenBuilders)

        fun isInvalid(permLevel: Int, customPermsKey: Int, outOfWorld: Boolean): Boolean {
            return this.permLevel != permLevel
                    || this.customPermsKey != customPermsKey
                    || this.outOfWorld != outOfWorld
                    || this.screens.get() == null
        }

        fun provideScreen(scope: String) : Screen? {
            val screenMap = screens.get()
            val screenTry = screenMap?.get(getValidSubScope(scope))
            return screenTry?.build()
        }

        private tailrec fun getValidSubScope(scope: String): String? {
            val screenMap = screens.get() ?: return null
            if(screenMap.keys.contains(scope)) return scope
            val validScopeTry = scope.substringBeforeLast('.')
            if (validScopeTry == scope) return null
            return getValidSubScope(validScopeTry)
        }

        fun receiveForwardedUpdate(config: Config, update: String, player: UUID, scope: String, summary: String) {
            var entry: Entry<*, *>? = null
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
            if (entry == null)
                return
            try {
                forwardedUpdates.add(ForwardedUpdate(scope, update, player, entry!!, summary))
            } catch (e: Throwable) {
                //empty catch block to avoid stupid crashes
            }
        }
    }

    internal class ForwardedUpdate(val scope: String, val update: String, val player: UUID, val entry: Entry<*, *>, val summary: String)

    private class SidebarData(val anchors: List<BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry>>, val anchorWidth: Int)

    internal inner class Sidebar {

        private val data: SidebarData by lazy {
            this@ConfigScreenManager.prepareSidebarData()
        }

        fun getAnchors(): List<BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry>> {
            return data.anchors
        }

        fun getAnchorWidth(): Int {
            return data.anchorWidth
        }

        fun needsSidebar(): Boolean {
            return (this@ConfigScreenManager.configs.size > 1) || (data.anchors.size > 1)
        }

    }
}