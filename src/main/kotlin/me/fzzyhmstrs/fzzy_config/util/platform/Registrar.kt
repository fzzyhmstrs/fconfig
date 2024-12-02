package me.fzzyhmstrs.fzzy_config.util.platform

import net.minecraft.registry.Registry
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import org.jetbrains.annotations.ApiStatus
import java.util.function.Supplier

@ApiStatus.Experimental
interface Registrar<T> {

    fun init()

    fun register(name: String, entrySupplier: Supplier<T>): RegistrySupplier<T>

    fun getRegistry(): Registry<T>

    fun createTag(path: String): TagKey<T>

    fun createTag(id: Identifier): TagKey<T>
}