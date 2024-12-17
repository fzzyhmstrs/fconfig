/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.FC
import me.fzzyhmstrs.fzzy_config.screen.LastSelectable
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowListener
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.Neighbor
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.Searcher
import me.fzzyhmstrs.fzzy_config.util.pos.ImmutableSuppliedPos
import me.fzzyhmstrs.fzzy_config.util.pos.Pos
import me.fzzyhmstrs.fzzy_config.util.pos.ReferencePos
import me.fzzyhmstrs.fzzy_config.util.pos.RelPos
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.*
import net.minecraft.client.gui.navigation.GuiNavigation
import net.minecraft.client.gui.navigation.GuiNavigation.Arrow
import net.minecraft.client.gui.navigation.GuiNavigationPath
import net.minecraft.client.gui.navigation.NavigationDirection
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.text.Text
import net.minecraft.util.math.MathHelper
import java.util.*
import java.util.function.Function
import java.util.function.Supplier
import java.util.function.UnaryOperator
import kotlin.math.max
import kotlin.math.min

class DynamicListWidget(
    client: MinecraftClient,
    entryBuilders: List<Function<DynamicListWidget, out Entry>>,
    x: Int,
    y: Int,
    width: Int,
    height: Int)
:
    CustomListWidget<DynamicListWidget.Entry>(
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

    fun search(searchInput: String): Int {
        return entries.search(searchInput)
    }

    fun toggleGroup(g: String) {
        entries.toggleGroup(g)
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
        if (entries.isEmpty() || scrollAmount == 0) return false
        if (scrollAmount > 0) {
            val topDelta = -topDelta()
            if (topDelta == 0) return true
            val clampedDist = min(topDelta, scrollAmount)
            entries.scroll(clampedDist)
        } else {
            val bottomDelta = -bottomDelta()
            if (bottomDelta >= 0) return true
            val clampedDist = max(bottomDelta, scrollAmount)
            entries.scroll(clampedDist)
        }
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

                    var c = 0

                    do {
                        entry2 = this.entries.getNextEntry(navigation.direction(), entry2)
                        if (entry2 == null) {
                            return null
                        }
                        guiNavigationPath = entry2.getNavigationPath(navigation)
                        FC.DEVLOG.info("Nav1")
                        FC.DEVLOG.info(guiNavigationPath.toString())
                        c++
                    } while (guiNavigationPath == null && c < 25)
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
                    FC.DEVLOG.info("Nav 2")
                    FC.DEVLOG.info(guiNavigationPath.toString())
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
            val pos = ReferencePos { this@DynamicListWidget.top }
            val entryMap: MutableMap<String, MutableMap<String, Entry>> = mutableMapOf()
            val groupMap: MutableMap<String, GroupPair> = mutableMapOf()

            for (e in delegate) {
                e.onAdd(pos, previousEntry)
                if (!e.visibility.skip) {
                    for (g in e.scope.inGroups) {
                        entryMap.computeIfAbsent(g) { _ -> mutableMapOf() }[e.scope.scope] = e
                    }

                }
                if (e.visibility.group) {
                    groupMap[e.scope.group] = GroupPair(e, true)
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
                if (delegate[index].bottom.get() > this@DynamicListWidget.top)
                    break
                else
                    index++
            }
            var index2 = delegate.lastIndex
            while (index2 >= 0) {
                if (delegate[index2].top.get() < this@DynamicListWidget.bottom)
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
                val groupEntries = delegateMap[e.groupEntry.scope.group]?.values
                if (groupEntries == null) {
                    e.groupEntry.applyVisibility { _ -> Visibility.GROUP_DISABLED }
                    continue
                }
                e.groupEntry.applyVisibility { v -> v.group(groupEntries) }
            }
            if (this@DynamicListWidget.focusedElement?.visibility?.selectable != true) {
                val replacement = this@DynamicListWidget.focusedElement?.getNeighbor(true) ?: this@DynamicListWidget.focusedElement?.getNeighbor(false)
                this@DynamicListWidget.focused = replacement
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
                if (this@DynamicListWidget.focusedElement?.visibility?.selectable != true) {
                    val replacement = this@DynamicListWidget.focusedElement?.getNeighbor(true) ?: this@DynamicListWidget.focusedElement?.getNeighbor(false)
                    this@DynamicListWidget.focused = replacement
                }
                if (bottom() - top() <= this@DynamicListWidget.height) {
                    this@DynamicListWidget.ensureVisible(delegate.first())
                }
                groupPair.visible = false
            } else {
                val otherGroups: MutableSet<String> = mutableSetOf()
                for (e in groupEntries.values) {
                    val g2 = e.scope.group
                    if (g2 != "" && g2 != g) {
                        otherGroups.add(g2)
                    }
                }
                outer@
                for (e in groupEntries.values) {
                    if (!e.visibility.group) {
                        for (g2 in otherGroups) {
                            if (e.scope.inGroups.contains(g2))
                                continue@outer //skip changing visibility of nested groups, except for the nested group headers
                        }
                    }
                    e.applyVisibility(Visibility::unhide)
                }
                groupPair.visible = true
            }
            val last = lastSelectable()
            if (last == null) {
                scrollToTop()
            } else if (last.bottom.get() < this@DynamicListWidget.top) {
                this@DynamicListWidget.ensureVisible(last)
            }
        }

        fun top(): Int {
            return firstSelectable()?.top?.get() ?: this@DynamicListWidget.top
        }

        fun bottom(): Int {
            return lastSelectable()?.bottom?.get() ?: this@DynamicListWidget.bottom
        }

        fun scrollToTop(): Boolean {
            delegate.firstOrNull()?.top?.set(0)?.also { dirty = true } ?: return false
            return true
        }

        fun scrollToBottom(): Boolean {
            delegate.firstOrNull()?.scroll(-this@DynamicListWidget.bottomDelta())?.also { dirty = true } ?: return false
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

        private fun firstSelectable(): Entry? {
            for (e in delegate) {
                if (e.visibility.selectable) return e
            }
            return null
        }

        private fun lastSelectable(): Entry? {
            val iterator = delegate.listIterator(delegate.size)
            while (iterator.hasPrevious()) {
                val e = iterator.previous()
                if (e.visibility.selectable) return e
            }
            return null
        }

        fun getNextEntry(direction: NavigationDirection, entry: Entry?): Entry? {
            FC.DEVLOG.info("This Entry")
            FC.DEVLOG.info(entry.toString())
            FC.DEVLOG.info("Next Entry")
            return if (entry == null) {
                FC.DEVLOG.info("NULL")
                when (direction) {
                    NavigationDirection.UP -> lastSelectable()
                    NavigationDirection.DOWN -> firstSelectable()
                    NavigationDirection.LEFT -> firstSelectable()
                    NavigationDirection.RIGHT -> lastSelectable()
                }.also { FC.DEVLOG.info(it.toString()) }
            } else {
                FC.DEVLOG.info("NOT NULL")
                entry.getNeighbor(!direction.isPositive)?.also { FC.DEVLOG.info(it.toString()) }
            }
        }
    }

    private class GroupPair(val groupEntry: Entry, var visible: Boolean)

    abstract class Entry(parentElement: DynamicListWidget, override val name: Text, override val desc: Text?, val scope: Scope)
        : CustomListWidget.Entry<DynamicListWidget>(parentElement), ParentElement, Searcher.SearchContent {

        var visibility = Visibility.VISIBLE

        protected open val x: Pos = ReferencePos { parentElement.rowX() }
        protected open val w: Pos = ReferencePos { parentElement.rowWidth() }
        protected abstract var h: Int

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
            FC.DEVLOG.info("ON_ADD")
            FC.DEVLOG.info(parentPos.toString())
            FC.DEVLOG.info(previous.toString())
            FC.DEVLOG.info(this.toString())

            if (previous == null) {
                top = RelEntryPos(parentPos, null)
            } else {
                top = ImmutableSuppliedEntryPos(
                    previous.bottom,
                    { if (visibility.visible) verticalPadding.get() else 0 },
                    previous.top
                )

                FC.DEVLOG.info(top.toString())
                previous.top.next = top
                FC.DEVLOG.info(previous.top.toString())
            }
            bottom = ImmutableSuppliedPos(top) { if (visibility.visible) h else 0 }
            init()
            FC.DEVLOG.info("")
        }

        open fun init() {}

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
            if (!focused) {
                this.focusedElement?.isFocused = false
                this.focusedElement = null
            }
        }

        override fun getFocused(): Element? {
            return focusedElement
        }

        override fun setFocused(focused: Element?) {
            this.focusedElement?.isFocused = false
            focused?.isFocused = true
            this.focusedElement = focused
        }

        override fun isDragging(): Boolean {
            return this.dragging
        }

        override fun setDragging(dragging: Boolean) {
            this.dragging = dragging
        }

        override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
            return mouseX >= x.get() && mouseY >= top.get() && mouseX < (x + w) && mouseY < bottom.get()
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            return super<ParentElement>.mouseClicked(mouseX, mouseY, button)
        }

        @Deprecated("Use renderEntry/renderBorder/renderHighlight for rendering instead")
        override fun render (context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            if (!visibility.visible) return
            val t = top.get()
            val bl = this.isMouseOver(mouseX.toDouble(), mouseY.toDouble())
            renderEntry(context, x.get(), t, w.get(), h, mouseX, mouseY, bl, isFocused, delta)
            renderBorder(context, x.get(), t, w.get(), h, mouseX, mouseY, bl, isFocused, delta)
            renderHighlight(context, x.get(), t, w.get(), h, mouseX, mouseY, bl, isFocused, delta)
        }

        abstract fun renderEntry(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float)

        open fun renderBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {}

        open fun renderHighlight(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {}

        override fun getFocusedPath(): GuiNavigationPath? {
            return super<ParentElement>.getFocusedPath()
        }

        override fun getNavigationPath(navigation: GuiNavigation): GuiNavigationPath? {
            if (children().isEmpty()) return null
            if (navigation is Arrow) {
                val i = if(navigation.direction().isPositive) 1 else -1

                val j = MathHelper.clamp(i + children().indexOf(this.focused), 0, children().size - 1)

                var k = j
                while (k >= 0 && k < children().size) {
                    val element = children()[k] as Element
                    val guiNavigationPath = element.getNavigationPath(navigation)
                    if (guiNavigationPath != null) {
                        return GuiNavigationPath.of(this, guiNavigationPath)
                    }
                    k += i
                }
            }

            return super<ParentElement>.getNavigationPath(navigation)
        }

        override fun getNavigationFocus(): ScreenRect {
            return ScreenRect(x.get(), top.get(), w.get(), h)
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

        override fun toString(): String {
            return "Entry:$scope"
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
                return neighbor(up)?.getThisOrSelectableNeighbor(up)
            }

            fun getThisOrSelectableNeighbor(up: Boolean): Entry? {
                return getEntry() ?: neighbor(up)?.getThisOrSelectableNeighbor(up)
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

        inner class ImmutableSuppliedEntryPos(parent: Pos, offset: Supplier<Int>, override val previous: EntryPos?, override var next: EntryPos? = null) : ImmutableSuppliedPos(parent, offset),
                                                                                                                                                           EntryPos {
            override fun getEntry(): Entry? {
                return this@Entry.takeIf { it.visibility.selectable }
            }

            override fun toString(): String {
                return "[${parent.get()} + ${offset.get()}](${previous?.getEntry()}|${next?.getEntry()})"
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

    data class Scope(val scope: String, val group: String = "", val inGroups: List<String> = EMPTY) {
        companion object {
            private val EMPTY = listOf<String>()
        }
    }
}