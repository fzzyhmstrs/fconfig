package me.fzzyhmstrs.fzzy_config.util.platform.impl

import me.fzzyhmstrs.fzzy_config.util.platform.RegistrySupplier
import net.minecraft.resources.ResourceKey
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentMap
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.registries.DeferredHolder
import java.util.*

internal class RegistrySupplierImpl<R: Any, T: R>(private val entry: DeferredHolder<R, T>): RegistrySupplier<R> {

    override fun getRegistryKey(): ResourceKey<R> {
        return entry.unwrap().left().get()
    }

    override fun getKey(): ResourceKey<R> {
        return entry.unwrap().left().get()
    }

    override fun getId(): Identifier {
        return entry.id
    }

    override fun getEntry(): Holder<R> {
        return entry
    }

    override fun get(): T {
        return entry.value()
    }

    override fun areComponentsBound(): Boolean {
        return entry.areComponentsBound()
    }

    override fun components(): DataComponentMap {
        return entry.components()
    }

    override fun unwrapKey(): Optional<ResourceKey<R>> {
        return entry.unwrapKey()
    }
}