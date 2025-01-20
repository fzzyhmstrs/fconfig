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
import net.peanuuutz.tomlkt.toDouble
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * A validated double number. This field is a wrapper of doubles that is both a [java.util.function.Supplier] and [java.util.function.Consumer] of type Double
 * @param defaultValue Double. the default value of this wrapper
 * @param maxValue Double. the maximum allowed value, inclusive
 * @param minValue Double. the minimum allowed value, inclusive
 * @property widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.doubles
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ValidatedDouble @JvmOverloads constructor(defaultValue: Double, maxValue: Double, minValue: Double, widgetType: WidgetType = if(maxValue == Double.MAX_VALUE || minValue == -Double.MAX_VALUE) WidgetType.TEXTBOX else WidgetType.SLIDER): ValidatedNumber<Double>(defaultValue, minValue, maxValue, widgetType) {

    /**
     * A validated double number with a default selected from the min of the allowable range.
     * @param minValue Double. the minimum allowed value, inclusive
     * @param maxValue Double. the maximum allowed value, inclusive
     * @param widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(minValue: Double, maxValue: Double, widgetType: WidgetType = WidgetType.SLIDER): this(minValue, maxValue, minValue, widgetType)

    /**
     * an unbounded validated double number.
     *
     * The validation will be limited to ensuring the value de/serializes as a double, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     * @param defaultValue Double. the default value of this wrapper
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: Double): this(defaultValue, Double.MAX_VALUE, -Double.MAX_VALUE, WidgetType.TEXTBOX)

    /**
     * an unbounded validated double number with default of 0.0.
     *
     * The validation will be limited to ensuring the value de/serializes as a double, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this(0.0, Double.MAX_VALUE, -Double.MAX_VALUE, WidgetType.TEXTBOX)

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Double> {
        return try {
            ValidationResult.success(toml.asTomlLiteral().toDouble())
        } catch (e: Throwable) {
            ValidationResult.error(defaultValue, "Problem deserializing ValidatedDouble [$fieldName]: ${e.localizedMessage}")
        }
    }

    @Internal
    override fun serialize(input: Double): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    /**
     * creates a deep copy of this ValidatedDouble
     * return ValidatedDouble wrapping the current double value and validation restrictions
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedDouble {
        return ValidatedDouble(copyStoredValue(), maxValue, minValue, widgetType)
    }

    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is Double && validateEntry(input, EntryValidator.ValidationType.STRONG).isValid()
    }

    @Internal
    override var increment: Double? = null

    @Internal
    override fun convert(input: Double): ValidationResult<Double> {
        return ValidationResult.success(input)
    }

    @Internal
    override fun minBound(): Double {
        return -Double.MAX_VALUE
    }

    @Internal
    override fun maxBound(): Double {
        return Double.MAX_VALUE
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        val validation = if(minValue==-Double.MAX_VALUE && maxValue== Double.MAX_VALUE)
            "Unbounded"
        else if(minValue == -Double.MAX_VALUE)
            "less than $maxValue"
        else if (maxValue == Double.MAX_VALUE)
            "greater than $minValue"
        else
            "between $minValue and $maxValue"
        return "Validated Double[value=$storedValue, validation=$validation]"
    }

    /**
     * Annotation-driven validation for Doubles
     * @param min: Double - minimum allowable value, default to Double.MIN_VALUE
     * @param max: Double - maximum allowable value, default to Double.MAX_VALUE
     * @param type: [ValidatedNumber.WidgetType] - The "style" of the GUI widget for the annotated setting
     * @author fzzyhmstrs
     * @since 0.2.0, added widget type 0.6.3
     */
    annotation class Restrict(val min: Double = -Double.MAX_VALUE, val max: Double = Double.MAX_VALUE, val type: WidgetType = WidgetType.SLIDER)
}