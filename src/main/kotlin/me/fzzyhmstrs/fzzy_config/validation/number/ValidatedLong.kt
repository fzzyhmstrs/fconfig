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
import net.peanuuutz.tomlkt.toLong
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * A validated long number. This field is a wrapper of longs that is both a [java.util.function.Supplier] and [java.util.function.Consumer] of type Long
 *
 * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Numbers) for more details and examples.
 * @param defaultValue Long. the default value of this wrapper
 * @param maxValue Long. the maximum allowed value, inclusive
 * @param minValue Long. the minimum allowed value, inclusive
 * @property widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.longs
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ValidatedLong @JvmOverloads constructor(defaultValue: Long, maxValue: Long, minValue: Long, widgetType: WidgetType = if(maxValue == Long.MAX_VALUE || minValue == Long.MIN_VALUE) WidgetType.TEXTBOX else WidgetType.SLIDER): ValidatedNumber<Long>(defaultValue, minValue, maxValue, widgetType) {

    /**
     * A validated long number generated with a [LongRange].
     *
     * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Numbers) for more details and examples.
     * @param defaultValue Long. the default value of this wrapper
     * @param defaultValue Long. the default value of this wrapper
     * @param widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(defaultValue: Long, range: LongRange, widgetType: WidgetType = WidgetType.SLIDER): this(defaultValue, range.last, range.first, widgetType)

    /**
     * A validated long number with a default selected from the min of the allowable range.
     *
     * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Numbers) for more details and examples.
     * @param range LongRange. the allowable range of this Validated Long
     * @param widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(range: LongRange, widgetType: WidgetType = WidgetType.SLIDER): this(range.first, range.last, range.first, widgetType)

    /**
     * A validated long number with a default selected from the min of the allowable range.
     *
     * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Numbers) for more details and examples.
     * @param minValue Long. the minimum allowed value, inclusive
     * @param maxValue Long. the maximum allowed value, inclusive
     * @param widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(minValue: Long, maxValue: Long, widgetType: WidgetType = WidgetType.SLIDER): this(minValue, maxValue, minValue, widgetType)

    /**
     * an unbounded validated long number.
     *
     * The validation will be limited to ensuring the value de/serializes as a long, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     *
     * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Numbers) for more details and examples.
     * @param defaultValue Long. the default value of this wrapper
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: Long): this(defaultValue, Long.MAX_VALUE, Long.MIN_VALUE, WidgetType.TEXTBOX)

    /**
     * an unbounded validated long number with default value 0L.
     *
     * The validation will be limited to ensuring the value de/serializes as a long, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     *
     * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Numbers) for more details and examples.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this(0L, Long.MAX_VALUE, Long.MIN_VALUE, WidgetType.TEXTBOX)

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Long> {
        return try {
            ValidationResult.success(toml.asTomlLiteral().toLong())
        } catch (e: Throwable) {
            ValidationResult.error(defaultValue, ValidationResult.Errors.DESERIALIZATION, "Exception deserializing long [$fieldName]", e)
        }
    }

    @Internal
    override fun serialize(input: Long): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    /**
     * creates a deep copy of this ValidatedLong
     * return ValidatedLong wrapping the current long value and validation restrictions
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedLong {
        return ValidatedLong(defaultValue, maxValue, minValue, widgetType)
    }

    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is Long && validateEntry(input, EntryValidator.ValidationType.STRONG).isValid()
    }

    @Internal
    override var increment: Long? = null

    @Internal
    override fun convert(input: Double): ValidationResult<Long> {
        return ValidationResult.predicated(
            input.toLong(),
            input % 1 == 0.0,
            ValidationResult.Errors.OUT_OF_BOUNDS) { b -> b.content("[$input] not a valid Long") }
    }

    @Internal
    override fun minBound(): Long {
        return Long.MIN_VALUE
    }

    @Internal
    override fun maxBound(): Long {
        return Long.MAX_VALUE
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        val validation = if(minValue==Long.MIN_VALUE && maxValue== Long.MAX_VALUE)
            "Unbounded"
        else if(minValue == Long.MIN_VALUE)
            "less than $maxValue"
        else if (maxValue == Long.MAX_VALUE)
            "greater than $minValue"
        else
            "between $minValue and $maxValue"
        return "Validated Long[value=$storedValue, validation=$validation]"
    }

    /**
     * Annotation-driven validation for Longs
     * @param min: Long - minimum allowable value, default to Long.MIN_VALUE
     * @param max: Long - maximum allowable value, default to Long.MAX_VALUE
     * @param type: [ValidatedNumber.WidgetType] - The "style" of the GUI widget for the annotated setting
     * @author fzzyhmstrs
     * @since 0.2.0, added widget type 0.6.3
     */
    annotation class Restrict(val min: Long = Long.MIN_VALUE, val max: Long = Long.MAX_VALUE, val type: WidgetType = WidgetType.SLIDER)
}