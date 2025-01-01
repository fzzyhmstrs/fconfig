package me.fzzyhmstrs.fzzy_config.util.platform

import net.minecraft.registry.Registry
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import org.jetbrains.annotations.ApiStatus
import java.util.function.Supplier

/**
 * A simple registrar for any registered object. This registrar works similarly to the (Neo)Forge way of doing object registration. Create a namespaced registrar instance with [createRegistrar][me.fzzyhmstrs.fzzy_config.util.PlatformApi.createRegistrar]
 *
 * Example registration pattern, in a registration object:
 * ```
 * private val ITEMS: Registrar<Item> = ConfigApi.platform().createRegistrar(MOD_ID, Registries.ITEM)
 *
 * val MY_ITEM = ITEMS.register("name") { MyItem(Item.Settings()) }
 * ```
 *
 * Scheduled for stability by 0.7.0
 * @see [me.fzzyhmstrs.fzzy_config.util.PlatformApi.createRegistrar]
 * @author fzzyhmstrs
 * @since 0.5.9
 */
@ApiStatus.Experimental
interface Registrar<T> {

    /**
     * Initializes the Registrar. This is *required* for the registrar to work properly. You don't have to init() before registering objects, necessarily, but it is a good idea.
     *
     * Initialization can be performed in the typical common mod entrypoint relevant to the platform.
     * @author fzzyhmstrs
     * @since 0.5.9
     */
    fun init()

    /**
     * Registers a game object. On fabric, will use direct registration. On (Neo)Forge, will use deferred registration under the hood.
     * @param name The id path of the object. The namespace is supplied when creating the registrar.
     * @author fzzyhmstrs
     * @since 0.5.9
     */
    fun register(name: String, entrySupplier: Supplier<T>): RegistrySupplier<T>

    /**
     * Returns the registry instance this registrar was linked to
     * @return [Registry] instance
     * @author fzzyhmstrs
     * @since 0.5.9
     */
    fun getRegistry(): Registry<T>

    /**
     * Shorthand method for creating a [TagKey] of the same type as the registrar
     * @param path String path portion of the tag id. Namespace will be this registrars namespace
     * @return [TagKey] instance
     * @author fzzyhmstrs
     * @since 0.5.9
     */
    fun createTag(path: String): TagKey<T>

    /**
     * Shorthand method for creating and [TagKey] with a fully arbitrary id of the same type as the registrar
     * @param id [Identifier] with arbitrary namespace and path. Does not have to be the same namespace as the registrar.
     * @return [TagKey] instance
     * @author fzzyhmstrs
     * @since 0.5.9
     */
    fun createTag(id: Identifier): TagKey<T>
}