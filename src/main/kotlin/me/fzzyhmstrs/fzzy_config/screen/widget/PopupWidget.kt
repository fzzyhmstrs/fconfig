package me.fzzyhmstrs.fzzy_config.screen.widget

import com.google.common.collect.Lists
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ParentElement
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.navigation.GuiNavigation
import net.minecraft.client.gui.navigation.GuiNavigationPath
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.text.Text

abstract class PopupWidget(x: Int, y: Int, width: Int, height: Int, message: Text, private val children: List<Element>, private val selectables: List<Selectable>, private val drawables: List<Drawable>)
    :
    ClickableWidget(x, y, width, height, message),
    ParentElement
{
    private var focused: Element? = null
    private var focusedSelectable: Selectable? = null
    private var dragging = false

    open fun onClose(){}

    fun position(screenWidth: Int, screenHeight: Int){
        this.x = screenWidth/2 - width/2
        this.y = screenHeight/2 - height/2
    }

    abstract fun selectableChildren(): List<Selectable>

    override fun renderWidget(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        TODO("Not yet implemented")
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder?) {
        TODO("Not yet implemented")
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return super<ParentElement>.mouseClicked(mouseX, mouseY, button).takeIf { it } ?: isMouseOver(mouseX, mouseY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return super<ParentElement>.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return super<ParentElement>.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun getNavigationPath(navigation: GuiNavigation): GuiNavigationPath? {
        return super<ParentElement>.getNavigationPath(navigation)
    }

    override fun setFocused(focused: Element?) {
        this.focused?.let { it.isFocused = false }
        focused?.let { it.isFocused = true }
        this.focused = focused
    }

    override fun setFocused(focused: Boolean) {
    }

    override fun isFocused(): Boolean {
        return getFocused() != null
    }

    override fun isDragging(): Boolean {
        return dragging
    }

    override fun setDragging(dragging: Boolean) {
        this.dragging = dragging
    }

    override fun getFocused(): Element? {
        return focused
    }
}