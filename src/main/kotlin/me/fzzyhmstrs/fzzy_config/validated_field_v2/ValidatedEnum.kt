package me.fzzyhmstrs.fzzy_config.validated_field_v2

import me.fzzyhmstrs.fzzy_config.api.StringTranslatable
import me.fzzyhmstrs.fzzy_config.api.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.client.gui.widget.Widget
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral

class ValidatedEnum<T>(protected val defaultValue: T, private val enum: Class<T>, private val widgetType: WidgetType = WidgetType.POPUP): ValidatedField<T>(defaultValue) where T: Enum<T>, T:StringTranslatable {

    private val valuesMap: Map<String, T> = enum.enumConstants.associateBy { it.name }
    override fun copyStoredValue(): T {
        return storedValue
    }

    override fun deserializeEntry(toml: TomlElement, fieldName: String): ValidationResult<T> {
        return try {
            val string = toml.toString()
            val chkEnum = valuesMap[string] ?: return ValidationResult.error(storedValue,"Invalid enum selection at setting [$fieldName]. Possible values are: [${valuesMap.keys}]")
            ValidationResult.success(chkEnum)
        } catch (e: Exception){
            ValidationResult.error(storedValue,"Critical error deserializing enum [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serializeEntry(input: T): TomlElement {
        return TomlLiteral(input.name)
    }
    override fun validateAndCorrectInputs(input: T): ValidationResult<T> {
        return ValidationResult.success(input)
    }

    override fun validate(input: T): ValidationResult<T> {
        return ValidationResult.success(input)
    }

    override fun reset() {
        validateAndSet(defaultValue)
    }

    override fun createEntry(name: Text, desc: Text): ConfigEntry {
        TODO("Not yet implemented")
    }

    override fun description(): Text {
        return FcText.translatable(descriptionKey(),valuesMap.keys.toString())
    }

    /**
     * Determines which type of selector widget will be used for the Enum option, default is POPUP
     */
    enum class WidgetType{
        /**
         * WIll display a button with the current option, clicking the button will pop up a window with the available options to click and select from.
         */
        POPUP,

        /**
         * A traditional MC cycling button widget, iterating through the enum options in order
         */
        CYCLING
    }

}
