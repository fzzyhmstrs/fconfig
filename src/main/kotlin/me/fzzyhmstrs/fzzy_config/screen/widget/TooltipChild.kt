/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.widget

import net.minecraft.text.Text

/**
 * A TooltipChild is a child of some parent element that may or may not need to present tooltip and narration information to its parent. This system is used to compile tooltips and narrations from pieces rather than having to manually build the final tooltip in the parent, or having whichever elements tooltip that renders last "winning".
 * @author fzzyhmstrs
 * @since 0.6.0
 */
@JvmDefaultWithoutCompatibility
interface TooltipChild {

    /**
     * Provides a list of text lines for appending to a tooltip. Unlike appending in MC, the parent has final say on compiling the tooltip from the pieces given to it.
     * @param mouseX Horizontal position of the mouse in pixels.
     * @param mouseY Vertical position of the mouse in pixels.
     * @param parentSelected True if the parent is hovered **or** focused
     * @param keyboardFocused True if the parent is focused **(NOT hovered)** and navigation is via keyboard
     * @return A list of [Text] tooltip lines to append into the parents tooltip
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun provideTooltipLines(mouseX: Int, mouseY: Int, parentSelected: Boolean, keyboardFocused: Boolean): List<Text> {
        return EMPTY
    }

    /**
     * Provides a list of text for appending into a Narration Hint. By default this will use the same lines passed from [provideTooltipLines], with predefined values for the inputs
     * - mouseX: 0
     * - mouseY: 0
     * - parentSelected: true
     * - keyboardFocused: true
     * @return A list of text to narrate
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    fun provideNarrationLines(): List<Text> {
        return provideTooltipLines(0, 0, parentSelected = true, keyboardFocused = true)
    }

    companion object {
        /**
         * A convenience empty list of [Text] for using in fallback scenarios (such as the default return of [provideTooltipLines])
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val EMPTY = listOf<Text>()
    }
}