package me.fzzyhmstrs.fzzy_config.util.platform.impl

import me.fzzyhmstrs.fzzy_config.nsId
import me.fzzyhmstrs.fzzy_config.util.platform.Registrar
import me.fzzyhmstrs.fzzy_config.util.platform.RegistrySupplier
import net.minecraft.registry.Registry
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import java.util.function.Supplier

internal class RegistrarImpl<T>(private val namespace: String, private val registry: Registry<T>): Registrar<T> {

    override fun init() {
        //fabric needs nothing
    }

    override fun register(name: String, entrySupplier: Supplier<out T>): RegistrySupplier<T> {
        return RegistrySupplierImpl(Registry.registerReference(registry, namespace.nsId(name), entrySupplier.get()))
    }

    override fun getRegistry(): Registry<T> {
        return registry
    }

    override fun createTag(path: String): TagKey<T> {
        return TagKey.of(registry.key, namespace.nsId(path))
    }

    override fun createTag(id: Identifier): TagKey<T> {
        return TagKey.of(registry.key, id)
    }
}