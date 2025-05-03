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
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.descLit
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum.WidgetType
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.math.max

/**
 * A validated Enum Class
 *
 * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Enums) for more details and examples.
 * @param T the enum type. Any [Enum]
 * @param defaultValue Enum Constant used as the default for this setting
 * @param widgetType [WidgetType] defines the GUI selection type. Defaults to POPUP
 * @author fzzyhmstrs
 * @since 0.2.0
 */
open class ValidatedEnum<T: Enum<*>> @JvmOverloads constructor(defaultValue: T, private val widgetType: WidgetType = WidgetType.POPUP): ValidatedField<T>(defaultValue) {

    /**
     * A validated Enum Class, initialized with the enum class.
     *
     * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Enums) for more details and examples.
     * @param T the enum type. Any [Enum]
     * @param clazz Enum class. The first constant will be used as the default value.
     * @author fzzyhmstrs
     * @since Unknown
     */
    constructor(clazz: Class<T>): this(clazz.enumConstants[0])

    /**
     * A validated Enum Class, initialized with the enum class.
     *
     * [See the Wiki](https://moddedmc.wiki/en/project/fzzy-config/docs/config-concepts/validation/Enums) for more details and examples.
     * @param T the enum type. Any [Enum]
     * @param clazz Enum class. The first constant will be used as the default value.
     * @param widgetType [WidgetType] defines the GUI selection type. Defaults to POPUP
     * @author fzzyhmstrs
     * @since 0.7.0
     */
    constructor(clazz: Class<T>, widgetType: WidgetType): this(clazz.enumConstants[0], widgetType)

    @Suppress("UNCHECKED_CAST")
    private val valuesMap: Map<String, T> = defaultValue.declaringJavaClass.enumConstants.associateBy { (it as Enum<*>).name } as Map<String, T>

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<T> {
        return try {
            val string = toml.toString().uppercase()
            val chkEnum = valuesMap[string] ?: return ValidationResult.error(storedValue, ValidationResult.ErrorEntry.DESERIALIZATION, "Invalid enum for [$fieldName]. Possible values are: [${valuesMap.keys}]")
            ValidationResult.success(chkEnum)
        } catch (e: Throwable) {
            ValidationResult.error(storedValue, ValidationResult.ErrorEntry.DESERIALIZATION, "Exception deserializing enum [$fieldName]", e)
        }
    }

    @Internal
    override fun serialize(input: T): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input.name))
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<T>): ClickableWidget {
        return when(widgetType) {
            WidgetType.POPUP -> {
                EnumPopupButtonWidget(this.translation(), choicePredicate)
            }
            WidgetType.CYCLING -> {
                CyclingOptionsWidget(choicePredicate, this)
            }
        }
    }

    /**
     * creates a deep copy of this ValidatedEnum
     * return ValidatedEnum wrapping a copy of the currently stored object and widget type
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedEnum<T> {
        return ValidatedEnum(this.defaultValue, this.widgetType)
    }

    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        if (input == null) return false
        return try {
            @Suppress("UNCHECKED_CAST")
            input::class.java == defaultValue::class.java && validateEntry(input as T, EntryValidator.ValidationType.STRONG).isValid()
        } catch (e: Throwable) {
            false
        }
    }

    @Internal
    override fun description(fallback: String?): MutableText {
        return FcText.translatable(descriptionKey(), fallback ?: valuesMap.keys.toString())
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated Enum[value=$storedValue, validation=$valuesMap]"
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

    private fun openEnumPopup(name: Text, choicePredicate: ChoiceValidator<T> = ChoiceValidator.any(), xPosition: BiFunction<Int, Int, Int> = PopupWidget.Builder.center(), yPosition: BiFunction<Int, Int, Int> = PopupWidget.Builder.center()) {
        val builder = PopupWidget.Builder(name, spacingH = 0)
        val textRenderer = MinecraftClient.getInstance().textRenderer
        var buttonWidth = 86
        val constants = this@ValidatedEnum.get().declaringJavaClass.enumConstants.mapNotNull {
            @Suppress("UNCHECKED_CAST")
            it as? T
        }.filter { choicePredicate.validateEntry(it, EntryValidator.ValidationType.STRONG).isValid() }
        for (const in constants) {
            buttonWidth = max(buttonWidth, textRenderer.getWidth(const.let { it.transLit(it.name) }))
        }
        buttonWidth = max(150, buttonWidth + 4)
        var prevParent = "title"
        for (const in constants) {
            val button = EnumOptionWidget(const, buttonWidth, {c -> (c as Enum<*>) != this@ValidatedEnum.get()}, { this@ValidatedEnum.accept(it); PopupWidget.pop() })
            builder.add(const.name, button, prevParent, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_CENTER)
            prevParent = const.name
        }
        builder.positionX(xPosition)
        builder.positionY(yPosition)
        builder.additionalNarration("fc.validated_field.current".translate(this@ValidatedEnum.get().transLit(this@ValidatedEnum.get().name)))
        PopupWidget.push(builder.build())
    }

    //client
    private inner class EnumPopupButtonWidget(private val name: Text, private val choicePredicate: ChoiceValidator<T>)
        : CustomPressableWidget(0, 0, 110, 20, FcText.EMPTY) {

        override fun getMessage(): Text {
            return this@ValidatedEnum.get().let { it.transLit(it.name) }
        }

        override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
            builder?.put(NarrationPart.TITLE, this.message)
        }

        override fun onPress() {
            openEnumPopup(name, choicePredicate, PopupWidget.Builder.popupContext { w -> this.x + this.width/2 - w/2 }, PopupWidget.Builder.popupContext { this.y - 20 })
        }
    }

    //client
    private class EnumOptionWidget<T: Enum<*>>(private val thisVal: T, width: Int, private val activePredicate: Predicate<T>, private val valueApplier: Consumer<T>): CustomPressableWidget(0, 0, width, 20, thisVal.transLit(thisVal.name)) {

        init {
            thisVal.descLit("").takeIf { it.string != "" }?.also { tooltip = Tooltip.of(it) }
        }

        override fun renderCustom(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
            this.active = activePredicate.test(thisVal)
            super.renderCustom(context, x, y, width, height, mouseX, mouseY, delta)
        }

        override fun getNarrationMessage(): MutableText {
            return thisVal.transLit(thisVal.name)
        }

        override fun onPress() {
            valueApplier.accept(thisVal)
        }

    }

    //client
    private class CyclingOptionsWidget<T: Enum<*>>(choicePredicate: ChoiceValidator<T>, private val entry: Entry<T, *>): CustomPressableWidget(0, 0, 110, 20, entry.get().let { it.transLit(it.name) }) {

        @Suppress("UNCHECKED_CAST")
        private val constants = entry.get().declaringJavaClass.enumConstants.mapNotNull { it as? T }.filter {
            choicePredicate.validateEntry(it, EntryValidator.ValidationType.STRONG).isValid()
        }

        init {
            entry.descLit("").takeIf { it.string != "" }?.let { tooltip = Tooltip.of(it) }
        }

        override fun getNarrationMessage(): MutableText {
            return entry.get().let { it.transLit(it.name) }
        }

        override fun onPress() {
            val newIndex = (constants.indexOf(entry.get()) + 1).takeIf { it < constants.size } ?: 0
            val newConst = constants[newIndex]
            message = newConst.let { it.transLit(it.name) }
            newConst.descLit("").takeIf { it.string != "" }?.also { tooltip = Tooltip.of(it) }
            entry.accept(newConst)
        }

    }

}