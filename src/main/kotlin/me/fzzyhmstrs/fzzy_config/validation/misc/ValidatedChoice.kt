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

import me.fzzyhmstrs.fzzy_config.entry.EntryHandler
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.descLit
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.also
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedChoiceList
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedChoice.WidgetType
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlElement
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.math.max

/**
 * A validated single choice of any type, from an input list of possible choices
 *
 * Similar to a [ValidatedEnum], but constructed from a pre-defined list of choices
 *
 * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Choices) for more details and examples.
 * @param T the choice type
 * @param defaultValue the default choice
 * @param choices [List]<T> defining the appropriate choices
 * @param handler [EntryHandler] to provide validation tasks for individual choice elements
 * @param translationProvider BiFunction [T], String, [Text] - converts a choice instance [T] and the base translation key of this ValidatedChoice into a text Translation
 * @param descriptionProvider BiFunction [T], String, [Text] - converts a choice instance [T] and the base description key of this ValidatedChoice into a text Description
 * @param widgetType [WidgetType] defines the GUI selection type. Defaults to POPUP
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.choices
 * @see me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList.toChoices
 * @author fzzyhmstrs
 * @since 0.2.0, added providers 0.3.6
 */
open class ValidatedChoice<T> @JvmOverloads constructor(
    defaultValue: T,
    private val choices: List<T>,
    private val handler: EntryHandler<T>,
    private val translationProvider: BiFunction<T, String, MutableText> = BiFunction { t, _ -> t.transLit(t.toString()) },
    private val descriptionProvider: BiFunction<T, String, Text> = BiFunction { t, _ -> t.descLit("") },
    private val widgetType: WidgetType = WidgetType.POPUP): ValidatedField<T>(defaultValue)
{

    /**
     * A validated single choice of any type, from an input list of possible choices
     *
     * Similar to a [ValidatedEnum], but constructed from a pre-defined list of choices
     * @param T the choice type
     * @param defaultValue the default choice
     * @param choices [List]<T> defining the appropriate choices
     * @param handler [EntryHandler] to provide validation tasks for individual choice elements
     * @param widgetType [WidgetType] defines the GUI selection type. Defaults to POPUP
     * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.choices
     * @see me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList.toChoices
     * @author fzzyhmstrs
     * @since 0.3.6
     */
    constructor(defaultValue: T, choices: List<T>, handler: EntryHandler<T>, widgetType: WidgetType): this(defaultValue, choices, handler, BiFunction { t, _ -> t.transLit(t.toString()) }, BiFunction { t, _ -> t.descLit("") }, widgetType)

    /**
     * A validated set of choices of any type using the first choice as the default
     *
     * Similar to a [ValidatedEnum], but constructed from a pre-defined list of choices
     * @param T the choice type
     * @param choices [List]<T> defining the appropriate choices; the first choice in the list will be the default
     * @param handler [EntryHandler] to provide validation tasks for individual choice elements
     * @param translationProvider BiFunction [T], String, [Text] - converts a choice instance [T] and the base translation key of this ValidatedChoice into a text Translation
     * @param descriptionProvider BiFunction [T], String, [Text] - converts a choice instance [T] and the base description key of this ValidatedChoice into a text Description
     * @param widgetType [WidgetType] defines the GUI selection type. Defaults to POPUP
     * @author fzzyhmstrs
     * @since 0.2.0, added providers 0.3.6
     */
    @JvmOverloads
    constructor(choices: List<T>, handler: EntryHandler<T>, translationProvider: BiFunction<T, String, MutableText> = BiFunction { t, _ -> t.transLit(t.toString()) }, descriptionProvider: BiFunction<T, String, Text> = BiFunction { t, _ -> t.descLit(t.toString()) }, widgetType: WidgetType = WidgetType.POPUP): this(choices[0], choices, handler, translationProvider, descriptionProvider, widgetType)

    /**
     * A validated set of choices of any type using the first choice as the default
     *
     * Similar to a [ValidatedEnum], but constructed from a pre-defined list of choices
     * @param T the choice type
     * @param choices [List]<T> defining the appropriate choices; the first choice in the list will be the default
     * @param handler [EntryHandler] to provide validation tasks for individual choice elements
     * @param widgetType [WidgetType] defines the GUI selection type. Defaults to POPUP
     * @author fzzyhmstrs
     * @since 0.3.6
     */
    constructor(choices: List<T>, handler: EntryHandler<T>, widgetType: WidgetType): this(choices[0], choices, handler, widgetType = widgetType)


    /**
     * A validated set of choices of any type using the first choice as the default
     *
     * Similar to a [ValidatedEnum], but constructed from a pre-defined list of choices
     * @param T the choice type
     * @param handler [EntryHandler] to provide validation tasks for individual choice elements
     * @param choice vararg [T] defining the appropriate choices; the first provided choice will be the default
     * @author fzzyhmstrs
     * @since 0.3.6
     */
    constructor(handler: EntryHandler<T>, vararg choice: T): this(choice.toList(), handler)

    init {
        if (!choices.contains(defaultValue))
            throw IllegalStateException("Default value [$defaultValue] of ValidatedChoices not within valid choice lists [$choices]")
        if(choices.isEmpty())
            throw IllegalStateException("ValidatedChoice can't have empty choice list")
    }

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<T> {
        return try {
            val errors = mutableListOf<String>()
            val value =  handler.deserializeEntry(toml, errors, fieldName, 1).report(errors)
            if (errors.isNotEmpty()) {
                ValidationResult.error(value.get(), "Error(s) encountered while deserializing choice: $errors")
            } else {
                value
            }
        } catch (e: Throwable) {
            ValidationResult.error(storedValue, "Critical error deserializing choices [$fieldName]: ${e.localizedMessage}")
        }
    }

    @Internal
    override fun serialize(input: T): ValidationResult<TomlElement> {
        return ValidationResult.success(handler.serializeEntry(input, mutableListOf(), 1))
    }

    @Internal
    override fun validateEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        return handler.validateEntry(input, type).also(choices.contains(input), "[$input] not a valid choice: [$choices]")
    }

    /**
     * creates a deep copy of this ValidatedChoice (as deep as possible)
     * return ValidatedChoice wrapping a copy of the currently stored choice, allowable choices, and handler
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedChoice<T> {
        return ValidatedChoice(copyStoredValue(), this.choices, this.handler, this.translationProvider, this.descriptionProvider, this.widgetType)
    }

    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        if (input == null) return false
        return try {
            @Suppress("UNCHECKED_CAST")
            input::class.java == defaultValue!!::class.java && validateEntry(input as T, EntryValidator.ValidationType.STRONG).isValid()
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Copies the provided input as deeply as possible. For immutables like numbers and booleans, this will simply return the input
     * @param input [T] input to be copied
     * @return copied output
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun copyValue(input: T): T {
        return handler.copyValue(input)
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<T>): ClickableWidget {
        return when(widgetType) {
            WidgetType.POPUP -> {
                ChoicePopupButtonWidget(translation(), choicePredicate, this)
            }
            WidgetType.CYCLING -> {
                CyclingOptionsWidget(choicePredicate, this)
            }
        }
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated Choice[value=$storedValue, choices=$choices]"
    }

    /**
     * Converts this ValidatedChoice into a [ValidatedChoiceList] wrapping this choice's options, widget type, and translation providers
     * @param selectedChoices List&lt;[T]&gt; - The default selected choices of the resulting choice set. Can be empty.
     * @param widgetType [ValidatedChoiceList.WidgetType] defines the GUI selection type. Defaults to POPUP if this choices widget type is POPUP (also default), INLINE otherwise
     * @return [ValidatedChoiceList] with options based on this choice's options
     * @author fzzyhmstrs
     * @since 0.6.3
     */
    @JvmOverloads
    fun toChoiceList(selectedChoices: List<T> = listOf(),
                     widgetType: ValidatedChoiceList.WidgetType = if (this.widgetType == WidgetType.POPUP)
                        ValidatedChoiceList.WidgetType.POPUP
                    else
                        ValidatedChoiceList.WidgetType.INLINE): ValidatedChoiceList<T>
    {
        @Suppress("DEPRECATION")
        return ValidatedChoiceList(selectedChoices, choices, handler, translationProvider, descriptionProvider, widgetType)
    }

    /**
     * Determines which type of selector widget will be used for the Choice selector, default is POPUP
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
         * A traditional MC cycling button widget, iterating through the choices in order
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        CYCLING
    }

    @Internal
    companion object {

        /**
         * Helper method for creating Choice translations from the base key and the choice value string.
         *
         * For example, if the [ValidatedChoice] has three entries `"a"`, `"b"`, `"c"`, we can use this method to create lang keys like the following
         *
         * - Translation: `"my_mod.my_config.choiceField.a"`
         * - Description: `"my_mod.my_config.choiceField.desc.a"`
         * - With Suffix: `"my_mod.my_config.choiceField.a.suffix"`
         *
         * Which will be used to create a Translatable Text.
         * @param suffix Optional string to add to the end of the created lang key.
         * @return [BiFunction] [T], String, [MutableText] the composed BiFunction for providing to a [ValidatedChoice] constructor.
         * @author fzzyhmstrs
         * @since 0.3.7
         */
        @JvmStatic
        @JvmOverloads
        fun <T> translate(suffix: String = ""): BiFunction<T, String, MutableText> {
            return if(suffix.isEmpty())
                BiFunction { t, u -> FcText.translatable(u + "." + t.toString()) }
            else
                BiFunction { t, u -> FcText.translatable(u + "." + t.toString() + "." + suffix) }
        }

    }

    //client
    private class ChoicePopupButtonWidget<T>(private val name: Text, choicePredicate: ChoiceValidator<T>, private val entry: ValidatedChoice<T>): CustomPressableWidget(0, 0, 110, 20, FcText.empty()) {

        private val choices = entry.choices.filter {
            choicePredicate.validateEntry(it, EntryValidator.ValidationType.STRONG).isValid()
        }

        init {
            constructTooltip()
        }

        override fun getMessage(): Text {
            return entry.translationProvider.apply(entry.get(), entry.translationKey())
        }

        override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
            builder?.put(NarrationPart.TITLE, "fc.validated_field.current".translate(this.message).append(". "))
        }

        private fun constructTooltip() {
            val text1 = entry.descLit("").takeIf { it.string != "" }?.copy()
            val text2 = entry.descriptionProvider.apply(entry.get(), entry.descriptionKey()).takeIf { it.string != "" }
            val totalText = if(text1 != null) {
                if (text2 != null) {
                    text1.append("; ".lit()).append(text2)
                } else {
                    text1
                }
            } else {
                text2 ?: FcText.empty()
            }
            if(totalText.string != "")
                tooltip = Tooltip.of(totalText)
        }

        override fun onPress() {
            val builder = PopupWidget.Builder(name, spacingH = 0)
            val textRenderer = MinecraftClient.getInstance().textRenderer
            var buttonWidth = 86
            for (const in choices) {
                buttonWidth = max(buttonWidth, textRenderer.getWidth(entry.translationProvider.apply(const, entry.translationKey())))
            }
            buttonWidth = max(150, buttonWidth + 4)
            var prevParent = "title"
            for ((index, const) in choices.withIndex()) {
                val button = ChoiceOptionWidget(
                    const,
                    buttonWidth,
                    { c: T -> c != entry.get() },
                    entry,
                    { entry.accept(it); constructTooltip(); PopupWidget.pop() })
                builder.add("choice$index", button, prevParent, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_CENTER)
                prevParent = "choice$index"
            }
            builder.positionX(PopupWidget.Builder.popupContext { w -> this.x + this.width/2 - w/2 })
            builder.positionY(PopupWidget.Builder.popupContext { this.y - 20 })
            builder.additionalNarration("fc.validated_field.current".translate(entry.translationProvider.apply(entry.get(), entry.translationKey())))
            PopupWidget.push(builder.build())
        }
    }

    //client
    private class ChoiceOptionWidget<T>(private val thisVal: T, width: Int, private val activePredicate: Predicate<T>, private val entry: ValidatedChoice<T>, private val valueApplier: Consumer<T>): CustomPressableWidget(0, 0, width, 20, entry.translationProvider.apply(thisVal, entry.translationKey())) {

        init {
            entry.descriptionProvider.apply(thisVal, entry.descriptionKey()).takeIf { it.string != "" }?.also { tooltip = Tooltip.of(it) }
        }

        override fun renderCustom(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
            this.active = activePredicate.test(thisVal)
            super.renderCustom(context, x, y, width, height, mouseX, mouseY, delta)
        }

        override fun getNarrationMessage(): MutableText {
            return entry.translationProvider.apply(thisVal, entry.translationKey())
        }

        override fun onPress() {
            valueApplier.accept(thisVal)
        }
    }

    //client
    private class CyclingOptionsWidget<T>(choicePredicate: ChoiceValidator<T>, private val entry: ValidatedChoice<T>): CustomPressableWidget(0, 0, 110, 20, entry.translationProvider.apply(entry.get(), entry.translationKey())) {

        private val choices = entry.choices.filter {
            choicePredicate.validateEntry(it, EntryValidator.ValidationType.STRONG).isValid()
        }

        init {
            constructTooltip()
        }

        private fun constructTooltip() {
            val text1 = entry.descLit("").takeIf { it.string != "" }?.copy()
            val text2 = entry.descriptionProvider.apply(entry.get(), entry.descriptionKey()).takeIf { it.string != "" }
            val totalText = if(text1 != null) {
                if (text2 != null) {
                    text1.append("; ".lit()).append(text2)
                } else {
                    text1
                }
            } else {
                text2 ?: FcText.empty()
            }
            if(totalText.string != "")
                tooltip = Tooltip.of(totalText)
        }

        override fun getNarrationMessage(): MutableText {
            return "fc.validated_field.current".translate(entry.translationProvider.apply(entry.get(), entry.translationKey()).append(". "))
        }

        override fun onPress() {
            val newIndex = (choices.indexOf(entry.get()) + 1).takeIf { it < choices.size } ?: 0
            val newConst = choices[newIndex]
            message = entry.translationProvider.apply(newConst, entry.translationKey())
            entry.accept(newConst)
            constructTooltip()
        }

    }
}