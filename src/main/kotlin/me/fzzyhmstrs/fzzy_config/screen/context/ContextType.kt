package me.fzzyhmstrs.fzzy_config.screen.context

import me.fzzyhmstrs.fzzy_config.impl.config.KeybindsConfig
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.TriState
import net.minecraft.client.util.InputUtil
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
     * Subclasses or lambdas determine whether a user key input is relevant or not. They should also provide a way for actively determining if they are pressed, though this behavior is deferred.
     * @author fzzyhmstrs
     * @since 0.6.0
     */
    @FunctionalInterface
    @JvmDefaultWithCompatibility
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

        /**
         * Determines if the key is relevant in this instant (is being pressed right now)
         * @author fzzyhmstrs
         * @since 0.7.0
         */
        fun isPressed(): Boolean {
            return false
        }

        /**
         * Whether this needs the control key to be pressed to be relevant. Should only return true if it definitely needs it, not "can have it"
         * @author fzzyhmstrs
         * @since 0.7.3
         */
        fun needsCtrl(): Boolean {
            return false
        }

        /**
         * Whether this needs the shift key to be pressed to be relevant. Should only return true if it definitely needs it, not "can have it"
         * @author fzzyhmstrs
         * @since 0.7.3
         */
        fun needsShift(): Boolean {
            return false
        }

        /**
         * Whether this needs the alt key to be pressed to be relevant. Should only return true if it definitely needs it, not "can have it"
         * @author fzzyhmstrs
         * @since 0.7.3
         */
        fun needsAlt(): Boolean {
            return false
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
        @JvmStatic
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
        @JvmStatic
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

        @JvmStatic
        val PAGE_UP = create("page_up", ContextInput.KEYBOARD, KeybindsConfig.INSTANCE.pageUp)

        @JvmStatic
        val PAGE_DOWN = create("page_down", ContextInput.KEYBOARD, KeybindsConfig.INSTANCE.pageDown)

        @JvmStatic
        val HOME = create("home", ContextInput.KEYBOARD, KeybindsConfig.INSTANCE.home)

        @JvmStatic
        val END = create("end", ContextInput.KEYBOARD, KeybindsConfig.INSTANCE.end)

        /**
         * Ctrl-C, should move the context object to a copy buffer in some way
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        val COPY = create("copy", ContextInput.KEYBOARD, KeybindsConfig.INSTANCE.copy)

        /**
         * Ctrl-V, should take something from a copy buffer of some kind and move it into the context object
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        val PASTE = create("paste", ContextInput.KEYBOARD, KeybindsConfig.INSTANCE.paste)

        /**
         * Ctrl-X should move the context object to a copy buffer in some way and clear the object
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        val CUT = create("cut", ContextInput.KEYBOARD, KeybindsConfig.INSTANCE.cut)

        /**
         * Ctrl-F, should open or focus a search box in some way
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        val FIND = create("find", ContextInput.KEYBOARD, KeybindsConfig.INSTANCE.find)

        /**
         * Ctrl-S, should save context object state
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        val SAVE = create("save", ContextInput.KEYBOARD, KeybindsConfig.INSTANCE.save)

        /**
         * Ctrl-Z, should revert the last action taken (total across the whole context object), as opposed to revert, which should focus on the scoped context object only.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        val UNDO = create("undo", ContextInput.KEYBOARD, KeybindsConfig.INSTANCE.undo)

        /**
         * Shift-F10 or Menu key, should open a context menu (right click menu)
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        val CONTEXT_KEYBOARD = create("keyboard_context_menu", ContextInput.KEYBOARD, KeybindsConfig.INSTANCE.contextKeyboard)

        /**
         * Right click, should open a context menu (right click menu)
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        val CONTEXT_MOUSE = create("mouse_context_menu", ContextInput.MOUSE, KeybindsConfig.INSTANCE.contextMouse)

        /**
         * Enter, should, well, act on something.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        val ACT = create("act", ContextInput.KEYBOARD, KeybindsConfig.INSTANCE.act)

        /**
         * Backspace, should back out to a previous context (such as backing out a screen)
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        val BACK = create("back", ContextInput.KEYBOARD, KeybindsConfig.INSTANCE.back)

        /**
         * Ctrl-E, should open a navigation menu, as opposed to find which opens text-based finding this should open a "goto" style menu.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        val SEARCH = create("search", ContextInput.KEYBOARD, KeybindsConfig.INSTANCE.search)

        /**
         * F1, should open information window/popup/text etc.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        val INFO = create("info", ContextInput.KEYBOARD, KeybindsConfig.INSTANCE.info)

        /**
         * Fully exit the current open screen, including any popups, nested screens, etc.
         * @author fzzyhmstrs
         * @since 0.6.5
         */
        @JvmStatic
        val FULL_EXIT = create("full_exit", ContextInput.KEYBOARD, KeybindsConfig.INSTANCE.fullExit)


        //////// map keys only /////////////

        /**
         * Does not listen to user input. used as a map key for forwarding context to another user.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        val FORWARD = create("forward", ContextInput.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        /**
         * Does not listen to user input. Used to revert the current input change of the scoped context object, as oppoed to undo which should act on the whole context object (screen)
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        val REVERT = create("revert", ContextInput.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        /**
         * Does not listen to user input. Fully reverts the scoped context object to defaults.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        val RESTORE = create("restore", ContextInput.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        /**
         * Does not listen to user input. Clears the context object of content.
         * @author fzzyhmstrs
         * @since 0.6.0
         */
        @JvmStatic
        val CLEAR = create("clear", ContextInput.MOUSE, false) { _: Int, _: Boolean, _: Boolean, _: Boolean -> false }

        /**
         * Does not listen to user input. Selects all possible options in the context object.
         * @author fzzyhmstrs
         * @since 0.6.3
         */
        @JvmStatic
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