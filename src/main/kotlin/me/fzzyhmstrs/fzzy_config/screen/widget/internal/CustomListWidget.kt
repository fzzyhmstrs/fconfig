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
import net.minecraft.text.Text
import net.minecraft.util.math.MathHelper
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

    protected val leftPadding: Supplier<Int> = Supplier { 16 }
    protected val rightPadding: Supplier<Int> = Supplier { 10 }
    protected val scrollWidth: Supplier<Int> = Supplier { 6 }
    protected val scrollType: Supplier<ScrollBarType> = Supplier { ScrollBarType.DYNAMIC }
    protected val scrollFixedHeight: Supplier<Int> = Supplier { 8 }
    protected val scrollButtonType: Supplier<ScrollBarButtons> = Supplier { ScrollBarButtons.SPLIT }
    protected val scrollClickMoveAmount: Supplier<Int> = Supplier { 50 }

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

    abstract fun contentHeight(): Int

    abstract fun entryAtY(mouseY: Int): E?

    protected open fun isSelectButton(button: Int): Boolean {
        return button == 0 || button == 1
    }

    private fun noScroll(): Boolean {
        return contentHeight() <= height
    }

    private var scrollingY = -1.0
    private var scrollingTop = -1.0
    private var scrollingBottom = -1.0

    private fun scrollHeight(): Int {
        return scrollButtonType.get().scrollBottom(bottom) - scrollButtonType.get().scrollTop(y)
    }

    private fun scrollTop(): Int {
        return scrollButtonType.get().scrollTop(y)
    }

    private fun scrollBottom(): Int {
        return scrollButtonType.get().scrollBottom(bottom)
    }

    private fun updateScrollingState(mouseY: Double, button: Int) {
        if (noScroll()) return
        if (button != 0) return
        this.scrollingY = mouseY
        if (scrollingY > 0.0) {
            if (scrollType.get() == ScrollBarType.DYNAMIC) {
                val contentFraction = (scrollHeight() / contentHeight()).toDouble()
                val upwardTravel = topDelta() * contentFraction
                val downwardTravel = bottomDelta() * contentFraction
                scrollingTop = scrollingY + upwardTravel
                scrollingBottom = scrollingY + downwardTravel
            } else {
                val halfScrollBarHeight = scrollFixedHeight.get() / 2
                scrollingTop = (scrollTop() + halfScrollBarHeight).toDouble()
                scrollingBottom = (scrollBottom() + halfScrollBarHeight).toDouble()
            }
        }
    }

    private fun jumpScrollBarToMouse(mouseY: Double, button: Int): Int {
        if (noScroll()) return 0
        if (button != 0) return 0
        if (mouseY < scrollTop() || mouseY > scrollBottom()) return 0
        when (scrollType.get()) {
            ScrollBarType.DYNAMIC -> {
                val contentFraction = (scrollHeight() / contentHeight()).toDouble()
                val topGap = topDelta() * contentFraction
                val bottomGap = bottomDelta() * contentFraction
                if ((mouseY >= scrollTop() - topGap) && (mouseY < scrollBottom() - bottomGap)) {
                    return 0
                }
                val progress = topDelta() / (topDelta() - bottomDelta()).toDouble()
                val halfBarHeight = (height * contentFraction).toInt() / 2
                val midPoint = MathHelper.lerp(progress.toFloat(), scrollTop() + halfBarHeight, scrollBottom() - halfBarHeight)
                return ((midPoint - mouseY)/contentFraction).toInt()
            }
            ScrollBarType.FIXED -> {
                val progress = topDelta() / (topDelta() - bottomDelta()).toDouble()
                val halfBarHeight = scrollFixedHeight.get() / 2
                val midPoint = MathHelper.lerp(progress.toFloat(), scrollTop() + halfBarHeight, scrollBottom() - halfBarHeight)
                val contentFraction = (scrollHeight() / contentHeight()).toDouble()
                return ((midPoint - mouseY)/contentFraction).toInt()
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!this.isSelectButton(button)) {
            return false
        }
        if (!isMouseOver(mouseX, mouseY)) {
            return false
        }
        if (mouseX > (right - scrollWidth.get()) && (mouseX < right)) {
            if (scrollButtonType.get().mouseOverUp(mouseY, y, bottom)) {
                return handleScrollByBar(50)
            } else if (scrollButtonType.get().mouseOverDown(mouseY, y, bottom)) {
                return handleScrollByBar(-50)
            } else {
                val jump = jumpScrollBarToMouse(mouseY, button)
                if (jump != 0) {
                    handleScrollByBar(jump)
                }
                updateScrollingState(mouseY, button)
                return true
            }
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
        return this.scrollingY >= 0.0
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return focused?.mouseReleased(mouseX, mouseY, button) == true
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (super<ParentElement>.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            return true
        } else if (button == 0 && this.scrollingY >= 0.0) {
            val mouseDelta = mouseY - scrollingY
            return if (mouseY < y.toDouble()) {
                this.scrollToTop()
            } else if (mouseY > this.bottom.toDouble()) {
                this.scrollToBottom()
            } else if(topDelta() >= 0 && mouseDelta < 0.0) {
                scrollingY = mouseY
                return true
            } else if(bottomDelta() <= 0 && mouseDelta > 0.0) {
                scrollingY = mouseY
                return true
            } else {
                val travelProgress = MathHelper.getLerpProgress(mouseY, scrollingTop, scrollingBottom)
                if (travelProgress < 0.0) {
                    scrollToTop()
                    scrollingBottom = scrollingBottom - scrollingTop + mouseY
                    scrollingTop = mouseY
                } else if (travelProgress > 1.0) {
                    scrollToBottom()
                    scrollingTop = mouseY - (scrollingBottom - scrollingTop)
                    scrollingBottom = mouseY
                } else {
                    val totalDelta = contentHeight() - height
                    val newTopDeltaAmount = (-1 * (totalDelta * travelProgress)).toInt()
                    val scrollToDo = newTopDeltaAmount - topDelta()
                    return handleScrollByBar(scrollToDo)
                }
                return true
            }
        }
        return false
    }

    abstract fun scrollToTop(): Boolean

    abstract fun scrollToBottom(): Boolean

    abstract fun handleScroll(verticalAmount: Double): Boolean

    abstract fun handleScrollByBar(scrollAmount: Int): Boolean

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
                builder.nextMessage().put(NarrationPart.POSITION, FcText.translatable("fc.narrator.position.list.focused", i + 1, selectableEntries().size))
            }
        } else {
            hoveredElement?.appendNarrations(builder.nextMessage().nextMessage())
            val i = selectableEntries().indexOf(hoveredElement)
            val j = selectableEntries().indexOf(focusedElement)
            if (i > 0) {
                builder.nextMessage().put(NarrationPart.POSITION, FcText.translatable("fc.narrator.position.list.hovered", i + 1, selectableEntries().size))
            }
            if (j > 0 && i != j) {
                builder.put(NarrationPart.POSITION, FcText.translatable("fc.narrator.position.list.focused", j + 1, selectableEntries().size))
            }
        }

        builder.put(NarrationPart.USAGE, FcText.translatable("narration.component_list.usage"))
    }

    protected enum class ScrollBarType {
        DYNAMIC,
        FIXED
    }

    protected enum class ScrollBarButtons {
        NONE {
            override fun scrollTop(top: Int): Int {
                return top
            }

            override fun scrollBottom(bottom: Int): Int {
                return bottom
            }

            override fun mouseOverUp(mouseY: Double, top: Int, bottom: Int): Boolean {
                return false
            }

            override fun mouseOverDown(mouseY: Double, top: Int, bottom: Int): Boolean {
                return false
            }
        },
        TOP {
            override fun scrollTop(top: Int): Int {
                return top + 12
            }

            override fun scrollBottom(bottom: Int): Int {
                return bottom
            }

            override fun mouseOverUp(mouseY: Double, top: Int, bottom: Int): Boolean {
                return mouseY < (top + 6) && mouseY >= top
            }

            override fun mouseOverDown(mouseY: Double, top: Int, bottom: Int): Boolean {
                return mouseY < scrollTop(top) && mouseY >= (top + 6)
            }
        },
        BOTTOM {
            override fun scrollTop(top: Int): Int {
                return top
            }

            override fun scrollBottom(bottom: Int): Int {
                return bottom - 12
            }

            override fun mouseOverUp(mouseY: Double, top: Int, bottom: Int): Boolean {
                return mouseY < (bottom - 6) && mouseY >= scrollBottom(bottom)
            }

            override fun mouseOverDown(mouseY: Double, top: Int, bottom: Int): Boolean {
                return mouseY < bottom && mouseY >= (bottom - 6)
            }
        },
        SPLIT {
            override fun scrollTop(top: Int): Int {
                return top + 6
            }

            override fun scrollBottom(bottom: Int): Int {
                return bottom - 6
            }

            override fun mouseOverUp(mouseY: Double, top: Int, bottom: Int): Boolean {
                return mouseY < scrollTop(top) && mouseY >= top
            }

            override fun mouseOverDown(mouseY: Double, top: Int, bottom: Int): Boolean {
                return mouseY < bottom && mouseY >= scrollBottom(bottom)
            }
        };

        abstract fun scrollTop(top: Int): Int
        abstract fun scrollBottom(bottom: Int): Int
        abstract fun mouseOverUp(mouseY: Double, top: Int, bottom: Int): Boolean
        abstract fun mouseOverDown(mouseY: Double, top: Int, bottom: Int): Boolean
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
