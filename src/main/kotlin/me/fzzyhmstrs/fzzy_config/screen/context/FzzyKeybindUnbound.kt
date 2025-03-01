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

import me.fzzyhmstrs.fzzy_config.util.FcText
import net.minecraft.text.Text

data object FzzyKeybindUnbound: FzzyKeybind {

    override fun relevant(inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
        return false
    }

    override fun keybind(): Text {
        return FcText.translatable("key.keyboard.unknown")
    }

    override fun clone(): FzzyKeybind {
        return this
    }

    override fun containedKeybinds(): List<FzzyKeybind> {
        return listOf()
    }

    override fun compoundWith(other: FzzyKeybind): FzzyKeybind {
        return other
    }
}