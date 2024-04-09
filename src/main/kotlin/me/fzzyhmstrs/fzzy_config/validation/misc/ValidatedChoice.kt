package me.fzzyhmstrs.fzzy_config.validation.misc

import me.fzzyhmstrs.fzzy_config.entry.EntryHandler
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.descLit
import me.fzzyhmstrs.fzzy_config.util.FcText.lit
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.also
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedChoice.WidgetType
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.PressableWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.math.max

//@sample me.fzzyhmstrs.fzzy_config.examples.ExampleTranslations.lang
/**
 * A validated set of choices of any type
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
 * @since 0.2.0
 */
class ValidatedChoice<T> @JvmOverloads constructor(defaultValue: T, private val choices: List<T>, private val handler: EntryHandler<T>, private val widgetType: WidgetType = WidgetType.POPUP): ValidatedField<T>(defaultValue) {

    /**
     * A validated set of choices of any typem using the first choice as the default
     *
     * Similar to a [ValidatedEnum], but constructed from a pre-defined list of choices
     * @param T the choice type
     * @param choices [List]<T> defining the appropriate choices
     * @param handler [EntryHandler] to provide validation tasks for individual choice elements
     * @param widgetType [WidgetType] defines the GUI selection type. Defaults to POPUP
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    @JvmOverloads
    constructor(choices: List<T>, handler: EntryHandler<T>, widgetType: WidgetType = WidgetType.POPUP): this(choices[0], choices, handler, widgetType)

    init{
        if (!choices.contains(defaultValue))
            throw IllegalStateException("Default value [$defaultValue] of ValidatedChoices not within valid choice lists [$choices]")
        if(choices.isEmpty())
            throw IllegalStateException("ValidatedChoice can't have empty choice list")
    }
    override fun copyStoredValue(): T {
        return storedValue
    }
    @Internal
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
    @Internal
    override fun serialize(input: T): ValidationResult<TomlElement> {
        return ValidationResult.success(handler.serializeEntry(input, mutableListOf(), true))
    }
    @Internal
    override fun validateEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        return handler.validateEntry(input,type).also(choices.contains(input),"[$input] not a valid choice: [$choices]")
    }
    @Internal
    @Environment(EnvType.CLIENT)
    override fun widgetEntry(choicePredicate: ChoiceValidator<T>): ClickableWidget {
        return when(widgetType){
            WidgetType.POPUP -> {
                ChoicePopupButtonWidget(translation(),choicePredicate,this)
            }
            WidgetType.CYCLING -> {
                CyclingOptionsWidget(choicePredicate,this)
            }
        }
    }

    /**
     * creates a deep copy of this ValidatedChoice (as deep as possible)
     * return ValidatedChoice wrapping a copy of the currently stored choice, allowable choices, and handler
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedChoice<T> {
        return ValidatedChoice(copyStoredValue(),this.choices,this.handler)
    }
    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        if (input == null) return false
        return try {
            input::class.java == defaultValue!!::class.java && validateEntry(input as T, EntryValidator.ValidationType.STRONG).isValid()
        } catch (e: Exception){
            false
        }
    }

    /**
     * @suppress
     */
     override fun toString() {
         return "Validated Choice[value=$storedValue, choices=$choices]"
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

    @Environment(EnvType.CLIENT)
    private class ChoicePopupButtonWidget<T>(private val name: Text, choicePredicate: ChoiceValidator<T>, private val entry: ValidatedChoice<T>): PressableWidget(0,0,110,20, FcText.empty()) {

        private val choices = entry.choices.filter {
            choicePredicate.validateEntry(it,EntryValidator.ValidationType.STRONG).isValid()
        }

        override fun getMessage(): Text {
            return entry.get().let { it.transLit(it.toString()) }
        }

        override fun getNarrationMessage(): MutableText {
            return this.message.copy()
        }

        override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
            builder.put(NarrationPart.TITLE, this.narrationMessage)
            //builder.put(NarrationPart.USAGE, FcText.translatable("narration.component_list.usage"))
        }

        override fun onPress() {
            val builder = PopupWidget.Builder(name, spacingH = 0)
            val textRenderer = MinecraftClient.getInstance().textRenderer
            var buttonWidth = 86
            for (const in choices) {
                buttonWidth = max(buttonWidth, textRenderer.getWidth(const.let { it.transLit(it.toString()) }))
            }
            buttonWidth = max(150, buttonWidth + 4)
            var prevParent = "title"
            for ((index,const) in choices.withIndex()){
                val button = ChoiceOptionWidget(
                    const,
                    buttonWidth,
                    { c: T -> c != entry.get() },
                    { entry.accept(it); PopupWidget.pop() })
                builder.addElement("choice$index",button, prevParent, PopupWidget.Builder.PositionRelativePos.BELOW)
                prevParent = "choice$index"
            }
            builder.positionX(PopupWidget.Builder.popupContext { w -> this.x + this.width/2 - w/2 })
            builder.positionY(PopupWidget.Builder.popupContext { this.y - 20 })
            builder.additionalNarration("fc.validated_field.enum.current".translate(entry.get().transLit(entry.get().toString())))
            PopupWidget.push(builder.build())
        }
    }

    @Environment(EnvType.CLIENT)
    private class ChoiceOptionWidget<T>(private val thisVal: T, width: Int, private val activePredicate: Predicate<T>, private val valueApplier: Consumer<T>): PressableWidget(0,0,width,20, thisVal.transLit(thisVal.toString())) {

        init {
            thisVal.descLit("").takeIf { it.string != "" }?.also { tooltip = Tooltip.of(it) }
        }

        override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            this.active = activePredicate.test(thisVal)
            super.renderWidget(context, mouseX, mouseY, delta)
        }

        override fun getNarrationMessage(): MutableText {
            return thisVal.transLit(thisVal.toString())
        }

        override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
            appendDefaultNarrations(builder)
        }

        override fun onPress() {
            valueApplier.accept(thisVal)
        }
    }

    @Environment(EnvType.CLIENT)
    private class CyclingOptionsWidget<T>(choicePredicate: ChoiceValidator<T>, private val entry: ValidatedChoice<T>): PressableWidget(0,0,110,20, entry.get().let { it.transLit(it.toString()) }) {

        private val choices = entry.choices.filter {
            choicePredicate.validateEntry(it,EntryValidator.ValidationType.STRONG).isValid()
        }

        init {
            constructTooltip()
        }

        private fun constructTooltip() {
            val text1 = entry.descLit("").takeIf { it.string != "" }?.copy()
            val text2 = entry.get().descLit("").takeIf { it.string != "" }
            val totalText = if(text1 != null){
                if (text2 != null){
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
            return entry.get().let { it.transLit(it.toString()) }
        }

        override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
            appendDefaultNarrations(builder)
        }

        override fun onPress() {
            val newIndex = (choices.indexOf(entry.get()) + 1).takeIf { it < choices.size } ?: 0
            val newConst = choices[newIndex]
            message = newConst.let { it.transLit(it.toString()) }
            constructTooltip()
            entry.accept(newConst)
        }

    }
}
