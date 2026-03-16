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

import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.screen.SuggestionWindowListener
import me.fzzyhmstrs.fzzy_config.screen.SuggestionWindowProvider
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomButtonWidget
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.client.Minecraft
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.components.ContainerObjectSelectionList
import net.minecraft.client.input.KeyEvent
import java.util.function.BiFunction
import java.util.function.Function

//client
internal class ListListWidget<T>(entryList: List<me.fzzyhmstrs.fzzy_config.entry.Entry<T, *>>, entrySupplier: me.fzzyhmstrs.fzzy_config.entry.Entry<T, *>, entryValidator: BiFunction<ListListWidget<T>, ListEntry<T>?, ChoiceValidator<T>>)
    :
    ContainerObjectSelectionList<ListListWidget.ListEntry<T>>(Minecraft.getInstance(), 144, 160, 0, 22), SuggestionWindowListener {

    fun getRawList(skip: ListEntry<T>? = null): List<T> {
        val list: MutableList<T> = mutableListOf()
        for (e in this.children()) {
            if (e !is ExistingEntry<T>) continue
            if (e == skip) continue
            list.add(e.get())
        }
        return list.toList()
    }

    fun getList(): List<T> {
        val list: MutableList<T> = mutableListOf()
        for (e in this.children()) {
            if (e !is ExistingEntry<T>) continue
            if (!e.isValid) continue
            list.add(e.get())
        }
        return list.toList()
    }

    private var suggestionWindowElement: GuiEventListener? = null

    override fun setSuggestionWindowElement(element: GuiEventListener?) {
        this.suggestionWindowElement = element
    }


    override fun extractListSeparators(context: GuiGraphicsExtractor) {
    }

    override fun extractListBackground(context: GuiGraphicsExtractor) {
    }

    override fun getRowWidth(): Int {
        return 134 //16 padding, 20 slider width and padding
    }

    override fun getRowLeft(): Int {
        return this.x
    }

    override fun scrollBarX(): Int {
        return this.x + this.width - 6
    }

    private fun makeVisible(entry: ListEntry<T>) {
        this.scrollToEntry(entry)
    }

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        if (suggestionWindowElement?.mouseClicked(click, doubled) == true) return true
        return super.mouseClicked(click, doubled)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (suggestionWindowElement?.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) ?: getChildAt(mouseX, mouseY).filter { element: GuiEventListener -> element.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) }.isPresent) return true
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        if (suggestionWindowElement?.keyPressed(input) == true) return true
        return super.keyPressed(input)
    }

    init {
        for (e in entryList) {
            this.addEntry(ExistingEntry(e, this, entryValidator))
        }
        this.addEntry(NewEntry(entrySupplier, this, entryValidator))
    }

    private class ExistingEntry<T>(private val entry: me.fzzyhmstrs.fzzy_config.entry.Entry<T, *>, private val parent: ListListWidget<T>, validator: BiFunction<ListListWidget<T>, ListEntry<T>?, ChoiceValidator<T>>): ListEntry<T>() {

        private var clickedWidget: GuiEventListener? = null

        private val entryWidget = entry.widgetAndTooltipEntry(validator.apply(parent, this)).also { if (it is SuggestionWindowProvider) it.addListener(parent) }

        private val deleteWidget = CustomButtonWidget.builder { parent.removeEntry(this) }
            .textures(TextureIds.DELETE,
                TextureIds.DELETE_INACTIVE,
                TextureIds.DELETE_HIGHLIGHTED)
            .tooltip(TextureIds.DELETE_LANG)
            .narrationSupplier { _, _ -> TextureIds.DELETE_LANG }
            .size(20, 20)
            .build()

        fun get(): T {
            return entry.get()
        }

        override fun children(): MutableList<out GuiEventListener> {
            return mutableListOf(entryWidget, deleteWidget)
        }

        override fun narratables(): MutableList<out NarratableEntry> {
            return mutableListOf(entryWidget, deleteWidget)
        }

        override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
            getChildAt(click.x, click.y).ifPresentOrElse({clickedWidget = it}, {clickedWidget = null})
            return super.mouseClicked(click, doubled)
        }

        override fun mouseReleased(click: MouseButtonEvent): Boolean {
            if (clickedWidget != null) {
                return (clickedWidget?.mouseReleased(click) ?: super.mouseReleased(click)).also { clickedWidget = null }
            }
            return super.mouseReleased(click)
        }

        override fun extractContent(
            context: GuiGraphicsExtractor,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            tickDelta: Float
        ) {
            entryWidget.setPosition(x, y)
            entryWidget.extractRenderState(context, mouseX, mouseY, tickDelta)
            deleteWidget.setPosition(x+114, y)
            deleteWidget.extractRenderState(context, mouseX, mouseY, tickDelta)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class NewEntry<T>(private val entrySupplier: me.fzzyhmstrs.fzzy_config.entry.Entry<T, *>, private val parent: ListListWidget<T>, private val validator: BiFunction<ListListWidget<T>, ListEntry<T>?, ChoiceValidator<T>>): ListEntry<T>() {

        private val addWidget = CustomButtonWidget.builder {
                parent.removeEntry(this)
                parent.addEntry(ExistingEntry(entrySupplier.instanceEntry() as me.fzzyhmstrs.fzzy_config.entry.Entry<T, *>, parent, validator))
                parent.addEntry(this)
                parent.makeVisible(this)
            }
            .textures(TextureIds.ADD,
                TextureIds.ADD_INACTIVE,
                TextureIds.ADD_HIGHLIGHTED)
            .tooltip(TextureIds.ADD_LANG)
            .narrationSupplier { _, _ -> TextureIds.ADD_LANG }
            .size(20, 20)
            .build()

        override fun children(): MutableList<out GuiEventListener> {
            return mutableListOf(addWidget)
        }

        override fun narratables(): MutableList<out NarratableEntry> {
            return mutableListOf(addWidget)
        }

        override fun extractContent(
            context: GuiGraphicsExtractor,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            tickDelta: Float
        ) {
            addWidget.setPosition(x+114, y)
            addWidget.extractRenderState(context, mouseX, mouseY, tickDelta)
        }
    }

    abstract class ListEntry<T>: Entry<ListEntry<T>>() {
        var isValid = true
    }

    internal class ExcludeSelfChoiceValidator<T>(private val self: ListEntry<T>?, private val disallowed: Function<ListEntry<T>?, List<T>>) : ChoiceValidator<T>(
        ValuesPredicate(null, null)
    ) {
        override fun validateEntry(input: T, type: EntryValidator.ValidationType): ValidationResult<T> {
            if (self == null) return ValidationResult.success(input)
            return ValidationResult.predicated(
                input,
                !disallowed.apply(self).contains(input),
                ValidationResult.Errors.INVALID) { b ->
                b.content("No duplicate values in a set")
            }.also { self.isValid = it.isValid() }
        }

    }
}