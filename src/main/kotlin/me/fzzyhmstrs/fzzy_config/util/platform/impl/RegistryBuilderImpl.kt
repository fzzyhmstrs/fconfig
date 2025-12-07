package me.fzzyhmstrs.fzzy_config.util.platform.impl

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.Lifecycle
import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.nsId
import me.fzzyhmstrs.fzzy_config.simpleId
import me.fzzyhmstrs.fzzy_config.util.platform.RegistryBuilder
import me.fzzyhmstrs.fzzy_config.util.platform.RegistrySupplier
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.item.ItemGroup
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.SimpleDefaultedRegistry
import net.minecraft.registry.SimpleRegistry
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.entry.RegistryEntry.Reference
import net.minecraft.registry.entry.RegistryEntryInfo
import net.minecraft.registry.tag.TagKey
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.dynamic.Codecs
import java.util.function.Function

class RegistryBuilderImpl(private val namespace: String): RegistryBuilder {

    override fun <T : Any> build(key: RegistryKey<Registry<T>>): Registry<T> {
        return FabricRegistryBuilder.createSimple(key).buildAndRegister()
    }

    override fun <T : Any> buildDefaulted(key: RegistryKey<Registry<T>>, defaultId: Identifier): Registry<T> {
        return FabricRegistryBuilder.createDefaulted(key, defaultId).buildAndRegister()
    }

    override fun <T : Any> buildIntrusive(key: RegistryKey<Registry<T>>): Registry<T> {
        return FabricRegistryBuilder.from(SimpleRegistry(key, Lifecycle.stable(), true)).buildAndRegister()
    }

    override fun <T : Any> buildDefaultedIntrusive(key: RegistryKey<Registry<T>>, defaultId: Identifier): Registry<T> {
        return FabricRegistryBuilder.from(SimpleDefaultedRegistry<T>(defaultId.toString(), key, Lifecycle.stable(), true)).buildAndRegister()
    }

    override fun itemGroup(): ItemGroup.Builder {
        return FabricItemGroup.builder()
    }

    override fun getTagName(tagKey: TagKey<*>): Text {
        return tagKey.name
    }

    override fun <T> namespaceCodec(registry: Registry<T>): Codec<T> {

        fun validateReference(entry: RegistryEntry<T>): DataResult<Reference<T>> {
            val dataResult: DataResult<Reference<T>> = if (entry is Reference<T>) {
                DataResult.success(entry)
            } else {
                DataResult.error { "Unregistered holder in " + registry.key.toString() + ": " + entry.toString() }
            }
            return dataResult
        }

        val idCodec: Codec<Identifier> = Codec.STRING.xmap(
            { s -> if (!s.contains(':')) namespace.nsId(s) else s.simpleId() },
            { i -> if(i.namespace == namespace) i.path else i.toString() }
        )

        return idCodec.flatXmap(
            {id -> registry.getEntry(id).map{ DataResult.success(it.value()) }.orElseGet{ DataResult.error<T> { "Unknown registry key in " + registry.key.toString() + ": " + id.toString() } }},
            { value -> validateReference(registry.getEntry(value)).map { it.registryKey().value } }
        )
    }

    override fun <T> regSupplierCodec(registry: Registry<T>): Codec<RegistryEntry<T>> {
        return registry.entryCodec.xmap(Function.identity()) { re -> if (re is RegistrySupplier<T>) re.getEntry().cast() else re }
    }

    override fun <T> referenceEntryCodec(registry: Registry<T>): Codec<Reference<T>> {
        val codec: Codec<Reference<T>> = Identifier.CODEC
            .comapFlatMap(
                { id: Identifier ->
                    registry.getEntry(id).map<DataResult<Reference<T>>> { result: Reference<T> ->
                        DataResult.success(result)
                    }.orElseGet { DataResult.error { "Unknown registry key in " + registry.key + ": " + id } }
                },
                { entry: Reference<T> -> entry.registryKey().value }
            )
        return Codecs.withLifecycle(codec) { entry: Reference<T> ->
            registry.getEntryInfo(
                entry.registryKey()
            ).map { obj: RegistryEntryInfo -> obj.lifecycle() }.orElse(Lifecycle.experimental()) as Lifecycle
        }
    }

    override fun <T> getEntryId(registry: Registry<T>, entry: RegistryEntry<T>): Identifier? {
        return if (entry is Reference) {
            return entry.registryKey().value
        } else {
            registry.getId(entry.value())
        }
    }
}