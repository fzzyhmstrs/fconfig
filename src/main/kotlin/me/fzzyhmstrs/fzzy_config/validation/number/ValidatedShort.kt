package me.fzzyhmstrs.fzzy_config.validation.number

import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import me.fzzyhmstrs.fzzy_config.validation.entry.Entry
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toShort

/**
 * A validated short number
 *
 * @param defaultValue Short. the default value of this wrapper
 * @param maxValue Short. the maximum allowed value, inclusive
 * @param minValue Short. the minimum allowed value, inclusive
 * @property widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.validatedShort]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.textBoxShort]
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ValidatedShort @JvmOverloads constructor(defaultValue: Short, maxValue: Short, minValue: Short, private val widgetType: WidgetType = WidgetType.SLIDER): ValidatedNumber<Short>(defaultValue, minValue, maxValue) {

    /**
     * an unbounded validated short number.
     *
     * The validation will be limited to ensuring the value de/serializes as a short, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     * @param defaultValue Short. the default value of this wrapper
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.unboundedShort]
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
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.emptyShort]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this(0, Short.MAX_VALUE, Short.MIN_VALUE, WidgetType.TEXTBOX)

    override fun copyStoredValue(): Short {
        return storedValue
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Short> {
        return try{
            ValidationResult.success(toml.asTomlLiteral().toShort())
        } catch (e: Exception){
            ValidationResult.error(defaultValue,"Problem deserializing ValidatedShort [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serialize(input: Short): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    override fun instanceEntry(): Entry<Short> {
        return ValidatedShort(defaultValue, maxValue, minValue, widgetType)
    }

    override fun widgetEntry(choicePredicate: ChoiceValidator<Short>): ClickableWidget {
        TODO("Not yet implemented")
    }
}