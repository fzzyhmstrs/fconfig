/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.util

import java.util.function.BooleanSupplier
import java.util.function.Supplier

/**
 * A generic representation of a three state system. Implements [BooleanSupplier]
 * @see TriState
 * @see me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedTriState
 * @author fzzyhmstrs
 * @since 0.6.5
 */
interface TriStateProvider: BooleanSupplier {
    override fun getAsBoolean(): Boolean
    fun getBoxed(): Boolean?
    fun orElse(value: Boolean): Boolean
    fun orElseGet(supplier: BooleanSupplier): Boolean
    fun orElseGet(supplier: Supplier<Boolean>): Boolean
    fun validate(input: Boolean): Boolean
}