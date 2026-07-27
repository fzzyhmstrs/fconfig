/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.strategy.builder

import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult

class OrValueBuilder<T: Any>(private val first: ValueBuilder<out T>, private val second: ValueBuilder<out T>): ValueBuilder<T> {

    override fun build(input: TokenQueue): ValidationResult<T?> {
        val result = first.build(input)
        if (result.isValid()) return ValidationResult.success(result.get())
        return second.build(input).cast()
    }
}