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
import net.minecraft.util.math.MathHelper
import java.awt.Color
import kotlin.jvm.optionals.getOrNull
import kotlin.math.abs

object HslFunction: CssFunction<Int> {
    override fun apply(function: FunctionConsumer.Function): ValidationResult<Int?> {
        val input = function.values
        val builder = RgbCreator()
        return SequencedValueBuilder.create<Int, RgbCreator>()
            .sequenceNoSkip(hue()) { t, c -> c.h = t / 360.0 }
            .sequenceNoSkip(sl()) { t, c -> c.s = t }
            .sequenceNoSkip(sl()) { t, c -> c.l = t }
            .sequence(alphaValue()) { t, c -> c.a = t }
            .build().applyValue(input, builder).map { optional -> optional.map { it.create() }.getOrNull() }
    }

    //https://web.archive.org/web/20170321223122/http://codeitdown.com/hsl-hsb-hsv-color/

    private fun angle(): ValueBuilder<Double> {
        return ValueBuilders.angleValueBuilder()
    }

    private fun number(): ValueBuilder<Double> {
        return SingleValueBuilder.create<Double, Parser.NumberValue>(CssType.NUMBER).safeConverter { it.getDouble().mod(360.0).wrapDegrees() }.build()
    }

    private fun Double.wrapDegrees(): Double {
        return if (this < 0.0) {
            this + 360.0
        } else if (this > 360.0) {
            this - 360.0
        } else {
            this
        }
    }

    private fun none(): ValueBuilder<Double> {
        return ValueBuilders.keyedIdentValueBuilder(0.0, "none")
    }

    private fun hue(): ValueBuilder<Double> {
        return angle().or(number()).or(none())
    }

    private fun slPercentage(): ValueBuilder<Double> {
        return SingleValueBuilder.create<Double, CssType.NumberWithUnitValue>(CssType.NUMBER_PERCENTAGE).safeConverter { it.getDouble().coerceIn(0.0, 100.0).div(100.0) }.build()
    }

    private fun slNone(): ValueBuilder<Double> {
        return ValueBuilders.keyedIdentValueBuilder(0.0, "none")
    }

    private fun sl(): ValueBuilder<Double> {
        return slPercentage().or(slNone())
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
        var h: Double = 0.0 //0 to 1 when stored here
        var s: Double = 0.0 //0 to 1 when stored here
        var l: Double = 0.0 //0 to 1 when stored here
        var a: Int = 0 //0 to 255
        override fun create(): Int {
            val b = (2 * l + s * (1 - abs((2 * l) - 1))) / 2
            val shsb = (2 * ( b - l)) / b
            val rgb = Color.HSBtoRGB(h.toFloat(), shsb.toFloat(), b.toFloat())
            return (((a and 0xFF) shl 24) or
                    rgb and 0xFFFFFF)
        }
    }
}