package me.fzzyhmstrs.fzzy_config.screen.widget.internal

import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ParentElement
import net.minecraft.client.gui.navigation.GuiNavigation
import net.minecraft.client.gui.navigation.GuiNavigationPath
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.screen.ScreenTexts
import java.util.function.Supplier

abstract class CustomListWidget<E: CustomListWidget.Entry<*>>(private val client: MinecraftClient, x: Int, y: Int, width: Int, height: Int) : ClickableWidget(
    x,
    y,
    width,
    height,
    ScreenTexts.EMPTY
), ParentElement {

    //// Widget ////

    protected var focusedElement: E? = null
    protected var hoveredElement: E? = null
    private var dragging = false
    private var scrolling = false

    protected val leftPadding: Supplier<Int> = Supplier { 16 }
    protected val rightPadding: Supplier<Int> = Supplier { 10 }
    protected val scrollWidth: Supplier<Int> = Supplier { 6 }

    fun rowWidth(): Int {
        return width - leftPadding.get() - rightPadding.get() - scrollWidth.get()
    }

    fun rowX(): Int {

        return x + leftPadding.get()
    }

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
            entry.render(context, mouseX, mouseY, delta)
        }
        context.disableScissor()
    }

    abstract fun ensureVisible(entry: E)

    override fun getFocused(): Element? {
        return this.focusedElement
    }

    override fun setFocused(focused: Element?) {
        val f = focused as? E
        if (f != null && !selectableEntries().contains(f)) return
        focusedElement?.isFocused = false
        f?.isFocused = true
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

    abstract fun topDelta(): Int

    abstract fun bottomDelta(): Int

    abstract fun entryAtY(mouseY: Int): E?

    protected open fun isSelectButton(button: Int): Boolean {
        return button == 0 || button == 1
    }

    private fun noScroll(): Boolean {
        return topDelta() >= 0 && bottomDelta() <= 0
    }

    private fun updateScrollingState(mouseX: Double, mouseY: Double, button: Int) {
        if (noScroll()) return
        if (button != 0) return
        this.scrolling = mouseX > (right - scrollWidth.get()) && (mouseY < right)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!this.isSelectButton(button)) {
            return false
        }
        updateScrollingState(mouseX, mouseY, button)
        if (!isMouseOver(mouseX, mouseY)) {
            return false
        }
        val e = entryAtY(mouseY.toInt())
        if (e != null && e.mouseClicked(mouseX, mouseY, button)) {
            val e2 = focused
            if (e2 != e && e2 is ParentElement) {
                e2.focused = null
            }
            focused = e
            dragging = true
            return true
        }
        return this.scrolling
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
        if (client.navigationType.isKeyboard) {
            focusedElement?.appendNarrations(builder.nextMessage())
            val i = selectableEntries().indexOf(focusedElement)
            if (i > 0) {

            }
        } else {
            hoveredElement?.appendNarrations(builder.nextMessage())
            val i = selectableEntries().indexOf(hoveredElement)
            if (i > 0) {

            }
        }

        builder.put(NarrationPart.USAGE, FcText.translatable("narration.component_list.usage"))
    }

    abstract class Entry<P: ParentElement>(val parentElement: P): Element {

        override fun isFocused(): Boolean {
            return this.parentElement.focused == this
        }

        override fun setFocused(focused: Boolean) {
        }

        abstract fun render (context: DrawContext, mouseX: Int, mouseY: Int, delta: Float)

        open fun appendNarrations(builder: NarrationMessageBuilder) {

        }
    }
}
