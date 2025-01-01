/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.screen

import net.minecraft.client.gui.Element

/**
 * A parent element marked as LastSelectable will cache the last selected element when a popup or other overlay is rendered, and return to that selection when the popup/overlay is removed.
 * @sample me.fzzyhmstrs.fzzy_config.examples.PopupWidgetExamples.lastSelectable
 * @author fzzyhmstrs
 * @since 0.2.0, added [resetHover] 0.6.0
 */
//client
@JvmDefaultWithoutCompatibility
interface LastSelectable {

    /**
     * getter and setter for the cached element.
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    var lastSelected: Element?

    /**
     * Indicates to the parent element to cache it's current focused element. The current focused element should be stored in [lastSelected]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun pushLast()

    /**
     * Indicates that the overlay has been removed and the parent should return focus to the cached element, if any, in [lastSelected]
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    fun popLast()

    /**
     * When called the parent element should reselect a hovered element based on the supplied mouse positions, if it tracks such things
     * @param mouseX scaled position of the mouse horizontally
     * @param mouseY scaled position of the mouse vertically
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun resetHover(mouseX: Double, mouseY: Double) {
    }
}