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

import me.fzzyhmstrs.fzzy_config.theme.parsing.css.CssType
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.Errors
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.rule.DeclarationKey
import me.fzzyhmstrs.fzzy_config.theme.parsing.css.value.LengthValue
import me.fzzyhmstrs.fzzy_config.theme.parsing.parser.Parser
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenQueue
import me.fzzyhmstrs.fzzy_config.theme.parsing.token.TokenType
import me.fzzyhmstrs.fzzy_config.util.TriState
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.map
import net.minecraft.util.Identifier
import java.util.function.Supplier
import kotlin.jvm.optionals.getOrNull

object SingleValueBuilders {
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
                ValidationResult.error(null, ValidationResult.Errors.INVALID, "Expectedone of: ${ident.joinToString()}, got $i")
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

    fun <T: Enum<T>> createMap(clazz: Class<T>): Map<String, T> {
        val values = clazz.enumConstants
        return values.associateBy { v -> v.name.lowercase() }
    }

    private val INVALID_ENUM = ValidationResult.ErrorEntry.Type<String>("Invalid enum option")
}
