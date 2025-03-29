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

import java.util.function.Supplier

/**
 * Supplier that always returns the pre-computed stored result.
 * @param T supplier type
 * @param t constant result to provide
 * @author fzzyhmstrs
 * @since 0.6.8
 */
open class ConstSupplier<T>(protected val t: T): Supplier<T> {

    override fun get(): T {
        return t
    }
}