package me.fzzyhmstrs.fzzy_config.screen.widget

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment

/**
 * Provides a set width/height method for parents of implementing widgets
 *
 * Expectation is that the set methods actually change the dimensions of the widget, much like [net.minecraft.client.gui.widget.ClickableWidget]
 */
@Environment(EnvType.CLIENT)
interface Scalable {
    fun setWidth(width: Int)
    fun setHeight(height: Int)
}