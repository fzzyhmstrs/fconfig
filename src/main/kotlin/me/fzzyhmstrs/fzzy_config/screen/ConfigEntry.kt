package me.fzzyhmstrs.fzzy_config.screen

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.widget.ElementListWidget

abstract class ConfigEntry(): ElementListWidget.Entry<ConfigEntry>() {
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
        //put basic rendering here
        renderConfigWidgets(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta)
    }

    abstract fun renderConfigWidgets(
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
    )

    override fun children(): MutableList<out Element> {
        TODO("Not yet implemented")
    }

    override fun selectableChildren(): MutableList<out Selectable> {
        TODO("Not yet implemented")
    }
}