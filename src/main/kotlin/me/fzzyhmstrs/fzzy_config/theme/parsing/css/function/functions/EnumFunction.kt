/*
 * Copyright (c) 2026 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.theme.parsing.css.function.functions

import me.fzzyhmstrs.fzzy_config.theme.parsing.builder.ValueBuilders
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.consumer.FunctionConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.function.CssFunction
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map

class EnumFunction<T: Enum<T>, V>(private val map: Map<String, T>, private val converter: (T?) -> V?): CssFunction<V> {
    override fun apply(function: FunctionConsumer.Function): ValidationResult<V?> {
        return ValueBuilders.enumValueBuilder(map).build(function.values).map { converter(it) }
    }
}