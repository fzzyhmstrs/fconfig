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
import me.fzzyhmstrs.fzzy_config.entry.EntryOpener
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.screen.entry.WidgetEntry
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutClickableWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.LayoutWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.NavigableTextFieldWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.descLit
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.Translatable
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
 * @since 0.2.0, implements EntryOpener as of 0.7.4
 */
open class ValidatedEnum<T: Enum<*>> @JvmOverloads constructor(defaultValue: T, private val widgetType: WidgetType = WidgetType.POPUP): ValidatedField<T>(defaultValue), EntryOpener {

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
            val chkEnum = valuesMap[string] ?: return ValidationResult.error(storedValue, ValidationResult.Errors.DESERIALIZATION, "Invalid enum for [$fieldName]. Possible values are: [${valuesMap.keys}]")
            ValidationResult.success(chkEnum)
        } catch (e: Throwable) {
            ValidationResult.error(storedValue, ValidationResult.Errors.DESERIALIZATION, "Exception deserializing enum [$fieldName]", e)
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
            WidgetType.INLINE -> {
                val layout = LayoutWidget.Builder().paddingBoth(0).spacingBoth(0).clampWidth(110).build()
                val constants = this@ValidatedEnum.get().declaringJavaClass.enumConstants.mapNotNull {
                    @Suppress("UNCHECKED_CAST")
                    it as? T
                }.filter { choicePredicate.validateEntry(it, EntryValidator.ValidationType.STRONG).isValid() }
                for ((index, const) in constants.withIndex()) {
                    val button = EnumOptionWidget(const, 110, {c -> (c as Enum<*>) != this@ValidatedEnum.get()}, { this@ValidatedEnum.accept(it); PopupWidget.pop() })
                    layout.add("choice$index", button, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_CENTER)
                }
                LayoutClickableWidget(0, 0, 110, 20 * constants.size, layout).withNarrationAppender { builder ->
                    builder.put(NarrationPart.TITLE, "fc.validated_field.current".translate(this@ValidatedEnum.get().transLit(this@ValidatedEnum.get().name)).append(". "))
                }
            }
            WidgetType.SCROLLABLE -> {
                EnumScrollablePopupButtonWidget(this.translation(), choicePredicate)
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

    @Internal
    override fun open(args: List<String>) {
        openPopup(translation())
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
        CYCLING,
        /**
         * The enum constants are displayed inline in the setting screen, stacked on top of each other. This widget will take up more than one "normal" setting widget of height (unless you only have one constant, in which case... why?).
         * @author fzzyhmstrs
         * @since 0.7.4
         */
        INLINE,
        /**
         * The choices are displayed in a popup with a scrollable list widget displaying the choices. Up to 6 choices are displayed in the "window", with additional options available via scrolling.
         * @author fzzyhmstrs
         * @since 0.7.4
         */
        SCROLLABLE
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

    private fun openPopup(name: Text, choicePredicate: ChoiceValidator<T> = ChoiceValidator.any(), choiceOptionRunnable: Runnable = Runnable { }, xPosition: BiFunction<Int, Int, Int> = PopupWidget.Builder.center(), yPosition: BiFunction<Int, Int, Int> = PopupWidget.Builder.center()) {
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
        val entries: MutableList<BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry>> = mutableListOf()
        if (constants.size <= 6)
            buttonWidth += 10
        for ((index, const) in constants.withIndex()) {
            val button = EnumOptionWidget(const, buttonWidth, {c -> (c as Enum<*>) != this@ValidatedEnum.get()}, { this@ValidatedEnum.accept(it); PopupWidget.pop() })
            val n = const.transLit(const.name)
            val desc = const.descLit("").takeIf { it.string != "" }
            entries.add( BiFunction { list, _ ->
                WidgetEntry(list, "enum$index", Translatable.createResult(n, desc), 20, button)
            })
        }
        var listWidth = buttonWidth
        val spec = if (entries.size > 6) {
            listWidth += 10
            DynamicListWidget.ListSpec(leftPadding = 0, rightPadding = 4, verticalPadding = 0, listNarrationKey = "fc.narrator.position.entry")
        } else {
            DynamicListWidget.ListSpec(leftPadding = 0, rightPadding = -6, verticalPadding = 0, listNarrationKey = "fc.narrator.position.entry")
        }
        val entryList = DynamicListWidget(MinecraftClient.getInstance(), entries, 0, 0, listWidth, 120, spec)

        val builder = PopupWidget.Builder(name)
        builder.add("enum_list", entryList, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT)
        if (entries.size > 6) {
            val searchField = NavigableTextFieldWidget(MinecraftClient.getInstance().textRenderer, listWidth, 20, FcText.EMPTY)
            searchField.setMaxLength(100)
            searchField.text = ""
            fun setColor(entries: Int) {
                if(entries > 0)
                    searchField.setEditableColor(-1)
                else
                    searchField.setEditableColor(-43691)
            }
            searchField.setChangedListener { s -> setColor(entryList.search(s)) }
            searchField.setTooltip(Tooltip.of("fc.config.search.desc".translate()))
            builder.add("choice_search", searchField, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_JUSTIFY_WEAK)
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
            builder?.put(NarrationPart.TITLE, this.getMessage())
        }

        override fun onPress() {
            openEnumPopup(name, choicePredicate, PopupWidget.Builder.popupContext { w -> this.x + this.width/2 - w/2 }, PopupWidget.Builder.popupContext { this.y - 20 })
        }
    }

    //client
    private inner class EnumScrollablePopupButtonWidget(private val name: Text, private val choicePredicate: ChoiceValidator<T>)
        : CustomPressableWidget(0, 0, 110, 20, FcText.EMPTY) {

        override fun getMessage(): Text {
            return this@ValidatedEnum.get().let { it.transLit(it.name) }
        }

        override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
            builder?.put(NarrationPart.TITLE, this.getMessage())
        }

        override fun onPress() {
            openPopup(name, choicePredicate, {  }, PopupWidget.Builder.popupContext { w -> this.x + this.width/2 - w/2 }, PopupWidget.Builder.popupContext { this.y - 20 })
        }
    }

    //client
    private class EnumOptionWidget<T: Enum<*>>(private val thisVal: T, width: Int, private val activePredicate: Predicate<T>, private val valueApplier: Consumer<T>): CustomPressableWidget(0, 0, width, 20, thisVal.transLit(thisVal.name)) {

        init {
            thisVal.descLit("").takeIf { it.string != "" }?.also { setTooltip(Tooltip.of(it)) }
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
            entry.descLit("").takeIf { it.string != "" }?.let { setTooltip(Tooltip.of(it)) }
        }

        override fun getNarrationMessage(): MutableText {
            return entry.get().let { it.transLit(it.name) }
        }

        override fun onPress() {
            val newIndex = (constants.indexOf(entry.get()) + 1).takeIf { it < constants.size } ?: 0
            val newConst = constants[newIndex]
            message = newConst.let { it.transLit(it.name) }
            newConst.descLit("").takeIf { it.string != "" }?.also { setTooltip(Tooltip.of(it)) }
            entry.accept(newConst)
        }

    }

}