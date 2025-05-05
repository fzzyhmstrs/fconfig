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
import net.peanuuutz.tomlkt.toInt
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * A validated integer number. This field is a wrapper of integers that is both a [java.util.function.Supplier] and [java.util.function.Consumer] of type Int
 * @param defaultValue Int. the default value of this wrapper
 * @param maxValue Int. the maximum allowed value, inclusive
 * @param minValue Int. the minimum allowed value, inclusive
 * @property widgetType [ValidatedNumber.WidgetType] defines what the config GUI widget looks like
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.ints
 * @author fzzyhmstrs
 * @since 0.1.0
 */

class ValidatedInt @JvmOverloads constructor(defaultValue: Int, maxValue: Int, minValue: Int, widgetType: WidgetType = if(maxValue == Int.MAX_VALUE || minValue == Int.MIN_VALUE) WidgetType.TEXTBOX else WidgetType.SLIDER): ValidatedNumber<Int>(defaultValue, minValue, maxValue, widgetType) {

    /**
     * A validated int number generated with an [IntRange].
     * @param defaultValue Int. the default value of this wrapper
     * @param range [IntRange]. the allowable range of this Validated Int
     * @param widgetType [ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(defaultValue: Int, range: IntRange, widgetType: WidgetType = WidgetType.SLIDER): this(defaultValue, range.last, range.first, widgetType)

    /**
     * A validated int number with a default selected from the min of the allowable range.
     * @param range [IntRange]. the allowable range of this Validated Int
     * @param widgetType [ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(range: IntRange, widgetType: WidgetType = WidgetType.SLIDER): this(range.first, range.last, range.first, widgetType)

    /**
     * A validated int number with a default selected from the min of the allowable range.
     * @param minValue Int. the minimum allowed value, inclusive
     * @param maxValue Int. the maximum allowed value, inclusive
     * @param widgetType [ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(minValue: Int, maxValue: Int, widgetType: WidgetType = WidgetType.SLIDER): this(minValue, maxValue, minValue, widgetType)

    /**
     * An unbounded validated int number.
     *
     * The validation will be limited to ensuring the value de/serializes as an int, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     * @param defaultValue Int. the default value of this wrapper
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: Int): this(defaultValue, Int.MAX_VALUE, Int.MIN_VALUE, WidgetType.TEXTBOX)

    /**
     * An unbounded validated int number with default of 0.
     *
     * The validation will be limited to ensuring the value de/serializes as an int, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this(0, Int.MAX_VALUE, Int.MIN_VALUE, WidgetType.TEXTBOX)

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Int> {
        return try {
            ValidationResult.success(toml.asTomlLiteral().toInt())
        } catch (e: Throwable) {
            ValidationResult.error(defaultValue, ValidationResult.Errors.DESERIALIZATION, "Exception deserializing int [$fieldName]", e)
        }
    }

    @Internal
    override fun serialize(input: Int): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    /**
     * creates a deep copy of this ValidatedInt
     * return ValidatedInt wrapping the current int value and validation restrictions
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedInt {
        return ValidatedInt(defaultValue, maxValue, minValue, widgetType)
    }

    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is Int && validateEntry(input, EntryValidator.ValidationType.STRONG).isValid()
    }

    @Internal
    override var increment: Int? = null

    @Internal
    override fun convert(input: Double): ValidationResult<Int> {
        return ValidationResult.predicated(
            input.toInt(),
            input % 1 == 0.0 && input.toLong() == input.toInt().toLong(),
            ValidationResult.Errors.OUT_OF_BOUNDS) { b -> b.content("[$input] not a valid Int") }
    }

    @Internal
    override fun minBound(): Int {
        return Int.MIN_VALUE
    }

    @Internal
    override fun maxBound(): Int {
        return Int.MAX_VALUE
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        val validation = if(minValue==Int.MIN_VALUE && maxValue== Int.MAX_VALUE)
            "Unbounded"
        else if(minValue == Int.MIN_VALUE)
            "less than $maxValue"
        else if (maxValue == Int.MAX_VALUE)
            "greater than $minValue"
        else
            "between $minValue and $maxValue"
        return "Validated Int[value=$storedValue, validation=$validation]"
    }

    /**
     * Annotation-driven validation for Ints
     * @param min: Int - minimum allowable value, default to Int.MIN_VALUE
     * @param max: Int - maximum allowable value, default to Int.MAX_VALUE
     * @param type: [ValidatedNumber.WidgetType] - The "style" of the GUI widget for the annotated setting
     * @author fzzyhmstrs
     * @since 0.2.0, added widget type 0.6.3
     */
    annotation class Restrict(val min: Int = Int.MIN_VALUE, val max: Int = Int.MAX_VALUE, val type: WidgetType = WidgetType.SLIDER)
}