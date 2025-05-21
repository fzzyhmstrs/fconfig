package me.fzzyhmstrs.fzzy_config.util.platform

import com.mojang.datafixers.util.Either
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.entry.RegistryEntryOwner
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import org.jetbrains.annotations.ApiStatus
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
interface RegistrySupplier<T>: Supplier<T>, RegistryEntry<T> {

    /**
     * The objects [RegistryKey]
     * @return the registry key for this object
     * @author fzzyhmstrs
     * @since 0.5.9, changed signature to getRegistryKey 0.6.5 to avoid conflict with new implementation of RegistryEntry
     */
    fun getRegistryKey(): RegistryKey<T>

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
    fun getEntry(): RegistryEntry<T>

    /////// Implementation of RegistryEntry ///////

    override fun hasKeyAndValue(): Boolean {
        return getEntry().hasKeyAndValue()
    }

    override fun matchesId(id: Identifier?): Boolean {
        return getEntry().matchesId(id)
    }

    override fun matchesKey(key: RegistryKey<T>?): Boolean {
        return getEntry().matchesKey(key)
    }

    override fun matches(predicate: Predicate<RegistryKey<T>>?): Boolean {
        return getEntry().matches(predicate)
    }

    override fun isIn(tag: TagKey<T>?): Boolean {
        return getEntry().isIn(tag)
    }

    override fun streamTags(): Stream<TagKey<T>> {
        return getEntry().streamTags()
    }

    override fun getKeyOrValue(): Either<RegistryKey<T>, T> {
        return getEntry().keyOrValue
    }

    override fun getKey(): Optional<RegistryKey<T>> {
        return getEntry().key
    }

    override fun getType(): RegistryEntry.Type {
        return getEntry().type
    }

    override fun ownerEquals(owner: RegistryEntryOwner<T>?): Boolean {
        return getEntry().ownerEquals(owner)
    }

    override fun value(): T {
        return get()
    }
}