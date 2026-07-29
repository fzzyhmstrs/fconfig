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

import me.fzzyhmstrs.fzzy_config.theme.parsing.builder.*
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.consumer.FunctionConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.function.CssFunction
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.Parser
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import kotlin.jvm.optionals.getOrNull

object RgbFunction: CssFunction<Int> {
    override fun apply(function: FunctionConsumer.Function): ValidationResult<Int?> {
        val input = function.values
        val builder = RgbCreator()
        return SequencedValueBuilder.create<Int, RgbCreator>()
            .sequenceNoSkip(value()) { t, c -> c.r = t }
            .sequenceNoSkip(value()) { t, c -> c.g = t }
            .sequenceNoSkip(value()) { t, c -> c.b = t }
            .sequence(alphaValue()) { t, c -> c.a = t }
            .build().applyValue(input, builder).map { optional -> optional.map { it.create() }.getOrNull() }
    }

    private fun number(): ValueBuilder<Int> {
        return SingleValueBuilder.create<Int, Parser.NumberValue>(CssType.NUMBER).safeConverter {it.getInt().coerceIn(0, 255) }.build()
    }

    private fun percent(): ValueBuilder<Int> {
        return SingleValueBuilder.create<Int, CssType.NumberWithUnitValue>(CssType.NUMBER_PERCENTAGE).safeConverter { it.getDouble().coerceIn(0.0, 100.0).div(100.0).times(255).toInt() }.build()
    }

    private fun none(): ValueBuilder<Int> {
        return ValueBuilders.keyedIdentValueBuilder(0, "none")
    }

    private fun value(): ValueBuilder<Int> {
        return number().or(percent()).or(none())
    }

    private fun alphaNumber(): ValueBuilder<Double> {
        return SingleValueBuilder.create<Double, Parser.NumberValue>(CssType.NUMBER).safeConverter {it.getDouble().coerceIn(0.0, 1.0) }.build()
    }

    private fun alphaPercent(): ValueBuilder<Double> {
        return SingleValueBuilder.create<Double, CssType.NumberWithUnitValue>(CssType.NUMBER_PERCENTAGE).safeConverter { it.getDouble().coerceIn(0.0, 100.0).div(100.0) }.build()
    }

    private fun alphaNone(): ValueBuilder<Double> {
        return ValueBuilders.keyedIdentValueBuilder(1.0, "none")
    }

    private fun alphaValue(): ValueBuilder<Int> {
        return ValueBuilders.predicatedValueBuilder(alphaNumber().or(alphaPercent()).or(alphaNone()), "Alpha needs to be separated with ' / '") { t -> t.asString() == "/" }.map { d -> d?.times(255)?.toInt() }
    }

    private class RgbCreator: Creator<Int> {
        var r: Int = 0
        var g: Int = 0
        var b: Int = 0
        var a: Int = 0
        override fun create(): Int {
            return (((a and 0xFF) shl 24) or
                    ((r and 0xFF) shl 16) or
                    ((g and 0xFF) shl 8) or
                    ((b and 0xFF) shl 0))
        }
    }
}