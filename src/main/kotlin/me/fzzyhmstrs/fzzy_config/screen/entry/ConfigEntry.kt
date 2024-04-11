package me.fzzyhmstrs.fzzy_config.screen.entry

import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient
import me.fzzyhmstrs.fzzy_config.screen.widget.ConfigListWidget
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.ElementListWidget
import net.minecraft.text.Text
import net.minecraft.util.Colors
import org.lwjgl.glfw.GLFW

@Environment(EnvType.CLIENT)
internal open class ConfigEntry(
    val name: Text,
    protected val description: Text,
    protected val parent: ConfigListWidget,
    protected val widget: ClickableWidget,
    val copyAction: Runnable?,
    val pasteAction: Runnable?,
    protected val rightClickAction: RightClickAction?)
    :
    ElementListWidget.Entry<ConfigEntry>()
{

    private val truncatedName = ConfigApiImplClient.ellipses(name,if(widget is Decorated) 124 else 146)

    fun positionWidget(y: Int){
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
            widget.renderDecoration(context,widget.x - 22, widget.y + 2, tickDelta)
        context.drawTextWithShadow(
            parent.getClient().textRenderer,
            truncatedName,
            x,
            y + entryHeight / 2 - parent.getClient().textRenderer.fontHeight / 2,
            Colors.WHITE
        )
        if (widget.isMouseOver(mouseX.toDouble(), mouseY.toDouble()) && widget.tooltip != null){
            //let widgets tooltip win
        } else if (this.isMouseOver(mouseX.toDouble(), mouseY.toDouble()) && description.string != ""){
            MinecraftClient.getInstance().currentScreen?.setTooltip(Tooltip.of(description), HoveredTooltipPositioner.INSTANCE,this.isFocused)
        }
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (((Screen.hasShiftDown() && keyCode == GLFW.GLFW_KEY_F10) || keyCode == GLFW.GLFW_KEY_MENU) && rightClickAction != null){
            rightClickAction.rightClick(this.widget.x,this.widget.y,this)
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    private var clickedWidget = false

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if(button == 1 && rightClickAction != null){
            rightClickAction.rightClick(mouseX.toInt(),mouseY.toInt(),this)
            clickedWidget = false
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button).also { clickedWidget = it }
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return if(clickedWidget) widget.mouseReleased(mouseX, mouseY, button) else super.mouseReleased(mouseX, mouseY, button)
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

    @FunctionalInterface
    fun interface RightClickAction{
        fun rightClick(mouseX: Int, mouseY: Int, configEntry: ConfigEntry)
    }
}