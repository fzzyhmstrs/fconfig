package me.fzzyhmstrs.fzzy_config.screen.widget.internal

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ParentElement
import net.minecraft.client.gui.navigation.GuiNavigation
import net.minecraft.client.gui.navigation.GuiNavigationPath
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.screen.ScreenTexts

abstract class CustomListWidget(private val client: MinecraftClient, x: Int, y: Int, width: Int, height: Int) : ClickableWidget(
    x,
    y,
    width,
    height,
    ScreenTexts.EMPTY
), ParentElement {

    //// Widget ////

    private val elements: MutableList<Element> = mutableListOf()
    private var focusedElement: Element? = null
    private var hoveredElement: Element? = null
    private var dragging = false

    override fun children(): MutableList<out Element> {
        return elements
    }

    override fun renderWidget(context: DrawContext?, mouseX: Int, mouseY: Int, delta: Float) {
        this.hoveredElement = elements.firstOrNull { it.isMouseOver(mouseX.toDouble(), mouseY.toDouble()) }
    }

    override fun isDragging(): Boolean {
        return this.dragging
    }

    override fun setDragging(dragging: Boolean) {
        this.dragging = dragging
    }

    override fun getFocused(): Element? {
        return this.focusedElement
    }

    override fun setFocused(focused: Element?) {
        if (this.focusedElement != null) {
            focusedElement!!.isFocused = false
        }

        if (focused != null) {
            focused.isFocused = true
        }

        this.focusedElement = focused
    }

    override fun getNavigationPath(navigation: GuiNavigation?): GuiNavigationPath? {
        return super<ParentElement>.getNavigationPath(navigation)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return super<ParentElement>.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return super<ParentElement>.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return super<ParentElement>.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return super<ClickableWidget>.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun isFocused(): Boolean {
        return super<ParentElement>.isFocused()
    }

    override fun setFocused(focused: Boolean) {
        super<ParentElement>.setFocused(focused)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {

    }


    abstract class Entry(private val parentElement: ParentElement, var h: Int): Element {

        private var x: Int = 0
        private var y: Int = 0
        private var w: Int = 0

        fun position(x: Int, y: Int, w: Int) {
            this.x = x
            this.y = y
            this.w = w
        }

        fun scroll(dX: Int) {
            x += dX
        }

        override fun isFocused(): Boolean {
            return this.parentElement.focused == this
        }

        override fun setFocused(focused: Boolean) {
        }

        override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
            return mouseX >= x && mouseY >= y && mouseX < (x + w) && mouseY < (y + h)
        }

        fun render (context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            renderEntry(context, x, y, w, mouseX, mouseY, delta)
            if (isFocused || this.isMouseOver(mouseX.toDouble(), mouseY.toDouble())) {
                renderBorder(context, x, y, w, mouseX, mouseY, delta)
            }
        }

        abstract fun renderEntry(context: DrawContext, x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int, delta: Float)

        open fun renderBorder(context: DrawContext, x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int, delta: Float) {}

        open fun appendHoveredNarrations(builder: NarrationMessageBuilder) {

        }

        open fun appendFocusedNarrations(builder: NarrationMessageBuilder) {

        }
    }

}