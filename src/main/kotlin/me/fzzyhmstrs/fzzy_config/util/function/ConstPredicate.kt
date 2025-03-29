/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.util.function

import java.util.function.Predicate

/**
 * Predicate that always returns the boolean provided
 * @param T input type, unused
 * @param bl constant boolean result to return
 * @author fzzyhmstrs
 * @since 0.6.8
 */
class ConstPredicate<T>(private val bl: Boolean): Predicate<T>, FunctionSupplier<T, Boolean> {

    override fun test(t: T): Boolean {
        return bl
    }

    override fun apply(t: T): Boolean {
        return bl
    }

    override fun get(): Boolean {
        return bl
    }
}