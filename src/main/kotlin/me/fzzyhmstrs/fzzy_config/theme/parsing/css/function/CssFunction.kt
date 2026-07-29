/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css.function

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.consumer.FunctionConsumer
import me.fzzyhmstrs.fzzy_config.util.ValidationResult

interface CssFunction<T> {
    fun apply(function: FunctionConsumer.Function): ValidationResult<T?>
}