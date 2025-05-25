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
 * @since 0.2.0, deprecated 0.7.0 for removal by 0.8.0
 */
@Deprecated("Removal by 0.8.0")
class ConfigContext<T: Any>(val config: T) {

    private val contextFlags: MutableMap<Key<*>, Any> = mutableMapOf()

    /**
     * Adds a flag-info pair into this context. Information should be a boolean or integer value
     * @param key the context key
     * @param value the data value associated with the context flag
     * @return this context with the new flag stored
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun <C: Any> withContext(key: Key<C>, value: C): ConfigContext<T> {
        contextFlags[key] = value
        return this
    }

    /**
     * get an integer value from this context, falling back to 0 if the flag doesn't exist
     * @return Integer value stored with the context flag, or 0 as fallback
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun getInt(key: Key<Int>): Int {
        return contextFlags[key] as? Int ?: 0
    }

    /**
     * get a generic value from this context, or null if it doesn't exist as the specified type at the specified key
     * @param [C] type to retrieve with the key
     * @return Value stored with the context flag, or null
     * @author fzzyhmstrs
     * @since 0.4.0
     */
    fun <C: Any> get(key: Key<C>): C? {
        return contextFlags[key] as? C
    }

    /**
     * get a generic value from this context, or null if it doesn't exist as the specified type at the specified key
     * @param [C] type to retrieve with the key
     * @return Value stored with the context flag, or null
     * @author fzzyhmstrs
     * @since 0.4.0
     */
    fun <C: Any> getOrDefault(key: Key<C>, fallback: C): C {
        return contextFlags[key] as? C ?: fallback
    }

    interface Key<C: Any>
}