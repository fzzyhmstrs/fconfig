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
import net.minecraft.util.CommonColors
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
    ContainerObjectSelectionList<MapListWidget.MapEntry<K, V>>(Minecraft.getInstance(), 268, 160, 0, 22), SuggestionWindowListener {

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

    private var suggestionWindowElement: GuiEventListener? = null

    override fun setSuggestionWindowElement(element: GuiEventListener?) {
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

    override fun extractListSeparators(context: GuiGraphicsExtractor) {
    }

    override fun extractListBackground(context: GuiGraphicsExtractor) {
    }

    override fun getRowWidth(): Int {
        return 258 //16 padding, 20 slider width and padding
    }

    override fun getRowLeft(): Int {
        return this.x
    }

    override fun scrollBarX(): Int {
        return this.x + this.width - 6
    }

    private fun makeVisible(entry: MapEntry<K, V>) {
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
        for (e in entryMap) {
            this.addEntry(ExistingEntry(e.key, e.value, this, entryValidator))
        }
        this.addEntry(NewEntry(keySupplier, valueSupplier, this, entryValidator))
    }

    private class ExistingEntry<K, V>(private val key: me.fzzyhmstrs.fzzy_config.entry.Entry<K, *>, private val value: me.fzzyhmstrs.fzzy_config.entry.Entry<V, *>, private val parent: MapListWidget<K, V>, keyValidator: BiFunction<MapListWidget<K, V>, MapEntry<K, V>?, ChoiceValidator<K>>): MapEntry<K, V>() {

        private var clickedWidget: GuiEventListener? = null

        private val keyWidget = key.widgetAndTooltipEntry(keyValidator.apply(parent, this)).also { if (it is SuggestionWindowProvider) it.addListener(parent) }
        private val valueWidget = value.widgetAndTooltipEntry(ChoiceValidator.any()).also { if (it is SuggestionWindowProvider) it.addListener(parent) }

        private val deleteWidget = CustomButtonWidget.builder { parent.removeEntry(this) }
            .textures(TextureIds.DELETE,
                TextureIds.DELETE_INACTIVE,
                TextureIds.DELETE_HIGHLIGHTED)
            .tooltip(TextureIds.DELETE_LANG)
            .narrationSupplier { _, _ -> TextureIds.DELETE_LANG }
            .size(20, 20)
            .build()

        fun get(): Pair<K, V> {
            return Pair(key.get(), value.get())
        }

        override fun children(): MutableList<out GuiEventListener> {
            return mutableListOf(keyWidget, valueWidget, deleteWidget)
        }

        override fun narratables(): MutableList<out NarratableEntry> {
            return mutableListOf(keyWidget, valueWidget, deleteWidget)
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
            keyWidget.setPosition(x, y)
            keyWidget.extractRenderState(context, mouseX, mouseY, tickDelta)
            context.text(parent.minecraft.font, TextureIds.MAP_ARROW, x + 115, y + 5, CommonColors.WHITE)
            valueWidget.setPosition(x+124, y)
            valueWidget.extractRenderState(context, mouseX, mouseY, tickDelta)
            deleteWidget.setPosition(x+238, y)
            deleteWidget.extractRenderState(context, mouseX, mouseY, tickDelta)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class NewEntry<K, V>(private val keySupplier: me.fzzyhmstrs.fzzy_config.entry.Entry<K, *>, private val valueSupplier: me.fzzyhmstrs.fzzy_config.entry.Entry<V, *>, private val parent: MapListWidget<K, V>, private val validator: BiFunction<MapListWidget<K, V>, MapEntry<K, V>?, ChoiceValidator<K>>): MapEntry<K, V>() {

        private val addWidget = CustomButtonWidget.builder {
                parent.removeEntry(this)
                parent.addEntry(ExistingEntry(keySupplier.instanceEntry() as Entry1<K, *>, valueSupplier.instanceEntry() as Entry1<V, *>, parent, validator))
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
            addWidget.setPosition(x+238, y)
            addWidget.extractRenderState(context, mouseX, mouseY, tickDelta)
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
            return ValidationResult.predicated(
                input,
                !disallowed.apply(self).containsKey(input),
                ValidationResult.Errors.INVALID) { b ->
                b.content("No duplicate map keys")
            }.also { self.isValid = it.isValid() }
        }

    }
}