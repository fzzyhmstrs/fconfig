package me.fzzyhmstrs.fzzy_config.validation.number

import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import me.fzzyhmstrs.fzzy_config.validation.entry.Entry
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toDouble

/**
 * A validated double number
 *
 * @param defaultValue Double. the default value of this wrapper
 * @param maxValue Double. the maximum allowed value, inclusive
 * @param minValue Double. the minimum allowed value, inclusive
 * @property widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.validatedDouble]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.textBoxDouble]
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ValidatedDouble @JvmOverloads constructor(defaultValue: Double, maxValue: Double, minValue: Double, private val widgetType: WidgetType = WidgetType.SLIDER): ValidatedNumber<Double>(defaultValue, minValue, maxValue) {

    /**
     * an unbounded validated double number.
     *
     * The validation will be limited to ensuring the value de/serializes as a double, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     * @param defaultValue Double. the default value of this wrapper
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.unboundedDouble]
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
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.emptyDouble]
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

    override fun instanceEntry(): Entry<Double> {
        return ValidatedDouble(defaultValue, maxValue, minValue, widgetType)
    }

    override fun widgetEntry(choicePredicate: ChoiceValidator<Double>): ClickableWidget {
        TODO("Not yet implemented")
    }
}