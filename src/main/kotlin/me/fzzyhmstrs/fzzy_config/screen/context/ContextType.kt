package me.fzzyhmstrs.fzzy_config.screen.context

import org.lwjgl.glfw.GLFW
import java.util.*

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

        val PAGE_UP = create("page_up", ContextInput.KEYBOARD) { inputCode: Int, _, _, _ ->
            inputCode == GLFW.GLFW_KEY_PAGE_UP
        }

        val PAGE_DOWN = create("page_down", ContextInput.KEYBOARD) { inputCode: Int, _, _, _ ->
            inputCode == GLFW.GLFW_KEY_PAGE_DOWN
        }

        val HOME = create("home", ContextInput.KEYBOARD) { inputCode: Int, _, _, _ ->
            inputCode == GLFW.GLFW_KEY_HOME
        }

        val END = create("end", ContextInput.KEYBOARD) { inputCode: Int, _, _, _ ->
            inputCode == GLFW.GLFW_KEY_END
        }

        val COPY = create("copy", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_C && ctrl && !shift && !alt
        }

        val PASTE = create("paste", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_V && ctrl && !shift && !alt
        }

        val CUT = create("cut", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_X && ctrl && !shift && !alt
        }

        val FIND = create("find", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            ((inputCode == GLFW.GLFW_KEY_F && ctrl && !shift && !alt) || inputCode == GLFW.GLFW_KEY_F3)
        }

        val SAVE = create("save", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_S && ctrl && !shift && !alt
        }

        val UNDO = create("undo", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_Z && ctrl && !shift && !alt
        }

        val CONTEXT_KEYBOARD = create("keyboard_context_menu", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            ((inputCode == GLFW.GLFW_KEY_F10 && !ctrl && shift && !alt) || inputCode == GLFW.GLFW_KEY_MENU)
        }

        val CONTEXT_MOUSE = create("mouse_context_menu", ContextInput.MOUSE) { inputCode: Int, _, _, _ ->
            inputCode == GLFW.GLFW_MOUSE_BUTTON_RIGHT
        }

        val ACT = create("act", ContextInput.KEYBOARD) { inputCode: Int, _: Boolean, _: Boolean, _: Boolean ->
            inputCode == GLFW.GLFW_KEY_ENTER || inputCode == GLFW.GLFW_KEY_KP_ENTER
        }

        val BACK = create("back", ContextInput.KEYBOARD) { inputCode: Int, _: Boolean, _: Boolean, _: Boolean ->
            inputCode == GLFW.GLFW_KEY_BACKSPACE
        }

        val SEARCH = create("search", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            ((inputCode == GLFW.GLFW_KEY_E && ctrl && !shift && !alt))
        }

        val INFO = create("info", ContextInput.KEYBOARD) { inputCode: Int, _: Boolean, _: Boolean, _: Boolean ->
            inputCode == GLFW.GLFW_KEY_F1
        }


        //////// map keys only /////////////

        val FORWARD = create("forward", ContextInput.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        val REVERT = create("revert", ContextInput.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        val RESTORE = create("restore", ContextInput.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        val CLEAR = create("clear", ContextInput.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        fun getRelevantContext(inputCode: Int, contextInput: ContextInput, ctrl: Boolean, shift: Boolean, alt: Boolean): ContextType? {
            for (type in types(contextInput)) {
                if (type.relevant(inputCode, ctrl, shift, alt)) {
                    return type
                }
            }
            return null
        }

        fun init() {}
    }
}