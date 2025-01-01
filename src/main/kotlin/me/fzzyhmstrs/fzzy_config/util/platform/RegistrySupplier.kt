package me.fzzyhmstrs.fzzy_config.util.platform

import net.minecraft.registry.RegistryKey
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.util.Identifier
import org.jetbrains.annotations.ApiStatus
import java.util.function.Supplier

/**
 * A reference to a registered object that can supply any of the relevant registry objects (thing, key, id, entry)
 * @param T the registered object type
 * @author fzzyhmstrs
 * @since 0.5.9
 */
@ApiStatus.Experimental
interface RegistrySupplier<T>: Supplier<T> {

    /**
     * The objects [RegistryKey]
     * @return the registry key for this object
     * @author fzzyhmstrs
     * @since 0.5.9
     */
    fun getKey(): RegistryKey<T>

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
}