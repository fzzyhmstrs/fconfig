package me.fzzyhmstrs.fzzy_config.util.platform.impl

import me.fzzyhmstrs.fzzy_config.nsId
import me.fzzyhmstrs.fzzy_config.util.platform.Registrar
import me.fzzyhmstrs.fzzy_config.util.platform.RegistrySupplier
import net.minecraft.registry.Registry
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModList
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

internal class RegistrarImpl<T>(private val namespace: String, private val registry: Registry<T>): Registrar<T> {

    companion object {
        private var unboundRegistrars: MutableSet<DeferredRegister<*>> = mutableSetOf()

        internal fun resolveUnbound(bus: IEventBus) {
            for (reg in unboundRegistrars) {
                reg.register(bus)
            }
            unboundRegistrars = mutableSetOf()
        }
    }

    private val deferred = DeferredRegister.create(registry, namespace)

    override fun init() {
        val bus = ModList.get().getModContainerById(namespace)?.get()?.eventBus
        if (bus == null) {
            unboundRegistrars.add(deferred)
            return
        }
        deferred.register(bus)
    }

    override fun register(name: String, entrySupplier: Supplier<T>): RegistrySupplier<T> {
        return RegistrySupplierImpl(deferred.register(name, entrySupplier))
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