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
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ElementListWidget
import net.minecraft.client.search.SuffixArray
import java.util.*

//client
internal class ConfigListWidget(minecraftClient: MinecraftClient, width: Int, contentHeight: Int, headerHeight: Int, private val drawBackground: Boolean) :
    ElementListWidget<BaseConfigEntry>(minecraftClient, width, contentHeight, headerHeight, 24), LastSelectable, SuggestionWindowListener
{

    constructor(minecraftClient: MinecraftClient, parent: ConfigScreen, drawBackground: Boolean = true): this(minecraftClient, parent.width, parent.layout.contentHeight, parent.layout.headerHeight, drawBackground)

    private var visibleElements = 5

    private var suggestionWindowElement: Element? = null

    override fun setSuggestionWindowElement(element: Element?) {
        this.suggestionWindowElement = element
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

    override fun drawHeaderAndFooterSeparators(context: DrawContext) {
        if (drawBackground)
            super.drawHeaderAndFooterSeparators(context)
    }

    override fun drawMenuListBackground(context: DrawContext) {
        if (drawBackground)
            super.drawMenuListBackground(context)
    }

    override fun pushLast() {
        lastSelected = focused
    }

    override fun popLast() {
        (lastSelected as? BaseConfigEntry)?.let { focused = it }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return suggestionWindowElement?.mouseClicked(mouseX, mouseY, button) ?: super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return suggestionWindowElement?.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) ?: super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
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
                list = if(result != null) listOf(wholeList[result]) else listOf()
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

    override fun getRowWidth(): Int {
        return 260
    }

    override fun position(width: Int, height: Int, y: Int) {
        super.position(width, height, y)
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

    public override fun getScrollbarX(): Int {
        return super.getScrollbarX()
    }

    override fun isSelectButton(button: Int): Boolean {
        return button == 0 || button == 1
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

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        super.appendClickableNarrations(builder)
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