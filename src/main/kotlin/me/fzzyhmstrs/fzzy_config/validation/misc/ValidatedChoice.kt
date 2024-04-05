package me.fzzyhmstrs.fzzy_config.validation.misc

import me.fzzyhmstrs.fzzy_config.entry.EntryHandler
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.also
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement

//@sample me.fzzyhmstrs.fzzy_config.examples.ExampleTranslations.lang
class ValidatedChoice<T: Any>(defaultValue: T, private val choices: List<T>, private val handler: EntryHandler<T>, widgetType: WidgetType = WidgetType.POPUP): ValidatedField<T>(defaultValue) {

    constructor(choices: List<T>,handler: EntryHandler<T>): this(choices[0],choices, handler)

    init{
        if (!choices.contains(defaultValue))
            throw IllegalStateException("Default value [$defaultValue] of ValidatedChoices not within valid choice lists [$choices]")
        if(choices.isEmpty())
            throw IllegalStateException("ValidatedChoice can't have empty choice list")
    }
    override fun copyStoredValue(): T {
        return storedValue
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<T> {
        return try {
            val errors = mutableListOf<String>()
            val value =  handler.deserializeEntry(toml,errors,fieldName,true).report(errors)
            if (errors.isNotEmpty()) {
                ValidationResult.error(value.get(), "Error(s) encountered while deserializing choice: $errors")
            } else {
                value
            }
        } catch (e: Exception){
            ValidationResult.error(storedValue,"Critical error deserializing choices [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serialize(input: T): ValidationResult<TomlElement> {
        return ValidationResult.success(handler.serializeEntry(input, mutableListOf(), true))
    }

    override fun correctEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        return validateEntry(input, type)
    }

    override fun validateEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        return handler.validateEntry(input,type).also(choices.contains(input),"[$input] not a valid choice: [$choices]")
    }

    @Environment(EnvType.CLIENT)
    override fun widgetEntry(choicePredicate: ChoiceValidator<T>): ClickableWidget {
        TODO()
    }

    override fun instanceEntry(): ValidatedChoice<T> {
        return ValidatedChoice(copyStoredValue(),this.choices,this.handler)
    }

    override fun isValidEntry(input: Any?): Boolean {
        if (input == null) return false
        return try {
            input::class.java == defaultValue::class.java && validateEntry(input as T, EntryValidator.ValidationType.STRONG).isValid()
        } catch (e: Exception){
            false
        }
    }

    /**
     * Determines which type of selector widget will be used for the Enum option, default is POPUP
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    enum class WidgetType {
        /**
         * Will display a button with the currently selected option, clicking the button will pop up a window with the available options to select from. Selecting a new option will close the popup.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        POPUP,
        /**
         * A traditional MC cycling button widget, iterating through the enum options in order
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        CYCLING
    }
}