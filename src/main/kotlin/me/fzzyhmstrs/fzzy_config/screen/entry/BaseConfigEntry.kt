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
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowProvider
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.ConfigListWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.FcText.translate
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawGuiTexture
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ScreenRect
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.tooltip.FocusedTooltipPositioner
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.tooltip.WidgetTooltipPositioner
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.ElementListWidget
import net.minecraft.text.MutableText
import net.minecraft.text.OrderedText
import net.minecraft.text.Text
import net.minecraft.util.Colors
import net.minecraft.util.Formatting

@Environment(EnvType.CLIENT)
internal open class BaseConfigEntry(
    val name: Text,
    protected val description: Text,
    protected var isRestartTriggering: Boolean,
    protected val parent: ConfigListWidget,
    protected val widget: ClickableWidget)
    :
    ElementListWidget.Entry<BaseConfigEntry>()
{

    private val truncatedName = ConfigApiImplClient.ellipses(name, if(widget is Decorated) 124 else 146)
    private val tooltip: List<OrderedText> by lazy {
        createTooltip()
    }
    private val tooltipString: String by lazy {
        createTooltipString()
    }

    init {
        if (widget is SuggestionWindowProvider)
            widget.addListener(parent)
    }

    fun restartTriggering(bl: Boolean): BaseConfigEntry {
        isRestartTriggering = bl
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
        if (widget is Decorated)
            widget.renderDecoration(context, widget.x - 22, widget.y + 2, tickDelta)
        context.drawTextWithShadow(
            parent.getClient().textRenderer,
            truncatedName,
            x,
            y + entryHeight / 2 - parent.getClient().textRenderer.fontHeight / 2,
            Colors.WHITE
        )
        if (isRestartTriggering) {
            RenderSystem.enableBlend()
            RenderSystem.enableDepthTest()
            context.drawGuiTexture("widget/entry_error".fcId(), x - 24, y, 20, 20)
        }
        if (widget.isMouseOver(mouseX.toDouble(), mouseY.toDouble()) && widget.tooltip != null) {
            //let widgets tooltip win
        } else if (this.isMouseOver(mouseX.toDouble(), mouseY.toDouble()) && tooltip.isNotEmpty()) {
            MinecraftClient.getInstance().currentScreen?.setTooltip(tooltip, HoveredTooltipPositioner.INSTANCE, this.isFocused)
        } else if (this.isFocused && MinecraftClient.getInstance().navigationType.isKeyboard && tooltip.isNotEmpty()) {
            MinecraftClient.getInstance().currentScreen?.setTooltip(tooltip, FocusedTooltipPositioner(ScreenRect(x, y, entryWidth, entryHeight)), this.isFocused)
        }
    }

    private fun createTooltip(): List<OrderedText> {
        val list: MutableList<OrderedText> = mutableListOf()
        if(isRestartTriggering) {
            list.addAll(MinecraftClient.getInstance().textRenderer.wrapLines(restartText().formatted(Formatting.RED), 170))
            if (description.string != "")
                list.add(FcText.empty().asOrderedText())
        }
        if (description.string != "") {
            list.addAll(MinecraftClient.getInstance().textRenderer.wrapLines(description, 170))
        }
        return list
    }
    private fun createTooltipString(): String {
        val builder = StringBuilder()
        for (tip in tooltip) {
            tip.accept{ _, _, codepoint ->
                builder.appendCodePoint(codepoint)
                true
            }
        }
        return builder.toString()
    }
    open fun restartText(): MutableText {
        return "fc.config.restart.warning".translate()
    }

    override fun children(): MutableList<out Element> {
        return mutableListOf(widget)
    }

    override fun selectableChildren(): MutableList<out Selectable> {
        return mutableListOf(widget)
    }

    override fun setFocused(focused: Boolean) {
        if(description.string != "") {
            widget.tooltip = Tooltip.of(description)
        }
        widget.isFocused = focused
    }

    open fun appendEntryNarrations(builder: NarrationMessageBuilder) {
        if(tooltip.isNotEmpty()) {
            builder.put(NarrationPart.HINT, tooltipString)
        }
    }

    @FunctionalInterface
    fun interface RightClickAction {
        fun rightClick(mouseX: Int, mouseY: Int, configEntry: BaseConfigEntry)
    }
}