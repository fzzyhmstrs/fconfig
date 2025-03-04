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
import net.minecraft.text.MutableText
import net.minecraft.text.Text

/**
 * Representation of a keybind in Fzzy Config, based on [ContextType] handling with the [Relevant] interface. These keybinds are not "linked" into anything by default, handling of the context input has be handled by you as introduced on the wiki [https://moddedmc.wiki/en/project/fzzy-config/docs/features/Context-Actions](https://moddedmc.wiki/en/project/fzzy-config/docs/features/Context-Actions)
 * @author fzzyhmstrs
 * @since 0.6.5
 */
@JvmDefaultWithoutCompatibility
sealed interface FzzyKeybind: Relevant {
    /**
     * Override from [Relevant]. Determines if an input is relevant for the handler or not.
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    override fun relevant(inputCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean): Boolean

    /**
     * Creates a [Text] representation of the current keybind
     * @return Text representation of the keybind
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    fun keybind(): MutableText

    /**
     * Copies the current keybind object where possible.
     * @return [FzzyKeybind] deep copy of this keybind
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    fun clone(): FzzyKeybind

    /**
     * Keybinds contained within this keybind.
     * @return List&lt;[FzzyKeybind]&gt; the keybinds within this keybind. By default, simply this keybind
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    fun containedKeybinds(): List<FzzyKeybind> {
        return listOf(this)
    }

    /**
     * Creates a compound keybind (multiple-choice) with [other]. If this compound is already compound, the new addition will be added as a further choice.
     * @param other [FzzyKeybind] other keybind to compound with
     * @return [FzzyKeybind] compounding this keybinds options with the other keybinds
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    fun compoundWith(other: FzzyKeybind): FzzyKeybind {
        val list = this.containedKeybinds() + other.containedKeybinds()
        return when (list.size) {
            0 -> FzzyKeybindUnbound
            1 -> list[0]
            else -> FzzyKeybindCompound(list)
        }
    }

    /**
     * Builds a [FzzyKeybind] from 0 or more individual keybind inputs. If 0 inputs are provided, an unbound keybind will be supplied. Multiple inputs are evaluated as "OR" arguments. Any 1 input can pass for the keybind to pass.
     * @author fzzyhmstrs
     * @since 0.6.5
     */
    class Builder {
        private val keybinds: MutableList<FzzyKeybind> = mutableListOf()

        /**
         * Adds a keyboard-type keybind.
         * @param keyCode Keyboard key to test against. Using GLFW here is recommended.
         * @param ctrl Whether the ctrl modifier needs to be active (left/right ctrl or left/right super pressed)
         * @param shift Whether the shift modifier needs to be active (left/right shift pressed)
         * @param alt Whether the alt modifier needs to be active (left/right alt pressed)
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.5
         */
        @JvmOverloads
        fun keyboard(keyCode: Int, ctrl: Boolean = false, shift: Boolean = false, alt: Boolean = false): Builder {
            keybinds.add(FzzyKeybindSimple(keyCode, ContextInput.KEYBOARD, ctrl, shift, alt))
            return this
        }

        /**
         * Adds a mouse-type keybind.
         * @param button Mouse button to test against. Using GLFW here is recommended.
         * @param ctrl Whether the ctrl modifier needs to be active (left/right ctrl or left/right super pressed)
         * @param shift Whether the shift modifier needs to be active (left/right shift pressed)
         * @param alt Whether the alt modifier needs to be active (left/right alt pressed)
         * @return this builder
         * @author fzzyhmstrs
         * @since 0.6.5
         */
        @JvmOverloads
        fun mouse(button: Int, ctrl: Boolean = false, shift: Boolean = false, alt: Boolean = false): Builder {
            keybinds.add(FzzyKeybindSimple(button, ContextInput.MOUSE, ctrl, shift, alt))
            return this
        }

        /**
         * Builds the keybind.
         * @return [FzzyKeybind] result.
         * @author fzzyhmstrs
         * @since 0.6.5
         */
        fun build(): FzzyKeybind {
            return when (keybinds.size) {
                0 -> FzzyKeybindUnbound
                1 -> keybinds[0]
                else -> FzzyKeybindCompound(keybinds)
            }
        }

    }
}