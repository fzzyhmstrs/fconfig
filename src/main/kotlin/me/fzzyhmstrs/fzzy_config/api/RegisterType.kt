package me.fzzyhmstrs.fzzy_config.api

/**
 * Defines the registries the config is registered to.
 *
 * Default is [BOTH]
 * @author fzzyhmstrs
 * @since 0.2.0
 */
enum class RegisterType {
    /**
     * Config is registered to both the SyncedConfigRegistry and the ClientConfigRegistry.
     *
     * Will auto-synchronize between clients and server, and will have a config GUI.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    BOTH,
    /**
     * Config is registered to only the SyncedConfigRegistry.
     *
     * Will auto-synchronize between clients and server, but will NOT have any client sided GUI support.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    SYNC,
    /**
     * Config is registered to only the ClientConfigRegistry.
     *
     * will have a config GUI, but will not auto-synchronize. Some syncing actions will still work, namely setting forwarding.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    CLIENT
}
