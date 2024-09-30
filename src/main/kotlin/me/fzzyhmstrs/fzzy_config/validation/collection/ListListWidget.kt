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
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowListener
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowProvider
import me.fzzyhmstrs.fzzy_config.screen.widget.TextlessActionWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds
import me.fzzyhmstrs.fzzy_config.theme.ThemeKeys
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.widget.ElementListWidget
import java.util.function.BiFunction
import java.util.function.Function

//client
internal class ListListWidget<T>(entryList: List<me.fzzyhmstrs.fzzy_config.entry.Entry<T, *>>, entrySupplier: me.fzzyhmstrs.fzzy_config.entry.Entry<T, *>, entryValidator: BiFunction<ListListWidget<T>, ListEntry<T>?, ChoiceValidator<T>>)
    :
    ElementListWidget<ListListWidget.ListEntry<T>>(MinecraftClient.getInstance(), ThemeKeys.WIDGET_LIST_WIDTH.provideConfig() + 40, 160, 0, 22), SuggestionWindowListener {

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
        return ThemeKeys.WIDGET_LIST_WIDTH.provideConfig() + 24 //16 padding, 20 slider width and padding
    }

    override fun getScrollbarX(): Int {
        return this.x + this.width / 2 + this.rowWidth / 2 + 6
    }

    private fun makeVisible(entry: ListEntry<T>) {
        this.ensureVisible(entry)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (suggestionWindowElement?.mouseClicked(mouseX, mouseY, button) == true) return true
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (suggestionWindowElement?.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) ?: hoveredElement(mouseX, mouseY).filter { element: Element -> element.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) }.isPresent) return true
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (suggestionWindowElement?.keyPressed(keyCode, scanCode, modifiers) == true) return true
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    init {
        for (e in entryList) {
            this.addEntry(ExistingEntry(e, this, entryValidator))
        }
        this.addEntry(NewEntry(entrySupplier, this, entryValidator))
    }

    private class ExistingEntry<T>(private val entry: me.fzzyhmstrs.fzzy_config.entry.Entry<T, *>, private val parent: ListListWidget<T>, validator: BiFunction<ListListWidget<T>, ListEntry<T>?, ChoiceValidator<T>>): ListEntry<T>() {

        private var clickedWidget: Element? = null

        private val entryWidget = entry.widgetAndTooltipEntry(validator.apply(parent, this)).also {  it.width = ThemeKeys.WIDGET_LIST_WIDTH.provideConfig(); if (it is SuggestionWindowProvider) it.addListener(parent) }
        private val deleteWidget = TextlessActionWidget(
            TextureIds.DELETE,
            TextureIds.DELETE_INACTIVE,
            TextureIds.DELETE_HIGHLIGHTED,
            TextureIds.DELETE_LANG,
            TextureIds.DELETE_LANG,
            { true },
            { parent.children().let { list ->
                list.indexOf(this).takeIf { i -> i >= 0 && i<list.size }?.let {
                        i -> list.removeAt(i)
                }
            } })

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
            index: Int,
            y: Int,
            x: Int,
            entryWidth: Int,
            entryHeight: Int,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            tickDelta: Float
        ) {
            entryWidget.setPosition(x, y)
            entryWidget.render(context, mouseX, mouseY, tickDelta)
            deleteWidget.setPosition(x + ThemeKeys.WIDGET_LIST_WIDTH.provideConfig() + 4, y)
            deleteWidget.render(context, mouseX, mouseY, tickDelta)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class NewEntry<T>(private val entrySupplier: me.fzzyhmstrs.fzzy_config.entry.Entry<T, *>, private val parent: ListListWidget<T>, private val validator: BiFunction<ListListWidget<T>, ListEntry<T>?, ChoiceValidator<T>>): ListEntry<T>() {

        private val addWidget = TextlessActionWidget(
            TextureIds.ADD,
            TextureIds.ADD_INACTIVE,
            TextureIds.ADD_HIGHLIGHTED,
            TextureIds.ADD_LANG,
            TextureIds.ADD_LANG,
            { true },
            {
                parent.children().let { it.add(it.lastIndex, ExistingEntry(entrySupplier.instanceEntry() as me.fzzyhmstrs.fzzy_config.entry.Entry<T, *>, parent, validator)) }
                parent.makeVisible(this)
            })


        override fun children(): MutableList<out Element> {
            return mutableListOf(addWidget)
        }

        override fun selectableChildren(): MutableList<out Selectable> {
            return mutableListOf(addWidget)
        }

        override fun render(
            context: DrawContext,
            index: Int,
            y: Int,
            x: Int,
            entryWidth: Int,
            entryHeight: Int,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            tickDelta: Float
        ) {
            addWidget.setPosition(x + ThemeKeys.WIDGET_LIST_WIDTH.provideConfig() + 4, y)
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
                "No duplicate values in a set"
            ).also { self.isValid = it.isValid() }
        }

    }
}