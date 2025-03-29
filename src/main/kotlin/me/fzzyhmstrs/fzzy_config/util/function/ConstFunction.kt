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

import java.util.function.Function

/**
 * Function that always returns the pre-computed stored result.
 * @param T input type, unused
 * @param R output type
 * @param r constant result to pass through the function
 * @author fzzyhmstrs
 * @since 0.6.8
 */
class ConstFunction<T, R>(r: R): ConstSupplier<R>(r), FunctionSupplier<T, R> {

    override fun apply(t: T): R {
        return this.t
    }
}