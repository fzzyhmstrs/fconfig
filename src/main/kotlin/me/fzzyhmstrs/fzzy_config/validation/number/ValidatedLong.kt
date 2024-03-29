package me.fzzyhmstrs.fzzy_config.validation.number

import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import me.fzzyhmstrs.fzzy_config.entry.Entry
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
 * @property widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.validatedLong]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.textBoxLong]
 * @sample me.fzzyhmstrs.fzzy_config.examples.ExampleTranslations.fieldLang
 * @author fzzyhmstrs
 * @since 0.1.0
 */
class ValidatedLong @JvmOverloads constructor(defaultValue: Long, maxValue: Long, minValue: Long, private val widgetType: WidgetType = WidgetType.SLIDER): ValidatedNumber<Long>(defaultValue, minValue, maxValue) {

    /**
     * A validated long number generated with a [LongRange].
     * @param defaultValue Long. the default value of this wrapper
     * @param defaultValue Long. the default value of this wrapper
     * @param widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.rangedDefaultedLong]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(defaultValue: Long, range: LongRange, widgetType: WidgetType = WidgetType.SLIDER): this(defaultValue, range.last, range.first, widgetType)

    /**
     * A validated long number with a default selected from the min of the allowable range.
     * @param range LongRange. the allowable range of this Validated Long
     * @param widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.rangedLong]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(range: LongRange, widgetType: WidgetType = WidgetType.SLIDER): this(range.first, range.last, range.first, widgetType)

    /**
     * A validated long number with a default selected from the min of the allowable range.
     * @param minValue Long. the minimum allowed value, inclusive
     * @param maxValue Long. the maximum allowed value, inclusive
     * @param widgetType [WidgetType][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType] defines what the config GUI widget looks like
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.minMaxLong]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(minValue: Long, maxValue: Long, widgetType: WidgetType = WidgetType.SLIDER): this(minValue, maxValue, minValue, widgetType)

    /**
     * an unbounded validated long number.
     *
     * The validation will be limited to ensuring the value de/serializes as a long, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     * @param defaultValue Long. the default value of this wrapper
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.unboundedLong]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: Long): this(defaultValue, Long.MAX_VALUE, Long.MIN_VALUE, WidgetType.TEXTBOX)

    /**
     * an unbounded validated long number with default value 0L.
     *
     * The validation will be limited to ensuring the value de/serializes as a long, since there are no bounds.
     *
     * The widget type is locked to [WidgetType.TEXTBOX][me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber.WidgetType.TEXTBOX]
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedNumberExamples.emptyLong]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this(0L, Long.MAX_VALUE, Long.MIN_VALUE, WidgetType.TEXTBOX)

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

    override fun widgetEntry(choicePredicate: ChoiceValidator<Long>): ClickableWidget {
        TODO("Not yet implemented")
    }
}