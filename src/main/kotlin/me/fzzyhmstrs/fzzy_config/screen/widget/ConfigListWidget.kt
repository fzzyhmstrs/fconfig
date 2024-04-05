package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.screen.ConfigScreen
import me.fzzyhmstrs.fzzy_config.screen.LastSelectable
import me.fzzyhmstrs.fzzy_config.screen.entry.ConfigEntry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ElementListWidget
import net.minecraft.client.search.SuffixArray
import java.util.*


internal class ConfigListWidget(minecraftClient: MinecraftClient, parent: ConfigScreen) :
    ElementListWidget<ConfigEntry>(minecraftClient, parent.width, parent.layout.contentHeight, parent.layout.headerHeight, 24), LastSelectable
{

    override var lastSelected: Element? = null

    override fun pushLast() {
        lastSelected = focused
    }

    override fun popLast() {
        (lastSelected as? ConfigEntry)?.let { focused = it }
    }

    private val wholeList: List<ConfigEntry> by lazy{
        this.children().toList()
    }

    private val  search: SuffixArray<Int> by lazy {
        val array = SuffixArray<Int>()
        for ((i, entry) in wholeList.withIndex()){
            array.add(i,entry.name.string.lowercase(Locale.ROOT))
        }
        array.build()
        array
    }

    fun updateSearchedEntries(searchInput: String): Int {
        if (searchInput == "") {
            this.replaceEntries(wholeList.toList())
            scrollAmount = 0.0
            return wholeList.size
        }
        val results = search.findAll(searchInput.lowercase(Locale.ROOT))
        val list = wholeList.filterIndexed { index, _ -> results.contains(index) }
        this.replaceEntries(list)
        scrollAmount = 0.0
        return list.size
    }

    override fun getRowWidth(): Int {
        return 260
    }

    public override fun getScrollbarPositionX(): Int {
        return super.getScrollbarPositionX()
    }

    override fun isSelectButton(button: Int): Boolean {
        return button == 0 || button == 1
    }

    fun getClient(): MinecraftClient{
        return this.client
    }

    fun add(entry: ConfigEntry){
        this.addEntry(entry)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        super.appendClickableNarrations(builder)
        builder.put(NarrationPart.USAGE,"")
    }

    override fun appendNarrations(builder: NarrationMessageBuilder, entry: ConfigEntry) {
        if(entry == focused){
            builder.put(NarrationPart.TITLE, entry.name)
        }
        super.appendNarrations(builder, entry)
    }

}