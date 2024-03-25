package me.fzzyhmstrs.fzzy_config.validation.number

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.entry.Entry
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toInt

/**
 * A validated integer number
 *
 * @param defaultValue Int. the default value of this wrapper
 * @param maxValue Int. the maximum allowed value, inclusive
 * @param minValue Int. the minimum allowed value, inclusive
 * @property widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.validatedInt]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.textBoxInt]
 * @author fzzyhmstrs
 * @since 0.1.0
 */

class ValidatedInt @JvmOverloads constructor(defaultValue: Int, maxValue: Int, minValue: Int, private val widgetType: WidgetType = WidgetType.SLIDER): ValidatedNumber<Int>(defaultValue, minValue, maxValue) {

    /**
     * an unbounded validated int number.
     *
     * The validation will be limited to ensuring the value de/serializes as an int, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     * @param defaultValue Int. the default value of this wrapper
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.unboundedInt]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: Int): this(defaultValue, Int.MAX_VALUE, Int.MIN_VALUE, WidgetType.TEXTBOX)

    override fun copyStoredValue(): Int {
        return storedValue
    }

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

    override fun instanceEntry(): Entry<Int> {
        return ValidatedInt(defaultValue, maxValue, minValue, widgetType)
    }

    override fun widgetEntry(): ClickableWidget {
        TODO("Not yet implemented")
    }
}