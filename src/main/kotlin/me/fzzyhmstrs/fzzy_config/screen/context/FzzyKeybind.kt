/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.context

import me.fzzyhmstrs.fzzy_config.screen.context.ContextType.Relevant
import net.minecraft.text.Text

/**
 * Representation of a keybind in Fzzy Config, based on [ContextType] handling with the [Relevant] interface. These keybinds are not "linked" into anything by default, handling of the context input has be handled by you as introduced on the wiki [https://moddedmc.wiki/en/project/fzzy-config/docs/features/Context-Actions](https://moddedmc.wiki/en/project/fzzy-config/docs/features/Context-Actions)
 * @author fzzyhmstrs
 * @since 0.6.5
 */
sealed interface FzzyKeybind: Relevant {
    /**
     * Override from [Relevant]. Determines if an input is relevant for the handler or not.
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    override fun relevant(inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean
    /**
     * Creates a [Text] representation of the current keybind
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    fun keybind(): Text
    /**
     * Copies the current keybind object where possible.
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    fun clone(): FzzyKeybind
}