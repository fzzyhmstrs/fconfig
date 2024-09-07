/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.widget.internal

import net.minecraft.client.gui.widget.GridWidget
import net.minecraft.client.gui.widget.LayoutWidget
import net.minecraft.client.gui.widget.Positioner
import net.minecraft.client.gui.widget.Widget
import net.minecraft.util.Util
import java.util.function.Consumer

class DirectionalLayoutWidget (x: Int, y: Int, private val axis: DisplayAxis): LayoutWidget {

    private var grid: GridWidget = GridWidget(x, y)
    private var currentIndex = 0

    constructor(axis: DisplayAxis) : this(0, 0, axis)

    fun spacing(spacing: Int): DirectionalLayoutWidget {
        axis.setSpacing(grid, spacing)
        return this
    }

    fun copyPositioner(): Positioner {
        return grid.copyPositioner()
    }

    fun getMainPositioner(): Positioner {
        return grid.mainPositioner
    }

    fun <T : Widget?> add(widget: T, positioner: Positioner): T {
        return axis.add(grid, widget, currentIndex++, positioner)
    }

    fun <T : Widget?> add(widget: T): T {
        return this.add(widget, copyPositioner())
    }

    fun <T : Widget?> add(widget: T, callback: Consumer<Positioner>): T {
        return axis.add(grid, widget, currentIndex++, Util.make(copyPositioner(), callback))
    }

    override fun forEachElement(consumer: Consumer<Widget>) {
        grid.forEachElement(consumer)
    }

    override fun refreshPositions() {
        grid.refreshPositions()
    }

    override fun getWidth(): Int {
        return grid.width
    }

    override fun getHeight(): Int {
        return grid.height
    }

    override fun setX(x: Int) {
        grid.x = x
    }

    override fun setY(y: Int) {
        grid.y = y
    }

    override fun getX(): Int {
        return grid.x
    }

    override fun getY(): Int {
        return grid.y
    }

    companion object {
        fun vertical(): DirectionalLayoutWidget {
            return DirectionalLayoutWidget(DisplayAxis.VERTICAL)
        }

        fun horizontal(): DirectionalLayoutWidget {
            return DirectionalLayoutWidget(DisplayAxis.HORIZONTAL)
        }
    }

    enum class DisplayAxis {
        HORIZONTAL,
        VERTICAL;

        fun setSpacing(grid: GridWidget?, spacing: Int) {
            when (ordinal) {
                0 -> {
                    grid!!.setColumnSpacing(spacing)
                }

                1 -> {
                    grid!!.setRowSpacing(spacing)
                }
            }
        }

        fun <T : Widget?> add(grid: GridWidget, widget: T, index: Int, positioner: Positioner?): T {
            return when (ordinal) {
                0 -> grid.add(widget, 0, index, positioner)
                1 -> grid.add(widget, index, 0, positioner)
                else -> throw IllegalStateException()
            }
        }
    }


}