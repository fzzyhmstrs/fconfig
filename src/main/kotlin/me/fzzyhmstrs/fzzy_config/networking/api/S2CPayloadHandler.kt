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

import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.network.packet.CustomPayload

/**
 * Handler for a client receiving a payload sent from the server
 * @param T the type of payload being handled
 * @author fzzyhmstrs
 * @since 0.4.1
 */
@FunctionalInterface
fun interface S2CPayloadHandler<T: CustomPayload>: PayloadHandler<T, ClientPlayerEntity, ClientPlayNetworkContext> {
    /**
     * Handles a payload sent from the server
     * @param payload the [CustomPayload] for handling
     * @param context [ClientPlayNetworkContext] a wrapper around platform context, used for getting various useful info and performing actions like replying to the payload
     * @author fzzyhmstrs
     * @since 0.4.1
     */
    override fun handle(payload: T, context: ClientPlayNetworkContext)
}