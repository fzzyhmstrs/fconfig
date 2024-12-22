/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.validation.minecraft

import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import net.minecraft.registry.DefaultedRegistry
import net.minecraft.registry.Registry
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.Identifier
import java.util.function.BiPredicate
import java.util.function.Predicate

/**
 * Helper to create validation for Registered objects, specifically things in a [DefaultedRegistry] like Items and Blocks.
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.registries
 * @see ValidatedField.map
 * @author fzzyhmstrs
 * @since 0.5.0
 */
object ValidatedRegistryType {

    /**
     * Validation for a registry object in a [DefaultedRegistry]
     *
     * This validation will accept any instance of an object that is registered in the provided registry. Will use the registries default value as its default.
     * @param T the object type
     * @param registry [DefaultedRegistry]&lt;[T]&gt; - the registry instance
     * @return [ValidatedField]&lt;[T]&gt; - Validation wrapping the registry. Is a [Supplier][java.util.function.Supplier] and [Consumer][java.util.function.Consumer] of the current instance of [T] it is holding
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    @JvmStatic
    fun <T: Any> of(registry: DefaultedRegistry<T>): ValidatedField<T> {
        return ValidatedIdentifier.ofRegistry(registry.defaultId, registry).map(
            { id -> registry.get(id) },
            { t -> registry.getId(t) }
        )
    }

    /**
     * Validation for a registry object in a [Registry], the Registry doesn't have to be Defaulted because a default is being provided.
     *
     * This validation will accept any instance of an object that is registered in the provided registry.
     * @param T the object type
     * @param defaultValue [T] default registry object instance
     * @param registry [Registry]&lt;[T]&gt; - the registry instance
     * @return [ValidatedField]&lt;[T]&gt; - Validation wrapping the registry. Is a [Supplier][java.util.function.Supplier] and [Consumer][java.util.function.Consumer] of the current instance of [T] it is holding
     * @throws IllegalStateException If the default provided isn't actually registered in the registry.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    @JvmStatic
    fun <T: Any> of(defaultValue: T, registry: Registry<T>): ValidatedField<T> {
        @Suppress("DEPRECATION")
        return ValidatedIdentifier.ofRegistry(registry).map(
            defaultValue,
            { id -> registry.get(id) ?: defaultValue },
            { t -> registry.getId(t) ?: registry.getId(defaultValue) ?: throw IllegalStateException("Entry $t not in registry ${registry.key}") }
        )
    }

    /**
     * Validation for a registry object in a [Registry], the Registry doesn't have to be Defaulted because a default is being provided.
     * @param T the object type
     * @param defaultValue [T] default registry object instance
     * @param registry [Registry]&lt;[T]&gt; - the registry instance
     * @param predicate Predicate&lt;[RegistryEntry]&lt;[T]&gt;&gt; - filters the allowable registry entries
     * @return [ValidatedField]&lt;[T]&gt; - Validation wrapping the registry. Is a [Supplier][java.util.function.Supplier] and [Consumer][java.util.function.Consumer] of the current instance of [T] it is holding
     * @throws IllegalStateException If the default provided isn't actually registered in the registry.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    @JvmStatic
    fun <T: Any> of(defaultValue: T, registry: Registry<T>, predicate: Predicate<RegistryEntry<T>>): ValidatedField<T> {
        @Suppress("DEPRECATION")
        return ValidatedIdentifier.ofRegistry(registry, predicate).map(
            defaultValue,
            { id -> registry.get(id) ?: defaultValue },
            { t -> registry.getId(t) ?: registry.getId(defaultValue) ?: throw IllegalStateException("Entry $t not in registry ${registry.key}") }
        )
    }

    /**
     * Validation for a registry object in a [Registry], the Registry doesn't have to be Defaulted because a default is being provided.
     *
     * This validation will accept any instance of an object that is registered in the provided registry.
     * @param T the object type
     * @param defaultValue [T] default registry object instance
     * @param registry [Registry]&lt;[T]&gt; - the registry instance
     * @param predicate [BiPredicate]&lt;[Identifier], [RegistryEntry]&lt;[T]&gt;&gt; - filters the allowable registry entries
     * @return [ValidatedField]&lt;[T]&gt; - Validation wrapping the registry. Is a [Supplier][java.util.function.Supplier] and [Consumer][java.util.function.Consumer] of the current instance of [T] it is holding
     * @throws IllegalStateException If the default provided isn't actually registered in the registry.
     * @author fzzyhmstrs
     * @since 0.5.0
     */
    @JvmStatic
    fun <T: Any> of(defaultValue: T, registry: Registry<T>, predicate: BiPredicate<Identifier, RegistryEntry<T>>): ValidatedField<T> {
        @Suppress("DEPRECATION")
        return ValidatedIdentifier.ofRegistry(registry, predicate).map(
            defaultValue,
            { id -> registry.get(id) ?: defaultValue },
            { t -> registry.getId(t) ?: registry.getId(defaultValue) ?: throw IllegalStateException("Entry $t not in registry ${registry.key}") }
        )
    }

}