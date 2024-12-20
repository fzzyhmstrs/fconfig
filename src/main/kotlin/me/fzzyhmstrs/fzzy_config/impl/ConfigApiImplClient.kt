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

import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.annotations.Translation
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import me.fzzyhmstrs.fzzy_config.screen.internal.RestartScreen
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.descSupplied
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.transSupplied
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import net.minecraft.client.MinecraftClient
import net.minecraft.client.resource.language.I18n
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlComment
import java.util.*

internal object ConfigApiImplClient {

    private val ellipses by lazy {
        FcText.literal("...")
    }

    private val ellipsesWidth by lazy {
        MinecraftClient.getInstance().textRenderer.getWidth(ellipses)
    }

    internal fun ellipses(input: Text, maxWidth: Int): Text {
        return if (MinecraftClient.getInstance().textRenderer.getWidth(input) <= maxWidth)
            input
        else
            MinecraftClient.getInstance().textRenderer.trimToWidth(input.string, maxWidth - ellipsesWidth).trimEnd().lit().append(ellipses)
    }

    internal fun getPerms(): Map<String, Map<String, Boolean>> {
        return HashMap(ClientConfigRegistry.getPerms())
    }

    internal fun updatePerms(id: String, perms: Map<String, Boolean>) {
        ClientConfigRegistry.updatePerms(id, perms)
    }

    internal fun registerConfig(config: Config, baseConfig: Config) {
        ClientConfigRegistry.registerConfig(config, baseConfig)
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
        ClientConfigRegistry.openScreen(scope)
    }

    internal fun openRestartScreen(): Boolean {
        if (MinecraftClient.getInstance().currentScreen is RestartScreen) return false
        MinecraftClient.getInstance().setScreen(RestartScreen())
        return true
    }

    internal fun handleForwardedUpdate(update: String, player: UUID, scope: String, summary: String) {
        ClientConfigRegistry.handleForwardedUpdate(update, player, scope, summary)
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

    internal fun getTranslation(thing: Any, fieldName: String, annotations: List<Annotation>, globalAnnotations: List<Annotation>, fallback: String = fieldName): MutableText {
        for (annotation in annotations) {
            if (annotation is Translation) {
                for (ga in globalAnnotations) {
                    if (ga is Translation && ga.negate) {
                        return thing.transSupplied { fallback.split(FcText.regex).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } } }
                    }
                }
                if (annotation.negate) {
                    return thing.transSupplied { fallback.split(FcText.regex).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } } }
                }
                val key = if(fieldName.isNotEmpty()) "${annotation.prefix}.$fieldName" else annotation.prefix
                if (I18n.hasTranslation(key)) return key.translate()
                break
            }
        }
        for (annotation in globalAnnotations) {
            if (annotation is Translation) {
                val key = if(fieldName.isNotEmpty()) "${annotation.prefix}.$fieldName" else annotation.prefix
                if (I18n.hasTranslation(key)) return key.translate()
                break
            }
        }
        return thing.transSupplied { fallback.split(FcText.regex).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } } }
    }

    internal fun getDescription(thing: Any, fieldName: String, annotations: List<Annotation>, globalAnnotations: List<Annotation>): MutableText {
        for (annotation in annotations) {
            if (annotation is Translation) {
                if (annotation.negate) {
                    return thing.descSupplied { getComments(annotations) }
                }
                val key = if(fieldName.isNotEmpty()) "${annotation.prefix}.$fieldName.desc" else "${annotation.prefix}.desc"
                if (I18n.hasTranslation(key)) return key.translate()
                break
            }
        }
        for (annotation in globalAnnotations) {
            if (annotation is Translation) {
                val key = if(fieldName.isNotEmpty()) "${annotation.prefix}.$fieldName.desc" else "${annotation.prefix}.desc"
                if (I18n.hasTranslation(key)) return key.translate()
                break
            }
        }
        return thing.descSupplied { getComments(annotations) }
    }

    private val spacer = ". "

    private fun getComments(annotations: List<Annotation>): String {
        val comment = StringBuilder()
        for (annotation in annotations) {
            if (annotation is TomlComment) {
                if (comment.isNotEmpty())
                    comment.append(spacer)
                comment.append(annotation.text)
            } else if(annotation is Comment) {
                if (comment.isNotEmpty())
                    comment.append(spacer)
                comment.append(annotation.value)
            }
        }
        if (comment.isNotEmpty())
            comment.append(".")
        return comment.toString()
    }
}