/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.entry

import me.fzzyhmstrs.fzzy_config.cast
import me.fzzyhmstrs.fzzy_config.screen.decoration.Decorated
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomPressableWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.Translatable
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.Widget

/**
 * Basic [DynamicListWidget.Entry] that wraps a single widget with no other bells and whistles.
 * @param T the widget type; must be all of [Selectable], [Element], [Drawable], and [Widget]
 * @param parentElement [DynamicListWidget] parent instance. This will almost always be automatically provided via the BiFunction builder system the list widget uses.
 * @param scope String scope representation of this entry in the list. Might be as simple as "0", "1" etc. string version of the entry index.
 * @param texts [Translatable.Result] translation result to pass into the base entry. This is used for searching and so on. An empty result can be provided if these features aren't used.
 * @param height Integer height of the entry.
 * @param widget [T] the widget to wrap.
 * @author fzzyhmstrs
 * @since 0.6.5
 */
open class WidgetEntry<T>(parentElement: DynamicListWidget, scope: String, texts: Translatable.Result, height: Int, private val widget: T):
        DynamicListWidget.Entry(parentElement, texts, DynamicListWidget.Scope(scope)) where T: Selectable, T: Element, T: Drawable, T: Widget {
    
        override var h: Int = height
        private val selectables: List<SelectableElement> = listOf(widget).cast()
        private val children = mutableListOf(widget)
    
        override fun selectableChildren(): List<SelectableElement> {
            return selectables
        }
    
        override fun children(): MutableList<out Element> {
            return children
        }
    
        override fun renderEntry(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {
            widget.setPosition(x, y)
            widget.render(context, mouseX, mouseY, delta)
        }
    }
