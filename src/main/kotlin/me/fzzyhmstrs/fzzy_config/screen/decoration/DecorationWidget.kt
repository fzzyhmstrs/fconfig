/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.decoration

import net.minecraft.client.gui.DrawContext

/**
 * Basic widget class for rendering a Decoration. This will consider a 20x20 region for positioning and rendering the decoration.
 * @param decoration [Decorated], Nullable. The decoration to render, or null to not render anything. This is a non-final field that can be updated as needed with [setDeco]
 * @param offsetX Integer X offset in pixels to render the decoration. This can be updated as needed with [setDeco]
 * @param offsetY Integer Y offset in pixels to render the decoration. This can be updated as needed with [setDeco]
 * @author fzzyhmstrs
 * @since 0.6.0
 */
class DecorationWidget @JvmOverloads constructor(private var decoration: Decorated? = null, private var offsetX: Int = 0, private var offsetY: Int = 0): AbstractDecorationWidget() {

    /**
     * @suppress
     */
    override fun getWidth(): Int {
        return if (decoration == null) 0 else 20
    }

    /**
     * @suppress
     */
    override fun getHeight(): Int {
        return if (decoration == null) 0 else 20
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        decoration?.renderDecoration(context, x + offsetX, y + offsetY, delta, enabled = true, selected = false)
    }

    /**
     * Updates the rendered decoration.
     * @param newDecoration [Decorated] to replace the current one
     * @param newOffsetX new X position offset for the decoration rendering
     * @param newOffsetY new Y position offset for the decoration rendering
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    @JvmOverloads
    fun setDeco(newDecoration: Decorated, newOffsetX: Int = offsetX, newOffsetY: Int = offsetY) {
        this.decoration = newDecoration
        this.offsetX = newOffsetX
        this.offsetY = newOffsetY
    }
}