/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.validation.collection

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.entry.EntryCreator
import me.fzzyhmstrs.fzzy_config.entry.EntryHandler
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl
import me.fzzyhmstrs.fzzy_config.screen.context.ContextAction
import me.fzzyhmstrs.fzzy_config.screen.context.ContextResultBuilder
import me.fzzyhmstrs.fzzy_config.screen.context.ContextType
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.entry.WidgetEntry
import me.fzzyhmstrs.fzzy_config.screen.widget.*
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.NavigableTextFieldWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.descLit
import me.fzzyhmstrs.fzzy_config.util.FcText.transLit
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawNineSlice
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.util.ValidationResult.Companion.report
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedChoiceList.WidgetType
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.peanuuutz.tomlkt.TomlArrayBuilder
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.asTomlArray
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.*
import kotlin.math.max

/**
 * A validated collection of choices of any type that can be enabled or disabled piece-meal.
 *
 * Related to (and can be built from) [ValidatedChoice][me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedChoice], but this is a variable collection of none to all the possible choices, where [ValidatedChoice][me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedChoice] is always a single choice from the options list
 *
 * This is useful for a feature-flag-style setting, polling for active flags using `List#contains`. This can come with performance implementations, so for usage in high-traffic situations a series of booleans or [ValidatedBoolean][me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean] is preferable.
 *
 * This [ValidatedField] implements [List], so you can directly use it as if it were an immutable list. While it does technically accept duplicates, as list implementations typically allow, you really shouldn't include duplicates. It rarely makes sense for the setting type anyway.
 * @param T the choice type
 * @param defaultValues the choices that are "active" by default. Can be empty.
 * @param choices [List]<T> defining the appropriate choices
 * @param handler [EntryHandler] to provide validation tasks for individual choice elements
 * @param translationProvider BiFunction [T], String, [Text] - converts a choice instance [T] and the base translation key of this ValidatedChoice into a text Translation
 * @param descriptionProvider BiFunction [T], String, [Text] - converts a choice instance [T] and the base description key of this ValidatedChoice into a text Description
 * @param widgetType [WidgetType] defines the GUI selection type. Defaults to POPUP
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedCollectionExamples.choices
 * @see me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedChoice.toChoiceList
 * @see me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList.toChoiceList
 * @see me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedSet.toChoiceList
 * @author fzzyhmstrs
 * @since 0.6.3
 */
open class ValidatedChoiceList<T> @JvmOverloads @Deprecated("Use toChoiceSet from ValidatedChoice/List/Set when possible") constructor(
    defaultValues: List<T>,
    private val choices: List<T>,
    private val handler: EntryHandler<T>,
    private val translationProvider: BiFunction<T, String, MutableText> = BiFunction { t, _ -> t.transLit(t.toString()) },
    private val descriptionProvider: BiFunction<T, String, Text> = BiFunction { t, _ -> t.descLit("") },
    private val widgetType: WidgetType = WidgetType.POPUP): ValidatedField<List<T>>(defaultValues), List<T>
{
    init {
        if (!choices.containsAll(defaultValues))
            throw IllegalStateException("Default value list [$defaultValues] not within valid choice list [$choices]")
        if(choices.isEmpty())
            throw IllegalStateException("ValidatedChoiceSet can't have empty choice list")
    }

    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<List<T>> {
        return try {
            val array = toml.asTomlArray()
            val list: MutableList<T> = mutableListOf()
            val errors: MutableList<String> = mutableListOf()
            for ((index, el) in array.content.withIndex()) {
                val result = handler.deserializeEntry(el, errors, "$fieldName[$index]", 1).report(errors)
                if (!result.isError()) {
                    val candidate = result.get()
                    if (!choices.contains(candidate)) {
                        errors.add("$fieldName[$index] is not a valid option. Options: $choices")
                    } else {
                        list.add(index, candidate)
                    }
                }
            }
            if (errors.isNotEmpty()) {
                ValidationResult.error(list, "Error(s) encountered while deserializing choice list, some entries were skipped: $errors")
            } else {
                ValidationResult.success(list)
            }
        } catch (e: Throwable) {
            ValidationResult.error(defaultValue, "Critical error encountered while deserializing choice list [$fieldName], using defaults: ${e.message}.")
        }
    }

    @Internal
    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    override fun serialize(input: List<T>): ValidationResult<TomlElement> {
        val toml = TomlArrayBuilder()
        val errors: MutableList<String> = mutableListOf()
        try {
            for (entry in input) {
                val tomlEntry = handler.serializeEntry(entry, errors, 1)
                val annotations = if (entry != null)
                    try {
                        ConfigApiImpl.tomlAnnotations(entry!!::class)
                    } catch (e: Throwable) {
                        emptyList()
                    }
                else
                    emptyList()
                toml.element(tomlEntry, annotations)
            }
        } catch (e: Throwable) {
            return ValidationResult.error(toml.build(), "Critical error encountered while serializing choice list: ${e.localizedMessage}")
        }
        return ValidationResult.predicated(toml.build(), errors.isEmpty(), errors.toString())
    }

    @Internal
    @Suppress("SafeCastWithReturn", "UNCHECKED_CAST")
    override fun deserializedChanged(old: Any?, new: Any?): Boolean {
        old as? List<T> ?: return true
        new as? List<T> ?: return true
        if (old.size != new.size) return true
        for ((index, e) in old.withIndex()) {
            val e2 = new[index]
            if (handler.deserializedChanged(e, e2)) return true
        }
        return false
    }

    @Internal
    override fun correctEntry(input: List<T>, type: EntryValidator.ValidationType): ValidationResult<List<T>> {
        val list: MutableList<T> = mutableListOf()
        val errors: MutableList<String> = mutableListOf()
        for ((index, entry) in input.withIndex()) {
            val result = handler.correctEntry(entry, type).report(errors)
            val candidate = result.get()
            if (!choices.contains(candidate)) {
                errors.add("Entry $entry at index [$index] is not a valid option. Options: $choices")
            }
            list.add(result.get())
        }
        return ValidationResult.predicated(list, errors.isEmpty(), "Errors corrected in choice list: $errors")
    }

    @Internal
    override fun validateEntry(input: List<T>, type: EntryValidator.ValidationType): ValidationResult<List<T>> {
        val errors: MutableList<String> = mutableListOf()
        for ((index, entry) in input.withIndex()) {
            handler.validateEntry(entry, type).report(errors)
            if (!choices.contains(entry)) {
                errors.add("Entry $entry at index [$index] is not a valid option. Options: $choices")
            }
        }
        return ValidationResult.predicated(input, errors.isEmpty(), "Errors found in choice list: $errors")
    }

    /**
     * creates a deep copy of this ValidatedChoice (as deep as possible)
     * return ValidatedChoice wrapping a copy of the currently stored choice, allowable choices, and handler
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedChoiceList<T> {
        @Suppress("DEPRECATION")
        return ValidatedChoiceList(copyStoredValue(), this.choices, this.handler, this.translationProvider, this.descriptionProvider, this.widgetType)
    }

    @Internal
    @Suppress("UNCHECKED_CAST")
    override fun isValidEntry(input: Any?): Boolean {
        if (input !is List<*>) return false
        return try {
            validateEntry(input as List<T>, EntryValidator.ValidationType.STRONG).isValid()
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
    override fun copyValue(input: List<T>): List<T> {
        return input.toList()
    }

    @Internal
    //client
    override fun widgetEntry(choicePredicate: ChoiceValidator<List<T>>): ClickableWidget {
        return when(widgetType) {
            WidgetType.POPUP -> {
                CustomButtonWidget.builder("fc.validated_field.choice_set".translate()) { b -> openChoicesEditPopup(b) }.size(110, 20).build()
            }
            WidgetType.INLINE -> {
                val layout = LayoutWidget(paddingW = 0, spacingW = 0).clampWidth(110)
                for ((index, const) in choices.withIndex()) {
                    val button = ChoiceWidget(
                        const,
                        110,
                        { c: T -> this.get().contains(c) },
                        this,
                        { c ->
                            val newList = this.get().toMutableList()
                            if (!newList.remove(c)) {
                                newList.add(c)
                            }
                            this.accept(newList)
                        })
                    layout.add("choice$index", button, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_CENTER)
                }
                LayoutClickableWidget(0, 0, 110, 20 * choices.size, layout)
            }
            WidgetType.SCROLLABLE -> {
                CustomButtonWidget.builder("fc.validated_field.choice_set".translate()) { b -> openChoicesScrollableEditPopup(b) }.size(110, 20).build()
            }
        }
    }

    @Internal
    override fun entryDeco(): Decorated.DecoratedOffset? {
        return Decorated.DecoratedOffset(TextureDeco.DECO_CHOICE_LIST, 2, 2)
    }

    @Internal
    override fun contextActionBuilder(context: EntryCreator.CreatorContext): MutableMap<String, MutableMap<ContextType, ContextAction.Builder>> {
        val map = super.contextActionBuilder(context)
        val select = ContextAction.Builder("fc.validated_field.choice_set.select".translate()) { _ ->
            this.accept(emptyList())
            true }
            .withActive { s -> Supplier { s.get() && this.isEmpty() } }
        val deselect = ContextAction.Builder("fc.validated_field.choice_set.deselect".translate()) { _ ->
            this.accept(emptyList())
            true }
            .withActive { s -> Supplier { s.get() && this.isNotEmpty() } }

        map[ContextResultBuilder.COLLECTION] = mutableMapOf(
            ContextType.SELECT_ALL to select,
            ContextType.CLEAR to deselect
        )
        return map
    }

    /**
     * Determines which type of selector widget will be used for the Choice selector, default is POPUP
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    enum class WidgetType {
        /**
         * Will display a button stating "Edit Choices...", clicking the button will pop up a window with the available options to select or deselect.
         * @author fzzyhmstrs
         * @since 0.6.3
         */
        POPUP,
        /**
         * The choices are displayed inline in the setting screen, stacked on top of each other. This widget will take up more than one "normal" setting widget of height (unless you only have one choice, in which case... consider [ValidatedBoolean][me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean]).
         * @author fzzyhmstrs
         * @since 0.6.3
         */
        INLINE,
        /**
         * The choices are displayed in a popup with a scrollable list widget displaying the choices. Up to 6 choices are displayed in the "window", with additional options available via scrolling.
         * @author fzzyhmstrs
         * @since 0.6.5
         */
        SCROLLABLE
    }

    private fun openChoicesEditPopup(b: CustomButtonWidget) {
        val choiceListTitle = "fc.validated_field.choice_set".translate()
        val builder = PopupWidget.Builder(choiceListTitle)
        val textRenderer = MinecraftClient.getInstance().textRenderer
        var buttonWidth = textRenderer.getWidth(choiceListTitle)
        for (const in choices) {
            buttonWidth = max(buttonWidth, textRenderer.getWidth(this.translationProvider.apply(const, this.translationKey())) + 8)
        }
        builder.pushSpacing(UnaryOperator.identity()) { 0 }
        for ((index, const) in choices.withIndex()) {
            val button = ChoiceWidget(
                const,
                buttonWidth,
                { c: T -> this.get().contains(c) },
                this,
                { c ->
                    val newList = this.get().toMutableList()
                    if (!newList.remove(c)) {
                        newList.add(c)
                    }
                    this.accept(newList)
                })
            builder.add("choice$index", button, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_CENTER)
        }
        builder.positionX(PopupWidget.Builder.popupContext { w -> b.x + b.width/2 - w/2 })
        builder.positionY(PopupWidget.Builder.popupContext { b.y - 20 })
        builder.popSpacing()
        builder.addDoneWidget()
        PopupWidget.push(builder.build())
    }

    private fun openChoicesScrollableEditPopup(b: CustomButtonWidget) {
        val choiceListTitle = "fc.validated_field.choice_set".translate()
        val textRenderer = MinecraftClient.getInstance().textRenderer
        var buttonWidth = textRenderer.getWidth(choiceListTitle)
        val entries: MutableList<BiFunction<DynamicListWidget, Int, out DynamicListWidget.Entry>> = mutableListOf()
        for (const in choices) {
            buttonWidth = max(buttonWidth, textRenderer.getWidth(this.translationProvider.apply(const, this.translationKey())) + 8)
        }
        if (choices.size <= 6)
            buttonWidth += 10
        for ((index, const) in choices.withIndex()) {
            entries.add( BiFunction { list, _ ->
                val button = ChoiceWidget(
                    const,
                    buttonWidth,
                    { c: T -> this.get().contains(c) },
                    this,
                    { c ->
                        val newList = this.get().toMutableList()
                        if (!newList.remove(c)) {
                            newList.add(c)
                        }
                        this.accept(newList)
                    })
                val name = this.translationProvider.apply(const, this.translationKey())
                val desc = this.descriptionProvider.apply(const, this.descriptionKey()).takeIf { it.string != "" }
                WidgetEntry(list, "choice$index", Translatable.Result(name, desc, null), 20, button)
            })
        }
        var listWidth = buttonWidth
        val spec = if (entries.size > 6) {
            listWidth += 10
            DynamicListWidget.ListSpec(leftPadding = 0, rightPadding = 4, verticalPadding = 0)
        } else {
            DynamicListWidget.ListSpec(leftPadding = 0, rightPadding = -6, verticalPadding = 0)
        }
        val entryList = DynamicListWidget(MinecraftClient.getInstance(), entries, 0, 0, listWidth, 120, spec)

        val builder = PopupWidget.Builder(choiceListTitle)
        builder.add("choice_list", entryList, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_LEFT_AND_JUSTIFY)
        if (entries.size > 6) {
            val searchField = NavigableTextFieldWidget(MinecraftClient.getInstance().textRenderer, listWidth, 20, FcText.EMPTY)
            searchField.setMaxLength(100)
            searchField.text = ""
            fun setColor(entries: Int) {
                if(entries > 0)
                    searchField.setEditableColor(-1)
                else
                    searchField.setEditableColor(0xFF5555)
            }
            searchField.setChangedListener { s -> setColor(entryList.search(s)) }
            searchField.tooltip = Tooltip.of("fc.config.search.desc".translate())
            builder.add("choice_search", searchField, LayoutWidget.Position.BELOW, LayoutWidget.Position.ALIGN_JUSTIFY_WEAK)
        }
        builder.positionX(PopupWidget.Builder.popupContext { w -> b.x + b.width/2 - w/2 })
        builder.positionY(PopupWidget.Builder.popupContext { b.y - 82 })
        builder.addDoneWidget()
        PopupWidget.push(builder.build())
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return "Validated Choice List[enabled=$storedValue, choices=$choices]"
    }

    // List Interface //////////////////////////////////

    override val size: Int
        get() = storedValue.size

    override fun get(index: Int): T {
        return storedValue[index]
    }

    override fun isEmpty(): Boolean {
        return storedValue.isEmpty()
    }

    override fun iterator(): Iterator<T> {
        return storedValue.iterator()
    }

    override fun listIterator(): ListIterator<T> {
        return storedValue.listIterator()
    }

    override fun listIterator(index: Int): ListIterator<T> {
        return storedValue.listIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        return storedValue.subList(fromIndex, toIndex)
    }

    override fun lastIndexOf(element: T): Int {
        return storedValue.lastIndexOf(element)
    }

    override fun indexOf(element: T): Int {
        return storedValue.indexOf(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return storedValue.containsAll(elements)
    }

    override fun contains(element: T): Boolean {
        return storedValue.contains(element)
    }

    // End List Interface //////////////////////////////

    //client
    private class ChoiceWidget<T>(private val thisVal: T, width: Int, private val selectedPredicate: Predicate<T>, private val entry: ValidatedChoiceList<T>, private val valueApplier: Consumer<T>): CustomPressableWidget(0, 0, width, 20, entry.translationProvider.apply(thisVal, entry.translationKey())) {

        init {
            entry.descriptionProvider.apply(thisVal, entry.descriptionKey()).takeIf { it.string != "" }?.also { tooltip = Tooltip.of(it) }
        }

        override val textures: TextureProvider = TextureSet.Quad(tex, disabled, highlighted, "widget/button_disabled_highlighted".fcId())

        var choiceSelected = selectedPredicate.test(thisVal)

        override fun renderBackground(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
            choiceSelected = selectedPredicate.test(thisVal)
            RenderSystem.enableBlend()
            RenderSystem.enableDepthTest()
            context.drawNineSlice(textures.get(choiceSelected, this.isSelected), x, y, width, height, this.alpha)
            if (choiceSelected) {
                context.drawTex(TextureIds.ENTRY_OK, x + width - 20, y, 20, 20)
            }
        }

        override fun renderCustom(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
            val text = message
            val i = getWidth() - 4 - (if (choiceSelected) 18 else 0)
            val j = MinecraftClient.getInstance().textRenderer.getWidth(text)
            val l = y + (getHeight() - MinecraftClient.getInstance().textRenderer.fontHeight + 1) / 2
            val orderedText = if (j > i) FcText.trim(text, i, MinecraftClient.getInstance().textRenderer) else text.asOrderedText()
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, orderedText, x + 4, l, -1)
        }

        override fun getMessage(): Text {
            return entry.translationProvider.apply(thisVal, entry.translationKey())
        }

        override fun getNarrationMessage(): MutableText {
            return if (choiceSelected)
                FcText.translatable("fc.validated_field.choice_set.selected", super.getNarrationMessage())
            else
                FcText.translatable("fc.validated_field.choice_set.deselected", super.getNarrationMessage())
        }

        override fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Text> {
            if (!((parentSelected && isFocused) || isMouseOver(mouseX.toDouble(), mouseY.toDouble()))) return TooltipChild.EMPTY
            return super.provideTooltipLines(mouseX, mouseY, parentSelected, keyboardFocused)
        }

        override fun onPress() {
            valueApplier.accept(thisVal)
        }
    }
}