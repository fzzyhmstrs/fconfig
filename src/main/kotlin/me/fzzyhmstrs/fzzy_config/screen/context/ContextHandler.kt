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

    fun handleContext(contextType: ContextType): Boolean

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

        @FunctionalInterface
        fun interface Relevant {
            fun relevant(inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean
        }

        companion object {



            private val ids: Vector<String> = Vector(16)

            private val keyboardCreated: Vector<ContextType> = Vector(16)
            private val mouseCreated: Vector<ContextType> = Vector(4)

            fun create(id: String, inputType: Input, relevantCheck: Relevant): ContextType {
                return create(id, inputType, true, relevantCheck)
            }

            fun create(id: String, inputType: Input, addToContextList: Boolean, relevantCheck: Relevant): ContextType {
                if (ids.contains(id))
                    throw IllegalStateException("Duplicate context types not allowed; $id already created for input $inputType")
                val ct = ContextType(id, relevantCheck)
                when (inputType) {
                    Input.KEYBOARD -> {
                        if (addToContextList)
                            keyboardCreated.add(ct)
                        ids.add(id)
                    }
                    Input.MOUSE -> {
                        if (addToContextList)
                            mouseCreated.add(ct)
                        ids.add(id)
                    }
                }
                return ct
            }

            fun types(inputType: Input): List<ContextType> {
                return when (inputType) {
                    Input.KEYBOARD -> keyboardCreated
                    Input.MOUSE -> mouseCreated
                }
            }
        }
    }

    enum class Input {
        KEYBOARD,
        MOUSE
    }

    companion object {

        val PAGE_UP = ContextType.create("page_up", Input.KEYBOARD) { inputCode: Int, _, _, _ ->
            inputCode == GLFW.GLFW_KEY_PAGE_UP
        }

        val PAGE_DOWN = ContextType.create("page_down", Input.KEYBOARD) { inputCode: Int, _, _, _ ->
            inputCode == GLFW.GLFW_KEY_PAGE_DOWN
        }

        val HOME = ContextType.create("home", Input.KEYBOARD) { inputCode: Int, _, _, _ ->
            inputCode == GLFW.GLFW_KEY_HOME
        }

        val END = ContextType.create("end", Input.KEYBOARD) { inputCode: Int, _, _, _ ->
            inputCode == GLFW.GLFW_KEY_END
        }

        val COPY = ContextType.create("copy", Input.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_C && ctrl && !shift && !alt
        }

        val PASTE = ContextType.create("paste", Input.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_V && ctrl && !shift && !alt
        }

        val CUT = ContextType.create("cut", Input.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_X && ctrl && !shift && !alt
        }

        val FIND = ContextType.create("find", Input.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            ((inputCode == GLFW.GLFW_KEY_F && ctrl && !shift && !alt) || inputCode == GLFW.GLFW_KEY_F3)
        }

        val SAVE = ContextType.create("save", Input.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_S && ctrl && !shift && !alt
        }

        val UNDO = ContextType.create("undo", Input.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_Z && ctrl && !shift && !alt
        }

        val CONTEXT_KEYBOARD = ContextType.create("keyboard_context_menu", Input.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            ((inputCode == GLFW.GLFW_KEY_F10 && !ctrl && shift && !alt) || inputCode == GLFW.GLFW_KEY_MENU)
        }

        val CONTEXT_MOUSE = ContextType.create("mouse_context_menu", Input.MOUSE) { inputCode: Int, _, _, _ ->
            inputCode == GLFW.GLFW_MOUSE_BUTTON_RIGHT
        }

        val ACT = ContextType.create("act", Input.MOUSE) { inputCode: Int, _: Boolean, _: Boolean, _: Boolean ->
            inputCode == GLFW.GLFW_KEY_ENTER || inputCode == GLFW.GLFW_KEY_KP_ENTER
        }

        //////// map keys only /////////////

        val FORWARD = ContextType.create("forward", Input.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        val REVERT = ContextType.create("revert", Input.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        val RESTORE = ContextType.create("restore", Input.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        fun getRelevantContext(inputCode: Int, input: Input, ctrl: Boolean, shift: Boolean, alt: Boolean): ContextType? {
            for (type in ContextType.types(input)) {
                if (type.relevant(inputCode, ctrl, shift, alt)) {
                    return type
                }
            }
            return null
        }
    }
}