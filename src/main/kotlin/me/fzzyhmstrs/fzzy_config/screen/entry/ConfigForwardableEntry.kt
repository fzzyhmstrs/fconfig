package me.fzzyhmstrs.fzzy_config.screen.entry

import me.fzzyhmstrs.fzzy_config.screen.ConfigListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextlessConfigActionWidget
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.PlainTextContent
import net.minecraft.text.Text
import net.minecraft.util.Colors

open class ConfigForwardableEntry(
    name: Text,
    description: Text,
    parent: ConfigListWidget,
    widget: ClickableWidget,
    private val revert: TextlessConfigActionWidget,
    private val default: TextlessConfigActionWidget,
    private val forward: TextlessConfigActionWidget
)
    :
    ConfigEntry(name, description, parent, widget)
{

    private val children: List<ClickableWidget> = listOf(widget,revert,default)

    init {
        widget.message = name
        if(description.content != PlainTextContent.EMPTY){
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
        revert.setPosition(i + 5, j)
        revert.render(context, mouseX, mouseY, tickDelta)
        default.setPosition(i + 25, j)
        default.render(context, mouseX, mouseY, tickDelta)
        forward.setPosition(i + 45, j)
        forward.render(context, mouseX, mouseY, tickDelta)
        context.drawTextWithShadow(
            parent.getClient().textRenderer,
            this.name,
            x,
            y + entryHeight / 2 - parent.getClient().textRenderer.fontHeight / 2,
            Colors.WHITE
        )

    }

    override fun children(): MutableList<out Element> {
        return children.toMutableList()
    }

    override fun selectableChildren(): MutableList<out Selectable> {
        return children.toMutableList()
    }
}