/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.util

import java.io.File

/**
 * API for abstraction of simple ModLoader requests
 * @author fzzyhmstrs
 * @since 0.5.0
 */
interface PlatformApi {

    /**
     * Whether the game includes a logical client or not. This will be true both for singleplayer games and the client side of a multiplayer game.
     * @return true if a logical client is present (so you can access client code like MinecraftClient), false if the environment is a dedicated server.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun isClient(): Boolean

    /**
     * The config directory
     * @return [File] respresenting the standard config directory inside the game folder.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun configDir(): File

    /**
     * The root game directory
     * @return [File] representing the path of the root game directory.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun gameDir(): File

    /**
     * Returns whether another mod is loaded based on their registered mod_id.
     * @return true if the mod is present, false otherwise
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    fun isModLoaded(mod: String): Boolean
}