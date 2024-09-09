/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.networking.api

import me.fzzyhmstrs.fzzy_config.networking.FzzyPayload
import net.minecraft.server.network.ServerPlayerEntity

/**
 * Handler for a server receiving a payload sent from a client
 * @param T the type of payload being handled
 * @author fzzyhmstrs
 * @since 0.4.1
 */
@FunctionalInterface
fun interface C2SPayloadHandler<T: FzzyPayload>: PayloadHandler<T, ServerPlayerEntity, ServerPlayNetworkContext> {
    /**
     * Handles a payload sent from a client
     * @param payload the [FzzyPayload] for handling
     * @param context [ServerPlayNetworkContext] a wrapper around platform context, used for getting various useful info and performing actions like replying to the payload
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    override fun handle(payload: T, context: ServerPlayNetworkContext)
}