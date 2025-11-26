/*
* Copyright (c) 2025 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.util.platform

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.Lifecycle
import me.fzzyhmstrs.fzzy_config.cast
import net.minecraft.item.ItemGroup
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.entry.RegistryEntry.Reference
import net.minecraft.registry.entry.RegistryEntryInfo
import net.minecraft.registry.tag.TagKey
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.dynamic.Codecs
import java.util.function.Function

interface RegistryBuilder {

    fun <T: Any> build(key: RegistryKey<Registry<T>>): Registry<T>

    fun <T: Any> buildIntrusive(key: RegistryKey<Registry<T>>): Registry<T>

    fun <T: Any> buildDefaulted(key: RegistryKey<Registry<T>>, defaultId: Identifier): Registry<T>

    fun <T: Any> buildDefaultedIntrusive(key: RegistryKey<Registry<T>>, defaultId: Identifier): Registry<T>

    fun itemGroup(): ItemGroup.Builder

    fun getTagName(tagKey: TagKey<*>): Text

    companion object {
        val INSTANCE: RegistryBuilder = try {
            Class.forName("me.fzzyhmstrs.imbued_sorcery.platform.RegistryBuilderImpl").getDeclaredConstructor().newInstance().cast<RegistryBuilder>()
        } catch (e: Exception) {
            throw IllegalStateException("Couldn't load a Registry Builder for Imbued Sorcery!")
        }

        internal fun <T> Codec<RegistryEntry<T>>.regSupplierCodec(): Codec<RegistryEntry<T>> {
            return this.xmap(Function.identity()) { re -> if (re is RegistrySupplier<T>) re.getEntry().cast() else re }
        }

        internal fun <T> Registry<T>.isCodec(): Codec<T> {

            fun validateReference(entry: RegistryEntry<T>): DataResult<RegistryEntry.Reference<T>> {
                val dataResult: DataResult<RegistryEntry.Reference<T>> = if (entry is RegistryEntry.Reference<T>) {
                    DataResult.success(entry)
                } else {
                    DataResult.error {
                        "Unregistered holder in " + this.key.toString() + ": " + entry.toString()
                    }
                }
                return dataResult
            }

            val idCodec: Codec<Identifier> = Codec.STRING.xmap(
                { s -> if (!s.contains(':')) IS.identity(s) else Identifier.of(s) },
                { i -> if(i.namespace == IS.ID) i.path else i.toString() }
            )

            return idCodec.flatXmap(
                {id -> this.getEntry(id).map{ DataResult.success(it.value()) }.orElseGet{ DataResult.error<T> { "Unknown registry key in " + this.key.toString() + ": " + id.toString() } }},
                { value -> validateReference(this.getEntry(value)).map { it.registryKey().value } }
            )
        }

        internal fun <T: Any> getReferenceEntryCodec(registry: Registry<T>): Codec<RegistryEntry.Reference<T>> {
            val codec: Codec<RegistryEntry.Reference<T>> = Identifier.CODEC
                .comapFlatMap(
                    { id: Identifier ->
                        registry.getEntry(id).map<DataResult<RegistryEntry.Reference<T>>> { result: RegistryEntry.Reference<T> ->
                            DataResult.success(result)
                        }
                            .orElseGet {
                                DataResult.error { "Unknown registry key in " + registry.key + ": " + id }
                            }
                    },
                    { entry: RegistryEntry.Reference<T> ->
                        entry.registryKey().value
                    }
                )
            return Codecs.withLifecycle(
                codec
            ) { entry: RegistryEntry.Reference<T> ->
                registry.getEntryInfo(
                    entry.registryKey()
                ).map { obj: RegistryEntryInfo -> obj.lifecycle() }
                    .orElse(Lifecycle.experimental()) as Lifecycle
            }
        }

        internal fun <T> Registry<T>.getEntryId(entry: RegistryEntry<T>): Identifier? {
            return if (entry is Reference) {
                return entry.registryKey().value
            } else {
                this.getId(entry.value())
            }
        }
    }

}