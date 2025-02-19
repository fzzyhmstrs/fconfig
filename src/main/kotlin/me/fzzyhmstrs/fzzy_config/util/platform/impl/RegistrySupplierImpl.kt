package me.fzzyhmstrs.fzzy_config.util.platform.impl

import me.fzzyhmstrs.fzzy_config.util.platform.RegistrySupplier
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.Identifier
import net.minecraftforge.registries.RegistryObject
import java.util.*

internal class RegistrySupplierImpl<T>(private val entry: RegistryObject<T>, private val key: RegistryKey<T>): RegistrySupplier<T> {

    override fun getRegistryKey(): RegistryKey<T> {
        return key
    }

    override fun getKey(): Optional<RegistryKey<T>> {
        return Optional.of(key)
    }

    override fun getId(): Identifier {
        return entry.id
    }

    override fun getEntry(): RegistryEntry<T> {
        return entry.holder.orElseThrow()
    }

    override fun get(): T {
        return entry.get()
    }
}