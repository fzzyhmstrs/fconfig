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

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry
import me.fzzyhmstrs.fzzy_config.screen.internal.RestartScreen
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import java.util.*
import kotlin.collections.HashMap

internal object ConfigApiImplClient {

    private val ellipses by lazy {
        FcText.literal("...")
    }

    private val ellipsesWidth by lazy {
        MinecraftClient.getInstance().textRenderer.getWidth(ellipses)
    }

    fun ellipses(input: Text, maxWidth: Int): Text {
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

    internal fun openScreen(scope: String) {
        ClientConfigRegistry.openScreen(scope)
    }
    internal fun openRestartScreen() {
        MinecraftClient.getInstance().setScreen(RestartScreen())
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
}