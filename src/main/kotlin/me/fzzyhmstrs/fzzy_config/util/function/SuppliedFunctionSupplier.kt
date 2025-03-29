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
import java.util.function.Supplier

/**
 * Both a supplier and function that uses a provided supplier of inputs to power the supplier output.
 * @param T input type
 * @param R supplied and function output type
 * @param t [Supplier]&lt;[T]&gt; supplier of function inputs. Used when this is called as a supplier
 * @param func [Function]&lt;[T], [R]&gt; function to apply inputs from the supplier or the [apply] call.
 * @author fzzyhmstrs
 * @since 0.6.8
 */
class SuppliedFunctionSupplier<T, R>(private val t: Supplier<T>, private val func: Function<T, R>): FunctionSupplier<T, R> {

    override fun get(): R {
        return func.apply(t.get())
    }

    override fun apply(t: T): R {
        return func.apply(t)
    }
}