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
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.widget.ElementListWidget
import net.minecraft.client.input.KeyInput
import java.util.function.BiFunction
import java.util.function.Function

//client
internal class ListListWidget<T>(entryList: List<me.fzzyhmstrs.fzzy_config.entry.Entry<T, *>>, entrySupplier: me.fzzyhmstrs.fzzy_config.entry.Entry<T, *>, entryValidator: BiFunction<ListListWidget<T>, ListEntry<T>?, ChoiceValidator<T>>)
    :
    ElementListWidget<ListListWidget.ListEntry<T>>(MinecraftClient.getInstance(), 144, 160, 0, 22), SuggestionWindowListener {

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

    private var suggestionWindowElement: Element? = null

    override fun setSuggestionWindowElement(element: Element?) {
        this.suggestionWindowElement = element
    }


    override fun drawHeaderAndFooterSeparators(context: DrawContext?) {
    }

    override fun drawMenuListBackground(context: DrawContext?) {
    }

    override fun getRowWidth(): Int {
        return 134 //16 padding, 20 slider width and padding
    }

    override fun getRowLeft(): Int {
        return this.x
    }

    override fun getScrollbarX(): Int {
        return this.x + this.width - 6
    }

    private fun makeVisible(entry: ListEntry<T>) {
        this.scrollTo(entry)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (suggestionWindowElement?.mouseClicked(mouseX, mouseY, button) == true) return true
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (suggestionWindowElement?.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) ?: hoveredElement(mouseX, mouseY).filter { element: Element -> element.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) }.isPresent) return true
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(input: KeyInput): Boolean {
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

        private var clickedWidget: Element? = null

        private val entryWidget = entry.widgetAndTooltipEntry(validator.apply(parent, this)).also { if (it is SuggestionWindowProvider) it.addListener(parent) }

        private val deleteWidget = CustomButtonWidget.builder { parent.children().let { list ->
                list.indexOf(this).takeIf { i -> i >=0 && i<list.size }?.let {
                        i -> list.removeAt(i)
                }
            } }
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

        override fun children(): MutableList<out Element> {
            return mutableListOf(entryWidget, deleteWidget)
        }

        override fun selectableChildren(): MutableList<out Selectable> {
            return mutableListOf(entryWidget, deleteWidget)
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            hoveredElement(mouseX, mouseY).ifPresentOrElse({clickedWidget = it}, {clickedWidget = null})
            return super.mouseClicked(mouseX, mouseY, button)
        }

        override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (clickedWidget != null) {
                return (clickedWidget?.mouseReleased(mouseX, mouseY, button) ?: super.mouseReleased(mouseX, mouseY, button)).also { clickedWidget = null }
            }
            return super.mouseReleased(mouseX, mouseY, button)
        }

        override fun render(
            context: DrawContext,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            tickDelta: Float
        ) {
            entryWidget.setPosition(x, y)
            entryWidget.render(context, mouseX, mouseY, tickDelta)
            deleteWidget.setPosition(x+114, y)
            deleteWidget.render(context, mouseX, mouseY, tickDelta)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class NewEntry<T>(private val entrySupplier: me.fzzyhmstrs.fzzy_config.entry.Entry<T, *>, private val parent: ListListWidget<T>, private val validator: BiFunction<ListListWidget<T>, ListEntry<T>?, ChoiceValidator<T>>): ListEntry<T>() {

        private val addWidget = CustomButtonWidget.builder {
                parent.children().let { it.add(it.lastIndex, ExistingEntry(entrySupplier.instanceEntry() as me.fzzyhmstrs.fzzy_config.entry.Entry<T, *>, parent, validator)) }
                parent.makeVisible(this)
            }
            .textures(TextureIds.ADD,
                TextureIds.ADD_INACTIVE,
                TextureIds.ADD_HIGHLIGHTED)
            .tooltip(TextureIds.ADD_LANG)
            .narrationSupplier { _, _ -> TextureIds.ADD_LANG }
            .size(20, 20)
            .build()

        override fun children(): MutableList<out Element> {
            return mutableListOf(addWidget)
        }

        override fun selectableChildren(): MutableList<out Selectable> {
            return mutableListOf(addWidget)
        }

        override fun render(
            context: DrawContext,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            tickDelta: Float
        ) {
            addWidget.setPosition(x+114, y)
            addWidget.render(context, mouseX, mouseY, tickDelta)
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