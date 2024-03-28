package me.fzzyhmstrs.fzzy_config.validation.number

import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.entry.ChoiceValidator
import me.fzzyhmstrs.fzzy_config.validation.entry.Entry
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toFloat

/**
 * A validated float number
 *
 * @param defaultValue Float. the default value of this wrapper
 * @param maxValue Float. the maximum allowed value, inclusive
 * @param minValue Float. the minimum allowed value, inclusive
 * @property widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.validatedFloat]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.textBoxFloat]
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ValidatedFloat @JvmOverloads constructor(defaultValue: Float, maxValue: Float, minValue: Float, private val widgetType: WidgetType = WidgetType.SLIDER): ValidatedNumber<Float>(defaultValue, minValue, maxValue) {

    /**
     * an unbounded validated float number.
     *
     * The validation will be limited to ensuring the value de/serializes as a float, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     * @param defaultValue Float. the default value of this wrapper
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.unboundedFloat]
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
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.emptyFloat]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this(0f, Float.MAX_VALUE, -Float.MAX_VALUE, WidgetType.TEXTBOX)

    override fun copyStoredValue(): Float {
        return storedValue
    }

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

    override fun instanceEntry(): Entry<Float> {
        return ValidatedFloat(defaultValue, maxValue, minValue, widgetType)
    }

    override fun widgetEntry(choicePredicate: ChoiceValidator<Float>): ClickableWidget {
        TODO("Not yet implemented")
    }
}