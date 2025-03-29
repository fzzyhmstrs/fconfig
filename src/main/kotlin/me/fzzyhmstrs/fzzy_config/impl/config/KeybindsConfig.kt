package me.fzzyhmstrs.fzzy_config.impl.config

import me.fzzyhmstrs.fzzy_config.annotations.Comment
import me.fzzyhmstrs.fzzy_config.api.ConfigApi
import me.fzzyhmstrs.fzzy_config.api.RegisterType
import me.fzzyhmstrs.fzzy_config.config.Config
import me.fzzyhmstrs.fzzy_config.fcId
import me.fzzyhmstrs.fzzy_config.screen.context.ContextInput
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedKeybind
import org.lwjgl.glfw.GLFW

internal class KeybindsConfig: Config("keybinds".fcId()) {

    @Comment("Scrolls up a 'page' in the Config GUI")
    var pageUp = ValidatedKeybind(GLFW.GLFW_KEY_PAGE_UP, ContextInput.KEYBOARD)
    @Comment("Scrolls down a 'page' in the Config GUI")
    var pageDown = ValidatedKeybind(GLFW.GLFW_KEY_PAGE_DOWN, ContextInput.KEYBOARD)
    @Comment("Scrolls to the top of the Config GUI")
    var home = ValidatedKeybind(GLFW.GLFW_KEY_HOME, ContextInput.KEYBOARD)
    @Comment("Scrolls to the bottom of the Config GUI")
    var end = ValidatedKeybind(GLFW.GLFW_KEY_END, ContextInput.KEYBOARD)
    @Comment("Copies the currently hovered or focused config setting")
    var copy = ValidatedKeybind(GLFW.GLFW_KEY_C, ContextInput.KEYBOARD, ctrl = true, shift = false, alt = false)
    @Comment("Pastes the last-copied setting into a compatible new setting")
    var paste = ValidatedKeybind(GLFW.GLFW_KEY_V, ContextInput.KEYBOARD, ctrl = true, shift = false, alt = false)
    @Comment("Not currently used by Fzzy Config - cuts the selected information to a copy buffer")
    var cut = ValidatedKeybind(GLFW.GLFW_KEY_X, ContextInput.KEYBOARD, ctrl = true, shift = false, alt = false)
    @Comment("Focuses the Config GUI search bar")
    var find = ValidatedKeybind { b -> b.keyboard(GLFW.GLFW_KEY_F, ctrl = true).keyboard(GLFW.GLFW_KEY_F3) }
    @Comment("Saves changes made to the current Config and sends updates to the server")
    var save = ValidatedKeybind(GLFW.GLFW_KEY_S, ContextInput.KEYBOARD, ctrl = true, shift = false, alt = false)
    @Comment("Undos changes made from newest to oldest change")
    var undo = ValidatedKeybind(GLFW.GLFW_KEY_Z, ContextInput.KEYBOARD, ctrl = true, shift = false, alt = false)
    @Comment("Opens the context menu for the currently hovered or selected element")
    var contextKeyboard = ValidatedKeybind { b -> b.keyboard(GLFW.GLFW_KEY_F10, shift = true).keyboard(GLFW.GLFW_KEY_MENU) }
    @Comment("Opens the context menu for the currently hovered or selected element")
    var contextMouse = ValidatedKeybind(GLFW.GLFW_MOUSE_BUTTON_RIGHT, ContextInput.MOUSE)
    @Comment("Not currently used by Fzzy Config - A universal 'Enter' keybind")
    var act = ValidatedKeybind { b -> b.keyboard(GLFW.GLFW_KEY_ENTER).keyboard(GLFW.GLFW_KEY_KP_ENTER) }
    @Comment("Backs out to the previous config screen")
    var back = ValidatedKeybind(GLFW.GLFW_KEY_BACKSPACE, ContextInput.KEYBOARD)
    @Comment("Opens the 'Go-To' menu (Not used to focus the search bar)")
    var search = ValidatedKeybind(GLFW.GLFW_KEY_E, ContextInput.KEYBOARD, ctrl = true, shift = false, alt = false)
    @Comment("Opens the GUI info menu")
    var info = ValidatedKeybind(GLFW.GLFW_KEY_F1, ContextInput.KEYBOARD)
    @Comment("Fully exits all open config GUIs (and saves them as applicable)")
    var fullExit = ValidatedKeybind(GLFW.GLFW_KEY_ESCAPE, ContextInput.KEYBOARD, ctrl = false, shift = true, alt = false)

    companion object {
        val INSTANCE = ConfigApi.registerAndLoadNoGuiConfig(::KeybindsConfig, RegisterType.CLIENT)
    }
}