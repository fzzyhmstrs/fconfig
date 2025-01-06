/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.util

import net.minecraft.client.gui.ScreenRect
import net.minecraft.client.gui.tooltip.TooltipPositioner
import org.joml.Vector2i
import org.joml.Vector2ic

class FocusedTooltipPositioner(private val widget: ScreenRect) : TooltipPositioner {
    override fun getPosition(
        screenWidth: Int,
        screenHeight: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): Vector2ic {
        val vector2i = Vector2i()
        vector2i.x = widget.left + 3
        vector2i.y = widget.top + widget.height + 3 + 1
        if (vector2i.y + height + 3 > screenHeight) {
            vector2i.y = widget.top - height - 3 - 1
        }
        if (vector2i.x + width > screenWidth) {
            vector2i.x = (widget.left + widget.width - width - 3).coerceAtLeast(4)
        }
        return vector2i
    }
}