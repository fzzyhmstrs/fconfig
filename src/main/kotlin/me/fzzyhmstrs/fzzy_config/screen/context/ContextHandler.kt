/*
 * Copyright (c) 2024 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.context

import org.lwjgl.glfw.GLFW
import java.util.*

interface ContextHandler {

    fun handleContext(contextType: ContextType, position: Position): Boolean

    class ContextType private constructor(private val id: String, private val relevantCheck: Relevant) {

        fun relevant(inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
            return relevantCheck.relevant(inputCode, ctrl, shift, alt)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ContextType) return false

            if (id != other.id) return false

            return true
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }

        override fun toString(): String {
            return "ContextType($id)"
        }

        @FunctionalInterface
        fun interface Relevant {
            fun relevant(inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean
        }

        companion object {

            private val ids: Vector<String> = Vector(16)

            private val keyboardCreated: Vector<ContextType> = Vector(16)
            private val mouseCreated: Vector<ContextType> = Vector(4)

            fun create(id: String, inputType: ContextInput, relevantCheck: Relevant): ContextType {
                return create(id, inputType, true, relevantCheck)
            }

            fun create(id: String, inputType: ContextInput, addToContextList: Boolean, relevantCheck: Relevant): ContextType {
                if (ids.contains(id))
                    throw IllegalStateException("Duplicate context types not allowed; $id already created for input $inputType")
                val ct = ContextType(id, relevantCheck)
                when (inputType) {
                    ContextInput.KEYBOARD -> {
                        if (addToContextList)
                            keyboardCreated.add(ct)
                        ids.add(id)
                    }
                    ContextInput.MOUSE -> {
                        if (addToContextList)
                            mouseCreated.add(ct)
                        ids.add(id)
                    }
                }
                return ct
            }

            fun types(inputType: ContextInput): List<ContextType> {
                return when (inputType) {
                    ContextInput.KEYBOARD -> keyboardCreated
                    ContextInput.MOUSE -> mouseCreated
                }
            }
        }
    }

    companion object {

        val PAGE_UP = ContextType.create("page_up", ContextInput.KEYBOARD) { inputCode: Int, _, _, _ ->
            inputCode == GLFW.GLFW_KEY_PAGE_UP
        }

        val PAGE_DOWN = ContextType.create("page_down", ContextInput.KEYBOARD) { inputCode: Int, _, _, _ ->
            inputCode == GLFW.GLFW_KEY_PAGE_DOWN
        }

        val HOME = ContextType.create("home", ContextInput.KEYBOARD) { inputCode: Int, _, _, _ ->
            inputCode == GLFW.GLFW_KEY_HOME
        }

        val END = ContextType.create("end", ContextInput.KEYBOARD) { inputCode: Int, _, _, _ ->
            inputCode == GLFW.GLFW_KEY_END
        }

        val COPY = ContextType.create("copy", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_C && ctrl && !shift && !alt
        }

        val PASTE = ContextType.create("paste", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_V && ctrl && !shift && !alt
        }

        val CUT = ContextType.create("cut", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_X && ctrl && !shift && !alt
        }

        val FIND = ContextType.create("find", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            ((inputCode == GLFW.GLFW_KEY_F && ctrl && !shift && !alt) || inputCode == GLFW.GLFW_KEY_F3)
        }

        val SAVE = ContextType.create("save", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_S && ctrl && !shift && !alt
        }

        val UNDO = ContextType.create("undo", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_Z && ctrl && !shift && !alt
        }

        val CONTEXT_KEYBOARD = ContextType.create("keyboard_context_menu", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            ((inputCode == GLFW.GLFW_KEY_F10 && !ctrl && shift && !alt) || inputCode == GLFW.GLFW_KEY_MENU)
        }

        val CONTEXT_MOUSE = ContextType.create("mouse_context_menu", ContextInput.MOUSE) { inputCode: Int, _, _, _ ->
            inputCode == GLFW.GLFW_MOUSE_BUTTON_RIGHT
        }

        val ACT = ContextType.create("act", ContextInput.MOUSE) { inputCode: Int, _: Boolean, _: Boolean, _: Boolean ->
            inputCode == GLFW.GLFW_KEY_ENTER || inputCode == GLFW.GLFW_KEY_KP_ENTER
        }

        //////// map keys only /////////////

        val FORWARD = ContextType.create("forward", ContextInput.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        val REVERT = ContextType.create("revert", ContextInput.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        val RESTORE = ContextType.create("restore", ContextInput.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        fun getRelevantContext(inputCode: Int, contextInput: ContextInput, ctrl: Boolean, shift: Boolean, alt: Boolean): ContextType? {
            for (type in ContextType.types(contextInput)) {
                if (type.relevant(inputCode, ctrl, shift, alt)) {
                    return type
                }
            }
            return null
        }

        fun init() {}
    }
}