package me.fzzyhmstrs.fzzy_config.screen.widget.internal

import me.fzzyhmstrs.fzzy_config.util.pos.*
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

    companion object {
        private val scrollMultiplier: Supplier<Double> = SUpplier { 10.0 }
        private val verticalPadding: Supplier<Int> = Supplier { 2 }
    }

    private val entries: Entries = Entries()
    private var focusedElement: Element? = null
    private var hoveredElement: Element? = null
    private var dragging = false
    private var scrollAmount = 0.0

    override fun children(): MutableList<out Element> {
        return entries
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
        if (entries.isEmpty())
        if (scrollAmount = 0.0 && verticalAmount < 0.0) return true
        val scrollDistance = verticalAmount * scrollMultiplier.get()
        
        return true
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

    fun insertEntry(insertion: UnaryOperator<Entries>) {
        
    }

    inner class Entries(): Iterable<Entry> {

        private val delegate: MutableList<Entry> = mutableListOf()
        //map <group, map <scope, entry> >
        private val delegateMap: MutableMap<String, MutableMap<String, Entry>> = mutableMapOf()

        fun get(index: Int): Entry {
            return delegate.get(index)
        }

        override fun iterator(): Iterator<Entry> {
            return delegate.iterator()
        }

        fun add(element: Entry) {
            add(delegate.lastIndex + 1, element)
        }
        
        fun add(index: Int, element: Entry) {
            previousIndex = index - 1
            nextIndex = index + 1
            val previous = if(previousIndex < 0 || previous > delegate.lastIndex) {
                null
            } else {
                delegate[previousIndex]
            }
            val next = if(nextIndex < 0 || nextIndex > delegate.lastIndex) {
                null
            } else {
                delegate[nextIndex]
            }
            element.onAdd(this@CustomListWidget.scrollAmount, previous, next)
            delegate.add(index, element)
        }

        fun remove(entry: Entry)
        
        fun removeAt(index: Int) {
            previousIndex = index - 1
            nextIndex = index + 1
            val previous = if(previousIndex < 0) {
                null
            } else {
                delegate[previousIndex]
            }
            val next = if(nextIndex > delegate.lastIndex) {
                null
            } else {
                delegate[nextIndex]
            }
            element.onRemove(previous, next)
            val removed = delegate.removeAt(index)
            delegateMap.remove(removed.
        }
        
    }

    abstract class Entry(private val parentElement: CustomListWidget, var h: Int, scope: String, group: String = ""): Element {

        val id: Id = Id(scope, group)
        protected var x: Int = 0
        protected var top: Pos = Pos.ZERO
        protected var bottom: Pos = ImmutableSuppliedPos(top) { if (visible) h else 0 }
        protected var w: Int = 0
        var visible = true

        fun onAdd(scrollAmount: Int, previous: Entry?, next: Entry?) {
            if (previous == null) {
                top = AbsPos(scrollAmount)
            } else {
                top = ImmutableSuppliedPos(previous.bottom) { if (visible) verticalPadding.get() else 0 }   
            }
            if (next != null) {
                next.top = ImmutableSuppliedPos(bottom) { if (next.visible) verticalPadding.get() else 0 }
            }
        }

        fun onRemove(previous: Entry?, next: Entry?) {
            if (previous != null && next != null) {
                next.top = ImmutableSuppliedPos(previous.bottom) { if (next.visible) verticalPadding.get() else 0 }
            } else if (next != null) {
                next.top = top
            }
        }
        
        fun position(x: Int, w: Int) {
            this.x = x
            this.w = w
        }

        fun scroll(dY: Int) {
            top.inc(dY)
        }

        fun bottom(): Int

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

        class Id(val scope: String, val group: String) {
            override fun equals(other: Any?): Boolean {
                if (other !is Id) return false
                return other.scope == this.scope && other.group == this.group
            }
            override fun hashcode(): Int {
                return scope.hashcode() + (31 * group.hashcode())
            }
        }
    }

}
