/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen.decoration

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable

/**
 * A [Drawable]-like interface used to render a icon, alert symbol, or other type of small decoration visual.
 *
 * This can be used in place of drawable or other rendering interface for a larger rendered area if desirable. It's not hard-locked to being used on small objects, but Fzzy Config treats this interface internally as used on 20x20 and smaller areas.
 * @author fzzyhmstrs
 * @since 0.6.0
 */
//client
@FunctionalInterface
fun interface Decorated {
    /**
     * Renders the decoration. Notably, this provides active and selected context for rendering different things based on those states.
     * @param context [DrawContext]
     * @param x Left edge of render area
     * @param y Top of render area
     * @param delta render frame delta
     * @param enabled Whether this rendering is "active" or not. Widgets that call this should pass their `this.active` state for this
     * @param selected Whether this rendering is "hovered" or "focused". Widgets that call this should use their `this.isSelected()` call for this
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun renderDecoration(context: DrawContext, x: Int, y: Int, delta: Float, enabled: Boolean, selected: Boolean)

    /**
     * A [decorated] wrapped with rendering position offsets. This is used in several places for the modder to define positional offsets for rendering their decoration in the proper place
     * @param decorated [Decorated]
     * @param offsetX horizontal offset in pixels to render the decorated
     * @param offsetY vertical offset in pixels to render this decorated
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    class DecoratedOffset(val decorated: Decorated, val offsetX: Int = 0, val offsetY: Int)

}