package me.fzzyhmstrs.fzzy_config.util.platform.impl

import me.fzzyhmstrs.fzzy_config.util.platform.RegistrySupplier
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.Identifier
import java.util.*

internal class RegistrySupplierImpl<T>(private val entry: RegistryEntry.Reference<T>): RegistrySupplier<T> {

    override fun getRegistryKey(): RegistryKey<T> {
        return entry.registryKey()
    }

    override fun getKey(): Optional<RegistryKey<T>> {
        return entry.key
    }

    override fun getId(): Identifier {
        return entry.registryKey().value
    }

    override fun getEntry(): RegistryEntry<T> {
        return entry
    }

    override fun get(): T {
        return entry.value()
    }
}