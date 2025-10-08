/*
 * Copyright (c) 2025 Fzzyhmstrs
 *
 * This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
 *
 * Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
 * You should have received a copy of the TDL-M with this software.
 * If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
 */

package me.fzzyhmstrs.fzzy_config.screen.widget.custom

import net.minecraft.client.gui.Click
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.client.input.MouseInput
import net.minecraft.client.util.InputUtil
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
@JvmDefaultWithCompatibility
interface CustomWidget {

    fun isMouse(event: MouseEvent): Boolean {
        return event.button() == 0
    }

    /**
     * Handles mouse click events for custom widgets. Override this or `onPress` for handling mouse inputs.
     * @param event [MouseEvent] click information to use during handling
     * @return whether handling was successful or not
     * @author fzzyhmstrs
     * @since 0.7.3
     */
    fun onMouse(event: MouseEvent): Boolean

    /**
     * Handles mouse drag events for custom widgets. Override this for handling drag inputs
     * @param event [MouseEvent] click information to use during handling
     * @return whether handling was successful or not
     * @author fzzyhmstrs
     * @since 0.7.3
     */
    fun onMouseDrag(event: MouseEvent): Boolean {
        return true
    }

    /**
     * Handles mouse button release events for custom widgets. Override this for handling mouse inputs.
     * @param event [MouseEvent] click information to use during handling
     * @return whether handling was successful or not
     * @author fzzyhmstrs
     * @since 0.7.3
     */
    fun onMouseRelease(event: MouseEvent): Boolean {
        return true
    }

    /**
     * Handles mouse scroll events for custom widgets. Override this for handling mouse inputs.
     * @param event [MouseEvent] click information to use during handling
     * @return whether handling was successful or not
     * @author fzzyhmstrs
     * @since 0.7.3
     */
    fun onMouseScroll(event: MouseEvent): Boolean {
        return false
    }

    /**
     * Handles keyboard events for custom widgets. Override this for handling keyboard inputs.
     * @param event [KeyEvent] keyboard information to use during handling
     * @return whether handling was successful or not
     * @author fzzyhmstrs
     * @since 0.7.3
     */
    fun onKey(event: KeyEvent): Boolean

    /**
     * Handles keyboard release events for custom widgets. Override this for handling keyboard inputs.
     * @param event [KeyEvent] keyboard information to use during handling
     * @return whether handling was successful or not
     * @author fzzyhmstrs
     * @since 0.7.3
     */
    fun onKeyRelease(event: KeyEvent): Boolean {
        return false
    }

    /**
     * Handles char input events for custom widgets. Override this for handling keyboard inputs.
     * @param event [CharEvent] character information to use during handling
     * @return whether handling was successful or not
     * @author fzzyhmstrs
     * @since 0.7.3
     */
    fun onChar(event: CharEvent): Boolean {
        return false
    }

    sealed interface MouseEvent {
        fun x(): Double
        fun y(): Double
        fun button(): Int
        fun modifiers(): Int
        fun double(): Boolean
        fun deltaX(): Double
        fun deltaY(): Double
        fun horizontalAmount(): Double
        fun verticalAmount(): Double

        fun clickWidget(widget: Element): Boolean {
            return clickWidgetOrNull(widget) ?: false
        }

        fun clickWidgetOrNull(widget: Element?): Boolean? {
            return if (widget is CustomWidget) {
                widget.onMouse(this)
            } else {
                widget?.mouseClicked(Click(x(), y(), MouseInput(button(), modifiers())), double())
            }
        }

        fun dragWidget(widget: Element): Boolean {
            return dragWidgetOrNull(widget) ?: false
        }

        fun dragWidgetOrNull(widget: Element?): Boolean? {
            return if (widget is CustomWidget) {
                widget.onMouseDrag(this)
            } else {
                widget?.mouseDragged(Click(x(), y(), MouseInput(button(), modifiers())), deltaX(), deltaY())
            }
        }

        fun releaseWidget(widget: Element): Boolean {
            return releaseWidgetOrNull(widget) ?: false
        }

        fun releaseWidgetOrNull(widget: Element?): Boolean? {
            return if (widget is CustomWidget) {
                widget.onMouseRelease(this)
            } else {
                widget?.mouseReleased(Click(x(), y(), MouseInput(button(), modifiers())))
            }
        }

        fun scrollWidget(widget: Element): Boolean {
            return scrollWidgetOrNull(widget) ?: false
        }

        fun scrollWidgetOrNull(widget: Element?): Boolean? {
            return if (widget is CustomWidget) {
                widget.onMouseScroll(this)
            } else {
                widget?.mouseScrolled(x(), y(), horizontalAmount(), verticalAmount())
            }
        }
    }

    class OnClick(private val click: Click, private val doubled: Boolean): MouseEvent {
        override fun x(): Double {
            return click.x
        }
        override fun y(): Double {
            return click.y
        }
        override fun button(): Int {
            return click.button()
        }
        override fun modifiers(): Int {
            return click.modifiers()
        }
        override fun double(): Boolean {
            return doubled
        }
        override fun deltaX(): Double {
            return 0.0
        }
        override fun deltaY(): Double {
            return 0.0
        }
        override fun horizontalAmount(): Double {
            return 0.0
        }
        override fun verticalAmount(): Double {
            return 0.0
        }
    }

    class OnRelease(private val click: Click): MouseEvent {
        override fun x(): Double {
            return click.x
        }
        override fun y(): Double {
            return click.y
        }
        override fun button(): Int {
            return click.button()
        }
        override fun modifiers(): Int {
            return click.modifiers()
        }
        override fun double(): Boolean {
            return false
        }
        override fun deltaX(): Double {
            return 0.0
        }
        override fun deltaY(): Double {
            return 0.0
        }
        override fun horizontalAmount(): Double {
            return 0.0
        }
        override fun verticalAmount(): Double {
            return 0.0
        }
    }

    class OnDrag(private val click: Click, private val offsetX: Double, private val offsetY: Double): MouseEvent {
        override fun x(): Double {
            return click.x
        }
        override fun y(): Double {
            return click.y
        }
        override fun button(): Int {
            return click.button()
        }
        override fun modifiers(): Int {
            return click.modifiers()
        }
        override fun double(): Boolean {
            return false
        }
        override fun deltaX(): Double {
            return offsetX
        }
        override fun deltaY(): Double {
            return offsetY
        }
        override fun horizontalAmount(): Double {
            return 0.0
        }
        override fun verticalAmount(): Double {
            return 0.0
        }
    }

    class OnScroll(private val mouseX: Double, private val mouseY: Double, private val horizontalAmount: Double, private val verticalAmount: Double): MouseEvent {

        override fun x(): Double {
            return mouseX
        }

        override fun y(): Double {
            return mouseY
        }

        override fun button(): Int {
            return -1
        }

        override fun modifiers(): Int {
            return 0
        }

        override fun double(): Boolean {
            return false
        }

        override fun deltaX(): Double {
            return 0.0
        }

        override fun deltaY(): Double {
            return 0.0
        }

        override fun horizontalAmount(): Double {
            return horizontalAmount
        }

        override fun verticalAmount(): Double {
            return verticalAmount
        }
    }

    class KeyEvent(private val input: KeyInput) {

        fun key(): Int {
            return input.key
        }
        fun scancode(): Int {
            return input.scancode
        }
        fun modifiers(): Int {
            return input.modifiers
        }

        fun isEnterOrSpace(): Boolean {
            return this.key() == InputUtil.GLFW_KEY_ENTER || (this.key() == InputUtil.GLFW_KEY_SPACE) || (this.key() == InputUtil.GLFW_KEY_KP_ENTER)
        }

        fun isLeft(): Boolean {
            return key() == InputUtil.GLFW_KEY_LEFT
        }

        fun isRight(): Boolean {
            return key() == InputUtil.GLFW_KEY_RIGHT
        }

        fun keyWidget(widget: Element): Boolean {
            return keyWidgetOrNull(widget) ?: false
        }

        fun keyWidgetOrNull(widget: Element?): Boolean? {
            return if (widget is CustomWidget) {
                widget.onKey(this)
            } else {
                widget?.keyPressed(KeyInput(key(), scancode(), modifiers()))
            }
        }

        fun releaseWidget(widget: Element): Boolean {
            return releaseWidgetOrNull(widget) ?: false
        }

        fun releaseWidgetOrNull(widget: Element?): Boolean? {
            return if (widget is CustomWidget) {
                widget.onKeyRelease(this)
            } else {
                widget?.keyReleased(KeyInput(key(), scancode(), modifiers()))
            }
        }
    }

    class CharEvent(private val input: CharInput) {

        fun codepoint(): Int {
            return input.codepoint
        }
        fun modifiers(): Int {
            return input.modifiers
        }

        fun charWidget(widget: Element): Boolean {
            return charWidgetOrNull(widget) ?: false
        }

        fun charWidgetOrNull(widget: Element?): Boolean? {
            return if (widget is CustomWidget) {
                widget.onChar(this)
            } else {
                widget?.charTyped(CharInput(codepoint(), modifiers()))
            }
        }
    }
}