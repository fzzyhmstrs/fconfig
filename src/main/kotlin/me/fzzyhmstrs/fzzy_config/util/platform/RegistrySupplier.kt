package me.fzzyhmstrs.fzzy_config.util.platform

import com.mojang.datafixers.util.Either
import net.minecraft.resources.ResourceKey
import net.minecraft.core.Holder
import net.minecraft.core.HolderOwner
import net.minecraft.tags.TagKey
import net.minecraft.resources.Identifier
import java.util.*
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Stream

/**
 * A reference to a registered object that can supply any of the relevant registry objects (thing, key, id, entry)
 *
 * **Warning! Certain usages of RegistryEntry require RegistryEntry.Reference to be passed and MC will crash if that specific subclass isn't passed in. Only use this as an RegistryEntry directly when you need to use it for construction of other registry objects!**
 * @param T the registered object type
 * @author fzzyhmstrs
 * @since 0.5.9, implements RegistryEntry itself as of 0.6.5, no longer experimental 0.7.0
 */
interface RegistrySupplier<T: Any>: Supplier<T>, Holder<T> {

    /**
     * The objects [RegistryKey]
     * @return the registry key for this object
     * @author fzzyhmstrs
     * @since 0.5.9, changed signature to getRegistryKey 0.6.5 to avoid conflict with new implementation of RegistryEntry
     */
    fun getRegistryKey(): ResourceKey<T>

    /**
     * The objects [Identifier]
     * @return the id for this object
     * @author fzzyhmstrs
     * @since 0.5.9
     */
    fun getId(): Identifier

    /**
     * The objects [RegistryEntry]. This will be a [RegistryEntry.Reference], so can be used in Codecs and so on.
     * @return the registry entry for this object
     * @author fzzyhmstrs
     * @since 0.5.9
     */
    fun getEntry(): Holder<T>

    /////// Implementation of RegistryEntry ///////

    override fun isBound(): Boolean {
        return getEntry().isBound
    }

    override fun `is`(id: Identifier): Boolean {
        return getEntry().`is`(id)
    }

    override fun `is`(key: ResourceKey<T>): Boolean {
        return getEntry().`is`(key)
    }

    override fun `is`(predicate: Predicate<ResourceKey<T>>): Boolean {
        return getEntry().`is`(predicate)
    }

    override fun `is`(tag: TagKey<T>): Boolean {
        return getEntry().`is`(tag)
    }

    override fun tags(): Stream<TagKey<T>> {
        return getEntry().tags()
    }

    override fun unwrap(): Either<ResourceKey<T>, T> {
        return getEntry().unwrap()
    }

    override fun getKey(): ResourceKey<T> {
        return getEntry().key!!
    }

    override fun kind(): Holder.Kind {
        return getEntry().kind()
    }

    override fun canSerializeIn(owner: HolderOwner<T>): Boolean {
        return getEntry().canSerializeIn(owner)
    }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Deprecated in Java")
    override fun `is`(entry: Holder<T>): Boolean {
        return getEntry().`is`(entry)
    }

    override fun value(): T {
        return get()
    }
}