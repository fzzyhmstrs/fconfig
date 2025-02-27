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

interface TriStateProvider: BooleanSupplier {
    override fun getAsBoolean(): Boolean
    fun getBoxed(): Boolean?
    fun orElse(value: Boolean): Boolean
    fun orElseGet(supplier: BooleanSupplier): Boolean
    fun orElseGet(supplier: Supplier<Boolean>): Boolean
    fun validate(input: Boolean): Boolean
}