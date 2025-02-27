package me.fzzyhmstrs.fzzy_config.screen.context

import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.TriState
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import java.util.*

/**
 * A context action key, that doubles as a "keybind", presenting itself as relevant for context handling if key/mouse input is relevant to it.
 * @author fzzyhmstrs
 * @since 0.6.0
 */
class ContextType private constructor(private val id: String, private val relevantCheck: Relevant) {

    /**
     * Determines whether this type is relevant to the user inputs provided. In general, only the key state you care about should be checked. For example, if your keybind is "X", check that the inputCode is correct but ignore ctrl/shift/alt entirely, unless it is specifically important that they not be pressed.
     * @param inputCode Integer code. For keyboard inputs, will be the keyboard code, for mouse inputs, will be the mouse button code.
     * @param ctrl whether the Ctrl key is pressed
     * @param shift whether the Shift key is pressed
     * @param alt whether the Alt key is pressed
     * @return true if the type is relevant to the current user input, false otherwise
     * @author fzzyhmstrs
     * @since 0.6.0
     */
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

    /**
     * Subclasses or lambdas determine whether a user key input is relevant or not.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    @FunctionalInterface
    fun interface Relevant {
        /**
         * Determines whether this is relevant to the user inputs provided. In general, only the state you care about should be checked. For example, if your keybind is "X", check that the inputCode is correct but ignore ctrl/shift/alt entirely, unless it is specifically important that they not be pressed.
         * @param inputCode Integer code. For keyboard inputs, will be the keyboard code, for mouse inputs, will be the mouse button code.
         * @param ctrl whether the Ctrl key is pressed
         * @param shift whether the Shift key is pressed
         * @param alt whether the Alt key is pressed
         * @return true if the type is relevant to the current user input, false otherwise
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun relevant(inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean
    }

    /**
     * Basic implementation of [Relevant] that uses [TriState] for processing modifier inputs. [TriState.DEFAULT] auto-passes the modifier key (either pressed or not-pressed will be considered relevant)
     * @param inputCode Int keycode of the key to test for
     * @param ctrl [TriState] whether ctrl modifier key is needed or not. Generally if ctrl is [TriState.TRUE], the other modifiers should be [TriState.FALSE] to avoid input relevance ambiguity
     * @param shift [TriState] whether shift modifier key is needed or not. Generally if shift is [TriState.TRUE], the other modifiers should be [TriState.FALSE] to avoid input relevance ambiguity
     * @param alt [TriState] whether alt modifier key is needed or not. Generally if alt is [TriState.TRUE], the other modifiers should be [TriState.FALSE] to avoid input relevance ambiguity
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    data class RelevantImpl(val inputCode: Int, val ctrl: TriState, val shift: TriState, val alt: TriState): Relevant {

        constructor(inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): this(inputCode, TriState.of(ctrl), TriState.of(shift), TriState.of(alt))
        
        override fun relevant(inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean {
            return this.inputCode == inputCode
                    && this.ctrl.validate(ctrl)
                    && this.shift.validate(shift)
                    && this.alt.validate(alt)
        }

        fun keybind(): Text {
            val key: Text = TODO()
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
                FcText.translatable("fc.keybind", key)
            }
        }
    }

    companion object {

        private val ids: Vector<String> = Vector(16)

        private val keyboardCreated: Vector<ContextType> = Vector(16)
        private val mouseCreated: Vector<ContextType> = Vector(4)

        /**
         * Creates a new [ContextType] and adds it to the context list for checking context inputs.
         * @param id Unique string id for this type. Duplicate ids will throw an exception.
         * @param inputType [ContextInput] what type of input this type monitors.
         * @param relevantCheck [Relevant] instance that checks inputs for type relevance.
         * @return [ContextType] instance
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun create(id: String, inputType: ContextInput, relevantCheck: Relevant): ContextType {
            return create(id, inputType, true, relevantCheck)
        }

        /**
         * Creates a new [ContextType]
         * @param id Unique string id for this type. Duplicate ids will throw an exception.
         * @param inputType [ContextInput] what type of input this type monitors.
         * @param addToContextList If true, will be added to the type list for monitoring inputs; if false, will act only as a map Key, and won't ever present itself as relevant when using [getRelevantContext]
         * @param relevantCheck [Relevant] instance that checks inputs for type relevance.
         * @return [ContextType] instance
         * @author fzzyhmstrs
         * @since 0.6.0
         */
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

        /**
         * Ctrl-C, should move the context object to a copy buffer in some way
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val COPY = create("copy", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_C && ctrl && !shift && !alt
        }

        /**
         * Ctrl-V, should take something from a copy buffer of some kind and move it into the context object
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val PASTE = create("paste", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_V && ctrl && !shift && !alt
        }

        /**
         * Ctrl-X should move the context object to a copy buffer in some way and clear the object
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val CUT = create("cut", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_X && ctrl && !shift && !alt
        }

        /**
         * Ctrl-F, should open or focus a search box in some way
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val FIND = create("find", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            ((inputCode == GLFW.GLFW_KEY_F && ctrl && !shift && !alt) || inputCode == GLFW.GLFW_KEY_F3)
        }

        /**
         * Ctrl-S, should save context object state
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val SAVE = create("save", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_S && ctrl && !shift && !alt
        }

        /**
         * Ctrl-Z, should revert the last action taken (total across the whole context object), as opposed to revert, which should focus on the scoped context object only.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val UNDO = create("undo", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_Z && ctrl && !shift && !alt
        }

        /**
         * Shift-F10 or Menu key, should open a context menu (right click menu)
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val CONTEXT_KEYBOARD = create("keyboard_context_menu", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            ((inputCode == GLFW.GLFW_KEY_F10 && !ctrl && shift && !alt) || inputCode == GLFW.GLFW_KEY_MENU)
        }

        /**
         * Right click, should open a context menu (right click menu)
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val CONTEXT_MOUSE = create("mouse_context_menu", ContextInput.MOUSE) { inputCode: Int, _, _, _ ->
            inputCode == GLFW.GLFW_MOUSE_BUTTON_RIGHT
        }

        /**
         * Enter, should, well, act on something.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val ACT = create("act", ContextInput.KEYBOARD) { inputCode: Int, _: Boolean, _: Boolean, _: Boolean ->
            inputCode == GLFW.GLFW_KEY_ENTER || inputCode == GLFW.GLFW_KEY_KP_ENTER
        }

        /**
         * Backspace, should back out to a previous context (such as backing out a screen)
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val BACK = create("back", ContextInput.KEYBOARD) { inputCode: Int, _: Boolean, _: Boolean, _: Boolean ->
            inputCode == GLFW.GLFW_KEY_BACKSPACE
        }

        /**
         * Ctrl-E, should open a navigation menu, as opposed to find which opens text-based finding this should open a "goto" style menu.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val SEARCH = create("search", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            ((inputCode == GLFW.GLFW_KEY_E && ctrl && !shift && !alt))
        }

        /**
         * F1, should open information window/popup/text etc.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val INFO = create("info", ContextInput.KEYBOARD) { inputCode: Int, _: Boolean, _: Boolean, _: Boolean ->
            inputCode == GLFW.GLFW_KEY_F1
        }

        /**
         * Fully exit the current open screen, including any popups, nested screens, etc.
         * @author fzzyhmstrs
         * @since 0.6.5
         */
        val FULL_EXIT = create("full_exit", ContextInput.KEYBOARD) { inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean ->
            inputCode == GLFW.GLFW_KEY_ESCAPE && !ctrl && shift && !alt
        }


        //////// map keys only /////////////

        /**
         * Does not listen to user input. used as a map key for forwarding context to another user.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val FORWARD = create("forward", ContextInput.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        /**
         * Does not listen to user input. Used to revert the current input change of the scoped context object, as oppoed to undo which should act on the whole context object (screen)
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val REVERT = create("revert", ContextInput.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        /**
         * Does not listen to user input. Fully reverts the scoped context object to defaults.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val RESTORE = create("restore", ContextInput.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        /**
         * Does not listen to user input. Clears the context object of content.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        val CLEAR = create("clear", ContextInput.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        /**
         * Does not listen to user input. Selects all possible options in the context object.
         * @author fzzyhmstrs
         * @since 0.6.3
         */
        val SELECT_ALL = create("select_all", ContextInput.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }


        private fun types(inputType: ContextInput): List<ContextType> {
            return when (inputType) {
                ContextInput.KEYBOARD -> keyboardCreated
                ContextInput.MOUSE -> mouseCreated
            }
        }

        /**
         * Returns a list of [ContextType] that are relevant to the provided input. This can return multiple inputs, which can be handled separately or together.
         * @param inputCode Integer code. For keyboard inputs, will be the keyboard code, for mouse inputs, will be the mouse button code.
         * @param contextInput [ContextInput] what type of input event is being triggered.
         * @param ctrl whether the Ctrl key is pressed
         * @param shift whether the Shift key is pressed
         * @param alt whether the Alt key is pressed
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        fun getRelevantContext(inputCode: Int, contextInput: ContextInput, ctrl: Boolean, shift: Boolean, alt: Boolean): List<ContextType> {
            return types(contextInput).filter { it.relevant(inputCode, ctrl, shift, alt) }
        }

        internal fun init() {}
    }
}
