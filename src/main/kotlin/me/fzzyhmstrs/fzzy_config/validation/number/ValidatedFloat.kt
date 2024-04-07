package me.fzzyhmstrs.fzzy_config.validation.number

import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toFloat

/**
 * A validated float number
 * @param defaultValue Float. the default value of this wrapper
 * @param maxValue Float. the maximum allowed value, inclusive
 * @param minValue Float. the minimum allowed value, inclusive
 * @property widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.validatedFloat
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.textBoxFloat
 * @sample me.fzzyhmstrs.fzzy_config.examples.ExampleTranslations.fieldLang
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ValidatedFloat @JvmOverloads constructor(defaultValue: Float, maxValue: Float, minValue: Float, widgetType: WidgetType = WidgetType.SLIDER): ValidatedNumber<Float>(defaultValue, minValue, maxValue, widgetType) {

    /**
     * A validated float number with a default selected from the min of the allowable range.
     * @param minValue Float. the minimum allowed value, inclusive
     * @param maxValue Float. the maximum allowed value, inclusive
     * @param widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.minMaxFloat
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(minValue: Float,maxValue: Float, widgetType: WidgetType = WidgetType.SLIDER): this(minValue, maxValue, minValue, widgetType)

    /**
     * an unbounded validated float number.
     *
     * The validation will be limited to ensuring the value de/serializes as a float, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     * @param defaultValue Float. the default value of this wrapper
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.unboundedFloat
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: Float): this(defaultValue, Float.MAX_VALUE, -Float.MAX_VALUE, WidgetType.TEXTBOX)

    /**
     * an unbounded validated float number with default of 0f.
     *
     * The validation will be limited to ensuring the value de/serializes as a float, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.emptyFloat
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this(0f, Float.MAX_VALUE, -Float.MAX_VALUE, WidgetType.TEXTBOX)

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Float> {
        return try{
            ValidationResult.success(toml.asTomlLiteral().toFloat())
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Problem deserializing ValidatedInt [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serialize(input: Float): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    override fun instanceEntry(): ValidatedFloat {
        return ValidatedFloat(defaultValue, maxValue, minValue, widgetType)
    }

    override fun isValidEntry(input: Any?): Boolean {
        return input is Float && validateEntry(input, EntryValidator.ValidationType.STRONG).isValid()
    }

    override fun convert(input: Double): ValidationResult<Float> {
        return ValidationResult.predicated(input.toFloat(),input <= Float.MAX_VALUE.toDouble() && input >= (-Float.MAX_VALUE).toDouble(),"[$input] out of Bounds for float value (${-Float.MIN_VALUE} to ${Float.MAX_VALUE} )")
    }

    override fun toString(): String {
        val validation = if(minValue==-Float.MAX_VALUE && maxValue== Float.MAX_VALUE)
            "Unbounded"
        else if(minValue == -Float.MAX_VALUE)
            "less than $maxValue"
        else if (maxValue == Float.MAX_VALUE)
            "greater than $minValue"
        else
            "between $minValue and $maxValue"
        return "Validated Float[value=$storedValue, validation=$validation]"
    }
}