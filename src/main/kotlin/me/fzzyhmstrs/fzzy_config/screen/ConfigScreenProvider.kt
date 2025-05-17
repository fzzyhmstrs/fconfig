/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen

import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen

/**
 * Provider of config screens for a particular namespace. This is used to provide custom screens in place of the default Fzzy Config implementation.
 *
 * Register your implementation using [ConfigApi.registerScreenProvider] or [ConfigApiJava.registerScreenProvider]
 *
 * This is one of two ways to disable the standard screen for a scope set; the other being registering a config using [ConfigApi.registerAndLoadNoGuiConfig] or [ConfigApiJava.registerAndLoadNoGuiConfig], which would allow you to edit your config in a completely custom way not linked to the standard `openScreen` methods. The built in configs for keybinds and search settings use this method and open a custom popup instead.
 * @author fzzyhmstrs
 * @since 0.7.0
 */
//client
@FunctionalInterface
fun interface ConfigScreenProvider {
    /**
     * Provides a [Screen] instance for use by something that may open it later (ModMenu for example). Return null if no relevant config screen should be opened.
     * @param namespace The string id used to register this provider; usually the mod id. Provided for verification.
     * @param scope String scope requesting to be opened. This will generally be something like "mod_id.config_name" [See the wiki for an overview of scope](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/Translation#-example)
     * @return A screen if a relevant scope is requested, null otherwise.
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun provideScreen(namespace: String, scope: String): Screen?
    /**
     * Opens a [Screen] based on the provided scope. The default method simply opens the screen. You may want to reimplement this to, for example, pass the previous screen into the newly created screen so it can go back to that screen when it closes.
     * @param namespace The string id used to register this provider; usually the mod id. Provided for verification.
     * @param scope String scope requesting to be opened. This will generally be something like "mod_id.config_name" [See the wiki for an overview of scope](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/Translation#-example)
     * @return true if a screen was successfully opened, false otherwise
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    fun openScreen(namespace: String, scope: String): Boolean {
        val screen = provideScreen(namespace, scope) ?: return false
        MinecraftClient.getInstance().setScreen(screen)
        return true
    }
}