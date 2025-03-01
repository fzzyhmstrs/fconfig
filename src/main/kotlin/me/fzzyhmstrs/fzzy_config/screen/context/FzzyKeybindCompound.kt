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

/**
 *
 * @author fzzyhmstrs
 * @since 0.6.5
 */
class FzzyKeybindCompound(val keybinds: List<FzzyKeybind>): FzzyKeybind {

    override fun relevant(inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
        return keybinds.any { it.relevant(inputCode, ctrl, shift, alt) }
    }

    override fun keybind(): Text {
        return when (keybinds.size) {
            0 -> {
                FcText.translatable("key.keyboard.unknown")
            }
            1 -> {
                keybinds[0].keybind()
            }
            else -> {
                var t = FcText.translatable("fc.keybind.or", keybinds[0].keybind(), keybinds[1].keybind())
                for (i in 2 until keybinds.size) {
                    t = FcText.translatable("fc.keybind.or", t, keybinds[i].keybind())
                }
                t
            }
        }
    }

    override fun clone(): FzzyKeybind {
        return FzzyKeybindCompound(keybinds.map { it.clone() }.toList())
    }
}