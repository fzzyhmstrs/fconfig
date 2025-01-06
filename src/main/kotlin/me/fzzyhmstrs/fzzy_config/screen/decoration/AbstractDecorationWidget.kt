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

import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.Widget
import java.util.function.Consumer

/**
 * Base class for a non-interactable widget used simply for rendering information. In Fzzy Config this is used for Decoration-based rendering, but it can be used elsewhere.
 *
 * The width and height of this widget are locked to 20, override if you need variable or different dimensions.
 * @author fzzyhmstrs
 * @since 0.6.0
 */
abstract class AbstractDecorationWidget: Widget, Drawable {

    private var x: Int = 0
    private var y: Int = 0

    /**
     * @suppress
     */
    override fun setX(x: Int) {
        this.x = x
    }

    /**
     * @suppress
     */
    override fun setY(y: Int) {
        this.y = y
    }

    /**
     * @suppress
     */
    override fun getX(): Int {
        return x
    }

    /**
     * @suppress
     */
    override fun getY(): Int {
        return y
    }

    /**
     * @suppress
     */
    override fun getWidth(): Int {
        return 20
    }

    /**
     * @suppress
     */
    override fun getHeight(): Int {
        return 20
    }

    /**
     * @suppress
     */
    override fun forEachChild(consumer: Consumer<ClickableWidget>?) {
    }
}