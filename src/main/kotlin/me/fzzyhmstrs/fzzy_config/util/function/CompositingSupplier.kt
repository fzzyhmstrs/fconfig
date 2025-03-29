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

import java.util.function.BiFunction
import java.util.function.Supplier

/**
 * Supplier that composites two results together and supplies that result. May or may not be constant
 * @param T supplier type
 * @author fzzyhmstrs
 * @since 0.6.8
 */
sealed interface CompositingSupplier<T>: Supplier<T> {

    private class Supplied1Supplier<T>(private val oneSupplier: Supplier<T>, private val two: T, private val compositingFunction: BiFunction<T, T, T>): CompositingSupplier<T> {

        override fun get(): T {
            return compositingFunction.apply(oneSupplier.get(), two)
        }
    }

    private class Supplied2Supplier<T>(private val one: T, private val twoSupplier: Supplier<T>, private val compositingFunction: BiFunction<T, T, T>): CompositingSupplier<T> {

        override fun get(): T {
            return compositingFunction.apply(one, twoSupplier.get())
        }
    }

    private class DualConstSupplier<T>(oneConst: T, twoConst: T, compositingFunction: BiFunction<T, T, T>): ConstSupplier<T>(compositingFunction.apply(oneConst, twoConst)), CompositingSupplier<T>

    private class DualSuppliedSupplier<T>(private val oneSupplier: Supplier<T>, private val twoSupplier: Supplier<T>, private val compositingFunction: BiFunction<T, T, T>): CompositingSupplier<T> {

        override fun get(): T {
            return compositingFunction.apply(oneSupplier.get(), twoSupplier.get())
        }
    }

    companion object {

        fun <T> of(one: T, two: T, compositingFunction: BiFunction<T, T, T>): Supplier<T> {
            return DualConstSupplier(one, two, compositingFunction)
        }

        fun <T> of(oneSupplier: Supplier<T>, twoSupplier: Supplier<T>, compositingFunction: BiFunction<T, T, T>): Supplier<T> {
            return if (oneSupplier is ConstSupplier<*>) {
                if (twoSupplier is ConstSupplier<*>) {
                    DualConstSupplier(oneSupplier.get(), twoSupplier.get(), compositingFunction)
                } else {
                    Supplied2Supplier(oneSupplier.get(), twoSupplier, compositingFunction)
                }
            } else {
                if (twoSupplier is ConstSupplier<*>) {
                    Supplied1Supplier(oneSupplier, twoSupplier.get(), compositingFunction)
                } else {
                    DualSuppliedSupplier(oneSupplier, twoSupplier, compositingFunction)
                }
            }
        }

        fun <T> of(oneSupplier: Supplier<T>, two: T, compositingFunction: BiFunction<T, T, T>): Supplier<T> {
            return if (oneSupplier is ConstSupplier<*>) {
                DualConstSupplier(oneSupplier.get(), two, compositingFunction)
            } else {
                Supplied1Supplier(oneSupplier, two, compositingFunction)
            }
        }

        fun <T> of(one: T, twoSupplier: Supplier<T>, compositingFunction: BiFunction<T, T, T>): Supplier<T> {
            return if (twoSupplier is ConstSupplier<*>) {
                DualConstSupplier(one, twoSupplier.get(), compositingFunction)
            } else {
                Supplied2Supplier(one, twoSupplier, compositingFunction)
            }
        }

    }

}