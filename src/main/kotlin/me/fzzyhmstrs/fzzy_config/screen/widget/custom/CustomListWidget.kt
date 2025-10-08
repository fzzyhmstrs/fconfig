/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.widget.custom

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.widget.RepositioningWidget
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawTex
import me.fzzyhmstrs.fzzy_config.util.function.ConstSupplier
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ParentElement
import net.minecraft.client.gui.navigation.GuiNavigation
import net.minecraft.client.gui.navigation.GuiNavigationPath
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.screen.ScreenTexts
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.Supplier

/**
 * A custom list widget implementation with improved scrolling, positioning, and interaction mechanics
 * @param client [MinecraftClient] the client instance
 * @param x Widgets x screen position
 * @param y Widgets y screen position
 * @param width Widget width in pixels
 * @param height Widget height in pixels
 * @author fzzyhmstrs
 * @since 0.6.0, implements RepositioningWidget 0.6.3
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class CustomListWidget<E: CustomListWidget.Entry<*>>(protected val client: MinecraftClient, x: Int, y: Int, width: Int, height: Int)
    : ClickableWidget(
    x,
    y,
    width,
    height,
    ScreenTexts.EMPTY), CustomWidget, ParentElement, RepositioningWidget {

    //// Widget ////

    protected var focusedElement: E? = null
    protected var hoveredElement: E? = null
    private var dragging = false

    protected open val leftPadding: Int = 16
    protected open val rightPadding: Int = 10
    protected val scrollWidth: Int = 6
    protected val scrollButtonHeight: Int = 6
    protected val scrollType: Supplier<ScrollBarType> = ConstSupplier(ScrollBarType.DYNAMIC)
    protected val scrollFixedHeight: Int = 8
    protected val scrollButtonType: Supplier<ScrollBarButtons> = ConstSupplier(ScrollBarButtons.SPLIT)
    protected val scrollBarBackground: Identifier = "widget/scroll/vanilla/scroller_background".fcId()
    protected val scrollBar: Identifier = "widget/scroll/vanilla/scroller".fcId()
    protected val scrollBarHighlighted: Identifier = "widget/scroll/vanilla/scroller_highlighted".fcId()
    protected val scrollBarDown: Identifier = "widget/scroll/vanilla/scroll_down".fcId()
    protected val scrollBarDownHighlighted: Identifier =  "widget/scroll/vanilla/scroll_down_highlighted".fcId()
    protected val scrollBarUp: Identifier = "widget/scroll/vanilla/scroll_up".fcId()
    protected val scrollBarUpHighlighted: Identifier = "widget/scroll/vanilla/scroll_up_highlighted".fcId()


    /**
     * The content width of the row, excluding padding and space for the scroll bar
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun rowWidth(): Int {
        return width - leftPadding - rightPadding - scrollWidth
    }

    /**
     * The X position of the row content, after padding
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun rowX(): Int {

        return x + leftPadding
    }

    /**
     * @suppress
     */
    override fun setWidth(width: Int) {
        super.setWidth(width)
        onReposition()
        focusedElement?.let { ensureVisible(it) }
    }

    /**
     * @suppress
     */
    override fun setHeight(height: Int) {
        super.setHeight(height)
        onReposition()
    }

    /**
     * @suppress
     */
    override fun setPosition(x: Int, y: Int) {
        super.setPosition(x, y)
        onReposition()
        focusedElement?.let { ensureVisible(it) }
    }

    /**
     * @suppress
     */
    override fun setDimensions(width: Int, height: Int) {
        super.setDimensions(width, height)
        onReposition()
        focusedElement?.let { ensureVisible(it) }
    }

    /**
     * @suppress
     */
    override fun setDimensionsAndPosition(width: Int, height: Int, x: Int, y: Int) {
        super.setPosition(x, y)
        super.setDimensions(width, height)
        onReposition()
        focusedElement?.let { ensureVisible(it) }
    }

    /**
     * This is run when the widget is moved or resized. List elements should be repositioned when this is called.
     * @author fzzyhmstrs
     * @since 0.6.0, overrides method from RepositioningWidget 0.6.3
     */
    override fun onReposition() {}
    /**
     * List entries tht are currently selectable. Hidden or otherwise disabled entries should not be counted in this list.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    abstract fun selectableEntries(): List<E>
    /**
     * Entries that are currently visible in the list frame. These entries will be rendered as applicable. Entries partially in frame should be included.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    abstract fun inFrameEntries(): List<E>

    /**
     * @suppress
     */
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

        //context.disableScissor()
        if (!noScroll() && (!hideScrollWhileNotHovered() || this.hovered || this.isDragging)) {
            val sW = scrollWidth
            context.drawTex(scrollBarBackground, right - sW, y, sW, height)
            val pos = scrollBarPosition(mouseY.toDouble())
            val type = scrollButtonType.get()
            if (mouseX >= (right - sW) && (mouseX < right)) {
                if (pos.over) {
                    context.drawTex(scrollBarHighlighted, right - sW, pos.top, sW, pos.bot - pos.top)
                } else {
                    context.drawTex(scrollBar, right - sW, pos.top, sW, pos.bot - pos.top)
                }

                if (type.renderButtons()) {
                    if (type.mouseOverUp(mouseY.toDouble(), y, bottom)) {
                        context.drawTex(scrollBarUpHighlighted, right - sW, type.upY(y, bottom), sW, scrollButtonHeight)
                    } else {
                        context.drawTex(scrollBarUp, right - sW, type.upY(y, bottom), sW, scrollButtonHeight)
                    }
                    if (type.mouseOverDown(mouseY.toDouble(), y, bottom)) {
                        context.drawTex(scrollBarDownHighlighted, right - sW, type.downY(y, bottom), sW, scrollButtonHeight)
                    } else {
                        context.drawTex(scrollBarDown, right - sW, type.downY(y, bottom), sW, scrollButtonHeight)
                    }
                }
            } else {
                context.drawTex(scrollBar, right - sW, pos.top, sW, pos.bot - pos.top)
                if (type.renderButtons()) {
                    context.drawTex(scrollBarUp, right - sW, type.upY(y, bottom), sW, scrollButtonHeight)
                    context.drawTex(scrollBarDown, right - sW, type.downY(y, bottom), sW, scrollButtonHeight
                    )
                }
            }
        }
        context.disableScissor()
    }

    /**
     * Ensures that the provided entry is in frame. Implementations should scroll as needed to fully incorporate the entry into the widget frame.
     * @param entry [E]
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    abstract fun ensureVisible(entry: E)

    override fun getFocused(): Element? {
        return this.focusedElement
    }

    override fun setFocused(focused: Element?) {
        if (focusedElement === focused) return
        @Suppress("UNCHECKED_CAST")
        val f = focused as? E
        if (f != null && !selectableEntries().contains(f)) return
        focusedElement?.isFocused = false
        f?.isFocused = true
        this.focusedElement = f
        if (f != null && client.navigationType.isKeyboard) {
            ensureVisible(f)
        }
    }

    @Internal
    override fun isDragging(): Boolean {
        return this.dragging
    }

    @Internal
    override fun setDragging(dragging: Boolean) {
        this.dragging = dragging
    }

    /**
     * Return a navigation path leading through this list and to an entry, as applicable. The default implementation will likely not function properly; see [DynamicListWidget][me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget] for an example implementation
     * @param navigation [GuiNavigation], nullable. The navigation type and direction.
     * @return [GuiNavigationPath], nullable. Return a navigation path through this widget to the entry that needs to be focused, or null if the navigation won't land on the widget.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    override fun getNavigationPath(navigation: GuiNavigation?): GuiNavigationPath? {
        return super<ParentElement>.getNavigationPath(navigation)
    }

    /**
     * The length in pixels that the top of the list is out of frame above the top of the widget.
     *
     * For example, if five 20 pixel entries are scrolled above the top border of the widget, the top delta will be -100
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    abstract fun topDelta(): Int

    /**
     * The length in pixels that the bottom of the list is out of frame below the bottom of the widget.
     *
     * If the bottom of the entries are at y: 1000 and the widget bottom is y: 100, bottom delta = 900
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    abstract fun bottomDelta(): Int

    /**
     * The height of the entries in the list. If the list contains twenty 20px entries, content height is 400
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    abstract fun contentHeight(): Int

    /**
     * The entry underneath the mouse, if any.
     * @param mouseY: Int
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    abstract fun entryAtY(mouseY: Int): E?

    /**
     * Defines mouse buttons that are valid selection buttons. Default is either left or right click
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    protected open fun isSelectButton(button: Int): Boolean {
        return button == 0 || button == 1
    }

    /**
     * If this widget doesn't need scrolling. Will return true if the current [contentHeight] is smaller or equal to the widget [height]
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    protected fun noScroll(): Boolean {
        return contentHeight() <= height
    }

    open fun listNarrationKey(): String {
        return "fc.narrator.position.list"
    }

    /**
     * Whether the scroll bar should not render if the widget isn't hovered. Default false.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    open fun hideScrollWhileNotHovered(): Boolean {
        return false
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
                val sH = scrollHeight()
                val cH = contentHeight()
                val contentFraction = (sH.toDouble() / cH.toDouble())
                val topGap = -topDelta() * contentFraction
                val bottomGap = bottomDelta() * contentFraction
                if ((mouseY >= scrollTop() + topGap) && (mouseY < scrollBottom() - bottomGap)) {
                    val upwardTravel = topDelta() * contentFraction
                    val downwardTravel = bottomDelta() * contentFraction
                    scrollingTop = scrollingY + upwardTravel
                    scrollingBottom = scrollingY + downwardTravel
                } else {
                    scrollingY = -1.0
                }

            } else {
                val progress = topDelta().toDouble() / (topDelta() - bottomDelta()).toDouble()
                val halfBarHeight = scrollFixedHeight / 2
                val midPoint = MathHelper.lerp(progress.toFloat(), scrollTop() + halfBarHeight, scrollBottom() - halfBarHeight)
                if ((mouseY >= midPoint - halfBarHeight) && (mouseY < midPoint + halfBarHeight)) {
                    scrollingTop = (scrollTop() + halfBarHeight).toDouble()
                    scrollingBottom = (scrollBottom() + halfBarHeight).toDouble()
                } else {
                    scrollingY = -1.0
                }
            }
        }
    }

    private fun jumpScrollBarToMouse(mouseY: Double, button: Int): Int {
        if (noScroll()) return 0
        if (button != 0) return 0
        if (mouseY < scrollTop() || mouseY > scrollBottom()) return 0
        when (scrollType.get()) {
            ScrollBarType.DYNAMIC -> {
                val contentFraction = (scrollHeight().toDouble() / contentHeight().toDouble())
                val topGap = topDelta() * contentFraction
                val bottomGap = bottomDelta() * contentFraction
                if ((mouseY >= scrollTop() - topGap) && (mouseY < scrollBottom() - bottomGap)) {
                    return 0
                }
                val progress = topDelta().toDouble() / (topDelta() - bottomDelta()).toDouble()
                val halfBarHeight = (height * contentFraction).toInt() / 2
                val midPoint = MathHelper.lerp(progress.toFloat(), scrollTop() + halfBarHeight, scrollBottom() - halfBarHeight)
                return ((midPoint - mouseY)/contentFraction).toInt()
            }
            ScrollBarType.FIXED -> {
                val progress = topDelta().toDouble() / (topDelta() - bottomDelta()).toDouble()
                val halfBarHeight = scrollFixedHeight / 2
                val midPoint = MathHelper.lerp(progress.toFloat(), scrollTop() + halfBarHeight, scrollBottom() - halfBarHeight)
                val contentFraction = (scrollHeight().toDouble() / contentHeight().toDouble())
                return ((midPoint - mouseY)/contentFraction).toInt()
            }
        }
    }

    private fun scrollBarPosition(mouseY: Double): ScrollBarPosition {
        return when (scrollType.get()) {
            ScrollBarType.DYNAMIC -> {
                val contentFraction = (scrollHeight().toDouble() / contentHeight().toDouble())
                val topGap = -topDelta() * contentFraction
                val bottomGap = bottomDelta() * contentFraction
                ScrollBarPosition((scrollTop() + topGap).toInt(),  (scrollBottom() - bottomGap).toInt(), (mouseY >= scrollTop() + topGap) && (mouseY < scrollBottom() - bottomGap))
            }
            ScrollBarType.FIXED -> {
                val progress = topDelta().toDouble() / (topDelta() - bottomDelta()).toDouble()
                val halfBarHeight = scrollFixedHeight / 2
                val midPoint = MathHelper.lerp(progress.toFloat(), scrollTop() + halfBarHeight, scrollBottom() - halfBarHeight)
                ScrollBarPosition(midPoint - halfBarHeight, midPoint + halfBarHeight, (mouseY >= midPoint - halfBarHeight) && (mouseY < midPoint + halfBarHeight))
            }
        }
    }

    override fun onMouse(event: CustomWidget.MouseEvent): Boolean {
        if (!this.isSelectButton(event.button())) {
            return false
        }
        if (!isMouseOver(event.x(), event.y())) {
            return false
        }
        if (event.x() >= (right - scrollWidth) && (event.x() < right)) {
            dragging = true
            if (scrollButtonType.get().mouseOverUp(event.y(), y, bottom)) {
                client.soundManager.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                return handleScrollByBar(50)
            } else if (scrollButtonType.get().mouseOverDown(event.y(), y, bottom)) {
                client.soundManager.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f))
                return handleScrollByBar(-50)
            } else {
                val jump = jumpScrollBarToMouse(event.y(), event.button())
                if (jump != 0) {
                    handleScrollByBar(jump)
                }
                updateScrollingState(event.y(), event.button())
                return true
            }
        } else {
            scrollingY = -1.0
        }

        val e = entryAtY(event.y().toInt())
        val e2 = focused
        if (e != null) {
            focused = e
            if (event.clickWidget(e)) {
                if (e2 != e && e2 is ParentElement) {
                    e2.focused = null
                }
                dragging = true
                return true
            } else {
                focused = e2
            }
        }

        return this.scrollingY >= 0.0
    }

    @Deprecated("Will be marked final 0.8.0. Use onMouse instead")
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return onMouse(CustomWidget.OnClick(mouseX, mouseY, button))
    }

    override fun onMouseRelease(event: CustomWidget.MouseEvent): Boolean {
        dragging = false
        return event.releaseWidgetOrNull(focused) == true
    }

    @Deprecated("Will be marked final 0.8.0. Use onMouseRelease instead")
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return onMouseRelease(CustomWidget.OnRelease(mouseX, mouseY, button))
    }

    override fun onMouseDrag(event: CustomWidget.MouseEvent): Boolean {
         if (event.button() == 0 && this.scrollingY >= 0.0) {
            val mouseDelta = event.y() - scrollingY
            return if (event.y() < y.toDouble()) {
                this.scrollToTop()
            } else if (event.y() > this.bottom.toDouble()) {
                this.scrollToBottom()
            } else if(topDelta() >= 0 && mouseDelta < 0.0) {
                scrollingY = event.y()
                return true
            } else if(bottomDelta() <= 0 && mouseDelta > 0.0) {
                scrollingY = event.y()
                return true
            } else {
                val travelProgress = MathHelper.getLerpProgress(event.y(), scrollingTop, scrollingBottom)
                if (travelProgress < 0.0) {
                    scrollToTop()
                    scrollingBottom = scrollingBottom - scrollingTop + event.y()
                    scrollingTop = event.y()
                } else if (travelProgress > 1.0) {
                    scrollToBottom()
                    scrollingTop = event.y() - (scrollingBottom - scrollingTop)
                    scrollingBottom = event.y()
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

    @Deprecated("Will be marked final 0.8.0. Use onMouseDrag instead")
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (super<ParentElement>.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            return true
        }
        return onMouseDrag(CustomWidget.OnDrag(mouseX, mouseY, button, deltaX, deltaY))
    }

    /**
     * "Pages" the list up or down as if Page Up/Down has been pressed.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun page(up: Boolean) {
        if (up) {
            handleScrollByBar(scrollHeight())
        } else {
            handleScrollByBar(-scrollHeight())
        }
    }

    /**
     * Scroll to the top of the list content. [topDelta] should be 0 after this scroll.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    abstract fun scrollToTop(): Boolean

    /**
     * Scroll to the bottom of the list content. [bottomDelta] should be 0 or negative after this scroll
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    abstract fun scrollToBottom(): Boolean

    /**
     * Handles a scroll input. This is scroll from a mouse, so the input should be multiplied by a factor to make scroll distance reasonable.
     * @param verticalAmount Double scroll amount from the mouse input.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    abstract fun handleScroll(verticalAmount: Double): Boolean

    /**
     * Handles "direct" scroll input. This amount is not scaled, so the amount input should be the amount scrolled.
     * @param scrollAmount Int scroll amount from a widget-based input (such as the scroll bar).
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    abstract fun handleScrollByBar(scrollAmount: Int): Boolean

    override fun onMouseScroll(event: CustomWidget.MouseEvent): Boolean {
        return false
    }

    @Deprecated("Will be marked final 0.8.0. Use onMouseScroll instead.")
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val event = CustomWidget.OnScroll(mouseX, mouseY, horizontalAmount, verticalAmount)
        if (onMouseScroll(event)) return true
        return handleScroll(verticalAmount)
    }

    override fun onKey(event: CustomWidget.KeyEvent): Boolean {
        return false
    }

    @Deprecated("Will be marked final 0.8.0. Use onKey instead.")
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (onKey(CustomWidget.KeyEvent(keyCode, scanCode, modifiers))) return true
        return super<ParentElement>.keyPressed(keyCode, scanCode, modifiers)
    }

    @Deprecated("Will be marked final 0.8.0. Use onKeyRelease instead.")
    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (onKeyRelease(CustomWidget.KeyEvent(keyCode, scanCode, modifiers))) return true
        return super<ClickableWidget>.keyReleased(keyCode, scanCode, modifiers)
    }

    @Deprecated("Will be marked final 0.8.0. Use onChar instead.")
    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        if (onChar(CustomWidget.CharEvent(chr, modifiers))) return true
        return super<ParentElement>.charTyped(chr, modifiers)
    }

    override fun isFocused(): Boolean {
        return super<ParentElement>.isFocused()
    }

    override fun setFocused(focused: Boolean) {
        if (!focused) {
            this.focusedElement?.isFocused = false
            this.focusedElement = null
        }
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        if (client.navigationType.isKeyboard) {
            focusedElement?.appendNarrations(builder.nextMessage())
            val i = selectableEntries().indexOf(focusedElement)
            if (i >= 0) {
                builder.put(NarrationPart.POSITION, FcText.translatable(listNarrationKey(), i + 1, selectableEntries().size))
            }
        } else {
            hoveredElement?.appendNarrations(builder.nextMessage().nextMessage())
            val i = selectableEntries().indexOf(hoveredElement)
            if (i >= 0) {
                builder.put(NarrationPart.POSITION, FcText.translatable(listNarrationKey(), i + 1, selectableEntries().size))
            }
        }

        builder.put(NarrationPart.USAGE, FcText.translatable("narration.component_list.usage"))
    }

    /**
     * The visual style of the scroll bar itself
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    protected enum class ScrollBarType {
        /**
         * The scroll bar changes height based on the amount of scroll available.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        DYNAMIC,
        /**
         * The scroll bar is a set height no matter the scroll amount.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        FIXED
    }

    protected enum class ScrollBarButtons {
        NONE {
            override fun renderButtons(): Boolean {
                return false
            }
            override fun upY(top: Int, bottom: Int): Int {
                return 0
            }

            override fun downY(top: Int, bottom: Int): Int {
                return 0
            }

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
            override fun renderButtons(): Boolean {
                return true
            }
            override fun upY(top: Int, bottom: Int): Int {
                return top
            }

            override fun downY(top: Int, bottom: Int): Int {
                return top + 6
            }

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
            override fun renderButtons(): Boolean {
                return true
            }
            override fun upY(top: Int, bottom: Int): Int {
                return bottom - 12
            }

            override fun downY(top: Int, bottom: Int): Int {
                return bottom - 6
            }

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
            override fun renderButtons(): Boolean {
                return true
            }

            override fun upY(top: Int, bottom: Int): Int {
                return top
            }

            override fun downY(top: Int, bottom: Int): Int {
                return bottom - 6
            }

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

        abstract fun renderButtons(): Boolean
        abstract fun upY(top: Int, bottom: Int): Int
        abstract fun downY(top: Int, bottom: Int): Int
        abstract fun scrollTop(top: Int): Int
        abstract fun scrollBottom(bottom: Int): Int
        abstract fun mouseOverUp(mouseY: Double, top: Int, bottom: Int): Boolean
        abstract fun mouseOverDown(mouseY: Double, top: Int, bottom: Int): Boolean
    }

    private class ScrollBarPosition(val top: Int, val bot: Int, val over: Boolean)

    /**
     * A list entry. This is responsible for managing its own position, rendering of the list row, management of any children widgets, providing correct navigation, narration, and so on.
     *
     * In particular, the [CustomListWidget] does not provide any mechanism to manage individual entry positions itself. Positions of entries must either be managed by list implementations, or in the entry itself. See [DynamicListWidget][me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget] for a (very in-depth) implementation example.
     * @param parentElement [P] a parent to attach to this entry. This is an indirect way of making this entry "inner", without actually doing that.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    abstract class Entry<P: ParentElement>(val parentElement: P): Element {

        override fun isFocused(): Boolean {
            return this.parentElement.focused == this
        }

        override fun setFocused(focused: Boolean) {
        }

        /**
         * Renders this entry. Works exactly as one would expect for any other render call.
         * @param context [DrawContext]
         * @param mouseX horizontal screen position of the mouse
         * @param mouseY vertical screen position of the mouse
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        abstract fun render (context: DrawContext, mouseX: Int, mouseY: Int, delta: Float)

        /**
         * Append narration messages to current [NarrationMessageBuilder]. The list will handle list position and navigation, the entry should focus on providing title and description information, and internal navigation information as applicable (if there are multiple children in one entry for example)
         * @param builder [NarrationMessageBuilder]
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        open fun appendNarrations(builder: NarrationMessageBuilder) {
        }
    }
}