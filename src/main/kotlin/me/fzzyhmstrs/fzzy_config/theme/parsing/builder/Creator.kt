/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.builder

import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import java.util.*

fun interface Creator<A> {
    fun create(): A

    companion object {
        fun <T: Any, C: Creator<*>> C.applyValue(
            input: TokenQueue,
            valueBuilder: ValueBuilder<T>,
            onSuccess: (T, C) -> Unit
        ): ValidationResult<Optional<C>> {
            val result = valueBuilder.build(input)
            if (result.isError()) return result.map { Optional.empty<C>() }
            val t = result.get() ?: return ValidationResult.error(Optional.empty<C>(), ValidationResult.Errors.INVALID, "Unexpected null value")
            onSuccess(t, this)
            return ValidationResult.success(Optional.of(this))
        }
    }
}