package me.fzzyhmstrs.fzzy_config.validation.misc

import me.fzzyhmstrs.fzzy_config.screen.widget.ActiveButtonWidget
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.toBoolean
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * a validated boolean value
 *
 * Since there is basically nothing to validate on a boolean, this primarily serves to parse and correct issues with de/serialization.
 * @param defaultValue the default boolean state
 * @see [me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.validatedBool]
 * @sample me.fzzyhmstrs.fzzy_config.examples.ExampleTranslations.fieldLang
 * @author fzzyhmstrs
 * since 0.1.0
 */
class ValidatedBoolean(defaultValue: Boolean): ValidatedField<Boolean>(defaultValue) {

    /**
     * A validated boolean value wth  default 'true' value
     *
     * Since there is basically nothing to validate on a boolean, this primarily serves to parse and correct issues with de/serialization.
     * @see [me.fzzyhmstrs.fzzy_config.validation.Shorthand.validated]
     * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.validatedBool]
     * @author fzzyhmstrs
     * since 0.2.0
     */
    constructor(): this(true)

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Boolean> {
        return try {
            ValidationResult.success(toml.asTomlLiteral().toBoolean())
        } catch (e: Exception){
            ValidationResult.error(storedValue,"Critical error deserializing boolean [$fieldName]: ${e.localizedMessage}")
        }
    }
    @Internal
    override fun serialize(input: Boolean): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    override fun instanceEntry(): ValidatedBoolean {
        return ValidatedBoolean(copyStoredValue())
    }
    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is Boolean
    }
    @Internal
    @Environment(EnvType.CLIENT)
    override fun widgetEntry(choicePredicate: ChoiceValidator<Boolean>): ClickableWidget {
        return ActiveButtonWidget(
            {if(get()) "fc.validated_field.boolean.true".translate() else "fc.validated_field.boolean.false".translate()},
            110,
            20,
            { true },
            { setAndUpdate(!get()) }
        )
    }

    override fun toString(): String {
        return "Validated Boolean[value=$storedValue, validation=true or false]"
    }
}