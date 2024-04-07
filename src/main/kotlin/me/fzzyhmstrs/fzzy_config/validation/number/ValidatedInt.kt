package me.fzzyhmstrs.fzzy_config.validation.number

import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toInt

/**
 * A validated integer number
 * @param defaultValue Int. the default value of this wrapper
 * @param maxValue Int. the maximum allowed value, inclusive
 * @param minValue Int. the minimum allowed value, inclusive
 * @property widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.validatedInt
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.textBoxInt
 * @sample me.fzzyhmstrs.fzzy_config.examples.ExampleTranslations.fieldLang
 * @sample me.fzzyhmstrs.fzzy_config.examples.java.ValidatedNumberJava.validatedInt
 * @author fzzyhmstrs
 * @since 0.1.0
 */

class ValidatedInt @JvmOverloads constructor(defaultValue: Int, maxValue: Int, minValue: Int, widgetType: WidgetType = WidgetType.SLIDER): ValidatedNumber<Int>(defaultValue, minValue, maxValue, widgetType) {

    /**
     * A validated int number generated with an [IntRange].
     * @param defaultValue Int. the default value of this wrapper
     * @param range [IntRange]. the allowable range of this Validated Int
     * @param widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.rangedDefaultedInt
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(defaultValue: Int, range: IntRange, widgetType: WidgetType = WidgetType.SLIDER): this(defaultValue, range.last, range.first, widgetType)

    /**
     * A validated int number with a default selected from the min of the allowable range.
     * @param range [IntRange]. the allowable range of this Validated Int
     * @param widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.rangedInt
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(range: IntRange, widgetType: WidgetType = WidgetType.SLIDER): this(range.first, range.last, range.first, widgetType)

    /**
     * A validated int number with a default selected from the min of the allowable range.
     * @param minValue Int. the minimum allowed value, inclusive
     * @param maxValue Int. the maximum allowed value, inclusive
     * @param widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.minMaxInt
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
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.unboundedInt
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
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.emptyInt
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this(0, Int.MAX_VALUE, Int.MIN_VALUE, WidgetType.TEXTBOX)

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Int> {
        return try{
            ValidationResult.success(toml.asTomlLiteral().toInt())
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Problem deserializing ValidatedInt [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serialize(input: Int): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    override fun instanceEntry(): ValidatedInt {
        return ValidatedInt(defaultValue, maxValue, minValue, widgetType)
    }

    override fun isValidEntry(input: Any?): Boolean {
        return input is Int && validateEntry(input, EntryValidator.ValidationType.STRONG).isValid()
    }

    override fun convert(input: Double): ValidationResult<Int> {
        return ValidationResult.predicated(input.toInt(),input.toLong() == input.toInt().toLong(),"[$input] out of Bounds for int value (${Int.MIN_VALUE} to ${Int.MAX_VALUE} )")
    }

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
}