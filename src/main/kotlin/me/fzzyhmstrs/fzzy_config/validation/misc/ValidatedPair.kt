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
import me.fzzyhmstrs.fzzy_config.entry.EntryCreator
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.screen.entry.ConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.widget.LabelWrappedWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutClickableWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.attachTo
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedPair.LayoutStyle
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedPair.Tuple
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlNull
import net.peanuuutz.tomlkt.TomlTableBuilder
import net.peanuuutz.tomlkt.asTomlTable
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.UnaryOperator

/**
 * A validated pair of values
 * @param A stored type of the left side of the tuple
 * @param B stored type of the right side of the tuple
 * @param defaultValue [Tuple] default pair of values
 * @param leftHandler [Entry]&lt;[A]&gt; handler for left side of the tuple
 * @param rightHandler [Entry]&lt;[B]&gt; handler for right side of the tuple
 * @param layoutStyle [LayoutStyle], optional. Whether the two handlers' widgets are laid-out side by side with half the space for each, or stacked like two "normal" settings on top of each other. Default is side-by-side.
 * @see me.fzzyhmstrs.fzzy_config.validation.ValidatedField.pairWith
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.pairs
 * @author fzzyhmstrs
 * since 0.6.0
 */
open class ValidatedPair<A, B> @JvmOverloads constructor(defaultValue: Tuple<A, B>, private val leftHandler: Entry<A, *>, private val rightHandler: Entry<B, *>, private val layoutStyle: LayoutStyle = LayoutStyle.SIDE_BY_SIDE): ValidatedField<Tuple<A, B>>(defaultValue) {

    @Internal
    companion object {

        /**
         * Convenience method for creating a pair of one type
         * @param A stored type of the both sides of the tuple
         * @param handler [Entry]&lt;[A]&gt; handler for both sides of the tuple. This method will instance this handler, so it can't be used for direct inspection of pair internals
         * @param defaultValue [Tuple], optional. default value pair. If not defined will use the current value of the handler for both sides of a tuple. Values are deep-copied.
         * @param layoutStyle [LayoutStyle], optional. Whether the two handlers' widgets are laid-out side by side with half the space for each, or stacked like two "normal" settings on top of each other. Default is side-by-side.
         * @return [ValidatedPair]&lt;[A], [A]&gt;
         * @author fzzyhmstrs
         * since 0.6.0
         */
        @JvmStatic
        @JvmOverloads
        fun <A, E: Entry<A, E>> of(handler: Entry<A, E>, defaultValue: Tuple<A, A> = Tuple(handler.copyValue(handler.get()), handler.copyValue(handler.get())), layoutStyle: LayoutStyle = LayoutStyle.SIDE_BY_SIDE): ValidatedPair<A, A> {
            return ValidatedPair(defaultValue, handler.instanceEntry(), handler.instanceEntry(), layoutStyle)
        }

        /**
         * Attached text labels to the widgets of this pair. Labels will appear below the respective widget
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        fun <F: ValidatedPair<*, *>> F.withLabels(leftLabel: Text, rightLabel: Text): F {
            this.leftLabel = leftLabel
            this.rightLabel = rightLabel
            return this
        }
    }

    private val lock: AtomicBoolean = AtomicBoolean(false)

    init {
        leftHandler.listenToEntry { e ->
            if (!lock.get()) {
                onLeftChanged(e)
                accept(storedValue.withLeft(e.get()))
            }
        }
        rightHandler.listenToEntry { e ->
            if (!lock.get()) {
                onRightChanged(e)
                accept(storedValue.withRight(e.get()))
            }
        }
    }

    /**
     * Called when the left side value is modified in-GUI or with set methods
     * @param left [Entry]&lt;[A], *&gt; the left side handler, which is an Entry for the left type
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    open fun onLeftChanged(left: Entry<A, *>) {}

    /**
     * Called when the right side value is modified in-GUI or with set methods
     * @param right [Entry]&lt;[B], *&gt; the right side handler, which is an Entry for the right type
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    open fun onRightChanged(right: Entry<B, *>) {}

    private var leftLabel: Text? = null
    private var rightLabel: Text? = null

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Tuple<A, B>> {
        return try {
            val table = toml.asTomlTable()
            val errors = ValidationResult.createMutable("Error(s) found deserializing pair $fieldName")
            val aToml = table["left"] ?: TomlNull
            val bToml = table["right"] ?: TomlNull
            val aResult = leftHandler.deserializeEntry(aToml, "$fieldName.left", 1).attachTo(errors)
            val bResult = rightHandler.deserializeEntry(bToml, "$fieldName.right", 1).attachTo(errors)
            ValidationResult.ofMutable(Tuple(aResult.get(), bResult.get()), errors)
        } catch (e: Throwable) {
            ValidationResult.error(storedValue, ValidationResult.Errors.DESERIALIZATION, "Exception deserializing pair [$fieldName]", e)
        }
    }

    @Internal
    override fun serialize(input: Tuple<A, B>): ValidationResult<TomlElement> {
        val builder = TomlTableBuilder()
        val errors = ValidationResult.createMutable("Error(s) found deserializing pair")
        val aElement = leftHandler.serializeEntry(input.left, 1).attachTo(errors)
        val bElement = rightHandler.serializeEntry(input.right, 1).attachTo(errors)
        builder.element("left", aElement.get())
        builder.element("right", bElement.get())
        return ValidationResult.ofMutable(builder.build(), errors)
    }

    @Internal
    override fun correctEntry(input: Tuple<A, B>, type: EntryValidator.ValidationType): ValidationResult<Tuple<A, B>> {
        val errors = ValidationResult.createMutable("Pair correction found errors")
        val aResult = leftHandler.correctEntry(input.left, type).attachTo(errors)
        val bResult = rightHandler.correctEntry(input.right, type).attachTo(errors)
        return ValidationResult.ofMutable(Tuple(aResult.get(), bResult.get()), errors)
    }

    @Internal
    override fun validateEntry(input: Tuple<A, B>, type: EntryValidator.ValidationType): ValidationResult<Tuple<A, B>> {
        val errors = ValidationResult.createMutable("Pair validation found errors")
        leftHandler.validateEntry(input.left, type).attachTo(errors)
        rightHandler.validateEntry(input.right, type).attachTo(errors)
        return ValidationResult.ofMutable(input, errors)
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<Tuple<A, B>>): ClickableWidget {
        val left = leftHandler.widgetEntry().wrap(leftLabel, "left")
        val right = rightHandler.widgetEntry().wrap(rightLabel, "right")
        if (layoutStyle == LayoutStyle.SIDE_BY_SIDE) {
            left.width = 53
            right.width = 53
        }
        val layout = LayoutWidget.Builder().paddingBoth(0).spacingBoth(0).build()
        layout.add(
            "left",
            left,
            LayoutWidget.Position.ALIGN_LEFT)
        if (layoutStyle == LayoutStyle.SIDE_BY_SIDE)
            layout.add(
                "right",
                right,
                LayoutWidget.Position.RIGHT,
                LayoutWidget.Position.ALIGN_LEFT_AND_JUSTIFY,
                LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
        else
            layout.add(
                "right",
                right,
                LayoutWidget.Position.ALIGN_LEFT,
                LayoutWidget.Position.BELOW)
        return LayoutClickableWidget(0, 0, 110, 40, layout)
    }

    /**
     * creates a deep copy of this ValidatedPair
     * @return ValidatedPair wrapping each half also copied by their respective [copyValue] methods
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun instanceEntry(): ValidatedPair<A, B> {
        return ValidatedPair(Tuple(leftHandler.copyValue(storedValue.left), rightHandler.copyValue(storedValue.right)), leftHandler, rightHandler, layoutStyle)
    }

    @Internal
    @Suppress("UNCHECKED_CAST")
    override fun isValidEntry(input: Any?): Boolean {
        if (input !is Tuple<*, *>) return false
        return try {
            validateEntry(input as Tuple<A, B>, EntryValidator.ValidationType.STRONG).isValid()
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Copies the provided input as deeply as possible. For immutables like numbers and booleans, this will simply return the input
     * @param input [Tuple]&lt;[A], [B]%gt; input to be copied
     * @return copied output
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun copyValue(input: Tuple<A, B>): Tuple<A, B> {
        return Tuple(leftHandler.copyValue(input.left), rightHandler.copyValue(input.right))
    }

    @Internal
    override fun set(input: Tuple<A, B>) {
        super.set(input)
        lock.set(true)
        leftHandler.accept(input.left)
        rightHandler.accept(input.right)
        lock.set(false)
    }

    @Internal
    override fun contentBuilder(context: EntryCreator.CreatorContext): UnaryOperator<ConfigEntry.ContentBuilder> {
        return UnaryOperator { contentBuilder ->
            contentBuilder.layoutContent { contentLayout ->
                val w = if (layoutStyle == LayoutStyle.SIDE_BY_SIDE)
                    (contentLayout.width - contentLayout.getGeneralHorizontalSpacing()) / 2
                else
                    contentLayout.width
                val left = leftHandler.widgetEntry().wrap(leftLabel, "left")
                val right = rightHandler.widgetEntry().wrap(rightLabel, "right")
                left.width = w
                right.width = w
                contentLayout.add(
                    "left",
                    left,
                    LayoutWidget.Position.ALIGN_LEFT)
                if (layoutStyle == LayoutStyle.SIDE_BY_SIDE)
                    contentLayout.add(
                        "right",
                        right,
                        LayoutWidget.Position.RIGHT,
                        LayoutWidget.Position.POSITION_RIGHT_OF_AND_JUSTIFY,
                        LayoutWidget.Position.HORIZONTAL_TO_TOP_EDGE)
                else
                    contentLayout.add(
                        "right",
                        right,
                        LayoutWidget.Position.ALIGN_LEFT,
                        LayoutWidget.Position.BELOW)
            }
            val deco = entryDeco()
            if (deco != null)
                contentBuilder.decoration(deco.decorated, deco.offsetX, deco.offsetY)
            contentBuilder
        }
    }

    private fun ClickableWidget.wrap(label: Text?, side: String): ClickableWidget {
        return if (label == null) LabelWrappedWidget(this, "fc.validated_field.pair.$side".translate(), false) else LabelWrappedWidget(this, label)
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated Pair[value=$storedValue, leftHandler=$leftHandler, rightHandler=$rightHandler]"
    }

    /**
     * Determines how the child widgets will be laid out in the overall pair widget
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    enum class LayoutStyle {
        /**
         * The two widgets will display side by side, "squashed" to half the width of one normal setting widget (and with padding between them)
         *
         * Labels will appear below each widget, also side by side and squashed.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        SIDE_BY_SIDE,
        /**
         * The two widgets will be stacked one on top of the other just like two settings in the normal setting list, but with only one setting title. Like a mini "group" of settings
         *
         * Labels will appear below each widget, so the total widget would be Widget 1 > Label 1 > Widget 2 > Label 2 stacked on top of each other
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        STACKED
    }

    /**
     * Simple pair data class for storing and interacting with arbitrary pairs of types.
     * @param X left element type
     * @param Y right element type
     * @param left [X] instance
     * @param right [Y] instance
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    data class Tuple<X, Y>(val left: X, val right: Y) {

        private constructor(left: X, right: Y, side: Boolean): this(left, right) {
            this.side = side
        }

        private var side: Boolean? = null

        /**
         * Constructs a new [Tuple] with an updated left side only, using the existing right side
         * @param newLeft [X] the new left element
         * @return new [Tuple] instance with new left element and existing right element
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun withLeft(newLeft: X): Tuple<X, Y> {
            return Tuple(newLeft, right, false)
        }

        /**
         * Constructs a new [Tuple] with an updated right side only, using the existing left side
         * @param newRight [Y] the new right element
         * @return new [Tuple] instance with existing right element and new right element
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun withRight(newRight: Y): Tuple<X, Y> {
            return Tuple(left, newRight, true)
        }

        /**
         * (Currently unused) which side of this tuple was the most recently updated
         * @return Boolean, nullable. The last side that received an update. This is a TriState, effectively. false = left, true = right, null = both sides updated at the same time
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun lastSide(): Boolean? {
            return side
        }
    }
}