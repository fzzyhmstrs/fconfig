/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(method = "onPlayerConnect", at = @At(value = "INVOKE", target = "net/minecraft/server/network/ServerPlayNetworkHandler.sendPacket (Lnet/minecraft/network/packet/Packet;)V", ordinal = 0, shift = At.Shift.AFTER))
    private void fzzy_config_syncConfigs(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci, @Local ServerPlayNetworkHandler serverPlayNetworkHandler) {
        SyncedConfigRegistry.INSTANCE.syncConfigs(serverPlayNetworkHandler);
    }

}