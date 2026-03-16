package me.fzzyhmstrs.fzzy_config.util.platform.impl

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.Lifecycle
import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.nsId
import me.fzzyhmstrs.fzzy_config.simpleId
import me.fzzyhmstrs.fzzy_config.util.platform.RegistryBuilder
import me.fzzyhmstrs.fzzy_config.util.platform.RegistrySupplier
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.core.DefaultedMappedRegistry
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Holder
import net.minecraft.core.Holder.Reference
import net.minecraft.core.RegistrationInfo
import net.minecraft.tags.TagKey
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.ExtraCodecs
import java.util.function.Function

class RegistryBuilderImpl(private val namespace: String): RegistryBuilder {

    override fun <T : Any> build(key: ResourceKey<Registry<T>>): Registry<T> {
        return FabricRegistryBuilder.create(key).buildAndRegister()
    }

    override fun <T : Any> buildDefaulted(key: ResourceKey<Registry<T>>, defaultId: Identifier): Registry<T> {
        return FabricRegistryBuilder.createDefaulted(key, defaultId).buildAndRegister()
    }

    override fun <T : Any> buildIntrusive(key: ResourceKey<Registry<T>>): Registry<T> {
        return FabricRegistryBuilder.from(MappedRegistry(key, Lifecycle.stable(), true)).buildAndRegister()
    }

    override fun <T : Any> buildDefaultedIntrusive(key: ResourceKey<Registry<T>>, defaultId: Identifier): Registry<T> {
        return FabricRegistryBuilder.from(DefaultedMappedRegistry<T>(defaultId.toString(), key, Lifecycle.stable(), true)).buildAndRegister()
    }

    override fun itemGroup(): CreativeModeTab.Builder {
        return FabricCreativeModeTab.builder()
    }

    override fun getTagName(tagKey: TagKey<*>): Component {
        return tagKey.name
    }

    override fun <T: Any> namespaceCodec(registry: Registry<T>): Codec<T> {

        fun validateReference(entry: Holder<T>): DataResult<Reference<T>> {
            val dataResult: DataResult<Reference<T>> = if (entry is Reference<T>) {
                DataResult.success(entry)
            } else {
                DataResult.error { "Unregistered holder in " + registry.key().toString() + ": " + entry.toString() }
            }
            return dataResult
        }

        val idCodec: Codec<Identifier> = Codec.STRING.xmap(
            { s -> if (!s.contains(':')) namespace.nsId(s) else s.simpleId() },
            { i -> if(i.namespace == namespace) i.path else i.toString() }
        )

        return idCodec.flatXmap(
            {id -> registry.get(id).map{ DataResult.success(it.value()) }.orElseGet{ DataResult.error<T> { "Unknown registry key in " + registry.key()
                .toString() + ": " + id.toString() } }},
            { value -> validateReference(registry.wrapAsHolder(value)).map { it.key().identifier() } }
        )
    }

    override fun <T: Any> regSupplierCodec(registry: Registry<T>): Codec<Holder<T>> {
        return registry.holderByNameCodec().xmap(Function.identity()) { re -> if (re is RegistrySupplier<T>) re.getEntry().cast() else re }
    }

    override fun <T: Any> referenceEntryCodec(registry: Registry<T>): Codec<Reference<T>> {
        val codec: Codec<Reference<T>> = Identifier.CODEC
            .comapFlatMap(
                { id: Identifier ->
                    registry.get(id).map<DataResult<Reference<T>>> { result: Reference<T> ->
                        DataResult.success(result)
                    }.orElseGet { DataResult.error { "Unknown registry key in " + registry.key() + ": " + id } }
                },
                { entry: Reference<T> -> entry.key().identifier() }
            )
        return ExtraCodecs.overrideLifecycle(codec) { entry: Reference<T> ->
            registry.registrationInfo(
                entry.key()
            ).map { obj: RegistrationInfo -> obj.lifecycle() }.orElse(Lifecycle.experimental()) as Lifecycle
        }
    }

    override fun <T: Any> getEntryId(registry: Registry<T>, entry: Holder<T>): Identifier? {
        return if (entry is Reference) {
            return entry.key().identifier()
        } else {
            registry.getKey(entry.value())
        }
    }
}