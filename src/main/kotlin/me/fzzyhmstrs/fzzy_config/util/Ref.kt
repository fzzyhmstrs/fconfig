/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.util

/**
 * A mutable reference to a value. About as simple as it gets. The value stored is volatile
 * @param T the type of value referenced by this
 * @param value stored value to reference to and update as needed
 * @author fzzyhmstrs
 * @since 0.6.0
 */
class Ref<T>(@Volatile private var value: T) {

    /**
     * Retrieves the stored value
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun get(): T {
        return this.value
    }

    /**
     * Updates the stored value with the provided input
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun set(value: T) {
        this.value = value
    }
}