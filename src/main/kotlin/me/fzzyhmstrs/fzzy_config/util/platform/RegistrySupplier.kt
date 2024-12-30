package me.fzzyhmstrs.fzzy_config.util.platform

import net.minecraft.registry.RegistryKey
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.Identifier
import org.jetbrains.annotations.ApiStatus
import java.util.function.Supplier

//TODO
@ApiStatus.Experimental
interface RegistrySupplier<T>: Supplier<T> {

    //TODO
    fun getKey(): RegistryKey<T>

    //TODO
    fun getId(): Identifier

    //TODO
    fun getEntry(): RegistryEntry<T>
}