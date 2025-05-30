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
import net.peanuuutz.tomlkt.toShort
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * A validated short number. This field is a wrapper of shorts that is both a [java.util.function.Supplier] and [java.util.function.Consumer] of type Short
 *
 * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Numbers) for more details and examples.
 * @param defaultValue Short. the default value of this wrapper
 * @param maxValue Short. the maximum allowed value, inclusive
 * @param minValue Short. the minimum allowed value, inclusive
 * @property widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.shorts
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ValidatedShort @JvmOverloads constructor(defaultValue: Short, maxValue: Short, minValue: Short, widgetType: WidgetType = if(maxValue == Short.MAX_VALUE || minValue == Short.MIN_VALUE) WidgetType.TEXTBOX else WidgetType.SLIDER): ValidatedNumber<Short>(defaultValue, minValue, maxValue, widgetType) {

    /**
     * A validated short number with a default selected from the min of the allowable range.
     *
     * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Numbers) for more details and examples.
     * @param minValue Short. the minimum allowed value, inclusive
     * @param maxValue Short. the maximum allowed value, inclusive
     * @param widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(minValue: Short, maxValue: Short, widgetType: WidgetType = WidgetType.SLIDER): this(minValue, maxValue, minValue, widgetType)

    /**
     * an unbounded validated short number.
     *
     * The validation will be limited to ensuring the value de/serializes as a short, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     *
     * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Numbers) for more details and examples.
     * @param defaultValue Short. the default value of this wrapper
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: Short): this(defaultValue, Short.MAX_VALUE, Short.MIN_VALUE, WidgetType.TEXTBOX)

    /**
     * an unbounded validated short number with default value 0.
     *
     * The validation will be limited to ensuring the value de/serializes as a short, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     *
     * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Numbers) for more details and examples.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this(0, Short.MAX_VALUE, Short.MIN_VALUE, WidgetType.TEXTBOX)

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Short> {
        return try {
            ValidationResult.success(toml.asTomlLiteral().toShort())
        } catch (e: Throwable) {
            ValidationResult.error(defaultValue, ValidationResult.Errors.DESERIALIZATION, "Problem deserializing short [$fieldName]: ${e.localizedMessage}")
        }
    }

    @Internal
    override fun serialize(input: Short): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    /**
     * creates a deep copy of this ValidatedShort
     * return ValidatedShort wrapping the current short value and validation restrictions
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedShort {
        return ValidatedShort(defaultValue, maxValue, minValue, widgetType)
    }

    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is Short && validateEntry(input, EntryValidator.ValidationType.STRONG).isValid()
    }

    override var increment: Short? = null

    override fun convert(input: Double): ValidationResult<Short> {
        return ValidationResult.predicated(
            input.toInt().toShort(),
            input % 1 == 0.0 && input.toLong() == input.toInt().toShort().toLong(),
            ValidationResult.Errors.OUT_OF_BOUNDS) { b -> b.content("[$input] not a valid Short") }
    }

    override fun minBound(): Short {
        return Short.MIN_VALUE
    }

    override fun maxBound(): Short {
        return Short.MAX_VALUE
    }

    override fun toString(): String {
        val validation = if(minValue==Short.MIN_VALUE && maxValue== Short.MAX_VALUE)
            "Unbounded"
        else if(minValue == Short.MIN_VALUE)
            "less than $maxValue"
        else if (maxValue == Short.MAX_VALUE)
            "greater than $minValue"
        else
            "between $minValue and $maxValue"
        return "Validated Short[value=$storedValue, validation=$validation]"
    }

    /**
     * Annotation-driven validation for Shorts
     * @param min: Short - minimum allowable value, default to Short.MIN_VALUE
     * @param max: Short - maximum allowable value, default to Short.MAX_VALUE
     * @param type: [ValidatedNumber.WidgetType] - The "style" of the GUI widget for the annotated setting
     * @author fzzyhmstrs
     * @since 0.2.0, added widget type 0.6.3
     */
    annotation class Restrict(val min: Short = Short.MIN_VALUE, val max: Short = Short.MAX_VALUE, val type: WidgetType = WidgetType.SLIDER)
}