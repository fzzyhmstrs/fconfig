package me.fzzyhmstrs.fzzy_config.validation.misc

import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.wrap
import me.fzzyhmstrs.fzzy_config.math.Expression
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.text.MutableText
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral

/**
 * A validated math expression
 *
 * This [ValidatedField] is itself an expression, so you can call eval() or evalSafe() on it directly
 * @param defaultValue String representation of the desired math expression, parsed to a cached [Expression] internally.
 * @param validVars Set<Char> representing the valid variable characters the user can utilize in their expression.
 * @param validator [EntryValidator], validates entered math strings
 * @Sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.validatedExpression]
 * @Sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.evalExpression]
 * @sample me.fzzyhmstrs.fzzy_config.examples.ExampleTranslations.fieldLang
 * @throws IllegalStateException if the provided defaultValue is not a parsable Expression.
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class ValidatedExpression @JvmOverloads constructor(defaultValue: String, private val validVars: Set<Char> = setOf(), private val validator: EntryValidator<String> = EntryValidator{ i, _ -> Expression.tryTest(i, validVars).wrap(i)})
    :
    ValidatedField<String>(defaultValue),
    Expression
{

    /**
     * A validated math expression with default equation of "0"
     *
     * This constructor is primarily intended for validation usage in other ValidatedFields (such as lists or maps)
     *
     * This [ValidatedField] is itself an expression, so you can call eval() or evalSafe() on it directly.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this("0")

    private var parsedString = defaultValue
    private var parsedExpression = Expression.parse(defaultValue, defaultValue)

    @Deprecated("Where possible use safeEval() to avoid throwing exceptions on evaluation failure")
    override fun eval(vars: Map<Char,Double>): Double {
        if (parsedString != storedValue) {
            val tryExpression = try {
                Expression.parse(storedValue, storedValue)
            } catch(e: Exception) {
                parsedExpression
            }
            parsedExpression = tryExpression
        }
        return parsedExpression.eval(vars)
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<String> {
        return try {
            val string = toml.toString()
            ValidationResult.success(string)
        } catch (e: Exception) {
            ValidationResult.error(storedValue,"Critical error deserializing math expression [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serialize(input: String): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }

    override fun correctEntry(input: String, type: EntryValidator.ValidationType): ValidationResult<String> {
        val result = validateEntry(input, type)
        return if(result.isError()) {
            ValidationResult.error(storedValue, "Invalid identifier [$input] found, reset to [$storedValue]: ${result.getError()}")} else result
    }

    override fun validateEntry(input: String, type: EntryValidator.ValidationType): ValidationResult<String> {
        return validator.validateEntry(input, type)
    }

    override fun copyStoredValue(): String {
        return String(storedValue.toCharArray())
    }

    override fun instanceEntry(): Entry<String> {
        return ValidatedExpression(copyStoredValue(), validVars, validator)
    }

    @Environment(EnvType.CLIENT)
    override fun widgetEntry(choicePredicate: ChoiceValidator<String>): ClickableWidget {
        TODO("Not yet implemented")
    }

    @Environment(EnvType.CLIENT)
    class ExpressionPopupWidget(private val entry: ValidatedExpression): PressableWidget(0,0,90,20,entry.supplyEntry().lit()){

        override fun getNarrationMessage(): MutableText {
            return "fc.validated_field.expression".translate()
        }

        override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
            appendDefaultNarrations(builder)
        }

        override fun onPress() {
            TODO("Not yet implemented")
        }

    }
}