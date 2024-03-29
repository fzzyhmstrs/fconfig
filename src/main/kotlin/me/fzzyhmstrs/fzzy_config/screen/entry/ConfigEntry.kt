package me.fzzyhmstrs.fzzy_config.screen.entry

import me.fzzyhmstrs.fzzy_config.screen.widget.ConfigListWidget
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.ElementListWidget
import net.minecraft.text.Text
import net.minecraft.util.Colors

internal open class ConfigEntry(val name: Text, protected val description: Text, protected val parent: ConfigListWidget, protected val widget: ClickableWidget): ElementListWidget.Entry<ConfigEntry>() {

    init {
        if(description.string != "") {
            widget.tooltip = Tooltip.of(description)
        }
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
        if (this.isMouseOver(mouseX.toDouble(), mouseY.toDouble()) && widget.tooltip != null){
            MinecraftClient.getInstance().currentScreen?.setTooltip(widget.tooltip, HoveredTooltipPositioner.INSTANCE,this.isFocused)
        }
        //75 = 10 + 20 + 20 + 20 + 5 = padding to scroll + revert width + default width + forward width + pad to widget
        //positions i at the left-hand side of the main widget
        val i = parent.scrollbarPositionX - 75
        val j = y - 2
        widget.setPosition(i - widget.width, j)
        widget.render(context, mouseX, mouseY, tickDelta)
        context.drawTextWithShadow(
            parent.getClient().textRenderer,
            this.name,
            x,
            y + entryHeight / 2 - parent.getClient().textRenderer.fontHeight / 2,
            Colors.WHITE
        )

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
        super.setFocused(focused)
    }
}