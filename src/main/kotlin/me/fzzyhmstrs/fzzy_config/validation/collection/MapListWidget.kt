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
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.widget.ElementListWidget
import net.minecraft.util.Colors
import java.util.function.BiFunction
import java.util.function.Function
import me.fzzyhmstrs.fzzy_config.entry.Entry as Entry1

//client
internal class MapListWidget<K, V>(
    entryMap: Map<Entry1<K, *>, Entry1<V, *>>,
    keySupplier: Entry1<K, *>,
    valueSupplier: Entry1<V, *>,
    entryValidator: BiFunction<MapListWidget<K, V>, MapEntry<K, V>?, ChoiceValidator<K>>)
    :
    ElementListWidget<MapListWidget.MapEntry<K, V>>(MinecraftClient.getInstance(), 272, 160, 0, 22), SuggestionWindowListener {

    fun getRawMap(skip: MapEntry<K, V>? = null): Map<K, V> {
        val map: MutableMap<K, V> = mutableMapOf()
        for (e in this.children()) {
            if (e !is ExistingEntry<K, V>) continue
            if (e == skip) continue
            val pair = e.get()
            map[pair.first] = pair.second
        }
        return map.toMap()
    }

    private var suggestionWindowElement: Element? = null

    override fun setSuggestionWindowElement(element: Element?) {
        this.suggestionWindowElement = element
    }

    fun getMap(): Map<K, V> {
        val map: MutableMap<K, V> = mutableMapOf()
        for (e in this.children()) {
            if (e !is ExistingEntry<K, V>) continue
            if (!e.isValid) continue
            val pair = e.get()
            map[pair.first] = pair.second
        }
        return map.toMap()
    }

    override fun drawHeaderAndFooterSeparators(context: DrawContext?) {
    }

    override fun drawMenuListBackground(context: DrawContext?) {
    }

    override fun getRowWidth(): Int {
        return 258 //16 padding, 20 slider width and padding
    }

    override fun getScrollbarX(): Int {
        return this.x + this.width / 2 + this.rowWidth / 2 + 6
    }

    private fun makeVisible(entry: MapEntry<K, V>) {
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
        for (e in entryMap) {
            this.addEntry(ExistingEntry(e.key, e.value, this, entryValidator))
        }
        this.addEntry(NewEntry(keySupplier, valueSupplier, this, entryValidator))
    }

    private class ExistingEntry<K, V>(private val key: me.fzzyhmstrs.fzzy_config.entry.Entry<K, *>, private val value: me.fzzyhmstrs.fzzy_config.entry.Entry<V, *>, private val parent: MapListWidget<K, V>, keyValidator: BiFunction<MapListWidget<K, V>, MapEntry<K, V>?, ChoiceValidator<K>>): MapEntry<K, V>() {

        private var clickedWidget: Element? = null

        private val keyWidget = key.widgetAndTooltipEntry(keyValidator.apply(parent, this)).also { if (it is SuggestionWindowProvider) it.addListener(parent) }
        private val valueWidget = value.widgetAndTooltipEntry(ChoiceValidator.any()).also { if (it is SuggestionWindowProvider) it.addListener(parent) }
        private val deleteWidget = TextlessActionWidget(
            TextureIds.DELETE,
            TextureIds.DELETE_INACTIVE,
            TextureIds.DELETE_HIGHLIGHTED,
            TextureIds.DELETE_LANG,
            TextureIds.DELETE_LANG,
            { true },
            { parent.children().let { list ->
                list.indexOf(this).takeIf { i -> i >=0 && i<list.size }?.let {
                        i -> list.removeAt(i)
                }
            } })

        fun get(): Pair<K, V> {
            return Pair(key.get(), value.get())
        }

        override fun children(): MutableList<out Element> {
            return mutableListOf(keyWidget, valueWidget, deleteWidget)
        }

        override fun selectableChildren(): MutableList<out Selectable> {
            return mutableListOf(keyWidget, valueWidget, deleteWidget)
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
            keyWidget.setPosition(x, y)
            keyWidget.render(context, mouseX, mouseY, tickDelta)
            context.drawTextWithShadow(parent.client.textRenderer, TextureIds.MAP_ARROW, x + 115, y + 5, Colors.WHITE)
            valueWidget.setPosition(x+124, y)
            valueWidget.render(context, mouseX, mouseY, tickDelta)
            deleteWidget.setPosition(x+238, y)
            deleteWidget.render(context, mouseX, mouseY, tickDelta)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class NewEntry<K, V>(private val keySupplier: me.fzzyhmstrs.fzzy_config.entry.Entry<K, *>, private val valueSupplier: me.fzzyhmstrs.fzzy_config.entry.Entry<V, *>, private val parent: MapListWidget<K, V>, private val validator: BiFunction<MapListWidget<K, V>, MapEntry<K, V>?, ChoiceValidator<K>>): MapEntry<K, V>() {

        private val addWidget = TextlessActionWidget(
            TextureIds.ADD,
            TextureIds.ADD_INACTIVE,
            TextureIds.ADD_HIGHLIGHTED,
            TextureIds.ADD_LANG,
            TextureIds.ADD_LANG,
            { true },
            {
                parent.children().let { it.add(it.lastIndex, ExistingEntry(keySupplier.instanceEntry() as Entry1<K, *>, valueSupplier.instanceEntry() as Entry1<V, *>, parent, validator)) }
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
            addWidget.setPosition(x+238, y)
            addWidget.render(context, mouseX, mouseY, tickDelta)
        }
    }

    abstract class MapEntry<K, V>: Entry<MapEntry<K, V>>() {
        var isValid = true
    }

    internal class ExcludeSelfChoiceValidator<K, V>(private val self: MapEntry<K, V>?, private val disallowed: Function<MapEntry<K, V>?, Map<K, V>>) : ChoiceValidator<K>(
        ValuesPredicate(null, null)
    ) {
        override fun validateEntry(input: K, type: EntryValidator.ValidationType): ValidationResult<K> {
            if (self == null) return ValidationResult.success(input)
            return ValidationResult.predicated(input, !disallowed.apply(self).containsKey(input), "No duplicate map keys").also { self.isValid = it.isValid() }
        }

    }
}