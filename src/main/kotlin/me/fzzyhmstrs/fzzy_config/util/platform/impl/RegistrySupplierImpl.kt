package me.fzzyhmstrs.fzzy_config.util.platform.impl

import me.fzzyhmstrs.fzzy_config.util.platform.RegistrySupplier
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.Identifier
import net.neoforged.neoforge.registries.DeferredHolder

internal class RegistrySupplierImpl<R, T: R>(private val entry: DeferredHolder<R, T>): RegistrySupplier<R> {

    override fun getKey(): RegistryKey<R> {
        return entry.keyOrValue.left().get()
    }

    override fun getId(): Identifier {
        return entry.id
    }

    override fun getEntry(): RegistryEntry<R> {
        return entry
    }

    override fun get(): T {
        return entry.value()
    }
}