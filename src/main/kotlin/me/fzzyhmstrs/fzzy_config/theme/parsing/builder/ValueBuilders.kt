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

import com.ibm.icu.text.PluralRules
import me.fzzyhmstrs.fzzy_config.theme.parsing.builder.ValueBuilders.enumValueBuilder
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.consumer.FunctionConsumer
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.function.CssFunction
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.function.functions.EnumFunction
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.function.functions.HslFunction
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.function.functions.RgbFunction
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.DeclarationKey
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.value.CssColor
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.value.LengthValue
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.Parser
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.Token
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.TriState
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import net.minecraft.util.Identifier
import java.util.function.Predicate
import kotlin.jvm.optionals.getOrNull
import kotlin.math.PI

object ValueBuilders {
    val STRING = SingleValueBuilder.create<String, String>(CssType.STRING).safeConverter { v -> v }.build()
    val INT = SingleValueBuilder.create<Int, Parser.NumberValue>(CssType.NUMBER).safeConverter { v -> v.getInt() }.build()
    val DOUBLE = SingleValueBuilder.create<Double, Parser.NumberValue>(CssType.NUMBER).safeConverter { v -> v.getDouble() }.build()

    fun boolValueBuilder(fallback: Boolean = false): ValueBuilder<Boolean> {
        return enumValueBuilder(TriState.MAP).map { it?.getBoxed() ?: fallback }
    }

    fun identValueBuilder(vararg ident: String): ValueBuilder<String> {
        return SingleValueBuilder.create<String, String>(CssType.IDENT).converter { i ->
            if (ident.contains(i)) {
                ValidationResult.success(i)
            } else {
                ValidationResult.error(null, ValidationResult.Errors.INVALID, "Expected one of: ${ident.joinToString()}, got $i")
            }
        }.build()
    }

    fun <T: Any> keyedIdentValueBuilder(keyedValue: T, vararg ident: String): ValueBuilder<T> {
        return SingleValueBuilder.create<T, String>(CssType.IDENT).converter { i ->
            if (ident.contains(i)) {
                ValidationResult.success(keyedValue)
            } else {
                ValidationResult.error(null, ValidationResult.Errors.INVALID, "Expected one of: ${ident.joinToString()}, got $i")
            }
        }.build()
    }

    fun identifierValueBuilder(validator: (Identifier) -> Boolean = { true }, fallback: Identifier? = null): ValueBuilder<Identifier> {
        return SingleValueBuilder.create<Identifier, String>(CssType.STRING).fallback(fallback).converter { s ->
            val i = Identifier.tryParse(s)
            if (i == null) {
                ValidationResult.error(null, ValidationResult.Errors.INVALID, i.toString())
            } else {
                if (validator(i)) {
                    ValidationResult.success(i)
                } else {
                    ValidationResult.error(null, ValidationResult.Errors.INVALID, i.toString())
                }
            }
        }.build()
    }

    fun <T: Enum<T>> enumValueBuilder(map: Map<String, T>, type: TokenType<String> = CssType.IDENT, fallback: T? = null): ValueBuilder<T> {
        return SingleValueBuilder.create<T, String>(type).fallback(fallback).converter { v ->
            val l = v.lowercase()
            if (l == v) {
                Errors.uppercase(null, v)
            } else {
                val t = map[l]
                if (t == null) {
                    ValidationResult.error(null, INVALID_ENUM, "Possible values: ${map.keys}, provided: $l")
                } else {
                    ValidationResult.success(t)
                }
            }
        }.build()
    }

    fun angleValueBuilder(): ValueBuilder<Double> {
        return SingleValueBuilder.create<Double, CssType.NumberWithUnitValue>(CssType.NUMBER_DIMENSION).converter { n ->
            val a = n.getDouble()
            val angle = when (n.unit()) {
                "deg" -> a
                "grad" -> a * 400.0 / 360.0
                "rad" -> a / 180.0 * PI
                "turn" -> a * 360.0
                else -> return@converter ValidationResult.error(null, INVALID_ANGLE, n.unit())
            }
            ValidationResult.success(angle)
        }.build()
    }

    fun lengthValueBuilder(fallback: LengthValue? = null): ValueBuilder<LengthValue> {
        return SingleValueBuilder.create<LengthValue, CssType.NumberWithUnitValue>(CssType.NUMBER_PERCENTAGE, CssType.NUMBER_DIMENSION)
            .fallback(fallback)
            .converter(LengthValue::parse)
            .build()
    }

    fun lengthValueBuilder(fallback: Int): ValueBuilder<LengthValue> {
        return lengthValueBuilder(LengthValue(fallback.toDouble(), LengthValue.Unit.PIXELS))
    }

    fun dimensionValueBuilder(autoValue: () -> DeclarationKey<LengthValue, *>): ValueBuilder<LengthValue> {
        val creator = DimensionCreator()
        val sequence = SequencedValueBuilder.create<LengthValue, DimensionCreator>()
            .sequence(autoValueBuilder(autoValue)) { t, c -> c.lengthValue = t }
            .sequence(lengthValueBuilder()) { t, c -> c.lengthValue = t }
            .sequence(identValueBuilder("stretch")) { t, c -> c.lengthValue = LengthValue(1.0, StretchUnit()) }
            .oneOf()
            .build()
        return object: ValueBuilder<LengthValue> {
            override fun build(input: TokenQueue): ValidationResult<LengthValue?> {
                val result = sequence.applyValue(input, creator)
                val v = result.get().getOrNull()?.create()
                if (result.isError()) return result.map { v }
                return ValidationResult.success(v)
            }
        }
    }

    fun hexValueBuilder(): ValueBuilder<Int> {
        return SingleValueBuilder.create<Int, String>(CssType.HASH).converter { s ->
            val ss = when (s.length) {
                3 -> "${s[0]}${s[0]}${s[1]}${s[1]}${s[2]}${s[2]}"
                4 -> "${s[0]}${s[0]}${s[1]}${s[1]}${s[2]}${s[2]}${s[3]}${s[3]}"
                6, 8 -> s
                else -> return@converter ValidationResult.error(null, NUMBER_FORMAT, "Invalid number of digits")
            }
            val i = try {
                Integer.parseUnsignedInt(ss, 16)
            } catch (_: Exception) {
                return@converter ValidationResult.error(null, NUMBER_FORMAT, "Unparseable number: $ss")
            }
            ValidationResult.success(i)
        }.build()
    }

    fun colorValueBuilder(): ValueBuilder<Int> {
        val creator = ValueCreator(0)
        val sequence = SequencedValueBuilder.create<Int, ValueCreator<Int>>()
            .sequence(enumValueBuilder(CssColor.MAP)) { t, c -> c.value = t.get() }
            .sequence(hexValueBuilder()) { t, c -> c.value = t }
            .sequence(functionValueBuilder(RgbFunction, "rgb", "rgba")) { t, c -> c.value = t }
            .sequence(functionValueBuilder(HslFunction, "hsl", "hsla")) { t, c -> c.value = t }
            .sequence(functionValueBuilder(EnumFunction(CssColor.DYE_COLOR_MAP) { dc -> dc?.entityColor }, "dye-color")) { t, c -> c.value = t }
            .sequence(functionValueBuilder(EnumFunction(CssColor.DYE_COLOR_MAP) { dc -> dc?.signColor }, "sign-color")) { t, c -> c.value = t }
            .sequence(functionValueBuilder(EnumFunction(CssColor.DYE_COLOR_MAP) { dc -> dc?.fireworkColor }, "firework-color")) { t, c -> c.value = t }
            .sequence(functionValueBuilder(EnumFunction(CssColor.FORMATTING_MAP) { dc -> dc?.colorValue }, "formatting-color")) { t, c -> c.value = t }
            .oneOf()
            .build()
        return object: ValueBuilder<Int> {
            override fun build(input: TokenQueue): ValidationResult<Int?> {
                val result = sequence.applyValue(input, creator)
                val v = result.get().getOrNull()?.create()
                if (result.isError()) return result.map { v }
                return ValidationResult.success(v)
            }
        }
    }

    fun <T: Any> functionValueBuilder(function: CssFunction<T>, vararg functionName: String): ValueBuilder<T> {
        return SingleValueBuilder.create<T, FunctionConsumer.Function>(CssType.FUNCTION_CONSUMED).converter { f ->
            if (functionName.none { f.identifier == it }) {
                ValidationResult.error(null, INVALID_FUNCTION, "Allowable names: ${functionName.toList()}")
            } else {
                function.apply(f)
            }
        }.build()
    }

    fun <T: Any> autoValueBuilder(autoValue: () -> DeclarationKey<T, *>): ValueBuilder<T> {
        return SingleValueBuilder.create<T, String>(CssType.IDENT).converter { s ->
            val key = autoValue()
            if (s == "auto") {
                val a = key.autoValue()
                if (a == null) {
                    ValidationResult.error(null, Errors.NON_AUTO, DeclarationKey.declName(key))
                } else {
                    ValidationResult.success(a)
                }
            } else {
                ValidationResult.error(null, Errors.NON_AUTO, DeclarationKey.declName(key))
            }
        }.build()
    }

    fun <T: Any> autoValueBuilder(name: String, autoValue: () -> T): ValueBuilder<T> {
        return SingleValueBuilder.create<T, String>(CssType.IDENT).converter { s ->
            if (s == "auto") {
                val a = autoValue()
                ValidationResult.success(a)
            } else {
                ValidationResult.error(null, Errors.NON_AUTO, name)
            }
        }.build()
    }

    fun twoValueValueBuilder(t: String = "true", f: String = "false"): ValueBuilder<Boolean> {
        return SingleValueBuilder.create<Boolean, String>(CssType.IDENT).converter { s ->
            when (s) {
                t -> {
                    ValidationResult.success(true)
                }
                f -> {
                    ValidationResult.success(false)
                }
                else -> {
                    ValidationResult.error(null, ValidationResult.Errors.INVALID, "$t or $f accepted")
                }
            }
        }.build()
    }

    fun <T: Any> oneOfValueBuilder(default: T, vararg valueBuilder: ValueBuilder<T>): ValueBuilder<T> {
        val creator = ValueCreator(default)
        val sequenceBuilder = SequencedValueBuilder.create<T, ValueCreator<T>>().oneOf()
        for (vb in valueBuilder) {
            sequenceBuilder.sequence(vb) { t, c -> c.value = t }
        }
        val sequence = sequenceBuilder.build()
        return object: ValueBuilder<T> {
            override fun build(input: TokenQueue): ValidationResult<T?> {
                val result = sequence.applyValue(input, creator)
                val v = result.get().getOrNull()?.create()
                if (result.isError()) return result.map { v }
                return ValidationResult.success(v)
            }
        }
    }

    fun <T> predicatedValueBuilder(andThen: ValueBuilder<T>, preludeFails: String, prelude: Predicate<Token<*>>): ValueBuilder<T> {
        return object : ValueBuilder<T> {
            override fun build(input: TokenQueue): ValidationResult<T?> {
                if (!input.canPoll()) return Errors.endOfQueue(null)
                val t = input.peek()
                if (!prelude.test(t)) return ValidationResult.error(null, ValidationResult.Errors.INVALID, preludeFails)
                input.poll()
                return andThen.build(input)
            }
        }
    }

    private class StretchUnit: LengthValue.UnitType {
        override fun applyUnit(value: Double): Int {
            TODO("Not yet implemented")
        }
    }

    private class DimensionCreator: Creator<LengthValue> {
        var lengthValue: LengthValue = LengthValue(0)
        override fun create(): LengthValue {
            return lengthValue
        }
    }

    private class ValueCreator<T: Any>(default: T): Creator<T> {
        var value: T = default
        override fun create(): T {
            return value
        }
    }



    fun <T: Enum<T>> createMap(clazz: Class<T>): Map<String, T> {
        val values = clazz.enumConstants
        return values.associateBy { v -> v.name.lowercase() }
    }

    private val INVALID_ENUM = ValidationResult.ErrorEntry.Type<String>("Invalid enum option")
    private val INVALID_ANGLE = ValidationResult.ErrorEntry.Type<String>("Invalid angle syntax")
    private val INVALID_FUNCTION = ValidationResult.ErrorEntry.Type<String>("Invalid function name")
    private val NUMBER_FORMAT = ValidationResult.ErrorEntry.Type<String>("Invalid hex number format")
}
