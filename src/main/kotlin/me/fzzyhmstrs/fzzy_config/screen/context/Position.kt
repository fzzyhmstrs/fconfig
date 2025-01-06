/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.context

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.Widget

/**
 * Position context information for context handling. Includes information about the screen, mouse, and scoped context area. This scoped context initially is the same as screen context (i.e. x: 0, y: 0, w: screenW, h: screenH), but can be updated with proper scoped information as handling moved downstream. For example if handling moves to a widget, it can update xy and wh with it's xy position and width/height.
 * @param contextInput [ContextInput] the type of input relevant to this context event
 * @param mX current mouse X position in pixels
 * @param mY current mouse Y position in pixels
 * @param x scoped X position. Downstream handlers should update this with their scoped position as needed.
 * @param y scoped Y position. Downstream handlers should update this with their scoped position as needed.
 * @param screenWidth current screen width in pixels
 * @param screenHeight current screen height in pixels
 * @author fzzyhmstrs
 * @since 0.6.0
 */
//client
data class Position(val contextInput: ContextInput,
                    val mX: Int, val mY: Int, //mouse xy
                    val x: Int, val y: Int, // element xy
                    val width: Int, val height: Int, //element wh
                    val screenWidth: Int, val screenHeight: Int) { //screen wh

    companion object {

        /**
         * Recreated position context from a provided widget.
         * @param widget [Widget] widget to build position context around. The widget will inform the "scoped" context
         * @param contextInput [ContextInput] Default [ContextInput.KEYBOARD], the type of input used in your context event.
         * @return [Position] instance built around the widget position
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun fromWidget(widget: Widget, contextInput: ContextInput = ContextInput.KEYBOARD): Position {
            val client = MinecraftClient.getInstance()
            val mX = client.mouse.x * client.window.scaledWidth / client.window.width
            val mY = client.mouse.y * client.window.scaledHeight / client.window.height
            return Position(contextInput,
                mX.toInt(), mY.toInt(),
                widget.x, widget.y,
                widget.width, widget.height,
                MinecraftClient.getInstance()?.currentScreen?.width ?: widget.width,
                MinecraftClient.getInstance()?.currentScreen?.height ?: widget.height)
        }
    }
}