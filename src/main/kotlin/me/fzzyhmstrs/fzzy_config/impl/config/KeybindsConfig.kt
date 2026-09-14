/*
* Copyright (c) 2025 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.impl.config

import com.mojang.blaze3d.platform.InputConstants
import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.annotations.Version
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.context.ContextInput
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedKeybind
import org.lwjgl.sdl.SDLScancode

@Version(1)
internal class KeybindsConfig: Config("keybinds".fcId()) {

    @Comment("Scrolls up a 'page' in the Config GUI")
    var pageUp = ValidatedKeybind(InputConstants.KEY_PAGEUP, ContextInput.KEYBOARD)
    @Comment("Scrolls down a 'page' in the Config GUI")
    var pageDown = ValidatedKeybind(InputConstants.KEY_PAGEDOWN, ContextInput.KEYBOARD)
    @Comment("Scrolls to the top of the Config GUI")
    var home = ValidatedKeybind(InputConstants.KEY_HOME, ContextInput.KEYBOARD)
    @Comment("Scrolls to the bottom of the Config GUI")
    var end = ValidatedKeybind(InputConstants.KEY_END, ContextInput.KEYBOARD)
    @Comment("Copies the currently hovered or focused config setting")
    var copy = ValidatedKeybind(InputConstants.KEY_C, ContextInput.KEYBOARD, ctrl = true, shift = false, alt = false)
    @Comment("Pastes the last-copied setting into a compatible new setting")
    var paste = ValidatedKeybind(InputConstants.KEY_V, ContextInput.KEYBOARD, ctrl = true, shift = false, alt = false)
    @Comment("Not currently used by Fzzy Config - cuts the selected information to a copy buffer")
    var cut = ValidatedKeybind(InputConstants.KEY_X, ContextInput.KEYBOARD, ctrl = true, shift = false, alt = false)
    @Comment("Focuses the Config GUI search bar")
    var find = ValidatedKeybind { b -> b.keyboard(InputConstants.KEY_F, ctrl = true).keyboard(InputConstants.KEY_F3) }
    @Comment("Saves changes made to the current Config and sends updates to the server")
    var save = ValidatedKeybind(InputConstants.KEY_S, ContextInput.KEYBOARD, ctrl = true, shift = false, alt = false)
    @Comment("Undos changes made from newest to oldest change")
    var undo = ValidatedKeybind(InputConstants.KEY_Z, ContextInput.KEYBOARD, ctrl = true, shift = false, alt = false)
    @Comment("Opens the context menu for the currently hovered or selected element")
    var contextKeyboard = ValidatedKeybind { b -> b.keyboard(InputConstants.KEY_F10, shift = true).keyboard(SDLScancode.SDL_SCANCODE_MENU) } //TODO(ender) should be tested (needs to be `SDL_SCANCODE_APPLICATION` on my system)
    @Comment("Opens the context menu for the currently hovered or selected element")
    var contextMouse = ValidatedKeybind(InputConstants.MOUSE_BUTTON_RIGHT, ContextInput.MOUSE)
    @Comment("Not currently used by Fzzy Config - A universal 'Enter' keybind")
    var act = ValidatedKeybind { b -> b.keyboard(InputConstants.KEY_RETURN).keyboard(InputConstants.KEY_NUMPADENTER) }
    @Comment("Backs out to the previous config screen")
    var back = ValidatedKeybind(InputConstants.KEY_BACKSPACE, ContextInput.KEYBOARD)
    @Comment("Opens the 'Go-To' menu (Not used to focus the search bar)")
    var search = ValidatedKeybind(InputConstants.KEY_E, ContextInput.KEYBOARD, ctrl = true, shift = false, alt = false)
    @Comment("Opens the GUI info menu")
    var info = ValidatedKeybind(InputConstants.KEY_F1, ContextInput.KEYBOARD)
    @Comment("Fully exits all open config GUIs (and saves them as applicable)")
    var fullExit = ValidatedKeybind(InputConstants.KEY_ESCAPE, ContextInput.KEYBOARD, ctrl = false, shift = true, alt = false)

    companion object {
        val INSTANCE = ConfigApi.registerAndLoadNoGuiConfig(::KeybindsConfig, RegisterType.CLIENT)
    }
}