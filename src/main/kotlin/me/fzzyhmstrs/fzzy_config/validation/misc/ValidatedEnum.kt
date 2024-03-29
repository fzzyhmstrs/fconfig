package me.fzzyhmstrs.fzzy_config.validation.misc

import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.screen.ConfigScreen
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.descLit
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.entry.Entry
import me.fzzyhmstrs.fzzy_config.validation.entry.EntryValidator
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
import net.peanuuutz.tomlkt.TomlLiteral
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.math.max

/**
 * A validated Enum Class
 * @param T the enum type. Any [Enum]
 * @param defaultValue Enum Constant used as the default for this setting
 * @param widgetType [WidgetType] defines the GUI selection type. Defaults to POPUP
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.TestEnum]
 * @sample [me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.validatedEnum]
 * @author fzzyhmstrs
 * @since 0.2.0
 */
class ValidatedEnum<T: Enum<*>> @JvmOverloads constructor(defaultValue: T, private val widgetType: WidgetType = WidgetType.POPUP): ValidatedField<T>(defaultValue) {

    private val valuesMap: Map<String, T> = defaultValue::class.java.enumConstants.associateBy { it.name }
    override fun copyStoredValue(): T {
        return storedValue
    }

    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<T> {
        return try {
            val string = toml.toString()
            val chkEnum = valuesMap[string] ?: return ValidationResult.error(storedValue,"Invalid enum selection at setting [$fieldName]. Possible values are: [${valuesMap.keys}]")
            ValidationResult.success(chkEnum)
        } catch (e: Exception){
            ValidationResult.error(storedValue,"Critical error deserializing enum [$fieldName]: ${e.localizedMessage}")
        }
    }

    override fun serialize(input: T): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input.name))
    }

    override fun correctEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        return ValidationResult.success(input)
    }

    override fun validateEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
        return ValidationResult.success(input)
    }

    @Environment(EnvType.CLIENT)
    override fun widgetEntry(choicePredicate: ChoiceValidator<T>): ClickableWidget {
        return when(widgetType){
            WidgetType.POPUP -> {
                EnumPopupButtonWidget(this.translation(), choicePredicate, this)
            }
            WidgetType.CYCLING -> {
                CyclingOptionsWidget(choicePredicate, this)
            }
        }
    }

    override fun description(): MutableText {
        return FcText.translatable(descriptionKey(),valuesMap.keys.toString())
    }

    override fun instanceEntry(): Entry<T> {
        return ValidatedEnum(this.defaultValue,this.widgetType)
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

    @Environment(EnvType.CLIENT)
    private class EnumPopupButtonWidget<T: Enum<*>>(private val name: Text, choicePredicate: ChoiceValidator<T>, private val entry: Entry<T>): PressableWidget(0,0,90,20, FcText.empty()) {

        val constants = entry.supplyEntry()::class.java.enumConstants.filter { choicePredicate.validateEntry(it,EntryValidator.ValidationType.STRONG).isValid() }

        override fun getMessage(): Text {
            return entry.supplyEntry().let { it.transLit(it.name) }
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
            for (const in constants) {
                buttonWidth = max(buttonWidth, textRenderer.getWidth(const.let { it.transLit(it.name) }))
            }
            buttonWidth = max(150, buttonWidth + 4)
            var prevParent = "title"
            for (const in constants){
                val button = EnumOptionWidget(const, buttonWidth, {c -> (c as Enum<*>) != entry.supplyEntry()}, { entry.applyEntry(it); (MinecraftClient.getInstance().currentScreen as? ConfigScreen)?.setPopup(null) })
                builder.addElement(const.name,button,prevParent,PopupWidget.Builder.PositionRelativePos.BELOW)
                prevParent = const.name
            }
            builder.positionX(PopupWidget.Builder.boundedByScreen { w -> this.x + this.width/2 - w/2 })
            builder.positionY(PopupWidget.Builder.boundedByScreen { this.y - 20 })
            (MinecraftClient.getInstance().currentScreen as? ConfigScreen)?.setPopup(
                builder.build()
            )
        }
    }

    @Environment(EnvType.CLIENT)
    private class EnumOptionWidget<T: Enum<*>>(private val thisVal: T, width: Int, private val activePredicate: Predicate<T>, private val valueApplier: Consumer<T>): PressableWidget(0,0,width,20, thisVal.transLit(thisVal.name)) {

        init {
            thisVal.descLit("").takeIf { it.string != "" }?.also { tooltip = Tooltip.of(it) }
        }

        override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            this.active = activePredicate.test(thisVal)
            super.renderWidget(context, mouseX, mouseY, delta)
        }

        override fun getNarrationMessage(): MutableText {
            return thisVal.transLit(thisVal.name)
        }

        override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
            appendDefaultNarrations(builder)
        }

        override fun onPress() {
            valueApplier.accept(thisVal)
        }

    }

    @Environment(EnvType.CLIENT)
    private class CyclingOptionsWidget<T: Enum<*>>(choicePredicate: ChoiceValidator<T>, private val entry: Entry<T>): PressableWidget(0,0,90,20, entry.supplyEntry().let { it.transLit(it.name) }) {

        private val constants = entry.supplyEntry()::class.java.enumConstants.filter { choicePredicate.validateEntry(it,EntryValidator.ValidationType.STRONG).isValid() }

        init {
            entry.supplyEntry().descLit("").takeIf { it.string != "" }?.also { tooltip = Tooltip.of(it) }
        }

        override fun getNarrationMessage(): MutableText {
            return entry.supplyEntry().let { it.transLit(it.name) }
        }

        override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
            appendDefaultNarrations(builder)
        }

        override fun onPress() {
            val newIndex = (constants.indexOf(entry.supplyEntry()) + 1).takeIf { it < constants.size } ?: 0
            val newConst = constants[newIndex]
            message = newConst.let { it.transLit(it.name) }
            newConst.descLit("").takeIf { it.string != "" }?.also { tooltip = Tooltip.of(it) }
            entry.applyEntry(newConst)
        }

    }

}