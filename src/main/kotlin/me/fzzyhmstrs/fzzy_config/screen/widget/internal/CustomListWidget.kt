package me.fzzyhmstrs.fzzy_config.screen.widget.internal

import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.NewConfigListWidget.Entry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ParentElement
import net.minecraft.client.gui.navigation.GuiNavigation
import net.minecraft.client.gui.navigation.GuiNavigationPath
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.screen.ScreenTexts

abstract class CustomListWidget<E: Entry>(private val client: MinecraftClient, x: Int, y: Int, width: Int, height: Int) : ClickableWidget(
    x,
    y,
    width,
    height,
    ScreenTexts.EMPTY
), ParentElement {

    //// Widget ////

    private var focusedElement: E? = null
    private var hoveredElement: E? = null
    private var dragging = false

    abstract fun selectableEntries(): List<E>

    abstract fun inFrameEntries(): List<E>

    override fun children(): List<Element> {
        return selectableEntries()
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        this.hoveredElement = if (isMouseOver(mouseX.toDouble(), mouseY.toDouble()))
            inFrameEntries().firstOrNull { it.isMouseOver(mouseX.toDouble(), mouseY.toDouble()) }
        else
            null
        context.enableScissor(this.x, this.y, this.right, this.bottom)
        for (entry in inFrameEntries()) {

        }
        context.disableScissor()
    }

    abstract fun ensureVisible(entry: E)

    override fun getFocused(): Element? {
        return this.focusedElement
    }

    override fun setFocused(focused: Element?) {
        if (this.focusedElement != null) {
            focusedElement?.isFocused = false
        }
        if (focused != null) {
            focused.isFocused = true
        }
        val f = focused as? E
        this.focusedElement = f
        if (f != null && client.navigationType.isKeyboard) {
            ensureVisible(f)
        }
    }

    override fun isDragging(): Boolean {
        return this.dragging
    }

    override fun setDragging(dragging: Boolean) {
        this.dragging = dragging
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

    abstract fun handleScroll(verticalAmount: Double): Boolean

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return handleScroll(verticalAmount)
    }

    override fun isFocused(): Boolean {
        return super<ParentElement>.isFocused()
    }

    override fun setFocused(focused: Boolean) {
        super<ParentElement>.setFocused(focused)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        hoveredElement?.appendHoveredNarrations(builder.nextMessage())
        focusedElement?.appendFocusedNarrations(builder.nextMessage())

    }

    abstract class Entry(val parentElement: ParentElement): Element {

        override fun isFocused(): Boolean {
            return this.parentElement.focused == this
        }

        override fun setFocused(focused: Boolean) {
        }

        abstract fun render (context: DrawContext, mouseX: Int, mouseY: Int, delta: Float)

        abstract fun renderEntry(context: DrawContext, x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int, delta: Float)

        open fun renderBorder(context: DrawContext, x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int, delta: Float) {}

        open fun appendHoveredNarrations(builder: NarrationMessageBuilder) {

        }

        open fun appendFocusedNarrations(builder: NarrationMessageBuilder) {

        }
    }
}
