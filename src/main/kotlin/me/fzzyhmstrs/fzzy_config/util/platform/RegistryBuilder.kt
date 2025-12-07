/*
* Copyright (c) 2025 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.util.platform

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.Lifecycle
import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.nsId
import me.fzzyhmstrs.fzzy_config.simpleId
import net.minecraft.item.ItemGroup
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.entry.RegistryEntry.Reference
import net.minecraft.registry.entry.RegistryEntryInfo
import net.minecraft.registry.tag.TagKey
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.dynamic.Codecs
import java.util.function.Function

/**
 * A utility for creating custom registries. Also includes some helper methods for interacting with registries
 * @author fzzyhmstrs
 * @since 0.7.4
 */
interface RegistryBuilder {

    /**
     * Creates a Simple Registry.
     *
     * | Feature | Included |
     * |---------|----------|
     * | Default value | false |
     * | Crashes if object not registered | false |
     *
     * @param T type of object to be stored in the registry
     * @param key [RegistryKey] key for the registry being built
     * @return built registry
     * @author fzzyhmstrs
     * @since 0.7.4
     */
    fun <T: Any> build(key: RegistryKey<Registry<T>>): Registry<T>

    /**
     * Creates an Intrusive Registry.
     *
     * | Feature | Included |
     * |---------|----------|
     * | Default value | false |
     * | Crashes if object not registered | true |
     *
     * @param T type of object to be stored in the registry
     * @param key [RegistryKey] key for the registry being built
     * @return built registry
     * @author fzzyhmstrs
     * @since 0.7.4
     */
    fun <T: Any> buildIntrusive(key: RegistryKey<Registry<T>>): Registry<T>

    /**
     * Creates a Defaulted Registry.
     *
     * | Feature | Included |
     * |---------|----------|
     * | Default value | true |
     * | Crashes if object not registered | false |
     *
     * @param T type of object to be stored in the registry
     * @param key [RegistryKey] key for the registry being built
     * @param defaultId [Identifier] id for the default value of the registry. This value needs to be registered separately.
     * @return built registry
     * @author fzzyhmstrs
     * @since 0.7.4
     */
    fun <T: Any> buildDefaulted(key: RegistryKey<Registry<T>>, defaultId: Identifier): Registry<T>

    /**
     * Creates a Defaulted Intrusive Registry.
     *
     * | Feature | Included |
     * |---------|----------|
     * | Default value | true |
     * | Crashes if object not registered | true |
     *
     * @param T type of object to be stored in the registry
     * @param key [RegistryKey] key for the registry being built
     * @param defaultId [Identifier] id for the default value of the registry. This value needs to be registered separately.
     * @return built registry
     * @author fzzyhmstrs
     * @since 0.7.4
     */
    fun <T: Any> buildDefaultedIntrusive(key: RegistryKey<Registry<T>>, defaultId: Identifier): Registry<T>

    /**
     * Makes a blank [ItemGroup.Builder] for creating an item group as needed.
     * @author fzzyhmstrs
     * @since 0.7.4
     */
    fun itemGroup(): ItemGroup.Builder

    /**
     * Returns the translated name for a tag, where available.
     * @return [Text] translated name for this tag. Falls back to `#namespace:path` if a translation isn't available
     * @author fzzyhmstrs
     * @since 0.7.4
     */
    fun getTagName(tagKey: TagKey<*>): Text

    /**
     * Creates a registry value codec that allows for identifier shortcutting based on the namespace used to build this builder. Vanilla will shorten ids for the `minecraft` namespace (`minecraft:thing` to `thing`). This does the same thing but for the namespace provided.
     *
     * This is useful for creating more concise data formats with many registered "keys" without having to resort to registering those keys to the MC namespace.
     * @param T type of object stored in the registry
     * @param registry [Registry] Registry to build the codec off of.
     * @return [Codec] for values of [T] that will shorten output strings if the id namepace is the one you built this builder with
     * @author fzzyhmstrs
     * @since 0.7.4
     */
    fun <T> namespaceCodec(registry: Registry<T>): Codec<T>

    /**
     * Creates a [RegistryEntry] codec that automatically handles [RegistrySupplier] properly
     * @param T type of object stored in the registry
     * @param registry [Registry] Registry to build the codec off of.
     * @return [Codec] for [RegistryEntry] ot [T] that will properly parse [RegistrySupplier] without needing to unwrap it first. The decoded entry will *not* be a [RegistrySupplier] in that case. It will be a standard [RegistryEntry.Reference].
     * @author fzzyhmstrs
     * @since 0.7.4
     */
    fun <T> regSupplierCodec(registry: Registry<T>): Codec<RegistryEntry<T>>

    /**
     * Creates an entry codec for a registry that works with [RegistryEntry.Reference] directly, allowing you to work with the methods of that class directly.
     * @param T type of object stored in the registry
     * @param registry [Registry] Registry to build the codec off of.
     * @return [Codec] for [RegistryEntry.Reference] ot [T]
     * @author fzzyhmstrs
     * @since 0.7.4
     */
    fun <T> referenceEntryCodec(registry: Registry<T>): Codec<Reference<T>>

    /**
     * Helper method for optimally extracting ids out of a [RegistryEntry]
     * @param [T] type of object stored in the registry
     * @param registry [Registry] Registry to get the id out of.
     * @param entry [RegistryEntry] the entry to get the id for.
     * @return [Identifier], nullable. If the entry is a [RegistryEntry.Reference], it will directly return it's stored id, otherwise it will search the registry for it, possibly returning null if you supply a [RegistryEntry.Direct]
     * @author fzzyhmstrs
     * @since 0.7.4
     */
    fun <T> getEntryId(registry: Registry<T>, entry: RegistryEntry<T>): Identifier?
}