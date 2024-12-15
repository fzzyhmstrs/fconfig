/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.validation.misc

import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget.Builder.Position
import me.fzzyhmstrs.fzzy_config.screen.widget.ValidationBackedTextFieldWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.util.Expression
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.wrap
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import org.jetbrains.annotations.ApiStatus.Internal

/**
 * A validated math expression
 *
 * This [ValidatedField] is itself an expression, so you can call eval() or evalSafe() on it directly
 * @param defaultValue String - representation of the desired math expression, parsed to a cached [Expression] internally.
 * @param validVars Set&lt;Char&gt; - represents the valid variable characters the user can utilize in their expression.
 * @param validator [EntryValidator], optional - validates entered math strings
 * @Sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.expressions
 * @throws IllegalStateException if the provided defaultValue is not a parsable Expression.
 * @author fzzyhmstrs
 * @since 0.2.0
 */
@Suppress("DEPRECATION")
open class ValidatedExpression @JvmOverloads constructor(
    defaultValue: String,
    private val validVars: Set<Char> = setOf(),
    private val validator: EntryValidator<String> = object: EntryValidator<String> {
        override fun validateEntry(input: String, type: EntryValidator.ValidationType): ValidationResult<String> {
            return Expression.tryTest(input, validVars).wrap(input)
        }
        override fun toString(): String {
            return "Dummy test with valid variable chars"
        }
    })
    :
    ValidatedField<String>(defaultValue),
    Expression
{

    /**
     * A validated math expression with default equation of "0"
     *
     * This constructor is primarily intended for validation in other ValidatedFields (such as lists or maps)
     *
     * This [ValidatedField] is itself an expression, so you can call eval() or evalSafe() on it directly.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this("0")

    private var parsedString = defaultValue
    private var parsedExpression = Expression.parse(defaultValue, defaultValue)

    @Deprecated("Where possible use safeEval() to avoid throwing exceptions on evaluation failure")
    override fun eval(vars: Map<Char, Double>): Double {
        if (parsedString != storedValue) {
            val tryExpression = try {
                Expression.parse(storedValue)
            } catch(e: Throwable) {
                parsedExpression
            }
            parsedExpression = tryExpression
        }
        return parsedExpression.eval(vars)
    }
    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<String> {
        return try {
            val string = toml.toString()
            ValidationResult.success(string)
        } catch (e: Throwable) {
            ValidationResult.error(storedValue, "Critical error deserializing math expression [$fieldName]: ${e.localizedMessage}")
        }
    }
    @Internal
    override fun serialize(input: String): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input))
    }
    @Internal
    override fun correctEntry(input: String, type: EntryValidator.ValidationType): ValidationResult<String> {
        val result = validateEntry(input, type)
        return if(result.isError()) {
            ValidationResult.error(storedValue, "Invalid identifier [$input] found, reset to [$storedValue]: ${result.getError()}")} else result
    }
    @Internal
    override fun validateEntry(input: String, type: EntryValidator.ValidationType): ValidationResult<String> {
        return validator.validateEntry(input, type)
    }
    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is String && validateEntry(input, EntryValidator.ValidationType.STRONG).isValid()
    }

    /**
     * Creates a deep copy of the stored value and returns it
     * @return String - deep copy of the string representation of the currently stored expression
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun copyStoredValue(): String {
        return String(storedValue.toCharArray())
    }

    /**
     * creates a deep copy of this ValidatedExpression
     * return ValidatedExpression wrapping a deep copy of the currently stored expression, valid variable, and validation
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedExpression {
        return ValidatedExpression(copyStoredValue(), validVars, validator)
    }
    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<String>): ClickableWidget {
        return ExpressionButtonWidget(this, choicePredicate)
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated Expression[value=$parsedExpression, vars=$validVars, validation=$validator]"
    }

    //client
    private class ExpressionButtonWidget(private val entry: ValidatedExpression, private val choiceValidator: ChoiceValidator<String>): CustomPressableWidget(0, 0, 110, 20, entry.get().lit()) {

        override fun getMessage(): Text {
            return entry.get().lit()
        }

        override fun getNarrationMessage(): MutableText {
            return "fc.validated_field.expression".translate().append(", ".lit()).append(this.message)
        }

        override fun onPress() {
            openExpressionEditPopup()
        }

        private fun openExpressionEditPopup() {
            val editBox = ValidationBackedTextFieldWidget(176, 20, entry, choiceValidator, entry, entry)
            fun add(s: String, moveCursor: Int) {
                val subText = editBox.selectedText
                val i = if(subText != "") {
                    editBox.text.indexOf(subText)
                } else {
                    editBox.cursor
                }
                val j = if(subText != "") {
                    i + subText.length
                } else {
                    editBox.cursor
                }
                editBox.text = StringBuilder(editBox.text).replace(i, j, s).toString()
                editBox.cursor = i + moveCursor
                if (MinecraftClient.getInstance().navigationType.isMouse)
                    PopupWidget.focusElement(editBox)
            }
            val popup = PopupWidget.Builder("fc.validated_field.expression".translate())
                .addElement("ln",    CustomButtonWidget.builder("ln".lit())    { add("ln()", 3) }    .size(56, 20).tooltip(Tooltip.of("fc.validated_field.expression.ln.tip".translate())).build(),             Position.BELOW, Position.ALIGN_LEFT)
                .addElement("min",   CustomButtonWidget.builder("min".lit())   { add("min(, )", 4) } .size(56, 20).tooltip(Tooltip.of("fc.validated_field.expression.min.tip".translate())).build(),   "ln",    Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
                .addElement("max",   CustomButtonWidget.builder("max".lit())   { add("max(, )", 4) } .size(56, 20).tooltip(Tooltip.of("fc.validated_field.expression.max.tip".translate())).build(),   "min",   Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
                .addElement("log",   CustomButtonWidget.builder("log".lit())   { add("log(, )", 4) } .size(56, 20).tooltip(Tooltip.of("fc.validated_field.expression.log.tip".translate())).build(),   "ln",    Position.BELOW, Position.ALIGN_LEFT)
                .addElement("log10", CustomButtonWidget.builder("log10".lit()) { add("log10()", 6) } .size(56, 20).tooltip(Tooltip.of("fc.validated_field.expression.log10.tip".translate())).build(), "log",   Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
                .addElement("log2",  CustomButtonWidget.builder("log2".lit())  { add("log2()", 5) }  .size(56, 20).tooltip(Tooltip.of("fc.validated_field.expression.log2.tip".translate())).build(),  "log10", Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
                .addElement("sqrt",  CustomButtonWidget.builder("sqrt".lit())  { add("sqrt()", 5) }  .size(32, 20).tooltip(Tooltip.of("fc.validated_field.expression.sqrt.tip".translate())).build(),  "log",   Position.BELOW, Position.ALIGN_LEFT)
                .addElement("abs",   CustomButtonWidget.builder("abs".lit())   { add("abs()", 4) }   .size(32, 20).tooltip(Tooltip.of("fc.validated_field.expression.abs.tip".translate())).build(),   "sqrt",  Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
                .addElement("sin",   CustomButtonWidget.builder("sin".lit())   { add("sin()", 4) }   .size(32, 20).tooltip(Tooltip.of("fc.validated_field.expression.sin.tip".translate())).build(),   "abs",   Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
                .addElement("cos",   CustomButtonWidget.builder("cos".lit())   { add("cos()", 4) }   .size(32, 20).tooltip(Tooltip.of("fc.validated_field.expression.cos.tip".translate())).build(),   "sin",   Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
                .addElement("pow",   CustomButtonWidget.builder("^".lit())     { add(" ^ ", 3) }     .size(32, 20).tooltip(Tooltip.of("fc.validated_field.expression.pow.tip".translate())).build(),   "cos",   Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
                .addElement("paren", CustomButtonWidget.builder("(_)".lit())   { add("()", 1) }      .size(32, 20).tooltip(Tooltip.of("fc.validated_field.expression.paren.tip".translate())).build(), "sqrt",  Position.BELOW, Position.ALIGN_LEFT)
                .addElement("incr",  CustomButtonWidget.builder("incr".lit())  { add("incr(, )", 5) }.size(32, 20).tooltip(Tooltip.of("fc.validated_field.expression.incr.tip".translate())).build(),  "paren", Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
                .addElement("ciel",  CustomButtonWidget.builder("ciel".lit())  { add("ciel()", 5) }  .size(32, 20).tooltip(Tooltip.of("fc.validated_field.expression.ciel.tip".translate())).build(),  "incr",  Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
                .addElement("flr",   CustomButtonWidget.builder("flr".lit())   { add("floor()", 6) } .size(32, 20).tooltip(Tooltip.of("fc.validated_field.expression.flr.tip".translate())).build(),   "ciel",  Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
                .addElement("rnd",   CustomButtonWidget.builder("rnd".lit())   { add("round()", 6) } .size(32, 20).tooltip(Tooltip.of("fc.validated_field.expression.rnd.tip".translate())).build(),   "flr",   Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
                .addElement("plus",  CustomButtonWidget.builder("+".lit())     { add(" + ", 3) }     .size(32, 20).tooltip(Tooltip.of("fc.validated_field.expression.plus.tip".translate())).build(),  "paren", Position.BELOW, Position.ALIGN_LEFT)
                .addElement("minus", CustomButtonWidget.builder("-".lit())     { add(" - ", 3) }     .size(32, 20).tooltip(Tooltip.of("fc.validated_field.expression.minus.tip".translate())).build(), "plus",  Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
                .addElement("times", CustomButtonWidget.builder("*".lit())     { add(" * ", 3) }     .size(32, 20).tooltip(Tooltip.of("fc.validated_field.expression.times.tip".translate())).build(), "minus", Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
                .addElement("div",   CustomButtonWidget.builder("/".lit())     { add(" / ", 3) }     .size(32, 20).tooltip(Tooltip.of("fc.validated_field.expression.div.tip".translate())).build(),   "times", Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
                .addElement("mod",   CustomButtonWidget.builder("%".lit())     { add(" % ", 3) }     .size(32, 20).tooltip(Tooltip.of("fc.validated_field.expression.mod.tip".translate())).build(),   "div",   Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)

            val charButtonSize = entry.validVars.size
            if(charButtonSize > 0) {
                if (charButtonSize == 1) {
                    val chr = entry.validVars.toList()[0]
                    popup.addElement("var", CustomButtonWidget.builder(chr.toString().lit()){ add(chr.toString(), 0) }.size(176, 20).build(), "plus", Position.BELOW, Position.ALIGN_LEFT)
                } else {
                    val list = entry.validVars.toList()
                    val chr = list[0]
                    val buttonWidth = (176 - ((charButtonSize - 1) * 4)) / charButtonSize
                    popup.addElement("var", CustomButtonWidget.builder(chr.toString().lit()){ add(chr.toString(), 0) }.size(buttonWidth, 20).build(), "plus", Position.BELOW, Position.ALIGN_LEFT)
                    for(i in 1 until charButtonSize) {
                        val chri = list[i]
                        popup.addElement("var$i", CustomButtonWidget.builder(chri.toString().lit()){ add(chri.toString(), 0) }.size(buttonWidth, 20).build(), Position.RIGHT, Position.HORIZONTAL_TO_TOP_EDGE)
                    }
                }
            }
            if(charButtonSize > 0) {
                popup.addElement("edit_box", editBox, "var", Position.BELOW, Position.ALIGN_LEFT)
            } else {
                popup.addElement("edit_box", editBox, "plus", Position.BELOW, Position.ALIGN_LEFT)
            }
            popup.addDoneWidget()
            popup.noCloseOnClick()
            PopupWidget.push(popup.build())
        }

    }
}