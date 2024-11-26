package me.fzzyhmstrs.fzzy_config.util.platform.impl

import me.fzzyhmstrs.fzzy_config.nsId
import me.fzzyhmstrs.fzzy_config.util.platform.Registrar
import me.fzzyhmstrs.fzzy_config.util.platform.RegistrySupplier
import net.minecraft.registry.Registry
import java.util.function.Supplier

class RegistrarImpl<T>(private val namespace: String, private val registry: Registry<T>): Registrar<T> {

    override fun register(name: String, entrySupplier: Supplier<T>): RegistrySupplier<T> {
        return RegistrySupplierImpl(Registry.registerReference(registry, namespace.nsId(name), entrySupplier.get()))
    }
}