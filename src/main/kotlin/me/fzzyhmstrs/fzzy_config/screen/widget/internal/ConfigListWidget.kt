/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.widget.internal

import me.fzzyhmstrs.fzzy_config.screen.LastSelectable
import me.fzzyhmstrs.fzzy_config.screen.entry.BaseConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.entry.SettingConfigEntry
import me.fzzyhmstrs.fzzy_config.screen.internal.ConfigScreen
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowListener
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.ScreenRect
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.ElementListWidget
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget
import net.minecraft.client.gui.widget.Widget
import net.minecraft.client.search.SuffixArray
import java.util.*
import java.util.function.Consumer

//client
@Deprecated("To Remove")
internal class ConfigListWidget(minecraftClient: MinecraftClient, width: Int, contentHeight: Int, headerHeight: Int, private val drawBackground: Boolean) :
    ElementListWidget<BaseConfigEntry>(minecraftClient, width, contentHeight, headerHeight, 24), LastSelectable, SuggestionWindowListener
{

    constructor(minecraftClient: MinecraftClient, parent: ConfigScreen, drawBackground: Boolean = true): this(minecraftClient, parent.width, parent.height, parent.layout.height - parent.layout.headerHeight - parent.layout.footerHeight, parent.layout.headerHeight, drawBackground)

    private var visibleElements = 5

    private var suggestionWindowElement: Element? = null

    override fun setSuggestionWindowElement(element: Element?) {
        this.suggestionWindowElement = element
    }

    init {
        this.setRenderHorizontalShadows(drawBackground)
        this.setRenderBackground(drawBackground)
    }

    private val wholeList: List<BaseConfigEntry> by lazy {
        this.children().toList()
    }

    private val search: SuffixArray<Int> by lazy {
        val array = SuffixArray<Int>()
        for ((i, entry) in wholeList.withIndex()) {
            array.add(i, entry.name.string.lowercase(Locale.ROOT))
        }
        array.build()
        array
    }

    private val searchExact: Map<String, Int> by lazy {
        val map: MutableMap<String, Int> = mutableMapOf()
        for ((i, entry) in wholeList.withIndex()) {
            map[entry.name.string.lowercase(Locale.ROOT)] = i
        }
        map
    }

    private val searchDesc: SuffixArray<Int> by lazy {
        val array = SuffixArray<Int>()
        for ((i, entry) in wholeList.withIndex()) {
            array.add(i, entry.description.string.lowercase(Locale.ROOT))
        }
        array.build()
        array
    }

    override var lastSelected: Element? = null

    override fun pushLast() {
        lastSelected = focused
    }

    override fun popLast() {
        (lastSelected as? BaseConfigEntry)?.let { focused = it }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return suggestionWindowElement?.mouseClicked(mouseX, mouseY, button) ?: super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        return suggestionWindowElement?.mouseScrolled(mouseX, mouseY, amount) ?: super.mouseScrolled(mouseX, mouseY, amount)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return suggestionWindowElement?.keyPressed(keyCode, scanCode, modifiers) ?: super.keyPressed(keyCode, scanCode, modifiers)
    }

    fun updateSearchedEntries(searchInput: String): Int {
        val type: SearchType
        val trimmedSearchInput = if (searchInput.startsWith('$')) {
            type = SearchType.DESCRIPTION
            if (searchInput.length == 1) {
                ""
            } else {
                searchInput.substring(1)
            }
        } else if (searchInput.startsWith('-')) {
            if (searchInput.startsWith("-$")) {
                type = SearchType.NEGATE_DESCRIPTION
                if (searchInput.length == 2) {
                    ""
                } else {
                    searchInput.substring(2)
                }
            } else if (searchInput.startsWith("-\"")) {
                if (searchInput.length > 2 && searchInput.endsWith('"')) {
                    type = SearchType.NEGATE_EXACT
                    if (searchInput.length == 3) {
                        ""
                    } else {
                        searchInput.substring(2, searchInput.lastIndex)
                    }
                } else {
                    type = SearchType.NEGATION
                    if (searchInput.length == 2) {
                        ""
                    } else {
                        searchInput.substring(2)
                    }
                }
            } else {
                type = SearchType.NEGATION
                if (searchInput.length == 1) {
                    ""
                } else {
                    searchInput.substring(1)
                }
            }
        } else if (searchInput.startsWith('"')) {
            if (searchInput.length > 1 && searchInput.endsWith('"')) {
                type = SearchType.EXACT
                if (searchInput.length == 2) {
                    ""
                } else {
                    searchInput.substring(1, searchInput.lastIndex)
                }
            } else {
                type = SearchType.NORMAL
                if (searchInput.length == 1) {
                    ""
                } else {
                    searchInput.substring(1)
                }
            }
        } else {
            type = SearchType.NORMAL
            searchInput
        }

        if (trimmedSearchInput == "") {
            this.replaceEntries(wholeList.toList())
            scrollAmount = 0.0
            return wholeList.size
        }

        val list: List<BaseConfigEntry>
        when (type) {
            SearchType.DESCRIPTION -> {
                val results = searchDesc.findAll(trimmedSearchInput.lowercase(Locale.ROOT))
                list = wholeList.filterIndexed { index, _ -> results.contains(index) }
            }
            SearchType.NEGATION -> {
                val results = search.findAll(trimmedSearchInput.lowercase(Locale.ROOT))
                list = wholeList.filterIndexed { index, _ -> !results.contains(index) }
            }
            SearchType.NEGATE_DESCRIPTION -> {
                val results = searchDesc.findAll(trimmedSearchInput.lowercase(Locale.ROOT))
                list = wholeList.filterIndexed { index, _ -> !results.contains(index) }
            }
            SearchType.EXACT -> {
                val result = searchExact[trimmedSearchInput.lowercase(Locale.ROOT)]
                list = if(result != null) listOf(wholeList[result]) else emptyList()
            }
            SearchType.NEGATE_EXACT -> {
                val result = searchExact[trimmedSearchInput.lowercase(Locale.ROOT)]
                list = wholeList.filterIndexed { index, _ -> index != result }
            }
            SearchType.NORMAL -> {
                val results = search.findAll(trimmedSearchInput.lowercase(Locale.ROOT))
                list = wholeList.filterIndexed { index, _ -> results.contains(index) }
            }
        }
        /*val results = search.findAll(searchInput.lowercase(Locale.ROOT))
        val list = wholeList.filterIndexed { index, _ -> results.contains(index) }*/
        this.replaceEntries(list)
        scrollAmount = 0.0
        return list.size
    }


    private enum class SearchType {
        DESCRIPTION,
        NEGATION,
        NEGATE_DESCRIPTION,
        EXACT,
        NEGATE_EXACT,
        NORMAL
    }

    override fun setX(x: Int) {
        this.left = x
        this.right = x + width
    }
    override fun setY(y: Int) {
        this.top = y + listHeaderHeight
        this.bottom = y + listHeaderHeight + contentHeight
    }
    override fun getX(): Int {
        return this.left
    }
    override fun getY(): Int {
        return this.top - listHeaderHeight
    }
    override fun getWidth(): Int {
        return this.width
    }
    override fun getHeight(): Int {
        return this.height
    }
    override fun getNavigationFocus(): ScreenRect {
        return ScreenRect(this.left, this.top, this.width, this.contentHeight)
    }
    override fun forEachChild(consumer: Consumer<ClickableWidget>) {
    }

    override fun getRowWidth(): Int {
        return 260
    }

    fun position(width: Int, height: Int, y: Int, contentHeight: Int) {
        this.setDimensions(width, height, contentHeight)
        setPosition(0, y)
        var count = 0
        for (i in 0 until this.entryCount) {
            val entryY = getRowTop(i)
            val entryYBottom = getRowBottom(i)
            if (entryY >= this.y && entryYBottom <= this.bottom) {
                count++
            }
            this.getEntry(i).positionWidget(entryY)
        }
        visibleElements = count
    }

    private fun setDimensions(width: Int, height: Int, contentHeight: Int) {
        this.contentHeight = contentHeight
        this.width = width
        this.right = this.left + width
        this.height = height
        this.bottom = this.top + (height - (height - listHeaderHeight - contentHeight))
    }

    fun position(width: Int, layout: ThreePartsLayoutWidget) {
        this.position(width, layout.height, 0, (layout.height - layout.headerHeight - layout.footerHeight))
    }

    fun getScrollbarX(): Int {
        return scrollbarPositionX
    }

    override fun getScrollbarPositionX(): Int {
        return this.left + this.width / 2 + this.rowWidth / 2 + 10
    }

    fun getClient(): MinecraftClient {
        return this.client
    }

    fun page(up: Boolean) {
        if (up) {
            scrollAmount -= (visibleElements * itemHeight)
        } else {
            scrollAmount += (visibleElements * itemHeight)
        }
    }

    fun add(entry: BaseConfigEntry) {
        this.addEntry(entry)
    }

    fun copy() {
        (focused as? SettingConfigEntry) ?.copyAction?.run() ?: (hoveredEntry as? SettingConfigEntry)?.copyAction?.run()
    }

    fun paste() {
        (focused as? SettingConfigEntry)?.pasteAction?.run() ?: (hoveredEntry as? SettingConfigEntry)?.pasteAction?.run()
    }

    override fun appendNarrations(builder: NarrationMessageBuilder) {
        super.appendNarrations(builder)
        builder.put(NarrationPart.USAGE, "")
    }

    override fun appendNarrations(builder: NarrationMessageBuilder, entry: BaseConfigEntry) {
        if(entry == focused || entry == hoveredEntry) {
            builder.put(NarrationPart.TITLE, entry.name)
            entry.appendEntryNarrations(builder)
        }
        super.appendNarrations(builder, entry)
    }
}