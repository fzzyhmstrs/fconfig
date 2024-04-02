package me.fzzyhmstrs.fzzy_config.validation.number

import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toByte

/**
 * A validated byte number
 * @param defaultValue Byte. the default value of this wrapper
 * @param maxValue Byte. the maximum allowed value, inclusive
 * @param minValue Byte. the minimum allowed value, inclusive
 * @property widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.validatedByte
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.textBoxByte
 * @sample me.fzzyhmstrs.fzzy_config.examples.ExampleTranslations.fieldLang
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ValidatedByte @JvmOverloads constructor(defaultValue: Byte, maxValue: Byte, minValue: Byte, widgetType: WidgetType = WidgetType.SLIDER): ValidatedNumber<Byte>(defaultValue, minValue, maxValue, widgetType) {

    /**
     * A validated byte number with a default selected from the min of the allowable range.
     * @param minValue Byte. the minimum allowed value, inclusive
     * @param maxValue Byte. the maximum allowed value, inclusive
     * @param widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.minMaxByte
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
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.unboundedByte
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
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.emptyByte
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this(0, Byte.MAX_VALUE, Byte.MIN_VALUE, WidgetType.TEXTBOX)

    override fun copyStoredValue(): Byte {
        return storedValue
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Byte> {
        return try{
            ValidationResult.success(toml.asTomlLiteral().toByte())
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Problem deserializing ValidatedByte [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serialize(input: Byte): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    override fun instanceEntry(): ValidatedByte {
        return ValidatedByte(copyStoredValue(), maxValue, minValue, widgetType)
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
}