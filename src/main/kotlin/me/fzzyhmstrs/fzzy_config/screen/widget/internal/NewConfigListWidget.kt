package me.fzzyhmstrs.fzzy_config.screen.widget.internal

import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.util.pos.*
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ParentElement
import java.util.function.Function
import java.util.function.Supplier
import kotlin.math.max
import kotlin.math.min

class NewConfigListWidget(private val client: MinecraftClient, entryBuilders: List<Function<NewConfigListWidget, out Entry>>, x: Int, y: Int, width: Int, height: Int) : CustomListWidget<NewConfigListWidget.Entry>(
    client,
    x,
    y,
    width,
    height
) {

    //// Widget ////

    companion object {
        private val scrollMultiplier: Supplier<Double> = Supplier { 10.0 }
        private val verticalPadding: Supplier<Int> = Supplier { 2 }
    }

    private val entries: Entries by lazy {
        Entries(entryBuilders.map { it.apply(this) })
    }

    override fun selectableEntries(): List<Entry> {
        return entries.selectableEntries()
    }

    override fun inFrameEntries(): List<Entry> {
        return entries.inFrameEntries()
    }

    private val top
        get() = y
    private val bottom
        get() = y + height


    override fun ensureVisible(entry: Entry) {
        if (entry.top.get() < top) {
            val scrollAmount = top - entry.top.get()
            entries.scroll(scrollAmount)
        } else if (entry.bottom.get() > bottom) {
            val scrollAmount = bottom - entry.bottom.get()
            entries.scroll(scrollAmount)
        }
    }

    override fun handleScroll(verticalAmount: Double): Boolean {
        if (entries.isEmpty() || verticalAmount == 0.0) return false
        if (verticalAmount > 0.0) {
            val topDelta = top - entries.top()
            if (topDelta == 0) return true
            val scrollDist = (verticalAmount * scrollMultiplier.get()).toInt().coerceAtLeast(1)
            val clampedDist = min(topDelta, scrollDist)
            entries.scroll(clampedDist)
        } else {
            val bottomDelta = bottom - entries.bottom()
            if (bottomDelta >= 0) return true
            val scrollDist = (verticalAmount * scrollMultiplier.get()).toInt().coerceAtMost(-1)
            val clampedDist = max(bottomDelta, scrollDist)
            entries.scroll(clampedDist)
        }
        return true
    }

    inner class Entries(private val delegate: List<Entry>): Iterable<Entry> {

        //map <group, map <scope, entry> >
        private val delegateMap: Map<String, Map<String, Entry>>

        init {
            var previousEntry: Entry? = null
            val pos = ReferencePos { this@NewConfigListWidget.top }
            val map: MutableMap<String, MutableMap<String, Entry>> = mutableMapOf()

            for (e in delegate) {
                e.onAdd(pos, previousEntry)
                map.computeIfAbsent(e.group) { _ -> mutableMapOf() }[e.scope] = e
                previousEntry = e
            }

            delegateMap = map
        }

        private var inFrameEntries: List<Entry> = listOf()
        private var selectableEntries: List<Entry> = listOf()
        private var dirty = true

        fun selectableEntries(): List<Entry> {
            if (delegate.isEmpty()) return listOf()
            refreshEntryLists()
            return selectableEntries
        }

        fun inFrameEntries(): List<Entry> {
            if (delegate.isEmpty()) return delegate
            refreshEntryLists()
            return inFrameEntries
        }

        private fun refreshEntryLists() {
            if (!dirty) return
            var index = 0
            while (index <= delegate.lastIndex) {
                if (delegate[index].bottom.get() > this@NewConfigListWidget.top)
                    break
                else
                    index++
            }
            var index2 = delegate.lastIndex
            while (index2 >= 0) {
                if (delegate[index2].top.get() < this@NewConfigListWidget.bottom)
                    break
                else
                    index2--
            }
            inFrameEntries = if (index > index2) {
                listOf()
            } else {
                delegate.subList(index, index2).filter { it.visibility.visible }
            }
            selectableEntries = delegate.filter { it.visibility.selectable}.toMutableList()
            dirty = false
        }

        fun top() = delegate.firstOrNull()?.top?.get() ?: this@NewConfigListWidget.top

        fun bottom(): Int {
            return delegate.lastOrNull()?.bottom?.get() ?: this@NewConfigListWidget.bottom
        }

        fun scroll(amount: Int) {
            if (delegate.isEmpty()) return
            dirty = true
            delegate.first().scroll(amount)
        }

        fun get(i: Int): Entry {
            return delegate[i]
        }

        fun isEmpty(): Boolean {
            return delegate.isEmpty()
        }

        override fun iterator(): Iterator<Entry> {
            return delegate.iterator()
        }
    }

    abstract class Entry(parentElement: ParentElement, var h: Int, val scope: String, val group: String = ""): CustomListWidget.Entry(parentElement) {

        var visibility = Visibility.VISIBLE
        protected var x: Int = 0
        protected var w: Int = 0
        internal var top: Pos = Pos.ZERO
        internal var bottom: Pos = ImmutableSuppliedPos(top) { if (visibility.visible) h else 0 }



        fun onAdd(parentPos: Pos, previous: Entry?) {
            if (previous == null) {
                top = RelPos(parentPos)
            } else {
                top = ImmutableSuppliedPos(previous.bottom) { if (visibility.visible) verticalPadding.get() else 0 }
            }
        }
        
        fun position(x: Int, w: Int) {
            this.x = x
            this.w = w
        }

        fun scroll(dY: Int) {
            top.inc(dY)
        }

        override fun isFocused(): Boolean {
            return this.parentElement.focused == this
        }

        override fun setFocused(focused: Boolean) {
        }

        override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
            return mouseX >= x && mouseY >= top.get() && mouseX < (x + w) && mouseY < bottom.get()
        }

        override fun render (context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            renderEntry(context, x, top.get(), w, mouseX, mouseY, delta)
            if (isFocused || this.isMouseOver(mouseX.toDouble(), mouseY.toDouble())) {
                renderBorder(context, x, top.get(), w, mouseX, mouseY, delta)
            }
        }
    }

    enum class Visibility(val visible: Boolean, val skip: Boolean, val selectable: Boolean) {
        HIDDEN(false, true, false),
        FILTERED(false, false, false),
        VISIBLE(true, false, true),
        GROUP_VISIBLE(true, true, true),
        GROUP_FILTERED(false, true, false),
        SUMMARY_VISIBLE(true, true, false),
        SUMMARY_FILTERED(true, true, false);

        fun reset(): Visibility {
            return when (this) {
                HIDDEN -> HIDDEN
                FILTERED -> VISIBLE
                VISIBLE -> VISIBLE
                GROUP_VISIBLE -> GROUP_VISIBLE
                GROUP_FILTERED -> GROUP_VISIBLE
                SUMMARY_VISIBLE -> SUMMARY_VISIBLE
                SUMMARY_FILTERED -> SUMMARY_FILTERED
            }
        }
    }

}
