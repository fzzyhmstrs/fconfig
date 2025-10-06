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
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.isAltDown
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.isControlDown
import me.fzzyhmstrs.fzzy_config.util.PortingUtils.isShiftDown
import me.fzzyhmstrs.fzzy_config.util.TriState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.util.InputUtil
import net.minecraft.text.MutableText
import net.minecraft.text.Text

/**
 * Basic implementation of [Relevant] that uses [TriState] for processing modifier inputs. [TriState.DEFAULT] auto-passes the modifier key (either pressed or not-pressed will be considered relevant)
 * @param inputCode Int keycode of the key to test for
 * @param ctrl [TriState] whether ctrl modifier key is needed or not. Generally if ctrl is [TriState.TRUE], the other modifiers should be [TriState.FALSE] to avoid input relevance ambiguity
 * @param shift [TriState] whether shift modifier key is needed or not. Generally if shift is [TriState.TRUE], the other modifiers should be [TriState.FALSE] to avoid input relevance ambiguity
 * @param alt [TriState] whether alt modifier key is needed or not. Generally if alt is [TriState.TRUE], the other modifiers should be [TriState.FALSE] to avoid input relevance ambiguity
 * @author fzzyhmstrs
 * @since 0.6.5
 */
data class FzzyKeybindSimple(val inputCode: Int, val type: ContextInput, val ctrl: TriState, val shift: TriState, val alt: TriState): FzzyKeybind {

    constructor(inputCode: Int, type: ContextInput, ctrl: Boolean, shift: Boolean, alt: Boolean): this(inputCode, type, TriState.of(ctrl), TriState.of(shift), TriState.of(alt))

    override fun relevant(inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
        return this.inputCode == inputCode
                && this.ctrl.validate(ctrl)
                && this.shift.validate(shift)
                && this.alt.validate(alt)
    }

    override fun isPressed(): Boolean {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().window, inputCode)
                && this.ctrl.validate(isControlDown())
                && this.shift.validate(isShiftDown())
                && this.alt.validate(isAltDown())
    }

    override fun needsCtrl(): Boolean {
        return ctrl.asBoolean
    }

    override fun needsShift(): Boolean {
        return shift.asBoolean
    }

    override fun needsAlt(): Boolean {
        return alt.asBoolean
    }

    override fun keybind(): MutableText {
        val key: Text = if (type == ContextInput.KEYBOARD)
            InputUtil.Type.MOUSE.createFromCode(inputCode).localizedText
        else
            InputUtil.Type.MOUSE.createFromCode(inputCode).localizedText
        val c = ctrl == TriState.TRUE
        val s = shift == TriState.TRUE
        val a = alt == TriState.TRUE
        return if (c) {
            if (s) {
                if (a) {
                    FcText.translatable("fc.keybind.ctrl.shift.alt", key)
                } else {
                    FcText.translatable("fc.keybind.ctrl.shift", key)
                }
            } else if (a) {
                FcText.translatable("fc.keybind.ctrl.alt", key)
            } else {
                FcText.translatable("fc.keybind.ctrl", key)
            }
        } else if (s) {
            if (a) {
                FcText.translatable("fc.keybind.shift.alt", key)
            } else {
                FcText.translatable("fc.keybind.shift", key)
            }
        } else if (a) {
            FcText.translatable("fc.keybind.alt", key)
        } else {
            key.copy()
        }
    }

    override fun clone(): FzzyKeybind {
        return copy()
    }
}