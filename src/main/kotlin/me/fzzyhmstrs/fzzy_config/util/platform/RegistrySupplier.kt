package me.fzzyhmstrs.fzzy_config.util.platform

import net.minecraft.registry.RegistryKey
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.Identifier
import org.jetbrains.annotations.ApiStatus
import java.util.function.Supplier

@ApiStatus.Experimental
interface RegistrySupplier<T>: Supplier<T> {

    fun getKey(): RegistryKey<T>

    fun getId(): Identifier

    fun getEntry(): RegistryEntry<T>
}