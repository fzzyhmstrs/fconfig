package me.fzzyhmstrs.fzzy_config.validation.number

import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toDouble

/**
 * A validated double number
 * @param defaultValue Double. the default value of this wrapper
 * @param maxValue Double. the maximum allowed value, inclusive
 * @param minValue Double. the minimum allowed value, inclusive
 * @property widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.validatedDouble
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.textBoxDouble
 * @sample me.fzzyhmstrs.fzzy_config.examples.ExampleTranslations.fieldLang
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ValidatedDouble @JvmOverloads constructor(defaultValue: Double, maxValue: Double, minValue: Double, widgetType: WidgetType = WidgetType.SLIDER): ValidatedNumber<Double>(defaultValue, minValue, maxValue, widgetType) {

    /**
     * A validated double number with a default selected from the min of the allowable range.
     * @param minValue Double. the minimum allowed value, inclusive
     * @param maxValue Double. the maximum allowed value, inclusive
     * @param widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.minMaxDouble
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
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.unboundedDouble
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
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.emptyDouble
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this(0.0, Double.MAX_VALUE, -Double.MAX_VALUE, WidgetType.TEXTBOX)

    override fun copyStoredValue(): Double {
        return storedValue
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Double> {
        return try{
            ValidationResult.success(toml.asTomlLiteral().toDouble())
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Problem deserializing ValidatedDouble [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serialize(input: Double): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    override fun instanceEntry(): ValidatedDouble {
        return ValidatedDouble(copyStoredValue(), maxValue, minValue, widgetType)
    }

    override fun convert(input: Double): Double {
        return input
    }

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
}