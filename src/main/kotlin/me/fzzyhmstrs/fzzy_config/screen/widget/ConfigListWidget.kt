package me.fzzyhmstrs.fzzy_config.screen.widget

import me.fzzyhmstrs.fzzy_config.screen.ConfigScreen
import me.fzzyhmstrs.fzzy_config.screen.entry.ConfigEntry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.screen.narration.NarrationPart
import net.minecraft.client.gui.widget.ElementListWidget


class ConfigListWidget(minecraftClient: MinecraftClient, parent: ConfigScreen) :
    ElementListWidget<ConfigEntry>(minecraftClient, parent.width, parent.layout.contentHeight, parent.layout.headerHeight, 24)
{
    override fun getRowWidth(): Int {
        return 340
    }

    public override fun getScrollbarPositionX(): Int {
        return super.getScrollbarPositionX()
    }

    fun getClient(): MinecraftClient{
        return this.client
    }

    fun add(entry: ConfigEntry){
        this.addEntry(entry)
    }

    override fun appendNarrations(builder: NarrationMessageBuilder, entry: ConfigEntry) {
        if(entry == focused){
            builder.put(NarrationPart.TITLE, entry.name)
        }
        super.appendNarrations(builder, entry)
    }

}