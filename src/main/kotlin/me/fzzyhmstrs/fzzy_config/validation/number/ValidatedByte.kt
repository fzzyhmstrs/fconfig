/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.validation.number

import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toByte
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * A validated byte number. This field is a wrapper of bytes that is both a [java.util.function.Supplier] and [java.util.function.Consumer] of type Byte
 * @param defaultValue Byte. the default value of this wrapper
 * @param maxValue Byte. the maximum allowed value, inclusive
 * @param minValue Byte. the minimum allowed value, inclusive
 * @property widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.bytes
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ValidatedByte @JvmOverloads constructor(defaultValue: Byte, maxValue: Byte, minValue: Byte, widgetType: WidgetType = if(maxValue == Byte.MAX_VALUE || minValue == Byte.MIN_VALUE) WidgetType.TEXTBOX else WidgetType.SLIDER): ValidatedNumber<Byte>(defaultValue, minValue, maxValue, widgetType) {

    /**
     * A validated byte number with a default selected from the min of the allowable range.
     * @param minValue Byte. the minimum allowed value, inclusive
     * @param maxValue Byte. the maximum allowed value, inclusive
     * @param widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(minValue: Byte,maxValue: Byte,widgetType: WidgetType = WidgetType.SLIDER): this(minValue,maxValue, minValue, widgetType)

    /**
     * an unbounded validated byte number.
     *
     * The validation will be limited to ensuring the value de/serializes as a byte, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     * @param defaultValue Byte. the default value of this wrapper
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: Byte): this(defaultValue, Byte.MAX_VALUE, Byte.MIN_VALUE, WidgetType.TEXTBOX)

    /**
     * an unbounded validated byte number with a default of 0b
     *
     * The validation will be limited to ensuring the value de/serializes as a byte, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this(0, Byte.MAX_VALUE, Byte.MIN_VALUE, WidgetType.TEXTBOX)

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Byte> {
        return try{
            ValidationResult.success(toml.asTomlLiteral().toByte())
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Problem deserializing ValidatedByte [$fieldName]: ${e.localizedMessage}")
        }
    }
    @Internal
    override fun serialize(input: Byte): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    override fun instanceEntry(): ValidatedByte {
        return ValidatedByte(copyStoredValue(), maxValue, minValue, widgetType)
    }

    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is Byte && validateEntry(input, EntryValidator.ValidationType.STRONG).isValid()
    }

    override fun convert(input: Double): ValidationResult<Byte> {
        return ValidationResult.predicated(input.toInt().toByte(),input.toLong() == input.toInt().toByte().toLong(),"[$input] out of Bounds for byte value (${Byte.MIN_VALUE} to ${Byte.MAX_VALUE} )")
    }

    override fun toString(): String {
        val validation = if(minValue==Byte.MIN_VALUE && maxValue== Byte.MAX_VALUE)
            "Unbounded"
        else if(minValue == Byte.MIN_VALUE)
            "less than $maxValue"
        else if (maxValue == Byte.MAX_VALUE)
            "greater than $minValue"
        else
            "between $minValue and $maxValue"
        return "Validated Byte[value=$storedValue, validation=$validation]"
    }

    /**
     * Annotation-driven validation for Bytes
     * @param min: Byte - minimum allowable value, default to Byte.MIN_VALUE
     * @param max: Byte - maximum allowable value, default to Byte.MAX_VALUE
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    annotation class Restrict(val min: Byte = Byte.MIN_VALUE, val max: Byte = Byte.MAX_VALUE)
}