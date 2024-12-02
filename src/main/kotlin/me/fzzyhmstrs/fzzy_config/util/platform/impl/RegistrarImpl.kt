package me.fzzyhmstrs.fzzy_config.util.platform.impl

import me.fzzyhmstrs.fzzy_config.nsId
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.util.platform.Registrar
import me.fzzyhmstrs.fzzy_config.util.platform.RegistrySupplier
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.javafmlmod.FMLModContainer
import net.minecraftforge.registries.DeferredRegister
import java.util.function.Supplier
import kotlin.jvm.optionals.getOrNull

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

    private val deferred = DeferredRegister.create(registry.key, namespace)

    override fun init() {
        val bus = ModList.get().getModContainerById(namespace)?.getOrNull()?.nullCast<FMLModContainer>()?.eventBus
        if (bus == null) {
            unboundRegistrars.add(deferred)
            return
        }
        deferred.register(bus)
    }

    override fun register(name: String, entrySupplier: Supplier<T>): RegistrySupplier<T> {
        return RegistrySupplierImpl(deferred.register(name, entrySupplier), RegistryKey.of(registry.key, Identifier(namespace, name)))
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