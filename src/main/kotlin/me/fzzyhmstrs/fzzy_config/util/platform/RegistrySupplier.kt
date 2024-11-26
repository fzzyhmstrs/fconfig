package me.fzzyhmstrs.fzzy_config.util.platform

import net.minecraft.registry.RegistryKey
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.Identifier
import java.util.function.Supplier

interface RegistrySupplier<T>: Supplier<T> {

    fun getKey(): RegistryKey<T>

    fun getId(): Identifier

    fun getEntry(): RegistryEntry<T>
}