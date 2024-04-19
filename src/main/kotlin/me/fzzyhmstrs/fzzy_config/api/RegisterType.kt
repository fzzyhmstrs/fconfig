/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.api

/**
 * Defines the registries the config is registered to.
 *
 * Default is [BOTH]
 * @author fzzyhmstrs
 * @since 0.2.0
 */
enum class RegisterType {
    /**
     * Default registration type. Config is registered to both the SyncedConfigRegistry and the ClientConfigRegistry.
     *
     * Will auto-synchronize between clients and server, and will have a config GUI.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    BOTH,
    /**
     * No-GUI server-synced config. Config is registered to only the SyncedConfigRegistry.
     *
     * Will auto-synchronize between clients and server, but will NOT have any client sided GUI support.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    SERVER,
    /**
     * Client config. Config is registered to only the ClientConfigRegistry.
     *
     * will have a config GUI, but will not auto-synchronize. Some syncing actions will still work, namely setting forwarding.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    CLIENT
}