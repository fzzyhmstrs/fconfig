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
import me.fzzyhmstrs.fzzy_config.impl.config.SearchConfig
import me.fzzyhmstrs.fzzy_config.nullCast
import me.fzzyhmstrs.fzzy_config.screen.LastSelectable
import me.fzzyhmstrs.fzzy_config.screen.SuggestionWindowListener
import me.fzzyhmstrs.fzzy_config.screen.context.*
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget.Entry
import me.fzzyhmstrs.fzzy_config.screen.widget.DynamicListWidget.ListSpec
import me.fzzyhmstrs.fzzy_config.screen.widget.custom.CustomListWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.internal.Neighbor
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.Searcher
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.function.ConstFunction
import me.fzzyhmstrs.fzzy_config.util.function.ConstSupplier
import me.fzzyhmstrs.fzzy_config.util.pos.ImmutableSuppliedPos
import me.fzzyhmstrs.fzzy_config.util.pos.Pos
import me.fzzyhmstrs.fzzy_config.util.pos.ReferencePos
import me.fzzyhmstrs.fzzy_config.util.pos.SuppliedPos
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.*
import net.minecraft.client.gui.navigation.GuiNavigation
import net.minecraft.client.gui.navigation.GuiNavigation.Arrow
import net.minecraft.client.gui.navigation.GuiNavigation.Tab
import net.minecraft.client.gui.navigation.GuiNavigationPath
import net.minecraft.client.gui.navigation.NavigationDirection
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.MathHelper
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.*
import java.util.function.*
import java.util.function.Function
import kotlin.math.max
import kotlin.math.min

/**
 * A Widget for displaying a list of [Entry] that supports dynamic operations and entry features.
 * - Entries can have different heights
 * - Entries can be hidden and unhidden
 * - Built-in entry text searching capabilities
 * - Add or remove entries (as of 0.6.5)
 * - Scrollable, navigable, narratable, and everything else expected from a vanilla list.
 * @param client [MinecraftClient] instance
 * @param entryBuilders List&lt;[BiFunction]&lt;DynamicListWidget, Int, [Entry]&gt;&gt; - builders for the initial entry set. Fzzy Config is built on a "lazy-as-possible" paradigm, so the entries themselves aren't passed here.
 * @param x horizontal position of the left edge of the widget in pixels
 * @param y vertical position of the top edge of the widget in pixels
 * @param width width of the widget in pixels
 * @param height height of the widget in pixels
 * @param spec [ListSpec] widget options for customizing list visuals and behavior.
 * @author fzzyhmstrs
 * @since 0.6.0
 */
class DynamicListWidget(
    client: MinecraftClient,
    entryBuilders: List<BiFunction<DynamicListWidget, Int, out Entry>>,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val spec: ListSpec = ListSpec())
:
    CustomListWidget<Entry>(
    client,
    x,
    y,
    width,
    height), Neighbor, LastSelectable, SuggestionWindowListener, ContextHandler, ContextProvider
{

    //// Widget ////

    private var entries: Entries = Entries(entryBuilders.mapIndexed { index, biFunction -> biFunction.apply(this, index) })

    val verticalPadding: Int
        get() = spec.verticalPadding

    override val leftPadding: Int
        get() = spec.leftPadding

    override val rightPadding: Int
        get() = spec.rightPadding

    override fun listNarrationKey(): String {
        return spec.listNarrationKey
    }

    override fun hideScrollWhileNotHovered(): Boolean {
        return spec.hideScrollBar
    }

    override fun onReposition() {
        entries.onResize()
    }

    /**
     * Removes an entry from this list widget if it exists in the entries list. Will remove all instances of it if more than one instance of the same object reference are included in the list.
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    fun removeEntry(entry: Entry) {
        entries = Entries(entries.filter { it != entry })
    }

    /**
     * Adds an entry at the end of the entries list.
     * @param entry [BiFunction]&lt;DynamicListWidget, Int, [Entry]&gt; - builder for the entry to add. Fzzy Config is built on a "lazy-as-possible" paradigm, so the entries themselves aren't passed here.
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    fun addEntry(entry: BiFunction<DynamicListWidget, Int, out Entry>) {
        val l = entries.toList()
        entries = Entries(l + listOf(entry.apply(this, l.size)))
    }

    /**
     * Adds an entry after the specified existing entry in the entries list.
     * @param entry [BiFunction]&lt;DynamicListWidget, Int, [Entry]&gt; - builder for the entry to add. Fzzy Config is built on a "lazy-as-possible" paradigm, so the entries themselves aren't passed here.
     * @param after [Entry] the entry to insert the new entry after
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    fun addEntryAfter(entry: BiFunction<DynamicListWidget, Int, out Entry>, after: Entry) {
        val list = entries.toMutableList().let {
            val i = it.indexOf(after)
            it.add(i + 1, entry.apply(this, i + 1))
            it
        }
        entries = Entries(list)
    }
    /**
     * Adds an entry before the specified existing entry in the entries list.
     * @param entry [BiFunction]&lt;DynamicListWidget, Int, [Entry]&gt; - builder for the entry to add. Fzzy Config is built on a "lazy-as-possible" paradigm, so the entries themselves aren't passed here.
     * @param after [Entry] the entry to insert the new entry after
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    fun addEntryBefore(entry: BiFunction<DynamicListWidget, Int, out Entry>, after: Entry) {
        val list = entries.toMutableList().let {
            val i = it.indexOf(after)
            it.add(i, entry.apply(this, i))
            it
        }
        entries = Entries(list)
    }

    /**
     * Searches the widgets list of [Entry] based on the [Translatable.Result] texts provided with the entries.
     * @param searchInput String input to search for. Searching has several special characters as explained in [Searcher]
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    fun search(searchInput: String): Int {
        return entries.search(searchInput)
    }

    fun groupIsVisible(g: String): Boolean {
        return entries.groupIsVisible(g)
    }

    fun toggleGroup(g: String) {
        entries.toggleGroup(g)
    }

    fun scrollToGroup(g: String) {
        entries.scrollToGroup(g)
    }

    fun scrollToEntry(e: String) {
        entries.scrollToEntry(e)
    }

    override fun selectableEntries(): List<Entry> {
        return entries.selectableEntries()
    }

    override fun inFrameEntries(): List<Entry> {
        return entries.inFrameEntries()
    }

    private val top
        get() = y

    fun fitToContent(max: Int) {
        this.height = min(max, this.contentHeight())
        onReposition()
    }

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
        if (mouseY < this.top || mouseY > this.bottom) return null
        return entries.entryAtY(mouseY)
    }

    override fun handleScroll(verticalAmount: Double): Boolean {
        if (entries.isEmpty() || verticalAmount == 0.0) return false
        if (verticalAmount > 0.0) {
            val topDelta = -topDelta()
            if (topDelta == 0) return true
            val scrollDist = (verticalAmount * 10).toInt().coerceAtLeast(1)
            val clampedDist = min(topDelta, scrollDist)
            entries.scroll(clampedDist)
        } else {
            val bottomDelta = -bottomDelta()
            if (bottomDelta >= 0) return true
            val scrollDist = (verticalAmount * 10).toInt().coerceAtMost(-1)
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

    fun scrollToBottomOutEntry(entry: Entry) {
        if (entry.bottom.get() < bottom && !noScroll()) {
            val scrollAmount = bottom - entry.bottom.get()
            entries.scroll(scrollAmount)
        }
    }

    override var lastSelected: Element? = null

    override fun pushLast() {
        focused?.nullCast<LastSelectable>()?.pushLast()
        lastSelected = focused

    }

    override fun popLast() {
        lastSelected?.nullCast<LastSelectable>()?.popLast()
        (lastSelected as? Entry)?.let { focused = it }
    }

    override fun resetHover(mouseX: Double, mouseY: Double) {
        this.hoveredElement = if (isMouseOver(mouseX, mouseY))
            inFrameEntries().firstOrNull { it.isMouseOver(mouseX, mouseY) }
        else
            null
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderWidget(context, mouseX, mouseY, delta)
        context.enableScissor(0, this.top, MinecraftClient.getInstance().currentScreen?.width ?: 320, this.bottom)
        for (entry in inFrameEntries()) {
            entry.renderExtras(context, mouseX, mouseY, delta)
        }
        context.disableScissor()
    }

    override val neighbor: EnumMap<NavigationDirection, Neighbor> = EnumMap(NavigationDirection::class.java)

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
                        c++
                    } while (guiNavigationPath == null && c < 25)
                    return GuiNavigationPath.of(this, guiNavigationPath)
                }
            } else {
                var entry2: Entry? = null

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

    override fun handleContext(contextType: ContextType, position: Position): Boolean {
        return when (contextType) {
            ContextType.PAGE_UP -> {
                page(true)
                focused = inFrameEntries().firstOrNull() ?: focused
                true
            }
            ContextType.PAGE_DOWN -> {
                page(false)
                focused = inFrameEntries().lastOrNull() ?: focused
                true
            }
            ContextType.HOME -> {
                scrollToTop()
                focused = inFrameEntries().firstOrNull() ?: focused
                true
            }
            ContextType.END -> {
                scrollToBottom()
                focused = inFrameEntries().lastOrNull() ?: focused
                true
            }
            else -> {
                hoveredElement?.handleContext(contextType, position) ?: focusedElement?.handleContext(contextType, position) ?: false
            }
        }
    }

    override fun provideContext(builder: ContextResultBuilder) {
        if (MinecraftClient.getInstance().navigationType.isKeyboard)
            focusedElement?.provideContext(builder) ?: resetHover(builder.position().mX.toDouble(), builder.position().mY.toDouble()).also { hoveredElement?.provideContext(builder) }
        else {
            resetHover(builder.position().mX.toDouble(), builder.position().mY.toDouble()).let { hoveredElement?.provideContext(builder) } ?: focusedElement?.provideContext(builder)
        }
    }

    //////////////////////////////

    private inner class Entries(private val delegate: List<Entry>): Iterable<Entry> {

        //map <group, map <scope, entry> >
        private val delegateMap: Map<String, Map<String, Entry>>
        private val groups: Map<String, GroupPair> by lazy {
            val groupMap: MutableMap<String, GroupPair> = mutableMapOf()
            for (e in delegate) {
                if (e.getVisibility().group) {
                    groupMap[e.scope.group] = GroupPair(e, e.getRootVisibility() != Visibility.GROUP_VISIBLE_CLOSED)
                }
            }
            groupMap
        }

        private val searcher: Searcher<Entry> by lazy { Searcher(delegate) }

        init {
            var previousEntry: Entry? = null
            val pos = ReferencePos { this@DynamicListWidget.top }
            val entryMap: MutableMap<String, MutableMap<String, Entry>> = mutableMapOf()
            val groupMap: MutableMap<String, Entry> = mutableMapOf()

            for ((index, e) in delegate.withIndex()) {
                e.onAdd(pos, previousEntry, index == delegate.lastIndex)
                val v = e.getVisibility()
                if (v.group) {
                    groupMap[e.scope.group] = e
                }
                if (!(v.skip xor v.group)) {
                    for (g in e.scope.inGroups) {
                        if (v.group && e.scope.group == g) continue
                        entryMap.computeIfAbsent(g) { _ -> mutableMapOf() }[e.scope.scope] = e
                    }
                }
                previousEntry = e
            }
            for (e in delegate) {
                for (g in e.scope.inGroups) {
                    val gV = groupMap[g]?.getRootVisibility() ?: continue
                    if (gV == Visibility.GROUP_VISIBLE_CLOSED) {
                        if (e.getVisibility().group && e.scope.group == g) continue
                        e.applyVisibility(Visibility::close)
                    }
                }
            }
            delegateMap = entryMap
        }

        private var inFrameEntries: List<Entry> = emptyList()
        private var selectableEntries: List<Entry> = emptyList()
        private var dirty = true

        fun onResize() {
            dirty = true
            delegate.forEach {
                it.onResize()
            }
        }

        fun selectableEntries(): List<Entry> {
            if (delegate.isEmpty()) return emptyList()
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
                emptyList()
            } else {
                delegate.subList(index, index2).filter { it.getVisibility().visible }
            }
            selectableEntries = delegate.filter { it.getVisibility().selectable }.toMutableList()
            dirty = false
        }

        fun search(searchInput: String): Int {
            dirty = true
            var childrenMatches = 0
            val foundEntries = if (searchInput.isEmpty()) delegate else searcher.search(searchInput)
            val gPrefixes: MutableMap<String, MutableList<Text>> = mutableMapOf()
            if (searchInput.isNotEmpty()) {
                for (e in delegate) {
                    if (e.getVisibility().skip) continue
                    val eResults = e.entrySearchResults(searchInput)
                    if (eResults.isNotEmpty()) {
                        var hidden = false
                        for (g in e.scope.inGroups) {
                            val gp = groups[g] ?: continue
                            if (!gp.visible) {
                                hidden = true
                                gp.groupEntry.applyVisibility(Visibility::searched)
                                gPrefixes.computeIfAbsent(g) { mutableListOf() }.addAll(eResults.map {
                                    FcText.translatable("fc.search.child", e.texts.name, it.name).formatted(Formatting.GRAY)
                                })
                            }
                        }
                        childrenMatches++
                        e.applyVisibility(Visibility::searched)
                        if (hidden)
                            e.applyVisibility(Visibility::hideSearched)
                        e.applyTooltipPrefix(SearchConfig.INSTANCE.prefixText(eResults.map { it.name.copy().formatted(Formatting.GRAY) }))
                    } else {
                        e.applyTooltipPrefix(Entry.EMPTY_PREFIX)
                        e.applyVisibility(Visibility::unsearched)
                        e.applyVisibility(Visibility::filter)
                    }
                }
            } else {
                for (e in delegate) {
                    e.applyTooltipPrefix(Entry.EMPTY_PREFIX)
                    e.applyVisibility(Visibility::unsearched)
                }
            }

            val g2Prefixes: MutableMap<String, MutableList<Text>> = mutableMapOf()
            for (e in foundEntries) {
                if (searchInput.isNotEmpty()) {
                    for (g in e.scope.inGroups) {
                        val gp = groups[g] ?: continue
                        if (!gp.visible) {
                            gp.groupEntry.applyVisibility(Visibility::searched)
                            g2Prefixes.computeIfAbsent(g) { mutableListOf() }.add(e.texts.name.copy().formatted(Formatting.GRAY))
                        }
                    }
                }
                e.applyVisibility(Visibility::unfilter)
            }

            for ((g, gp) in groups) {
                val groupEntries = delegateMap[g]?.values
                if (groupEntries == null) {
                    FC.LOGGER.error("Errored group $g disabled!")
                    gp.groupEntry.applyVisibility(Visibility::disable)
                    continue
                }
                gp.groupEntry.applyVisibility(Visibility.groupFilter(groupEntries))
                if (gPrefixes.containsKey(g) || g2Prefixes.containsKey(g)) {
                    val l = (g2Prefixes[g] ?: listOf()) + (gPrefixes[g] ?: listOf())
                    gp.groupEntry.applyTooltipPrefix( if(l.isEmpty()) Entry.EMPTY_PREFIX else ConstSupplier(Visibility.GROUP_PREFIX + l))
                } else {
                    gp.groupEntry.applyTooltipPrefix(Entry.EMPTY_PREFIX)
                }
            }
            if (this@DynamicListWidget.focusedElement?.getVisibility()?.selectable != true) {
                val replacement = this@DynamicListWidget.focusedElement?.getNeighbor(true) ?: this@DynamicListWidget.focusedElement?.getNeighbor(false)
                this@DynamicListWidget.focused = replacement
            }
            val last = lastSelectable()
            if (last == null) {
                scrollToTop()
            } else {
                if (this@DynamicListWidget.noScroll()) {
                    scrollToTop()
                } else if (searchInput.isNotEmpty()) {
                    this@DynamicListWidget.scrollToBottomOutEntry(last)
                }
            }
            delegate.forEach { it.onScroll(0) }
            return if (childrenMatches > 0) -(foundEntries.size + childrenMatches) else foundEntries.size
        }

        fun groupIsVisible(g: String): Boolean {
            return groups[g]?.visible ?: return false
        }

        fun toggleGroup(g: String) {
            if (delegate.isEmpty()) return
            dirty = true
            val groupEntries = delegateMap[g] ?: return
            val groupPair = groups[g] ?: return
            if (groupPair.visible) {
                for ((s, e) in groupEntries) {
                    e.applyVisibility(Visibility::hide)
                    if (e.getVisibility().group) {
                        groups[s]?.visible = false
                    }
                }
                if (this@DynamicListWidget.focusedElement?.getVisibility()?.selectable != true) {
                    val replacement = this@DynamicListWidget.focusedElement?.getNeighbor(true) ?: this@DynamicListWidget.focusedElement?.getNeighbor(false)
                    this@DynamicListWidget.focused = replacement
                }
                if (bottom() - top() <= this@DynamicListWidget.height) {
                    this@DynamicListWidget.ensureVisible(delegate.first())
                }
                groupPair.visible = false
            } else {
                for ((s, e) in groupEntries) {
                    e.applyVisibility(Visibility::unhide)
                    if (e.getVisibility().group) {
                        val otherGroup = delegateMap[e.scope.group] ?: continue
                        if (otherGroup.values.any { it.getVisibility().visible }) {
                            groups[s]?.visible = true
                        }
                    }
                }
                groupPair.visible = true
            }
            val last = lastSelectable()
            if (last == null) {
                scrollToTop()
            } else {
                if (this@DynamicListWidget.noScroll()) {
                    scrollToTop()
                } else {
                    ensureVisible(last)
                }
            }
            delegate.forEach { it.onScroll(0) }
        }

        fun top(): Int {
            return firstSelectable()?.top?.get()?.minus(this@DynamicListWidget.spec.verticalFirstPadding) ?: this@DynamicListWidget.top
        }

        fun bottom(): Int {
            return lastSelectable()?.bottom?.get() ?: this@DynamicListWidget.bottom
        }

        fun scrollToTop(): Boolean {
            delegate.firstOrNull()?.top?.let {
                val before = it.get()
                it.set(0)
                val after = it.get()
                if (after - before != 0) {
                    dirty = true
                    delegate.forEach { e -> e.onScroll(after - before) }
                }
            } ?: return false
            return true
        }

        fun scrollToBottom(): Boolean {
            delegate.firstOrNull()?.let {
                val delta = -this@DynamicListWidget.bottomDelta()
                if (delta != 0) {
                    it.scroll(delta)
                    dirty = true
                    delegate.forEach { e -> e.onScroll(delta) }
                }
            } ?: return false
            return true
        }

        fun scrollToGroup(g: String) {
            if (delegate.isEmpty()) return
            val groupPair = groups[g] ?: return
            if (this@DynamicListWidget.noScroll()) return
            val delta = this@DynamicListWidget.top - groupPair.groupEntry.top.get()
            this@DynamicListWidget.handleScrollByBar(delta)
            this@DynamicListWidget.focused = groupPair.groupEntry
        }

        fun scrollToEntry(e: String) {
            for (entry in delegate) {
                if (entry.scope.scope == e) {
                    val delta = this@DynamicListWidget.top - entry.top.get()
                    this@DynamicListWidget.handleScrollByBar(delta)
                    this@DynamicListWidget.focused = entry
                    break
                }
            }
        }

        fun scroll(amount: Int) {
            if (delegate.isEmpty()) return
            dirty = true
            delegate.first().scroll(amount)
            if (amount != 0) {
                delegate.forEach { it.onScroll(amount) }
            }
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
                if (e.getVisibility().selectable) return e
            }
            return null
        }

        private fun lastSelectable(): Entry? {
            val iterator = delegate.listIterator(delegate.size)
            while (iterator.hasPrevious()) {
                val e = iterator.previous()
                if (e.getVisibility().selectable) return e
            }
            return null
        }

        fun getNextEntry(direction: NavigationDirection, entry: Entry?): Entry? {
            return if (entry == null) {
                when (direction) {
                    NavigationDirection.UP -> lastSelectable()
                    NavigationDirection.DOWN -> firstSelectable()
                    NavigationDirection.LEFT -> firstSelectable()
                    NavigationDirection.RIGHT -> lastSelectable()
                }
            } else {
                entry.getNeighbor(!direction.isPositive)
            }
        }
    }

    private class GroupPair(val groupEntry: Entry, var visible: Boolean)

    /**
     * Base entry class for list widget entries. This is typically built using a BiFunction that supplies the [parentElement] and an entry index
     * @param parentElement The [DynamicListWidget] this entry belongs to.
     * @param content [Translatable.Result] text set for this entry. This is used when searching entries.
     * @param scope [Scope] defines the entries personal scope as well as any entry groups this entry owns and/or belongs to. Basic entries will only need to provide the personal scope id, which might be as simple as the index in string form.
     * @param visibility [Visibility], default [Visibility.VISIBLE]. The starting visibility setting for the entry.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    abstract class Entry(parentElement: DynamicListWidget, override val content: Translatable.Result, val scope: Scope, visibility: Visibility = Visibility.VISIBLE)
        :
        CustomListWidget.Entry<DynamicListWidget>(parentElement),
        ParentElement,
        Searcher.SearchContent,
        ContextHandler,
        ContextProvider,
        LastSelectable
    {

        companion object {
            @JvmField
            val EMPTY_RESULTS: Function<String, List<Translatable.Result>> = ConstFunction(listOf())
            @JvmField
            val EMPTY_PREFIX: Supplier<List<Text>> = ConstSupplier(listOf())
        }

        val texts: Translatable.Result
            get() = content

        private var visibilityProvider: VisibilityProvider = visibility

        @Internal
        fun getVisibility(): Visibility {
            return visibilityProvider.get()
        }

        @Internal
        fun getRootVisibility(): Visibility {
            return visibilityProvider.getRoot()
        }

        @Internal
        fun applyVisibility(consumer: Consumer<VisibilityStack>) {
            if (this.visibilityProvider !is VisibilityStack) {
                val vs = VisibilityStack(this.getVisibility(), LinkedList())
                this.visibilityProvider = vs
                consumer.accept(vs)
            } else {
                consumer.accept(this.visibilityProvider as VisibilityStack)
            }
        }

        override val skip: Boolean
            get() = getVisibility().skip

        protected open val x: Pos = ReferencePos { parentElement.rowX() }
        protected open val w: Pos = ReferencePos { parentElement.rowWidth() }
        protected abstract var h: Int

        internal var top: EntryPos = EntryPos.ZERO
        internal var bottom: Pos = Pos.ZERO

        @Internal
        fun atY(mouseY: Int): Boolean {
            return mouseY >= top.get() && mouseY < bottom.get()
        }

        override var lastSelected: Element? = null
        private var focusedSelectable: Selectable? = null
        private var focusedElement: Element? = null
        private var dragging = false
        protected var tooltipPrefix: Supplier<List<Text>> = EMPTY_PREFIX

        @Internal
        fun applyTooltipPrefix(prefix: Supplier<List<Text>>) {
            this.tooltipPrefix = prefix
        }

        /**
         * Provides a list of indirect search matches to the dynamic list parent. Used to determine which entries should stay visible because they are indirectly relevant.
         * @param searchInput The raw input string. Has not been parsed for special characters etc. Passing it into a [Searcher] to generate results may be prudent.
         * @return List&lt;[Translatable.Result]&gt; list of text results relevant to the provided search. Default behavior is an empty list.
         * @author fzzyhmstrs
         * @since 0.6.8
         */
        open fun entrySearchResults(searchInput: String): List<Translatable.Result> {
            return EMPTY_RESULTS.apply(searchInput)
        }

        /**
         * The children of the entry that comply to [SelectableElement], that is both [Selectable] and an [Element]
         *
         * This can be used to differentiate "cosmetic" children from "content" children (ones that can be selected via keyboard navigation.
         * - All children, including cosmetic ones (labels, titles, etc.) are passed to [children]
         * - only children that should be considered in keyboard navigation should be passed to this.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        abstract fun selectableChildren(): List<SelectableElement>

        @Internal
        fun getNeighbor(up: Boolean): Entry? {
            return this.top.getSelectableNeighbor(up)
        }

        @Internal
        fun onAdd(parentPos: Pos, previous: Entry?, last: Boolean) {
            if (previous == null) {
                top = RelEntryPos(parentPos, null, offset = parentElement.spec.verticalFirstPadding)
            } else {
                top = RelEntryPos(previous.bottom, previous.top)
                previous.top.next = top
            }
            bottom = ImmutableSuppliedPos(top) {
                if (getVisibility().visible)
                    h + (if(last) parentElement.spec.verticalLastPadding else parentElement.verticalPadding)
                else
                    0
            }
            init()
        }

        override fun provideContext(builder: ContextResultBuilder) {}

        override fun handleContext(contextType: ContextType, position: Position): Boolean {
            return false
        }

        /**
         * Applied when the entry is added to an entries list for the first time. Generally this will happen when the Dynamic List is displayed. This may happen more than once, if an entry is removed or added (the entries list is rebuilt).
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        open fun init() {}

        /**
         * Applied when the parent Dynamic List is resized. This should handle any reorganization/repositioning of children.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        open fun onResize() {}

        /**
         * Applied when the Dynamic List is scrolled. Use this to move children to match the scroll, as applicable.
         * @param dY the amount of scroll.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        open fun onScroll(dY: Int) {}

        @Internal
        fun scroll(dY: Int) {
            if (dY == 0) return
            top.inc(dY)
        }

        override fun pushLast() {
            focused?.nullCast<LastSelectable>()?.pushLast()
            lastSelected = focused
        }

        override fun popLast() {
            lastSelected?.nullCast<LastSelectable>()?.popLast()
            lastSelected?.let { focused = it }
            if (lastSelected == null) {
                focused = selectableChildren().firstOrNull()
            }
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
            if (focusedElement === focused) return
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
            return parentElement.isMouseOver(mouseX, mouseY) && mouseX >= x.get() && mouseY >= top.get() && mouseX < (x + w) && mouseY < bottom.get()
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            return super<ParentElement>.mouseClicked(mouseX, mouseY, button)
        }

        /**
         * Renders the entry. This is the base method of [CustomListWidget.Entry] and generally shouldn't be overridden directly.
         * @param context [DrawContext]
         * @param mouseX Integer horizontal position of the mouse from the left of the screen in pixels
         * @param mouseY Integer vertical position of the mouse from the top of the screen in pixels
         * @param delta tick delta
         * @see renderEntry
         * @see renderBorder
         * @see renderHighlight
         * @see renderExtras
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @Internal
        @Deprecated("Use renderEntry/renderBorder/renderHighlight for rendering instead")
        override fun render (context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            if (!getVisibility().visible) return
            val xx = x.get()
            val tt = top.get()
            val ww = w.get()
            val hh = h
            val bl = this.isMouseOver(mouseX.toDouble(), mouseY.toDouble())
            renderHighlight(context, xx, tt, ww, hh, mouseX, mouseY, bl, isFocused, delta)
            renderBorder(context, xx, tt, ww, hh, mouseX, mouseY, bl, isFocused, delta)
            renderEntry(context, xx, tt, ww, hh, mouseX, mouseY, bl, isFocused, delta)
        }

        /**
         * Base render method for the entry.
         * @param context [DrawContext]
         * @param x Integer position of the left edge of the entry in pixels
         * @param y Integer position of the top edge of the entry in pixels
         * @param width Integer width of the entry in pixels
         * @param height Integer height of the entry in pixels
         * @param mouseX Integer horizontal position of the mouse from the left of the screen in pixels
         * @param mouseY Integer vertical position of the mouse from the top of the screen in pixels
         * @param hovered whether the entry is hovered with the mouse
         * @param focused whether the entry is focused (has been clicked on or navigated to via keyboard)
         * @param delta tick delta
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        abstract fun renderEntry(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float)

        /**
         * Render call for drawing a border on the entry, as applicable. This happens before, therefor "under" the main entry render calls.
         * @param context [DrawContext]
         * @param x Integer position of the left edge of the entry in pixels
         * @param y Integer position of the top edge of the entry in pixels
         * @param width Integer width of the entry in pixels
         * @param height Integer height of the entry in pixels
         * @param mouseX Integer horizontal position of the mouse from the left of the screen in pixels
         * @param mouseY Integer vertical position of the mouse from the top of the screen in pixels
         * @param hovered whether the entry is hovered with the mouse
         * @param focused whether the entry is focused (has been clicked on or navigated to via keyboard)
         * @param delta tick delta
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        open fun renderBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {}
        /**
         * Render call for drawing an entry background highlight. This happens before, therefor "under" both the entry and border render calls
         * @param context [DrawContext]
         * @param x Integer position of the left edge of the entry in pixels
         * @param y Integer position of the top edge of the entry in pixels
         * @param width Integer width of the entry in pixels
         * @param height Integer height of the entry in pixels
         * @param mouseX Integer horizontal position of the mouse from the left of the screen in pixels
         * @param mouseY Integer vertical position of the mouse from the top of the screen in pixels
         * @param hovered whether the entry is hovered with the mouse
         * @param focused whether the entry is focused (has been clicked on or navigated to via keyboard)
         * @param delta tick delta
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        open fun renderHighlight(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {}

        fun renderExtras(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            renderExtras(context, x.get(), top.get(), w.get(), h, mouseX, mouseY, this.isMouseOver(mouseX.toDouble(), mouseY.toDouble()), isFocused, delta)
        }

        /**
         * Render call for rendering anything else related to the entry. This call is not bound by a render scissor, so you can draw "out of bounds" as needed.
         * @param context [DrawContext]
         * @param x Integer position of the left edge of the entry in pixels
         * @param y Integer position of the top edge of the entry in pixels
         * @param width Integer width of the entry in pixels
         * @param height Integer height of the entry in pixels
         * @param mouseX Integer horizontal position of the mouse from the left of the screen in pixels
         * @param mouseY Integer vertical position of the mouse from the top of the screen in pixels
         * @param hovered whether the entry is hovered with the mouse
         * @param focused whether the entry is focused (has been clicked on or navigated to via keyboard)
         * @param delta tick delta
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        open fun renderExtras(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, hovered: Boolean, focused: Boolean, delta: Float) {}

        override fun getFocusedPath(): GuiNavigationPath? {
            return super<ParentElement>.getFocusedPath()
        }

        override fun getNavigationPath(navigation: GuiNavigation): GuiNavigationPath? {
            if (selectableChildren().isEmpty()) return null
            if (navigation is Arrow) {
                val i = if(navigation.direction().isPositive) 1 else -1

                val j = MathHelper.clamp(i + selectableChildren().indexOf(this.focused), 0, selectableChildren().size - 1)

                var k = j
                while (k >= 0 && k < selectableChildren().size) {
                    val element = selectableChildren()[k] as Element
                    val guiNavigationPath = element.getNavigationPath(navigation)
                    if (guiNavigationPath != null) {
                        return GuiNavigationPath.of(this, guiNavigationPath)
                    }
                    k += i
                }
            } else if (navigation is Tab) {
                return computeNavigationPath(navigation)
            }
            return null
        }

        private fun computeNavigationPath(navigation: Tab): GuiNavigationPath? {
            val bl = navigation.forward()
            val element = this.focused
            val focusedPath = element?.getNavigationPath(navigation)
            if (focusedPath != null) return GuiNavigationPath.of(this, focusedPath)
            val list: List<Element?> = ArrayList(this.selectableChildren())
            Collections.sort(list, Comparator.comparingInt { e: Element -> e.navigationOrder })
            val i = list.indexOf(element)
            val j = if (element != null && i >= 0) {
                i + (if (bl) 1 else 0)
            } else if (bl) {
                0
            } else {
                list.size
            }

            val listIterator = list.listIterator(j)
            val booleanSupplier = if (bl) listIterator::hasNext else listIterator::hasPrevious
            val supplier = if (bl) listIterator::next else listIterator::previous

            while (booleanSupplier()) {
                val guiNavigationPath = supplier()?.getNavigationPath(navigation)
                if (guiNavigationPath != null) {
                    return GuiNavigationPath.of(this, guiNavigationPath)
                }
            }
            return null
        }

        override fun getNavigationFocus(): ScreenRect {
            return ScreenRect(x.get(), top.get(), w.get(), h)
        }

        override fun appendNarrations(builder: NarrationMessageBuilder) {
            appendTitleNarrations(builder)
            val list: List<Selectable?> = this.selectableChildren()
            val selectedElementNarrationData = Screen.findSelectedElementData(list, this.focusedSelectable)
            if (selectedElementNarrationData != null) {
                if (selectedElementNarrationData.selectType.isFocused) {
                    this.focusedSelectable = selectedElementNarrationData.selectable
                }

                if (list.size > 1) {
                    builder.put(
                        NarrationPart.POSITION,
                        FcText.translatable("fc.narrator.position.entry", selectedElementNarrationData.index + 1, list.size)
                    )
                    if (selectedElementNarrationData.selectType == Selectable.SelectionType.FOCUSED) {
                        builder.put(NarrationPart.USAGE, FcText.translatable("narration.component_list.usage"))
                    }
                }

                selectedElementNarrationData.selectable.appendNarrations(builder.nextMessage())
            }
        }

        open fun appendTitleNarrations(builder: NarrationMessageBuilder) {
            builder.put(NarrationPart.TITLE, texts.name)
        }

        override fun toString(): String {
            return "Entry:$scope"
        }

        /**
         * Marks an element as both Selectable and an Element. It should be safe to cast something that is separately both an Element and Selectable to this.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        interface SelectableElement: Selectable, Element

        internal interface EntryPos: Pos {
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
                @JvmField
                val ZERO = object: EntryPos {
                    override val previous: EntryPos? = null
                    override var next: EntryPos? = null
                    override fun getEntry(): Entry? { return null }
                    override fun get(): Int { return 0 }
                    override fun set(new: Int) {}
                    override fun inc(amount: Int) {}
                    override fun dec(amount: Int) {}
                    override fun toString(): String {return "ZERO"}
                }
            }
        }

        private inner class RelEntryPos(parent: Pos, override val previous: EntryPos?, override var next: EntryPos? = null, offset: Int = 0) : SuppliedPos(parent, 0, ConstSupplier(offset)), EntryPos {

            override fun getEntry(): Entry? {
                return this@Entry.takeIf { it.getVisibility().selectable }
            }
        }
    }

    /**
     * Enum defining how the user sees and can interact with an entry.
     * - `visible`: 1. Whether the entry is visible. A non-visible entry will take up 0 width in the list (effectively removed from the list, visually)
     * - `skip`: 2. Whether the entry should be skipped when searching
     * - `selectable`: 4. Whether the user can interact with the entry
     * - `group`: 8. Whether the entry belongs to a group
     * - `repeatable`: 16. Whether this visibility can be applied to an entry more than once
     * - `searched`: 32. Visibility applied as a result of searching
     * - `closed`: 64. Visibility applied from an initially-closed state
     * - `filtered`: 128. Entry has been excluded by a search
     * @param affectedBy Predicate to determine if this visibility is affected by another
     * @author fzzyhmstrs
     * @since 0.6.0, uses flags 0.6.8
     */
    //val visible: 1, val skip: 2, val selectable: 4, val group: 8, val repeatable: 16, val searched: 32, val closed: 64, val filtered: 128
    enum class Visibility(private val flags: Int, val affectedBy: Predicate<Visibility>): VisibilityProvider {
        /**
         * Standard visibility. The Entry can be seen, searched, and interacted with.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        //true, false, true, false, false
        VISIBLE(0b00000101, { v -> !v.group }),
        /**
         * Visible because it contains valid search results inside it.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        //true, false, true, false, false, true
        VISIBLE_SEARCHED(0b00100101, { v -> !v.group }),
        /**
         * Entry hidden by a user action like toggling a group, or some visibility button. Not visible nor selectable, but searchable (in case the user reverses the hidden state after searching)
         * @see hide
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        //false, false, false, false, true
        HIDDEN(0b00010000, { v -> !v.group }),
        /**
         * Hidden because it contains valid search results inside it, but it's group is currently collapsed.
         * @author fzzyhmstrs
         * @since 0.6.8
         */
        //false, false, false, false, false, true
        HIDDEN_SEARCHED(0b00100000, { v -> !v.group }),
        /**
         * Entry hidden initially by an initial-closed state
         * @see hide
         * @author fzzyhmstrs
         * @since 0.6.5
         */
        //false, false, false, false, false, false, true
        HIDDEN_CLOSED(0b01000000, { v -> !v.group }),
        /**
         * Entry filtered by searching. Not visible nor selectable
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        //false, false, false, false, false, false, false, true
        FILTERED(0b10000000, { v -> !v.group }),
        /**
         * Visible entry that represents a group heading
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        //true, true, true, true, false
        GROUP_VISIBLE(0b00001111, { v -> v.group }),
        /**
         * Visible entry that represents a group heading
         * @author fzzyhmstrs
         * @since 0.6.8
         */
        //true, true, true, true, false, true
        GROUP_VISIBLE_SEARCHED(0b00101111, { v -> v.group }),
        /**
         * Visible entry that represents a group heading, but should be "closed"/"collapsed" by default
         * @author fzzyhmstrs
         * @since 0.6.5
         */
        //true, true, true, true, false, false, true
        GROUP_VISIBLE_CLOSED(0b01001111, { v -> v.group }),
        /**
         * Filtered entry that represents a group heading. Filtering is usually done with a search.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        //false, true, false, true, false, false, false, true
        GROUP_FILTERED(0b10001010, { v -> v.group }), //filtering handled externally
        /**
         * Hidden entry that represents a group heading. Hiding is usually done with a button or other toggle.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        //false, true, false, true, true
        GROUP_HIDDEN(0b00011010, { v -> v.group }), //hiding handled externally
        /**
         * Hidden entry that represents a group heading. This group has valid search results inside it, hence the searched flag
         * @author fzzyhmstrs
         * @since 0.6.8
         */
        //false, true, false, true, true, true, false
        GROUP_HIDDEN_SEARCHED(0b00111010, { v -> v.group }), //hiding handled externally
        /**
         * Hidden entry that represents a group heading. Group hidden initially by an initial-closed state. Hiding is usually done with a button or other toggle.
         * @author fzzyhmstrs
         * @since 0.6.8
         */
        //false, true, false, true, true, false, true
        GROUP_HIDDEN_CLOSED(0b01011010, { v -> v.group }), //hiding handled externally
        /**
         * A disabled group heading, typically caused by a layout error.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        //false, true, false, true, false
        GROUP_DISABLED(0b00001010, { _ -> false }), //problem group with no entries
        /**
         * A header entry. Always visible, skipped in searching, and not selectable. Visibility of headers can't be changed.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        //true, true, false, false, false
        HEADER_VISIBLE(0b00000011, { _ -> false });

        val visible: Boolean
            get() = flags and 1 == 1
        val skip: Boolean
            get() = flags and 2 == 2
        val selectable: Boolean
            get() = flags and 4 == 4
        val group: Boolean
            get() = flags and 8 == 8
        val repeatable: Boolean
            get() = flags and 16 == 16
        val searched: Boolean
            get() = flags and 32 == 32
        val closed: Boolean
            get() = flags and 64 == 64
        val filtered: Boolean
            get() = flags and 128 == 128

        override fun get(): Visibility {
            return this
        }

        companion object {
            fun disable(visibilityStack: VisibilityStack) {
                visibilityStack.push(GROUP_DISABLED)
            }

            fun filter(visibilityStack: VisibilityStack) {
                visibilityStack.push(FILTERED)
            }

            fun unfilter(visibilityStack: VisibilityStack) {
                visibilityStack.remove(FILTERED)
            }

            fun searched(visibilityStack: VisibilityStack) {
                visibilityStack.push(VISIBLE_SEARCHED)
                visibilityStack.push(GROUP_VISIBLE_SEARCHED)
            }

            fun unsearched(visibilityStack: VisibilityStack) {
                visibilityStack.remove(VISIBLE_SEARCHED)
                visibilityStack.remove(GROUP_VISIBLE_SEARCHED)
            }

            fun close(visibilityStack: VisibilityStack) {
                visibilityStack.push(HIDDEN_CLOSED)
                visibilityStack.push(GROUP_HIDDEN_CLOSED)
            }

            fun hide(visibilityStack: VisibilityStack) {
                visibilityStack.push(HIDDEN)
                visibilityStack.push(GROUP_HIDDEN)
            }

            fun hideSearched(visibilityStack: VisibilityStack) {
                visibilityStack.push(HIDDEN_SEARCHED)
                visibilityStack.push(GROUP_HIDDEN_SEARCHED)
            }

            fun unhide(visibilityStack: VisibilityStack) {
                visibilityStack.remove(HIDDEN)
                visibilityStack.remove(GROUP_HIDDEN)
                visibilityStack.remove(HIDDEN_SEARCHED)
                visibilityStack.remove(GROUP_HIDDEN_SEARCHED)
                visibilityStack.remove(HIDDEN_CLOSED)
                visibilityStack.remove(GROUP_HIDDEN_CLOSED)
            }

            fun groupFilter(groupEntries: Collection<Entry>): Consumer<VisibilityStack> {
                return if (groupEntries.all { it.getVisibility().filtered }) {
                    Consumer { stack -> stack.push(GROUP_FILTERED) }
                } else {
                    Consumer { stack -> stack.remove(GROUP_FILTERED) }
                }
            }

            @Deprecated("Removal in 0.7.0")
            val EMPTY: Consumer<LinkedList<Visibility>> = Consumer { _-> }

            @JvmField
            internal val GROUP_PREFIX: List<Text> = listOf(FcText.translatable("fc.search.indirect.group"))
        }
    }

    /**
     * The scope context of an entry. This is used during searching and other methods of filtering. For basic lists, all that is needed is [scope]
     * @param scope String id of the entry, should be unique
     * @param group Default "", the group this entry owns
     * @param inGroups Default empty list, the group(s) this entry is part of
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    data class Scope(val scope: String, val group: String = "", val inGroups: List<String> = listOf())

    /**
     * Feature configuration spec for a Dynamic List
     * @param leftPadding Default 16; the horizontal space in pixels between the left edge of the list widget and the left edge of the entry rows
     * @param rightPadding Default 10; the horizontal spacing in pixels between the right edge of the entry rows and the left edge of the scroll bar. The scroll bar typically takes up 6 pixels of width, hence the delta between this and [leftPadding]
     * @param verticalPadding Default 4; the vertical spacing in pixels between list entries.
     * @param verticalFirstPadding Default whatever [verticalPadding] is; the vertical spacing in pixels between the top of the left widget and the top of the first entry row, when the list is fully scrolled to the top of the list.
     * @param verticalLastPadding Default whatever [verticalFirstPadding] is; the vertical spacing in pixels between the bottom of the last list entry and the bottom of the widget, then the list is fully scrolled down, if applicable. If the set of entries is shorter than the list widget height, this won't apply.
     * @param hideScrollBar Default false; whether the scroll bar should be hidden if it's not needed (the entries height is shorter than the widget height).
     * @param listNarrationKey Default "fc.narrator.position.config"; the translation key of the narration used when describing the position of the entry in the list. The default key translates to "Setting X of Y".
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    data class ListSpec(val leftPadding: Int = 16,
                        val rightPadding: Int = 10,
                        val verticalPadding: Int = 4,
                        val verticalFirstPadding: Int = verticalPadding,
                        val verticalLastPadding: Int = verticalFirstPadding,
                        val hideScrollBar: Boolean = false,
                        val listNarrationKey: String = "fc.narrator.position.config")

    /**
     * Returns a Visibility for a consumer of an entries Visibility. [Visibility] itself is a visibility provider, returning itself. This is to lower the memory footprint of the entries if, as is typical, they are only ever one visibility (usually [Visibility.VISIBLE]). If visibility is modified during usage, the default visibility provider enum is replaced with a [VisibilityStack] to track visibility changes over time.
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    @FunctionalInterface
    fun interface VisibilityProvider {
        /**
         * Returns the entries current [Visibility] status
         * @author fzzyhmstrs
         * @since 0.6.5
         */
        fun get(): Visibility

        /**
         * Returns the entries root [Visibility] status
         * @author fzzyhmstrs
         * @since 0.6.6
         */
        fun getRoot(): Visibility {
            return get()
        }
    }

    /**
     * A [VisibilityProvider] that has child visibilities that can be pushed to and removed from.
     * @author fzzyhmstrs
     * @since 0.6.8
     */
    interface VisibilityStackProvider: VisibilityProvider {
        fun push(v: Visibility)
        fun remove(v: Visibility)
    }

    /**
     * A [VisibilityProvider] that tracks the base visibility of an entry as well as the entries visibility history through a stack (linked list).
     * @author fzzyhmstrs
     * @since 0.6.0, implements [VisibilityProvider] as of 0.6.5, implements [VisibilityStackProvider] as of 0.6.8
     */
    data class VisibilityStack (private val baseVisibility: Visibility, private val visibilityStack: LinkedList<Visibility>): VisibilityStackProvider {
        /** @suppress */
        override fun get(): Visibility {
            return visibilityStack.firstOrNull() ?: baseVisibility
        }

        /** @suppress */
        override fun getRoot(): Visibility {
            return baseVisibility
        }

        /** @suppress */
        override fun push(v: Visibility) {
            if (baseVisibility.affectedBy.test(v) && (v.repeatable || !visibilityStack.contains(v))) visibilityStack.push(v)
        }

        /** @suppress */
        override fun remove(v: Visibility) {
            if (baseVisibility.affectedBy.test(v)) visibilityStack.removeFirstOccurrence(v)
        }
    }
}