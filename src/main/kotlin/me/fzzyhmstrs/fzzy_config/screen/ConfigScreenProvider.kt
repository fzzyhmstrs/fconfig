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

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen

/**
 * TODO()
 * @author fzzyhmstrs
 * @since 0.7.0
 */
//client
@FunctionalInterface
fun interface ConfigScreenProvider {
    fun provideScreen(namespace: String, scope: String): Screen?
    fun openScreen(namespace: String, scope: String): Boolean {
        val screen = provideScreen(namespace, scope) ?: return false
        MinecraftClient.getInstance().setScreen(screen)
        return true
    }
}