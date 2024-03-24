package me.fzzyhmstrs.fzzy_config.validated_field.number

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validated_field.entry.Entry
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toLong

/**
 * A validated long number
 *
 * @param defaultValue Long. the default value of this wrapper
 * @param maxValue Long. the maximum allowed value, inclusive
 * @param minValue Long. the minimum allowed value, inclusive
 * @property widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validated_field.number.ValidatedNumber.WidgetType]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.validatedLong]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.textBoxLong]
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ValidatedLong @JvmOverloads constructor(defaultValue: Long, maxValue: Long, minValue: Long, private val widgetType: WidgetType = WidgetType.SLIDER): ValidatedNumber<Long>(defaultValue, minValue, maxValue) {

    /**
     * an unbounded validated long number.
     *
     * The validation will be limited to ensuring the value de/serializes as a long, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validated_field.number.ValidatedNumber.WidgetType.TEXTBOX]
     * @param defaultValue Long. the default value of this wrapper
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.unboundedLong]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: Long): this(defaultValue, Long.MAX_VALUE, Long.MIN_VALUE, WidgetType.TEXTBOX)
    override fun copyStoredValue(): Long {
        return storedValue
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Long> {
        return try{
            ValidationResult.success(toml.asTomlLiteral().toLong())
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Problem deserializing ValidatedLong [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serialize(input: Long): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    override fun instanceEntry(): Entry<Long> {
        return ValidatedLong(defaultValue, maxValue, minValue, widgetType)
    }

    override fun widgetEntry(): ClickableWidget {
        TODO("Not yet implemented")
    }
}