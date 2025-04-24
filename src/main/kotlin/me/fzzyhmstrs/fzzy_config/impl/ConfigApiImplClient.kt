/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.impl

import me.fzzyhmstrs.fzzy_config.annotations.*
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.entry.EntryFlag
import me.fzzyhmstrs.fzzy_config.entry.EntryParent
import me.fzzyhmstrs.fzzy_config.entry.EntryPermissible
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import me.fzzyhmstrs.fzzy_config.screen.internal.ConfigBaseUpdateManager
import me.fzzyhmstrs.fzzy_config.screen.internal.RestartScreen
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.literal
import me.fzzyhmstrs.fzzy_config.util.FcText.prefixLit
import me.fzzyhmstrs.fzzy_config.util.FcText.transSupplied
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.client.MinecraftClient
import net.minecraft.client.resource.language.I18n
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlComment
import java.util.function.Supplier

internal object ConfigApiImplClient {

    internal fun getPerms(): Map<String, Map<String, Boolean>> {
        return ClientConfigRegistry.getPerms()
    }

    internal fun getPermsRef(): Map<String, Map<String, Boolean>> {
        return ClientConfigRegistry.getPermsRef()
    }

    internal fun registerConfig(config: Config, baseConfig: Config, noGui: Boolean) {
        ClientConfigRegistry.registerConfig(config, baseConfig, noGui)
    }

    internal fun isConfigLoaded(id: Identifier): Boolean {
        return ClientConfigRegistry.hasClientConfig(id.toTranslationKey())
    }

    internal fun isConfigLoaded(scope: String): Boolean {
        var startIndex = 0
        while (startIndex < scope.length) {
            val nextStartIndex = scope.indexOf(".", startIndex)
            if (nextStartIndex == -1) {
                return false
            }
            startIndex = nextStartIndex + 1
            val testScope = scope.substring(0, nextStartIndex)
            if (ClientConfigRegistry.hasClientConfig(testScope)) return true
        }
        return false
    }

    internal fun getClientConfig(scope: String): Config? {
        return ClientConfigRegistry.getClientConfig(scope)
    }

    internal fun getClientConfig(id: Identifier): Config? {
        return ClientConfigRegistry.getClientConfig(id.toTranslationKey())
    }

    internal fun openScreen(scope: String) {
        MinecraftClient.getInstance().execute {
            ClientConfigRegistry.openScreen(scope)
        }
    }

    internal fun isScreenOpen(scope: String): Boolean {
        return ClientConfigRegistry.isScreenOpen(scope)
    }

    internal fun getScreenUpdateManager(scope: String): ConfigBaseUpdateManager? {
        return ClientConfigRegistry.provideUpdateManager(scope)
    }

    internal fun openRestartScreen(): Boolean {
        if (MinecraftClient.getInstance().currentScreen is RestartScreen) return false
        MinecraftClient.getInstance().execute {
            if (MinecraftClient.getInstance().currentScreen !is RestartScreen)
                MinecraftClient.getInstance().setScreen(RestartScreen())
        }
        return true
    }

    internal fun getPlayerPermissionLevel(): Int {
        val client = MinecraftClient.getInstance()
        if(client.server != null && client?.server?.isSingleplayer == true) return 4 // single player game, they can change whatever they want
        var i = 0
        while(client.player?.hasPermissionLevel(i) == true) {
            i++
        }
        return i - 1
    }

    internal fun getText(thing: Any, scope: String, fieldName: String, annotations: List<Annotation>, globalAnnotations: List<Annotation>, fallback: String = fieldName): Translatable.Result {
        return Translatable.Impls.getText(thing, scope, fieldName, annotations, globalAnnotations, fallback)
    }

    //////////////////////////

    internal fun prepare(thing: Any?,
                         playerPermLevel: Int,
                         config: Any,
                         configId: String,
                         id: String,
                         annotations: List<Annotation>,
                         globalAnnotations: List<Annotation>,
                         clientOnly: Boolean,
                         flags: List<EntryFlag.Flag>)
    : PrepareResult {
        if (thing == null) return PrepareResult.FAIL
        val fieldName = id.substringAfterLast('.')
        val texts = getText(thing, id, fieldName, annotations, globalAnnotations)
        val permResult = hasNeededPermLevel(thing, playerPermLevel, config, configId, id, annotations, clientOnly, flags, getPerms())
        if (!permResult.success) {
            return PrepareResult(permResult, setOf(), texts, (thing is EntryParent) && thing.continueWalk(), false)
        }
        val action = ConfigApiImpl.requiredAction(annotations, globalAnnotations)
        val totalActions = action?.let { mutableSetOf(it) } ?: mutableSetOf()
        if (thing is EntryParent) {
            val anyActions = thing.actions()
            totalActions.addAll(anyActions)
            return PrepareResult(permResult, totalActions, texts, thing.continueWalk(), false)
        }
        return PrepareResult(permResult, totalActions, texts, cont = false, fail = false)
    }

    class PrepareResult(val perms: PermResult, val actions: Set<Action>, val texts: Translatable.Result, val cont: Boolean, val fail: Boolean) {
        companion object {
            val FAIL = PrepareResult(PermResult.FAILURE, setOf(), Translatable.Result.EMPTY, cont = false, fail = true)
        }
    }

    internal fun hasNeededPermLevel(thing: Any?, playerPermLevel: Int, config: Any, configId: String, id: String, annotations: List<Annotation>, clientOnly: Boolean, flags: List<EntryFlag.Flag>, cachedPerms:  Map<String, Map<String, Boolean>>): PermResult {
        if (thing is EntryPermissible) return PermResult.SUCCESS
        val client = MinecraftClient.getInstance()
        val needsWorld = flags.contains(EntryFlag.Flag.REQUIRES_WORLD)
        if (client.isInSingleplayer) return PermResult.SUCCESS //single player or client config, they can do what they want!!
        if((clientOnly && !needsWorld))
            return PermResult.SUCCESS //single player or client config, they can do what they want!!
        else if ((client.world == null || client.networkHandler == null) && needsWorld) {
            return PermResult.OUT_OF_GAME //but this one needs the world to be loaded
        }
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
            //7. whole-config WithPerms
            if (annotation is WithPerms) {
                return if (playerPermLevel >= annotation.opLevel) {
                    PermResult.SUCCESS
                } else {
                    PermResult.FAILURE
                }
            }
        }
        //8. fallback to default vanilla permission level
        return if (config is Config && playerPermLevel < config.defaultPermLevel()) {
            PermResult.FAILURE
        } else {
            PermResult.SUCCESS
        }
    }

    internal enum class PermResult(val success: Boolean) {
        SUCCESS(true),
        OUT_OF_GAME(false),
        FAILURE(false)
    }

}
