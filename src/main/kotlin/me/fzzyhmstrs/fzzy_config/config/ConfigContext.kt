/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.config

/**
 * Holds a config and any applicable secondary flags and their associated information
 * @param T any Non-null type
 * @param config the Config wrapped by this context
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class ConfigContext<T: Any>(val config: T) {

    companion object Keys {
        val RESTART_KEY = "restart"
        val VERSION_KEY = "version"
    }

    private val contextFlags: MutableMap<String, Any> = mutableMapOf()

    /**
     * Adds a flag-info pair into this context. Information should be a boolean or integer value
     * @param flag the context flag
     * @param value the data value associated with the context flag
     * @return this context with the new flag stored
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun withFlag(flag: String, value: Any): ConfigContext<T> {
        contextFlags[flag] = value
        return this
    }

    /**
     * get a boolean value from this context, falling back to false if the flag doesn't exist
     * @return Boolean value stored with the context flag, or false as fallback
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun getBoolean(key: String): Boolean {
        return contextFlags[key] as? Boolean ?: false
    }
    /**
     * get an integer value from this context, falling back to 0 if the flag doesn't exist
     * @return Integer value stored with the context flag, or 0 as fallback
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun getInt(key: String): Int {
        return contextFlags[key] as? Int ?: 0
    }
}