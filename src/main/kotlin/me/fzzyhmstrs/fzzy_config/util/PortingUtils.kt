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

import me.fzzyhmstrs.fzzy_config.cast
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.entry.RegistryEntryList
import net.minecraft.registry.tag.TagKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.awt.Color
import java.util.Optional
import java.util.function.Predicate

object PortingUtils {

    fun getWhite(alpha: Float): Int {
        return Color(1f, 1f, 1f, alpha).rgb
    }

    fun PlayerEntity.sendChat(message: Text) {
        this.sendMessage(message, false)
    }

    fun getDynamicManager(player: ServerPlayerEntity): RegistryWrapper.WrapperLookup {
        return player.world.registryManager.cast()
    }

    fun <T> Registry<T>.optional(id: Identifier): Optional<T> {
        return this.getOrEmpty(id)
    }

    fun RegistryWrapper.WrapperLookup.anyOptional(key: RegistryKey<out Registry<*>>):  Optional<out RegistryWrapper.Impl<*>> {
        return this.getOptionalWrapper(key)
    }

    fun <T> RegistryWrapper.WrapperLookup.optional(key: RegistryKey<out Registry<T>>):  Optional<out RegistryWrapper.Impl<T>> {
        return this.getOptionalWrapper(key)
    }

    fun <T> Registry<T>.tagIdList(): List<Identifier> {
        return this.streamTags().map { it.id }.toList()
    }

    fun <T> Registry<T>.tagIdList(predicate: Predicate<Identifier>? = null): List<Identifier> {
        return if(predicate == null)
            this.streamTags().map { it.id }.toList()
        else
            this.streamTags().map { it.id }.filter(predicate).toList()
    }

    fun <T> RegistryWrapper.Impl<T>.tagIdList(predicate: Predicate<Identifier>? = null): List<Identifier> {
        return if(predicate == null)
            this.streamTags().map { it.tag.id }.toList()
        else
            this.streamTags().map { it.tag.id }.filter(predicate).toList()
    }

    fun <T> Registry<T>.namedEntryList(tagKey: TagKey<T>): Optional<RegistryEntryList.Named<T>> {
        return this.getEntryList(tagKey)
    }

    fun <T> TagKey<T>.regRef(): RegistryKey<out Registry<T>> {
        return this.registry
    }

    fun TagKey<*>.regRefId(): Identifier {
        return this.registry.value
    }
}