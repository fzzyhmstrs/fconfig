package me.fzzyhmstrs.fzzy_config.util.platform

import net.minecraft.registry.Registry
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import org.jetbrains.annotations.ApiStatus
import java.util.function.Supplier

//TODO
@ApiStatus.Experimental
interface Registrar<T> {

    //TODO
    fun init()

    //TODO
    fun register(name: String, entrySupplier: Supplier<T>): RegistrySupplier<T>

    //TODO
    fun getRegistry(): Registry<T>

    //TODO
    fun createTag(path: String): TagKey<T>

    //TODO
    fun createTag(id: Identifier): TagKey<T>
}