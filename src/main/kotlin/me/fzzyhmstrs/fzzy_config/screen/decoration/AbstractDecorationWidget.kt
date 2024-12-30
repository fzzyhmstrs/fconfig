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
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.function.Consumer

//TODO
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