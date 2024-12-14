/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.entry

import com.mojang.blaze3d.systems.RenderSystem
import me.fzzyhmstrs.fzzy_config.annotations.Action
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.screen.decoration.SpriteDecorated
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowProvider
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.ConfigListWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.isNotEmpty
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ScreenRect
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.tooltip.FocusedTooltipPositioner
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.ElementListWidget
import net.minecraft.text.OrderedText
import net.minecraft.text.Text
import net.minecraft.util.Colors
import kotlin.math.min

//client
internal open class BaseConfigEntry(
    val name: Text,
    val description: Text,
    private var actions: Set<Action>,
    protected val parent: ConfigListWidget,
    protected val widget: ClickableWidget)
    :
    ElementListWidget.Entry<BaseConfigEntry>()
{

    private val truncatedName = ConfigApiImplClient.ellipses(name, if(widget is SpriteDecorated) 124 else 146)
    private val tooltip: List<OrderedText> by lazy {
        createTooltip()
    }
    private val fullTooltip: List<OrderedText> by lazy {
        createFullTooltip()
    }

    init {
        if (widget is SuggestionWindowProvider)
            widget.addListener(parent)
        actions = actions.toSortedSet { a1, a2 -> a1.ordinal.compareTo(a2.ordinal) }
    }

    fun restartTriggering(actions: Set<Action>): BaseConfigEntry {
        this.actions = actions.toSortedSet { a1, a2 -> a1.ordinal.compareTo(a2.ordinal) }
        return this
    }

    fun positionWidget(y: Int) {
        widget.setPosition(parent.scrollbarX - widget.width - 10, y)
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
        //75 = 10 + 20 + 20 + 20 + 5 = padding to scroll + revert width + default width + forward width + pad to widget
        //positions i at the left-hand side of the main widget
        widget.setPosition(parent.scrollbarX - widget.width - 10, y)
        widget.render(context, mouseX, mouseY, tickDelta)
        if (widget is SpriteDecorated)
            widget.renderDecoration(context, widget.x - 22, widget.y + 2, tickDelta)
        context.drawTextWithShadow(
            parent.getClient().textRenderer,
            truncatedName,
            x,
            y + entryHeight / 2 - parent.getClient().textRenderer.fontHeight / 2,
            Colors.WHITE
        )
        if (actions.isNotEmpty()) {
            var offset = -24
            val offsetIncrement = if(actions.size == 1) 19 else min(19, (x - 24) / (actions.size - 1))
            for (action in actions) {
                RenderSystem.enableBlend()
                RenderSystem.enableDepthTest()
                context.drawTex(action.sprite, x + offset, y, 20, 20)
                if (isMouseOverAction(offset, offsetIncrement, mouseX, mouseY, x, y)) {
                    MinecraftClient.getInstance().currentScreen?.setTooltip(MinecraftClient.getInstance().textRenderer.wrapLines(restartText(action), 190), HoveredTooltipPositioner.INSTANCE, this.isFocused)
                }
                offset -= offsetIncrement
            }
        }

        if (mouseY < parent.y || mouseY > parent.bottom) return

        val list: MutableList<OrderedText> = mutableListOf()

        widget.tooltip?.let { list.addAll(it.getLines(MinecraftClient.getInstance())) }

        if (this.isFocused && MinecraftClient.getInstance().navigationType.isKeyboard && fullTooltip.isNotEmpty()) {
            if(list.isNotEmpty()) list.add(FcText.empty().asOrderedText())
            list.addAll(fullTooltip)
            MinecraftClient.getInstance().currentScreen?.setTooltip(list, FocusedTooltipPositioner(ScreenRect(x, y, entryWidth, entryHeight)), this.isFocused)
        } else if (this.isMouseOver(mouseX.toDouble(), mouseY.toDouble()) && tooltip.isNotEmpty() && !MinecraftClient.getInstance().navigationType.isKeyboard) {
            if(list.isNotEmpty()) list.add(FcText.empty().asOrderedText())
            list.addAll(tooltip)
            MinecraftClient.getInstance().currentScreen?.setTooltip(list, HoveredTooltipPositioner.INSTANCE, true)
        }

        /*if (widget.isMouseOver(mouseX.toDouble(), mouseY.toDouble()) && widget.tooltip != null) {
            //let widgets tooltip win
        } else if (this.isFocused && MinecraftClient.getInstance().navigationType.isKeyboard && widget.tooltip != null && actions.isNotEmpty()) {
            val lines: MutableList<OrderedText> = mutableListOf()
            widget.tooltip?.let { lines.addAll(it.getLines(MinecraftClient.getInstance())) }
            lines.add(FcText.empty().asOrderedText())
            for (action in actions) {
                lines.addAll(MinecraftClient.getInstance().textRenderer.wrapLines(restartText(action), 190))
            }
            MinecraftClient.getInstance().currentScreen?.setTooltip(lines, FocusedTooltipPositioner(ScreenRect(x, y, entryWidth, entryHeight)), this.isFocused)
        } else if (this.isFocused && MinecraftClient.getInstance().navigationType.isKeyboard && fullTooltip.isNotEmpty()) {

        } else if (this.isMouseOver(mouseX.toDouble(), mouseY.toDouble()) && tooltip.isNotEmpty()) {
            MinecraftClient.getInstance().currentScreen?.setTooltip(tooltip, HoveredTooltipPositioner.INSTANCE, this.isFocused)
        }*/
    }

    private fun isMouseOverAction(offset: Int, offsetIncrement: Int, mouseX: Int, mouseY: Int, x: Int, y: Int): Boolean {
        return mouseY >= y && mouseY < (y + 20) && mouseX >= (x + offset) && mouseX < (x + offset + offsetIncrement)
    }

    private fun createTooltip(): List<OrderedText> {
        val list: MutableList<OrderedText> = mutableListOf()
        if (truncatedName != name) {
            list.addAll(MinecraftClient.getInstance().textRenderer.wrapLines(name, 190))
            if (description.isNotEmpty())
                list.add(FcText.empty().asOrderedText())
        }
        if (description.isNotEmpty()) {
            list.addAll(MinecraftClient.getInstance().textRenderer.wrapLines(description, 190))
        }
        return list
    }

    private fun createFullTooltip(): List<OrderedText> {
        val list: MutableList<OrderedText> = mutableListOf()
        if (truncatedName != name) {
            list.addAll(MinecraftClient.getInstance().textRenderer.wrapLines(name, 190))
            list.add(FcText.empty().asOrderedText())
        }
        if (description.isNotEmpty()) {
            list.addAll(MinecraftClient.getInstance().textRenderer.wrapLines(description, 190))
        }
        if(actions.isNotEmpty()) {
            if (description.isNotEmpty())
                list.add(FcText.empty().asOrderedText())
            for (action in actions) {
                list.addAll(MinecraftClient.getInstance().textRenderer.wrapLines(restartText(action), 190))
            }
        }
        return list
    }

    private fun createTooltipString(tt: List<OrderedText>): String {
        val builder = StringBuilder()
        for (tip in tt) {
            tip.accept { _, _, codepoint ->
                builder.appendCodePoint(codepoint)
                true
            }
        }
        return builder.toString()
    }

    open fun restartText(action: Action): Text {
        return action.settingTooltip
    }

    override fun children(): MutableList<out Element> {
        return mutableListOf(widget)
    }

    override fun selectableChildren(): MutableList<out Selectable> {
        return mutableListOf(widget)
    }

    override fun setFocused(focused: Boolean) {
        widget.isFocused = focused
    }

    open fun appendEntryNarrations(builder: NarrationMessageBuilder) {
        val prefix = widget.tooltip?.let { createTooltipString(it.getLines(MinecraftClient.getInstance())) + ". " } ?: ""
        if (MinecraftClient.getInstance().navigationType.isKeyboard && fullTooltip.isNotEmpty()) {
            builder.put(NarrationPart.HINT, prefix +  createTooltipString(fullTooltip))
        } else if(tooltip.isNotEmpty()) {
            builder.put(NarrationPart.HINT, prefix +  createTooltipString(tooltip))
        }
    }

    @FunctionalInterface
    fun interface RightClickAction {
        fun rightClick(mouseX: Int, mouseY: Int, configEntry: BaseConfigEntry)
    }
}