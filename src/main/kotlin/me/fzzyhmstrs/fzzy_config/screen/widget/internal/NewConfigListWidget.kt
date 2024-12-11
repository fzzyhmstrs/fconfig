package me.fzzyhmstrs.fzzy_config.screen.widget.internal

import me.fzzyhmstrs.fzzy_config.screen.LastSelectable
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowListener
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.Searcher
import me.fzzyhmstrs.fzzy_config.util.pos.ImmutableSuppliedPos
import me.fzzyhmstrs.fzzy_config.util.pos.Pos
import me.fzzyhmstrs.fzzy_config.util.pos.ReferencePos
import me.fzzyhmstrs.fzzy_config.util.pos.RelPos
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ParentElement
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.navigation.GuiNavigation
import net.minecraft.client.gui.navigation.GuiNavigation.Arrow
import net.minecraft.client.gui.navigation.GuiNavigationPath
import net.minecraft.client.gui.navigation.NavigationDirection
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.text.Text
import java.util.*
import java.util.function.Function
import java.util.function.Supplier
import java.util.function.UnaryOperator
import kotlin.math.max
import kotlin.math.min

class NewConfigListWidget(
    client: MinecraftClient,
    entryBuilders: List<Function<NewConfigListWidget, out Entry>>,
    x: Int,
    y: Int,
    width: Int,
    height: Int)
:
CustomListWidget<NewConfigListWidget.Entry>(
    client,
    x,
    y,
    width,
    height), Neighbor, LastSelectable, SuggestionWindowListener
{

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


    override fun ensureVisible(entry: Entry) {
        if (entry.top.get() < top) {
            val scrollAmount = top - entry.top.get()
            entries.scroll(scrollAmount)
        } else if (entry.bottom.get() > bottom) {
            val scrollAmount = bottom - entry.bottom.get()
            entries.scroll(scrollAmount)
        }
    }


    override fun topDelta(): Int {
        return entries.top() - top
    }

    override fun bottomDelta(): Int {
        return entries.bottom() - bottom
    }

    override fun contentHeight(): Int {
        return entries.bottom() - entries.top()
    }

    override fun entryAtY(mouseY: Int): Entry? {
        return entries.entryAtY(mouseY)
    }

    override fun handleScroll(verticalAmount: Double): Boolean {
        if (entries.isEmpty() || verticalAmount == 0.0) return false
        if (verticalAmount > 0.0) {
            val topDelta = -topDelta()
            if (topDelta == 0) return true
            val scrollDist = (verticalAmount * scrollMultiplier.get()).toInt().coerceAtLeast(1)
            val clampedDist = min(topDelta, scrollDist)
            entries.scroll(clampedDist)
        } else {
            val bottomDelta = -bottomDelta()
            if (bottomDelta >= 0) return true
            val scrollDist = (verticalAmount * scrollMultiplier.get()).toInt().coerceAtMost(-1)
            val clampedDist = max(bottomDelta, scrollDist)
            entries.scroll(clampedDist)
        }
        return true
    }

    override fun handleScrollByBar(scrollAmount: Int): Boolean {
        if (entries.isEmpty()) return false
        entries.scroll(scrollAmount)
        return true
    }

    override var lastSelected: Element? = null

    override fun pushLast() {
        lastSelected = focused
    }

    override fun popLast() {
        (lastSelected as? Entry)?.let { focused = it }
    }

    override val neighbor: EnumMap<NavigationDirection, Neighbor> = EnumMap(NavigationDirection::class.java)

    /*
        https://webaim.org/techniques/keyboard/

        Navigation strategy will be broadly based on the information provided in the link above

        https://accessibleweb.com/question-answer/navigate-website-keyboard/
        https://www.w3.org/TR/WCAG22/

        Navigation goal is to fully comply in a standardized way with the above accesibleweb information.

    */
    override fun getNavigationPath(navigation: GuiNavigation?): GuiNavigationPath? {
        if (this.entries.isEmpty()) {
            return null
        } else if (navigation !is Arrow) {
            return super<CustomListWidget>.getNavigationPath(navigation)
        } else {
            val entry: Entry? = this.focusedElement
            if (entry != null) {
                //this needs to return null if the keyboard action should "escape" the content
                val entryNavigationPath = entry.getNavigationPath(navigation)
                if (entryNavigationPath != null) {
                    return GuiNavigationPath.of(this, entryNavigationPath)
                } else {
                    val neighbor = getNeighbor(navigation.direction())
                    val neighborNavigationPath = neighbor?.getNavigationPath(navigation)
                    if (neighborNavigationPath != null) {
                        return neighborNavigationPath
                    }
                    var entry2: Entry? = entry
                    var guiNavigationPath: GuiNavigationPath?
                    do {
                        entry2 = this.entries.getNextEntry(navigation.direction(), entry2)
                        if (entry2 == null) {
                            return null
                        }
                        guiNavigationPath = entry2.getNavigationPath(navigation)
                    } while (guiNavigationPath == null)
                    return GuiNavigationPath.of(this, guiNavigationPath)
                }
            } else {
                var entry2: Entry? = entry

                var guiNavigationPath: GuiNavigationPath?
                do {
                    entry2 = this.entries.getNextEntry(navigation.direction(), entry2)
                    if (entry2 == null) {
                        return null
                    }
                    guiNavigationPath = entry2.getNavigationPath(navigation)
                } while (guiNavigationPath == null)

                return GuiNavigationPath.of(this, guiNavigationPath)
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return suggestionWindowElement?.mouseClicked(mouseX, mouseY, button) ?: super<CustomListWidget>.mouseClicked(mouseX, mouseY, button)
    }

    override fun scrollToTop(): Boolean {
        return entries.scrollToTop()
    }

    override fun scrollToBottom(): Boolean {
        return entries.scrollToBottom()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return suggestionWindowElement?.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) ?: super<CustomListWidget>.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return suggestionWindowElement?.keyPressed(keyCode, scanCode, modifiers) ?: super<CustomListWidget>.keyPressed(keyCode, scanCode, modifiers)
    }

    private var suggestionWindowElement: Element? = null

    override fun setSuggestionWindowElement(element: Element?) {
        this.suggestionWindowElement = element
    }


    //////////////////////////////

    inner class Entries(private val delegate: List<Entry>): Iterable<Entry> {

        //map <group, map <scope, entry> >
        private val delegateMap: Map<String, Map<String, Entry>>
        private val groups: Map<String, GroupPair>

        private val searcher: Searcher<Entry> = Searcher(delegate)

        init {
            var previousEntry: Entry? = null
            val pos = ReferencePos { this@NewConfigListWidget.top }
            val entryMap: MutableMap<String, MutableMap<String, Entry>> = mutableMapOf()
            val groupMap: MutableMap<String, GroupPair> = mutableMapOf()

            for (e in delegate) {
                e.onAdd(pos, previousEntry)
                if (!e.visibility.skip) {
                    entryMap.computeIfAbsent(e.group) { _ -> mutableMapOf() }[e.scope] = e
                }
                if (e.visibility.group) {
                    groupMap[e.group] = GroupPair(e, true)
                }
                previousEntry = e
            }
            delegateMap = entryMap
            groups = groupMap
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
            index2++
            inFrameEntries = if (index > index2) {
                listOf()
            } else {
                delegate.subList(index, index2).filter { it.visibility.visible }
            }
            selectableEntries = delegate.filter { it.visibility.selectable }.toMutableList()
            dirty = false
        }

        fun search(searchInput: String): Int {
            dirty = true
            val foundEntries = searcher.search(searchInput)
            for (e in delegate) {
                if (e.visibility.skip) continue
                e.applyVisibility(Visibility::filter)
            }
            for (e in foundEntries) {
                e.applyVisibility(Visibility::unfilter)
            }
            for (e in groups.values) {
                val groupEntries = delegateMap[e.group.group]?.values
                if (groupEntries == null) {
                    e.group.applyVisibility { _ -> Visibility.GROUP_FILTERED }
                    continue
                }
                e.group.applyVisibility { v -> v.group(groupEntries) }
            }
            if (this@NewConfigListWidget.focusedElement?.visibility?.selectable != true) {
                val replacement = this@NewConfigListWidget.focusedElement?.getNeighbor(true) ?: this@NewConfigListWidget.focusedElement?.getNeighbor(false)
                this@NewConfigListWidget.focused = replacement
            }
            return foundEntries.size
        }

        fun toggleGroup(g: String) {
            if (delegate.isEmpty()) return
            dirty = true
            val groupEntries = delegateMap[g] ?: return
            val groupPair = groups[g] ?: return
            if (groupPair.visible) {
                for (e in groupEntries.values) {
                    e.applyVisibility(Visibility::hide)
                }
                if (this@NewConfigListWidget.focusedElement?.visibility?.selectable != true) {
                    val replacement = this@NewConfigListWidget.focusedElement?.getNeighbor(true) ?: this@NewConfigListWidget.focusedElement?.getNeighbor(false)
                    this@NewConfigListWidget.focused = replacement
                }
                if (bottom() - top() <= this@NewConfigListWidget.height) {
                    this@NewConfigListWidget.ensureVisible(delegate.first())
                }
                groupPair.visible = false
            } else {
                for (e in groupEntries.values) {
                    e.applyVisibility(Visibility::unhide)
                }
                groupPair.visible = true
            }
        }

        fun top() = delegate.firstOrNull()?.top?.get() ?: this@NewConfigListWidget.top

        fun bottom(): Int {
            return delegate.lastOrNull()?.bottom?.get() ?: this@NewConfigListWidget.bottom
        }

        fun scrollToTop(): Boolean {
            delegate.firstOrNull()?.top?.set(0) ?: return false
            return true
        }

        fun scrollToBottom(): Boolean {
            delegate.firstOrNull()?.scroll(-this@NewConfigListWidget.bottomDelta()) ?: return false
            return true
        }

        fun scroll(amount: Int) {
            if (delegate.isEmpty()) return
            dirty = true
            delegate.first().scroll(amount)
        }

        fun entryAtY(mouseY: Int): Entry? {
            for (entry in inFrameEntries) {
                if (entry.atY(mouseY)) return entry
            }
            return null
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

        fun getNextEntry(direction: NavigationDirection, entry: Entry?): Entry? {
            return if (entry == null) {
                when (direction) {
                    NavigationDirection.UP -> delegate.lastOrNull()
                    NavigationDirection.DOWN -> delegate.firstOrNull()
                    NavigationDirection.LEFT -> delegate.firstOrNull()
                    NavigationDirection.RIGHT -> delegate.lastOrNull()
                }
            } else {
                entry.getNeighbor(!direction.isPositive)
            }
        }
    }

    private class GroupPair(val group: Entry, var visible: Boolean)

    abstract class Entry(parentElement: NewConfigListWidget, var h: Int, override val name: Text, override val desc: Text, val scope: String, val group: String = "")
        : CustomListWidget.Entry<NewConfigListWidget>(parentElement), ParentElement, Searcher.SearchContent {

        var visibility = Visibility.VISIBLE

        protected open val x: Int
            get() = parentElement.rowX()
        protected open val w: Int
            get() = parentElement.rowWidth()

        internal var top: EntryPos = EntryPos.ZERO
        internal var bottom: Pos = Pos.ZERO

        fun atY(mouseY: Int): Boolean {
            return mouseY >= top.get() && mouseY < bottom.get()
        }

        private var focusedSelectable: Selectable? = null
        private var focusedElement: Element? = null
        private var dragging = false

        override val skip: Boolean
            get() = visibility.skip

        abstract fun selectableChildren(): List<Selectable>

        fun getNeighbor(up: Boolean): Entry? {
            return this.top.getSelectableNeighbor(up)
        }

        fun onAdd(parentPos: Pos, previous: Entry?) {
            if (previous == null) {
                top = RelEntryPos(parentPos, null)
            } else {
                top = ImmutableSuppliedEntryPos(
                    previous.bottom,
                    { if (visibility.visible) verticalPadding.get() else 0 },
                    previous.top
                )
                previous.top.next = top
            }
            bottom = ImmutableSuppliedPos(top) { if (visibility.visible) h else 0 }
        }

        fun scroll(dY: Int) {
            top.inc(dY)
        }

        fun applyVisibility(operator: UnaryOperator<Visibility>) {
            this.visibility = operator.apply(this.visibility)
        }

        override fun isFocused(): Boolean {
            return this.parentElement.focused == this
        }

        override fun setFocused(focused: Boolean) {
        }

        override fun getFocused(): Element? {
            return focusedElement
        }

        override fun setFocused(focused: Element?) {
            this.focusedElement = focused
        }

        override fun isDragging(): Boolean {
            return this.dragging
        }

        override fun setDragging(dragging: Boolean) {
            this.dragging = dragging
        }

        override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
            return mouseX >= x && mouseY >= top.get() && mouseX < (x + w) && mouseY < bottom.get()
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            return super<ParentElement>.mouseClicked(mouseX, mouseY, button)
        }

        @Deprecated("Use renderEntry/renderBorder/renderHighlight for rendering instead")
        override fun render (context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            if (!visibility.visible) return
            val t = top.get()
            renderEntry(context, x, t, w, h, mouseX, mouseY, delta)
            val bl = this.isMouseOver(mouseX.toDouble(), mouseY.toDouble())
            if (isFocused || bl) {
                renderBorder(context, x, t, w, h, mouseX, mouseY, delta)
            }
            if (bl) {
                renderHighlight(context, x, t, w, h, mouseX, mouseY, delta)
            }
        }

        abstract fun renderEntry(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float)

        open fun renderBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {}

        open fun renderHighlight(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {}

        override fun getFocusedPath(): GuiNavigationPath? {
            return super<ParentElement>.getFocusedPath()
        }

        override fun getNavigationPath(navigation: GuiNavigation?): GuiNavigationPath? {
            return super<ParentElement>.getNavigationPath(navigation)
        }

        override fun appendNarrations(builder: NarrationMessageBuilder) {
            val list: List<Selectable?> = this.selectableChildren()
            val selectedElementNarrationData = Screen.findSelectedElementData(list, this.focusedSelectable)
            if (selectedElementNarrationData != null) {
                if (selectedElementNarrationData.selectType.isFocused) {
                    this.focusedSelectable = selectedElementNarrationData.selectable
                }

                if (list.size > 1) {
                    builder.put(
                        NarrationPart.POSITION,
                        FcText.translatable("narrator.position.object_list", selectedElementNarrationData.index + 1, list.size)
                    )
                    if (selectedElementNarrationData.selectType == Selectable.SelectionType.FOCUSED) {
                        builder.put(NarrationPart.USAGE, FcText.translatable("narration.component_list.usage"))
                    }
                }

                selectedElementNarrationData.selectable.appendNarrations(builder.nextMessage())
            }
        }


        interface EntryPos: Pos {
            val previous: EntryPos?
            var next: EntryPos?
            fun getEntry(): Entry?

            fun neighbor(up: Boolean): EntryPos? {
                return if (up) {
                    previous
                } else
                    next
            }

            fun getSelectableNeighbor(up: Boolean): Entry? {
                return getEntry() ?: neighbor(up)?.getSelectableNeighbor(up)
            }

            companion object {
                val ZERO = object: EntryPos {
                    override val previous: EntryPos? = null
                    override var next: EntryPos? = null
                    override fun getEntry(): Entry? { return null }
                    override fun get(): Int { return 0 }
                    override fun set(new: Int) {}
                    override fun inc(amount: Int) {}
                    override fun dec(amount: Int) {}
                }
            }
        }

        inner class RelEntryPos(parent: Pos, override val previous: EntryPos?, override var next: EntryPos? = null) : RelPos(parent), EntryPos {
            override fun getEntry(): Entry? {
                return this@Entry.takeIf { it.visibility.selectable }
            }
        }

        inner class ImmutableSuppliedEntryPos(parent: Pos, offset: Supplier<Int>, override val previous: EntryPos?, override var next: EntryPos? = null) : ImmutableSuppliedPos(parent, offset), EntryPos {
            override fun getEntry(): Entry? {
                return this@Entry.takeIf { it.visibility.selectable }
            }
        }
    }

    enum class Visibility(val visible: Boolean, val skip: Boolean, val selectable: Boolean, val group: Boolean) {
        VISIBLE(true, false, true, false),
        HIDDEN(false, false, false, false),
        FILTERED(false, false, false, false),
        FILTERED_HIDDEN(false, false, false, false),
        GROUP_VISIBLE(true, true, true, true),
        GROUP_FILTERED(false, true, false, true), //filtering handled externally
        GROUP_DISABLED(false, true, false, false), //errored group with no entries
        HEADER_VISIBLE(true, true, false, false);

        fun filter(): Visibility {
            return when (this) {
                VISIBLE -> FILTERED
                HIDDEN -> FILTERED_HIDDEN
                else -> this
            }
        }

        fun unfilter(): Visibility {
            return when (this) {
                FILTERED -> VISIBLE
                FILTERED_HIDDEN -> HIDDEN
                else -> this
            }
        }

        fun hide(): Visibility {
            return when (this) {
                FILTERED -> FILTERED_HIDDEN
                VISIBLE -> HIDDEN
                else -> this
            }
        }

        fun unhide(): Visibility {
            return when (this) {
                HIDDEN -> VISIBLE
                FILTERED_HIDDEN -> FILTERED
                else -> this
            }
        }

        fun group(groupEntries: Collection<Entry>): Visibility {
            return if (this.group) {
                if (groupEntries.any { it.visibility.visible }) {
                    GROUP_VISIBLE
                } else {
                    GROUP_FILTERED
                }
            } else {
                this
            }
        }

    }
}