package me.fzzyhmstrs.fzzy_config.validation.number

import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import me.fzzyhmstrs.fzzy_config.validation.entry.Entry
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toByte

/**
 * A validated byte number
 *
 * @param defaultValue Byte. the default value of this wrapper
 * @param maxValue Byte. the maximum allowed value, inclusive
 * @param minValue Byte. the minimum allowed value, inclusive
 * @property widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.validatedByte]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.textBoxByte]
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ValidatedByte @JvmOverloads constructor(defaultValue: Byte, maxValue: Byte, minValue: Byte, private val widgetType: WidgetType = WidgetType.SLIDER): ValidatedNumber<Byte>(defaultValue, minValue, maxValue) {

    /**
     * an unbounded validated byte number.
     *
     * The validation will be limited to ensuring the value de/serializes as a byte, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     * @param defaultValue Byte. the default value of this wrapper
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.unboundedByte]
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
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.emptyByte]
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

    override fun instanceEntry(): Entry<Byte> {
        return ValidatedByte(defaultValue, maxValue, minValue, widgetType)
    }

    override fun widgetEntry(choicePredicate: ChoiceValidator<Byte>): ClickableWidget {
        TODO("Not yet implemented")
    }
}