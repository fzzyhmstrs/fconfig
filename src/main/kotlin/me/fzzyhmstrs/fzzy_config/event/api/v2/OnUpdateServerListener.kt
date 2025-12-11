/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.event.api.v2

import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.event.api.ServerUpdateContext
import net.minecraft.util.Identifier

/**
 * Listener for on-changed events on the server side. Will be fired for any and all updated configs, allowing inspection of other configs. For use in your own configs, consider directly implementing [Config.onUpdateServer][me.fzzyhmstrs.fzzy_config.config.Config.onUpdateServer]. Common code should also use this, as server configs are loaded on both sides regardless of environment (they are not technically "server" configs, they are "synced" configs).
 *
 * Register with [EventApi.onUpdateServer][me.fzzyhmstrs.fzzy_config.event.api.EventApi.onUpdateServer]
 * @author fzzyhmstrs
 * @since 0.7.4
 */
@FunctionalInterface
fun interface OnUpdateServerListener {

    /**
     * Called by the `onChangedServer` event when the server side of a config is changed.
     * @param id [Identifier] the registered id attached to the config instance.
     * @param config [Config] the config instance. This should only be read, or changes only made to transient fields/methods. Making updates to settings here will NOT be captured by the synchronization system.
     * @param context [ServerUpdateContext] information regarding the server and possibly player involved with the update
     * @author fzzyhmstrs
     * @since 0.7.4
     */
    fun onChanged(id: Identifier, config: Config, context: ServerUpdateContext)
}