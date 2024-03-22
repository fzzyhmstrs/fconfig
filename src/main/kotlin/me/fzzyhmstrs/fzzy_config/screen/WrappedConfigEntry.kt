package me.fzzyhmstrs.fzzy_config.screen

import me.fzzyhmstrs.fzzy_config.screen.entry.ConfigEntry
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.widget.ElementListWidget

/**
 * ConfigEntry wrapper that adds optional context-specific buttons like "Forward"
 */
abstract class WrappedConfigEntry(private val entry: ConfigEntry): ElementListWidget.Entry<WrappedConfigEntry>() {
    override fun render(
        context: DrawContext,
        index: Int,
        y: Int,
        x: Int,
        entryWidth: Int,
        entryHeight: Int,
        mouseX: Int,
        mouseY: Int,
        hovered: Boolean,
        tickDelta: Float
    ) {
        entry.render(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta)
    }

    override fun children(): MutableList<out Element> {
        TODO("Not yet implemented")
    }

    override fun selectableChildren(): MutableList<out Selectable> {
        TODO("Not yet implemented")
    }
}