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

import me.fzzyhmstrs.fzzy_config.entry.Entry
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.screen.entry.ConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedPair.Tuple
import net.minecraft.client.gui.widget.ClickableWidget
import net.peanuuutz.tomlkt.*
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.UnaryOperator

/**
 * A validated pair of values
 * @param A stored type of the left side of the tuple
 * @param B stored type of the right side of the tuple
 * @param defaultValue [Tuple] default pair of values
 * @param leftHandler [Entry]&lt;[A]&gt; handler for left side of the tuple
 * @param rightHandler [Entry]&lt;[B]&gt; handler for right side of the tuple
 * @see me.fzzyhmstrs.fzzy_config.validation.ValidatedField.pairWith
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.pairs
 * @author fzzyhmstrs
 * since 0.6.0
 */
open class ValidatedPair<A, B>(defaultValue: Tuple<A, B>, private val leftHandler: Entry<A, *>, private val rightHandler: Entry<B, *>): ValidatedField<ValidatedPair.Tuple<A, B>>(defaultValue) {

    @Internal
    companion object {

        /**
         * Convenience method for creating a pair of one type
         * @param A stored type of the both sides of the tuple
         * @param defaultValue [Tuple] default pair of values
         * @param handler [Entry]&lt;[A]&gt; handler for both sides of the tuple
         * @return [ValidatedPair]&lt;[A], [A]&gt;
         * @author fzzyhmstrs
         * since 0.6.0
         */
        fun <A> of(defaultValue: Tuple<A, A>, handler: Entry<A, *>): ValidatedPair<A, A> {
            return ValidatedPair(defaultValue, handler, handler)
        }
    }

    init {
        leftHandler.listenToEntry { e ->
            accept(storedValue.withLeft(e.get()))
        }
        rightHandler.listenToEntry { e ->
            accept(storedValue.withRight(e.get()))
        }
    }

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Tuple<A, B>> {
        return try {
            val table = toml.asTomlTable()
            val errors: MutableList<String> = mutableListOf()
            val aToml = table["left"] ?: TomlNull
            val bToml = table["right"] ?: TomlNull
            val aResult = leftHandler.deserializeEntry(aToml, errors, "$fieldName.left", 1)
            val bResult = rightHandler.deserializeEntry(bToml, errors, "$fieldName.right", 1)
            ValidationResult.predicated(Tuple(aResult.get(), bResult.get()), errors.isEmpty(), "Errors encountered while deserializing pair [$fieldName]: $errors")
        } catch (e: Throwable) {
            ValidationResult.error(storedValue, "Critical error deserializing pair [$fieldName]: ${e.localizedMessage}")
        }
    }

    @Internal
    override fun serialize(input: Tuple<A, B>): ValidationResult<TomlElement> {
        val builder = TomlTableBuilder()
        val errors: MutableList<String> = mutableListOf()
        val aElement = leftHandler.serializeEntry(input.left, errors, 1)
        val bElement = rightHandler.serializeEntry(input.right, errors, 1)
        builder.element("left", aElement)
        builder.element("right", bElement)
        return ValidationResult.predicated(builder.build(), errors.isEmpty(), "Errors encountered serializing pair: $errors")
    }

    @Internal
    override fun correctEntry(input: Tuple<A, B>, type: EntryValidator.ValidationType): ValidationResult<Tuple<A, B>> {
        val errors: MutableList<String> = mutableListOf()
        val aResult = leftHandler.correctEntry(input.left, type).report(errors)
        val bResult = rightHandler.correctEntry(input.right, type).report(errors)
        return ValidationResult.predicated(Tuple(aResult.get(), bResult.get()), errors.isEmpty(), "Errors corrected in pair: $errors")
    }

    @Internal
    override fun validateEntry(input: Tuple<A, B>, type: EntryValidator.ValidationType): ValidationResult<Tuple<A, B>> {
        val errors: MutableList<String> = mutableListOf()
        leftHandler.validateEntry(input.left, type).report(errors)
        rightHandler.validateEntry(input.right, type).report(errors)
        return ValidationResult.predicated(input, errors.isEmpty(), "Errors found in pair: $errors")
    }

    override fun copyValue(input: Tuple<A, B>): Tuple<A, B> {
        return Tuple(leftHandler.copyValue(input.left), rightHandler.copyValue(input.right))
    }

    override fun copyStoredValue(): Tuple<A, B> {
        return Tuple(leftHandler.copyValue(storedValue.left), rightHandler.copyValue(storedValue.right))
    }

    /**
     * creates a deep copy of this ValidatedBoolean
     * return ValidatedBoolean wrapping the current boolean value
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedPair<A, B> {
        return ValidatedPair(Tuple(leftHandler.copyValue(storedValue.left), rightHandler.copyValue(storedValue.right)), leftHandler, rightHandler)
    }

    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        if (input !is Tuple<*, *>) return false
        return try {
            validateEntry(input as Tuple<A, B>, EntryValidator.ValidationType.STRONG).isValid()
        } catch (e: Throwable) {
            false
        }
    }

    @Internal
    @Deprecated("Not used by this widget")
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<Tuple<A, B>>): ClickableWidget {
        val left = leftHandler.widgetEntry()
        val right = rightHandler.widgetEntry()
        left.width = 53
        right.width = 53
        val layout = LayoutWidget(paddingW = 0, spacingW = 0)
        layout.add(
            "left",
            left,
            LayoutWidget.Position.ALIGN_LEFT)
        layout.add(
            "right",
            right,
            LayoutWidget.Position.ALIGN_LEFT_AND_JUSTIFY,
            LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
        return LayoutClickableWidget(0, 0, 110, 20, layout)
    }

    override fun contentBuilder(): UnaryOperator<ConfigEntry.ContentBuilder> {
        return UnaryOperator { contentBuilder ->
            contentBuilder.layoutContent { contentLayout ->
                val w = (contentLayout.width - contentLayout.getGeneralHorizontalSpacing()) / 2
                val left = leftHandler.widgetEntry()
                val right = rightHandler.widgetEntry()
                left.width = w
                right.width = w
                contentLayout.add(
                    "left",
                    left,
                    LayoutWidget.Position.ALIGN_LEFT)
                contentLayout.add(
                    "right",
                    right,
                    LayoutWidget.Position.ALIGN_LEFT_AND_JUSTIFY,
                    LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
            }
        }
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated Pair[value=$storedValue, leftHandler=$leftHandler, rightHandler=$rightHandler]"
    }

    data class Tuple<X, Y>(val left: X, val right: Y) {

        private contructor(left: X, right: Y, side: Boolean): this(left, right) {
            this.side = side
        }
        
        private var side: Boolean? = null
        
        fun withLeft(newLeft: X): Tuple<X, Y> {
            return Tuple(newLeft, right)
        }

        fun withRight(newRight: Y): Tuple<X, Y> {
            return Tuple(left, newRight)
        }

        fun lastSide(): Boolean? {
            return side
        }
    }
}
