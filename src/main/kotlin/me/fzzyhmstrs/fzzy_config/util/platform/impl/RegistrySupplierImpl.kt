package me.fzzyhmstrs.fzzy_config.util.platform.impl

import me.fzzyhmstrs.fzzy_config.util.platform.RegistrySupplier
import net.minecraft.resources.ResourceKey
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentMap
import net.minecraft.resources.Identifier
import java.util.*

internal class RegistrySupplierImpl<T: Any>(private val entry: Holder.Reference<T>): RegistrySupplier<T> {

    override fun getRegistryKey(): ResourceKey<T> {
        return entry.key()
    }

    override fun areComponentsBound(): Boolean {
        return entry.areComponentsBound()
    }

    override fun components(): DataComponentMap {
        return entry.components()
    }

    override fun unwrapKey(): Optional<ResourceKey<T>> {
        return entry.unwrapKey()
    }

    override fun getId(): Identifier {
        return entry.key().identifier()
    }

    override fun getEntry(): Holder<T> {
        return entry
    }

    override fun get(): T {
        return entry.value()
    }
}