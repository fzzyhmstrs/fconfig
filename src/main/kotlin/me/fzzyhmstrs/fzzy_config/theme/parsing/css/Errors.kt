/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css

import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.ErrorEntry.Type

object Errors {
    val CRITICAL = Type<String>("Fatal Parsing Error")
    val UNKNOWN_RULE = Type<String>("Unknown or Errored Input")
    val INVALID_AT = Type<String>("At-rule in Invalid Context")

    fun ValidationResult<*>.cssCritical(): Boolean {
        return this.has(CRITICAL)
    }
}